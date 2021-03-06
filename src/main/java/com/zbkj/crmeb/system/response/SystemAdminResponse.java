package com.zbkj.crmeb.system.response;


import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 系统管理员Response对象
 */
@Data
public class SystemAdminResponse implements Serializable {

    private Integer id;

    private String account;

//    private String pwd;

    private String realName;

    private String roles;

    private String roleNames;

    private String lastIp;

    private Date lastTime;

    private Integer addTime;

    private Integer loginCount;

    private Integer level;

    private Boolean status;

//    private Boolean isDel;

    private String Token;
}
