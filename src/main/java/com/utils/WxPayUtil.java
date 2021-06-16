package com.utils;

import com.constants.Constants;
import com.exception.CrmebException;

import java.util.HashMap;

/**
 * 微信支付工具类
 */
public class WxPayUtil {

    /**
     * TODO 后期微信签名生成、校验等在这里开发
     */

    /**
     * 处理 HTTPS API返回数据，转换成Map对象。return_code为SUCCESS时，验证签名。
     *
     * @param xmlStr API返回的XML格式数据
     * @return Map类型数据
     * @throws Exception
     */
    public static HashMap<String, Object> processResponseXml(String xmlStr) throws Exception {
        String RETURN_CODE = "return_code";
        String return_code;
        HashMap<String, Object> respData = XmlUtil.xmlToMap(xmlStr);
        if (respData.containsKey(RETURN_CODE)) {
            return_code = (String) respData.get(RETURN_CODE);
        } else {
            throw new CrmebException(String.format("No `return_code` in XML: %s", xmlStr));
        }

        if (return_code.equals(Constants.FAIL)) {
            return respData;
        } else if (return_code.equals(Constants.SUCCESS)) {
            return respData;
        } else {
            throw new CrmebException(String.format("return_code value %s is invalid in XML: %s", return_code, xmlStr));
        }
    }
}
