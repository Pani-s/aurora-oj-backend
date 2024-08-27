package com.pani.oj.judge;

import com.pani.oj.model.dto.questionsubmit.QuestionDebugRequest;
import com.pani.oj.model.entity.QuestionSubmit;
import com.pani.oj.model.vo.QuestionDebugResponse;

/**
 * @author Pani
 * @date Created in 2024/3/8 20:41
 * @description 判题服务
 */
public interface JudgeService {
    int RUN_SUCCESS = 0;
    /**
     * 判题
     * @param questionSubmitId
     * @return
     */
    boolean doJudge(long questionSubmitId);

    /**
     * debug 测试运行
     * @return
     */
    QuestionDebugResponse doDebug(QuestionDebugRequest questionDebugRequest);

    /**
     * 设置代码沙箱类型
     * @param type
     */
    void setType(String type);

    /**
     * get代码沙箱类型
     */
    String getType();

}
