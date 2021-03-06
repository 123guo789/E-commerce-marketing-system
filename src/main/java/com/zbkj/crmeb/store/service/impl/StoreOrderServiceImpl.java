package com.zbkj.crmeb.store.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.common.CommonPage;
import com.common.MyRecord;
import com.common.PageParamRequest;
import com.constants.Constants;
import com.exception.CrmebException;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.utils.DateUtil;
import com.utils.RedisUtil;
import com.utils.ValidateFormUtil;
import com.utils.vo.dateLimitUtilVo;
import com.zbkj.crmeb.combination.model.StorePink;
import com.zbkj.crmeb.combination.service.StorePinkService;
import com.zbkj.crmeb.express.model.Express;
import com.zbkj.crmeb.express.service.ExpressService;
import com.zbkj.crmeb.express.service.LogisticService;
import com.zbkj.crmeb.express.vo.ExpressSheetVo;
import com.zbkj.crmeb.express.vo.LogisticsResultVo;
import com.zbkj.crmeb.finance.request.FundsMonitorSearchRequest;
import com.zbkj.crmeb.front.vo.OrderAgainVo;
import com.zbkj.crmeb.pass.service.OnePassService;
import com.zbkj.crmeb.payment.service.OrderPayService;
import com.zbkj.crmeb.store.dao.StoreOrderDao;
import com.zbkj.crmeb.store.model.StoreOrder;
import com.zbkj.crmeb.store.model.StoreOrderStatus;
import com.zbkj.crmeb.store.model.StoreProduct;
import com.zbkj.crmeb.store.model.StoreProductAttrValue;
import com.zbkj.crmeb.store.request.*;
import com.zbkj.crmeb.store.response.*;
import com.zbkj.crmeb.store.service.*;
import com.zbkj.crmeb.store.utilService.OrderUtils;
import com.zbkj.crmeb.store.vo.StoreOrderInfoVo;
import com.zbkj.crmeb.system.model.SystemAdmin;
import com.zbkj.crmeb.system.model.SystemStore;
import com.zbkj.crmeb.system.model.SystemStoreStaff;
import com.zbkj.crmeb.system.request.SystemWriteOffOrderSearchRequest;
import com.zbkj.crmeb.system.response.StoreOrderItemResponse;
import com.zbkj.crmeb.system.response.SystemWriteOffOrderResponse;
import com.zbkj.crmeb.system.service.SystemAdminService;
import com.zbkj.crmeb.system.service.SystemConfigService;
import com.zbkj.crmeb.system.service.SystemStoreService;
import com.zbkj.crmeb.system.service.SystemStoreStaffService;
import com.zbkj.crmeb.user.model.User;
import com.zbkj.crmeb.user.model.UserBill;
import com.zbkj.crmeb.user.model.UserToken;
import com.zbkj.crmeb.user.request.UserOperateFundsRequest;
import com.zbkj.crmeb.user.service.UserBillService;
import com.zbkj.crmeb.user.service.UserService;
import com.zbkj.crmeb.user.service.UserTokenService;
import com.zbkj.crmeb.wechat.service.TemplateMessageService;
import com.zbkj.crmeb.wechat.service.impl.WechatSendMessageForMinService;
import com.zbkj.crmeb.wechat.vo.WechatSendMessageForPaySuccess;
import com.zbkj.crmeb.wechat.vo.WechatSendMessageForReFundEd;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

/**
 * StoreOrderServiceImpl ????????????
 */
@Service
public class StoreOrderServiceImpl extends ServiceImpl<StoreOrderDao, StoreOrder> implements StoreOrderService {

    @Resource
    private StoreOrderDao dao;

    @Autowired
    private SystemStoreService systemStoreService;

    @Autowired
    private SystemStoreStaffService systemStoreStaffService;

    @Autowired
    private StoreOrderInfoService StoreOrderInfoService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserBillService userBillService;

    @Autowired
    private StoreOrderStatusService storeOrderStatusService;

    @Autowired
    private StoreOrderRefundService storeOrderRefundService;

    @Autowired
    private ExpressService expressService;

    @Autowired
    private TemplateMessageService templateMessageService;

    @Autowired
    private LogisticService logisticService;

    @Autowired
    private OrderUtils orderUtils;

    private Page<StoreOrder> pageInfo;

    @Autowired
    private StoreProductService storeProductService;

    @Autowired
    private StoreProductAttrValueService storeProductAttrValueService;

    @Autowired
    private WechatSendMessageForMinService wechatSendMessageForMinService;

    @Autowired
    private SystemAdminService systemAdminService;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private StorePinkService storePinkService;

    @Autowired
    private SystemConfigService systemConfigService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private OnePassService onePassService;

    @Autowired
    private OrderPayService orderPayService;

    @Autowired
    private UserTokenService userTokenService;

    /**
    * ??????
    * @param request ????????????
    * @param pageParamRequest ???????????????
    * @return List<StoreOrder>
    */
    @Override
    public StoreOrderResponse getAdminList(StoreOrderSearchRequest request, PageParamRequest pageParamRequest) {
        StoreOrderResponse storeOrderResponse = new StoreOrderResponse();

        //????????????
        storeOrderResponse.setStatus(
                new StoreOrderCountItemResponse(
                        getCount(request, Constants.ORDER_STATUS_ALL),
                        getCount(request, Constants.ORDER_STATUS_UNPAID),
                        getCount(request, Constants.ORDER_STATUS_NOT_SHIPPED),
                        getCount(request, Constants.ORDER_STATUS_SPIKE),
                        getCount(request, Constants.ORDER_STATUS_BARGAIN),
                        getCount(request, Constants.ORDER_STATUS_COMPLETE),
                        getCount(request, Constants.ORDER_STATUS_TOBE_WRITTEN_OFF),
                        getCount(request, Constants.ORDER_STATUS_REFUNDING),
                        getCount(request, Constants.ORDER_STATUS_REFUNDED),
                        getCount(request, Constants.ORDER_STATUS_DELETED)
                )
        );

        //????????????
        List<StoreOrderItemResponse> storeOrderItemResponseArrayList = new ArrayList<>();
        List<StoreOrder> storeOrderList = getList(request, pageParamRequest);

        if(storeOrderList != null && storeOrderList.size() > 0){
            storeOrderItemResponseArrayList = formatOrder(storeOrderList);
        }

        storeOrderResponse.setList(CommonPage.restPage(CommonPage.copyPageInfo(this.pageInfo, storeOrderItemResponseArrayList)));


        //????????????
        pageParamRequest.setLimit(1);
        storeOrderResponse.setTop(
                new StoreOrderTopItemResponse(
                        getCount(request, request.getStatus()),
                        getAmount(request, null),
                        getAmount(request, Constants.PAY_TYPE_WE_CHAT),
                        getAmount(request, Constants.PAY_TYPE_YUE)
               )
        );
        return storeOrderResponse;
    }

    /**
     * ??????
     * @param request StoreOrderSearchRequest ????????????
     * @param pageParamRequest PageParamRequest ??????
     * @return List<StoreOrder>
     */
    @Override
    public List<StoreOrder> getList(StoreOrderSearchRequest request, PageParamRequest pageParamRequest) {
        this.pageInfo = PageHelper.startPage(pageParamRequest.getPage(), pageParamRequest.getLimit());
        QueryWrapper<StoreOrder> queryWrapper = new QueryWrapper<>();
        getRequestWhere(queryWrapper, request);
        getStatusWhere(queryWrapper, request.getStatus());
        getIsDelWhere(queryWrapper, request.getIsDel());
        queryWrapper.orderByDesc("id");
        return dao.selectList(queryWrapper);
    }

    /**
     * H5 ????????????
     * @param storeOrder ????????????
     * @param pageParamRequest ????????????
     * @return ????????????
     */
    @Override
    public List<StoreOrder> getUserOrderList(StoreOrder storeOrder, PageParamRequest pageParamRequest) {
        this.pageInfo = PageHelper.startPage(pageParamRequest.getPage(), pageParamRequest.getLimit());
        LambdaQueryWrapper<StoreOrder> lqw = new LambdaQueryWrapper<>();
        orderUtils.statusApiByWhere(lqw,storeOrder.getStatus());
        if(null != storeOrder.getId()){
            lqw.eq(StoreOrder::getId, storeOrder.getId());
        }
        if(null != storeOrder.getDeliveryId()){
            lqw.eq(StoreOrder::getDeliveryId, storeOrder.getDeliveryId());
        }
        if(null != storeOrder.getUnique()){
            lqw.eq(StoreOrder::getUnique, storeOrder.getUnique());
        }
        if(null != storeOrder.getIsDel()){
            lqw.eq(StoreOrder::getIsDel, storeOrder.getIsDel());
        }
        if(null != storeOrder.getUid()){
            lqw.eq(StoreOrder::getUid, storeOrder.getUid());
        }
        if(null != storeOrder.getOrderId()){
            lqw.eq(StoreOrder::getOrderId, storeOrder.getOrderId());
        }
        if(null != storeOrder.getPayTime()){
            lqw.eq(StoreOrder::getPayTime, storeOrder.getPayTime());
        }
        if(null != storeOrder.getStoreId()){
            lqw.eq(StoreOrder::getStoreId, storeOrder.getStoreId());
        }
        if(null != storeOrder.getShippingType()){
            lqw.eq(StoreOrder::getShippingType, storeOrder.getShippingType());
        }
        if(null != storeOrder.getPayType()){
            lqw.eq(StoreOrder::getPayType, storeOrder.getPayType());
        }

        lqw.orderByDesc(StoreOrder::getCreateTime);
        return dao.selectList(lqw);
    }

    /**
     * ????????????
     * @param storeOrder ????????????
     * @return ????????????
     */
    @Override
    public boolean create(StoreOrder storeOrder) {
        return dao.insert(storeOrder) > 0;
    }

