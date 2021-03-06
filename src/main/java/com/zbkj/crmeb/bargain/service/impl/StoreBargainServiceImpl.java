package com.zbkj.crmeb.bargain.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.parser.Feature;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.common.CommonPage;
import com.common.PageParamRequest;
import com.constants.Constants;
import com.exception.CrmebException;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.utils.DateUtil;
import com.utils.RedisUtil;
import com.zbkj.crmeb.bargain.dao.StoreBargainDao;
import com.zbkj.crmeb.bargain.model.StoreBargain;
import com.zbkj.crmeb.bargain.model.StoreBargainUser;
import com.zbkj.crmeb.bargain.request.StoreBargainRequest;
import com.zbkj.crmeb.bargain.request.StoreBargainSearchRequest;
import com.zbkj.crmeb.bargain.response.StoreBargainResponse;
import com.zbkj.crmeb.bargain.service.StoreBargainService;
import com.zbkj.crmeb.bargain.service.StoreBargainUserHelpService;
import com.zbkj.crmeb.bargain.service.StoreBargainUserService;
import com.zbkj.crmeb.front.request.BargainFrontRequest;
import com.zbkj.crmeb.front.response.BargainDetailResponse;
import com.zbkj.crmeb.store.model.*;
import com.zbkj.crmeb.store.request.StoreProductAttrValueRequest;
import com.zbkj.crmeb.store.request.StoreProductStockRequest;
import com.zbkj.crmeb.store.response.StoreProductAttrValueResponse;
import com.zbkj.crmeb.store.response.StoreProductResponse;
import com.zbkj.crmeb.store.service.*;
import com.zbkj.crmeb.system.service.SystemAttachmentService;
import com.zbkj.crmeb.user.model.User;
import com.zbkj.crmeb.user.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * StoreBargainService ?????????
 */
@Service
public class StoreBargainServiceImpl extends ServiceImpl<StoreBargainDao, StoreBargain> implements StoreBargainService {

    @Resource
    private StoreBargainDao dao;

    @Autowired
    private StoreBargainUserService storeBargainUserService;

    @Autowired
    private StoreBargainUserHelpService storeBargainUserHelpService;

    @Autowired
    private SystemAttachmentService systemAttachmentService;

    @Autowired
    private StoreProductAttrService attrService;

    @Autowired
    private StoreProductAttrValueService attrValueService;

    @Autowired
    private StoreProductAttrResultService storeProductAttrResultService;

    @Autowired
    private StoreProductDescriptionService storeProductDescriptionService;

    @Autowired
    private UserService userService;

    @Autowired
    private StoreProductAttrValueService storeProductAttrValueService;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private StoreProductService storeProductService;

    @Autowired
    private StoreOrderService storeOrderService;

    private static final Logger logger = LoggerFactory.getLogger(StoreBargainServiceImpl.class);

    /**
    * ??????
    * @param request ????????????
    * @param pageParamRequest ???????????????
    * @return List<StoreBargain>
    */
    @Override
    public PageInfo<StoreBargainResponse> getList(StoreBargainSearchRequest request, PageParamRequest pageParamRequest) {
        Page<StoreBargain> storeBargainPage = PageHelper.startPage(pageParamRequest.getPage(), pageParamRequest.getLimit());
        LambdaQueryWrapper<StoreBargain> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(StoreBargain::getIsDel, false);
        if (StrUtil.isNotEmpty(request.getKeywords())) {
            lambdaQueryWrapper.and(i -> i.like(StoreBargain::getId, request.getKeywords())
                    .or().like(StoreBargain::getStoreName, request.getKeywords())
                    .or().like(StoreBargain::getTitle, request.getKeywords()));
        }
        if (ObjectUtil.isNotNull(request.getStatus())) {
            lambdaQueryWrapper.eq(StoreBargain::getStatus, request.getStatus());
        }
        lambdaQueryWrapper.orderByDesc(StoreBargain::getSort, StoreBargain::getId);
        List<StoreBargain> storeBargainList = dao.selectList(lambdaQueryWrapper);
        if (CollUtil.isEmpty(storeBargainList)) {
            return CommonPage.copyPageInfo(storeBargainPage, CollUtil.newArrayList());
        }
        // 1.?????????????????????2.?????????????????????3.??????????????????
        List<StoreBargainResponse> storeProductResponses = CollUtil.newArrayList();
        for (StoreBargain storeBargain : storeBargainList) {
            StoreBargainResponse storeBargainResponse = new StoreBargainResponse();
            BeanUtils.copyProperties(storeBargain, storeBargainResponse);
            storeBargainResponse.setStartTime(DateUtil.timestamp2DateStr(storeBargain.getStartTime(), Constants.DATE_FORMAT_DATE));
            storeBargainResponse.setStopTime(DateUtil.timestamp2DateStr(storeBargain.getStopTime(), Constants.DATE_FORMAT_DATE));
            storeBargainResponse.setAddTime(DateUtil.timestamp2DateStr(storeBargain.getAddTime(), Constants.DATE_FORMAT));
            List<StoreBargainUser> bargainUserList = storeBargainUserService.getListByBargainId(storeBargain.getId());
            if (CollUtil.isEmpty(bargainUserList)) {
                storeBargainResponse.setCountPeopleAll(0L);
                storeBargainResponse.setCountPeopleHelp(0L);
                storeBargainResponse.setCountPeopleSuccess(0L);
                //????????????
                storeBargainResponse.setSurplusQuota(storeBargain.getQuota());
                storeProductResponses.add(storeBargainResponse);
                continue ;
            }
            //??????????????????
            Integer countPeopleAll = bargainUserList.size();
            //??????????????????
            Long countPeopleSuccess = bargainUserList.stream().filter(o -> o.getStatus().equals(3)).count();
            //??????????????????
            Long countPeopleHelp = storeBargainUserHelpService.getHelpCountByBargainId(storeBargain.getId());
            storeBargainResponse.setCountPeopleAll(countPeopleAll.longValue());
            storeBargainResponse.setCountPeopleHelp(countPeopleHelp);
            storeBargainResponse.setCountPeopleSuccess(countPeopleSuccess);
            //????????????
            storeBargainResponse.setSurplusQuota(storeBargain.getQuota());
            storeProductResponses.add(storeBargainResponse);
        }
        return CommonPage.copyPageInfo(storeBargainPage, storeProductResponses);
    }

