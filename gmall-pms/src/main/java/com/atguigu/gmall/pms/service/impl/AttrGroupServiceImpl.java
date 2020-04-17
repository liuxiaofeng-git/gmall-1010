package com.atguigu.gmall.pms.service.impl;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.pms.entity.AttrEntity;
import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.pms.entity.SpuAttrValueEntity;
import com.atguigu.gmall.pms.mapper.AttrMapper;
import com.atguigu.gmall.pms.mapper.SkuAttrValueMapper;
import com.atguigu.gmall.pms.mapper.SpuAttrValueMapper;
import com.atguigu.gmall.pms.vo.AttrValueVo;
import com.atguigu.gmall.pms.vo.GroupVo;
import com.atguigu.gmall.pms.vo.ItemGroupVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.pms.mapper.AttrGroupMapper;
import com.atguigu.gmall.pms.entity.AttrGroupEntity;
import com.atguigu.gmall.pms.service.AttrGroupService;
import org.springframework.util.CollectionUtils;


@Service("attrGroupService")
public class AttrGroupServiceImpl extends ServiceImpl<AttrGroupMapper, AttrGroupEntity> implements AttrGroupService {

    @Autowired
    private AttrMapper attrMapper;

    @Autowired
    private AttrGroupMapper attrGroupMapper;

    @Autowired
    private SpuAttrValueMapper spuAttrValueMapper;

    @Autowired
    private SkuAttrValueMapper skuAttrValueMapper;

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<AttrGroupEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<AttrGroupEntity>()
        );

        return new PageResultVo(page);
    }

    @Override
    public ResponseVo<List<AttrGroupEntity>> queryAttrGroupsByCategoryId(Long cid) {
        return ResponseVo.ok(baseMapper.selectList(new QueryWrapper<AttrGroupEntity>().eq("category_id", cid)));
    }

    @Override
    public List<GroupVo> queryById(Long catId) {
        List<AttrGroupEntity> groupEntities = this.list(new QueryWrapper<AttrGroupEntity>().eq("category_id", catId));
        return groupEntities.stream().map(attrGroupEntity -> {
            GroupVo groupVo = new GroupVo();
            BeanUtils.copyProperties(attrGroupEntity, groupVo);
            List<AttrEntity> attrEntities = attrMapper.selectList(new QueryWrapper<AttrEntity>().eq("group_id", attrGroupEntity.getId()).eq("type", 1));
            groupVo.setAttrEntities(attrEntities);
            return groupVo;
        }).collect(Collectors.toList());

    }

    @Override
    public List<ItemGroupVo> queryGroupsWithAttrValues(Long cid, Long spuId, Long skuId) {
        List<AttrGroupEntity> attrGroupEntities = this.attrGroupMapper.selectList(new QueryWrapper<AttrGroupEntity>().eq("category_id", cid));
        if (CollectionUtils.isEmpty(attrGroupEntities)) {
            return null;
        }
        return attrGroupEntities.stream().map(attrGroupEntity -> {
            ItemGroupVo itemGroupVo = new ItemGroupVo();
            List<AttrEntity> attrEntities = this.attrMapper.selectList(new QueryWrapper<AttrEntity>().eq("group_id", attrGroupEntity.getId()));
            if (!CollectionUtils.isEmpty(attrEntities)) {
                List<Long> attrIds = attrEntities.stream().map(AttrEntity::getId).collect(Collectors.toList());
                //查询所有的销售属性
                ArrayList<AttrValueVo> attrValueVos = new ArrayList<>();
                List<SkuAttrValueEntity> skuAttrValueEntities = this.skuAttrValueMapper.selectList(new QueryWrapper<SkuAttrValueEntity>().eq("sku_id", skuId).in("attr_id", attrIds));
                if (!CollectionUtils.isEmpty(skuAttrValueEntities)) {
                    List<AttrValueVo> skuAttrValueVoList = skuAttrValueEntities.stream().map(skuAttrValueEntity -> {
                        AttrValueVo attrValueVo = new AttrValueVo();
                        BeanUtils.copyProperties(skuAttrValueEntity, attrValueVo);
                        return attrValueVo;
                    }).collect(Collectors.toList());
                    attrValueVos.addAll(skuAttrValueVoList);
                }

                //查询所有的基本属性
                List<SpuAttrValueEntity> spuAttrValueEntities = this.spuAttrValueMapper.selectList(new QueryWrapper<SpuAttrValueEntity>().eq("spu_id", spuId).in("attr_id", attrIds));
                if (!CollectionUtils.isEmpty(spuAttrValueEntities)) {
                    List<AttrValueVo> spuAttrValueVoList = spuAttrValueEntities.stream().map(spuAttrValueEntity -> {
                        AttrValueVo attrValueVo = new AttrValueVo();
                        BeanUtils.copyProperties(spuAttrValueEntity, attrValueVo);
                        return attrValueVo;
                    }).collect(Collectors.toList());
                    attrValueVos.addAll(spuAttrValueVoList);
                }
                itemGroupVo.setAttrValues(attrValueVos);
            }
            itemGroupVo.setGroupId(attrGroupEntity.getId());
            itemGroupVo.setGroupName(attrGroupEntity.getName());
            return itemGroupVo;
        }).collect(Collectors.toList());
    }

}