package com.pani.oj.judge.codesandbox.impl;


import com.pani.oj.judge.codesandbox.CodeSandbox;
import com.pani.oj.judge.codesandbox.CodeSandboxProxy;
import com.pani.oj.model.sandbox.ExecuteCodeRequest;
import com.pani.oj.model.sandbox.ExecuteCodeResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author Pani
 * @date Created in 2024/3/8 20:10
 * @description 引入的第三方代码沙箱
 */
@Slf4j
@Component
public class ThirdPartyCodeSandbox implements CodeSandbox {
    @Resource
    private RemoteCodeSandbox remoteCodeSandbox;
    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        //如果第三方没api了
        if(false){
            return new CodeSandboxProxy(remoteCodeSandbox).
                    executeCode(executeCodeRequest);
        }
        log.info("第三方代码沙箱---执行");
        return null;
    }
}
