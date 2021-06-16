package com.zbkj.crmeb.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zbkj.crmeb.system.model.SystemCity;
import com.zbkj.crmeb.system.request.SystemCityRequest;
import com.zbkj.crmeb.system.request.SystemCitySearchRequest;

/**
 * SystemCityAsyncService 接口
 */
public interface SystemCityAsyncService{

    void async(Integer id);
    void setListTree();
}
