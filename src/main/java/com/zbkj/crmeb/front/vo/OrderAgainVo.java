package com.zbkj.crmeb.front.vo;

import com.zbkj.crmeb.store.model.StoreOrder;
import com.zbkj.crmeb.store.response.StoreCartResponse;
import com.zbkj.crmeb.store.vo.StoreOrderInfoVo;
import lombok.Data;

import java.util.List;

/**
 * 再次下单VO对象
 */
@Data
public class OrderAgainVo {
    private StoreOrder storeOrder;
    private List<StoreOrderInfoVo> cartInfo;
    private OrderAgainItemVo status;
    private String payTime;
    private String addTime;
    private String statusPic;
    private Integer offlinePayStatus;
}
