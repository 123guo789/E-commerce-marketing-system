package com.zbkj.crmeb.payment.service;

import com.zbkj.crmeb.payment.vo.wechat.CreateOrderResponseVo;
import com.zbkj.crmeb.store.model.StoreOrder;
import com.zbkj.crmeb.user.model.User;
import com.zbkj.crmeb.user.model.UserToken;

/**
 * 订单支付
 */
public interface OrderPayService{
    CreateOrderResponseVo payOrder(Integer orderId, String fromType, String clientIp);

    boolean success(String orderId, Integer userId, String payType);

    void afterPaySuccess();

    /**
     * 支付成功处理
     * @param storeOrder 订单
     * @param user  用户
     * @param userToken 用户Token
     */
    Boolean paySuccess(StoreOrder storeOrder, User user, UserToken userToken);
}
