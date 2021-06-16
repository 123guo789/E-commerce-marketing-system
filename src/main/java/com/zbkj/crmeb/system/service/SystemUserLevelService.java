package com.zbkj.crmeb.system.service;

import com.common.PageParamRequest;
import com.zbkj.crmeb.system.model.SystemUserLevel;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zbkj.crmeb.system.request.SystemUserLevelRequest;
import com.zbkj.crmeb.system.request.SystemUserLevelSearchRequest;

import java.util.List;

/**
 * SystemUserLevelService 接口
 */
public interface SystemUserLevelService extends IService<SystemUserLevel> {

    List<SystemUserLevel> getList(SystemUserLevelSearchRequest request, PageParamRequest pageParamRequest);

    List<SystemUserLevel> getGradeListByLevelId(Integer levelId);

    boolean create(SystemUserLevelRequest request);

    boolean update(Integer id, SystemUserLevelRequest request);

    SystemUserLevel getByLevelId(Integer levelId);
}
