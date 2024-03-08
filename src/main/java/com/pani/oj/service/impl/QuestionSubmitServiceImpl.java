package com.pani.oj.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pani.oj.common.ErrorCode;
import com.pani.oj.constant.CommonConstant;
import com.pani.oj.exception.BusinessException;
import com.pani.oj.judge.JudgeService;
import com.pani.oj.mapper.QuestionSubmitMapper;
import com.pani.oj.model.dto.questionsubmit.QuestionSubmitAddRequest;
import com.pani.oj.model.dto.questionsubmit.QuestionSubmitQueryRequest;
import com.pani.oj.model.entity.Question;
import com.pani.oj.model.entity.QuestionSubmit;
import com.pani.oj.model.entity.User;
import com.pani.oj.model.enums.QuestionSubmitLanguageEnum;
import com.pani.oj.model.enums.QuestionSubmitStatusEnum;
import com.pani.oj.model.vo.QuestionSubmitVO;
import com.pani.oj.service.QuestionService;
import com.pani.oj.service.QuestionSubmitService;
import com.pani.oj.service.UserService;
import com.pani.oj.utils.SqlUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
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
@Service
public class QuestionSubmitServiceImpl extends ServiceImpl<QuestionSubmitMapper, QuestionSubmit>
        implements QuestionSubmitService {

    @Resource
    private QuestionService questionService;

    @Resource
    private UserService userService;

    /*
    todo: 有循环依赖 加了@Lazy 还是想个办法解决一下
    https://blog.csdn.net/WX10301075WX/article/details/123904543
     */

    @Resource
    @Lazy
    private JudgeService judgeService;


    @Override
    public long doQuestionSubmit(QuestionSubmitAddRequest questionSubmitAddRequest, User loginUser) {
        // 校验编程语言是否合法
        String language = questionSubmitAddRequest.getLanguage();
        QuestionSubmitLanguageEnum languageEnum = QuestionSubmitLanguageEnum.getEnumByValue(language);
        if (languageEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "编程语言错误");
        }
        long questionId = questionSubmitAddRequest.getQuestionId();
        // 判断实体是否存在，根据类别获取实体
        Question question = questionService.getById(questionId);
        if (question == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        // 是否已提交题目
        long userId = loginUser.getId();
        // 每个用户【串行】提交题目 手动防止连点是叭
        QuestionSubmit questionSubmit = new QuestionSubmit();
        questionSubmit.setUserId(userId);
        questionSubmit.setQuestionId(questionId);
        questionSubmit.setCode(questionSubmitAddRequest.getCode());
        questionSubmit.setLanguage(language);
        // 设置初始状态
        questionSubmit.setStatus(QuestionSubmitStatusEnum.WAITING.getValue());
        questionSubmit.setJudgeInfo("{}");
        boolean save = this.save(questionSubmit);
        if (!save) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "数据插入失败");
        }
        Long questionSubmitId = questionSubmit.getId();

        // todo : 执行判题服务
        CompletableFuture.runAsync(() -> {
            judgeService.doJudge(questionSubmitId);
        });
        return questionSubmitId;

        //        return questionSubmit.getId();
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
        String sortField = questionSubmitQueryRequest.getSortField();
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
        // 1. 关联查询用户/题目信息
        Set<Long> userIdSet = questionSubmitList.stream().map(QuestionSubmit::getUserId).collect(Collectors.toSet());
        Set<Long> questionIdSet = questionSubmitList.stream().map(QuestionSubmit::getQuestionId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        Map<Long, List<Question>> questionIdUserListMap = questionService.listByIds(questionIdSet).stream()
                .collect(Collectors.groupingBy(Question::getId));

        // 填充信息
        List<QuestionSubmitVO> questionSubmitVOList = questionSubmitList.stream().map(questionSubmit -> {
            QuestionSubmitVO questionSubmitVO = QuestionSubmitVO.objToVo(questionSubmit);
            Long userId = questionSubmit.getUserId();
            Long questionId = questionSubmit.getQuestionId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            Question question = null;
            if (questionIdUserListMap.containsKey(questionId)) {
                question = questionIdUserListMap.get(questionId).get(0);
            }
            questionSubmitVO.setUserVO(userService.getUserVO(user));
            questionSubmitVO.setQuestionVO(questionService.getQuestionVO(question));
            return questionSubmitVO;
        }).collect(Collectors.toList());

        questionVOPage.setRecords(questionSubmitVOList);
        return questionVOPage;
    }
}




