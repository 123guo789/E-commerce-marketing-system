package com.zbkj.crmeb.upload.service;

import com.qiniu.storage.UploadManager;
import com.zbkj.crmeb.upload.vo.CloudVo;

/**
 * QiNiuService 接口
 */
public interface QiNiuService {
    void upload(UploadManager uploadManager, CloudVo cloudVo, String upToken, String webPth, String localFile, Integer id);
}