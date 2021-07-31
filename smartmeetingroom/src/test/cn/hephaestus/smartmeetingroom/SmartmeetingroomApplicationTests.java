package cn.hephaestus.smartmeetingroom;

import cn.hephaestus.smartmeetingroom.model.News;
import cn.hephaestus.smartmeetingroom.service.NewsService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Date;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class SmartmeetingroomApplicationTests {

    @Autowired
    NewsService newsService;
    @Test
    public void test(){
        News news = new News(1,2,3,"123",new Date(),123,"2385");
        newsService.insertNews(news);
    }
}