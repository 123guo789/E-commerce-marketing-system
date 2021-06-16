package com.zbkj.crmeb.front.response;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 计算订单价格 response
 */
@Data
public class ComputeOrderResponse {
    private BigDecimal totalPrice;
        // 支付金额
        private BigDecimal payPrice;
        // 邮费金额
        private BigDecimal payPostage;
        // 优惠券金额
        private BigDecimal couponPrice;
        // 扣除金额
        private BigDecimal deductionPrice;
        // 使用金粉
        private Integer usedIntegral;
        // 剩余积分
        private Integer SurplusIntegral;
}
