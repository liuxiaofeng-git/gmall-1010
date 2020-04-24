package com.atguigu.gmall.oms.mapper;

import com.atguigu.gmall.oms.entity.OrderEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单
 * 
 * @author liuxiaofeng
 * @email xfliu@atguigu.com
 * @date 2020-04-23 14:24:54
 */
@Mapper
public interface OrderMapper extends BaseMapper<OrderEntity> {
	
}
