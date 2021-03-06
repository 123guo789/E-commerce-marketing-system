package com.zbkj.crmeb.seckill.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.parser.Feature;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.common.CommonPage;
import com.common.PageParamRequest;
import com.constants.Constants;
import com.exception.CrmebException;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.utils.CrmebUtil;
import com.utils.DateUtil;
import com.utils.RedisUtil;
import com.zbkj.crmeb.combination.model.StoreCombination;
import com.zbkj.crmeb.front.response.SecKillResponse;
import com.zbkj.crmeb.seckill.dao.StoreSeckillDao;
import com.zbkj.crmeb.seckill.model.StoreSeckill;
import com.zbkj.crmeb.seckill.model.StoreSeckillManger;
import com.zbkj.crmeb.seckill.request.StoreSeckillRequest;
import com.zbkj.crmeb.seckill.request.StoreSeckillSearchRequest;
import com.zbkj.crmeb.seckill.response.StoreSeckillDetailResponse;
import com.zbkj.crmeb.seckill.response.StoreSeckillManagerResponse;
import com.zbkj.crmeb.seckill.response.StoreSeckillResponse;
import com.zbkj.crmeb.seckill.response.StoreSeckillStoreInfoResponse;
import com.zbkj.crmeb.seckill.service.StoreSeckillMangerService;
import com.zbkj.crmeb.seckill.service.StoreSeckillService;
import com.zbkj.crmeb.store.model.*;
import com.zbkj.crmeb.store.request.StoreProductAttrValueRequest;
import com.zbkj.crmeb.store.request.StoreProductStockRequest;
import com.zbkj.crmeb.store.response.StoreProductAttrValueResponse;
import com.zbkj.crmeb.store.response.StoreProductRecommendResponse;
import com.zbkj.crmeb.store.response.StoreProductResponse;
import com.zbkj.crmeb.store.service.*;
import com.zbkj.crmeb.store.utilService.ProductUtils;
import com.zbkj.crmeb.system.service.SystemAttachmentService;
import com.zbkj.crmeb.task.order.OrderRefundByUser;
import com.zbkj.crmeb.user.model.User;
import com.zbkj.crmeb.user.service.UserService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * StoreSeckillService ?????????
 */
