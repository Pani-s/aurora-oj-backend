package com.pani.oj.constant;

/**
 * 用户常量
 *
 * @author pani
 * 
 */
public interface UserConstant {

    /**
     * 用户登录态键
     */
    String USER_LOGIN_STATE = "user_login";

    //  region 权限

    /**
     * 默认角色
     */
    String DEFAULT_ROLE = "user";

    /**
     * 管理员角色
     */
    String ADMIN_ROLE = "admin";

    /**
     * 被封号
     */
    String BAN_ROLE = "ban";

    // endregion
    int ACCOUNT_LEN_SHORTEST = 3;
    int PWD_LEN_SHORTEST = 6;
}