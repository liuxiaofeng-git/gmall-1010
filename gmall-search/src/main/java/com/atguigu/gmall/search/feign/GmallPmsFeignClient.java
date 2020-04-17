package com.atguigu.gmall.search.feign;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.pms.api.GmallPmsApi;
import com.atguigu.gmall.pms.entity.SpuEntity;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient("pms-service")
public interface GmallPmsFeignClient extends GmallPmsApi {

    @GetMapping("pms/spu/{id}")
    @ApiOperation("详情查询")
    public ResponseVo<SpuEntity> querySpuById(@PathVariable("id") Long id);
}
