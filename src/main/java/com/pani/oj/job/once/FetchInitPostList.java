package com.pani.oj.job.once;

import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.pani.oj.model.entity.Post;
import com.pani.oj.service.PostService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 帖子
 *
 * @author Pani
 */

//用了一次我就注释
//@Component
@Slf4j
public class FetchInitPostList implements CommandLineRunner {

    @Resource
    private PostService postService;



    @Override
    public void run(String... args) {
        // 1. 获取数据
        String json = "{\"current\": 1, \"pageSize\": 8, \"sortField\": \"createTime\", " +
                "\"sortOrder\": \"descend\", \"category\": \"文章\",\"reviewStatus\": 1}";
        String url = "https://www.code-nav.cn/api/post/search/page/vo";
        String result = HttpRequest
                .post(url)
                .body(json)
                .execute()
                .body();
        Map<String,Object> map = JSONUtil.toBean(result, Map.class);
        JSONObject data = (JSONObject) map.get("data");
        JSONArray records = (JSONArray) data.get("records");

        ArrayList<Post> postList = new ArrayList<>();
        for (Object record : records) {
            JSONObject tempRec = (JSONObject)record;
            Post post = new Post();
            post.setId(Long.parseLong(tempRec.getStr("id")));
            post.setTitle(tempRec.getStr("title"));
            post.setContent(tempRec.getStr("content"));
            JSONArray tags = (JSONArray)tempRec.get("tags");
            List<String> tagsList = tags.toList(String.class);
            post.setTags(JSONUtil.toJsonStr(tagsList));
            post.setUserId(1L);
            postList.add(post);
        }
        boolean b = postService.saveBatch(postList);
        if(b){
            log.info("获取初始化帖子列表成功, 条数 = {}", postList.size());
        }else{
            System.err.println("获取初始化帖子列表失败");
            log.error("获取初始化帖子列表失败!!!!");
        }
    }
}
