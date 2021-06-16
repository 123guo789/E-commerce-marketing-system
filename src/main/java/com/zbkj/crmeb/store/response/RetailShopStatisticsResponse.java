package com.zbkj.crmeb.store.response;

import lombok.Data;

/**
 * 分销头部数据Response
 */
@Data
public class RetailShopStatisticsResponse {
    public RetailShopStatisticsResponse() {
    }

    public RetailShopStatisticsResponse(String name, Integer count) {
        this.name = name;
        this.count = count;
    }

    private String name;
    private Integer count;
//    private String className;
}
