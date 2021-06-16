package com.zbkj.crmeb.store.response;

import io.swagger.annotations.Api;
import lombok.Data;

/**
 * 订单统计 response item
 */
@Data
public class StoreOrderStatisticsChartItemResponse {

    private String num;
    private String time;
}
