package com.pani.oj.judge;

import com.pani.oj.judge.strategy.DefaultJudgeStrategy;
import com.pani.oj.judge.strategy.JavaLangJudgeStrategy;
import com.pani.oj.judge.strategy.JudgeContext;
import com.pani.oj.judge.strategy.JudgeStrategy;
import com.pani.oj.model.enums.QuestionSubmitLanguageEnum;
import com.pani.oj.model.sandbox.JudgeInfo;
import org.springframework.stereotype.Component;

/**
 * @author Pani
 * @date Created in 2024/3/8 21:43
 * @description 判题管理,简化调用
 */
@Component
public class JudgeManager {
    /**
     * 执行判题
     *
     * @param judgeContext
     * @return
     */
    JudgeInfo doJudge(JudgeContext judgeContext) {
        String language = judgeContext.getLanguage();
        JudgeStrategy judgeStrategy;
        if (QuestionSubmitLanguageEnum.JAVA.getValue().equals(language)) {
            judgeStrategy = new JavaLangJudgeStrategy();
        }else{
            judgeStrategy = new DefaultJudgeStrategy();
        }
        return judgeStrategy.doJudge(judgeContext);
    }

}
