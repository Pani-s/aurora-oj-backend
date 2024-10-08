package com.pani.oj.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pani.oj.common.ErrorCode;
import com.pani.oj.constant.CommonConstant;
import com.pani.oj.constant.JudgeConstant;
import com.pani.oj.exception.BusinessException;
import com.pani.oj.exception.ThrowUtils;
import com.pani.oj.judge.JudgeService;
import com.pani.oj.mapper.QuestionSubmitMapper;
import com.pani.oj.model.dto.questionsubmit.QuestionDebugRequest;
import com.pani.oj.model.dto.questionsubmit.QuestionSubmitAddRequest;
import com.pani.oj.model.dto.questionsubmit.QuestionSubmitQueryRequest;
import com.pani.oj.model.entity.Question;
import com.pani.oj.model.entity.QuestionSubmit;
import com.pani.oj.model.entity.User;
import com.pani.oj.model.enums.QuestionSubmitLanguageEnum;
import com.pani.oj.model.enums.QuestionSubmitStatusEnum;
import com.pani.oj.model.vo.QuestionDebugResponse;
import com.pani.oj.model.vo.QuestionSubmitVO;
import com.pani.oj.mq.rabbitmq.JudgeMessageProducer;
import com.pani.oj.service.QuestionService;
import com.pani.oj.service.QuestionSubmitService;
import com.pani.oj.service.UserService;
import com.pani.oj.utils.SqlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * @author Pani
 * @description 针对表【question_submit(题目提交)】的数据库操作Service实现
 * @createDate 2024-03-06 12:30:40
 */
