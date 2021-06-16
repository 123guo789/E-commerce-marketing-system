package com.zbkj.crmeb.system.service;

import com.common.PageParamRequest;
import com.zbkj.crmeb.system.model.SystemGroup;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zbkj.crmeb.system.model.SystemGroupData;
import com.zbkj.crmeb.system.request.SystemGroupSearchRequest;

import java.util.List;

/**
 * SystemGroupService 接口
 */
public interface SystemGroupService extends IService<SystemGroup> {

    List<SystemGroup> getList(SystemGroupSearchRequest request, PageParamRequest pageParamRequest);
}
