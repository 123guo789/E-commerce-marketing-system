package com.zbkj.crmeb.payment.wechat;

import com.zbkj.crmeb.payment.vo.wechat.CreateOrderResponseVo;
import com.zbkj.crmeb.payment.vo.wechat.PayParamsVo;

/**
 * 微信支付
 */
public interface WeChatPayService {
    CreateOrderResponseVo create(PayParamsVo payParamsVo);
}
