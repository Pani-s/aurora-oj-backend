package com.pani.oj.judge;

import cn.hutool.json.JSONUtil;

import com.pani.oj.common.ErrorCode;
import com.pani.oj.exception.BusinessException;
import com.pani.oj.judge.codesandbox.CodeSandbox;
import com.pani.oj.judge.codesandbox.CodeSandboxFactory;
import com.pani.oj.judge.codesandbox.CodeSandboxProxy;
import com.pani.oj.judge.strategy.JudgeContext;
import com.pani.oj.model.dto.question.JudgeCase;
import com.pani.oj.model.dto.question.JudgeConfig;
import com.pani.oj.model.dto.questionsubmit.QuestionDebugRequest;
import com.pani.oj.model.entity.Question;
import com.pani.oj.model.entity.QuestionSubmit;
import com.pani.oj.model.enums.JudgeInfoMessageEnum;
import com.pani.oj.model.enums.QuestionSubmitStatusEnum;
import com.pani.oj.model.sandbox.ExecuteCodeRequest;
import com.pani.oj.model.sandbox.ExecuteCodeResponse;
import com.pani.oj.model.sandbox.JudgeInfo;
import com.pani.oj.model.vo.QuestionDebugResponse;
import com.pani.oj.mq.rabbitmq.QuestionMessageProducer;
import com.pani.oj.service.QuestionService;
import com.pani.oj.service.QuestionSubmitService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * @author Pani
 * @date Created in 2024/3/8 20:42
 * @description
 */
@Slf4j
@Service
public class JudgeServiceImpl implements JudgeService {
    @Value("${codeSandbox.type:example}")
    private String type;

    private final AtomicReference<String> atomicType = new AtomicReference<>();

    @Resource
    private CodeSandboxFactory codeSandboxFactory;

    @Resource
    @Lazy
    private QuestionService questionService;

    @Resource
    @Lazy
    private QuestionSubmitService questionSubmitService;

    @Resource
    private JudgeManager judgeManager;

    @Resource
    private QuestionMessageProducer questionMessageProducer;

    @PostConstruct
    public void initializeType() {
        atomicType.set(type);
    }

    /**
     * 题外话：因为代码量太多，本来想把前面抽出成单独的方法，但question的judge case需要获得，request需要获得
     * 最后还是先放回一块了
     */
    @Override
    public boolean doJudge(long questionSubmitId) {
        //1 传入题目的提交 id，在数据库中获取到对应的题目、提交信息（包含代码、编程语言等）
        QuestionSubmit questionSubmit = questionSubmitService.getQuestionSubmitById(questionSubmitId);
        if (questionSubmit == null) {
            //todo:测试的时候为什么有时候 提交信息不存在 TAT
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "题目提交信息不存在！");
        }
        String language = questionSubmit.getLanguage();
        String code = questionSubmit.getCode();
        Long questionId = questionSubmit.getQuestionId();
        Integer status = questionSubmit.getStatus();

