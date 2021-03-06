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
 * @Description ??????????????????????????????????????????
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
            //?????????????????????searchParamVo????????????
            responseVo.setPageNum(searchParamVo.getPageNum());
            responseVo.setPageSize(searchParamVo.getPageSize());
            // ????????????????????????
            return responseVo;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    private SearchResponseVo parseResult(SearchResponse response){
        SearchResponseVo responseVo = new SearchResponseVo();
        //??????hits
        SearchHits hits = response.getHits();
        //????????????
        responseVo.setTotal(hits.totalHits);
        //???????????????
        SearchHit[] hitsHits = hits.getHits();
        List<Goods> goodsList = Stream.of(hitsHits).map(hitsHit -> {
            //SearchHit???????????????Goods
            String json = hitsHit.getSourceAsString();
            Goods goods = JSON.parseObject(json, Goods.class);
            //??????????????????????????????_source?????????title
            Map<String, HighlightField> highlightFields = hitsHit.getHighlightFields();
            HighlightField highlightField = highlightFields.get("title");
            goods.setTitle(highlightField.getFragments()[0].string());
            return goods;
        }).collect(Collectors.toList());
        responseVo.setGoodsList(goodsList);
        //??????aggregation,????????????????????????????????????
        Aggregations aggregations = response.getAggregations();
        //?????????????????????map??????????????????key-???????????? value-???????????????
        Map<String, Aggregation> aggregationMap = aggregations.asMap();
        // 2.1 ????????????
        ParsedLongTerms brandIdAgg = (ParsedLongTerms) aggregationMap.get("brandIdAgg");
        //??????brandIdAgg?????????
        List<? extends Terms.Bucket> brandIdAggBuckets = brandIdAgg.getBuckets();
        if(!CollectionUtils.isEmpty(brandIdAggBuckets)){
            responseVo.setBrands(brandIdAggBuckets.stream().map(bucket -> {
                BrandEntity brandEntity = new BrandEntity();
                // ????????????key???????????????id
                brandEntity.setId(bucket.getKeyAsNumber().longValue());
                // ??????????????????????????? ?????????logo????????????
                Map<String, Aggregation> brandSubAgeMap = bucket.getAggregations().asMap();

                //??????brandName????????????,??????brandName????????????????????????????????????
                ParsedStringTerms brandNameAgg = (ParsedStringTerms) brandSubAgeMap.get("brandNameAgg");
                brandEntity.setName(brandNameAgg.getBuckets().get(0).getKeyAsString());
                // ??????logo????????????,??????logo????????????????????????????????????
                ParsedStringTerms logoAgg = (ParsedStringTerms) brandSubAgeMap.get("logoAgg");
                brandEntity.setLogo(logoAgg.getBuckets().get(0).getKeyAsString());
                return brandEntity;
            }).collect(Collectors.toList()));
        }
        // 2.2 ????????????
        ParsedLongTerms categoryIdAgg = (ParsedLongTerms) aggregationMap.get("categoryIdAgg");
        List<? extends Terms.Bucket> categoryIdAggBuckets = categoryIdAgg.getBuckets();
        if(!CollectionUtils.isEmpty(categoryIdAggBuckets)){
            // ????????????????????????????????????
            responseVo.setCategories(categoryIdAggBuckets.stream().map(bucket -> {
                CategoryEntity categoryEntity = new CategoryEntity();
                // ????????????key???????????????id
                categoryEntity.setId(bucket.getKeyAsNumber().longValue());

                Map<String, Aggregation> stringAggregationMap = bucket.getAggregations().asMap();
                ParsedStringTerms categoryNameAgg = (ParsedStringTerms) stringAggregationMap.get("categoryNameAgg");
                // ????????????????????????,??????????????????????????????????????????
                categoryEntity.setName(categoryNameAgg.getBuckets().get(0).getKeyAsString());
                return categoryEntity;
            }).collect(Collectors.toList()));
        }

        // 2.2 ????????????
        // ????????????????????????????????????
        ParsedNested attrAgg = (ParsedNested) aggregationMap.get("attrAgg");
        // ????????????????????????attrId?????????
        ParsedLongTerms attrIdAgg = (ParsedLongTerms) attrAgg.getAggregations().get("attrIdAgg");
        List<? extends Terms.Bucket> attrIdAggBuckets = attrIdAgg.getBuckets();
        if(!CollectionUtils.isEmpty(attrIdAggBuckets)){
            // ???attrId??????????????????????????????????????????List<SearchResponseAttrVo>
            List<SearchResponseAttrVo> searchResponseAttrVos = attrIdAggBuckets.stream().map(bucket -> {
                SearchResponseAttrVo searchResponseAttrVo = new SearchResponseAttrVo();
                Map<String, Aggregation> stringAggregationMap = bucket.getAggregations().asMap();
                //?????????key??????attrId
                searchResponseAttrVo.setAttrId(bucket.getKeyAsNumber().longValue());
                // ?????????????????????attrName ??? attrValues
                //??????attrName????????????
                ParsedStringTerms attrNameAgg = (ParsedStringTerms) stringAggregationMap.get("attrNameAgg");
                searchResponseAttrVo.setAttrName(attrNameAgg.getBuckets().get(0).getKeyAsString());
                //??????attrValue????????????
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
            //TODO: ???????????????
            return sourceBuilder;
        }
        //1.??????????????????
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        sourceBuilder.query(boolQueryBuilder);
        //1.1??????????????????
        boolQueryBuilder.must(QueryBuilders.matchQuery("title", keyWord).operator(Operator.AND));
        //1.2??????????????????
        List<Long> brandId = searchParamVo.getBrandId();
        //1.2.1??????????????????
        if (!CollectionUtils.isEmpty(brandId)) {
            boolQueryBuilder.filter(QueryBuilders.termsQuery("brandId", brandId));
        }
        //1.2.2??????????????????
        List<Long> categoryIds = searchParamVo.getCategoryId();
        if (!CollectionUtils.isEmpty(categoryIds)) {
            boolQueryBuilder.filter(QueryBuilders.termsQuery("categoryId", categoryIds));
        }
        //1.2.3???????????????????????????
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
        //1.2.4???????????????????????????
        Boolean store = searchParamVo.getStore();
        if (store != null) {
            if (store) {
                boolQueryBuilder.filter(QueryBuilders.termQuery("store", store));
            }
        }
        //1.2.5????????????????????????????????????
        List<String> props = searchParamVo.getProps();
        if (!CollectionUtils.isEmpty(props)) {
            //??????????????????["4:8G","5:8G-16G"]
            props.forEach(prop -> {
                BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
                String[] attrs = StringUtils.split(prop, ":");
                if (attrs != null && attrs.length == 2) {
                    //????????????????????????attrId:4
                    boolQuery.must(QueryBuilders.termQuery("searchAttrs.attrId", attrs[0]));
                    //????????????????????????attrValue:8G-16G
                    String[] attrValues = StringUtils.split(attrs[1], "-");
                    boolQuery.must(QueryBuilders.termsQuery("searchAttrs.attrValue", attrValues));
                }
                boolQueryBuilder.filter(QueryBuilders.nestedQuery("searchAttrs", boolQuery, ScoreMode.None));
            });
        }
        //2.??????
        //???????????? 0??????????????????????????????1-???????????????2-???????????????3-??????????????????4-???????????????
        Integer sort = searchParamVo.getSort();
        switch (sort) {
            case 1: sourceBuilder.sort("price", SortOrder.DESC); break;
            case 2: sourceBuilder.sort("price", SortOrder.ASC); break;
            case 3: sourceBuilder.sort("sales", SortOrder.DESC); break;
            case 4: sourceBuilder.sort("createTime", SortOrder.DESC); break;
            default:
                sourceBuilder.sort("_score", SortOrder.DESC); break;
        }
        //3.??????????????????
        Integer pageSize = searchParamVo.getPageSize();
        Integer pageNum = searchParamVo.getPageNum();
        sourceBuilder.from((pageNum - 1) * pageSize);
        sourceBuilder.size(pageSize);
        //4.????????????
        sourceBuilder.highlighter(
                new HighlightBuilder().field("title")
                        .preTags("<font style='color:red;'>")
                        .postTags("</font>"));
        //5.????????????
        //5.1??????????????????
        sourceBuilder.aggregation(AggregationBuilders.terms("brandIdAgg").field("brandId")
                .subAggregation(AggregationBuilders.terms("brandNameAgg").field("brandName"))
                .subAggregation(AggregationBuilders.terms("logoAgg").field("logo"))
        );
        //5.2?????????????????????
        sourceBuilder.aggregation(AggregationBuilders.terms("categoryIdAgg").field("categoryId")
                .subAggregation(AggregationBuilders.terms("categoryNameAgg").field("categoryName"))
        );
        //5.3????????????????????????
        sourceBuilder.aggregation(AggregationBuilders.nested("attrAgg", "searchAttrs")
            .subAggregation(AggregationBuilders.terms("attrIdAgg").field("searchAttrs.attrId")
                    .subAggregation(AggregationBuilders.terms("attrNameAgg").field("searchAttrs.attrName"))
                    .subAggregation(AggregationBuilders.terms("attrValueAgg").field("searchAttrs.attrValue")))
        );
        //6 ?????????????????????
        sourceBuilder.fetchSource(new String[] {"skuId","defaultImages","price","title","subTitle"},null);
//        System.out.println(sourceBuilder);
        return sourceBuilder;
    }

}