package com.zbkj.crmeb.front.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotNull;

/**
 * 支付订单参数
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@ApiModel(value="OrderPayRequest对象", description="订单支付")
public class OrderPayRequest {

    @ApiModelProperty(value = "订单id")
    @NotNull(message = "订单id不能为空")
    private String uni;

    @ApiModelProperty(value = "支付类型")
    private String paytype;

    @ApiModelProperty(value = "支付平台")
    private String from;
}
