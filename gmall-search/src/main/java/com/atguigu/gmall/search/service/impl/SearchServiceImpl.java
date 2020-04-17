package com.atguigu.gmall.search.service.impl;

import com.alibaba.csp.sentinel.util.StringUtil;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.search.entity.Goods;
import com.atguigu.gmall.search.entity.SearchParam;
import com.atguigu.gmall.search.entity.SearchResponseAttrVo;
import com.atguigu.gmall.search.entity.SearchResponseVo;
import com.atguigu.gmall.search.service.SearchService;
import org.apache.lucene.search.join.ScoreMode;
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
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SearchServiceImpl implements SearchService {

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    @Override
    public SearchResponseVo search(SearchParam searchParam) {
        try {
            SearchRequest searchRequest = new SearchRequest(new String[]{"goods"}, buildDSL(searchParam));
            SearchResponse searchResponse = this.restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
            //解析响应封装数据
            SearchResponseVo searchResponseVo = parseResult(searchResponse);
            searchResponseVo.setPageNum(searchParam.getPageNum());
            searchResponseVo.setPageSize(searchParam.getPageSize());
            return searchResponseVo;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    //解析响应结果的方法
    private SearchResponseVo parseResult(SearchResponse searchResponse) {
        SearchResponseVo searchResponseVo = new SearchResponseVo();
        SearchHits hits = searchResponse.getHits();
        //设置总记录数
        searchResponseVo.setTotal(hits.getTotalHits());
        //设置data数据
        SearchHit[] hitsHits = hits.getHits();
        List<Goods> goodsList = Arrays.asList(hitsHits).stream().map(hitHit -> {
            String goodsAsString = hitHit.getSourceAsString();
            Goods goods = JSON.parseObject(goodsAsString, Goods.class);//反序列化为对象
            Map<String, HighlightField> highlightFields = hitHit.getHighlightFields();
            goods.setTitle(highlightFields.get("title").getFragments()[0].toString());
            return goods;
        }).collect(Collectors.toList());
        searchResponseVo.setData(goodsList);
        //解析聚合结果集的品牌，分类，属性
        Map<String, Aggregation> aggregationMap = searchResponse.getAggregations().getAsMap();
        ParsedLongTerms brandIdAgg = (ParsedLongTerms) aggregationMap.get("brandIdAgg");
        List<? extends Terms.Bucket> brandIdAggBuckets = brandIdAgg.getBuckets();
        if (!CollectionUtils.isEmpty(brandIdAggBuckets)) {
            List<String> brandValues = brandIdAggBuckets.stream().map(brandIdAggBucket -> {
                Map<String, Object> map = new HashMap<>();
                //品牌id
                map.put("brandId", ((Terms.Bucket) brandIdAggBucket).getKeyAsNumber().longValue());
                //品牌名字
                ParsedStringTerms brandNameAgg = (ParsedStringTerms) ((Terms.Bucket) brandIdAggBucket).getAggregations().get("brandNameAgg");
                map.put("brandName", brandNameAgg.getBuckets().get(0).getKeyAsString());
                //品牌logo
                ParsedStringTerms logoAgg = (ParsedStringTerms) ((Terms.Bucket) brandIdAggBucket).getAggregations().get("logoAgg");
                map.put("logo", logoAgg.getBuckets().get(0).getKeyAsString());
                return JSON.toJSONString(map);
            }).collect(Collectors.toList());
            SearchResponseAttrVo searchResponseAttrVo = new SearchResponseAttrVo();
            searchResponseAttrVo.setAttrValues(brandValues);
            searchResponseAttrVo.setAttrName("品牌");
            searchResponseAttrVo.setAttrId(null);
            searchResponseVo.setBrand(searchResponseAttrVo);
        }

        //解析聚合结果中的分类
        ParsedLongTerms categoryIdAgg = (ParsedLongTerms) aggregationMap.get("categoryIdAgg");
        List<? extends Terms.Bucket> categoryIdAggBuckets = categoryIdAgg.getBuckets();
        if (!CollectionUtils.isEmpty(categoryIdAggBuckets)) {
            List<String> categoryValues = categoryIdAggBuckets.stream().map(categoryIdAggBucket -> {
                Map<String, Object> map = new HashMap<>();
                //分类id
                long categoryId = ((Terms.Bucket) categoryIdAggBucket).getKeyAsNumber().longValue();
                map.put("categoryId", categoryId);
                ParsedStringTerms categoryNameAgg = (ParsedStringTerms) ((Terms.Bucket) categoryIdAggBucket).getAggregations().get("categoryNameAgg");
                map.put("categoryName", categoryNameAgg.getBuckets().get(0).getKeyAsString());
                return JSON.toJSONString(map);
            }).collect(Collectors.toList());
            SearchResponseAttrVo searchResponseAttrVo = new SearchResponseAttrVo();
            searchResponseAttrVo.setAttrId(null);
            searchResponseAttrVo.setAttrName("分类");
            searchResponseAttrVo.setAttrValues(categoryValues);
            searchResponseVo.setCategory(searchResponseAttrVo);
        }

        //解析属性集
        ParsedNested attrsAgg = (ParsedNested) aggregationMap.get("attrsAgg");
        ParsedLongTerms attrIdAgg = (ParsedLongTerms) attrsAgg.getAggregations().get("attrIdAgg");
        List<? extends Terms.Bucket> attrIdAggBuckets = attrIdAgg.getBuckets();
        if (!CollectionUtils.isEmpty(attrIdAggBuckets)) {
            List<SearchResponseAttrVo> searchResponseAttrVoList = attrIdAggBuckets.stream().map(attrIdAggBucket -> {
                SearchResponseAttrVo searchResponseAttrVo = new SearchResponseAttrVo();
                //设置属性id
                searchResponseAttrVo.setAttrId(((Terms.Bucket) attrIdAggBucket).getKeyAsNumber().longValue());
                ParsedStringTerms attrNameAgg = (ParsedStringTerms) ((Terms.Bucket) attrIdAggBucket).getAggregations().get("attrNameAgg");
                searchResponseAttrVo.setAttrName(attrNameAgg.getBuckets().get(0).getKeyAsString());
                ParsedStringTerms attrValueAgg = (ParsedStringTerms) ((Terms.Bucket) attrIdAggBucket).getAggregations().get("attrValueAgg");
                List<? extends Terms.Bucket> attrValueAggBuckets = attrValueAgg.getBuckets();
                if (!CollectionUtils.isEmpty(attrValueAggBuckets)) {
                    List<String> attrValues = attrValueAggBuckets.stream().map(Terms.Bucket::getKeyAsString).collect(Collectors.toList());
                    searchResponseAttrVo.setAttrValues(attrValues);
                }
                return searchResponseAttrVo;
            }).collect(Collectors.toList());
            searchResponseVo.setAttrs(searchResponseAttrVoList);
        }
        System.err.println("解析后的searchResponseVo："+searchResponseVo);
        return searchResponseVo;
    }

    private SearchSourceBuilder buildDSL(SearchParam searchParam) {
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        searchSourceBuilder.query(boolQueryBuilder);
        String keyword = searchParam.getKeyword();
        if (StringUtils.isEmpty(keyword)) {
            return null;
        }
        //1.依据标题构建布尔查询
        boolQueryBuilder.must(QueryBuilders.matchQuery("title", keyword).operator(Operator.AND));
        //2.过滤查询
        //2.1品牌过滤
        List<Long> brandId = searchParam.getBrandId();
        if (!CollectionUtils.isEmpty(brandId)) {
            boolQueryBuilder.filter(QueryBuilders.termsQuery("brandId", brandId));
        }
        //2.2分类过滤
        Long categoryId = searchParam.getCategoryId();
        if (categoryId != null) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("categoryId", categoryId));
        }
        //2.3价格过滤
        Double priceFrom = searchParam.getPriceFrom();
        Double priceTo = searchParam.getPriceTo();
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
        //2.4是否有货过滤
        Boolean store = searchParam.getStore();
        if (store != null) {
            boolQueryBuilder.filter(QueryBuilders.termsQuery("store", store));
        }
        //2.5属性嵌套过滤
        List<String> props = searchParam.getProps();
        if (!CollectionUtils.isEmpty(props)) {
            props.forEach(prop -> {
                String[] attrs = StringUtils.split(prop, ":");
                if (attrs != null || attrs.length == 2) {
                    String attrId = attrs[0];
                    String attrValueString = attrs[1];
                    String[] attrValues = StringUtils.delimitedListToStringArray(attrValueString, "-");
                    BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
                    boolQuery.must(QueryBuilders.termQuery("attrs.attrId", attrId));
                    boolQuery.must(QueryBuilders.termsQuery("attrs.attrValue", attrValues));
                    boolQueryBuilder.filter(QueryBuilders.nestedQuery("attrs", boolQuery, ScoreMode.None));
                }
            });
        }
        //2.6排序查询
        String order = searchParam.getOrder();
        if (StringUtil.isNotBlank(order)) {
            String[] orderString = StringUtils.split(order, ":");
            String sortField = "_score";
            if (orderString != null && orderString.length == 2) {
                switch (orderString[0]) {
                    case "1":
                        sortField = "price";
                        break;
                    case "2":
                        sortField = "sale";
                        break;
                    case "3":
                        sortField = "createTime";
                        break;
                }

                searchSourceBuilder.sort(sortField, StringUtil.equals("desc", orderString[1]) ? SortOrder.DESC : SortOrder.ASC);
            }
        }
        //3.分页查询
        Integer pageNum = searchParam.getPageNum();
        Integer pageSize = searchParam.getPageSize();
        searchSourceBuilder.from((pageNum - 1) * pageSize);
        searchSourceBuilder.size(pageSize);

        //4.构建高亮集合
        searchSourceBuilder.highlighter(new HighlightBuilder()
                .field("title")
                .preTags("<p style='color:red;'>")
                .postTags("</P>"));

        //5.构建聚合
        //5.1构建品牌聚合
        searchSourceBuilder.aggregation(AggregationBuilders.terms("brandIdAgg").field("brandId")
                .subAggregation(AggregationBuilders.terms("brandNameAgg").field("brandName"))
                .subAggregation(AggregationBuilders.terms("logoAgg").field("logo")));

        //5.2构建分类聚合
        searchSourceBuilder.aggregation(AggregationBuilders.terms("categoryIdAgg").field("categoryId")
                .subAggregation(AggregationBuilders.terms("categoryNameAgg").field("categoryName")));

        //5.3构建嵌套属性聚合
        searchSourceBuilder.aggregation(AggregationBuilders.nested("attrsAgg", "attrs")
                .subAggregation(AggregationBuilders.terms("attrIdAgg").field("attrs.attrId")
                        .subAggregation(AggregationBuilders.terms("attrNameAgg").field("attrs.attrName"))
                        .subAggregation(AggregationBuilders.terms("attrValueAgg").field("attrs.attrValue"))));

        //6.构建结果集过滤
        searchSourceBuilder.fetchSource(new String[]{"skuId", "defaultImage", "title", "price"}, null);
        System.err.println(searchSourceBuilder.toString());
        return searchSourceBuilder;
    }
}
