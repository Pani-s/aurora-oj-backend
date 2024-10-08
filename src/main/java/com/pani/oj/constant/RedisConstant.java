package com.pani.oj.constant;

/**
 * @author Pani
 * @date Created in 2024/3/25 20:48
 * @description
 */
public interface RedisConstant {
    //region 题目
    /**
     * 题目页缓存（已去除）
     */
    String CACHE_QUESTION_PAGE = "cache:question:page:";
    /**
     * 题目信息缓存
     */
    String CACHE_QUESTION = "cache:question:";

    String QUESTION_SUBMIT_LIMIT = "question:submit:";
    String QUESTION_DEBUG_LIMIT = "question:debug:";

    long CACHE_NULL_TTL = 5;

    long CACHE_QUESTION_TTL = 30;

    long CACHE_QUESTION_PAGE_TTL = 10;
    //endregion

    //region rank

    String CACHE_RANK_NEW_PASS = "cache:rank:newpass:";

    String CACHE_RANK_PASS = "cache:rank:pass:";
    //endregion
}
