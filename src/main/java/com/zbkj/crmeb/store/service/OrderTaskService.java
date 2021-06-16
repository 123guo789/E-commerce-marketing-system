package com.zbkj.crmeb.store.service;


/**
 * 订单任务服务 StoreOrderService 接口
 */
 public interface OrderTaskService{

     void cancelByUser();

     void refundApply();

     void complete();

     void takeByUser();

     void deleteByUser();
 }
