package com.atguigu.gmall.pms;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class GmallPmsApplicationTests {

    @Test
    void contextLoads() {
        int i = 1 ;
        int j = i++ ;
        if((i==(++j))&&((i++)==j))     {
            i += j ;
        }
        System.out.println("i = "+i);
    }



}
