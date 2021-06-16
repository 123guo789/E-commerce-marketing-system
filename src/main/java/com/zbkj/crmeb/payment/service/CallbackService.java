package com.zbkj.crmeb.payment.service;

/**
 * 订单支付回调 service
 */
public interface CallbackService {
    /**
     * 微信支付回调
     * @param xmlInfo 微信会到json
     * @return String
     */
    String weChat(String xmlInfo);

    /**
     * 支付宝支付回调
     * @param request
     * @return
     */
    boolean aliPay(String request);
}
