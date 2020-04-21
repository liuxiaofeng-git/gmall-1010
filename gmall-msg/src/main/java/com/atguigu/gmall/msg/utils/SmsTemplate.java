package com.atguigu.gmall.msg.utils;

import java.util.HashMap;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import lombok.Data;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
public class SmsTemplate {

    private String host;

    private String path;

    private String method;

    private String appcode;

    public boolean sendMessage(Map<String, String> querys) {
        boolean flag = false;
        Map<String, String> headers = new HashMap<String, String>();
        // 最后在header中的格式(中间是英文空格)为Authorization:APPCODE 83359fd73fe94948385f570e3c139105
        headers.put("Authorization", "APPCODE " + appcode);

        Map<String, String> bodys = new HashMap<String, String>();
        try {
            HttpResponse response = HttpUtils.doPost(host, path, method, headers, querys, bodys);
            String bodyStr = EntityUtils.toString(response.getEntity());
            log.info("获取到的响应体内容是：{}", bodyStr);
            Map map = JSON.parseObject(bodyStr, Map.class);
            log.info("解析响应json字符串的内容是：{}", map);
            flag = "00000".equals(map.get("return_code"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return flag;
    }
}
