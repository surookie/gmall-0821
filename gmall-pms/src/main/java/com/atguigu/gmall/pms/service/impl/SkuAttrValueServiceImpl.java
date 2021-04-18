package com.atguigu.gmall.pms.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.pms.entity.AttrEntity;
import com.atguigu.gmall.pms.entity.SkuEntity;
import com.atguigu.gmall.pms.mapper.AttrMapper;
import com.atguigu.gmall.pms.mapper.SkuMapper;
import com.atguigu.gmall.pms.mapper.SpuAttrValueMapper;
import com.atguigu.gmall.pms.vo.SaleAttrValueVo;
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

import com.atguigu.gmall.pms.mapper.SkuAttrValueMapper;
import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.pms.service.SkuAttrValueService;
import org.springframework.util.CollectionUtils;


@Service("skuAttrValueService")
public class SkuAttrValueServiceImpl extends ServiceImpl<SkuAttrValueMapper, SkuAttrValueEntity> implements SkuAttrValueService {

    @Autowired
    private AttrMapper attrMapper;

    @Autowired
    private SkuMapper skuMapper;

    @Autowired
    private SkuAttrValueMapper skuAttrValueMapper;
    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<SkuAttrValueEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<SkuAttrValueEntity>()
        );

        return new PageResultVo(page);
    }

    @Override
    public List<SkuAttrValueEntity> querySearchSkuAttrValueByCidAndSkuId(Long cid, Long skuId) {
        //根据cid查询出检索类型的规格参数
        List<AttrEntity> attrEntities = this.attrMapper.selectList(new QueryWrapper<AttrEntity>().eq("search_type", 1).eq("category_id", cid));
        if(CollectionUtils.isEmpty(attrEntities)){
            return null;
        }
        //获取attr表的id集合
        List<Long> ids = attrEntities.stream().map(AttrEntity::getId).collect(Collectors.toList());
        return this.list(new QueryWrapper<SkuAttrValueEntity>().eq("sku_id", skuId).in("attr_id", ids));
    }

    @Override
    public List<SaleAttrValueVo> querySaleAttrBySpuId(Long spuId) {
        List<Long> skuIds = getSkuIdsBySpuId(spuId);
        if (skuIds == null) return null;

        List<SkuAttrValueEntity> skuAttrValueEntities = this.list(new QueryWrapper<SkuAttrValueEntity>().in("sku_id", skuIds).orderByAsc("attr_id"));
        if(CollectionUtils.isEmpty(skuAttrValueEntities)){
            return null;
        }
        // 将返回的对象集合，以attrId分组，attrId-key,SkuAttrValueEntity-value
        Map<Long, List<SkuAttrValueEntity>> map = skuAttrValueEntities.stream().collect(Collectors.groupingBy(t -> t.getAttrId()));

        // 将map每个元素转换为SaleAttrValueVo
        List<SaleAttrValueVo> saleAttrValueVos = new ArrayList<>();
        map.forEach((attrId,attrValueEntities) -> {
            SaleAttrValueVo saleAttrValueVo = new SaleAttrValueVo();
            saleAttrValueVo.setAttrId(attrId);
            saleAttrValueVo.setAttrName(attrValueEntities.get(0).getAttrName());
            saleAttrValueVo.setAttrValues(attrValueEntities.stream().map(SkuAttrValueEntity::getAttrValue).collect(Collectors.toSet()));
            saleAttrValueVos.add(saleAttrValueVo);
        });
        return saleAttrValueVos;
    }

    // 抽取代码
    private List<Long> getSkuIdsBySpuId(Long spuId) {
        List<SkuEntity> skuEntities = skuMapper.selectList(new QueryWrapper<SkuEntity>().eq("spu_id", spuId));
        if(CollectionUtils.isEmpty(skuEntities)){
            return null;
        }
        // 搜集所有的sku的Id值
        List<Long> skuIds = skuEntities.stream().map(SkuEntity::getId).collect(Collectors.toList());
        return skuIds;
    }

    @Override
    public String querySpuMappingBySpuId(Long spuId) {
        List<Long> skuIds = getSkuIdsBySpuId(spuId);
        List<Map<String, Object>> maps = this.skuAttrValueMapper.querySaleAttrsMappingSkuIds(skuIds);
        // {sku_id=29, attr_values=白色,8G,256G} -> {29:'白色,8G,256G'}
        Map<String, Long> collect = maps.stream().collect(Collectors.toMap(map -> map.get("attr_values").toString(), map -> (Long) map.get("sku_id")));
        String jsonString = JSON.toJSONString(collect);
        return jsonString;
    }

}