package com.atguigu.gmall.pms.controller;

import com.aliyun.oss.OSSClient;
import com.aliyun.oss.common.utils.BinaryUtil;
import com.aliyun.oss.model.MatchMode;
import com.aliyun.oss.model.PolicyConditions;
import com.atguigu.gmall.common.bean.ResponseVo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.SimpleFormatter;

@RestController
@RequestMapping("pms/oss")
public class OssController {

    @GetMapping("policy")
    public ResponseVo<Object> policy() {
        String accessId = "LTAI4FdtNTcKPZGatuyo4iGf"; // 请填写您的AccessKeyId。
        //String accessId = "LTAI4Fh3FUjUiXBJRT1uTCVL"; // 请填写您的AccessKeyId。

        String accessKey = "WQIApOe1C9NiZmrk17KXlBUb6NBjIS"; // 请填写您的AccessKeySecret。
        // String accessKey = "dikhTNspA348l21EedrhXT8xd0BS0N"; // 请填写您的AccessKeySecret。

        String endpoint = "oss-cn-shanghai.aliyuncs.com"; // 请填写您的 endpoint。
        String bucket = "scw-sh191010java117"; // 请填写您的 bucketname 。
        // String bucket = "zyzyzyzy"; // 请填写您的 bucketname 。

        String host = "https://" + bucket + "." + endpoint; // host的格式为 bucketname.endpoint
        // callbackUrl为 上传回调服务器的URL，请将下面的IP和Port配置为您自己的真实信息。
        // String callbackUrl = "http://88.88.88.88:8888";
        String dir = "gmall/" + new SimpleDateFormat("yyyy-MM-dd").format(new Date()); // 用户上传文件时指定的前缀。
        // String dir = new SimpleDateFormat("yyyy-MM-dd").format(new Date()); // 用户上传文件时指定的前缀。

        OSSClient client = new OSSClient(endpoint, accessId, accessKey);
        Map<String, String> respMap = null;
        try {
            long expireTime = 30;
            long expireEndTime = System.currentTimeMillis() + expireTime * 1000;
            Date expiration = new Date(expireEndTime);
            PolicyConditions policyConds = new PolicyConditions();
            policyConds.addConditionItem(PolicyConditions.COND_CONTENT_LENGTH_RANGE, 0, 1048576000);
            policyConds.addConditionItem(MatchMode.StartWith, PolicyConditions.COND_KEY, dir);

            String postPolicy = client.generatePostPolicy(expiration, policyConds);
            byte[] binaryData = postPolicy.getBytes("utf-8");
            String encodedPolicy = BinaryUtil.toBase64String(binaryData);
            String postSignature = client.calculatePostSignature(postPolicy);

            respMap = new LinkedHashMap<String, String>();
            respMap.put("accessid", accessId);
            respMap.put("policy", encodedPolicy);
            respMap.put("signature", postSignature);
            respMap.put("dir", dir);
            respMap.put("host", host);
            respMap.put("expire", String.valueOf(expireEndTime / 1000));
            // respMap.put("expire", formatISO8601Date(expiration));


        } catch (Exception e) {
            // Assert.fail(e.getMessage());
            System.out.println(e.getMessage());
        }

        return ResponseVo.ok(respMap);

    }
}