    /**
     * ?????? ????????????????????? orderAgain ???redis ???confirm ????????? ???redis???????????????????????? ??????orderCreate??? isNew=true ?????????????????????
     * @param userId ??????id
     * @param productId ??????id
     * @param cartNum ????????????
     * @param orderUnique ??????????????????
     * @param type ??????????????????
     * @param isNew isNew
     * @param combinationId ??????id
     * @param skillId ??????id
     * @param bargainId ??????id
     * @return
     */
    @Override
    @Transactional(rollbackFor = {RuntimeException.class, Error.class, CrmebException.class})
    public List<String> addCartAgain(Integer userId, Integer productId, Integer cartNum, String orderUnique, String type, boolean isNew, Integer combinationId, Integer skillId, Integer bargainId) {
        List<String> cacheIdsResult = new ArrayList<>();
        // todo ????????????????????????

        // ????????????????????????
        StoreOrder storeOrderPram = new StoreOrder();
        storeOrderPram.setUnique(orderUnique);
        storeOrderPram.setUid(userId);
        storeOrderPram.setIsDel(false);
        StoreOrder existOrder = getByEntityOne(storeOrderPram);
        if(null == existOrder) throw new CrmebException("???????????????");
        OrderAgainVo orderAgainVo = orderUtils.tidyOrder(existOrder, true, false);
//        Long cacheKey = DateUtil.getTime()+userId;
        List<StoreCartResponse> orderAgainCache = new ArrayList<>();
        for (StoreOrderInfoVo so : orderAgainVo.getCartInfo()) {
            // ????????????????????????
            StoreProduct storeProductPram = new StoreProduct();
            storeProductPram.setId(productId);
            storeProductPram.setIsDel(false);
            storeProductPram.setIsShow(true);
            StoreProduct existProduct = storeProductService.getByEntity(storeProductPram);
            if(null == existProduct) throw new CrmebException("??????????????????????????????");

            // ????????????????????????????????????
            StoreProductAttrValue apAttrValuePram = new StoreProductAttrValue();
            apAttrValuePram.setProductId(productId);
            apAttrValuePram.setId(Integer.valueOf(so.getUnique()));
            apAttrValuePram.setType(0);
            List<StoreProductAttrValue> byEntity = storeProductAttrValueService.getByEntity(apAttrValuePram);
            StoreProductAttrValue existSPAttrValue = new StoreProductAttrValue();
            if(null != byEntity && byEntity.size() > 0) existSPAttrValue = byEntity.get(0);
            if(null == existSPAttrValue) throw new CrmebException("??????????????????????????????");
            if(existSPAttrValue.getStock() < cartNum) throw new CrmebException("?????????????????????");
            // ??????????????????????????? ????????????????????????
            if(isNew){
                StoreCartResponse storeCartResponse = new StoreCartResponse();
//                storeCartResponse.setId(cacheKey);
                storeCartResponse.setType(type);
                storeCartResponse.setProductId(productId);
                storeCartResponse.setProductAttrUnique(so.getUnique());
                storeCartResponse.setCartNum(cartNum);
                StoreProductCartProductInfoResponse spcpInfo = new StoreProductCartProductInfoResponse();
                BeanUtils.copyProperties(existProduct,spcpInfo);
                spcpInfo.setAttrInfo(existSPAttrValue);
                storeCartResponse.setProductInfo(spcpInfo);
                storeCartResponse.setTrueStock(storeCartResponse.getProductInfo().getAttrInfo().getStock());
                storeCartResponse.setCostPrice(storeCartResponse.getProductInfo().getAttrInfo().getCost());
//                storeCartResponse.setTruePrice(BigDecimal.ZERO);
                storeCartResponse.setTruePrice(existSPAttrValue.getPrice());
                storeCartResponse.setVipTruePrice(BigDecimal.ZERO);
                orderAgainCache.add(storeCartResponse);
            }
        }
        cacheIdsResult.add(orderUtils.setCacheOrderData(userService.getInfo(), orderAgainCache)+"");

        return cacheIdsResult;
    }

    /**
     * ??????????????????
     * @param storeOrder ????????????
     * @return ??????????????????
     */
    @Override
    public List<StoreOrder> getByEntity(StoreOrder storeOrder) {
        LambdaQueryWrapper<StoreOrder> lqw = new LambdaQueryWrapper<>();
        lqw.setEntity(storeOrder);
        return dao.selectList(lqw);
    }

    /**
     * ????????????????????????
     * @param storeOrder ??????
     * @return ????????????
     */
    @Override
    public StoreOrder getByEntityOne(StoreOrder storeOrder) {
        LambdaQueryWrapper<StoreOrder> lqw = new LambdaQueryWrapper<>();
        lqw.setEntity(storeOrder);
        return dao.selectOne(lqw);
    }

    /**
     * ??????????????????
     * @param storeOrder ????????????
     * @return ????????????
     */
    @Override
    public boolean updateByEntity(StoreOrder storeOrder) {
        LambdaUpdateWrapper<StoreOrder> lambdaUpdateWrapper = new LambdaUpdateWrapper<>();
        if(null != storeOrder.getPayPrice()){
            lambdaUpdateWrapper.set(StoreOrder::getPayPrice, storeOrder.getPayPrice());
        }
        if(null != storeOrder.getPayType()){
            lambdaUpdateWrapper.set(StoreOrder::getPayType, storeOrder.getPayType());
        }
        if(null != storeOrder.getVerifyCode()){
            lambdaUpdateWrapper.set(StoreOrder::getVerifyCode, storeOrder.getVerifyCode());
        }
        if(null != storeOrder.getShippingType()){
            lambdaUpdateWrapper.set(StoreOrder::getShippingType, storeOrder.getShippingType());
        }
        if(null != storeOrder.getOrderId()){
            lambdaUpdateWrapper.set(StoreOrder::getOrderId, storeOrder.getOrderId());
        }
        if(storeOrder.getIsChannel() > -1){
            lambdaUpdateWrapper.set(StoreOrder::getIsChannel, storeOrder.getIsChannel());
        }
        if(null != storeOrder.getDeliveryType()){
            lambdaUpdateWrapper.set(StoreOrder::getDeliveryType, storeOrder.getDeliveryType());
        }
        if(null != storeOrder.getPaid()){
            lambdaUpdateWrapper.set(StoreOrder::getPaid, storeOrder.getPaid());
        }
        if(null != storeOrder.getStatus()){
            lambdaUpdateWrapper.set(StoreOrder::getStatus, storeOrder.getStatus());
        }
        if(null != storeOrder.getRefundStatus()){
            lambdaUpdateWrapper.set(StoreOrder::getRefundStatus, storeOrder.getRefundStatus());
        }
        if(null != storeOrder.getUid()){
            lambdaUpdateWrapper.set(StoreOrder::getUid, storeOrder.getUid());
        }
        if(null != storeOrder.getIsDel()){
            lambdaUpdateWrapper.set(StoreOrder::getIsDel, storeOrder.getIsDel());
        }
        if(null != storeOrder.getIsSystemDel()){
            lambdaUpdateWrapper.set(StoreOrder::getIsSystemDel, storeOrder.getIsSystemDel());
        }
        if(null != storeOrder.getStoreId()){
            lambdaUpdateWrapper.set(StoreOrder::getStoreId, storeOrder.getStoreId());
        }
        if(null != storeOrder.getPayTime()){
            lambdaUpdateWrapper.set(StoreOrder::getPayTime, storeOrder.getPayTime());
        }
        lambdaUpdateWrapper.eq(StoreOrder::getId, storeOrder.getId());
        return update(lambdaUpdateWrapper);
    }

    /**
     * ????????????
     * @param currentUser ????????????
     * @param formId ??????????????????
     * @return ????????????
     */
    @Override
    @Transactional
    public boolean yuePay(StoreOrder storeOrder, User currentUser, String formId) {
        if(currentUser.getNowMoney().compareTo(storeOrder.getPayPrice()) < 0){// ??????0??????
            throw new CrmebException("????????????");
        }
//        BigDecimal priceSubtract = currentUser.getNowMoney().subtract(storeOrder.getPayPrice());
//        UserBill userBill = new UserBill();
//        userBill.setTitle("????????????");
//        userBill.setUid(currentUser.getUid());
//        userBill.setCategory(Constants.USER_BILL_CATEGORY_MONEY);
//        userBill.setType(Constants.USER_BILL_TYPE_PAY_PRODUCT);
//        userBill.setNumber(storeOrder.getPayPrice());
//        userBill.setLinkId(storeOrder.getId()+"");
//        userBill.setBalance(currentUser.getNowMoney());
//        userBill.setMark("????????????" + storeOrder.getPayPrice() + "???????????????");

        UserToken userToken = userTokenService.getByUid(currentUser.getUid());

        Boolean execute = orderPayService.paySuccess(storeOrder, currentUser, userToken);
//        Boolean execute = transactionTemplate.execute(e -> {
//            userService.updateNowMoney(currentUser.getUid(), priceSubtract);
//            userBillService.save(userBill);
//            paySuccess(storeOrder, currentUser, formId);
//            return Boolean.TRUE;
//        });

        // ??????????????????????????? ????????????
//        String storeNameAndCarNumString = orderUtils.getStoreNameAndCarNumString(storeOrder.getId());
//        if(StringUtils.isNotBlank(storeNameAndCarNumString)){
//            WechatSendMessageForPaySuccess paySuccess = new WechatSendMessageForPaySuccess(
//                    storeOrder.getOrderId(),storeOrder.getPayPrice()+"",storeOrder.getPayTime()+"","??????",
//                    storeOrder.getTotalPrice()+"",storeNameAndCarNumString);
//            orderUtils.sendWeiChatMiniMessageForPaySuccess(paySuccess, currentUser.getUid());
//        }
        return execute;
    }

