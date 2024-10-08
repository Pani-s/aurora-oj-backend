package com.pani.oj.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户题目通过记录
 * @TableName user_submit
 */
@TableName(value ="user_submit")
@Data
public class UserSubmit implements Serializable {
    /**
     * 用户 id
     */
    @TableId
    private Long userId;

    /**
     * 题目 id
     */
    private Long questionId;

    /**
     * 通过时间
     */
    private Date createTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}