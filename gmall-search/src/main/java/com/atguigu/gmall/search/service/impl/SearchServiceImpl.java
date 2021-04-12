package com.atguigu.gmall.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.pms.entity.BrandEntity;
import com.atguigu.gmall.pms.entity.CategoryBrandEntity;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.atguigu.gmall.search.pojo.Goods;
import com.atguigu.gmall.search.pojo.SearchParamVo;
import com.atguigu.gmall.search.pojo.SearchResponseAttrVo;
import com.atguigu.gmall.search.pojo.SearchResponseVo;
import com.atguigu.gmall.search.service.SearchService;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.search.join.ScoreMode;
import org.aspectj.weaver.ast.Var;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;


import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @Description 获取搜索参数，将结果封装返回
 * @Author rookie
 * @Date 2021/4/8 8:51
 */
@Service
public class SearchServiceImpl implements SearchService {

    @Autowired
    private RestHighLevelClient restHighLevelClient;


    @Override
    public SearchResponseVo search(SearchParamVo searchParamVo) {
        try {
            SearchRequest searchRequest = new SearchRequest(new String[]{"goods"}, this.buildDsl(searchParamVo));
            SearchResponse response = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
            SearchResponseVo responseVo = this.parseResult(response);
            //分页参数数据，searchParamVo参数才有
            responseVo.setPageNum(searchParamVo.getPageNum());
            responseVo.setPageSize(searchParamVo.getPageSize());
            // 解析搜索的结果集
            return responseVo;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    private SearchResponseVo parseResult(SearchResponse response){
        SearchResponseVo responseVo = new SearchResponseVo();
        //解析hits
        SearchHits hits = response.getHits();
        //总记录数
        responseVo.setTotal(hits.totalHits);
        //当前页数据
        SearchHit[] hitsHits = hits.getHits();
        List<Goods> goodsList = Stream.of(hitsHits).map(hitsHit -> {
            //SearchHit反序列化为Goods
            String json = hitsHit.getSourceAsString();
            Goods goods = JSON.parseObject(json, Goods.class);
            //获取高亮的标题，覆盖_source的普通title
            Map<String, HighlightField> highlightFields = hitsHit.getHighlightFields();
            HighlightField highlightField = highlightFields.get("title");
            goods.setTitle(highlightField.getFragments()[0].string());
            return goods;
        }).collect(Collectors.toList());
        responseVo.setGoodsList(goodsList);
        //解析aggregation,获取品牌、分类、过滤条件
        Aggregations aggregations = response.getAggregations();
        //把聚合结果集以map的形式解析：key-聚合名称 value-聚合的内容
        Map<String, Aggregation> aggregationMap = aggregations.asMap();
        // 2.1 获取品牌
        ParsedLongTerms brandIdAgg = (ParsedLongTerms) aggregationMap.get("brandIdAgg");
        //获取brandIdAgg的子桶
        List<? extends Terms.Bucket> brandIdAggBuckets = brandIdAgg.getBuckets();
        if(!CollectionUtils.isEmpty(brandIdAggBuckets)){
            responseVo.setBrands(brandIdAggBuckets.stream().map(bucket -> {
                BrandEntity brandEntity = new BrandEntity();
                // 外层桶的key就是品牌的id
                brandEntity.setId(bucket.getKeyAsNumber().longValue());
                // 获取到桶中的子聚合 品牌、logo的字聚合
                Map<String, Aggregation> brandSubAgeMap = bucket.getAggregations().asMap();

                //获取brandName的字聚合,每个brandName的字聚合中也是有且一个桶
                ParsedStringTerms brandNameAgg = (ParsedStringTerms) brandSubAgeMap.get("brandNameAgg");
                brandEntity.setName(brandNameAgg.getBuckets().get(0).getKeyAsString());
                // 获取logo的字聚合,每个logo的字聚合中也是有且一个桶
                ParsedStringTerms logoAgg = (ParsedStringTerms) brandSubAgeMap.get("logoAgg");
                brandEntity.setLogo(logoAgg.getBuckets().get(0).getKeyAsString());
                return brandEntity;
            }).collect(Collectors.toList()));
        }
        // 2.2 获取分类
        ParsedLongTerms categoryIdAgg = (ParsedLongTerms) aggregationMap.get("categoryIdAgg");
        List<? extends Terms.Bucket> categoryIdAggBuckets = categoryIdAgg.getBuckets();
        if(!CollectionUtils.isEmpty(categoryIdAggBuckets)){
            // 获取每个桶转化为每个分类
            responseVo.setCategories(categoryIdAggBuckets.stream().map(bucket -> {
                CategoryEntity categoryEntity = new CategoryEntity();
                // 外层桶的key就是分类的id
                categoryEntity.setId(bucket.getKeyAsNumber().longValue());

                Map<String, Aggregation> stringAggregationMap = bucket.getAggregations().asMap();
                ParsedStringTerms categoryNameAgg = (ParsedStringTerms) stringAggregationMap.get("categoryNameAgg");
                // 获取分类的字聚合,每个分类的字聚合中有且一个桶
                categoryEntity.setName(categoryNameAgg.getBuckets().get(0).getKeyAsString());
                return categoryEntity;
            }).collect(Collectors.toList()));
        }

        // 2.2 获取分类
        // 获取到规格参数的嵌套聚合
        ParsedNested attrAgg = (ParsedNested) aggregationMap.get("attrAgg");
        // 获取嵌套聚合中的attrId的聚合
        ParsedLongTerms attrIdAgg = (ParsedLongTerms) attrAgg.getAggregations().get("attrIdAgg");
        List<? extends Terms.Bucket> attrIdAggBuckets = attrIdAgg.getBuckets();
        if(!CollectionUtils.isEmpty(attrIdAggBuckets)){
            // 把attrId中的桶集合转化为自定义封装的List<SearchResponseAttrVo>
            List<SearchResponseAttrVo> searchResponseAttrVos = attrIdAggBuckets.stream().map(bucket -> {
                SearchResponseAttrVo searchResponseAttrVo = new SearchResponseAttrVo();
                Map<String, Aggregation> stringAggregationMap = bucket.getAggregations().asMap();
                //桶中的key就是attrId
                searchResponseAttrVo.setAttrId(bucket.getKeyAsNumber().longValue());
                // 获取字聚合中的attrName 和 attrValues
                //获取attrName的字聚合
                ParsedStringTerms attrNameAgg = (ParsedStringTerms) stringAggregationMap.get("attrNameAgg");
                searchResponseAttrVo.setAttrName(attrNameAgg.getBuckets().get(0).getKeyAsString());
                //获取attrValue的字聚合
                ParsedStringTerms attrValueAgg = (ParsedStringTerms) stringAggregationMap.get("attrValueAgg");
                List<? extends Terms.Bucket> attrValueAggBuckets = attrValueAgg.getBuckets();
                if(!CollectionUtils.isEmpty(attrValueAggBuckets)){
                    searchResponseAttrVo.setAttrValues(attrValueAggBuckets.stream().map(Terms.Bucket::getKeyAsString).collect(Collectors.toList()));
                }
                return searchResponseAttrVo;
            }).collect(Collectors.toList());
            responseVo.setFilters(searchResponseAttrVos);
        }
        return responseVo;
    }

    private SearchSourceBuilder buildDsl(SearchParamVo searchParamVo) {
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        String keyWord = searchParamVo.getKeyWord();
        if (StringUtils.isBlank(keyWord)) {
            //TODO: 打广告赚钱
            return sourceBuilder;
        }
        //1.构建检索条件
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        sourceBuilder.query(boolQueryBuilder);
        //1.1构建查询条件
        boolQueryBuilder.must(QueryBuilders.matchQuery("title", keyWord).operator(Operator.AND));
        //1.2构建过滤条件
        List<Long> brandId = searchParamVo.getBrandId();
        //1.2.1构建品牌过滤
        if (!CollectionUtils.isEmpty(brandId)) {
            boolQueryBuilder.filter(QueryBuilders.termsQuery("brandId", brandId));
        }
        //1.2.2构建分类过滤
        List<Long> categoryIds = searchParamVo.getCategoryId();
        if (!CollectionUtils.isEmpty(categoryIds)) {
            boolQueryBuilder.filter(QueryBuilders.termsQuery("categoryId", categoryIds));
        }
        //1.2.3构建价格区间的过滤
        Double priceFrom = searchParamVo.getPriceFrom();
        Double priceTo = searchParamVo.getPriceTo();
        if (priceFrom != null || priceTo != null) {
            RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("price");
            if (priceFrom != null) {
                rangeQuery.gte(priceFrom);
            }

            if (priceTo != null) {
                rangeQuery.lte(priceTo);
            }
            boolQueryBuilder.filter(rangeQuery);
        }
        //1.2.4构建是否有货的过滤
        Boolean store = searchParamVo.getStore();
        if (store != null) {
            if (store) {
                boolQueryBuilder.filter(QueryBuilders.termQuery("store", store));
            }
        }
        //1.2.5构建规格参数的嵌套的过滤
        List<String> props = searchParamVo.getProps();
        if (!CollectionUtils.isEmpty(props)) {
            //处理这个参数["4:8G","5:8G-16G"]
            props.forEach(prop -> {
                BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
                String[] attrs = StringUtils.split(prop, ":");
                if (attrs != null && attrs.length == 2) {
                    //分割出第一个参数attrId:4
                    boolQuery.must(QueryBuilders.termQuery("searchAttrs.attrId", attrs[0]));
                    //分割出第二个参数attrValue:8G-16G
                    String[] attrValues = StringUtils.split(attrs[1], "-");
                    boolQuery.must(QueryBuilders.termsQuery("searchAttrs.attrValue", attrValues));
                }
                boolQueryBuilder.filter(QueryBuilders.nestedQuery("searchAttrs", boolQuery, ScoreMode.None));
            });
        }
        //2.排序
        //排序字段 0默认，根据得分降序。1-价格降序，2-价格升序，3-销量的降序，4-时间的降序
        Integer sort = searchParamVo.getSort();
        switch (sort) {
            case 1: sourceBuilder.sort("price", SortOrder.DESC); break;
            case 2: sourceBuilder.sort("price", SortOrder.ASC); break;
            case 3: sourceBuilder.sort("sales", SortOrder.DESC); break;
            case 4: sourceBuilder.sort("createTime", SortOrder.DESC); break;
            default:
                sourceBuilder.sort("_score", SortOrder.DESC); break;
        }
        //3.构建分页条件
        Integer pageSize = searchParamVo.getPageSize();
        Integer pageNum = searchParamVo.getPageNum();
        sourceBuilder.from((pageNum - 1) * pageSize);
        sourceBuilder.size(pageSize);
        //4.构建高亮
        sourceBuilder.highlighter(
                new HighlightBuilder().field("title")
                        .preTags("<font style='color:red;'>")
                        .postTags("</font>"));
        //5.构建聚合
        //5.1构建品牌聚合
        sourceBuilder.aggregation(AggregationBuilders.terms("brandIdAgg").field("brandId")
                .subAggregation(AggregationBuilders.terms("brandNameAgg").field("brandName"))
                .subAggregation(AggregationBuilders.terms("logoAgg").field("logo"))
        );
        //5.2构建分类的聚合
        sourceBuilder.aggregation(AggregationBuilders.terms("categoryIdAgg").field("categoryId")
                .subAggregation(AggregationBuilders.terms("categoryNameAgg").field("categoryName"))
        );
        //5.3构建规格参数聚合
        sourceBuilder.aggregation(AggregationBuilders.nested("attrAgg", "searchAttrs")
            .subAggregation(AggregationBuilders.terms("attrIdAgg").field("searchAttrs.attrId")
                    .subAggregation(AggregationBuilders.terms("attrNameAgg").field("searchAttrs.attrName"))
                    .subAggregation(AggregationBuilders.terms("attrValueAgg").field("searchAttrs.attrValue")))
        );
        //6 构建结果集过滤
        sourceBuilder.fetchSource(new String[] {"skuId","defaultImages","price","title","subTitle"},null);
//        System.out.println(sourceBuilder);
        return sourceBuilder;
    }

}