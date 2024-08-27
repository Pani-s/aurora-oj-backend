package com.pani.oj.judge.codesandbox.impl;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.pani.oj.common.ErrorCode;
import com.pani.oj.exception.BusinessException;
import com.pani.oj.judge.codesandbox.CodeSandbox;
import com.pani.oj.model.sandbox.ExecuteCodeRequest;
import com.pani.oj.model.sandbox.ExecuteCodeResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author Pani
 * @date Created in 2024/3/8 20:11
 * @description 远程代码沙箱
 */
@Slf4j
@Component
public class RemoteCodeSandbox implements CodeSandbox {
    private final int TIMELIMIT_10 = 10000;
    @Value("${codeSandbox.remoteUrl}")
    private String remoteUrl;
    /**
     * 定义鉴权请求头和密钥
     */
    private static final String AUTH_REQUEST_HEADER = "auth";

    private static final String AUTH_REQUEST_SECRET = SecureUtil.md5("kookv");

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        log.info("-------远程代码沙箱docker---------");
//        String url = remoteUrl + "/executeCode/";
        String url = remoteUrl + "/executeCode/ACM";
        String json = JSONUtil.toJsonStr(executeCodeRequest);
        try (HttpResponse httpResponse = HttpUtil.createPost(url)
                .header(AUTH_REQUEST_HEADER, AUTH_REQUEST_SECRET)
                .body(json).timeout(TIMELIMIT_10)
                //设置超时控制。。3秒
                .execute()) {
            String responseStr = httpResponse.body();
            if (StringUtils.isBlank(responseStr)) {
                throw new BusinessException(ErrorCode.API_REQUEST_ERROR, "executeCode remoteSandbox error, message = " + responseStr);
            }
            return JSONUtil.toBean(responseStr, ExecuteCodeResponse.class);
        } catch (Exception e) {
            log.info("远程代码沙箱调用异常：{}", e.getMessage());
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, e.getMessage());
        }
    }
}
