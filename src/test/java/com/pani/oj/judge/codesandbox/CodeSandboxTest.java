package com.pani.oj.judge.codesandbox;

import com.pani.oj.judge.codesandbox.impl.ExampleCodeSandbox;
import com.pani.oj.model.enums.QuestionSubmitLanguageEnum;
import com.pani.oj.model.sandbox.ExecuteCodeRequest;
import com.pani.oj.model.sandbox.ExecuteCodeResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Pani
 * @date Created in 2024/3/8 20:12
 * @description
 */
@SpringBootTest
class CodeSandboxTest {
    @Value("${codeSandbox.type:example}")
    private  String type;

    @Resource
    private CodeSandboxFactory codeSandboxFactory;

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
        CodeSandbox codeSandbox = codeSandboxFactory.getInstanceWithType(type);
        ExecuteCodeRequest request = ExecuteCodeRequest.builder().
                code("hello world").
                language(QuestionSubmitLanguageEnum.JAVA.getValue()).
                inputList(new ArrayList<>()).
                build();
        ExecuteCodeResponse executeCodeResponse = codeSandbox.executeCode(request);
    }

    @Test
    void testProxy(){
        CodeSandbox codeSandbox = codeSandboxFactory.getInstanceWithType(type);
        ExecuteCodeRequest request = ExecuteCodeRequest.builder().
                code("hello world").
                language(QuestionSubmitLanguageEnum.JAVA.getValue()).
                inputList(new ArrayList<>()).
                build();
        ExecuteCodeResponse executeCodeResponse = new CodeSandboxProxy(codeSandbox).executeCode(request);
    }

    @Test
    void executeCodeByProxy() {
        CodeSandbox codeSandbox = codeSandboxFactory.getInstanceWithType(type);
        codeSandbox = new CodeSandboxProxy(codeSandbox);
        String code = "public class Main {\n" +
                "    public static void main(String[] args) {\n" +
                "        int a = Integer.parseInt(args[0]);\n" +
                "        int b = Integer.parseInt(args[1]);\n" +
                "        System.out.println(\"结果:\" + (a + b));\n" +
                "    }\n" +
                "}";
        String language = QuestionSubmitLanguageEnum.JAVA.getValue();
        List<String> inputList = Arrays.asList("1 2", "3 4");
        ExecuteCodeRequest executeCodeRequest = ExecuteCodeRequest.builder()
                .code(code)
                .language(language)
                .inputList(inputList)
                .build();
        ExecuteCodeResponse executeCodeResponse = codeSandbox.executeCode(executeCodeRequest);
        Assertions.assertNotNull(executeCodeResponse);
    }


}