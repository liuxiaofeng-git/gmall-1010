package com.atguigu.gmall.search.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.Date;
import java.util.List;

@Data
@Document(indexName = "goods", shards = 3, replicas = 2, type = "info")
public class Goods {

    @Id
    private Long skuId;

    @Field(type = FieldType.Keyword, index = false)
    private String defaultImage;

    @Field(type = FieldType.Text,analyzer = "ik_max_word")
    private String title;

    @Field(type = FieldType.Double)
    private Double price;

    @Field(type = FieldType.Long)
    private Long sales = 0L;

    @Field(type = FieldType.Boolean)
    private Boolean store = false;//是否有库存，默认没有

    @Field(type = FieldType.Date)
    private Date createTime;

    @Field(type = FieldType.Long)
    private Long brandId;

    @Field(type = FieldType.Keyword)
    private String brandName;

    @Field(type = FieldType.Keyword)
    private String logo;

    @Field(type = FieldType.Long)
    private Long categoryId;

    @Field(type = FieldType.Keyword)
    private String categoryName;

    @Field(type = FieldType.Nested)
    private List<SearchAttrValue> attrs;
}
