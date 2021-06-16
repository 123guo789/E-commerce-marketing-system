package com.zbkj.crmeb.system.service;

import com.common.PageParamRequest;
import com.zbkj.crmeb.express.vo.ExpressSheetVo;
import com.zbkj.crmeb.system.model.SystemConfig;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zbkj.crmeb.system.request.SystemFormCheckRequest;

import java.util.HashMap;
import java.util.List;

/**
 * SystemConfigService 接口
 */
public interface SystemConfigService extends IService<SystemConfig> {
    List<SystemConfig> getList(PageParamRequest pageParamRequest);

    String getValueByKey(String key);

    /**
     * 同时获取多个配置
     * @param keys 多个配置key
     * @return 查询到的多个结果
     */
    List<String> getValuesByKes(List<String> keys);

    boolean updateOrSaveValueByName(String name, String value);

    String getValueByKeyException(String key);

    boolean saveForm(SystemFormCheckRequest systemFormCheckRequest);

    HashMap<String, String> info(Integer formId);

    boolean checkName(String name);

    /**
     * 获取系统电子面单信息
     */
    ExpressSheetVo getExpressSheet();

    /**
     * 获取面单默认配置信息
     * @return
     */
    ExpressSheetVo getDeliveryInfo();
}
