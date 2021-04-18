package com.atguigu.gmall.index.config;


import com.atguigu.gmall.index.feign.GmallPmsClient;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * @Description TODO
 * @Author rookie
 * @Date 2021/4/14 21:20
 */
@Configuration
public class BloomFilter {

    @Autowired
    private GmallPmsClient gmallPmsClient;

    @Autowired
    private RedissonClient redissonClient;

    private static final String KEY_PREFIX = "index:cates:";

    @Bean
    public RBloomFilter instanceBloomFilter() {
        RBloomFilter<Object> bloomFilter = redissonClient.getBloomFilter("index:bloom");
        bloomFilter.tryInit(1500l,0.03);
        List<CategoryEntity> categoryEntities = gmallPmsClient.queryCategory(0l).getData();
        if(!CollectionUtils.isEmpty(categoryEntities)) {
            categoryEntities.stream().forEach(categoryEntity -> bloomFilter.add(KEY_PREFIX + "[" + categoryEntity.getId() + "]"));
        }
        return bloomFilter;
    }
}