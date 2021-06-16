package com.zbkj.crmeb.finance.service;

import com.common.PageParamRequest;
import com.github.pagehelper.PageInfo;
import com.zbkj.crmeb.finance.model.UserRecharge;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zbkj.crmeb.finance.request.UserRechargeRefundRequest;
import com.zbkj.crmeb.finance.request.UserRechargeSearchRequest;
import com.zbkj.crmeb.finance.response.UserRechargeResponse;
import com.zbkj.crmeb.front.request.UserRechargeRequest;

import java.math.BigDecimal;
import java.util.HashMap;

/**
* UserRechargeService 接口
*/
public interface UserRechargeService extends IService<UserRecharge> {

    PageInfo<UserRechargeResponse> getList(UserRechargeSearchRequest request, PageParamRequest pageParamRequest);

    HashMap<String, BigDecimal> getBalanceList();

    UserRecharge getInfoByEntity(UserRecharge userRecharge);

    UserRecharge create(UserRechargeRequest request);

    Boolean complete(UserRecharge userRecharge);

    BigDecimal getSumBigDecimal(Integer uid);

    /**
     * 充值退款
     * @param request 退款参数
     * @return
     */
    Boolean refund(UserRechargeRefundRequest request);
}
