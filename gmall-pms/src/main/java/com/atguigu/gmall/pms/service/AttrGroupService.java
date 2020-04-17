package com.atguigu.gmall.pms.service;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.pms.vo.GroupVo;
import com.atguigu.gmall.pms.vo.ItemGroupVo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.pms.entity.AttrGroupEntity;

import java.util.List;
import java.util.Map;

/**
 * 属性分组
 *
 * @author liuxiaofeng
 * @email xfliu@atguigu.com
 * @date 2020-03-31 19:58:53
 */
public interface AttrGroupService extends IService<AttrGroupEntity> {

    PageResultVo queryPage(PageParamVo paramVo);

    ResponseVo<List<AttrGroupEntity>> queryAttrGroupsByCategoryId(Long cid);

    List<GroupVo> queryById(Long catId);

    List<ItemGroupVo> queryGroupsWithAttrValues(Long cid, Long spuId, Long skuId);
}

