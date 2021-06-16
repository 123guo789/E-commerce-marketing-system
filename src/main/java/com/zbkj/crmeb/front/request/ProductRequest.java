package com.zbkj.crmeb.front.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 商品搜索
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@ApiModel(value="ProductRequest对象", description="商品搜索")
public class ProductRequest implements Serializable {

    @ApiModelProperty(value = "搜索关键字")
    private String keyword;

    @ApiModelProperty(value = "分类id")
    private Integer cid;

    @ApiModelProperty(value = "价格排序", allowableValues = "range[asc,desc]")
    private String priceOrder;

    @ApiModelProperty(value = "销量排序", allowableValues = "range[asc,desc]")
    private String salesOrder;

    @ApiModelProperty(value = "是否新品")
    private Boolean news;
}
