package com.zbkj.crmeb.upload.service;

import com.aliyun.oss.OSS;
import com.zbkj.crmeb.upload.vo.CloudVo;

/**
 * OssService 接口
 */
public interface OssService {
    void upload(CloudVo cloudVo, String webPth, String localFile, Integer id);
}