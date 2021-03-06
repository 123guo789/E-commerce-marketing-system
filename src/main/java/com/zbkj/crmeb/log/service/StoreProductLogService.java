package com.zbkj.crmeb.log.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zbkj.crmeb.log.model.StoreProductLog;

/**
 * StoreProductLogService 接口
 */
public interface StoreProductLogService extends IService<StoreProductLog> {

//    List<StoreProductLog> getList(StoreProductLogSearchRequest request, PageParamRequest pageParamRequest);

    Integer getCountByTimeAndType(String time, String type);

    void addLogTask();
}