    /**
     * ????????????
     * @param request ????????????
     * @param pageParamRequest ???????????????
     * @return List<StoreOrder>
     */
    @Override
    public SystemWriteOffOrderResponse getWriteOffList(SystemWriteOffOrderSearchRequest request, PageParamRequest pageParamRequest) {
        LambdaQueryWrapper<StoreOrder> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        String where = " is_del = 0 and shipping_type = 2";
//        String where = " is_del = 0 and paid = 1";
        //??????
        if(!StringUtils.isBlank(request.getDateLimit())){
            dateLimitUtilVo dateLimit = DateUtil.getDateLimit(request.getDateLimit());
            where += " and (create_time between '" + dateLimit.getStartTime() + "' and '" + dateLimit.getEndTime() + "' )";
        }

        if(!StringUtils.isBlank(request.getKeywords())){
            where += " and (real_name like '%"+ request.getKeywords() +"%' or user_phone = '"+ request.getKeywords() +"' or order_id = '" + request.getKeywords() + "' or id = '" + request.getKeywords() + "' )";
        }

        if(request.getStoreId() != null && request.getStoreId() > 0){
            where += " and store_id = " + request.getStoreId();
        }

        SystemWriteOffOrderResponse systemWriteOffOrderResponse = new SystemWriteOffOrderResponse();
        BigDecimal totalPrice = dao.getTotalPrice(where);
        BigDecimal price = new BigDecimal(BigInteger.ZERO);
        if(totalPrice == null){
            totalPrice = price;
        }
        systemWriteOffOrderResponse.setOrderTotalPrice(totalPrice);   //???????????????

        BigDecimal refundPrice = dao.getRefundPrice(where);
        if(refundPrice == null){
            refundPrice = price;
        }
        systemWriteOffOrderResponse.setRefundTotalPrice(refundPrice); //???????????????
        systemWriteOffOrderResponse.setRefundTotal(dao.getRefundTotal(where));  //???????????????


        Page<StoreOrder> storeOrderPage = PageHelper.startPage(pageParamRequest.getPage(), pageParamRequest.getLimit());

        lambdaQueryWrapper.apply(where);
        lambdaQueryWrapper.orderByDesc(StoreOrder::getId);
        List<StoreOrder> storeOrderList = dao.selectList(lambdaQueryWrapper);

        if(storeOrderList.size() < 1){
            systemWriteOffOrderResponse.setList(CommonPage.restPage(new PageInfo<>()));
            return systemWriteOffOrderResponse;
        }

        List<StoreOrderItemResponse> storeOrderItemResponseArrayList = formatOrder(storeOrderList);

        systemWriteOffOrderResponse.setTotal(storeOrderPage.getTotal()); //?????????
        systemWriteOffOrderResponse.setList(CommonPage.restPage(CommonPage.copyPageInfo(storeOrderPage, storeOrderItemResponseArrayList)));

        return systemWriteOffOrderResponse;
    }

    /**
     * ??????????????????????????????????????????
     * @param storeOrderList List<StoreOrder> ????????????
     * @return List<StoreOrderItemResponse>
     */
    private List<StoreOrderItemResponse> formatOrder(List<StoreOrder> storeOrderList) {
        List<StoreOrderItemResponse> storeOrderItemResponseArrayList  = new ArrayList<>();
        if(null == storeOrderList || storeOrderList.size() < 1){
            return storeOrderItemResponseArrayList;
        }
        //??????id
        List<Integer> storeIdList = storeOrderList.stream().map(StoreOrder::getStoreId).distinct().collect(Collectors.toList());
        //??????id / ?????????id
        List<Integer> clerkIdList = storeOrderList.stream().map(StoreOrder::getClerkId).distinct().collect(Collectors.toList());

        //??????id??????
        List<Integer> orderIdList = storeOrderList.stream().map(StoreOrder::getId).distinct().collect(Collectors.toList());

        //????????????map
        HashMap<Integer, SystemStore> systemStoreList = systemStoreService.getMapInId(storeIdList);
        //????????????map
        HashMap<Integer, SystemStoreStaff> systemStoreStaffList = systemStoreStaffService.getMapInId(clerkIdList);
        //??????????????????map
        HashMap<Integer, List<StoreOrderInfoVo>> orderInfoList = StoreOrderInfoService.getMapInId(orderIdList);

        //????????????????????????
        List<Integer> userIdList = storeOrderList.stream().map(StoreOrder::getUid).distinct().collect(Collectors.toList());
        //??????????????????
        HashMap<Integer, User> userList = userService.getMapListInUid(userIdList);

        //???????????????id??????
        List<Integer> spreadPeopleUidList = new ArrayList<>();
        for(Map.Entry<Integer, User> entry : userList.entrySet()){
            spreadPeopleUidList.add(entry.getValue().getSpreadUid());
        }

        //????????????
        HashMap<Integer, User> mapListInUid = new HashMap<>();
        if(userIdList.size() > 0 && spreadPeopleUidList.size() > 0) {
            //???????????????
            mapListInUid = userService.getMapListInUid(spreadPeopleUidList);
        }

        for (StoreOrder storeOrder : storeOrderList) {
            StoreOrderItemResponse storeOrderItemResponse = new StoreOrderItemResponse();
            BeanUtils.copyProperties(storeOrder, storeOrderItemResponse);
            String storeName = "";
            if(systemStoreList.containsKey(storeOrder.getStoreId())){
                storeName = systemStoreList.get(storeOrder.getStoreId()).getName();
            }
            storeOrderItemResponse.setStoreName(storeName);

            // ?????????????????????
            String clerkName = "";
            if(systemStoreStaffList.containsKey(storeOrder.getClerkId())){
                clerkName = systemStoreStaffList.get(storeOrder.getClerkId()).getStaffName();
            }
            storeOrderItemResponse.setProductList(orderInfoList.get(storeOrder.getId()));
            storeOrderItemResponse.setTotalNum(storeOrder.getTotalNum());

            //????????????
            storeOrderItemResponse.setStatusStr(getStatus(storeOrder));
            storeOrderItemResponse.setStatus(storeOrder.getStatus());
            //????????????
            storeOrderItemResponse.setPayTypeStr(getPayType(storeOrder.getPayType()));

            //???????????????
            if(!userList.isEmpty()  && null != userList.get(storeOrder.getUid()) && mapListInUid.containsKey(userList.get(storeOrder.getUid()).getSpreadUid())){
                storeOrderItemResponse.getSpreadInfo().setId(mapListInUid.get(userList.get(storeOrder.getUid()).getSpreadUid()).getUid());
                storeOrderItemResponse.getSpreadInfo().setName(mapListInUid.get(userList.get(storeOrder.getUid()).getSpreadUid()).getNickname());
            }
            storeOrderItemResponse.setRefundStatus(storeOrder.getRefundStatus());

            storeOrderItemResponse.setClerkName(clerkName);

            // ????????????????????????
            String orderTypeFormat = "[{}??????]{}";
            String orderType = "";
            // ??????
            if (ObjectUtil.isNotNull(storeOrder.getClerkId()) && storeOrder.getClerkId() > 0) {
                orderType = StrUtil.format(orderTypeFormat, "??????", "");
            }
            // ??????
            if (ObjectUtil.isNotNull(storeOrder.getSeckillId()) && storeOrder.getSeckillId() > 0) {
                orderType = StrUtil.format(orderTypeFormat, "??????", "");
            }
            // ??????
            if (ObjectUtil.isNotNull(storeOrder.getBargainId()) && storeOrder.getBargainId() > 0) {
                orderType = StrUtil.format(orderTypeFormat, "??????", "");
            }
            // ??????
            if (ObjectUtil.isNotNull(storeOrder.getPinkId()) && storeOrder.getPinkId() > 0) {
                StorePink storePink = storePinkService.getById(storeOrder.getPinkId());
                if (ObjectUtil.isNotNull(storePink)) {
                    String pinkstatus = "";
                    if (storePink.getStatus() == 2) {
                        pinkstatus = "?????????";
                    } else if (storePink.getStatus() == 3) {
                        pinkstatus = "?????????";
                    } else {
                        pinkstatus = "???????????????";
                    }
                    orderType = StrUtil.format(orderTypeFormat, "??????", pinkstatus);
                }
            }
            if (StrUtil.isBlank(orderType)) {
                orderType = StrUtil.format(orderTypeFormat, "??????", "");
            }
            storeOrderItemResponse.setOrderType(orderType);
            storeOrderItemResponseArrayList.add(storeOrderItemResponse);
        }
        return storeOrderItemResponseArrayList;
    }

    /**
     * ????????????
     * @param userId Integer ??????id
     * @return UserBalanceResponse
     */
    @Override
    public BigDecimal getSumBigDecimal(Integer userId, String date) {
        QueryWrapper<StoreOrder> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("sum(pay_price) as pay_price").
                eq("paid", 1).
                eq("is_del", 0);
        if(null != userId){
            queryWrapper.eq("uid", userId);
        }
        if(null != date){
            dateLimitUtilVo dateLimit = DateUtil.getDateLimit(date);
            queryWrapper.between("create_time", dateLimit.getStartTime(), dateLimit.getEndTime());
        }
        StoreOrder storeOrder = dao.selectOne(queryWrapper);
        if(null == storeOrder || null == storeOrder.getPayPrice()){
            return BigDecimal.ZERO;
        }
        return storeOrder.getPayPrice();
    }

    /**
     * ??????????????????map
     * @param orderIdList Integer ??????id
     * @return UserBalanceResponse
     */
    @Override
    public Map<Integer, StoreOrder> getMapInId(List<Integer> orderIdList) {
        Map<Integer, StoreOrder> map = new HashMap<>();
        if (null == orderIdList || orderIdList.size() < 1) {
            return map;
        }
        LambdaQueryWrapper<StoreOrder> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.in(StoreOrder::getId, orderIdList);
        List<StoreOrder> orderList = dao.selectList(lambdaQueryWrapper);

        if (null == orderList || orderList.size() < 1) {
            return map;
        }

        for (StoreOrder storeOrder : orderList) {
            map.put(storeOrder.getId(), storeOrder);
        }
        return map;
    }

