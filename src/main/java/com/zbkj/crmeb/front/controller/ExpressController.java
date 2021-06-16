package com.zbkj.crmeb.front.controller;


import com.common.CommonPage;
import com.common.CommonResult;
import com.zbkj.crmeb.express.model.Express;
import com.zbkj.crmeb.express.service.ExpressService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * 物流公司 前端控制器
 */
@Slf4j
@RestController("ExpressFrontController")
@RequestMapping("api/front")
@Api(tags = "物流公司")
public class ExpressController {
    @Autowired
    private ExpressService expressService;

    /**
     * 物流公司列表
     */
    @ApiOperation(value = "列表")
    @RequestMapping(value = "/logistics", method = RequestMethod.GET)
    public CommonResult<CommonPage<Express>> register(){
        return CommonResult.success(CommonPage.restPage(expressService.findAll("normal")));
    }
}


