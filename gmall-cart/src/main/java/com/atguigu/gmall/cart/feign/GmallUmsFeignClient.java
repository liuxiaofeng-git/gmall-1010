package com.atguigu.gmall.cart.feign;

import com.atguigu.gmall.ums.api.GmallUmsApi;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient("ums-service")
public interface GmallUmsFeignClient extends GmallUmsApi {
}