    /** ??????????????????
     * @param userId ??????id
     * @param date ??????????????? moth
     * @return ??????????????????????????????
     */
    @Override
    public int getOrderCount(Integer userId, String date) {
        LambdaQueryWrapper<StoreOrder> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(StoreOrder::getPaid,1)
                .eq(StoreOrder::getIsDel, 0);

        if(null != userId){
            lambdaQueryWrapper.eq(StoreOrder::getUid, userId);
        }
        if(StringUtils.isNotBlank(date)){
            dateLimitUtilVo dateLimit = DateUtil.getDateLimit(date);
            lambdaQueryWrapper.between(StoreOrder::getCreateTime, dateLimit.getStartTime(), dateLimit.getEndTime());
        }
        return dao.selectCount(lambdaQueryWrapper);
    }

    /**
     * ?????????????????????????????????
     * @param date String ????????????
     * @param lefTime int ????????????????????????
     * @return HashMap<String, Object>
     */
    public List<StoreOrder> getOrderGroupByDate(String date, int lefTime) {
        QueryWrapper<StoreOrder> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("sum(pay_price) as pay_price", "left(create_time, "+lefTime+") as orderId", "count(id) as id");
        if(StringUtils.isNotBlank(date)){
            dateLimitUtilVo dateLimit = DateUtil.getDateLimit(date);
            queryWrapper.between("create_time", dateLimit.getStartTime(), dateLimit.getEndTime());
        }
        queryWrapper.groupBy("orderId").orderByAsc("orderId");
        return dao.selectList(queryWrapper);
    }

    /** ??????
     * @param request StoreOrderRefundRequest ????????????
     * @return boolean
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean refund(StoreOrderRefundRequest request) {
        StoreOrder storeOrder = getById(request.getOrderId());
        if(null == storeOrder){throw new CrmebException("???????????????");}
        if(!storeOrder.getPaid()){throw new CrmebException("?????????????????????");}
        if(storeOrder.getRefundPrice().add(request.getAmount()).compareTo(storeOrder.getPayPrice()) > 0){throw new CrmebException("??????????????????????????????????????????????????????");}


        //??????
        User user = userService.getById(storeOrder.getUid());

        //??????
        if (storeOrder.getPayType().equals(Constants.PAY_TYPE_WE_CHAT)) {
            try {
                storeOrderRefundService.refund(request, storeOrder);
            } catch (Exception e) {
                e.printStackTrace();
                throw new CrmebException("???????????????????????????");
            }
        }
        if (storeOrder.getPayType().equals(Constants.PAY_TYPE_YUE)) {
            UserOperateFundsRequest userOperateFundsRequest = new UserOperateFundsRequest();
            userOperateFundsRequest.setUid(storeOrder.getUid());
            userOperateFundsRequest.setValue(request.getAmount());
            userOperateFundsRequest.setFoundsCategory(Constants.USER_BILL_CATEGORY_MONEY);
            userOperateFundsRequest.setFoundsType(Constants.ORDER_STATUS_REFUNDED);
            userOperateFundsRequest.setType(1);
            userOperateFundsRequest.setTitle(Constants.ORDER_STATUS_STR_REFUNDED);
            boolean addMoney = userService.updateFounds(userOperateFundsRequest, false); //????????????
            if(!addMoney){throw new CrmebException("??????????????????");}

            //????????????
            boolean addBill = userBillService.saveRefundBill(request, user);
            if(!addBill){throw new CrmebException("??????????????????");}
        }

        //????????????????????????
        if(request.getType() == 1){
            storeOrder.setRefundStatus(2);
        }else if(request.getType() == 2){
            storeOrder.setRefundStatus(0);
        }else{
            throw new CrmebException("????????????????????????");
        }
        storeOrder.setRefundPrice(request.getAmount());
        boolean updateOrder = updateById(storeOrder);
        if(!updateOrder){
            storeOrderStatusService.saveRefund(request.getOrderId(), request.getAmount(), "??????");
            throw new CrmebException("??????????????????");
        }
        //????????????
        storeOrderStatusService.saveRefund(request.getOrderId(), request.getAmount(), null);

        //??????
        subtractBill(request, Constants.USER_BILL_CATEGORY_MONEY,
                Constants.USER_BILL_TYPE_BROKERAGE, Constants.USER_BILL_CATEGORY_BROKERAGE_PRICE);

        //??????
        subtractBill(request, Constants.USER_BILL_CATEGORY_INTEGRAL,
                Constants.USER_BILL_TYPE_GAIN, Constants.USER_BILL_CATEGORY_INTEGRAL);

        // ???????????? ??????????????????redis
        redisUtil.lPush(Constants.ORDER_TASK_REDIS_KEY_AFTER_REFUND_BY_USER, storeOrder.getId());

        // ????????????????????? ????????????
        String storeNameAndCarNumString = orderUtils.getStoreNameAndCarNumString(storeOrder.getId());
        WechatSendMessageForReFundEd forReFundEd = new WechatSendMessageForReFundEd(
                "????????????",storeNameAndCarNumString,request.getAmount()+"",DateUtil.nowDateTimeStr(),"???????????????????????????",
                storeOrder.getOrderId(),storeOrder.getId()+"",storeOrder.getCreateTime()+"",storeOrder.getRefundPrice()+"",
                storeNameAndCarNumString,storeOrder.getRefundReason(),"CRMEB",storeOrder.getRefundReasonWapExplain(),
                "??????"
        );
        wechatSendMessageForMinService.sendReFundEdMessage(forReFundEd, userService.getUserIdException());

        return true;
    }

    /** ????????????
     * @param id Integer ??????id
     * @return StoreOrderItemResponse
     */
    @Override
    public StoreOrderInfoResponse info(Integer id) {
        StoreOrder storeOrder = getInfoException(id);
        List<StoreOrderInfoVo> orderInfos = StoreOrderInfoService.getOrderListByOrderId(id);
        StoreOrderInfoResponse storeOrderInfoResponse = new StoreOrderInfoResponse();
        BeanUtils.copyProperties(storeOrder, storeOrderInfoResponse);
        storeOrderInfoResponse.setOrderInfo(orderInfos);
        storeOrderInfoResponse.setPayTypeStr(getPayType(storeOrder.getPayType()));
        storeOrderInfoResponse.setStatusStr(getStatus(storeOrder));
        SystemStore systemStorePram = new SystemStore();
        systemStorePram.setId(storeOrder.getStoreId());
        storeOrderInfoResponse.setSystemStore(systemStoreService.getByCondition(systemStorePram));

        //????????????
        User user = userService.getById(storeOrder.getUid());
        User spread = userService.getById(user.getSpreadUid());
        if(null != spread){
            storeOrderInfoResponse.getSpreadInfo().setId(spread.getUid());
            storeOrderInfoResponse.getSpreadInfo().setName(spread.getNickname());
        }
        storeOrderInfoResponse.setUser(user);
        return storeOrderInfoResponse;
    }

    /** ????????????
     * @param request StoreOrderSendRequest ????????????
     * @return boolean
     */
    @Override
    public boolean send(StoreOrderSendRequest request) {
        //????????????
        StoreOrder storeOrder = getById(request.getId());
        if (ObjectUtil.isNull(storeOrder)) throw new CrmebException("??????????????????,????????????!");
        if (storeOrder.getIsDel()) throw new CrmebException("???????????????,????????????!");
        if (storeOrder.getStatus() > 0) throw new CrmebException("?????????????????????????????????!");

        SystemAdmin currentAdmin = systemAdminService.getInfo();

        switch (request.getType()){
            case "1":// ??????
                express(request, storeOrder);
                orderUtils.sendWeiChatMiniMessageForPackageExpress(storeOrder,currentAdmin.getId());
                break;
            case "2":// ??????
                delivery(request, storeOrder);
                orderUtils.senWeiChatMiniMessageForDeliver(storeOrder,currentAdmin.getId());
                break;
            case "3":// ??????
                virtual(request, storeOrder);
                break;
            default:
                throw new CrmebException("????????????");
        }

        //????????????

        return true;
    }

    /**
     * ????????????
     * @param id integer id
     * @param mark String ??????
     * @return boolean
     */
    @Override
    public boolean mark(Integer id, String mark) {
        StoreOrder storeOrder = getInfoException(id);
        storeOrder.setRemark(mark);
        return updateById(storeOrder);
    }

    /**
     * ????????????
     * @param id integer id
     * @param reason String ??????
     * @return boolean
     */
    @Override
    public boolean refundRefuse(Integer id, String reason) {
        StoreOrder storeOrder = getInfoException(id);
        storeOrder.setRefundReason(reason);
        storeOrder.setRefundStatus(0);
        storeOrder.setStatus(1);
        updateById(storeOrder);

        storeOrderStatusService.createLog(storeOrder.getId(), Constants.ORDER_LOG_REFUND_REFUSE, Constants.ORDER_LOG_MESSAGE_REFUND_REFUSE.replace("{reason}", reason));

        //TODO ??????????????????

        return true;
    }

    /**
     * ????????????
     * @param storeOrder StoreOrder ????????????
     * @return StoreOrder
     */
    @Override
    public StoreOrder getInfoByEntity(StoreOrder storeOrder) {
        LambdaQueryWrapper<StoreOrder> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.setEntity(storeOrder);
        return dao.selectOne(lambdaQueryWrapper);
    }

    /**
     * ??????????????????
     * @param storeOrder ????????????
     * @return ??????
     */
    @Override
    public StoreOrder getInfoJustOrderInfo(StoreOrder storeOrder) {
        LambdaQueryWrapper<StoreOrder> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        if(null != storeOrder.getUnique()){
            lambdaQueryWrapper.or().eq(StoreOrder::getOrderId, storeOrder.getUnique())
            .or().eq(StoreOrder::getUnique, storeOrder.getUnique());
        }
//        if(null != storeOrder.getUid()){
            lambdaQueryWrapper.eq(StoreOrder::getUid, storeOrder.getUid());
//        }
        if(null != storeOrder.getIsDel()){
            lambdaQueryWrapper.eq(StoreOrder::getIsDel, storeOrder.getIsDel());
        }
        return dao.selectOne(lambdaQueryWrapper);
    }

