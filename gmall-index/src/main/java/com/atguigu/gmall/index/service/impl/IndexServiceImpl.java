package com.atguigu.gmall.index.service.impl;



import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.index.config.GmallCache;
import com.atguigu.gmall.index.feign.GmallPmsFeignClient;
import com.atguigu.gmall.index.service.IndexService;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.atguigu.gmall.pms.vo.CategoryVo;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.util.List;


@Service
public class IndexServiceImpl implements IndexService {

    @Autowired
    private GmallPmsFeignClient pmsFeignClient;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    public static final String KEY_PREFIX = "index:cates:";

    @Override
    @GmallCache(keyPrefix = KEY_PREFIX, lockKey = "lock", timeout = 5, random = 50)
    public List<CategoryEntity> queryCategoryOneLevel() {
        ResponseVo<List<CategoryEntity>> categoryResponseVo = pmsFeignClient.queryCategoryByParentId(0L);
        List<CategoryEntity> categoryResponseVoData = categoryResponseVo.getData();
        return categoryResponseVoData;
    }

    @Override
    @GmallCache(keyPrefix = KEY_PREFIX, lockKey = "lock", timeout = 5, random = 50)
    public List<CategoryVo> queryCategoryTwoLevelByParentIdWithSubs(Long pid) {
        //缓存中没有，去数据库查询
        ResponseVo<List<CategoryVo>> categoryVoResponseVo = this.pmsFeignClient.queryCategoryVoByParentId(pid);
        List<CategoryVo> categoryVoResponseVoData = categoryVoResponseVo.getData();
        return categoryVoResponseVoData;
    }
}
