package com.zbkj.crmeb.wechat.service;

import com.common.PageParamRequest;
import com.zbkj.crmeb.wechat.model.WechatQrcode;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zbkj.crmeb.wechat.request.WechatQrcodeSearchRequest;

import java.util.List;

/**
 *  WechatQrcodeService 接口
 */
public interface WechatQrcodeService extends IService<WechatQrcode> {

    List<WechatQrcode> getList(WechatQrcodeSearchRequest request, PageParamRequest pageParamRequest);
}