    @Override
    public LogisticsResultVo getLogisticsInfo(Integer id) {
        StoreOrder info = getById(id);
        if(StringUtils.isBlank(info.getDeliveryId())){
            //???????????????????????????
            throw new CrmebException("???????????????????????????");
        }

        return logisticService.info(info.getDeliveryId(), null, Optional.ofNullable(info.getDeliveryCode()).orElse(""), info.getUserPhone());
    }

    /**
     * ????????????id??????????????????
     * @param userId ??????id
     * @return
     */
    @Override
    public RetailShopOrderDataResponse getOrderDataByUserId(Integer userId) {
        QueryWrapper<StoreOrder> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("sum(pay_price) as totalPrice, count(id) as id");
        queryWrapper.ge("paid",1);
        queryWrapper.ge("refund_status",0);
        queryWrapper.eq("uid", userId);
        queryWrapper.groupBy("uid");
        StoreOrder storeOrder = dao.selectOne(queryWrapper);
        RetailShopOrderDataResponse rop = new RetailShopOrderDataResponse();
        if(null != storeOrder){
            rop.setOrderCount(storeOrder.getId()); // id=?????????????????????
            rop.setOrderPrice(storeOrder.getTotalPrice());
        }

        return rop;
    }

    /**
     * ????????????id??????????????????????????????????????????
     * @param ids ??????id??????
     * @return ????????????????????????
     */
    @Override
    public List<StoreOrder> getOrderByUserIdsForRetailShop(List<Integer> ids) {
        LambdaQueryWrapper<StoreOrder> lqw = new LambdaQueryWrapper<>();
        lqw.in(StoreOrder::getUid,ids);
        lqw.ge(StoreOrder::getPaid, 1);
        lqw.ge(StoreOrder::getRefundStatus, 0);
        return dao.selectList(lqw);
    }

    /**
     * ?????? top ????????????
     * @param status ????????????
     * @return ??????????????????
     */
    @Override
    public List<StoreOrder> getTopDataUtil(int status, int userId) {
        LambdaQueryWrapper<StoreOrder> lqw = new LambdaQueryWrapper<>();
        orderUtils.statusApiByWhere(lqw,status);
        lqw.eq(StoreOrder::getIsDel, false);
        lqw.eq(StoreOrder::getUid,userId);
        return dao.selectList(lqw);
    }

    /**
     * ??????????????????
     *
     * @param orderId ??????id wx??????
     * @param price   ???????????????
     * @return ????????????
     */
    @Override
    public boolean editPrice(String orderId, BigDecimal price) {
        String oldPrice = null;
        StoreOrder existOrder = getInfoByEntity(new StoreOrder().setOrderId(orderId));
        // ???????????????
        if(null == existOrder) throw new CrmebException(Constants.RESULT_ORDER_NOTFOUND.replace("${orderCode}", orderId));

        // ???????????????
        if(existOrder.getPaid()) throw new CrmebException(Constants.RESULT_ORDER_PAYED.replace("${orderCode}", orderId));

        // ?????????????????????????????????
        if(existOrder.getPayPrice().compareTo(price) ==0)
            throw new CrmebException(Constants.RESULT_ORDER_EDIT_PRICE_SAME.replace("${oldPrice}",existOrder.getPayPrice()+"")
            .replace("${editPrice}",price+""));

        oldPrice = existOrder.getPayPrice()+"";
        // ??????????????????
        existOrder.setPayPrice(price);
        boolean updateOrderPrice = updateByEntity(existOrder);
        if(!updateOrderPrice) throw new CrmebException(Constants.RESULT_ORDER_EDIT_PRICE_SUCCESS
                .replace("${orderNo}", existOrder.getOrderId()).replace("${price}", price+""));
        // ????????????????????????
        storeOrderStatusService.createLog(existOrder.getId(),Constants.ORDER_LOG_EDIT,
                Constants.RESULT_ORDER_EDIT_PRICE_LOGS.replace("${orderPrice}",oldPrice)
                        .replace("${price}", existOrder.getPayPrice()+""));
        return true;
    }

    /**
     * ????????????
     *
     * @param orderId ?????????
     * @return ??????????????????
     */
    @Override
    public boolean confirmPayed(String orderId) {
        StoreOrder existOrder = getByEntityOne(new StoreOrder().setOrderId(orderId));
        if(null == existOrder) throw new CrmebException(Constants.RESULT_ORDER_NOTFOUND.replace("${orderCode}", orderId));
        return payOrderOffLine(existOrder.getId());
    }

    /**
     * ????????????
     *
     * @param orderId ???????????????id
     * @return ????????????
     */
    @Override
    public boolean payOrderOffLine(Integer orderId) {
        StoreOrder existOrder = getByEntityOne(new StoreOrder().setId(orderId));
        if(null == existOrder) throw new CrmebException(
                Constants.RESULT_ORDER_NOTFOUND_IN_ID.replace("${orderId}", orderId+""));
        if(existOrder.getPaid()) throw new CrmebException(Constants.RESULT_ORDER_PAYED.replace("${orderCode}",existOrder.getOrderId()));
        existOrder.setPaid(true);
        // ????????????????????????
        storeOrderStatusService.createLog(existOrder.getId(),Constants.ORDER_LOG_PAY_OFFLINE,
                Constants.RESULT_ORDER_PAY_OFFLINE.replace("${orderNo}",existOrder.getOrderId())
                        .replace("${price}", existOrder.getPayPrice()+""));
        return updateById(existOrder);
    }

    /**
     * ???????????????????????????????????????
     *
     * @param dateLimit ????????????
     * @param type ??????
     * @return ??????????????????
     */
    @Override
    public StoreOrderStatisticsResponse orderStatisticsByTime(String dateLimit,Integer type) {
        StoreOrderStatisticsResponse response = new StoreOrderStatisticsResponse();
        // ???????????????????????????????????????????????? ?????????????????????????????????????????? ?????????????????????????????????????????? ?????????????????????
        dateLimitUtilVo dateRange = DateUtil.getDateLimit(dateLimit);
        String dateStartD = dateRange.getStartTime();
        String dateEndD = dateRange.getEndTime();
        int days = DateUtil.daysBetween(
                DateUtil.strToDate(dateStartD,Constants.DATE_FORMAT_DATE),
                DateUtil.strToDate(dateEndD,Constants.DATE_FORMAT_DATE)
        );
        // ???????????????????????????????????????
        String perDateStart = DateUtil.addDay(
                DateUtil.strToDate(dateStartD,Constants.DATE_FORMAT_DATE), -days, Constants.DATE_FORMAT_START);
        // ??????????????????
        String dateStart = DateUtil.addDay(
                DateUtil.strToDate(dateStartD,Constants.DATE_FORMAT_DATE),0,Constants.DATE_FORMAT_START);
        String dateEnd = DateUtil.addDay(
                DateUtil.strToDate(dateEndD,Constants.DATE_FORMAT_DATE),0,Constants.DATE_FORMAT_END);

        // ????????????????????????
        List<StoreOrder> orderPerList = getOrderPayedByDateLimit(perDateStart,dateStart);

        // ???????????????
        List<StoreOrder> orderCurrentList = getOrderPayedByDateLimit(dateStart, dateEnd);
        double increasePrice = 0;
        if(type == 1){
            double perSumPrice = orderPerList.stream().mapToDouble(e -> e.getPayPrice().doubleValue()).sum();
            double currentSumPrice = orderCurrentList.stream().mapToDouble(e -> e.getPayPrice().doubleValue()).sum();

            response.setChart(dao.getOrderStatisticsPriceDetail(new StoreDateRangeSqlPram(dateStart,dateEnd)));
            response.setTime(BigDecimal.valueOf(currentSumPrice).setScale(2,BigDecimal.ROUND_HALF_UP));
            // ??????????????????????????????????????????????????????
            increasePrice = currentSumPrice - perSumPrice;
            if(increasePrice <= 0) response.setGrowthRate(0);
            else if(perSumPrice == 0) response.setGrowthRate((int) increasePrice * 100);
            else response.setGrowthRate((int)((increasePrice * perSumPrice) * 100));
        }else if(type ==2){
            response.setChart(dao.getOrderStatisticsOrderCountDetail(new StoreDateRangeSqlPram(dateStart,dateEnd)));
            response.setTime(BigDecimal.valueOf(orderCurrentList.size()));
            increasePrice = orderCurrentList.size() - orderPerList.size();
            if(increasePrice <= 0) response.setGrowthRate(0);
            else if(orderPerList.size() == 0) response.setGrowthRate((int) increasePrice);
            else response.setGrowthRate((int)((increasePrice / orderPerList.size()) * 100));
        }
        response.setIncreaseTime(increasePrice+"");
        response.setIncreaseTimeStatus(increasePrice >= 0 ? 1:2);
        return response;
    }

    /**
     * ?????????????????????????????????
     *
     * @param storeOrder ??????????????????
     * @return ???????????????????????????????????????
     */
    @Override
    public List<StoreOrder> getUserCurrentDaySecKillOrders(StoreOrder storeOrder) {
        String dayStart = DateUtil.nowDateTime(Constants.DATE_FORMAT_START);
        String dayEnd = DateUtil.nowDateTime(Constants.DATE_FORMAT_END);
        LambdaQueryWrapper<StoreOrder> lqw = Wrappers.lambdaQuery();
        lqw.eq(StoreOrder::getUid,storeOrder.getUid()).eq(StoreOrder::getSeckillId,storeOrder.getSeckillId())
                .between(StoreOrder::getCreateTime,dayStart,dayEnd);
        return dao.selectList(lqw);
    }

    /**
     * ???????????????????????????????????????
     * @param storeOrder    ??????????????????
     * @return
     */
    @Override
    public List<StoreOrder> getUserCurrentBargainOrders(StoreOrder storeOrder) {
        LambdaQueryWrapper<StoreOrder> lqw = Wrappers.lambdaQuery();
        lqw.eq(StoreOrder::getUid,storeOrder.getUid()).eq(StoreOrder::getBargainId,storeOrder.getBargainId());
        return dao.selectList(lqw);
    }

