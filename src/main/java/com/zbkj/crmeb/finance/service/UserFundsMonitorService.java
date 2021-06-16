package com.zbkj.crmeb.finance.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.common.PageParamRequest;
import com.zbkj.crmeb.finance.model.UserFundsMonitor;
import com.zbkj.crmeb.finance.model.UserRecharge;
import com.zbkj.crmeb.finance.request.FundsMonitorUserSearchRequest;

import java.util.List;

/**
*  UserRechargeService 接口
*/
public interface UserFundsMonitorService extends IService<UserFundsMonitor> {

    /**
     * 佣金列表
     * @return List<User>
     */
    List<UserFundsMonitor> getFundsMonitor(FundsMonitorUserSearchRequest request, PageParamRequest pageParamRequest);
}