        //如果题目提交状态不为等待中，就不用重复执行了
        if (status.equals(QuestionSubmitStatusEnum.RUNNING.getValue())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "正在判题中，请勿重复提交！");
        }

        //查找题目信息
        Question question = questionService.getById(questionId);
        if (question == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "题目信息不存在！");
        }
        String judgeCase = question.getJudgeCase();
        List<JudgeCase> judgeCaseList = JSONUtil.toList(judgeCase, JudgeCase.class);
        //stream 真好用，爱来自拆那，下次还用stream
        List<String> inputList = judgeCaseList.stream().
                map(JudgeCase::getInput).collect(Collectors.toList());


        //2 更改判题（题目提交）的状态为 “判题中”，防止重复执行，也能让用户即时看到状态
        questionSubmit.setStatus(QuestionSubmitStatusEnum.RUNNING.getValue());
        boolean b = questionSubmitService.updateById(questionSubmit);
        if (!b) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新题目提交状态失败！");
        }


        //3 调用沙箱，获取到执行结果
        ExecuteCodeRequest request = ExecuteCodeRequest.builder().
                code(code).
                language(language).
                inputList(inputList).
                build();
        CodeSandbox codeSandbox = codeSandboxFactory.getInstanceWithType(atomicType.get());
        ExecuteCodeResponse executeCodeResponse;
        try {
            log.info("----调用沙箱，获取到执行结果-----");
            executeCodeResponse = new CodeSandboxProxy(codeSandbox).
                    executeCode(request);
        } catch (Exception e) {
            questionSubmit.setStatus(QuestionSubmitStatusEnum.ERROR.getValue());
            questionSubmitService.updateById(questionSubmit);
            return false;
        }

        //4 根据沙箱的执行结果，设置题目的判题状态和信息
        Integer resStatus = executeCodeResponse.getStatus();
        if (!resStatus.equals(RUN_SUCCESS)) {
            //说明没有正常退出，但是得到执行结果了？ 说明流程还是走完了的
            questionSubmit = new QuestionSubmit();
            questionSubmit.setId(questionSubmitId);
            questionSubmit.setStatus(QuestionSubmitStatusEnum.FAILED.getValue());
            questionSubmit.setJudgeInfo(JSONUtil.toJsonStr(executeCodeResponse.getJudgeInfo()));
            b = questionSubmitService.updateById(questionSubmit);
            if (!b) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "题目状态更新错误");
            }
            return true;
        }


        //根据语言不同。[判题]标准应该不一样 ---> 策略模式
        JudgeConfig judgeConfig = JSONUtil.toBean(question.getJudgeConfig(), JudgeConfig.class);
        //JudgeContext
        JudgeContext judgeContext = new JudgeContext();
        judgeContext.setJudgeInfo(executeCodeResponse.getJudgeInfo());
        judgeContext.setOutputList(executeCodeResponse.getOutputList());
        judgeContext.setJudgeCaseList(judgeCaseList);
        judgeContext.setJudgeConfig(judgeConfig);
        judgeContext.setLanguage(questionSubmit.getLanguage());
        //策略模式，交给他去判题
        JudgeInfo judgeInfoRes = judgeManager.doJudge(judgeContext);

        // 5）修改数据库中的判题结果
        questionSubmit = new QuestionSubmit();
        questionSubmit.setId(questionSubmitId);
        questionSubmit.setJudgeInfo(JSONUtil.toJsonStr(judgeInfoRes));
        if (judgeInfoRes.getMessage().equals(JudgeInfoMessageEnum.ACCEPTED.getValue())) {
            log.info("该题AC了");
            //ac了才算成功
            questionSubmit.setStatus(QuestionSubmitStatusEnum.SUCCESS.getValue());
            questionMessageProducer.sendMessage(String.valueOf(questionSubmitId));
        } else {
            log.info("该题有错误");
            questionSubmit.setStatus(QuestionSubmitStatusEnum.FAILED.getValue());
        }

        b = questionSubmitService.updateById(questionSubmit);
        if (!b) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "题目提交状态更新错误");
        }

        return true;
    }

    @Override
    public QuestionDebugResponse doDebug(QuestionDebugRequest questionDebugRequest) {
        String language = questionDebugRequest.getLanguage();
        String code = questionDebugRequest.getCode();
        String input = questionDebugRequest.getInput();
        QuestionDebugResponse questionDebugResponse = new QuestionDebugResponse();


        //调用沙箱，获取到执行结果
        ExecuteCodeRequest request = ExecuteCodeRequest.builder().
                code(code).
                language(language).
                inputList(Collections.singletonList(input)).
                build();
        CodeSandbox codeSandbox = codeSandboxFactory.getInstanceWithType(atomicType.get());
        ExecuteCodeResponse executeCodeResponse;
        try {
            log.info("----调用沙箱，获取到执行结果-----");
            executeCodeResponse = new CodeSandboxProxy(codeSandbox).
                    executeCode(request);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, e.getMessage());
            //            JudgeInfo judgeInfo = new JudgeInfo();
            //            judgeInfo.setMessage("系统错误，执行出错，请稍后再试。");
            //            questionDebugResponse.setJudgeInfo(judgeInfo);
            //            return questionDebugResponse;
        }

        //根据沙箱的执行结果，设置题目的判题状态和信息
        Integer resStatus = executeCodeResponse.getStatus();
        if (!resStatus.equals(RUN_SUCCESS)) {
            //说明没有正常退出，但是得到执行结果了？ 说明流程还是走完了的
            log.info("沙箱没有正常退出，但是得到执行结果,如编译错误，运行时错误");
            questionDebugResponse.setIsSuccess(false);
        } else {
            questionDebugResponse.setIsSuccess(true);
        }

        if (executeCodeResponse.getOutputList() != null &&
                !executeCodeResponse.getOutputList().isEmpty()) {
            //可能成功执行但是也没有输出任何东西~
            questionDebugResponse.setOutput(executeCodeResponse.getOutputList().get(0));
        }
        questionDebugResponse.setJudgeInfo(executeCodeResponse.getJudgeInfo());
        return questionDebugResponse;
    }

    @Override
    public void setType(String newType) {
        //防止并发问题 CAS
        if (!atomicType.compareAndSet(this.atomicType.get(), newType)) {
            // 可能需要进行重试逻辑，但我不，抛出异常
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "修改沙箱类型失败！");
        }
        log.info("sandbox 类型发生变换:{}", newType);
    }

    @Override
    public String getType() {
        return atomicType.get();
    }

}
