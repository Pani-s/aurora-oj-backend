package com.pani.oj.judge;

import com.pani.oj.judge.strategy.DefaultJudgeStrategy;
import com.pani.oj.judge.strategy.JavaLangJudgeStrategy;
import com.pani.oj.judge.strategy.JudgeContext;
import com.pani.oj.judge.strategy.JudgeStrategy;
import com.pani.oj.model.dto.questionsubmit.JudgeInfo;
import com.pani.oj.model.entity.QuestionSubmit;

/**
 * @author Pani
 * @date Created in 2024/3/8 21:43
 * @description 判题管理,简化调用
 */
public class JudgeManager {
    /**
     * 执行判题
     *
     * @param judgeContext
     * @return
     */
    JudgeInfo doJudge(JudgeContext judgeContext) {
        QuestionSubmit questionSubmit = judgeContext.getQuestionSubmit();
        String language = questionSubmit.getLanguage();
        JudgeStrategy judgeStrategy = new DefaultJudgeStrategy();
        if ("java".equals(language)) {
            judgeStrategy = new JavaLangJudgeStrategy();
        }
        return judgeStrategy.doJudge(judgeContext);
    }

}