    /**
     * ??????????????????
     * @param id
     * @return
     */
    @Override
    public boolean deleteById(Integer id) {
        StoreBargain existBargain = getById(id);
        long timeMillis = System.currentTimeMillis();
        if (existBargain.getStatus().equals(true) && existBargain.getStartTime() <= timeMillis && timeMillis <= existBargain.getStopTime()) {
            throw new CrmebException("???????????????????????????????????????");
        }

        StoreBargain storeBargain = new StoreBargain();
        storeBargain.setId(id).setIsDel(true);
        return dao.updateById(storeBargain) > 0;
    }

    /**
     * ??????????????????
     * @param request   ????????????result
     * @return ????????????
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveBargain(StoreBargainRequest request) {
        // ????????????
        if (null == request.getAttrValue() || request.getAttrValue().size() < 1) {
            throw new CrmebException("????????????????????????????????????");
        }
        StoreProductAttrValueRequest attrValueRequest = request.getAttrValue().get(0);
        if (ObjectUtil.isNull(attrValueRequest.getQuota()) || attrValueRequest.getQuota() <= 0) {
            throw new CrmebException("??????????????????????????????0");
        }

        StoreBargain bargain = new StoreBargain();
        BeanUtils.copyProperties(request, bargain);
        bargain.setId(null);
        // ??????????????????
        bargain.setImage(systemAttachmentService.clearPrefix(request.getImage()));
        bargain.setImages(systemAttachmentService.clearPrefix(request.getImages()));
        // ????????????????????????
        bargain.setStartTime(DateUtil.dateStr2Timestamp(request.getStartTime(), Constants.DATE_TIME_TYPE_BEGIN));
        bargain.setStopTime(DateUtil.dateStr2Timestamp(request.getStopTime(), Constants.DATE_TIME_TYPE_END));
        bargain.setAddTime(System.currentTimeMillis());
        bargain.setStoreName(request.getProName());
        // ??????????????????
        bargain.setPrice(attrValueRequest.getPrice());
        bargain.setMinPrice(attrValueRequest.getMinPrice());
        bargain.setCost(attrValueRequest.getCost());
        bargain.setStock(attrValueRequest.getStock());
        bargain.setQuota(attrValueRequest.getQuota());
        bargain.setIsDel(false);
        bargain.setQuotaShow(bargain.getQuota());
        bargain.setSales(0);
        boolean save = save(bargain);
        if (!save) throw new CrmebException("????????????????????????");

        // ???????????????????????????????????????????????????????????????
        StoreProductAttr singleAttr = new StoreProductAttr();
        singleAttr.setProductId(bargain.getId()).setAttrName("??????").setAttrValues("??????").setType(Constants.PRODUCT_TYPE_BARGAIN);
        boolean attrAddResult = attrService.save(singleAttr);
        if (!attrAddResult) throw new CrmebException("?????????????????????");

        // ?????????????????????????????????????????????????????????
        StoreProductAttrValue singleAttrValue = new StoreProductAttrValue();
        BeanUtils.copyProperties(attrValueRequest, singleAttrValue);
        singleAttrValue.setProductId(bargain.getId()).setType(Constants.PRODUCT_TYPE_BARGAIN);
        singleAttrValue.setImage(systemAttachmentService.clearPrefix(singleAttrValue.getImage()));
        boolean saveAttrValue = attrValueService.save(singleAttrValue);
        if(!saveAttrValue) throw new CrmebException("????????????????????????");

        // ?????????????????????result
        StoreProductAttrResult attrResult = new StoreProductAttrResult(
                0,
                bargain.getId(),
                systemAttachmentService.clearPrefix(JSON.toJSONString(request.getAttrValue())),
                DateUtil.getNowTime(),Constants.PRODUCT_TYPE_BARGAIN);
        boolean saveResult = storeProductAttrResultService.save(attrResult);
        if(!saveResult) throw new CrmebException("????????????????????????????????????");

        // ???????????????
        StoreProductDescription spd = new StoreProductDescription(
                bargain.getId(),  request.getContent().length() > 0
                ? systemAttachmentService.clearPrefix(request.getContent()) : "" , Constants.PRODUCT_TYPE_BARGAIN);
        storeProductDescriptionService.deleteByProductId(spd.getProductId(), Constants.PRODUCT_TYPE_BARGAIN);
        boolean saveDesc = storeProductDescriptionService.save(spd);
        if (!saveDesc) throw new CrmebException("???????????????????????????");

        return save;
    }

    /**
     * ??????????????????
     * @param request
     * @return
     */
    @Override
    public boolean updateBarhain(StoreBargainRequest request) {
        StoreBargain existBargain = getById(request.getId());
        long timeMillis = System.currentTimeMillis();
        if (existBargain.getStatus().equals(true) && existBargain.getStartTime() <= timeMillis && timeMillis <= existBargain.getStopTime()) {
            throw new CrmebException("???????????????????????????????????????");
        }

        if (null == request.getAttrValue() || request.getAttrValue().size() < 1) {
            throw new CrmebException("????????????????????????????????????");
        }
        StoreProductAttrValueRequest attrValueRequest = request.getAttrValue().get(0);

        StoreBargain bargain = new StoreBargain();
        BeanUtils.copyProperties(request, bargain);
        // ??????????????????
        bargain.setImage(systemAttachmentService.clearPrefix(request.getImage()));
        bargain.setImages(systemAttachmentService.clearPrefix(request.getImages()));
        // ????????????????????????
        bargain.setStartTime(DateUtil.dateStr2Timestamp(request.getStartTime(), Constants.DATE_TIME_TYPE_BEGIN));
        bargain.setStopTime(DateUtil.dateStr2Timestamp(request.getStopTime(), Constants.DATE_TIME_TYPE_END));
        bargain.setStoreName(request.getProName());
        // ??????????????????
        bargain.setPrice(attrValueRequest.getPrice());
        bargain.setMinPrice(attrValueRequest.getMinPrice());
        bargain.setCost(attrValueRequest.getCost());
        bargain.setStock(attrValueRequest.getStock());
        bargain.setQuota(attrValueRequest.getQuota());
        bargain.setQuotaShow(attrValueRequest.getQuota());
        int saveCount = dao.updateById(bargain);
        if (saveCount <= 0) {
            throw new CrmebException("????????????????????????");
        }

        // ??????????????????attr???????????????attrValue?????????????????????????????????
        attrValueService.removeByProductId(request.getId(), Constants.PRODUCT_TYPE_BARGAIN);
        StoreProductAttrValue singleAttrValue = new StoreProductAttrValue();
        BeanUtils.copyProperties(attrValueRequest, singleAttrValue);
        singleAttrValue.setProductId(bargain.getId()).setType(Constants.PRODUCT_TYPE_BARGAIN);
        singleAttrValue.setImage(systemAttachmentService.clearPrefix(singleAttrValue.getImage()));
        boolean saveAttrValue = attrValueService.save(singleAttrValue);
        if(!saveAttrValue) throw new CrmebException("????????????????????????");

        // attrResult???????????????????????????
        storeProductAttrResultService.deleteByProductId(bargain.getId(),Constants.PRODUCT_TYPE_BARGAIN);
        StoreProductAttrResult attrResult = new StoreProductAttrResult(
                0,
                bargain.getId(),
                systemAttachmentService.clearPrefix(JSON.toJSONString(request.getAttrValue())),
                DateUtil.getNowTime(),Constants.PRODUCT_TYPE_BARGAIN);
        storeProductAttrResultService.save(attrResult);

        return saveCount > 0;
    }

