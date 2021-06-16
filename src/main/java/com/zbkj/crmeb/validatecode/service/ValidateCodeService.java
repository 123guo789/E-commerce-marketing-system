package com.zbkj.crmeb.validatecode.service;

import com.zbkj.crmeb.validatecode.model.ValidateCode;

import java.util.HashMap;

/**
 * ValidateCodeService 接口
 */
public interface ValidateCodeService {

    ValidateCode get();

    boolean check(ValidateCode validateCode);
}