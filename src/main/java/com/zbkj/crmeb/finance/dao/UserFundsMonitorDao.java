package com.zbkj.crmeb.finance.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zbkj.crmeb.finance.model.UserFundsMonitor;

import java.util.HashMap;
import java.util.List;

/**
 * 用户充值表 Mapper 接口
 */
public interface UserFundsMonitorDao extends BaseMapper<UserFundsMonitor> {

    /**
     * 佣金列表
     * @return List<User>
     */
    List<UserFundsMonitor> getFundsMonitor(HashMap<String, Object> map);
}