    /**
     * ??????????????????????????????????????????
     * ????????????????????????????????????
     * @param bargainId ??????????????????
     * @return
     */
    @Override
    public Integer getCountByBargainId(Integer bargainId) {
        LambdaQueryWrapper<StoreOrder> lqw = Wrappers.lambdaQuery();
        lqw.eq(StoreOrder::getBargainId, bargainId);
        lqw.eq(StoreOrder::getPaid, true);
        lqw.eq(StoreOrder::getRefundStatus, 0);
        return dao.selectCount(lqw);
    }

    /**
     * ??????????????????????????????????????????
     * ????????????????????????????????????
     * @param bargainId ??????????????????
     * @return
     */
    @Override
    public Integer getCountByBargainIdAndUid(Integer bargainId, Integer uid) {
        LambdaQueryWrapper<StoreOrder> lqw = Wrappers.lambdaQuery();
        lqw.eq(StoreOrder::getBargainId, bargainId);
        lqw.eq(StoreOrder::getUid, uid);
        lqw.eq(StoreOrder::getPaid, true);
        lqw.eq(StoreOrder::getRefundStatus, 0);
        return dao.selectCount(lqw);
    }

    @Override
    public StoreOrder getByOderId(String orderId) {
        LambdaQueryWrapper<StoreOrder> lqw = Wrappers.lambdaQuery();
        lqw.eq(StoreOrder::getOrderId, orderId);
        return dao.selectOne(lqw);
    }

    /**
     * ??????????????????????????????
     * @return
     */
    @Override
    public ExpressSheetVo getDeliveryInfo() {
        return systemConfigService.getDeliveryInfo();
    }

    @Override
    public PageInfo<StoreOrder> findListByUserIdsForRetailShop(List<Integer> userIds, RetailShopStairUserRequest request, PageParamRequest pageParamRequest) {
        Page<StoreOrder> storeOrderPage = PageHelper.startPage(pageParamRequest.getPage(), pageParamRequest.getLimit());
        LambdaQueryWrapper<StoreOrder> lqw = new LambdaQueryWrapper<>();
        lqw.in(StoreOrder::getUid, userIds);
        lqw.ge(StoreOrder::getPaid, 1);
        lqw.ge(StoreOrder::getRefundStatus, 0);
        if(StrUtil.isNotBlank(request.getDateLimit())){
            dateLimitUtilVo dateLimit = DateUtil.getDateLimit(request.getDateLimit());
            lqw.between(StoreOrder::getCreateTime, dateLimit.getStartTime(), dateLimit.getEndTime());
        }
        if(StrUtil.isNotBlank(request.getNickName())){
            lqw.eq(StoreOrder::getOrderId, request.getNickName());
        }
        lqw.orderByDesc(StoreOrder::getId);
        return CommonPage.copyPageInfo(storeOrderPage, dao.selectList(lqw));
    }

///////////////////////////////////////////////////////////////////////////////////////////////////// ????????????????????????

    /**
     * ????????????????????????????????????
     * @return ??????????????????
     */
    private List<StoreOrder> getOrderPayedByDateLimit(String startTime, String endTime){
        LambdaQueryWrapper<StoreOrder> lqw = Wrappers.lambdaQuery();
        lqw.eq(StoreOrder::getIsDel, false).eq(StoreOrder::getPaid, true).eq(StoreOrder::getRefundStatus,0)
                .between(StoreOrder::getCreateTime, startTime, endTime);
     return dao.selectList(lqw);
    }

    /** ??????????????????/??????
     * @param request StoreOrderRefundRequest ????????????
     */
    private void subtractBill(StoreOrderRefundRequest request, String category, String type, String foundsType) {
        try{
            FundsMonitorSearchRequest fundsMonitorSearchRequest = new FundsMonitorSearchRequest();
            fundsMonitorSearchRequest.setCategory(category);
            fundsMonitorSearchRequest.setType(type);
            fundsMonitorSearchRequest.setLinkId(request.getOrderId().toString());
            fundsMonitorSearchRequest.setPm(1);

            PageParamRequest pageParamRequest = new PageParamRequest();
            pageParamRequest.setLimit(Constants.EXPORT_MAX_LIMIT);
            List<UserBill> list = userBillService.getList(fundsMonitorSearchRequest, pageParamRequest);

            if(null == list || list.size() < 1){
                return;
            }

            for (UserBill userBill : list) {
                User user = userService.getById(userBill.getUid());
                if(null == user){
                    continue;
                }
                BigDecimal price;
                if(category.equals(Constants.USER_BILL_CATEGORY_INTEGRAL)){
                    price = user.getIntegral();
                }else{
                    price = user.getBrokeragePrice();
                }

                if(userBill.getNumber().compareTo(price) > 0){
                    userBill.setNumber(price);
                    //????????????
                    UserOperateFundsRequest userOperateFundsRequest = new UserOperateFundsRequest();
                    userOperateFundsRequest.setUid(user.getUid());
                    userOperateFundsRequest.setValue(request.getAmount());
                    userOperateFundsRequest.setFoundsType(foundsType);
                    userOperateFundsRequest.setType(0);
                    userService.updateFounds(userOperateFundsRequest, false); //????????????/??????
                    if(category.equals(Constants.USER_BILL_CATEGORY_INTEGRAL)){
                        userBillService.saveRefundIntegralBill(request, user);
                    }else{
                        userBillService.saveRefundBrokeragePriceBill(request, user);
                    }
                }
            }
        }catch (Exception e){
            throw new CrmebException("??????????????????/????????????");
        }
    }

    /** ??????
     * @param id Integer id
     * @return Boolean
     */
    public StoreOrder getInfoException(Integer id) {
        StoreOrder info = getById(id);
        if(null == info){
            throw new CrmebException("????????????????????????");
        }
        return info;
    }


    /** ??????
     * @param request StoreOrderSendRequest ????????????
     * @param storeOrder StoreOrder ????????????
     */
    private void express(StoreOrderSendRequest request, StoreOrder storeOrder) {
        // ????????????????????????
        validateExpressSend(request);
        //??????????????????
        Express express = expressService.getByCode(request.getExpressCode());
        if (request.getExpressRecordType().equals("1")) { // ????????????
            deliverGoods(request, storeOrder, express);
        }
        if (request.getExpressRecordType().equals("2")) { // ????????????
            request.setExpressName(express.getName());
            expressDump(request, storeOrder, express);
        }

        storeOrder.setDeliveryCode(express.getCode());
        storeOrder.setDeliveryName(express.getName());
        storeOrder.setStatus(1);
        storeOrder.setDeliveryType("express");

        String message = Constants.ORDER_LOG_MESSAGE_EXPRESS.replace("{deliveryName}", express.getName()).replace("{deliveryCode}", storeOrder.getDeliveryId());

        Boolean execute = transactionTemplate.execute(i -> {
            updateById(storeOrder);
            //??????????????????
            storeOrderStatusService.createLog(request.getId(), Constants.ORDER_LOG_EXPRESS, message);
            return Boolean.TRUE;
        });
        if (!execute) throw new CrmebException("?????????????????????");

        //????????????????????????
        HashMap<String, String> map = new HashMap<>();
        map.put(Constants.WE_CHAT_TEMP_KEY_FIRST, "??????????????????");
        map.put("keyword1", storeOrder.getOrderId());
        map.put("keyword2", storeOrder.getDeliveryName());
        map.put("keyword3", storeOrder.getDeliveryId());
        map.put(Constants.WE_CHAT_TEMP_KEY_END, "?????????????????????");
        templateMessageService.push(Constants.WE_CHAT_TEMP_KEY_EXPRESS, map, storeOrder.getUid(), Constants.PAY_TYPE_WE_CHAT_FROM_PUBLIC);
    }

    /**
     * ????????????
     * @param request
     * @param storeOrder
     * @param express
     */
    private void expressDump(StoreOrderSendRequest request, StoreOrder storeOrder, Express express) {
        String configExportOpen = systemConfigService.getValueByKeyException("config_export_open");
        if (!configExportOpen.equals("1")) {// ?????????????????????
            throw new CrmebException("????????????????????????");
        }
        MyRecord record = new MyRecord();
        record.set("com", express.getCode());// ??????????????????
        record.set("to_name", storeOrder.getRealName());// ?????????
        record.set("to_tel", storeOrder.getUserPhone());// ???????????????
        record.set("to_addr", storeOrder.getUserAddress());// ?????????????????????
        record.set("from_name", request.getToName());// ?????????
        record.set("from_tel", request.getToTel());// ???????????????
        record.set("from_addr", request.getToAddr());// ?????????????????????
        record.set("temp_id", request.getExpressTempId());// ??????????????????ID
        String siid = systemConfigService.getValueByKeyException("config_export_siid");
        record.set("siid", siid);// ??????????????????
        record.set("count", storeOrder.getTotalNum());// ????????????

        //????????????????????????
        List<Integer> orderIdList = new ArrayList<>();
        orderIdList.add(storeOrder.getId());
        HashMap<Integer, List<StoreOrderInfoVo>> orderInfoMap = StoreOrderInfoService.getMapInId(orderIdList);
        if(orderInfoMap.isEmpty() || !orderInfoMap.containsKey(storeOrder.getId())){
            throw new CrmebException("?????????????????????????????????");
        }
        List<String> productNameList = new ArrayList<>();
        for (StoreOrderInfoVo storeOrderInfoVo : orderInfoMap.get(storeOrder.getId())) {
            productNameList.add(storeOrderInfoVo.getInfo().getProductInfo().getStoreName());
        }

        record.set("cargo", String.join(",", productNameList));// ????????????
        if (express.getPartnerId()) {
            record.set("partner_id", express.getAccount());// ????????????????????????(????????????????????????)
        }
        if (express.getPartnerKey()) {
            record.set("partner_key", express.getPassword());// ??????????????????(????????????????????????)
        }
        if (express.getNet()) {
            record.set("net", express.getNetName());// ??????????????????(????????????????????????)
        }

        MyRecord myRecord = onePassService.expressDump(record);
        storeOrder.setDeliveryId(myRecord.getStr("kuaidinum"));
    }

