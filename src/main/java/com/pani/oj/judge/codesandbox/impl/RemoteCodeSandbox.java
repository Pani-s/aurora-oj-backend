package com.pani.oj.judge.codesandbox.impl;

import com.pani.oj.judge.codesandbox.CodeSandbox;
import com.pani.oj.judge.codesandbox.model.ExecuteCodeRequest;
import com.pani.oj.judge.codesandbox.model.ExecuteCodeResponse;

/**
 * @author Pani
 * @date Created in 2024/3/8 20:11
 * @description 远程代码沙箱
 */
public class RemoteCodeSandbox implements CodeSandbox {
    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        System.out.println("远程代码沙箱");
        return null;
    }
}