    /**
     * ????????????????????????
     * @param id
     * @param status
     * @return
     */
    @Override
    public boolean updateBargainStatus(Integer id, boolean status) {
        StoreBargain temp = getById(id);
        if (ObjectUtil.isNull(temp) || temp.getIsDel()) {
            throw new CrmebException("?????????????????????");
        }
        if (status) {
            // ????????????????????????
            StoreProduct product = storeProductService.getById(temp.getProductId());
            if (ObjectUtil.isNull(product)) {
                throw new CrmebException("?????????????????????????????????????????????");
            }
        }

        StoreBargain storeBargain = new StoreBargain();
        storeBargain.setId(id).setStatus(status);
        return dao.updateById(storeBargain) > 0;
    }

    /**
     * ????????????????????????
     * @param bargainId ????????????id
     * @return
     */
    @Override
    public StoreProductResponse getAdminDetail(Integer bargainId) {
        StoreBargain storeBargain = dao.selectById(bargainId);
        if (ObjectUtil.isNull(storeBargain)) throw new CrmebException("?????????????????????????????????");
        StoreProductResponse storeProductResponse = new StoreProductResponse();
        BeanUtils.copyProperties(storeBargain, storeProductResponse);
        storeProductResponse.setStartTime(new Date(storeBargain.getStartTime()));
        storeProductResponse.setStopTime(new Date(storeBargain.getStopTime()));
        storeProductResponse.setStatus(storeBargain.getStatus().equals(true) ? 1 : 0);

        // ??????attr
        StoreProductAttr spaPram = new StoreProductAttr();
        spaPram.setProductId(storeBargain.getProductId() ).setType(Constants.PRODUCT_TYPE_NORMAL);
        List<StoreProductAttr> attrs = attrService.getByEntity(spaPram);
        storeProductResponse.setAttr(attrs);
        storeProductResponse.setSliderImage(String.join(",",storeBargain.getImages()));

        boolean specType = false;
        if (attrs.size() > 1) {
            specType = true;
        }
        storeProductResponse.setSpecType(specType);

        // ???????????????????????????????????????????????????????????????????????????sku????????????????????????sku???????????????????????????????????????sku??????
        StoreProductAttrValue spavPramBargain = new StoreProductAttrValue();
        spavPramBargain.setProductId(bargainId).setType(Constants.PRODUCT_TYPE_BARGAIN);
        List<StoreProductAttrValue> storeProductAttrValuesBargain = attrValueService.getByEntity(spavPramBargain);
        List<HashMap<String, Object>> attrValuesBargain = genratorSkuInfo(bargainId, specType, storeBargain, storeProductAttrValuesBargain, Constants.PRODUCT_TYPE_BARGAIN);

        // ??????attrValue
        StoreProductAttrValue spavPramProduct = new StoreProductAttrValue();
        spavPramProduct.setProductId(storeBargain.getProductId()).setType(Constants.PRODUCT_TYPE_NORMAL);
        List<StoreProductAttrValue> storeProductAttrValuesProduct = attrValueService.getByEntity(spavPramProduct);
        List<HashMap<String, Object>> attrValuesProduct = genratorSkuInfo(storeBargain.getProductId(), specType, storeBargain, storeProductAttrValuesProduct, Constants.PRODUCT_TYPE_NORMAL);

        // H5 ???????????????skuList
        List<StoreProductAttrValueResponse> sPAVResponses = new ArrayList<>();

        for (StoreProductAttrValue storeProductAttrValue : storeProductAttrValuesBargain) {
            StoreProductAttrValueResponse atr = new StoreProductAttrValueResponse();
            BeanUtils.copyProperties(storeProductAttrValue, atr);
            // ?????????????????????????????????
            atr.setQuota(storeProductResponse.getQuota());
            atr.setMinPrice(storeBargain.getMinPrice());
            atr.setChecked(true);
            sPAVResponses.add(atr);
        }

        for (int k = 0; k < attrValuesProduct.size(); k++) {
            for (int i = 0; i < attrValuesBargain.size(); i++) {
                HashMap<String, Object> bargainMap = attrValuesBargain.get(i);
                HashMap<String, Object> productMap = attrValuesProduct.get(k);
                productMap.put("checked", false);
                productMap.put("quota", productMap.get("stock"));
                productMap.put("price", productMap.get("price"));
                if(bargainMap.get("suk").equals(productMap.get("suk"))){
                    productMap.put("checked", true);
                    productMap.put("quota", bargainMap.get("quota"));
                    productMap.put("price",bargainMap.get("price"));
                    break;
                }
            }
        }

        storeProductResponse.setAttrValues(attrValuesProduct);
        storeProductResponse.setAttrValue(sPAVResponses);

        StoreProductDescription sd = storeProductDescriptionService.getOne(
                new LambdaQueryWrapper<StoreProductDescription>()
                        .eq(StoreProductDescription::getProductId, bargainId)
                        .eq(StoreProductDescription::getType, Constants.PRODUCT_TYPE_BARGAIN));
        if(null != sd){
            storeProductResponse.setContent(StrUtil.isBlank(sd.getDescription()) ? "" : sd.getDescription());
        }
        if (StrUtil.isNotBlank(storeProductResponse.getRule())) {
            storeProductResponse.setRule(systemAttachmentService.clearPrefix(storeBargain.getRule()));
        }
        return storeProductResponse;
    }

