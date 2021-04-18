package com.atguigu.gmall.index.filter;

import com.google.common.base.Charsets;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;

/**
 * @Description bloom filter test
 * @Author rookie
 * @Date 2021/4/14 20:28
 */
public class BloomFilterDemo {
    public static void main(String[] args) {

        BloomFilter<CharSequence> bloomFilter = BloomFilter.create(Funnels.stringFunnel(Charsets.UTF_8),20,0.3);
        for (int i = 1; i < 9; i++) {
            bloomFilter.put(String.valueOf(i));
        }

        for (int i = 5; i < 20; i++) {
            System.out.println(i + "：bloomFilter.mightContain(String.valueOf(i)) = " + bloomFilter.mightContain(String.valueOf(i)));
        }
    }
}