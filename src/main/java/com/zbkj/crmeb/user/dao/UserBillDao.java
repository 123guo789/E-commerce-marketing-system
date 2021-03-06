package com.zbkj.crmeb.user.dao;

import com.zbkj.crmeb.user.model.UserBill;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zbkj.crmeb.user.response.UserBillResponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 用户账单表 Mapper 接口
 */
public interface UserBillDao extends BaseMapper<UserBill> {

    List<UserBillResponse> getListAdmin(Map<String, Object> map);

    List<UserBillResponse> getListAdminAndIntegeal(Map<String, Object> map);
}
