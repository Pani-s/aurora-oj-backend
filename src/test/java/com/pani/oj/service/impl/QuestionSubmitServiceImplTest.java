package com.pani.oj.service.impl;

import com.pani.oj.model.dto.questionsubmit.QuestionSubmitAddRequest;
import com.pani.oj.model.entity.User;
import com.pani.oj.service.QuestionSubmitService;
import lombok.ToString;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Pani
 * @date Created in 2024/3/8 21:50
 * @description
 */
@SpringBootTest
class QuestionSubmitServiceImplTest {
    @Resource
    private QuestionSubmitService questionSubmitService;
    
    @Test
    void testDoSubmit(){
        QuestionSubmitAddRequest request = new QuestionSubmitAddRequest();
        request.setLanguage("java");
        request.setCode("hello!!");
        request.setQuestionId(1765726748353830914L);
        User user = new User();
        user.setId(1764904080301883393L);
        long l = questionSubmitService.doQuestionSubmit(request, user);
        //ok 通过
    }

}