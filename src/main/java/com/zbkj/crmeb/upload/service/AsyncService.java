package com.zbkj.crmeb.upload.service;

import com.zbkj.crmeb.system.model.SystemAttachment;
import java.util.List;

/**
 * AsyncService 接口
 */
public interface AsyncService {
    void async(List<SystemAttachment> systemAttachmentList);

    String getCurrentBaseUrl();
}