    /**
     * h5 ????????????????????????
     * @param pageParamRequest
     * @return
     */
    @Override
    public PageInfo<StoreBargainResponse> getH5List(PageParamRequest pageParamRequest) {
        Page<StoreBargain> storeBargainPage = PageHelper.startPage(pageParamRequest.getPage(), pageParamRequest.getLimit());
        LambdaQueryWrapper<StoreBargain> lqw = new LambdaQueryWrapper();
        lqw.eq(StoreBargain::getStatus, true);
        lqw.eq(StoreBargain::getIsDel, false);
        long timeMillis = System.currentTimeMillis();
        lqw.le(StoreBargain::getStartTime, timeMillis);
        lqw.ge(StoreBargain::getStopTime, timeMillis);
        lqw.orderByDesc(StoreBargain::getSort, StoreBargain::getId);
        List<StoreBargain> storeBargains = dao.selectList(lqw);
        List<StoreBargainResponse> bargainResponseList = storeBargains.stream().map(bargain -> {
            StoreBargainResponse storeBargainResponse = new StoreBargainResponse();
            Long countByBargainId = storeBargainUserService.getCountByBargainId(bargain.getId());
            BeanUtils.copyProperties(bargain, storeBargainResponse);
            storeBargainResponse.setCountPeopleAll(countByBargainId);
            return storeBargainResponse;
        }).collect(Collectors.toList());
        return CommonPage.copyPageInfo(storeBargainPage, bargainResponseList);
    }

