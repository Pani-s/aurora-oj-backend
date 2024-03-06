package com.pani.oj.model.dto.questionsubmit;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.pani.oj.model.dto.question.JudgeCase;
import com.pani.oj.model.dto.question.JudgeConfig;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 提交题目 的 创建请求
 *
 * @author pani
 */
@Data
public class QuestionSubmitAddRequest implements Serializable {

    /**
     * 编程语言
     */
    private String language;

    /**
     * 用户代码
     */
    private String code;

    /**
     * 题目 id
     */
    private Long questionId;


    private static final long serialVersionUID = 1L;
}