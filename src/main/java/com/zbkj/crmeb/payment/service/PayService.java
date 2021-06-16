package com.zbkj.crmeb.payment.service;

import com.zbkj.crmeb.payment.vo.wechat.CreateOrderResponseVo;

/**
 * 订单支付
 */
public abstract class PayService {
    public abstract CreateOrderResponseVo payOrder(Integer orderId, String from, String clientIp);

    public abstract boolean success(String orderId, Integer userId, String payType);
}
