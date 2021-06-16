package com.zbkj.crmeb.front.response;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 价格计算集合
 */
@Data
public class PriceGroupResponse {
    private BigDecimal storePostage;
    private BigDecimal storeFreePostage;
    private BigDecimal totalPrice;
    private BigDecimal costPrice;
    private BigDecimal vipPrice;
    private BigDecimal couponPrice;
    private BigDecimal payPrice;
    private BigDecimal payPostage;
    private BigDecimal deductionPrice;
    private Integer usedIntegral;
}
