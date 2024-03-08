package com.pani.oj.judge;

import com.pani.oj.model.entity.QuestionSubmit;

/**
 * @author Pani
 * @date Created in 2024/3/8 20:41
 * @description 判题服务
 */
public interface JudgeService {
    /**
     * 判题
     * @param questionSubmitId
     * @return
     */
    QuestionSubmit doJudge(long questionSubmitId);

}
