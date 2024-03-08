package com.pani.oj.judge.strategy;

import com.pani.oj.model.dto.question.JudgeCase;
import com.pani.oj.model.dto.question.JudgeConfig;
import com.pani.oj.model.dto.questionsubmit.JudgeInfo;
import com.pani.oj.model.entity.QuestionSubmit;
import lombok.Data;

import java.util.List;

/**
 * @author Pani
 * @date Created in 2024/3/8 21:11
 * @description 上下文（用于定义在策略中传递的参数）
 * 可以理解为一种 DTO
 */
@Data
public class JudgeContext {
    /**
     * executeCodeResponse judgeInfo
     */
    private JudgeInfo judgeInfo;

    //    /**
    //     * 题目的 输入list
    //     */
    //    private List<String> inputList;

    /**
     * 执行出来的
     */
    private List<String> outputList;

    /**
     * 标答
     */
    private List<JudgeCase> judgeCaseList;

    /**
     * 答案的时间空间限制
     */
    private JudgeConfig judgeConfig;

    private QuestionSubmit questionSubmit;

}
