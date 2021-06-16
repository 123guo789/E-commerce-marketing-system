package com.zbkj.crmeb.bargain.dao;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.zbkj.crmeb.bargain.model.StoreBargain;
import org.apache.ibatis.annotations.Select;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 砍价表 Mapper 接口
 */
public interface StoreBargainDao extends BaseMapper<StoreBargain> {

}