    /**
     * ????????????
     */
    private void deliverGoods(StoreOrderSendRequest request, StoreOrder storeOrder, Express express) {
        storeOrder.setDeliveryId(request.getExpressNumber());
    }

    /**
     * ????????????????????????
     */
    private void validateExpressSend(StoreOrderSendRequest request) {
        if (request.getExpressRecordType().equals("1")) {
            if (StrUtil.isBlank(request.getExpressNumber())) throw new CrmebException("?????????????????????");
            return;
        }
//        if (StrUtil.isBlank(request.getExpressName())) throw new CrmebException("?????????????????????");
        if (StrUtil.isBlank(request.getExpressCode())) throw new CrmebException("?????????????????????");
        if (StrUtil.isBlank(request.getExpressRecordType())) throw new CrmebException("???????????????????????????");
        if (StrUtil.isBlank(request.getExpressTempId())) throw new CrmebException("?????????????????????");
        if (StrUtil.isBlank(request.getToName())) throw new CrmebException("????????????????????????");
        if (StrUtil.isBlank(request.getToTel())) throw new CrmebException("????????????????????????");
        if (StrUtil.isBlank(request.getToAddr())) throw new CrmebException("????????????????????????");
    }

    /** ????????????
     * @param request StoreOrderSendRequest ????????????
     * @param storeOrder StoreOrder ????????????
     */
    private void delivery(StoreOrderSendRequest request, StoreOrder storeOrder) {
        if (StrUtil.isBlank(request.getDeliveryName())) throw new CrmebException("????????????????????????");
        if (StrUtil.isBlank(request.getDeliveryTel())) throw new CrmebException("??????????????????????????????");
        ValidateFormUtil.isPhone(request.getDeliveryTel(), "?????????????????????");

        //????????????
        storeOrder.setDeliveryName(request.getDeliveryName());
        storeOrder.setDeliveryId(request.getDeliveryTel());
        storeOrder.setStatus(1);
        storeOrder.setDeliveryType("send");

        //????????????????????????
        List<Integer> orderIdList = new ArrayList<>();
        orderIdList.add(storeOrder.getId());
        HashMap<Integer, List<StoreOrderInfoVo>> orderInfoMap = StoreOrderInfoService.getMapInId(orderIdList);
        if(orderInfoMap.isEmpty() || !orderInfoMap.containsKey(storeOrder.getId())){
            throw new CrmebException("?????????????????????????????????");
        }
        List<String> productNameList = new ArrayList<>();
        for (StoreOrderInfoVo storeOrderInfoVo : orderInfoMap.get(storeOrder.getId())) {
            productNameList.add(storeOrderInfoVo.getInfo().getProductInfo().getStoreName());
        }

        String message = Constants.ORDER_LOG_MESSAGE_DELIVERY.replace("{deliveryName}", request.getDeliveryName()).replace("{deliveryCode}", request.getDeliveryTel());

        Boolean execute = transactionTemplate.execute(i -> {
            // ????????????
            updateById(storeOrder);
            // ??????????????????
            storeOrderStatusService.createLog(request.getId(), Constants.ORDER_LOG_DELIVERY, message);
            return Boolean.TRUE;
        });
        if (!execute) throw new CrmebException("????????????????????????");

        //????????????????????????
        HashMap<String, String> map = new HashMap<>();
        map.put(Constants.WE_CHAT_TEMP_KEY_FIRST, "??????????????????");
        map.put("keyword1", StringUtils.join(productNameList, "|"));
        map.put("keyword2", DateUtil.dateToStr(storeOrder.getCreateTime(), Constants.DATE_FORMAT));
        map.put("keyword3", storeOrder.getUserAddress());
        map.put("keyword4", request.getDeliveryName());
        map.put("keyword5", request.getDeliveryTel());
        map.put(Constants.WE_CHAT_TEMP_KEY_END, "?????????????????????");

        templateMessageService.push(Constants.WE_CHAT_TEMP_KEY_DELIVERY, map, storeOrder.getUid() , Constants.PAY_TYPE_WE_CHAT_FROM_PUBLIC);
    }
    /** ??????
     * @param request StoreOrderSendRequest ????????????
     * @param storeOrder StoreOrder ????????????
     */
    private void virtual(StoreOrderSendRequest request, StoreOrder storeOrder) {
        //????????????
        storeOrder.setDeliveryType("fictitious");
        storeOrder.setStatus(1);

        Boolean execute = transactionTemplate.execute(i -> {
            updateById(storeOrder);
            //??????????????????
            storeOrderStatusService.createLog(request.getId(), Constants.ORDER_LOG_DELIVERY_VI, Constants.ORDER_LOG_DELIVERY_VI);
            return Boolean.TRUE;
        });
        if (!execute) throw new CrmebException("????????????????????????");
        //????????????????????????
    }
    /**
     * ??????????????????
     * @param storeOrder ??????????????????
     * @param currentUser ????????????
     * @param formId ??????
     * @return ??????????????????
     */
    public boolean paySuccess(StoreOrder storeOrder,User currentUser, String formId){
        // ??????????????????????????????
        StoreOrder storeOrderUpdate = new StoreOrder();
        storeOrderUpdate.setOrderId(storeOrder.getOrderId());
        storeOrderUpdate.setId(storeOrder.getId());
        storeOrderUpdate.setPaid(true);
        storeOrderUpdate.setPayType(storeOrder.getPayType());
        storeOrderUpdate.setPayTime(new Date());
        boolean orderUpdate2PayResult = updateById(storeOrderUpdate);
        StoreOrderStatus storeOrderStatus = new StoreOrderStatus();
        storeOrderStatus.setOid(storeOrderUpdate.getId());
        storeOrderStatus.setChangeType(Constants.ORDER_LOG_PAY_SUCCESS);
        storeOrderStatus.setChangeMessage(Constants.ORDER_LOG_MESSAGE_PAY_SUCCESS);
        storeOrderStatusService.save(storeOrderStatus);
        UserBill userBill = new UserBill();
        userBill.setTitle("????????????");
        userBill.setUid(currentUser.getUid());
        userBill.setCategory(Constants.USER_BILL_CATEGORY_MONEY);
        userBill.setType(Constants.USER_BILL_TYPE_PAY_MONEY);
        userBill.setNumber(storeOrder.getPayPrice());
        userBill.setLinkId(storeOrder.getId()+"");
        userBill.setBalance(currentUser.getNowMoney());
        userBill.setMark("??????" + storeOrder.getPayPrice() + "???????????????");
        boolean saveUserbillResult = userBillService.save(userBill);
        if(storeOrder.getUseIntegral() > 0){
            BigDecimal useIntegral = BigDecimal.valueOf(storeOrder.getUseIntegral(),0);
            currentUser.setIntegral(currentUser.getIntegral().subtract(useIntegral));
            userService.updateBase(currentUser);
        }
        userService.userPayCountPlus(currentUser);
        return orderUpdate2PayResult;
    }

    /**
     * ????????????
     * @param request ????????????
     * @param status String ??????
     * @return Integer
     */
    private Integer getCount(StoreOrderSearchRequest request, String status) {
        //?????????????????????
        QueryWrapper<StoreOrder> queryWrapper = new QueryWrapper<>();
        getRequestTimeWhere(queryWrapper, request);
        getStatusWhere(queryWrapper, status);
        return dao.selectCount(queryWrapper);
    }

    /**
     * ??????????????????
     * @param request ????????????
     * @return Integer
     */
    private BigDecimal getAmount(StoreOrderSearchRequest request, String type) {
        QueryWrapper<StoreOrder> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("sum(pay_price) as pay_price");
        if(StringUtils.isNotBlank(type)){
            queryWrapper.eq("pay_type", type);
        }
        getRequestWhere(queryWrapper, request);
        getStatusWhere(queryWrapper, request.getStatus());
        StoreOrder storeOrder = dao.selectOne(queryWrapper);
        if(null == storeOrder){
            return BigDecimal.ZERO;
        }

        return storeOrder.getPayPrice();
    }

    /**
     * ??????request???where??????
     * @param queryWrapper QueryWrapper<StoreOrder> ?????????
     * @param request StoreOrderSearchRequest ????????????
     */
    private void getRequestWhere(QueryWrapper<StoreOrder> queryWrapper, StoreOrderSearchRequest request) {
        getRequestOrderIdWhere(queryWrapper, request);
        getRequestUidWhere(queryWrapper, request);
        getRequestTimeWhere(queryWrapper, request);
    }

    private void getIsDelWhere(QueryWrapper<StoreOrder> queryWrapper, Boolean isDel){
        if(null != isDel){
            queryWrapper.eq("is_del", isDel);
        }
    }

    /**
     * ??????request???where??????
     * @param queryWrapper QueryWrapper<StoreOrder> ?????????
     * @param request StoreOrderSearchRequest ????????????
     */
    private void getRequestOrderIdWhere(QueryWrapper<StoreOrder> queryWrapper, StoreOrderSearchRequest request) {
        if(StringUtils.isNotBlank(request.getOrderId())){
            queryWrapper.eq("order_id", request.getOrderId());
        }
    }

    /**
     * ??????request???where??????
     * @param queryWrapper QueryWrapper<StoreOrder> ?????????
     * @param request StoreOrderSearchRequest ????????????
     */
    private void getRequestUidWhere(QueryWrapper<StoreOrder> queryWrapper, StoreOrderSearchRequest request) {
        if(null != request.getUid() && request.getUid() > 0){
            queryWrapper.eq("uid", request.getUid());
        }
    }
    /**
     * ??????request???where??????
     * @param queryWrapper QueryWrapper<StoreOrder> ?????????
     * @param request StoreOrderSearchRequest ????????????
     */
    private void getRequestTimeWhere(QueryWrapper<StoreOrder> queryWrapper, StoreOrderSearchRequest request) {
        if(StringUtils.isNotBlank(request.getDateLimit())){
            dateLimitUtilVo dateLimitUtilVo = DateUtil.getDateLimit(request.getDateLimit());
            queryWrapper.between("create_time",dateLimitUtilVo.getStartTime(),dateLimitUtilVo.getEndTime());
        }
    }


