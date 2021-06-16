package com.zbkj.crmeb.express.service;

import com.zbkj.crmeb.express.vo.LogisticsResultVo;

/**
* ExpressService 接口
*/
public interface LogisticService {
    LogisticsResultVo info(String expressNo, String type, String com, String phone);
}
