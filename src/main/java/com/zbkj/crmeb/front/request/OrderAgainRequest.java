package com.zbkj.crmeb.front.request;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 *  OrderAgainRequest
 */
@Data
public class OrderAgainRequest {

    @ApiModelProperty(value = "订单id")
    @NotNull(message = "uni参数不能为空")
    private String nui;
}