    /**
     * ????????????????????????where??????
     * @param queryWrapper QueryWrapper<StoreOrder> ?????????
     * @param status String ??????
     */
    private void getStatusWhere(QueryWrapper<StoreOrder> queryWrapper, String status) {
        if(null == status){
            return;
        }
        Integer orderStatus = null; //????????????
        Integer paidStatus = null; //????????????
        Integer refundStatus = null; //????????????
        Integer deleteStatus = null; //????????????
        Integer systemDeleteStatus = null; //????????????
        Integer shippingType = null; //????????????
        switch (status){
            case Constants.ORDER_STATUS_UNPAID: //?????????
                paidStatus = 0; //????????????
                orderStatus = 0; //????????????
                refundStatus = 0; //????????????
                deleteStatus = 0; //????????????
                systemDeleteStatus = 0; //????????????
                break;
            case Constants.ORDER_STATUS_NOT_SHIPPED: //?????????
                paidStatus = 1; //????????????
                orderStatus = 0; //????????????
                refundStatus = 0; //????????????
                shippingType = 1; //????????????
                deleteStatus = 0; //????????????
                systemDeleteStatus = 0; //????????????
                break;
            case Constants.ORDER_STATUS_SPIKE: //?????????
                paidStatus = 1; //????????????
                orderStatus = 1; //????????????
                refundStatus = 0; //????????????
                deleteStatus = 0; //????????????
                systemDeleteStatus = 0; //????????????
                break;
            case Constants.ORDER_STATUS_BARGAIN: //?????????
                paidStatus = 1; //????????????
                orderStatus = 2; //????????????
                refundStatus = 0; //????????????
                deleteStatus = 0; //????????????
                systemDeleteStatus = 0; //????????????
                break;
            case Constants.ORDER_STATUS_COMPLETE: //????????????
                paidStatus = 1; //????????????
                orderStatus = 3; //????????????
                refundStatus = 0; //????????????
                deleteStatus = 0; //????????????
                systemDeleteStatus = 0; //????????????
                break;
            case Constants.ORDER_STATUS_TOBE_WRITTEN_OFF: //?????????
                paidStatus = 1; //????????????
                orderStatus = 0; //????????????
                refundStatus = 0; //????????????
                shippingType = 2; //????????????
                deleteStatus = 0; //????????????
                systemDeleteStatus = 0; //????????????
                break;
            case Constants.ORDER_STATUS_REFUNDING: //?????????
                paidStatus = 1; //????????????
                refundStatus = 1; //????????????
                deleteStatus = 0; //????????????
                systemDeleteStatus = 0; //????????????
                break;
            case Constants.ORDER_STATUS_REFUNDED: //?????????
                paidStatus = 1; //????????????
                refundStatus = 2; //????????????
                deleteStatus = 0; //????????????
                systemDeleteStatus = 0; //????????????
                break;
            case Constants.ORDER_STATUS_DELETED: //?????????
                deleteStatus = 1; //????????????
                systemDeleteStatus = 1; //????????????,  ???????????????????????????
                break;
            default:
                break;
        }
        if(paidStatus != null){
            queryWrapper.eq("paid", paidStatus);
        }

        if(orderStatus != null){
            queryWrapper.eq("status", orderStatus);
        }

        if(refundStatus != null){
            queryWrapper.eq("refund_status", refundStatus);
        }

        if(shippingType != null){
            queryWrapper.eq("shipping_type", shippingType);
        }

        if(deleteStatus != null){
            if(deleteStatus == 1){
                queryWrapper.eq("is_del", 1);
            }else{
                queryWrapper.eq("is_del", deleteStatus);
            }
        }
        queryWrapper.eq("is_system_del", 0);
//        if(systemDeleteStatus != null){
//            if(deleteStatus == 1 && systemDeleteStatus == 1){
//                queryWrapper.and(i -> i.or().eq("is_del", 1).or().eq("is_system_del", 1));
//            }else{
//                queryWrapper.eq("is_del", deleteStatus);
//            }
//        }




    }

    /**
     * ??????????????????
     * @param storeOrder StoreOrder ????????????
     */
    public Map<String, String> getStatus(StoreOrder storeOrder) {
        Map<String, String> map = new HashMap<>();
        map.put("key", "");
        map.put("value", "");
        if(null == storeOrder){
            return map;
        }
        // ?????????
        if(!storeOrder.getPaid()
                && storeOrder.getStatus() == 0
                && storeOrder.getRefundStatus() == 0
                && !storeOrder.getIsDel()
                && !storeOrder.getIsSystemDel()){
            map.put("key", Constants.ORDER_STATUS_UNPAID);
            map.put("value", Constants.ORDER_STATUS_STR_UNPAID);
            return map;
        }
        // ?????????
        if(storeOrder.getPaid()
                && storeOrder.getStatus() == 0
                && storeOrder.getRefundStatus() == 0
                && storeOrder.getShippingType() == 1
                && !storeOrder.getIsDel()
                && !storeOrder.getIsSystemDel()){
            map.put("key", Constants.ORDER_STATUS_NOT_SHIPPED);
            map.put("value", Constants.ORDER_STATUS_STR_NOT_SHIPPED);
            return map;
        }
        // ?????????
        if(storeOrder.getPaid()
                && storeOrder.getStatus() == 1
                && storeOrder.getRefundStatus() == 0
                && storeOrder.getShippingType() == 1
                && !storeOrder.getIsDel()
                && !storeOrder.getIsSystemDel()){
            map.put("key", Constants.ORDER_STATUS_SPIKE);
            map.put("value", Constants.ORDER_STATUS_STR_SPIKE);
            return map;
        }
        // ?????????
        if(storeOrder.getPaid()
                && storeOrder.getStatus() == 2
                && storeOrder.getRefundStatus() == 0
                && !storeOrder.getIsDel()
                && !storeOrder.getIsSystemDel()){
            map.put("key", Constants.ORDER_STATUS_BARGAIN);
            map.put("value", Constants.ORDER_STATUS_STR_BARGAIN);
            return map;
        }
        // ????????????
        if(storeOrder.getPaid()
                && storeOrder.getStatus() == 3
                && storeOrder.getRefundStatus() == 0
                && !storeOrder.getIsDel()
                && !storeOrder.getIsSystemDel()){
            map.put("key", Constants.ORDER_STATUS_COMPLETE);
            map.put("value", Constants.ORDER_STATUS_STR_COMPLETE);
            return map;
        }
        // ?????????
        if(storeOrder.getPaid()
                && storeOrder.getStatus() == 0
                && storeOrder.getRefundStatus() == 0
                && storeOrder.getShippingType() == 2
                && !storeOrder.getIsDel()
                && !storeOrder.getIsSystemDel()){
            map.put("key", Constants.ORDER_STATUS_TOBE_WRITTEN_OFF);
            map.put("value", Constants.ORDER_STATUS_STR_TOBE_WRITTEN_OFF);
            return map;
        }

        //?????????
        if(storeOrder.getPaid()
                && storeOrder.getRefundStatus() == 1
                && !storeOrder.getIsDel()
                && !storeOrder.getIsSystemDel()){
            map.put("key", Constants.ORDER_STATUS_REFUNDING);
            map.put("value", Constants.ORDER_STATUS_STR_REFUNDING);
            return map;
        }

        //?????????
        if(storeOrder.getPaid()
                && storeOrder.getRefundStatus() == 2
                && !storeOrder.getIsDel()
                && !storeOrder.getIsSystemDel()){
            map.put("key", Constants.ORDER_STATUS_REFUNDED);
            map.put("value", Constants.ORDER_STATUS_STR_REFUNDED);
        }

        //?????????
        if(storeOrder.getPaid()
                && storeOrder.getStatus() == 0
                && !storeOrder.getIsDel()
                && !storeOrder.getIsSystemDel()){
            map.put("key", Constants.ORDER_STATUS_NOT_SHIPPED);
            map.put("value", Constants.ORDER_STATUS_STR_NOT_SHIPPED);
        }

        //?????????
        if(storeOrder.getPaid()
                && storeOrder.getStatus() == 1
                && !storeOrder.getIsDel()
                && !storeOrder.getIsSystemDel()){
            map.put("key", Constants.ORDER_STATUS_SPIKE);
            map.put("value", Constants.ORDER_STATUS_STR_SPIKE);
        }

        //???????????????
        if(storeOrder.getPaid()
                && storeOrder.getStatus() == 2
                && !storeOrder.getIsDel()
                && !storeOrder.getIsSystemDel()){
            map.put("key", Constants.ORDER_STATUS_COMPLETE);
            map.put("value", Constants.ORDER_STATUS_STR_TAKE);
        }


        //?????????
        if(storeOrder.getIsDel() || storeOrder.getIsSystemDel()){
            map.put("key", Constants.ORDER_STATUS_DELETED);
            map.put("value", Constants.ORDER_STATUS_STR_DELETED);
        }

        return map;
    }
    /**
     * ??????????????????
     * @param payType String ????????????
     */
    private String getPayType(String payType) {
        switch (payType){
            case Constants.PAY_TYPE_WE_CHAT:
                return Constants.PAY_TYPE_STR_WE_CHAT;
            case Constants.PAY_TYPE_YUE:
                return Constants.PAY_TYPE_STR_YUE;
            case Constants.PAY_TYPE_OFFLINE:
                return Constants.PAY_TYPE_STR_OFFLINE;
            case Constants.PAY_TYPE_ALI_PAY:
                return Constants.PAY_TYPE_STR_ALI_PAY;
            default:
                return Constants.PAY_TYPE_STR_OTHER;
        }
    }

}

