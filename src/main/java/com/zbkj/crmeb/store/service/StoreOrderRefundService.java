package com.zbkj.crmeb.store.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zbkj.crmeb.store.model.StoreOrder;
import com.zbkj.crmeb.store.request.StoreOrderRefundRequest;


/**
 * StoreOrderRefundService 接口
 */
public interface StoreOrderRefundService extends IService<StoreOrder> {
    void refund(StoreOrderRefundRequest request, StoreOrder storeOrder);
}
