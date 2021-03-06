package com.zbkj.crmeb.front.request;

import com.constants.RegularConstants;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import javax.validation.constraints.Pattern;
import java.io.Serializable;

/**
 * 用户地址表
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@ApiModel(value="UserBindingRequest对象", description="绑定手机号")
public class UserBindingRequest implements Serializable {

    private static final long serialVersionUID=1L;

    @ApiModelProperty(value = "手机号", required = true)
    @Pattern(regexp = RegularConstants.PHONE, message = "手机号码格式错误")
    @JsonProperty(value = "account")
    private String phone;

    @ApiModelProperty(value = "验证码", required = true)
    @Pattern(regexp = RegularConstants.SMS_VALIDATE_CODE_NUM, message = "验证码格式错误，验证码必须为6位数字")
    @JsonProperty(value = "captcha")
    private String validateCode;


}
