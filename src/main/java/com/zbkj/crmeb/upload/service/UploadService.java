package com.zbkj.crmeb.upload.service;

import com.zbkj.crmeb.upload.vo.FileResultVo;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * UploadService 接口
 */
public interface UploadService {
    FileResultVo image(MultipartFile multipart, String model, Integer pid) throws IOException;

    FileResultVo file(MultipartFile multipart, String model, Integer pid) throws IOException;
}
