package com.pani.oj.service.impl;


import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pani.oj.common.ErrorCode;
import com.pani.oj.constant.CommonConstant;
import com.pani.oj.constant.RedisConstant;
import com.pani.oj.exception.BusinessException;
import com.pani.oj.exception.ThrowUtils;
import com.pani.oj.manager.CacheClient;
import com.pani.oj.mapper.QuestionMapper;
import com.pani.oj.model.dto.question.*;
import com.pani.oj.model.entity.Question;
import com.pani.oj.model.entity.QuestionSubmit;
import com.pani.oj.model.entity.User;
import com.pani.oj.model.vo.QuestionVO;
import com.pani.oj.model.vo.UserVO;
import com.pani.oj.service.QuestionService;
import com.pani.oj.service.QuestionSubmitService;
import com.pani.oj.service.UserService;
import com.pani.oj.service.UserSubmitService;
import com.pani.oj.utils.SqlUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author Pani
 * @description 针对表【question(题目)】的数据库操作Service实现
 * @createDate 2024-03-06 12:30:26
 */
@Service
public class QuestionServiceImpl extends ServiceImpl<QuestionMapper, Question>
        implements QuestionService {
    @Resource
    private CacheClient cacheClient;
    @Resource
    private UserService userService;
    @Resource
    private UserSubmitService userSubmitService;

    @Resource
    @Lazy
    private QuestionSubmitService questionSubmitService;

    //todo
    private final int TITLE_LENGTH_LIMIT = 80;
    private final int CONTENT_LENGTH_LIMIT = 8192;

    /**
     * 校验题目是否合法
     */
    @Override
    public void validQuestion(Question question, boolean add) {
        if (question == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String title = question.getTitle();
        String content = question.getContent();
        String tags = question.getTags();
        String answer = question.getAnswer();
        String judgeCase = question.getJudgeCase();
        String judgeConfig = question.getJudgeConfig();
        // 创建时，参数不能为空 ( 如果是修改，那可以)
        if (add) {
            ThrowUtils.throwIf(StringUtils.isAnyBlank(title, content, tags, answer),
                    ErrorCode.PARAMS_ERROR);
        }
        // 有参数则校验
        if (StringUtils.isNotBlank(title) && title.length() > TITLE_LENGTH_LIMIT) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "标题过长");
        }
        if (StringUtils.isNotBlank(content) && content.length() > CONTENT_LENGTH_LIMIT) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "内容过长");
        }
        if (StringUtils.isNotBlank(answer) && answer.length() > CONTENT_LENGTH_LIMIT) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "答案过长");
        }
        if (StringUtils.isNotBlank(judgeCase) && judgeCase.length() > CONTENT_LENGTH_LIMIT) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "判题用例过长");
        }
        if (StringUtils.isNotBlank(judgeConfig) && judgeConfig.length() > CONTENT_LENGTH_LIMIT) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "判题配置过长");
        }
    }

    @Override
    public Page<QuestionVO> listQuestionVOByPage(QuestionQueryRequest questionQueryRequest, HttpServletRequest request) {
        long current = questionQueryRequest.getCurrent();
        long size = questionQueryRequest.getPageSize();
        ThrowUtils.throwIf(current < 0 || size < 0, ErrorCode.PARAMS_ERROR);
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR, "请求参数过大");
        User loginUser = userService.getLoginUserPermitNull(request);

        //查询
        Page<Question> questionPage = this.page(new Page<>(current, size),
                this.getQueryWrapperWithoutContent(questionQueryRequest));
        Page<QuestionVO> questionVOPage = this.getQuestionVOPage(questionPage, loginUser);
        return questionVOPage;
    }