    /**
     * ????????????????????????????????????
     * ??????????????????????????????????????????????????????????????????????????????
     * @return
     */
    @Override
    public Map<String, Object> getH5Share(Integer bargainId) {
        QueryWrapper<StoreBargain> qw = new QueryWrapper<>();
        qw.select("ifnull(sum(look), 0) as lookCount", "ifnull(sum(share), 0) as shareCount");
        qw.eq("is_del", false);
        Map<String, Object> map = getMap(qw);
//        Long userCount = storeBargainUserHelpService.getHelpPeopleCount();
        Integer count = storeBargainUserHelpService.count();
        map.put("userCount", count.longValue());
        return map;
    }

    /**
     * H5 ??????????????????
     * @param id
     * @return
     */
    @Override
    public BargainDetailResponse getH5Detail(Integer id) {
        StoreBargain storeBargain = dao.selectById(id);
        if (ObjectUtil.isNull(storeBargain)) throw new CrmebException("?????????????????????????????????");
        StoreProductResponse storeProductResponse = new StoreProductResponse();
        BeanUtils.copyProperties(storeBargain, storeProductResponse);
        storeProductResponse.setRule(systemAttachmentService.clearPrefix(storeProductResponse.getRule()));
        storeProductResponse.setEndTime(storeBargain.getStopTime());

        StoreProductAttr spaPram = new StoreProductAttr();
        spaPram.setProductId(id).setType(Constants.PRODUCT_TYPE_BARGAIN);
        storeProductResponse.setAttr(attrService.getByEntity(spaPram));

        StoreProductAttrValue spavPramBargain = new StoreProductAttrValue();
        spavPramBargain.setProductId(id).setType(Constants.PRODUCT_TYPE_BARGAIN);
        List<StoreProductAttrValue> storeProductAttrValuesBargain = storeProductAttrValueService.getByEntity(spavPramBargain);

        // ??????attr
        StoreProductAttr spaPramNormal = new StoreProductAttr();
        spaPramNormal.setProductId(storeBargain.getProductId() ).setType(Constants.PRODUCT_TYPE_NORMAL);
        List<StoreProductAttr> attrs = attrService.getByEntity(spaPramNormal);
        boolean specType = false;
        if (attrs.size() > 1) {
            specType = true;
        }
        List<HashMap<String, Object>> attrValuesBargain = genratorSkuInfo(id, specType, storeBargain, storeProductAttrValuesBargain, Constants.PRODUCT_TYPE_BARGAIN);

        // H5 ???????????????skuList
        List<StoreProductAttrValueResponse> sPAVResponses = new ArrayList<>();

        for (StoreProductAttrValue storeProductAttrValue : storeProductAttrValuesBargain) {
            StoreProductAttrValueResponse atr = new StoreProductAttrValueResponse();
            BeanUtils.copyProperties(storeProductAttrValue,atr);
            atr.setQuota(storeProductAttrValue.getQuota());
            atr.setMinPrice(storeBargain.getMinPrice());
            sPAVResponses.add(atr);
        }
        storeProductResponse.setAttrValues(attrValuesBargain);
        storeProductResponse.setAttrValue(sPAVResponses);
        StoreProductDescription sd = storeProductDescriptionService.getOne(
                new LambdaQueryWrapper<StoreProductDescription>()
                        .eq(StoreProductDescription::getProductId, id)
                        .eq(StoreProductDescription::getType, Constants.PRODUCT_TYPE_BARGAIN));
        if(null != sd){
            storeProductResponse.setContent(null == sd.getDescription()?"":sd.getDescription());
        }

        User user = userService.getInfo();
        // ?????????????????????????????????
        int userBargainStatus = isCanPink(storeBargain, user.getUid());

        // ??????????????????????????????
        Integer bargainSumCount = storeOrderService.getCountByBargainIdAndUid(storeBargain.getId(), user.getUid());

        BargainDetailResponse bargainDetailResponse = new BargainDetailResponse(storeProductResponse, userBargainStatus, user, bargainSumCount);
        bargainDetailResponse.setAloneAttrValueId(storeProductResponse.getAttrValue().get(0).getId());

        // ????????????+1
        storeBargain.setLook(storeBargain.getLook() + 1);
        updateById(storeBargain);
        return bargainDetailResponse;
    }

