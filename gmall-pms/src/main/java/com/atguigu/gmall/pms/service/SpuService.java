package com.atguigu.gmall.pms.service;

import com.atguigu.gmall.pms.vo.SpuVo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.pms.entity.SpuEntity;

import java.util.Map;

/**
 * spu信息
 *
 * @author liuxiaofeng
 * @email xfliu@atguigu.com
 * @date 2020-03-31 19:58:53
 */
public interface SpuService extends IService<SpuEntity> {

    PageResultVo queryPage(PageParamVo paramVo);

    PageResultVo querySpusPageByCategoryId(PageParamVo pageParamVo, Long categoryId);

    void bigSave(SpuVo spuVo);

    void saveSku(SpuVo spuVo, Long spuId);

    void saveSpuAttrValue(SpuVo spuVo, Long spuId);

    void saveSpu(SpuVo spuVo);
}