/*    @Override
    public Page<QuestionVO> listQuestionVOByPage(QuestionQueryRequest questionQueryRequest, HttpServletRequest request) {
        long current = questionQueryRequest.getCurrent();
        long size = questionQueryRequest.getPageSize();
        ThrowUtils.throwIf(current < 0 || size < 0, ErrorCode.PARAMS_ERROR);
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR, "请求参数过大");
        User loginUser = userFeignClient.getLoginUserMayNull(request);

        //如果没有任何搜索词，就可以走缓存。
        Long id = questionQueryRequest.getId();
        String title = questionQueryRequest.getTitle();
        String content = questionQueryRequest.getContent();
        List<String> tags = questionQueryRequest.getTags();
        if (id != null || !(StringUtils.isAllBlank(title, content)) || !(CollectionUtil.isEmpty(tags))) {
            //id不为空，搜索词不全空，tags不空，则不能走缓存
            Page<Question> questionPage = this.page(new Page<>(current, size),
                    this.getQueryWrapperWithoutContent(questionQueryRequest));
            return this.getQuestionVOPage(questionPage, loginUser);
        }

        String key = null;
        //缓存里拿
        if (loginUser == null) {
            key = RedisConstant.CACHE_QUESTION_PAGE + current + "-" + size;
            Page<QuestionVO> pageCache = cacheClient.getPageCache(key, QuestionVO.class);
            if (pageCache != null) {
                return pageCache;
            }

            Page<Question> questionPage = this.page(new Page<>(current, size),
                    this.getQueryWrapperWithoutContent(questionQueryRequest));
            Page<QuestionVO> questionVOPage = this.getQuestionVOPage(questionPage, null);
            cacheClient.set(key, questionVOPage, RedisConstant.CACHE_QUESTION_PAGE_TTL, TimeUnit.MINUTES);
            return questionVOPage;
        }
//        else {
//            key = RedisConstant.CACHE_QUESTION_PAGE + current + "-" + size + "-" + loginUser.getId();
//        }


        //查询。放缓存
        Page<Question> questionPage = this.page(new Page<>(current, size),
                this.getQueryWrapperWithoutContent(questionQueryRequest));
        Page<QuestionVO> questionVOPage = this.getQuestionVOPage(questionPage, loginUser);
//        cacheClient.set(key, questionVOPage, RedisConstant.CACHE_QUESTION_PAGE_TTL, TimeUnit.MINUTES);
        return questionVOPage;
    }*/


    /**
     * 获取查询包装类 但是不要题目的详细信息
     *
     * @param questionQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<Question> getQueryWrapperWithoutContent(QuestionQueryRequest questionQueryRequest) {
        QueryWrapper<Question> queryWrapper = new QueryWrapper<>();
        if (questionQueryRequest == null) {
            return queryWrapper;
        }
        Long id = questionQueryRequest.getId();
        String title = questionQueryRequest.getTitle();
        List<String> tagList = questionQueryRequest.getTags();
        String sortField = questionQueryRequest.getSortField();
        String sortOrder = questionQueryRequest.getSortOrder();

        // 拼接查询条件
        queryWrapper.like(StringUtils.isNotBlank(title), "title", title);
        if (CollectionUtils.isNotEmpty(tagList)) {
            for (String tag : tagList) {
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }
        queryWrapper.eq(ObjectUtils.isNotEmpty(id), "id", id);
        queryWrapper.eq("isDelete", false);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        queryWrapper.select("id","title","tags","submitNum","acceptedNum");
        return queryWrapper;
    }

    /**
     * 获取查询包装类（用户根据哪些字段查询，根据前端传来的请求对象，得到 mybatis 框架支持的查询 QueryWrapper 类）
     *
     * @param questionQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<Question> getQueryWrapper(QuestionQueryRequest questionQueryRequest) {
        QueryWrapper<Question> queryWrapper = new QueryWrapper<>();
        if (questionQueryRequest == null) {
            return queryWrapper;
        }
        Long id = questionQueryRequest.getId();
        String title = questionQueryRequest.getTitle();
        String content = questionQueryRequest.getContent();
        List<String> tagList = questionQueryRequest.getTags();
        String answer = questionQueryRequest.getAnswer();
        Long userId = questionQueryRequest.getUserId();
        String sortField = questionQueryRequest.getSortField();
        String sortOrder = questionQueryRequest.getSortOrder();

        // 拼接查询条件
        queryWrapper.like(StringUtils.isNotBlank(title), "title", title);
        queryWrapper.like(StringUtils.isNotBlank(content), "content", content);
        queryWrapper.like(StringUtils.isNotBlank(answer), "answer", answer);
        if (CollectionUtils.isNotEmpty(tagList)) {
            for (String tag : tagList) {
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }
        queryWrapper.eq(ObjectUtils.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq("isDelete", false);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }


    //    @Override
    //    public QuestionVO getQuestionVO(Question question, HttpServletRequest request) {
    //        QuestionVO questionVO = QuestionVO.objToVo(question);
    //        // 1. 关联查询用户信息
    //        Long userId = question.getUserId();
    //        User user = null;
    //        if (userId != null && userId > 0) {
    //            user = userService.getById(userId);
    //        }
    //        UserVO userVO = userService.getUserVO(user);
    //        questionVO.setUserVO(userVO);
    //        //返回
    //        return questionVO;
    //    }

    @Override
    public QuestionVO getQuestionVO(Question question) {
        QuestionVO questionVO = QuestionVO.objToVo(question);
        // 1. 关联查询用户信息
        Long userId = question.getUserId();
        User user = null;
        if (userId != null && userId > 0) {
            user = userService.getById(userId);
        }
        UserVO userVO = userService.getUserVO(user);
        questionVO.setUserVO(userVO);
        //返回
        return questionVO;
    }

    @Override
    public QuestionVO getQuestionVOById(long id) {
        Question question = cacheClient.queryWithPassThrough(RedisConstant.CACHE_QUESTION, id, Question.class,
                this::getById, RedisConstant.CACHE_QUESTION_TTL, TimeUnit.MINUTES);
        if (question == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        //这里没装userVO，但是真感觉不需要
        //        return this.getQuestionVO(question);
        return QuestionVO.objToVo(question);
    }

    @Override
    public Page<QuestionVO> getMyQuestionVOPage(Page<Question> questionPage, HttpServletRequest request) {
        List<Question> questionList = questionPage.getRecords();
        Page<QuestionVO> questionVOPage = new Page<>(questionPage.getCurrent(), questionPage.getSize(), questionPage.getTotal());
        if (CollectionUtils.isEmpty(questionList)) {
            return questionVOPage;
        }
        // 1. 关联查询用户信息 【批查询，所以没有用 getQuestionVO
        Set<Long> userIdSet = questionList.stream().map(Question::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));

        // 填充信息
        List<QuestionVO> questionVOList = questionList.stream().map(question -> {
            QuestionVO questionVO = QuestionVO.objToVo(question);
            Long userId = question.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            questionVO.setUserVO(userService.getUserVO(user));
            return questionVO;
        }).collect(Collectors.toList());
        questionVOPage.setRecords(questionVOList);
        return questionVOPage;
    }

    @Override
    public Page<QuestionVO> getQuestionVOPage(Page<Question> questionPage, User user) {
        List<Question> questionList = questionPage.getRecords();
        Page<QuestionVO> questionVOPage = new Page<>(questionPage.getCurrent(), questionPage.getSize(), questionPage.getTotal());
        if (CollectionUtils.isEmpty(questionList)) {
            return questionVOPage;
        }
        // 填充信息
        //        List<QuestionVO> questionVOList = questionList.stream().map(QuestionVO::objToVo).collect(Collectors.toList());

        List<Long> questionIdList = questionList.stream().map(Question::getId).collect(Collectors.toList());
        Map<Long, Map<String,Long>> maps;
        if (user != null) {
            maps = userSubmitService.checkExistForQuestionIdsMapper(user.getId(), questionIdList);
        } else {
            maps = null;
        }
        if (maps != null && maps.size() == 1) {
            if (maps.containsKey(null)) {
                maps = null;
            }
        }

        Map<Long, Map<String,Long>> finalMaps = maps;
        List<QuestionVO> questionVOList = questionList.stream().map(question -> {
            QuestionVO questionVO = QuestionVO.objToVo(question);
            Long questionVOId = questionVO.getId();
            if (finalMaps != null && finalMaps.containsKey(questionVOId)) {
                Long i = finalMaps.get(questionVOId).get("count");
                questionVO.setIsPass(i != 0);
            }else{
                questionVO.setIsPass(false);
            }
            return questionVO;
        }).collect(Collectors.toList());
        questionVOPage.setRecords(questionVOList);
        return questionVOPage;
    }

    //region 改
    @Override
    public Boolean editQuestion(QuestionEditRequest questionEditRequest, HttpServletRequest request) {
        Question question = new Question();
        BeanUtils.copyProperties(questionEditRequest, question);
        List<String> tags = questionEditRequest.getTags();
        if (tags != null) {
            question.setTags(JSONUtil.toJsonStr(tags));
        }
        List<JudgeCase> judgeCase = questionEditRequest.getJudgeCase();
        if (judgeCase != null) {
            question.setJudgeCase(JSONUtil.toJsonStr(judgeCase));
        }
        JudgeConfig judgeConfig = questionEditRequest.getJudgeConfig();
        if (judgeConfig != null) {
            question.setJudgeConfig(JSONUtil.toJsonStr(judgeConfig));
        }
        // 参数校验
        this.validQuestion(question, false);
        User loginUser = userService.getLoginUser(request);
        long id = questionEditRequest.getId();
        // 判断是否存在
        Question oldQuestion = this.getById(id);
        ThrowUtils.throwIf(oldQuestion == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可编辑
        if (!oldQuestion.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean result = this.updateById(question);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
        return result;
    }

    @Override
    public Boolean updateQuestion(QuestionUpdateRequest questionUpdateRequest) {
        Question question = new Question();
        BeanUtils.copyProperties(questionUpdateRequest, question);
        List<String> tags = questionUpdateRequest.getTags();
        if (tags != null) {
            question.setTags(JSONUtil.toJsonStr(tags));
        }
        List<JudgeCase> judgeCase = questionUpdateRequest.getJudgeCase();
        if (judgeCase != null) {
            question.setJudgeCase(JSONUtil.toJsonStr(judgeCase));
        }
        JudgeConfig judgeConfig = questionUpdateRequest.getJudgeConfig();
        if (judgeConfig != null) {
            question.setJudgeConfig(JSONUtil.toJsonStr(judgeConfig));
        }
        // 参数校验
        this.validQuestion(question, false);
        long id = questionUpdateRequest.getId();
        // 判断是否存在
        Question oldQuestion = this.getById(id);
        ThrowUtils.throwIf(oldQuestion == null, ErrorCode.NOT_FOUND_ERROR);
        boolean result = this.updateById(question);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
        //删除缓存
        cacheClient.deleteKey(RedisConstant.CACHE_QUESTION + id);
//        cacheClient.deleteKeyWithPrefix(RedisConstant.CACHE_QUESTION_PAGE);
        return result;
    }

    @Override
    public Question getQuestionAnswerById(long questionId, HttpServletRequest request) {
        User loginUser = userService.getLoginUserPermitNull(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        //查询用户是否提交过该题
        Long userId = loginUser.getId();
        QueryWrapper<QuestionSubmit> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId).eq("questionId", questionId);
        long count = questionSubmitService.count(queryWrapper);
        if (count == 0) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "未提交过该题！");
        }
        //返回答案

        QueryWrapper<Question> questionQueryWrapper = new QueryWrapper<>();
        questionQueryWrapper.eq("id", questionId);
        questionQueryWrapper.select("id", "answer");
        Question question = this.getOne(questionQueryWrapper);
        if (question == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
        return question;
    }

    @Override
    public boolean incrAcNum(Long questionId) {
        UpdateWrapper<Question> questionUpdateWrapper = new UpdateWrapper<>();
        questionUpdateWrapper.eq("id", questionId);
        questionUpdateWrapper.setSql("acceptedNum = acceptedNum + 1");
        boolean update = this.update(questionUpdateWrapper);
        return update;
    }
    //endregion

}