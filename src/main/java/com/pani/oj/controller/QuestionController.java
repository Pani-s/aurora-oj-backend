package com.pani.oj.controller;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pani.oj.annotation.AuthCheck;
import com.pani.oj.common.BaseResponse;
import com.pani.oj.common.DeleteRequest;
import com.pani.oj.common.ErrorCode;
import com.pani.oj.common.ResultUtils;
import com.pani.oj.constant.UserConstant;
import com.pani.oj.exception.BusinessException;
import com.pani.oj.exception.ThrowUtils;
import com.pani.oj.model.dto.question.*;
import com.pani.oj.model.entity.Question;
import com.pani.oj.model.entity.User;
import com.pani.oj.model.vo.QuestionVO;
import com.pani.oj.model.vo.Rank;
import com.pani.oj.service.QuestionService;
import com.pani.oj.service.UserService;
import com.pani.oj.service.UserSubmitService;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 题目接口
 *
 * @author pani
 *
 * getmapping遇到很多坑，还是回来用postmapping吧
 * 
 */
@RestController
@RequestMapping("/question")
@Slf4j
public class QuestionController {

    @Resource
    private QuestionService questionService;

    @Resource
    private UserService userService;

    @Resource
    private UserSubmitService userSubmitService;

    // region 增删改查

    /**
     * 创建
     *
     */
    @PostMapping("/add")
    @ApiOperation("创建题目")
    public BaseResponse<Long> addQuestion(@RequestBody QuestionAddRequest questionAddRequest, HttpServletRequest request) {
        if (questionAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Question question = new Question();
        BeanUtils.copyProperties(questionAddRequest, question);
        List<String> tags = questionAddRequest.getTags();
        if (tags != null) {
            question.setTags(JSONUtil.toJsonStr(tags));
        }
        List<JudgeCase> judgeCase = questionAddRequest.getJudgeCase();
        if (judgeCase != null) {
            question.setJudgeCase(JSONUtil.toJsonStr(judgeCase));
        }
        JudgeConfig judgeConfig = questionAddRequest.getJudgeConfig();
        if (judgeConfig != null) {
            question.setJudgeConfig(JSONUtil.toJsonStr(judgeConfig));
        }
        questionService.validQuestion(question, true);
        User loginUser = userService.getLoginUser(request);
        question.setUserId(loginUser.getId());
        boolean result = questionService.save(question);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        long newQuestionId = question.getId();
        return ResultUtils.success(newQuestionId);
    }

    /**
     * 删除 （仅本人或管理员）
     *
     */
    @PostMapping("/delete")
    @ApiOperation("delete题目")
    public BaseResponse<Boolean> deleteQuestion(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        // 判断是否存在
        Question oldQuestion = questionService.getById(id);
        ThrowUtils.throwIf(oldQuestion == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!oldQuestion.getUserId().equals(user.getId()) && !userService.isAdmin(user)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean b = questionService.removeById(id);
        return ResultUtils.success(b);
    }

    /**
     * 前端用的是update
     * 更新（仅管理员）
     *
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @ApiOperation("update题目")
    public BaseResponse<Boolean> updateQuestion(@RequestBody QuestionUpdateRequest questionUpdateRequest) {
        if (questionUpdateRequest == null || questionUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Boolean b = questionService.updateQuestion(questionUpdateRequest);
        return ResultUtils.success(b);
    }

    /**
     * 根据 id 获取 题目内容（只能管理员或者本人）
     *
     */
    @GetMapping("/get")
    @ApiOperation("根据id获取题目内容（只能管理员或者本人）")
    public BaseResponse<Question> getQuestionById(long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Question question = questionService.getById(id);
        if (question == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        // 不是本人或管理员，不能直接获取所有信息
        if (!question.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        return ResultUtils.success(question);
    }


    /**
     * 根据 id 获取 题目VO（脱敏后）
     *
     */
    @GetMapping("/get/vo")
    @ApiOperation("根据id获取题目脱敏")
    public BaseResponse<QuestionVO> getQuestionVOById(long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        return ResultUtils.success(questionService.getQuestionVOById(id));
    }

    /**
     * 根据 id 获取 答案，前提是这个用户做过
     *
     */
    @GetMapping("/get/answer")
    @ApiOperation("根据id获取答案")
    public BaseResponse<Question> getQuestionAnswerById(long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        return ResultUtils.success(questionService.getQuestionAnswerById(id,request));
    }

    /**
     * 分页获取列表（封装类）
     *QuestionVO中的UserVO没有填充
     */
    @PostMapping("/list/page/vo")
    @ApiOperation("分页获取题目列表")
    public BaseResponse<Page<QuestionVO>> listQuestionVOByPage(@RequestBody QuestionQueryRequest questionQueryRequest,
                                                               HttpServletRequest request) {
        /*
        其实题目列表缓存。。。是否需要说题目数更新了之后就删除缓存呢？不过题目信息的更新需要删除缓存
         */
        ThrowUtils.throwIf(questionQueryRequest == null,ErrorCode.PARAMS_ERROR);
        //不支持以答案和内容查找
        questionQueryRequest.setAnswer(null);
        questionQueryRequest.setContent(null);
        return ResultUtils.success(questionService.listQuestionVOByPage(questionQueryRequest,request));
    }

    /**
     * 分页获取当前用户创建的问题列表
     *
     */
    @PostMapping("/my/list/page/vo")
    @ApiOperation("分页获取当前用户创建的问题列表")
    public BaseResponse<Page<QuestionVO>> listMyQuestionVOByPage(@RequestBody QuestionQueryRequest questionQueryRequest,
                                                                 HttpServletRequest request) {
        if (questionQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        questionQueryRequest.setUserId(loginUser.getId());
        long current = questionQueryRequest.getCurrent();
        long size = questionQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Question> questionPage = questionService.page(new Page<>(current, size),
                questionService.getQueryWrapper(questionQueryRequest));
        return ResultUtils.success(questionService.getMyQuestionVOPage(questionPage, request));
    }

    /**
     * 分页获取题目列表（仅管理员）
     *
     */
    @PostMapping("/list/page")
    @ApiOperation("分页获取题目列表（仅管理员）")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Question>> listQuestionByPage(@RequestBody QuestionQueryRequest questionQueryRequest,
                                                           HttpServletRequest request) {
        long current = questionQueryRequest.getCurrent();
        long size = questionQueryRequest.getPageSize();
        Page<Question> questionPage = questionService.page(new Page<>(current, size),
                questionService.getQueryWrapper(questionQueryRequest));
        return ResultUtils.success(questionPage);
    }

    /**
     * 编辑题目的信息（用户）
     *
     */
    @PostMapping("/edit")
    @ApiOperation("编辑题目的信息（用户）")
    public BaseResponse<Boolean> editQuestion(@RequestBody QuestionEditRequest questionEditRequest, HttpServletRequest request) {
        if (questionEditRequest == null || questionEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Boolean b = questionService.editQuestion(questionEditRequest, request);
        return ResultUtils.success(b);
    }

    // endregion


    //region rank
    /**
     * 获取每日排行榜 -- 新通过数
     */
    @GetMapping("/rank/daily/new")
    public BaseResponse<List<Rank>> getDailyRankNewPass() {
        return ResultUtils.success(userSubmitService.getDailyRankNewPass());
    }
    //endregion
}
