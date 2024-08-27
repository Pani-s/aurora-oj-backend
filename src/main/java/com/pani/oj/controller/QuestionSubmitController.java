package com.pani.oj.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pani.oj.common.BaseResponse;
import com.pani.oj.common.DeleteRequest;
import com.pani.oj.common.ErrorCode;
import com.pani.oj.common.ResultUtils;
import com.pani.oj.constant.RedisConstant;
import com.pani.oj.exception.BusinessException;
import com.pani.oj.manager.RedisLimiterManager;
import com.pani.oj.model.dto.questionsubmit.QuestionDebugRequest;
import com.pani.oj.model.dto.questionsubmit.QuestionSubmitAddRequest;
import com.pani.oj.model.dto.questionsubmit.QuestionSubmitQueryRequest;
import com.pani.oj.model.entity.QuestionSubmit;
import com.pani.oj.model.entity.User;
import com.pani.oj.model.vo.QuestionDebugResponse;
import com.pani.oj.model.vo.QuestionSubmitVO;
import com.pani.oj.service.QuestionSubmitService;
import com.pani.oj.service.UserService;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 *
 * @author pani
 * 
 */
@RestController
@RequestMapping("/question_submit")
@Slf4j
public class QuestionSubmitController {

    @Resource
    private RedisLimiterManager redisLimiterManager;

    @Resource
    private QuestionSubmitService questionSubmitService;

    @Resource
    private UserService userService;

    //region 题目提交
    /**
     * 提交题目---异步
     * @return id
     */
    @PostMapping("/submit/do")
    @ApiOperation("提交题目---异步")
    public BaseResponse<Long> doQuestionSubmit(@RequestBody QuestionSubmitAddRequest questionSubmitAddRequest,
                                               HttpServletRequest request) {
        if (questionSubmitAddRequest == null || questionSubmitAddRequest.getQuestionId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 登录才能操作
        final User loginUser = userService.getLoginUser(request);
        //判定限流
        redisLimiterManager.doRateLimit(RedisConstant.QUESTION_SUBMIT_LIMIT + loginUser.getId());
        long questionSubmitId = questionSubmitService.doQuestionSubmit(questionSubmitAddRequest, loginUser);
        return ResultUtils.success(questionSubmitId);
    }

    /**
     * debug测试---同步
     * @return id
     */
    @PostMapping("/submit/debug")
    @ApiOperation("debug测试---同步")
    public BaseResponse<QuestionDebugResponse> doQuestionSubmitDebug(@RequestBody QuestionDebugRequest questionDebugRequest,
                                                                     HttpServletRequest request) {
        if (questionDebugRequest == null || questionDebugRequest.getQuestionId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 登录才能操作
        final User loginUser = userService.getLoginUser(request);
        //判定限流
        redisLimiterManager.doRateLimit(RedisConstant.QUESTION_DEBUG_LIMIT + loginUser.getId());
        QuestionDebugResponse questionDebugResponse = questionSubmitService.doQuestionDebug(questionDebugRequest);
        return ResultUtils.success(questionDebugResponse);
    }

    /**
     * 分页获取题目提交列表（除了管理员外，普通用户只能看到非答案、提交代码等公开信息）
     * 该题目的提交列表---可以看代码，目前不能看提交用户名，我觉得还是匿名吧
     */
    @PostMapping("/submit/list/page")
    @ApiOperation("debug测试---同步")
    public BaseResponse<Page<QuestionSubmitVO>> listSubmitQuestionByPage(@RequestBody QuestionSubmitQueryRequest questionSubmitQueryRequest,
                                                                         HttpServletRequest request) {
        if (questionSubmitQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long current = questionSubmitQueryRequest.getCurrent();
        long size = questionSubmitQueryRequest.getPageSize();
        // 从数据库中查询原始的题目提交分页信息
        Page<QuestionSubmit> questionSubmitPage = questionSubmitService.page(new Page<>(current, size),
                questionSubmitService.getQueryWrapper(questionSubmitQueryRequest));
        final User loginUser = userService.getLoginUser(request);
        // 返回脱敏信息
        return ResultUtils.success(questionSubmitService.getQuestionSubmitVOPage(questionSubmitPage, loginUser));
    }

    /**
     * 获取本人提交的题目
     */
    @PostMapping("/submit/list/my/page")
    @ApiOperation("获取本人提交的题目")
    public BaseResponse<Page<QuestionSubmit>> listMySubmitQuestionByPage(@RequestBody QuestionSubmitQueryRequest questionSubmitQueryRequest,
                                                                         HttpServletRequest request) {
        if (questionSubmitQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        questionSubmitQueryRequest.setUserId(loginUser.getId());

        long current = questionSubmitQueryRequest.getCurrent();
        long size = questionSubmitQueryRequest.getPageSize();
        // 从数据库中查询原始的题目提交分页信息
        Page<QuestionSubmit> questionSubmitPage = questionSubmitService.page(new Page<>(current, size),
                questionSubmitService.getQueryWrapper(questionSubmitQueryRequest));
        // 返回脱敏信息
        return ResultUtils.success(questionSubmitPage);
    }

    /**
     * 所有人的所有题目提交记录，按照时间排序(【【公屏】】，所以只能看到【提交用户id（甚至匿名） 语言)
     */
    @PostMapping("/submit/list/all/page")
    @ApiOperation("所有人的所有题目提交记录")
    public BaseResponse<Page<QuestionSubmit>> listAllSubmitQuestionByPage(@RequestBody QuestionSubmitQueryRequest questionSubmitQueryRequest,
                                                                          HttpServletRequest request) {
        if (questionSubmitQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long current = questionSubmitQueryRequest.getCurrent();
        long size = questionSubmitQueryRequest.getPageSize();
        // 从数据库中查询原始的题目提交分页信息
        Page<QuestionSubmit> questionSubmitPage = questionSubmitService.page(new Page<>(current, size),
                questionSubmitService.getQueryWrapper(questionSubmitQueryRequest));
        //提交代码不给看了
        questionSubmitPage.getRecords().forEach((o) -> o.setCode(null));
        return ResultUtils.success(questionSubmitPage);
    }

    /**
     * 删除修改失败的提交
     */
    @PostMapping("/submit/delete")
    @ApiOperation("删除修改失败的提交")
    public BaseResponse<Boolean> delMyErrorSubmit(@RequestBody DeleteRequest deleteRequest,
                                                  HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        questionSubmitService.checkErrorQuestion(deleteRequest.getId(),request);
        //删除
        boolean b = questionSubmitService.removeById(deleteRequest.getId());
        if(!b){
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"删除失败！");
        }
        return ResultUtils.success(b);
    }

    /**
     * 重试修改失败的提交
     */
    @PostMapping("/submit/retry")
    @ApiOperation("重试修改失败的提交")
    public BaseResponse<Boolean> retryMyErrorSubmit(@RequestBody DeleteRequest deleteRequest,
                                                    HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean b = questionSubmitService.retryMyErrorSubmit(deleteRequest.getId(), request);
        return ResultUtils.success(b);
    }
    //endregion


}
