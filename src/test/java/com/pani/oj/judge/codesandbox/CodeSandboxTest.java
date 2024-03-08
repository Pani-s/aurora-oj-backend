package com.pani.oj.judge.codesandbox;

import com.pani.oj.judge.codesandbox.impl.ExampleCodeSandbox;
import com.pani.oj.judge.codesandbox.model.ExecuteCodeRequest;
import com.pani.oj.judge.codesandbox.model.ExecuteCodeResponse;
import com.pani.oj.model.enums.QuestionSubmitLanguageEnum;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.shadow.com.univocity.parsers.annotations.Validate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Pani
 * @date Created in 2024/3/8 20:12
 * @description
 */
@SpringBootTest
class CodeSandboxTest {
    @Value("${codeSandbox.type:example}")
    private  String type;

    @Test
    void testSandBox(){
        CodeSandbox codeSandbox = new ExampleCodeSandbox();
        ExecuteCodeRequest request = ExecuteCodeRequest.builder().
                code("hello world").
                language(QuestionSubmitLanguageEnum.JAVA.getValue()).
                inputList(new ArrayList<>()).
                build();
        ExecuteCodeResponse executeCodeResponse = codeSandbox.executeCode(request);

    }

    @Test
    void testFactory() {
        CodeSandbox codeSandbox = CodeSandboxFactory.getInstance(type);
        ExecuteCodeRequest request = ExecuteCodeRequest.builder().
                code("hello world").
                language(QuestionSubmitLanguageEnum.JAVA.getValue()).
                inputList(new ArrayList<>()).
                build();
        ExecuteCodeResponse executeCodeResponse = codeSandbox.executeCode(request);
    }

    @Test
    void testProxy(){
        CodeSandbox codeSandbox = CodeSandboxFactory.getInstance(type);
        ExecuteCodeRequest request = ExecuteCodeRequest.builder().
                code("hello world").
                language(QuestionSubmitLanguageEnum.JAVA.getValue()).
                inputList(new ArrayList<>()).
                build();
        ExecuteCodeResponse executeCodeResponse = new CodeSandboxProxy(codeSandbox).executeCode(request);
    }

}