    /**
     * ????????????????????????
     * @param storeBargain
     * @param uid
     * @return
     * 1.?????????????????????????????????
     * 2.?????????????????????????????????????????????
     */
    private int isCanPink(StoreBargain storeBargain, Integer uid) {
        int userBargainStatus = 0; // ???
//        StoreBargainUser storeBargainUser = storeBargainUserService.getByBargainIdAndUidAndPink(storeBargain.getId(), uid);
//        if (ObjectUtil.isNotNull(storeBargainUser)) {
//            userBargainStatus = 1; // ??????
//            return userBargainStatus;
//        }
//        List<StoreBargainUser> list = storeBargainUserService.getListByBargainIdAndUid(storeBargain.getId(), uid);
//        if (CollUtil.isNotEmpty(list) && list.size() >= storeBargain.getNum()) {
//            userBargainStatus = 1;
//        }
        List<StoreBargainUser> list = storeBargainUserService.getListByBargainIdAndUid(storeBargain.getId(), uid);
        if (CollUtil.isNotEmpty(list)) {
            userBargainStatus = list.size();
        }
        return userBargainStatus;
    }

    /**
     * ????????????????????????????????????
     * @param productId
     * @return
     */
    @Override
    public List<StoreBargain> getCurrentBargainByProductId(Integer productId) {
        long timeMillis = System.currentTimeMillis();
        PageParamRequest pageParamRequest = new PageParamRequest();
        LambdaQueryWrapper<StoreBargain> lqw = new LambdaQueryWrapper<>();
        lqw.eq(StoreBargain::getProductId, productId);
        lqw.eq(StoreBargain::getIsDel, false);
        lqw.le(StoreBargain::getStartTime, timeMillis);
        lqw.ge(StoreBargain::getStopTime, timeMillis);
        lqw.orderByDesc(StoreBargain::getSort, StoreBargain::getId);
        return dao.selectList(lqw);
    }

    /**
     * ????????????????????????
     * @param request
     * @return
     */
    @Override
    public Boolean start(BargainFrontRequest request) {
        StoreBargain storeBargain = dao.selectById(request.getBargainId());
        if (ObjectUtil.isNull(storeBargain)) throw new CrmebException("??????????????????????????????");
        if (!storeBargain.getStatus())  throw new CrmebException("?????????????????????");
        User user = userService.getInfo();

        // ?????????????????????????????????
        StoreBargainUser spavBargainUser = new StoreBargainUser();
        spavBargainUser.setIsDel(false).setBargainId(request.getBargainId()).setUid(user.getUid());
        List<StoreBargainUser> historyList = storeBargainUserService.getByEntity(spavBargainUser);
        if (CollUtil.isNotEmpty(historyList)) {
            List<StoreBargainUser> collect = historyList.stream().filter(i -> i.getStatus() == 1).collect(Collectors.toList());
            if (CollUtil.isNotEmpty(collect)) throw new CrmebException("??????????????????????????????");
            // ??????????????????????????????????????????
            if (historyList.size() >= storeBargain.getNum()) {
                throw new CrmebException("????????????????????????????????????");
            }
        }

        StoreBargainUser storeBargainUser = new StoreBargainUser();
        storeBargainUser.setUid(user.getUid());
        storeBargainUser.setBargainId(request.getBargainId());
        storeBargainUser.setBargainPriceMin(storeBargain.getMinPrice());
        storeBargainUser.setBargainPrice(storeBargain.getPrice());
        storeBargainUser.setPrice(BigDecimal.ZERO);
        storeBargainUser.setAddTime(System.currentTimeMillis());
        storeBargainUser.setStatus(1);
        return storeBargainUserService.save(storeBargainUser);
    }

