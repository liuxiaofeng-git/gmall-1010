package com.atguigu.gmall.pms.api;

import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.pms.vo.AttrValueVo;
import com.atguigu.gmall.pms.vo.CategoryVo;
import com.atguigu.gmall.pms.vo.ItemCategoryVo;
import com.atguigu.gmall.pms.vo.ItemGroupVo;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import java.util.List;

public interface GmallPmsApi {

    @GetMapping("pms/attrgroup/attr/withvalue")
    public ResponseVo<List<ItemGroupVo>> queryGroupsWithAttrValues(@RequestParam("cid") Long cid,
                                                                   @RequestParam("spuId") Long spuId,
                                                                   @RequestParam("skuId") Long skuId);

    @GetMapping("pms/skuattrvalue/sku/{skuId}")
    public ResponseVo<List<AttrValueVo>> querySkuAttrValuesBySkuId(@PathVariable("skuId") Long skuId);

    @GetMapping("pms/skuattrvalue/spu/{spuId}")
    public ResponseVo<List<AttrValueVo>> querySkuAttrValuesBySpuId(@PathVariable("spuId") Long spuId);

    @GetMapping("pms/category/all/{cid3}")
    public ResponseVo<List<ItemCategoryVo>> queryCategoriesByCid3(@PathVariable("cid3") Long cid3);

    @GetMapping("pms/skuimages/sku/{skuId}")
    @ApiOperation("根据skuId查询详情")
    public ResponseVo<List<SkuImagesEntity>> querySkuImagesBySkuId(@PathVariable("skuId") Long skuId);

    @GetMapping("pms/spudesc/{spuId}")
    public ResponseVo<SpuDescEntity> querySpuDescById(@PathVariable("spuId") Long spuId);

    @GetMapping("pms/spu/{id}")
    public ResponseVo<SpuEntity> querySpuById(@PathVariable("id") Long id);

    @GetMapping("pms/sku/{id}")
    public ResponseVo<SkuEntity> querySkuById(@PathVariable("id") Long id);

    @PostMapping("pms/spu/page")
    public ResponseVo<List<SpuEntity>> querySpusByPage(@RequestBody PageParamVo paramVo);

    @GetMapping("pms/sku/spu/{spuId}")
    public ResponseVo<List<SkuEntity>> querySkusBySpuId(@PathVariable("spuId")Long spuId);

    @GetMapping("pms/category/{id}")
    public ResponseVo<CategoryEntity> queryCategoryById(@PathVariable("id") Long id);

    @GetMapping("pms/brand/{id}")
    public ResponseVo<BrandEntity> queryBrandById(@PathVariable("id") Long id);

    @GetMapping("pms/attr/category/{cid}")
    public ResponseVo<List<AttrEntity>> queryAttrsByCategoryId(@PathVariable("cid") Long cid,
                                                               @RequestParam(value = "type", required = false) Integer type,
                                                               @RequestParam(value = "searchType", required = false) Integer searchType);

    @GetMapping("pms/skuattrvalue/search/attr")
    public ResponseVo<List<SkuAttrValueEntity>> querySkuAttrValuesById(@RequestParam("skuId")Long skuId,
                                                                       @RequestParam("attrIds")List<Long> attrIds);

    @GetMapping("pms/spuattrvalue/search/attr")
    public ResponseVo<List<SpuAttrValueEntity>> querySpuAttrValuesById(@RequestParam("spuId")Long spuId,
                                                                       @RequestParam("attrIds")List<Long> attrIds);


    @GetMapping("pms/category/parent/{parentId}")
    public ResponseVo<List<CategoryEntity>> queryCategoryByParentId(@PathVariable("parentId") Long parentId);

    @GetMapping("pms/category/levelOne/{pid}")
    public ResponseVo<List<CategoryVo>> queryCategoryVoByParentId(@PathVariable("pid")Long pid);
}