@Service
public class StoreSeckillServiceImpl extends ServiceImpl<StoreSeckillDao, StoreSeckill>
        implements StoreSeckillService {

    @Resource
    private StoreSeckillDao dao;

    @Autowired
    private SystemAttachmentService systemAttachmentService;

    @Autowired
    private StoreProductDescriptionService storeProductDescriptionService;

    @Autowired
    private StoreSeckillMangerService storeSeckillMangerService;

    @Autowired
    private UserService userService;

    @Autowired
    private StoreProductRelationService storeProductRelationService;

    @Autowired
    private StoreProductService storeProductService;

    @Autowired
    private StoreProductAttrService attrService;

    @Autowired
    private StoreProductAttrValueService storeProductAttrValueService;

    @Autowired
    private StoreProductAttrResultService storeProductAttrResultService;

    @Autowired
    private ProductUtils productUtils;

    @Autowired
    private RedisUtil redisUtil;

    private static final Logger logger = LoggerFactory.getLogger(OrderRefundByUser.class);

    /**
    * ??????
    * @param request ????????????
    * @param pageParamRequest ???????????????
    * @return List<StoreSeckill>
    */
    @Override
    public PageInfo<StoreSeckillResponse> getList(StoreSeckillSearchRequest request, PageParamRequest pageParamRequest) {
        //??? StoreSeckill ?????????????????????
        Page<StoreSeckill> storeSeckillProductPage = PageHelper.startPage(pageParamRequest.getPage(), pageParamRequest.getLimit());

        LambdaQueryWrapper<StoreSeckill> lambdaQueryWrapper = Wrappers.lambdaQuery();
        StoreSeckill model = new StoreSeckill();
        BeanUtils.copyProperties(request, model);
        if(null != request.getStatus()){
            lambdaQueryWrapper.eq(StoreSeckill::getStatus,request.getStatus());
        }
        if(StringUtils.isNotBlank(request.getKeywords())){
            lambdaQueryWrapper.like(StoreSeckill::getTitle,request.getKeywords())
                    .or().like(StoreSeckill::getId,request.getKeywords());
        }
        if(null != request.getTimeId()){
            lambdaQueryWrapper.eq(StoreSeckill::getTimeId,request.getTimeId());
        }
        lambdaQueryWrapper.eq(StoreSeckill::getIsDel,false);
        lambdaQueryWrapper.orderByDesc(StoreSeckill::getSort).orderByDesc(StoreSeckill::getId);
        List<StoreSeckill> storeProducts = dao.selectList(lambdaQueryWrapper);
        List<StoreSeckillResponse> storeProductResponses = new ArrayList<>();

        // ?????????????????????timeId ???????????????????????????????????????????????????
        List<StoreSeckillManger> currentSeckillManager = storeSeckillMangerService.getCurrentSeckillManager();
        Integer currentSkillTimeId = 0;
//        String currentSkillTime = null;
        if(null != currentSeckillManager && currentSeckillManager.size() > 0){
            currentSkillTimeId = currentSeckillManager.get(0).getId();
        }

        // ?????????????????????????????????????????????????????????????????????
        List<StoreSeckillManagerResponse> storeSeckillMangerServiceList =
                storeSeckillMangerService.getList(new StoreSeckillManger(), new PageParamRequest());
        for (StoreSeckill product : storeProducts) {
            StoreSeckillResponse storeProductResponse = new StoreSeckillResponse();
            BeanUtils.copyProperties(product, storeProductResponse);
            storeProductResponse.setImages(CrmebUtil.stringToArrayStr(product.getImages()));

            StoreProductAttr storeProductAttrPram = new StoreProductAttr();
            storeProductAttrPram.setProductId(product.getId()).setType(Constants.PRODUCT_TYPE_SECKILL);
            List<StoreProductAttr> attrs = attrService.getByEntity(storeProductAttrPram);

            if(attrs.size() > 0){
                storeProductResponse.setAttr(attrs);
            }

            List<StoreProductAttrValueResponse> storeProductAttrValueResponse = new ArrayList<>();
            List<StoreProductAttrValue> storeProductAttrValues =storeProductAttrValueService.getListByProductId(product.getId());

            storeProductAttrValues.stream().map(e->{
                StoreProductAttrValueResponse response = new StoreProductAttrValueResponse();
                BeanUtils.copyProperties(e,response);
                storeProductAttrValueResponse.add(response);
                // ?????????????????????????????????????????????
//                int limitNum = 0;
//                if(null != e.getQuotaShow() && e.getQuotaShow()>= 0 && null != e.getQuota() && e.getQuota()>= 0){
//                    limitNum = e.getQuotaShow() - e.getQuota();
//                }
//                storeProductResponse.setLimitLeftNum(limitNum);
                return e;
            }).collect(Collectors.toList());
            storeProductResponse.setAttrValue(storeProductAttrValueResponse);
//                }
            // ???????????????
            StoreProductDescription sd = storeProductDescriptionService.getOne(
                    new LambdaQueryWrapper<StoreProductDescription>()
                            .eq(StoreProductDescription::getProductId, product.getId())
                                .eq(StoreProductDescription::getType, Constants.PRODUCT_TYPE_SECKILL));
            if(null != sd){
                storeProductResponse.setContent(null == sd.getDescription()?"":sd.getDescription());
            }
            // ????????????????????????
            List<StoreSeckillManagerResponse> hasTimeIds = storeSeckillMangerServiceList.stream()
                    .filter(e -> e.getId() == storeProductResponse.getTimeId()).collect(Collectors.toList());
            if(null != hasTimeIds && hasTimeIds.size() > 0){
                storeProductResponse.setStoreSeckillManagerResponse(hasTimeIds.get(0));
                storeProductResponse.setCurrentTimeId(currentSkillTimeId);
                storeProductResponse.setCurrentTime(hasTimeIds.get(0).getTime());
            }
            storeProductResponses.add(storeProductResponse);
        }
        // ??????sql????????????????????????
        return CommonPage.copyPageInfo(storeSeckillProductPage, storeProductResponses);
    }

    /**
     * ????????????
     *
     * @param id ??????id
     * @return ????????????
     */
    @Override
    public boolean deleteById(int id) {
        StoreSeckill skill = new StoreSeckill().setId(id).setIsDel(true);
        return dao.updateById(skill) > 0;
    }

    /**
     * ??????????????????
     *
     * @param request ?????????????????????
     * @return ????????????
     */
    @Override
    public boolean saveSeckill(StoreSeckillRequest request) {
        // ?????????checked=false?????????
        clearNotCheckedAndValidationPrice(request);

        // ???????????????????????????????????????
        checkProductInSeamTime(request);
        StoreSeckill storeSeckill = new StoreSeckill();
        BeanUtils.copyProperties(request, storeSeckill);
        // ??????
        storeSeckill.setImage(systemAttachmentService.clearPrefix(storeSeckill.getImage()));
        // ?????????
        storeSeckill.setImages(systemAttachmentService.clearPrefix(storeSeckill.getImages()));
        // ???????????????????????????????????????
        storeSeckill.setStartTime(DateUtil.strToDate(request.getStartTime(), Constants.DATE_FORMAT_DATE));
        storeSeckill.setStopTime(DateUtil.strToDate(request.getStopTime(), Constants.DATE_FORMAT_DATE));

        //????????????
        productUtils.calcPriceForAttrValuesSeckill(request, storeSeckill);
        //????????????
        boolean save = save(storeSeckill);
        if(request.getSpecType()) { // ?????????
            if(null != request.getAttr() && request.getAttr().size() > 0){
                request.getAttr().forEach(e->{
                    e.setProductId(storeSeckill.getId());
                    e.setAttrValues(StringUtils.strip(e.getAttrValues().replace("\"",""),"[]"));
                    e.setType(Constants.PRODUCT_TYPE_SECKILL);
                });
                boolean attrAddResult = attrService.saveBatch(request.getAttr());
                if (!attrAddResult) throw new CrmebException("?????????????????????");
            }
        }else{ // ?????????
            StoreProductAttr singleAttr = new StoreProductAttr();
            singleAttr.setProductId(storeSeckill.getId()).setAttrName("??????").setAttrValues("??????").setType(Constants.PRODUCT_TYPE_SECKILL);
            boolean attrAddResult = attrService.save(singleAttr);
            if (!attrAddResult) throw new CrmebException("?????????????????????");
            StoreProductAttrValue singleAttrValue = new StoreProductAttrValue();
            BigDecimal commissionL1= BigDecimal.ZERO;
            BigDecimal commissionL2= BigDecimal.ZERO;
            if(request.getAttrValue().size()>0){
                commissionL1 = null != request.getAttrValue().get(0).getBrokerage() ?
                        request.getAttrValue().get(0).getBrokerage():BigDecimal.ZERO;
                commissionL2 = null != request.getAttrValue().get(0).getBrokerageTwo() ?
                        request.getAttrValue().get(0).getBrokerageTwo():BigDecimal.ZERO;
            }

            singleAttrValue.setProductId(storeSeckill.getId()).setStock(storeSeckill.getStock()).setSuk("??????")
                    .setSales(storeSeckill.getSales()).setPrice(storeSeckill.getPrice())
                    .setImage(systemAttachmentService.clearPrefix(storeSeckill.getImage()))
                    .setCost(storeSeckill.getCost())
//                    .setBarCode(storeSeckill.getBarCode())
                    .setType(Constants.PRODUCT_TYPE_SECKILL)
                    .setOtPrice(storeSeckill.getOtPrice()).setBrokerage(commissionL1)
                    .setBrokerageTwo(commissionL2).setQuota(storeSeckill.getQuota())
                    .setQuotaShow(storeSeckill.getQuota());
            boolean saveOrUpdateResult = storeProductAttrValueService.save(singleAttrValue);
            if(!saveOrUpdateResult) throw new CrmebException("????????????????????????");
        }
        if (null != request.getAttrValue() && request.getAttrValue().size() > 0) {
            // ????????????attrValues???????????????id
            List<StoreProductAttrValueRequest> storeSeckillAttrValueRequests = request.getAttrValue();
            storeSeckillAttrValueRequests.forEach(e->{
                e.setProductId(storeSeckill.getId());
            });
            List<StoreProductAttrValue> storeProductAttrValues = new ArrayList<>();
            for (StoreProductAttrValueRequest attrValuesRequest : storeSeckillAttrValueRequests) {
                StoreProductAttrValue spav = new StoreProductAttrValue();
                BeanUtils.copyProperties(attrValuesRequest,spav);
                //??????sku??????
                if(null == attrValuesRequest.getAttrValue()){
                    break;
                }
                List<String> skuList = new ArrayList<>();
                for(Map.Entry<String,String> vo: attrValuesRequest.getAttrValue().entrySet()){
                    skuList.add(vo.getValue());
                    spav.setSuk(String.join(",",skuList));
                }
                spav.setImage(systemAttachmentService.clearPrefix(spav.getImage()));
                spav.setAttrValue(JSON.toJSONString(attrValuesRequest.getAttrValue()));
                spav.setQuotaShow(spav.getQuota());
                spav.setType(Constants.PRODUCT_TYPE_SECKILL);
                storeProductAttrValues.add(spav);
            }
            // ????????????
            if(storeProductAttrValues.size() > 0){
                boolean saveOrUpdateResult = storeProductAttrValueService.saveBatch(storeProductAttrValues);
                StoreProductAttrResult attrResult = new StoreProductAttrResult(
                        0,
                        storeSeckill.getId(),
                        systemAttachmentService.clearPrefix(JSON.toJSONString(request.getAttrValue())),
                        DateUtil.getNowTime(),Constants.PRODUCT_TYPE_SECKILL);
                storeProductAttrResultService.save(attrResult);
                if(!saveOrUpdateResult) throw new CrmebException("????????????????????????");
            }
        }
        // ???????????????
        StoreProductDescription spd = new StoreProductDescription(
                storeSeckill.getId(),  request.getContent().length() > 0
                ? systemAttachmentService.clearPrefix(request.getContent()):"",Constants.PRODUCT_TYPE_SECKILL);
        storeProductDescriptionService.deleteByProductId(spd.getProductId(),Constants.PRODUCT_TYPE_SECKILL);
        storeProductDescriptionService.save(spd);
        return save;
    }

    /**
     * ??????????????????
     *
     * @param request ?????????????????????
     * @return ????????????
     */
    @Override
    public boolean updateSeckill(StoreSeckillRequest request) {
        // ?????????checked=false?????????
        clearNotCheckedAndValidationPrice(request);

        // ???????????????????????????????????????
        checkProductInSeamTime(request);

        StoreSeckill storeProduct = new StoreSeckill();
        BeanUtils.copyProperties(request, storeProduct);
        storeProduct.setStartTime(DateUtil.strToDate(request.getStartTime(),Constants.DATE_FORMAT_DATE));
        storeProduct.setStopTime(DateUtil.strToDate(request.getStopTime(),Constants.DATE_FORMAT_DATE));

        //??????
        storeProduct.setImage(systemAttachmentService.clearPrefix(storeProduct.getImage()));

        //?????????
        storeProduct.setImages(systemAttachmentService.clearPrefix(storeProduct.getImages()));

//        List<StoreProductAttrValueRequest> storeProductAttrValuesRequest = getStoreProductAttrValueRequests(storeProductRequest);

        productUtils.calcPriceForAttrValuesSeckill(request, storeProduct);
        int saveCount = dao.updateById(storeProduct);
        // ???attr?????????????????????????????????????????????????????????
        attrService.removeByProductId(request.getId(),Constants.PRODUCT_TYPE_SECKILL);
        storeProductAttrValueService.removeByProductId(request.getId(),Constants.PRODUCT_TYPE_SECKILL);
//        if(request.getSpecType()) {
            request.getAttr().forEach(e->{
                e.setProductId(request.getId());
                e.setAttrValues(StringUtils.strip(e.getAttrValues().replace("\"",""),"[]"));
                e.setType(Constants.PRODUCT_TYPE_SECKILL);
            });
        attrService.saveBatch(request.getAttr());
            if(null != request.getAttrValue() && request.getAttrValue().size() > 0){
                List<StoreProductAttrValueRequest> storeProductAttrValuesRequest = request.getAttrValue();
                // ????????????attrValues???????????????id
                storeProductAttrValuesRequest.forEach(e->e.setProductId(request.getId()));
                List<StoreProductAttrValue> storeProductAttrValues = new ArrayList<>();
                for (StoreProductAttrValueRequest attrValuesRequest : storeProductAttrValuesRequest) {
                    StoreProductAttrValue spav = new StoreProductAttrValue();
                    BeanUtils.copyProperties(attrValuesRequest,spav);
                    //??????sku??????
                    if(null != attrValuesRequest.getAttrValue()){
                        List<String> skuList = new ArrayList<>();
                        for(Map.Entry<String,String> vo: attrValuesRequest.getAttrValue().entrySet()){
                            skuList.add(vo.getValue());
                        }
                        spav.setSuk(String.join(",",skuList));
                    }
                    String attrValue = null;
                    if(null != attrValuesRequest.getAttrValue() && attrValuesRequest.getAttrValue().size() > 0){
                        attrValue = JSON.toJSONString(attrValuesRequest.getAttrValue());
                    }
                    spav.setAttrValue(attrValue);
                    spav.setImage(systemAttachmentService.clearPrefix(spav.getImage()));
                    spav.setType(Constants.PRODUCT_TYPE_SECKILL);
                    spav.setQuotaShow(spav.getQuota());
                    storeProductAttrValues.add(spav);
                }
                boolean saveOrUpdateResult = storeProductAttrValueService.saveBatch(storeProductAttrValues);
                // attrResult???????????????????????????
                storeProductAttrResultService.deleteByProductId(storeProduct.getId(),Constants.PRODUCT_TYPE_SECKILL);
                StoreProductAttrResult attrResult = new StoreProductAttrResult(
                        0,
                        storeProduct.getId(),
                        systemAttachmentService.clearPrefix(JSON.toJSONString(request.getAttrValue())),
                        DateUtil.getNowTime(),Constants.PRODUCT_TYPE_SECKILL);
                storeProductAttrResultService.save(attrResult);
                if(!saveOrUpdateResult) throw new CrmebException("????????????????????????");

            }

        // ???????????????
        StoreProductDescription spd = new StoreProductDescription(
                storeProduct.getId(),
                request.getContent().length() > 0
                        ? systemAttachmentService.clearPrefix(request.getContent()):"",
                Constants.PRODUCT_TYPE_SECKILL);
        storeProductDescriptionService.deleteByProductId(storeProduct.getId(),Constants.PRODUCT_TYPE_SECKILL);
        storeProductDescriptionService.save(spd);

        // ???????????????????????????
//        shipProductCoupons(request, storeProduct);
        return saveCount > 0;
    }

    /**
     * ??????????????????
     *
     * @param secKillId ??????id
     * @param status    ????????????
     * @return ????????????
     */
    @Override
    public boolean updateSecKillStatus(int secKillId, boolean status) {
        StoreSeckill seckill = getById(secKillId);
        if (ObjectUtil.isNull(seckill) || seckill.getIsDel()) {
            throw new CrmebException("?????????????????????");
        }
        if (status) {
            // ????????????????????????
            StoreProduct product = storeProductService.getById(seckill.getProductId());
            if (ObjectUtil.isNull(product)) {
                throw new CrmebException("?????????????????????????????????????????????");
            }
        }

        StoreSeckill storeSeckill = new StoreSeckill().setId(secKillId).setStatus(status?1:0);
        return dao.updateById(storeSeckill) > 0;
    }

    /**
     * ??????????????????
     *
     * @param skillId ????????????id
     * @return ??????
     */
    @Override
    public StoreSeckillDetailResponse getDetailH5(int skillId) {
        StoreSeckillDetailResponse productDetailResponse = new StoreSeckillDetailResponse();
        StoreProductResponse productResponse = getSkillDetailJustForH5(skillId);
        StoreSeckillStoreInfoResponse storeInfo = new StoreSeckillStoreInfoResponse();

        BeanUtils.copyProperties(productResponse,storeInfo);

        storeInfo.setStoreName(productResponse.getTitle());

        // ?????????????????????
        User user = userService.getInfo();
        if(null != user && null != user.getUid()){
            storeInfo.setUserLike(storeProductRelationService.getLikeOrCollectByUser(user.getUid(),productResponse.getProductId(),true).size() > 0);
            storeInfo.setUserCollect(storeProductRelationService.getLikeOrCollectByUser(user.getUid(),productResponse.getProductId(),false).size() > 0);
//                user = userService.updateForPromoter(user);
//                productDetailResponse.setPriceName(getPacketPriceRange(productResponse,user.getIsPromoter()));
        }else{
            storeInfo.setUserLike(false);
            storeInfo.setUserCollect(false);
        }
        productDetailResponse.setStoreInfo(storeInfo);
        productDetailResponse.setStatusName(storeInfo.getStatusName());
        productDetailResponse.setKillStatus(storeInfo.getKillStatus());

        // ??????????????????attr??????
       setSkuAttr(skillId, productDetailResponse, productResponse);

        // ???????????????attrValueId ?????????????????????????????????
        if(!productResponse.getSpecType()){
            productDetailResponse.setAloneAttrValueId(productResponse.getAttrValue().get(0).getId());
            productResponse.getAttrValue().get(0).setQuota(productResponse.getQuota());
            productDetailResponse.setQuota(productResponse.getQuota());
            productDetailResponse.setQuotaShow(productResponse.getQuotaShow());
        }

        // ??????????????????sku??????
        HashMap<String,Object> skuMap = new HashMap<>();
        for (StoreProductAttrValueResponse attrValue : productResponse.getAttrValue()) {
            skuMap.put(attrValue.getSuk(),attrValue);
        }
        productDetailResponse.setProductValue(skuMap);
//        // ????????????
//        List<StoreProduct> storeProducts = storeProductService.getRecommendStoreProduct(18);
//        List<StoreProductRecommendResponse> storeProductRecommendResponses = new ArrayList<>();
//        for (StoreProduct product:storeProducts) {
//            StoreProductRecommendResponse sPRecommendResponse = new StoreProductRecommendResponse();
//            BeanUtils.copyProperties(product,sPRecommendResponse);
//            sPRecommendResponse.setActivity(null);
////            sPRecommendResponse.setCheckCoupon(storeCouponService.getListByUser(product.getId()).size() > 0);
//            storeProductRecommendResponses.add(sPRecommendResponse);
//        }
//        productDetailResponse.setGoodList(storeProductRecommendResponses);

        return productDetailResponse;
    }

    /**
     * ?????????????????? ?????????
     *
     * @param skillId ??????id
     * @return ????????????
     */
    @Override
    public StoreProductResponse getDetailAdmin(int skillId) {
        StoreSeckill storeProduct = dao.selectById(skillId);
        if(null == storeProduct) throw new CrmebException("???????????????????????????");
        StoreProductResponse storeProductResponse = new StoreProductResponse();
        BeanUtils.copyProperties(storeProduct, storeProductResponse);

        // ??????attr
        StoreProductAttr spaPram = new StoreProductAttr();
        spaPram.setProductId(skillId).setType(Constants.PRODUCT_TYPE_SECKILL);
        storeProductResponse.setAttr(attrService.getByEntity(spaPram));
        storeProductResponse.setSliderImage(String.join(",",storeProduct.getImages()));

        // ???????????????????????????????????????????????????????????????????????????sku????????????????????????sku???????????????????????????????????????sku??????
        StoreProductAttrValue spavPramSkill = new StoreProductAttrValue();
        spavPramSkill.setProductId(skillId).setType(Constants.PRODUCT_TYPE_SECKILL);
        List<StoreProductAttrValue> storeProductAttrValuesSkill = storeProductAttrValueService.getByEntity(spavPramSkill);
        List<HashMap<String, Object>> attrValuesSkill = genratorSkuInfo(skillId,storeProduct, storeProductAttrValuesSkill, Constants.PRODUCT_TYPE_SECKILL);

        // ??????attrValue
        StoreProductAttrValue spavPramProduct = new StoreProductAttrValue();
        spavPramProduct.setProductId(storeProduct.getProductId()).setType(Constants.PRODUCT_TYPE_NORMAL);
        List<StoreProductAttrValue> storeProductAttrValuesProduct = storeProductAttrValueService.getByEntity(spavPramProduct);
        List<HashMap<String, Object>> attrValuesProduct = genratorSkuInfo(storeProduct.getProductId(),storeProduct, storeProductAttrValuesProduct, Constants.PRODUCT_TYPE_NORMAL);

        // H5 ???????????????skuList
        List<StoreProductAttrValueResponse> sPAVResponses = new ArrayList<>();

        for (StoreProductAttrValue storeProductAttrValue : storeProductAttrValuesSkill) {
            StoreProductAttrValueResponse atr = new StoreProductAttrValueResponse();
            BeanUtils.copyProperties(storeProductAttrValue,atr);
            // ?????????????????????????????????
            atr.setQuota(storeProductResponse.getQuota());
//            if(storeProductAttrValuesSkill.size() == 0){ // ?????????????????????????????????????????????
//                atr.setStock(storeProductResponse.getStock());
//                atr.setPrice(storeProductResponse.getPrice());
//            }
            sPAVResponses.add(atr);
        }

        for (int k = 0; k < attrValuesProduct.size(); k++) {
            for (int i = 0; i < attrValuesSkill.size(); i++) {
                HashMap<String, Object> skill = attrValuesSkill.get(i);
                HashMap<String, Object> product = attrValuesProduct.get(k);
                product.put("checked", false);
                product.put("quota", product.get("stock"));
                product.put("price", product.get("price"));
                if(skill.get("suk").equals(product.get("suk"))){
                    product.put("checked", true);
                    product.put("quota", skill.get("quota"));
                    product.put("price",skill.get("price"));
                    break;
                }
            }
        }

        storeProductResponse.setAttrValues(attrValuesProduct);
        storeProductResponse.setAttrValue(sPAVResponses);
//        if(null != storeProductAttrResult){
        StoreProductDescription sd = storeProductDescriptionService.getOne(
                new LambdaQueryWrapper<StoreProductDescription>()
                        .eq(StoreProductDescription::getProductId, skillId)
                        .eq(StoreProductDescription::getType, Constants.PRODUCT_TYPE_SECKILL));
        if(null != sd){
            storeProductResponse.setContent(null == sd.getDescription()?"":sd.getDescription());
        }
//        }
        return storeProductResponse;
    }

    /**
     * ????????? ??????????????????
     *
     * @return ????????????
     */
    @Override
    public HashMap<String,Object> getForH5Index() {
//        Integer timeSwap = DateUtil.getSecondTimestamp();
        HashMap<String,Object> result = new HashMap<>();
        List<SecKillResponse> response = new ArrayList<>();
        StoreSeckillManger storeSeckillManger = new StoreSeckillManger();
        storeSeckillManger.setIsDel(false);
        List<StoreSeckillManagerResponse> skillManagerList =
                storeSeckillMangerService.getList(storeSeckillManger, new PageParamRequest());
        // ???????????????????????? ?????????????????????????????????????????????
        int currentHour = DateUtil.getCurrentHour();
        List<StoreSeckillManagerResponse> hasSkillTime =
                skillManagerList.stream().filter(e -> e.getEndTime() > currentHour).collect(Collectors.toList());
        hasSkillTime.stream()
                .sorted(Comparator.comparing(StoreSeckillManagerResponse::getStartTime))
                .map(e->{
                    // ????????????????????????id?????????????????????????????????????????????
                    List<StoreSeckillResponse> existKills = getKillListByTimeId(e.getId() + "", new PageParamRequest(),true);
                    if(null != existKills && existKills.size() >0){
                        int secKillEndSecondTimestamp =
                                DateUtil.getSecondTimestamp(DateUtil.nowDateTime("yyyy-MM-dd " + e.getEndTime() + ":00:00"));
                        SecKillResponse r = new SecKillResponse(e.getId(),e.getSilderImgs(),e.getStatusName(),
                                e.getTime(),e.getKillStatus(),secKillEndSecondTimestamp+"");
                        response.add(r);
                    }
            return e;
        }).collect(Collectors.toList());

        // ???????????????????????????????????????????????????
        int selectIndex = 0;
        for (int i = 0; i < hasSkillTime.size(); i++) {
            if(currentHour == hasSkillTime.get(i).getEndTime()){
                selectIndex = i;
            }
        }
        result.put("seckillTime", response);
        result.put("seckillTimeIndex", selectIndex);
        return result;
    }

    /**
     * ???????????????????????????????????????????????????
     *
     * @param timeId ??????id
     * @return ??????????????????
     */
    @Override
    public List<StoreSeckillResponse> getKillListByTimeId(String timeId,PageParamRequest pageParamRequest,boolean inCurrentTime) {
        PageHelper.startPage(pageParamRequest.getPage(), pageParamRequest.getLimit());
        List<StoreSeckillResponse> responses = new ArrayList<>();
        Integer currentTimeSwap = DateUtil.getSecondTimestamp();
        String currentDate = DateUtil.nowDate(Constants.DATE_FORMAT_DATE);
        LambdaQueryWrapper<StoreSeckill> lqw = Wrappers.lambdaQuery();
        lqw.eq(StoreSeckill::getStatus,1)
                .eq(StoreSeckill::getIsDel,false)
                .eq(StoreSeckill::getIsShow,true)
                .eq(StoreSeckill::getTimeId,timeId);
        if(inCurrentTime){
            lqw.le(StoreSeckill::getStartTime, currentDate);
            lqw.ge(StoreSeckill::getStopTime,currentDate);
        }
        lqw.orderByDesc(StoreSeckill::getId);
        List<StoreSeckill> storeSeckills = dao.selectList(lqw);
        storeSeckills.stream().map(e->{
            StoreSeckillResponse r = new StoreSeckillResponse();
            BeanUtils.copyProperties(e,r);
            r.setImages(CrmebUtil.stringToArrayStr(e.getImages()));
//            r.setLimitLeftNum(r.getQuotaShow() - e.getQuota());
            r.setTimeSwap(currentTimeSwap+"");
            r.setPercent(CrmebUtil.percentInstanceIntVal(e.getQuotaShow() - e.getQuota(), e.getQuotaShow()));
            responses.add(r);
            return e;
        }).collect(Collectors.toList());
        return responses;
    }

    /**
     * ??????????????????
     *
     * @param storeSeckill ??????????????????
     * @return ????????????????????????
     */
    @Override
    public List<StoreSeckill> getByEntity(StoreSeckill storeSeckill) {
        LambdaQueryWrapper<StoreSeckill> lqw = Wrappers.lambdaQuery();
        lqw.setEntity(storeSeckill);
        return dao.selectList(lqw);
    }

    /**
     * ?????????????????????
     *
     * @param seckillId   ??????id
     * @param num         ????????????
     * @param attrValueId
     * @param type
     * @return ????????????
     */
    @Override
    public boolean decProductStock(Integer seckillId, Integer num, Integer attrValueId, Integer type) {
        // ??????attrvalue??????unique??????Id?????????????????????????????????????????????

        // ??????SKU ?????????????????????
        StoreProductAttrValue spavPram = new StoreProductAttrValue();
        spavPram.setProductId(seckillId).setType(type).setId(attrValueId);
        List<StoreProductAttrValue> existAttrValues = storeProductAttrValueService.getByEntity(spavPram);
        if(null == existAttrValues && existAttrValues.size() == 0) throw new CrmebException("?????????????????????????????????");

        StoreProductAttrValue productsInAttrValue = existAttrValues.get(0); // ??????????????????????????????
        StoreSeckill storeSeckill = getById(seckillId);
        boolean result = false;
        if(null != productsInAttrValue){
            boolean resultDecProductStock = storeProductAttrValueService.decProductAttrStock(seckillId,attrValueId,num,type);
            if(!resultDecProductStock) throw new CrmebException("????????????sku????????????");
        }

        // ????????????????????????????????????
        LambdaUpdateWrapper<StoreSeckill> lqwuper = new LambdaUpdateWrapper<>();
        lqwuper.eq(StoreSeckill::getId, seckillId);
        lqwuper.set(StoreSeckill::getStock, storeSeckill.getStock()-num);
        lqwuper.set(StoreSeckill::getSales, storeSeckill.getSales()+num);
        lqwuper.set(StoreSeckill::getQuota, storeSeckill.getQuota()-num);
        result = update(lqwuper);
//        if(result){ //?????????????????????
//            Integer alterNumI=0;
//            String alterNum = systemConfigService.getValueByKey("store_stock");
//            if(StringUtils.isNotBlank(alterNum)) alterNumI = Integer.parseInt(alterNum);
//            if(alterNumI >= productsInAttrValue.getStock()){
//                // todo socket ??????????????????
//            }
//        }
        return result;
    }

    /**
     * ????????????id?????????????????????????????????
     *
     * @param productId ??????id
     * @return ???????????????????????????
     */
    @Override
    public List<StoreSeckill> getCurrentSecKillByProductId(Integer productId) {
        List<StoreSeckill> result = new ArrayList<>();
        // ??????????????????????????????
        PageParamRequest pageParamRequest = new PageParamRequest();
        pageParamRequest.setLimit(20);
        List<StoreSeckillManagerResponse> storeSeckillManagerResponses =
                storeSeckillMangerService.getList(new StoreSeckillManger(), pageParamRequest);
        List<StoreSeckillManagerResponse> currentSsmr =
                storeSeckillManagerResponses.stream().filter(e -> e.getKillStatus() == 2).collect(Collectors.toList());
        if(currentSsmr.size() == 0){
            return result;
        }
        List<Integer> skillManagerIds = currentSsmr.stream().map(e -> e.getId()).collect(Collectors.toList());
        // ?????????????????????????????????

        LambdaQueryWrapper<StoreSeckill> lqw = Wrappers.lambdaQuery();
        lqw.eq(StoreSeckill::getProductId,productId);
        lqw.eq(StoreSeckill::getIsDel,false);
        lqw.in(StoreSeckill::getTimeId, skillManagerIds);
        result = dao.selectList(lqw);
        return result;
    }


    /**
     * ??????????????????redis??????
     * @param request StoreProductStockRequest ????????????
     * @return int
     */
    @Override
    public boolean stockAddRedis(StoreProductStockRequest request) {
        String _productString = JSON.toJSONString(request);
        redisUtil.lPush(Constants.PRODUCT_SECKILL_STOCK_UPDATE, _productString);
        return true;
    }

    /**
     * ??????????????????????????????
     */
    @Override
    public void consumeProductStock() {
        String redisKey = Constants.PRODUCT_SECKILL_STOCK_UPDATE;
        Long size = redisUtil.getListSize(redisKey);
        logger.info("StoreProductServiceImpl.doProductStock | size:" + size);
        if(size < 1){
            return;
        }
        for (int i = 0; i < size; i++) {
            //??????10????????????????????????????????????????????????
            Object data = redisUtil.getRightPop(redisKey, 10L);
            if(null == data){
                continue;
            }
            try{
                StoreProductStockRequest storeProductStockRequest =
                        com.alibaba.fastjson.JSONObject.toJavaObject(com.alibaba.fastjson.JSONObject.parseObject(data.toString()), StoreProductStockRequest.class);
                boolean result = doProductStock(storeProductStockRequest);
                if(!result){
                    redisUtil.lPush(redisKey, data);
                }
            }catch (Exception e){
                redisUtil.lPush(redisKey, data);
            }
        }
    }

    /**
     * ??????????????????????????????
     * @param productId ????????????
     * @return
     */
    @Override
    public Boolean isExistActivity(Integer productId) {
        LambdaQueryWrapper<StoreSeckill> lqw = Wrappers.lambdaQuery();
        lqw.eq(StoreSeckill::getProductId, productId);
        lqw.eq(StoreSeckill::getIsDel, false);
        List<StoreSeckill> seckillList = dao.selectList(lqw);
        if (CollUtil.isEmpty(seckillList)) {
            return false;
        }
        // ???????????????????????????????????????????????????
        List<StoreSeckill> list = seckillList.stream().filter(i -> i.getStatus().equals(1)).collect(Collectors.toList());
        return CollUtil.isNotEmpty(list);
    }
    ///////////////////////////////////////////////////////////////////  ???????????????

    // ??????????????????
    private boolean doProductStock(StoreProductStockRequest storeProductStockRequest){
        // ????????????????????????
        StoreSeckill existProduct = getById(storeProductStockRequest.getSeckillId());
        List<StoreProductAttrValue> existAttr =
                storeProductAttrValueService.getListByProductIdAndAttrId(
                        storeProductStockRequest.getSeckillId(),
                        storeProductStockRequest.getAttrId().toString(),
                        storeProductStockRequest.getType());
        if(null == existProduct || null == existAttr){ // ???????????????
            logger.info("??????????????????????????????????????????"+JSON.toJSONString(storeProductStockRequest));
            return true;
        }

        // ??????????????????/?????? ?????????
        boolean isPlus = storeProductStockRequest.getOperationType().equals("add");
        int productStock = isPlus ? existProduct.getStock() + storeProductStockRequest.getNum() : existProduct.getStock() - storeProductStockRequest.getNum();
        existProduct.setStock(productStock);
        existProduct.setSales(existProduct.getSales() - storeProductStockRequest.getNum());
        existProduct.setQuota(existProduct.getQuota() + storeProductStockRequest.getNum());
        updateById(existProduct);

        // ??????sku??????
        for (StoreProductAttrValue attrValue : existAttr) {
            int productAttrStock = isPlus ? attrValue.getStock() + storeProductStockRequest.getNum() : attrValue.getStock() - storeProductStockRequest.getNum();
            attrValue.setStock(productAttrStock);
            attrValue.setSales(attrValue.getSales()-storeProductStockRequest.getNum());
            attrValue.setQuota(attrValue.getQuota() + storeProductStockRequest.getNum());
            storeProductAttrValueService.updateById(attrValue);
        }

        // ????????????????????????
        // StoreProductStockRequest ?????????????????????????????????????????????????????????????????????
        StoreProductResponse existProductLinkedSeckill = storeProductService.getByProductId(storeProductStockRequest.getProductId());
        for (StoreProductAttrValueResponse attrValueResponse : existProductLinkedSeckill.getAttrValue()) {
            if(attrValueResponse.getSuk().equals(storeProductStockRequest.getSuk())){
                StoreProductStockRequest r = new StoreProductStockRequest()
                        .setAttrId(attrValueResponse.getId())
                        .setNum(storeProductStockRequest.getNum())
                        .setOperationType("add")
                        .setProductId(storeProductStockRequest.getProductId())
                        .setType(Constants.PRODUCT_TYPE_NORMAL)
                        .setSuk(storeProductStockRequest.getSuk());
                storeProductService.doProductStock(r);
            }
        }

        return true;
    }

    /**
     * ?????????????????????attr??????
     * @param id ??????id
     * @param productDetailResponse ????????????
     * @param productResponse ????????????
     */
    private void setSkuAttr(Integer id, StoreSeckillDetailResponse productDetailResponse, StoreProductResponse productResponse) {
        List<HashMap<String,Object>> attrMapList = new ArrayList<>();
        for (StoreProductAttr attr : productResponse.getAttr()) {
            HashMap<String, Object> attrMap = new HashMap<>();
            attrMap.put("productId",attr.getProductId());
            attrMap.put("attrName",attr.getAttrName());
//            attrMap.put("type",attr.getType());
            List<String> attrValues = new ArrayList<>();
            String trimAttr = attr.getAttrValues()
                    .replace("[","")
                    .replace("]","");
            if(attr.getAttrValues().contains(",")){
                attrValues = Arrays.asList(trimAttr.split(","));
            }else{
                attrValues.add(trimAttr);
            }
            attrMap.put("attrValues",attrValues);
            // ??????????????????????????????sku??????
            List<HashMap<String,Object>> attrValueMapList = new ArrayList<>();
            for (String attrValue : attrValues) {
                HashMap<String,Object> attrValueMap = new HashMap<>();
                attrValueMap.put("attr",attrValue);
//                attrValueMap.put("check",storeCouponService.getListByProductCanUse(id).size()>0);
                attrValueMapList.add(attrValueMap);
            }
            attrMap.put("attrValue",attrValueMapList);
            attrMapList.add(attrMap);
        }
        productDetailResponse.setProductAttr(attrMapList);
    }


    /**
     * ????????????????????????????????????????????????
     * @param request   ???????????????
     * @return          ????????????
     */
    private void checkProductInSeamTime(StoreSeckillRequest request){
        // ?????????????????????????????????????????????????????????????????????
        LambdaQueryWrapper<StoreSeckill> lqw = Wrappers.lambdaQuery();
        lqw.eq(StoreSeckill::getTimeId,request.getTimeId())
                .eq(StoreSeckill::getProductId,request.getProductId())
                .eq(StoreSeckill::getIsDel,false)
                .eq(StoreSeckill::getTimeId, request.getTimeId());
        List<StoreSeckill> seckills = dao.selectList(lqw);
        if(null != seckills && seckills.size() == 1 && null != request.getId() && request.getId().equals(seckills.get(0).getId())){ // ????????????????????????
            return;
        }
        if(null != seckills && seckills.size() >= 1) throw new CrmebException("????????????????????????"+seckills.get(0).getTitle()+"????????????");
    }

    /**
     * ??????????????????sku????????????
     * @param productId     ??????id
     * @param storeProduct  ??????????????????
     * @param storeProductAttrValues    ????????????
     * @param productType   ?????????????????????
     * @return  sku??????
     */
    private  List<HashMap<String, Object>> genratorSkuInfo(int productId,StoreSeckill storeProduct,
                                                           List<StoreProductAttrValue> storeProductAttrValues,
                                                           int productType) {
        List<HashMap<String, Object>> attrValues = new ArrayList<>();
        if(storeProduct.getSpecType()){

            StoreProductAttrResult sparPram = new StoreProductAttrResult();
            sparPram.setProductId(productId).setType(productType);
            List<StoreProductAttrResult> attrResults = storeProductAttrResultService.getByEntity(sparPram);
            if(null == attrResults || attrResults.size() == 0){
                throw new CrmebException("????????????????????????");
            }
            StoreProductAttrResult attrResult = attrResults.get(0);
            //PC ?????????skuAttrInfo
            List<StoreProductAttrValueRequest> storeProductAttrValueRequests =
                    com.alibaba.fastjson.JSONObject.parseArray(attrResult.getResult(), StoreProductAttrValueRequest.class);
            if(null != storeProductAttrValueRequests){
                for (int i = 0; i < storeProductAttrValueRequests.size(); i++) {
//                    StoreProductAttrValueRequest storeProductAttrValueRequest = storeProductAttrValueRequests.get(i);
                    HashMap<String, Object> attrValue = new HashMap<>();
                    String currentSku = storeProductAttrValues.get(i).getSuk();
                    List<StoreProductAttrValue> hasCurrentSku =
                            storeProductAttrValues.stream().filter(e -> e.getSuk().equals(currentSku)).collect(Collectors.toList());
                    StoreProductAttrValue currentAttrValue = hasCurrentSku.get(0);
                    attrValue.put("id", hasCurrentSku.size() > 0 ? hasCurrentSku.get(0).getId():0);
                    attrValue.put("image", currentAttrValue.getImage());
                    attrValue.put("cost", currentAttrValue.getCost());
                    attrValue.put("price", currentAttrValue.getPrice());
                    attrValue.put("otPrice", currentAttrValue.getOtPrice());
                    attrValue.put("stock", currentAttrValue.getStock());
                    attrValue.put("barCode", currentAttrValue.getBarCode());
                    attrValue.put("weight", currentAttrValue.getWeight());
                    attrValue.put("volume", currentAttrValue.getVolume());
                    attrValue.put("suk", currentSku);
                    attrValue.put("attrValue", JSON.parse(storeProductAttrValues.get(i).getAttrValue(), Feature.OrderedField));
                    attrValue.put("brokerage", currentAttrValue.getBrokerage());
                    attrValue.put("brokerageTwo", currentAttrValue.getBrokerageTwo());
                    attrValue.put("quota", currentAttrValue.getQuota());
                    String[] skus = currentSku.split(",");
                    for (int k = 0; k < skus.length; k++) {
                        attrValue.put("value"+k,skus[k]);
                    }
                    attrValues.add(attrValue);
                }

            }
        }
        return attrValues;
    }

    // ??????AttrValue?????????checked=false?????????
    private void clearNotCheckedAndValidationPrice(StoreSeckillRequest request){
        if(request.getSpecType()){
            request.setAttrValue(request.getAttrValue().stream().filter(e-> e.getChecked()).collect(Collectors.toList()));
        }
        for (StoreProductAttrValueRequest attr : request.getAttrValue()) {
            if((null == attr.getPrice() || attr.getPrice().compareTo(BigDecimal.ZERO) <= 0)
            || (null == attr.getQuota() || attr.getQuota() <= 0)){
                throw new CrmebException("??????????????? ?????????????????????");
            }
        }
    }

    /**
     * ????????????H5????????????????????????????????????
     * @param skillId ??????id
     * @return ??????????????????
     */
    public StoreProductResponse getSkillDetailJustForH5(int skillId) {
        StoreSeckill storeProduct = dao.selectById(skillId);
        if(null == storeProduct) throw new CrmebException("???????????????????????????");
        StoreProductResponse storeProductResponse = new StoreProductResponse();
        BeanUtils.copyProperties(storeProduct, storeProductResponse);

        // ???????????? = ???????????????????????????????????????
        StoreProductResponse currentSeckillProductInfo =
                storeProductService.getByProductId(storeProduct.getProductId());
        storeProductResponse.setSales(currentSeckillProductInfo.getSales());
        storeProductResponse.setFicti(currentSeckillProductInfo.getFicti());
//        if(storeProduct.getSpecType()){
        StoreProductAttr spaPram = new StoreProductAttr();
        spaPram.setProductId(skillId).setType(Constants.PRODUCT_TYPE_SECKILL);
        storeProductResponse.setAttr(attrService.getByEntity(spaPram));
//        storeProductResponse.setSliderImage(String.join(",",storeProduct.getImages()));
//        }else{
//            storeProductResponse.setAttr(new ArrayList<>());
//        }
//        List<StoreProductAttrValue> storeProductAttrValues = storeProductAttrValueService.getListByProductId(storeProduct.getId());

        // ???????????????????????????????????????????????????????????????????????????sku????????????????????????sku???????????????????????????????????????sku??????
        StoreProductAttrValue spavPramSkill = new StoreProductAttrValue();
        spavPramSkill.setProductId(skillId).setType(Constants.PRODUCT_TYPE_SECKILL);
        List<StoreProductAttrValue> storeProductAttrValuesSkill = storeProductAttrValueService.getByEntity(spavPramSkill);
        List<HashMap<String, Object>> attrValuesSkill = genratorSkuInfo(skillId,storeProduct, storeProductAttrValuesSkill, Constants.PRODUCT_TYPE_SECKILL);

//        StoreProductAttrValue spavPramProduct = new StoreProductAttrValue();
//        spavPramProduct.setProductId(storeProduct.getProductId()).setType(Constants.PRODUCT_TYPE_NORMAL);
//        List<StoreProductAttrValue> storeProductAttrValuesProduct = storeProductAttrValueService.getByEntity(spavPramProduct);
//        List<HashMap<String, Object>> attrValuesProduct = genratorSkuInfo(storeProduct.getProductId(),storeProduct, storeProductAttrValuesProduct, Constants.PRODUCT_TYPE_NORMAL);

        // H5 ???????????????skuList
        List<StoreProductAttrValueResponse> sPAVResponses = new ArrayList<>();

        for (StoreProductAttrValue storeProductAttrValue : storeProductAttrValuesSkill) {
            StoreProductAttrValueResponse atr = new StoreProductAttrValueResponse();
            BeanUtils.copyProperties(storeProductAttrValue,atr);
            sPAVResponses.add(atr);
        }

        storeProductResponse.setAttrValues(attrValuesSkill);
        storeProductResponse.setAttrValue(sPAVResponses);
//        if(null != storeProductAttrResult){
        StoreProductDescription sd = storeProductDescriptionService.getOne(
                new LambdaQueryWrapper<StoreProductDescription>()
                        .eq(StoreProductDescription::getProductId, skillId)
                        .eq(StoreProductDescription::getType, Constants.PRODUCT_TYPE_SECKILL));
        if(null != sd){
            storeProductResponse.setContent(null == sd.getDescription()?"":sd.getDescription());
        }
//        }
        return storeProductResponse;
    }

}