    /**
     * ??????????????????????????????
     * @param storeBargainParam
     * @return
     */
    @Override
    public List<StoreBargain> getByEntity(StoreBargain storeBargainParam) {
        LambdaQueryWrapper<StoreBargain> lqw = new LambdaQueryWrapper<>();
        lqw.setEntity(storeBargainParam);
        return dao.selectList(lqw);
    }

    /**
     * ????????????????????????
     * @param bargainId
     * @param num
     * @param attrValueId
     * @param type
     * @return
     * ???????????????????????????
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean decProductStock(Integer bargainId, Integer num, Integer attrValueId, Integer type) {
        // ??????attrvalue??????unique??????Id?????????????????????????????????????????????
        StoreProductAttrValue spavParm = new StoreProductAttrValue();
        spavParm.setId(attrValueId).setType(type).setProductId(bargainId);
        List<StoreProductAttrValue> attrvalues = storeProductAttrValueService.getByEntity(spavParm);
        if (CollUtil.isEmpty(attrvalues)) throw new CrmebException("???????????????????????????");

        // ?????????????????????????????????suk   ???????????????????????????sku
        StoreProductAttrValue storeProductAttrValue = attrvalues.get(0);
        if (ObjectUtil.isNotNull(storeProductAttrValue)) {
            boolean updateAttrValue = storeProductAttrValueService.decProductAttrStock(bargainId, attrValueId, num, type);
            if (!updateAttrValue) {
                throw new CrmebException("??????????????????sku????????????");
            }
        }

        // ??????SKU ????????????????????????????????????
        StoreBargain storeBargain = getById(bargainId);
        LambdaUpdateWrapper<StoreBargain> luw = new LambdaUpdateWrapper<>();
        luw.eq(StoreBargain::getId, bargainId);
        luw.set(StoreBargain::getStock, storeBargain.getStock() - num);
        luw.set(StoreBargain::getSales, storeBargain.getSales() + num);
        luw.set(StoreBargain::getQuota, storeBargain.getQuota() - num);
        boolean update = update(luw);
        if (!update) {
            throw new CrmebException("??????????????????????????????");
        }
        return update;
    }

    /**
     * ????????????
     * @param stockRequest  StoreProductStockRequest ????????????
     * @return
     */
    @Override
    public Boolean stockAddRedis(StoreProductStockRequest stockRequest) {
        String _productString = JSON.toJSONString(stockRequest);
        redisUtil.lPush(Constants.PRODUCT_BARGAIN_STOCK_UPDATE, _productString);
        return true;
    }

