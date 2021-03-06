package com.zbkj.crmeb.wechat.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.common.PageParamRequest;
import com.zbkj.crmeb.wechat.model.WechatProgramPublicTemp;
import com.zbkj.crmeb.wechat.request.WechatProgramPublicTempSearchRequest;

import java.util.List;

/**
 *  WechatProgramPublicTempService 接口
 */
public interface WechatProgramPublicTempService extends IService<WechatProgramPublicTemp> {

    List<WechatProgramPublicTemp> getList(WechatProgramPublicTempSearchRequest request, PageParamRequest pageParamRequest);

    void async();
}