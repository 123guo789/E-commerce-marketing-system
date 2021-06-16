package com.zbkj.crmeb.payment.service;

import com.zbkj.crmeb.finance.model.UserRecharge;
import com.zbkj.crmeb.payment.vo.wechat.CreateOrderResponseVo;
import com.zbkj.crmeb.user.model.UserToken;

/**
 * 订单支付
 */
public interface RechargePayService {
    CreateOrderResponseVo payOrder(Integer orderId, String payType, String clientIp);

    boolean success(String orderId, Integer userId, String payType);

    /**
     * 支付成功处理
     * @param userRecharge 充值订单
     * @param userToken 用户Token
     */
    Boolean paySuccess(UserRecharge userRecharge, UserToken userToken);
}
