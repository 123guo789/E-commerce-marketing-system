package com.zbkj.crmeb.user.service;

import com.common.PageParamRequest;
import com.zbkj.crmeb.user.model.UserGroup;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * UserGroupService 接口实现
 */
public interface UserGroupService extends IService<UserGroup> {

    List<UserGroup> getList(PageParamRequest pageParamRequest);

    String clean(String groupIdValue);

    String getGroupNameInId(String groupIdValue);
}