    /**
     * ??????????????????????????????
     */
    @Override
    public void consumeProductStock() {
        String redisKey = Constants.PRODUCT_BARGAIN_STOCK_UPDATE;
        Long size = redisUtil.getListSize(redisKey);
        logger.info("StoreBargainServiceImpl.consumeProductStock | size:" + size);
        if (size < 1) {
            return;
        }
        for (int i = 0; i < size; i++) {
            //??????10????????????????????????????????????????????????
            Object data = redisUtil.getRightPop(redisKey, 10L);
            if (ObjectUtil.isNull(data)) {
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
     * ???????????????????????????
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void stopAfterChange() {
        // ?????????????????????????????????????????????????????????????????????
        List<StoreBargain> storeBargainList = getByStatusAndGtStopTime();
        logger.info("StoreBargainServiceImpl.stopAfterChange | size:" + storeBargainList.size());
        if (CollUtil.isEmpty(storeBargainList)) {
            return;
        }
        List<StoreBargainUser> bargainUserList = CollUtil.newArrayList();
        for (StoreBargain bargain : storeBargainList) {
            // ?????????????????????????????????????????????????????????
            StoreBargainUser spavBargainUser = new StoreBargainUser();
            spavBargainUser.setBargainId(bargain.getId());
            spavBargainUser.setStatus(1);
            spavBargainUser.setIsDel(false);
            List<StoreBargainUser> bargainUsers = storeBargainUserService.getByEntity(spavBargainUser);
            if (CollUtil.isEmpty(bargainUsers)) {
                continue ;
            }
            for (int i = 0; i < bargainUsers.size(); i++) {
                bargainUsers.get(i).setStatus(2);
            }
            bargainUserList.addAll(bargainUsers);
        }
        boolean b = storeBargainUserService.updateBatchById(bargainUserList, 100);
        if (!b) {
            logger.error("???????????????????????????????????????????????????????????????????????????");
            throw new CrmebException("?????????????????????????????????????????????");
        }
    }

    /**
     * ??????????????????????????????
     * @param productId ????????????
     * @return
     */
    @Override
    public Boolean isExistActivity(Integer productId) {
        // ?????????????????????????????????
        LambdaQueryWrapper<StoreBargain> lqw = new LambdaQueryWrapper<>();
        lqw.eq(StoreBargain::getProductId, productId);
        List<StoreBargain> bargainList = dao.selectList(lqw);
        if (CollUtil.isEmpty(bargainList)) {
            return false;
        }
        // ???????????????????????????????????????????????????
        List<StoreBargain> list = bargainList.stream().filter(i -> i.getStatus().equals(true)).collect(Collectors.toList());
        return CollUtil.isNotEmpty(list);
    }

    /**
     * ?????????????????????????????????????????????????????????????????????
     * @return
     */
    private List<StoreBargain> getByStatusAndGtStopTime() {
        LambdaQueryWrapper<StoreBargain> lqw = new LambdaQueryWrapper<>();
        lqw.eq(StoreBargain::getStatus, true);
        lqw.lt(StoreBargain::getStopTime, System.currentTimeMillis());
        return dao.selectList(lqw);
    }

    /**
     * ??????????????????sku????????????
     * @param productId     ??????id
     * @param specType  ??????????????????
     * @param storeProductAttrValues    ????????????
     * @param productType   ?????????????????????
     * @return  sku??????
     */
    private  List<HashMap<String, Object>> genratorSkuInfo(int productId, boolean specType, StoreBargain storeBargain,
                                                           List<StoreProductAttrValue> storeProductAttrValues,
                                                           int productType) {
        List<HashMap<String, Object>> attrValues = new ArrayList<>();
        if (specType) {
            StoreProductAttrResult sparPram = new StoreProductAttrResult();
            sparPram.setProductId(productId).setType(productType);
            List<StoreProductAttrResult> attrResults = storeProductAttrResultService.getByEntity(sparPram);
            if (null == attrResults || attrResults.size() == 0) {
                throw new CrmebException("????????????????????????");
            }
            StoreProductAttrResult attrResult = attrResults.get(0);
            //PC ?????????skuAttrInfo
            List<StoreProductAttrValueRequest> storeProductAttrValueRequests =
                    com.alibaba.fastjson.JSONObject.parseArray(attrResult.getResult(), StoreProductAttrValueRequest.class);
            if (null != storeProductAttrValueRequests) {
                for (int i = 0; i < storeProductAttrValueRequests.size(); i++) {
                    HashMap<String, Object> attrValue = new HashMap<>();
                    String currentSku = storeProductAttrValues.get(i).getSuk();
                    List<StoreProductAttrValue> hasCurrentSku =
                            storeProductAttrValues.stream().filter(e -> e.getSuk().equals(currentSku)).collect(Collectors.toList());
                    StoreProductAttrValue currentAttrValue = hasCurrentSku.get(0);
                    attrValue.put("id", hasCurrentSku.size() > 0 ? hasCurrentSku.get(0).getId() : 0);
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
                    attrValue.put("minPrice", storeBargain.getMinPrice());
                    String[] skus = currentSku.split(",");
                    for (int k = 0; k < skus.length; k++) {
                        attrValue.put("value" + k, skus[k]);
                    }
                    attrValues.add(attrValue);
                }

            }
        }
        return attrValues;
    }

    // ??????????????????
    private boolean doProductStock(StoreProductStockRequest storeProductStockRequest){
        // ????????????????????????
        StoreBargain existProduct = getById(storeProductStockRequest.getBargainId());
        List<StoreProductAttrValue> existAttr =
                storeProductAttrValueService.getListByProductIdAndAttrId(
                        storeProductStockRequest.getBargainId(),
                        storeProductStockRequest.getAttrId().toString(),
                        storeProductStockRequest.getType());
        if(ObjectUtil.isNull(existProduct) || ObjectUtil.isNull(existAttr)){ // ???????????????
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

}

