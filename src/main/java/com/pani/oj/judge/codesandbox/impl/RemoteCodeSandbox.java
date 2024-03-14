package com.pani.oj.judge.codesandbox.impl;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.pani.oj.common.ErrorCode;
import com.pani.oj.exception.BusinessException;
import com.pani.oj.judge.codesandbox.CodeSandbox;
import com.pani.oj.judge.codesandbox.model.ExecuteCodeRequest;
import com.pani.oj.judge.codesandbox.model.ExecuteCodeResponse;
import org.apache.commons.lang3.StringUtils;

/**
 * @author Pani
 * @date Created in 2024/3/8 20:11
 * @description 远程代码沙箱
 */
public class RemoteCodeSandbox implements CodeSandbox {
    /**
     * 定义鉴权请求头和密钥
     */
    private static final String AUTH_REQUEST_HEADER = "auth";

    private static final String AUTH_REQUEST_SECRET = SecureUtil.md5("kookv");

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        System.out.println("远程代码沙箱");
        String url = "http://localhost:8123/executeCode";
        String json = JSONUtil.toJsonStr(executeCodeRequest);
        String responseStr = HttpUtil.createPost(url)
                .header(AUTH_REQUEST_HEADER, AUTH_REQUEST_SECRET)
                .body(json)
                .execute()
                .body();
        if (StringUtils.isBlank(responseStr)) {
            throw new BusinessException(ErrorCode.API_REQUEST_ERROR, "executeCode remoteSandbox error, message = " + responseStr);
        }
        return JSONUtil.toBean(responseStr, ExecuteCodeResponse.class);

    }
}