@Slf4j
@Service
public class QuestionSubmitServiceImpl extends ServiceImpl<QuestionSubmitMapper, QuestionSubmit>
        implements QuestionSubmitService {

    @Resource
    @Lazy
    private QuestionService questionService;

    @Resource
    private UserService userService;

    @Resource
    @Lazy
    private JudgeService judgeService;

    @Resource
    private JudgeMessageProducer judgeMessageProducer;


    @Transactional(rollbackFor = RuntimeException.class)
    @Override
    public long doQuestionSubmit(QuestionSubmitAddRequest questionSubmitAddRequest, User loginUser) {
        // 校验编程语言是否合法
        String language = questionSubmitAddRequest.getLanguage();
        QuestionSubmitLanguageEnum languageEnum = QuestionSubmitLanguageEnum.getEnumByValue(language);
        if (languageEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "编程语言错误");
        }
        //校检长度
        int length = questionSubmitAddRequest.getCode().length();
        if (length > JudgeConstant.MAX_CODE_LEN) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户提交代码长度超出限制");
        }

        long questionId = questionSubmitAddRequest.getQuestionId();
        // 判断实体是否存在，根据类别获取实体
        Question question = questionService.getById(questionId);
        if (question == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "对应的题目不存在");
        }

        long userId = loginUser.getId();
        QuestionSubmit questionSubmit = new QuestionSubmit();
        questionSubmit.setUserId(userId);
        questionSubmit.setQuestionId(questionId);
        questionSubmit.setCode(questionSubmitAddRequest.getCode());
        questionSubmit.setLanguage(language);
        // 设置初始状态，等待判题中
        questionSubmit.setStatus(QuestionSubmitStatusEnum.WAITING.getValue());
        questionSubmit.setJudgeInfo("{}");
        boolean save = this.save(questionSubmit);
        if (!save) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "题目提交数据插入失败");
        }
        Long questionSubmitId = questionSubmit.getId();
        log.info("题目提交信息初始化完成并存入数据库:{}", questionSubmit);

        //题目提交数+1
        CompletableFuture.runAsync(() -> {
            UpdateWrapper<Question> questionUpdateWrapper = new UpdateWrapper<>();
            questionUpdateWrapper.eq("id", questionId);
            questionUpdateWrapper.setSql("submitNum = submitNum + 1");
            boolean update = questionService.update(questionUpdateWrapper);
            if (!update) {
                log.error("数据库 - 题目提交数+1 失败：{}", questionId);
            }
        });


        //消息队列
        //        log.info("题目提交信息发送至消息队列，id是:{}",questionSubmitId);
        //        judgeMessageProducer.sendMessage(String.valueOf(questionSubmitId));

        //异步执行判题服务
        CompletableFuture.runAsync(() -> {
            log.info("执行判题服务:{}",questionSubmitId);
            judgeService.doJudge(questionSubmitId);
        });

        return questionSubmitId;
    }

    @Override
    public QuestionDebugResponse doQuestionDebug(QuestionDebugRequest questionDebugRequest) {
        int length = questionDebugRequest.getCode().length();
        if (length > JudgeConstant.MAX_CODE_LEN) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户提交代码长度超出限制");
        }
        QuestionDebugResponse questionDebugResponse = judgeService.doDebug(questionDebugRequest);
        //todo : 解决一下远程调用后抛出的异常怎么接的问题。。
        if (questionDebugResponse.getJudgeInfo() == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
        return questionDebugResponse;
    }

    @Override
    public QueryWrapper<QuestionSubmit> getQueryWrapper(QuestionSubmitQueryRequest questionSubmitQueryRequest) {
        QueryWrapper<QuestionSubmit> queryWrapper = new QueryWrapper<>();
        if (questionSubmitQueryRequest == null) {
            return queryWrapper;
        }
        String language = questionSubmitQueryRequest.getLanguage();
        Long questionId = questionSubmitQueryRequest.getQuestionId();
        Integer status = questionSubmitQueryRequest.getStatus();
        Long userId = questionSubmitQueryRequest.getUserId();
        //        String sortField = questionSubmitQueryRequest.getSortField();
        String sortField = "updateTime";
        String sortOrder = questionSubmitQueryRequest.getSortOrder();

        // 拼接查询条件
        queryWrapper.eq(ObjectUtils.isNotEmpty(questionId), "questionId", questionId);
        queryWrapper.eq(ObjectUtils.isNotEmpty(language), "language", language);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq(QuestionSubmitStatusEnum.getEnumByValue(status) != null,
                "status", status);
        queryWrapper.eq("isDelete", false);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

    @Override
    public QuestionSubmitVO getQuestionSubmitVO(QuestionSubmit questionSubmit, User loginUser) {
        QuestionSubmitVO questionSubmitVO = QuestionSubmitVO.objToVo(questionSubmit);
        // 脱敏：仅【本人和管理员】能看见自己（提交 userId 和登录用户 id 不同）提交的代码
        // todo：现在感觉又可以看，不过这个方法目前没人用
        // 1. 关联查询用户信息
        Long userId1 = loginUser.getId();
        if (!userId1.equals(questionSubmit.getUserId()) && !userService.isAdmin(loginUser)) {
            questionSubmitVO.setCode(null);
        }
        //        Long userId = questionSubmit.getUserId();
        //        User user = null;
        //        if (userId != null && userId > 0) {
        //            user = userService.getById(userId);
        //        }
        //        UserVO userVO = userService.getUserVO(user);
        //        questionSubmitVO.setUserVO(userVO);
        //返回
        return questionSubmitVO;
    }

    @Override
    public Page<QuestionSubmitVO> getQuestionSubmitVOPage(Page<QuestionSubmit> questionSubmitPage, User loginUser) {
        List<QuestionSubmit> questionSubmitList = questionSubmitPage.getRecords();
        Page<QuestionSubmitVO> questionVOPage = new Page<>(questionSubmitPage.getCurrent(),
                questionSubmitPage.getSize(), questionSubmitPage.getTotal());
        if (CollectionUtils.isEmpty(questionSubmitList)) {
            return questionVOPage;
        }

        /*
        batch查比单独查性能高很多 嗯嗯\(^_^)/
         */
        // 关联查询用户信息
        Set<Long> userIdSet = questionSubmitList.stream().map(QuestionSubmit::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));

        // 填充信息
        List<QuestionSubmitVO> questionSubmitVOList = questionSubmitList.stream().map(questionSubmit -> {
            QuestionSubmitVO questionSubmitVO = QuestionSubmitVO.objToVo(questionSubmit);
            Long userId = questionSubmit.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            questionSubmitVO.setUserVO(userService.getUserVO(user));
            return questionSubmitVO;
        }).collect(Collectors.toList());

        questionVOPage.setRecords(questionSubmitVOList);
        return questionVOPage;
    }

    @Override
    public boolean setQuestionSubmitFailure(long questionSubmitId) {
        QuestionSubmit questionSubmit = new QuestionSubmit();
        questionSubmit.setId(questionSubmitId);
        questionSubmit.setStatus(QuestionSubmitStatusEnum.FAILED.getValue());
        return this.updateById(questionSubmit);
    }

    @Override
    public void checkErrorQuestion(long questionSubmitId, HttpServletRequest request) {
        //检查用户在不在
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        //查找questionSubmit
        QuestionSubmit questionSubmit = this.getById(questionSubmitId);
        ThrowUtils.throwIf(questionSubmit == null, ErrorCode.NOT_FOUND_ERROR, "题目提交信息不存在");
        if (!questionSubmit.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "该题目提交信息不属于该用户！");
        }
        if (!questionSubmit.getStatus().equals(QuestionSubmitStatusEnum.ERROR.getValue())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "该题目提交信息非ERROR状态");
        }
    }

    @Override
    public boolean retryMyErrorSubmit(long questionSubmitId, HttpServletRequest request) {
        checkErrorQuestion(questionSubmitId, request);
        //消息队列
        log.info("Retry - 题目提交信息发送至消息队列，id是:{}", questionSubmitId);
        judgeMessageProducer.sendMessage(String.valueOf(questionSubmitId));
        return true;
    }

    @Override
    public QuestionSubmit getQuestionSubmitById(long questionSubmitId) {
        log.info("getQuestionSubmitById：{}", questionSubmitId);
        QuestionSubmit questionSubmit = this.getById(questionSubmitId);
        if (questionSubmit == null) {
            log.info("查到空，悲伤，questionSubmit为null，手动再试一下");
            //手动再试一下
            questionSubmit = this.getById(questionSubmitId);
        }
        log.info("之后questionSubmit：{}", questionSubmit);
        return questionSubmit;
    }
}




