package com.zbkj.crmeb.store.response;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

/**
 * StoreCategoryTreeList
 */
@Data
public class StoreCategoryTreeList {
    private static final long serialVersionUID=1L;

    private Integer id;

    private Integer pid;

    private String cateName;

    private Integer sort;

    private String pic;

    private Boolean isShow;

    private Integer addTime;

    private List<StoreCategoryTreeList> child = new ArrayList<>();
}
