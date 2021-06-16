package com.zbkj.crmeb.front.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 附近的门店
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@ApiModel(value="StoreNearRequest对象", description="附近的门店")
public class StoreNearRequest implements Serializable {

    private static final long serialVersionUID=1L;

    @ApiModelProperty(value = "经度")
    private String latitude;

    @ApiModelProperty(value = "纬度")
    private String longitude;
}
