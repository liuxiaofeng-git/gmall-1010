package com.atguigu.gmall.pms.service.impl;

import com.alibaba.nacos.client.utils.StringUtils;
import com.atguigu.gmall.pms.vo.SpuVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.pms.mapper.SpuDescMapper;
import com.atguigu.gmall.pms.entity.SpuDescEntity;
import com.atguigu.gmall.pms.service.SpuDescService;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;


@Service("spuDescService")
public class SpuDescServiceImpl extends ServiceImpl<SpuDescMapper, SpuDescEntity> implements SpuDescService {

    @Autowired
    private SpuDescMapper spuDescMapper;

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<SpuDescEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<SpuDescEntity>()
        );

        return new PageResultVo(page);
    }

    @Override
    @Transactional
    public Long saveSpuDesc(SpuVo spuVo) {
        SpuDescEntity spuDescEntity = new SpuDescEntity();
        Long spuId = spuVo.getId();
        spuDescEntity.setSpuId(spuId);
        spuDescEntity.setDecript(StringUtils.join(spuVo.getSpuImages(), ","));
        spuDescMapper.insert(spuDescEntity);
        return spuId;
    }
}