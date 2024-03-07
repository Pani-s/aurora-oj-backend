package com.pani.oj.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pani.oj.common.BaseResponse;
import com.pani.oj.common.ErrorCode;
import com.pani.oj.common.ResultUtils;
import com.pani.oj.exception.BusinessException;
import com.pani.oj.model.dto.questionsubmit.QuestionSubmitAddRequest;
import com.pani.oj.model.dto.questionsubmit.QuestionSubmitQueryRequest;
import com.pani.oj.model.entity.QuestionSubmit;
import com.pani.oj.model.entity.User;
import com.pani.oj.model.vo.QuestionSubmitVO;
import com.pani.oj.model.vo.QuestionVO;
import com.pani.oj.service.QuestionService;
import com.pani.oj.service.QuestionSubmitService;
import com.pani.oj.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 帖子收藏接口
 *
 * @author pani
 * 
 */
@RestController
@RequestMapping("/question_submit")
@Slf4j
public class QuestionSubmitController {

    @Resource
    private QuestionSubmitService questionSubmitService;

    @Resource
    private QuestionService questionService;

    @Resource
    private UserService userService;

    /**
     * 提交题目
     * @return id
     */
    @PostMapping("/")
    public BaseResponse<Long> doQuestionSubmit(@RequestBody QuestionSubmitAddRequest questionSubmitAddRequest,
            HttpServletRequest request) {
        if (questionSubmitAddRequest == null || questionSubmitAddRequest.getQuestionId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 登录才能操作
        final User loginUser = userService.getLoginUser(request);
        long questionSubmitId = questionSubmitService.doQuestionSubmit(questionSubmitAddRequest, loginUser);
        return ResultUtils.success(questionSubmitId);
    }


    /**
     * 分页获取题目提交列表（除了管理员外，普通用户只能看到非答案、提交代码等公开信息）
     * todo: 思考ing 要不要弄个获取本人提交的题目的接口
     * @param questionSubmitQueryRequest
     * @param request
     */
    @PostMapping("/list/page")
    public BaseResponse<Page<QuestionSubmitVO>> listFavourQuestionByPage(@RequestBody QuestionSubmitQueryRequest questionSubmitQueryRequest,
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
}
