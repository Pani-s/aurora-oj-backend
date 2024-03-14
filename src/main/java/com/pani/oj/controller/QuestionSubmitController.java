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
import com.pani.oj.service.QuestionService;
import com.pani.oj.service.QuestionSubmitService;
import com.pani.oj.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 帖子收藏接口
 *
 * @author pani
 * 
 */
//@RestController
//@RequestMapping("/question_submit")
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
    @PostMapping("/do")
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
     * 该题目的提交列表---可以看代码，目前不能看提交用户名，我觉得还是匿名吧
//     * todo: 思考ing 要不要弄个获取本人提交的题目的接口
     * @param questionSubmitQueryRequest
     * @param request
     */
    @GetMapping("/list/page")
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
     * @param questionSubmitQueryRequest
     * @param request
     */
    @GetMapping("/list/my/page")
    public BaseResponse<Page<QuestionSubmit>> listMySubmitQuestionByPage(@RequestBody QuestionSubmitQueryRequest questionSubmitQueryRequest,
                                                                         HttpServletRequest request) {
        if (questionSubmitQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        if(!loginUser.getId().equals(questionSubmitQueryRequest.getUserId())){
            //不是查询的本人
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

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
     * @param questionSubmitQueryRequest
     * @param request
     */
    @GetMapping("/list/all/page")
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
}
