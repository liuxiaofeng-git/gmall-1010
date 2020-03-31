package com.atguigu.gmall.sms.mapper;

import com.atguigu.gmall.sms.entity.SeckillSessionEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 秒杀活动场次
 * 
 * @author liuxiaofeng
 * @email xfliu@atguigu.com
 * @date 2020-03-31 21:22:36
 */
@Mapper
public interface SeckillSessionMapper extends BaseMapper<SeckillSessionEntity> {
	
}
