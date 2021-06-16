package com.zbkj.crmeb.user.service;

import com.common.PageParamRequest;
import com.zbkj.crmeb.user.model.UserTag;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * UserTagService 接口实现
 */
public interface UserTagService extends IService<UserTag> {

    List<UserTag> getList(PageParamRequest pageParamRequest);

    String clean(String tagIdValue);

    String getGroupNameInId(String tagIdValue);
}