package com.zbkj.crmeb.front.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.common.PageParamRequest;
import com.constants.Constants;
import com.exception.CrmebException;
import com.utils.CrmebUtil;
import com.utils.DateUtil;
import com.utils.RedisUtil;
import com.zbkj.crmeb.bargain.model.StoreBargain;
import com.zbkj.crmeb.bargain.service.StoreBargainService;
import com.zbkj.crmeb.combination.model.StoreCombination;
import com.zbkj.crmeb.combination.service.StoreCombinationService;
import com.zbkj.crmeb.express.model.ShippingTemplates;
import com.zbkj.crmeb.express.service.LogisticService;
import com.zbkj.crmeb.express.service.ShippingTemplatesService;
import com.zbkj.crmeb.express.vo.LogisticsResultVo;
import com.zbkj.crmeb.front.request.*;
import com.zbkj.crmeb.front.response.*;
import com.zbkj.crmeb.front.service.OrderService;
import com.zbkj.crmeb.front.vo.OrderAgainVo;
import com.zbkj.crmeb.marketing.response.StoreCouponUserResponse;
import com.zbkj.crmeb.payment.service.OrderPayService;
import com.zbkj.crmeb.payment.vo.wechat.CreateOrderResponseVo;
import com.zbkj.crmeb.seckill.model.StoreSeckill;
import com.zbkj.crmeb.seckill.service.StoreSeckillService;
import com.zbkj.crmeb.sms.service.SmsService;
import com.zbkj.crmeb.store.model.StoreCart;
import com.zbkj.crmeb.store.model.StoreOrder;
import com.zbkj.crmeb.store.model.StoreOrderInfo;
import com.zbkj.crmeb.store.request.StoreOrderInfoSearchRequest;
import com.zbkj.crmeb.store.request.StoreProductReplyAddRequest;
import com.zbkj.crmeb.store.response.StoreCartResponse;
import com.zbkj.crmeb.store.service.*;
import com.zbkj.crmeb.store.utilService.OrderUtils;
import com.zbkj.crmeb.store.vo.StoreOrderInfoVo;
import com.zbkj.crmeb.system.model.SystemAttachment;
import com.zbkj.crmeb.system.model.SystemStore;
import com.zbkj.crmeb.system.service.SystemAttachmentService;
import com.zbkj.crmeb.system.service.SystemConfigService;
import com.zbkj.crmeb.system.service.SystemStoreService;
import com.zbkj.crmeb.user.model.User;
import com.zbkj.crmeb.user.model.UserAddress;
import com.zbkj.crmeb.user.service.UserAddressService;
import com.zbkj.crmeb.user.service.UserService;
import com.zbkj.crmeb.wechat.service.impl.WechatSendMessageForMinService;
import com.zbkj.crmeb.wechat.vo.WechatSendMessageForCreateOrder;
import com.zbkj.crmeb.wechat.vo.WechatSendMessageForReFundNotify;
import org.apache.catalina.Store;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * H5???????????????
 */
@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private UserService userService;

    @Autowired
    private StoreCartService storeCartService;

    @Autowired
    private StoreOrderService storeOrderService;

    @Autowired
    private StoreOrderInfoService storeOrderInfoService;

    @Autowired
    private StoreOrderStatusService storeOrderStatusService;

    @Autowired
    private ShippingTemplatesService shippingTemplatesService;

    @Autowired
    private UserAddressService userAddressService;

    @Autowired
    private SystemConfigService systemConfigService;

    @Autowired
    private StoreProductReplyService storeProductReplyService;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private SystemStoreService systemStoreService;


    @Autowired
    private OrderPayService orderPayService;

    @Autowired
    private OrderUtils orderUtils;

    @Autowired
    private SmsService smsService;

    @Autowired
    private SystemAttachmentService systemAttachmentService;

    @Autowired
    private LogisticService logisticsService;

    @Autowired
    private WechatSendMessageForMinService wechatSendMessageForMinService;

    @Autowired
    private StoreSeckillService storeSeckillService;

    @Autowired
    private StoreCombinationService storeCombinationService;

    @Autowired
    private StoreBargainService storeBargainService;

    /**
     * ????????????
     * @param cartIds ?????????id??????
     * @param isNew ??????????????????=????????????????????????
     * @return ????????????response
     */
    @Override
    public ConfirmOrderResponse confirmOrder(List<String> cartIds, boolean isNew, boolean addAgain,boolean seckill, boolean bargain, boolean combination, Integer addressId) {
        ConfirmOrderResponse response = new ConfirmOrderResponse();
        // ????????????????????????
        // ShippingTemplates template = shippingTemplatesService.getById(69);
        // if (null == template)
        ShippingTemplates template = shippingTemplatesService.getById(1);
        if (null == template) throw new CrmebException("????????????????????????????????????");
        User currentUserInfo = userService.getInfoException();

        List<StoreCartResponse> storeCartResponse;
        // ???????????????????????????????????????
        if (addAgain || seckill || bargain || combination) { // ???redis??????????????????????????????????????????????????????????????????????????????????????????
            String cacheOrderData = orderUtils.getCacheOrderData(cartIds.get(0) + "");
            if (StrUtil.isBlank(cacheOrderData)) throw new CrmebException("?????????????????????");
            storeCartResponse = JSONObject.parseArray(cacheOrderData, StoreCartResponse.class);
            if (seckill) {
                // ????????????????????????
                StoreCart storeCartPram = new StoreCart()
                        .setSeckillId(storeCartResponse.get(0).getSeckillId())
                        .setUid(currentUserInfo.getUid())
                        .setProductAttrUnique(storeCartResponse.get(0).getProductAttrUnique());
                orderUtils.validSecKill(storeCartPram, currentUserInfo);
            }
            if (bargain) {
                // ????????????????????????
                StoreCart storeCartPram = new StoreCart()
                        .setBargainId(storeCartResponse.get(0).getBargainId())
                        .setUid(currentUserInfo.getUid())
                        .setProductAttrUnique(storeCartResponse.get(0).getProductAttrUnique());
                orderUtils.validBargain(storeCartPram, currentUserInfo);
            }
            if (combination) {
                // ????????????????????????
                StoreCart storeCartPram = new StoreCart()
                        .setCombinationId(storeCartResponse.get(0).getCombinationId())
                        .setUid(currentUserInfo.getUid())
                        .setProductAttrUnique(storeCartResponse.get(0).getProductAttrUnique())
                        .setCartNum(storeCartResponse.get(0).getCartNum());
                if (ObjectUtil.isNotNull(storeCartResponse.get(0).getPinkId())) {
                    storeCartPram.setPinkId(storeCartResponse.get(0).getPinkId());
                }
                orderUtils.validCombination(storeCartPram, currentUserInfo);
            }
        }else if(isNew){// ????????????????????????
            storeCartResponse = storeCartService.getListByUserIdAndCartIds(currentUserInfo.getUid(),cartIds,1);
        }else{ // ?????????????????????
            storeCartResponse = storeCartService.getListByUserIdAndCartIds(currentUserInfo.getUid(),cartIds,null);
        }

        // ?????????????????????????????????????????????????????????????????????
        UserAddress defaultAddress;
        if (addressId > 0) {// ?????????????????????
            defaultAddress = userAddressService.getById(addressId);
        } else {// ??????????????????
            defaultAddress = userAddressService.getDefault();
        }

        // ??????????????????
        PriceGroupResponse orderPriceGroup = orderUtils.getOrderPriceGroup(storeCartResponse, defaultAddress);

        // other
        HashMap<String, Object> otherMap = new HashMap<>();
        otherMap.put("offlinePostage",systemConfigService.getValueByKey("offline_postage"));
        otherMap.put("integralRatio",systemConfigService.getValueByKey("integral_ratio"));

        // ?????????????????????
        List<StoreCouponUserResponse> canUseUseCouponList = orderUtils.getCanUseCouponList(storeCartResponse);

        // ??????response??????
        StoreCouponUserResponse canUserCoupon = null;
        if(null != canUseUseCouponList && canUseUseCouponList.size() > 0){
            canUserCoupon = canUseUseCouponList.get(0);
        }
        response.setUsableCoupon(canUserCoupon);
        response.setAddressInfo(defaultAddress);
        response.setCartInfo(storeCartResponse);
        response.setPriceGroup(orderPriceGroup);
        response.setOfflinePostage(otherMap.get("offlinePostage").toString());
        response.setIntegralRatio(otherMap.get("integralRatio").toString());
        response.setUserInfo(currentUserInfo);
        response.setOfflinePayStatus(systemConfigService.getValueByKey("offline_pay_status"));
        response.setYuePayStatus(
                (systemConfigService.getValueByKey("balance_func_status").equals("1")
                        && systemConfigService.getValueByKey("yue_pay_status").equals("1"))? "1":"2"); // 1?????? 2??????
        response.setPayWeixinOpen(systemConfigService.getValueByKey("pay_weixin_open"));
        response.setStoreSelfMention(systemConfigService.getValueByKey("store_self_mention"));
        response.setOther(otherMap);
        response.setSystemStore(null);

        response.setSecKillId(Optional.ofNullable(storeCartResponse.get(0).getSeckillId()).orElse(0));
        response.setBargainId(Optional.ofNullable(storeCartResponse.get(0).getBargainId()).orElse(0));
        response.setCombinationId(Optional.ofNullable(storeCartResponse.get(0).getCombinationId()).orElse(0));
        response.setPinkId(Optional.ofNullable(storeCartResponse.get(0).getPinkId()).orElse(0));
        response.setOrderKey(orderUtils.cacheSetOrderInfo(currentUserInfo.getUid(), response));
        return response;
    }

    /**
     * ????????????
     * @param request ??????????????????
     * @param orderKey orderKey
     * @return ??????????????????
     */
    @Override
    public OrderPayResponse createOrder(OrderCreateRequest request, String orderKey, String ip) {
        OrderPayResponse orderPayResponse = new OrderPayResponse();
        User currentUser = userService.getInfo();
        if (ObjectUtil.isNull(currentUser)) throw new CrmebException("????????????????????????");
        // ????????????????????????
        if(orderUtils.checkOrderExist(orderKey, currentUser.getUid())) throw new CrmebException(orderKey + "???????????????");

        // ??????????????????
        if(!orderUtils.checkPayType(request.getPayType())) throw new CrmebException("??????????????????????????????????????????????????????????????????");

        // ????????????????????????
        String existCacheOrder = orderUtils.cacheGetOrderInfo(currentUser.getUid(), orderKey);
        if(null == existCacheOrder) throw new CrmebException("???????????????,?????????????????????!");

        ConfirmOrderResponse cor = JSONObject.parseObject(existCacheOrder,ConfirmOrderResponse.class);

        // ????????????????????????
        orderUtils.computedOrder(request, cor, orderKey);

        // ??????????????????
         StoreOrder orderCreated = orderUtils.createOrder(request, cor, 0, request.getStoreId(), orderKey);
        if(null == orderCreated)
            throw new CrmebException("??????????????????");

        // ??????????????????
        if(request.getIsNew()){
            HashMap<String, Object> resultMap = new HashMap<>();
            OrderPayRequest orderPayRequest = new OrderPayRequest();
            orderPayRequest.setFrom(request.getFrom());
            orderPayRequest.setPaytype(request.getPayType());
            orderPayRequest.setUni(orderCreated.getOrderId());
            boolean b = doPayOrder(orderPayRequest, ip, resultMap, orderCreated);
            OrderPayItemResponse itemResponse = new OrderPayItemResponse();
            String payType = null;
            switch (request.getPayType()){
                case Constants.PAY_TYPE_WE_CHAT:
                case Constants.PAY_TYPE_WE_CHAT_FROM_PROGRAM:
                    payType = "???????????????????????? ?????????";
                    orderPayResponse.setStatus("WECHAT_PAY");
                    orderPayResponse.setJsConfig(resultMap.get("jsConfig"));
                    break;
                case Constants.PAY_TYPE_YUE:
                    payType = "??????????????????";
                    orderPayResponse.setStatus("SUCCESS");
                    break;
                case Constants.PAY_TYPE_OFFLINE:
                    payType = "????????????";
                    orderPayResponse.setStatus("SUCCESS");
                    break;
            }
            orderPayResponse.setMessage(payType);
            itemResponse.setKey(orderCreated.getOrderId());
            itemResponse.setOrderId(orderKey);
            orderPayResponse.setResult(itemResponse);
        }else{
            String tempStatus = "";
            switch (request.getPayType()){
                case Constants.PAY_TYPE_WE_CHAT:tempStatus="WECHAT_H5_PAY";  break;
                case Constants.PAY_TYPE_YUE:tempStatus="SUCCESS";  break;
                case Constants.PAY_TYPE_OFFLINE:tempStatus="SUCCESS";  break;
            }
            orderPayResponse.setStatus(tempStatus);
            OrderPayItemResponse itemResponse = new OrderPayItemResponse();
            itemResponse.setKey(orderCreated.getOrderId());
            itemResponse.setOrderId(orderKey);
            orderPayResponse.setResult(itemResponse);
        }

        // ?????????????????????
        List<StoreCartResponse> cartInfo = cor.getCartInfo();
        List<StoreCartResponse> cartList = cartInfo.stream().filter(i -> ObjectUtil.isNotNull(i.getId())).collect(Collectors.toList());
        if (CollUtil.isNotEmpty(cartList)) {
            List<Integer> cartIdList = cartList.stream().map(temp -> temp.getId().intValue()).collect(Collectors.toList());
            storeCartService.deleteCartByIds(cartIdList);
        }

        // ???????????????????????????
        WechatSendMessageForCreateOrder createOrder = new WechatSendMessageForCreateOrder(
                orderUtils.getPayTypeStrByOrder(orderCreated),orderUtils.getStoreNameAndCarNumString(orderCreated.getId()),
                orderCreated.getPayPrice()+"",orderCreated.getId()+"","CRMEB",orderCreated.getCreateTime()+"","??????????????????","????????????",
                orderUtils.getPayTypeStrByOrder(orderCreated),orderCreated.getUserAddress());
        wechatSendMessageForMinService.sendCreateOrderMessage(createOrder, userService.getUserIdException());

        return orderPayResponse;
    }

    /**
     * ?????????????????????
     * @param id Integer ??????id
     * @return ????????????????????????
     */
    @Override
    @Transactional(rollbackFor = {RuntimeException.class, Error.class, CrmebException.class})
    public Boolean delete(Integer id) {
        try{
            StoreOrder storeOrder = orderUtils.getInfoById(id);

            Map<String, String> statusMap = storeOrderService.getStatus(storeOrder);
            String orderStatus = statusMap.get("key");

            //??????????????????
            orderUtils.checkDeleteStatus(orderStatus);

            //????????????
            storeOrder.setIsDel(true);
            boolean result = storeOrderService.updateById(storeOrder);

            //??????????????????redis
            redisUtil.lPush(Constants.ORDER_TASK_REDIS_KEY_AFTER_DELETE_BY_USER, id);

            return result;

        }catch (Exception e){
            throw new CrmebException("???????????? " + e.getMessage());
        }

    }

    /**
     * ????????????
     * @param request StoreProductReplyAddRequest ????????????
     */
    @Override
    public boolean reply(StoreProductReplyAddRequest request) {
        StoreOrder storeOrderPram = new StoreOrder();
        storeOrderPram.setId(request.getOid());
        storeOrderPram.setUid(userService.getUserIdException());
        StoreOrder existStoreOrder = storeOrderService.getByEntityOne(storeOrderPram);
        if(null == existStoreOrder) throw new CrmebException("??????????????????");
        // ???????????? ????????????????????????????????? ????????????productId?????????
        if(null != existStoreOrder.getSeckillId() && existStoreOrder.getSeckillId() > 0){
            StoreSeckill currentSeckill = storeSeckillService.getById(existStoreOrder.getSeckillId());
            request.setProductId(currentSeckill.getProductId());

        }
        // ????????????
        if (ObjectUtil.isNotNull(existStoreOrder.getCombinationId()) && existStoreOrder.getCombinationId() > 0) {
            StoreCombination currentCombination = storeCombinationService.getById(existStoreOrder.getCombinationId());
            request.setProductId(currentCombination.getProductId());
        }
        // ????????????
        if (ObjectUtil.isNotNull(existStoreOrder.getBargainId()) && existStoreOrder.getBargainId() > 0) {
            StoreBargain tempBargain = storeBargainService.getById(existStoreOrder.getBargainId());
            request.setProductId(tempBargain.getProductId());
        }
        return storeProductReplyService.create(request);
    }

    /**
     * ????????????
     * @param id Integer ??????id
     */
    @Override
    public boolean take(Integer id) {
        try{
            StoreOrder storeOrder = orderUtils.getInfoById(id);
            if(!storeOrder.getStatus().equals(Constants.ORDER_STATUS_INT_SPIKE)){
                throw new CrmebException("??????????????????");
            }

            //?????????????????????
            storeOrder.setStatus(Constants.ORDER_STATUS_INT_BARGAIN);
            boolean result = storeOrderService.updateById(storeOrder);

            //??????????????????redis
            redisUtil.lPush(Constants.ORDER_TASK_REDIS_KEY_AFTER_TAKE_BY_USER, id);
            return result;
        }catch (Exception e){
            throw new CrmebException("????????????" + e.getMessage());
        }
    }

    /**
     * ????????????
     * @param id Integer ??????id
     */
    @Override
    public boolean cancel(Integer id) {
        StoreOrder storeOrder = orderUtils.getInfoById(id);
        //?????????????????????
        storeOrder.setIsDel(true);
        boolean result = storeOrderService.updateById(storeOrder);

        //??????????????????redis
        redisUtil.lPush(Constants.ORDER_TASK_REDIS_KEY_AFTER_CANCEL_BY_USER, id);
        return result;
    }

    /**
     * ?????????????????????
     * @param request ????????????
     */
    @Override
    public boolean refundVerify(OrderRefundVerifyRequest request) {
        User currentUser = userService.getInfo();
        StoreOrder storeOrderPram = new StoreOrder();
        storeOrderPram.setOrderId(request.getUni());
        storeOrderPram.setIsDel(false);
        storeOrderPram.setPaid(true);
        StoreOrder existStoreOrder = storeOrderService.getByEntityOne(storeOrderPram);
        if(null == existStoreOrder) throw new CrmebException("?????????????????????");
        if(existStoreOrder.getRefundStatus() == 2) throw new CrmebException("???????????????");
        if(existStoreOrder.getRefundStatus() == 1) throw new CrmebException("?????????????????????");
        if(existStoreOrder.getStatus() == 1) throw new CrmebException("????????????????????????");

        storeOrderStatusService.createLog(existStoreOrder.getId(), Constants.ORDER_LOG_REFUND_APPLY,"???????????????????????????" + request.getRefund_reason_wap_explain());

        existStoreOrder.setRefundStatus(1);
        existStoreOrder.setStatus(-1);
        existStoreOrder.setRefundReasonTime(DateUtil.nowDateTime());
        existStoreOrder.setRefundReasonWap(request.getText());
        existStoreOrder.setRefundReasonWapExplain(request.getRefund_reason_wap_explain());
        existStoreOrder.setRefundReasonWapImg(systemAttachmentService.clearPrefix(request.getRefund_reason_wap_img()));
        boolean updateOrderResult = storeOrderService.updateById(existStoreOrder);
        if(!updateOrderResult) throw new CrmebException("??????????????????");

        HashMap<String, Object> smsInfo = new HashMap<>();
        smsInfo.put("orderId", existStoreOrder.getOrderId());
        smsInfo.put("adminName", currentUser.getNickname());
        boolean codeResult = smsService.pushCodeToList(currentUser.getPhone(),1, smsInfo);
        if(!codeResult) throw new CrmebException("??????????????????????????????");
        return true;
    }

    /**
     * ??????????????????
     * @param request OrderRefundApplyRequest ????????????
     */
    @Override
    public boolean refundApply(OrderRefundApplyRequest request) {
        StoreOrder storeOrder = orderUtils.getInfoById(request.getId());
        if(storeOrder.getRefundStatus() == 1){
            throw new CrmebException("?????????????????????");
        }

        if(storeOrder.getRefundStatus() == 2){
            throw new CrmebException("???????????????");
        }

        if(storeOrder.getStatus() == 1){
            throw new CrmebException("????????????????????????");
        }
        storeOrder.setRefundReasonWapImg(systemAttachmentService.clearPrefix(request.getReasonImage()));
        storeOrder.setRefundStatus(1);
        storeOrder.setRefundReasonWapExplain(request.getExplain());
        storeOrder.setRefundReason(request.getText());
        storeOrder.setRefundPrice(storeOrder.getPayPrice());
        storeOrder.setRefundReasonTime(DateUtil.nowDateTime());

        // ?????????????????????????????????
        String storeNameAndCarNumString = orderUtils.getStoreNameAndCarNumString(storeOrder.getId());
        if(StringUtils.isNotBlank(storeNameAndCarNumString)){
            WechatSendMessageForReFundNotify notify = new WechatSendMessageForReFundNotify(
                    storeNameAndCarNumString,storeOrder.getPayPrice().toString(),
                    storeOrder.getCreateTime().toString(),storeOrder.getOrderId()+"",DateUtil.nowDateTimeStr(),
                    "CRMEB","????????????",request.getExplain(),storeOrder.getPayPrice()+"",
                    request.getText(),storeOrder.getUserPhone(),"CRMEB");
            wechatSendMessageForMinService.sendReFundNotifyMessage(notify, userService.getUserId());
        }
        return storeOrderService.updateById(storeOrder);
    }

    /**
     * ??????????????????Task??????
     * @param applyList OrderRefundApplyRequest ????????????
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean refundApplyTask(List<OrderRefundApplyRequest> applyList) {
        if (CollUtil.isEmpty(applyList)) {
            return false;
        }
        List<StoreOrder> orderList = CollUtil.newArrayList();
        List<Map<String, Object>> notifyMapList = CollUtil.newArrayList();
        for (OrderRefundApplyRequest request : applyList) {
            StoreOrder storeOrder = storeOrderService.getById(request.getId());
            if(ObjectUtil.isNull(storeOrder)){
                //???????????????
                throw new CrmebException("??????????????????????????????!");
            }
            if(storeOrder.getRefundStatus() == 1){
                throw new CrmebException("?????????????????????");
            }

            if(storeOrder.getRefundStatus() == 2){
                throw new CrmebException("???????????????");
            }

            if(storeOrder.getStatus() == 1){
                throw new CrmebException("????????????????????????");
            }
            storeOrder.setRefundReasonWapImg(systemAttachmentService.clearPrefix(request.getReasonImage()));
            storeOrder.setRefundStatus(1);
            storeOrder.setRefundReasonWapExplain(request.getExplain());
            storeOrder.setRefundReason(request.getText());
            storeOrder.setRefundPrice(BigDecimal.ZERO);
            storeOrder.setRefundReasonTime(DateUtil.nowDateTime());
            orderList.add(storeOrder);

            // ?????????????????????????????????
            String storeNameAndCarNumString = orderUtils.getStoreNameAndCarNumString(storeOrder.getId());
            if(StringUtils.isNotBlank(storeNameAndCarNumString)){
                WechatSendMessageForReFundNotify notify = new WechatSendMessageForReFundNotify(
                        storeNameAndCarNumString,storeOrder.getPayPrice().toString(),
                        storeOrder.getCreateTime().toString(),storeOrder.getOrderId()+"",DateUtil.nowDateTimeStr(),
                        "CRMEB","????????????",request.getExplain(),storeOrder.getPayPrice()+"",
                        request.getText(),storeOrder.getUserPhone(),"CRMEB");
                Map<String, Object> map = CollUtil.newHashMap();
                map.put("notify", notify);
                map.put("uid", storeOrder.getUid());
                notifyMapList.add(map);
            }
        }

        boolean batch = storeOrderService.updateBatchById(orderList, 100);
        if (batch && notifyMapList.size() > 0) {
            // ?????????????????????????????????
            notifyMapList.forEach(i -> {
                WechatSendMessageForReFundNotify notify = (WechatSendMessageForReFundNotify) i.get("notify");
                Integer uid = (Integer) i.get("uid");
                wechatSendMessageForMinService.sendReFundNotifyMessage(notify, uid);
            });
        }

        return batch;
    }


    /**
     * ????????????
     * @param request ??????
     * @return ????????????
     */
    @Override
    public HashMap<String, Object> againOrder(OrderAgainRequest request) {
        // ????????????????????????????????????????????????
//        StoreCartResponse cacheOrderAgain = orderUtils.getCacheOrderAgain(request.getNui());
//        if(null != cacheOrderAgain) throw new CrmebException("??????????????????????????????");

        HashMap<String, Object> resultMap = new HashMap<>();
        User currentUser = userService.getInfo();
        StoreOrder storeOrder = new StoreOrder();
        storeOrder.setUid(currentUser.getUid());
        storeOrder.setUnique(request.getNui());
        StoreOrder storeOrderExist = storeOrderService.getInfoByEntity(storeOrder);
        if(null == storeOrderExist) throw new CrmebException("???????????????");
        OrderAgainVo orderAgainVo = orderUtils.tidyOrder(storeOrderExist, true, false);
//        List<StoreCart> storeCartResultList = new ArrayList<>();
        for (StoreOrderInfoVo oldCartInfo : orderAgainVo.getCartInfo()) { // todo ??????????????????????????????
            // todo ????????????????????????
            List<String> orderAgainCacheKeys = storeOrderService.addCartAgain(userService.getUserIdException(), oldCartInfo.getProductId(), oldCartInfo.getInfo().getCartNum(),
                    storeOrderExist.getUnique(), oldCartInfo.getInfo().getType(), true,
                    oldCartInfo.getInfo().getCombinationId(), oldCartInfo.getInfo().getSeckillId(), oldCartInfo.getInfo().getBargainId());
            resultMap.put("cateId",orderAgainCacheKeys.get(0));

        }
        if(resultMap.size() == 0) throw new CrmebException("????????????????????????????????????");
//        resultMap.put("cateId",storeCartResultList.stream().map(StoreCart::getId).distinct().collect(Collectors.toList()));
        return resultMap;
    }

    /**
     * ????????????
     * @param request ????????????
     * @return ????????????
     */
    @Override
    @Transactional(rollbackFor = {RuntimeException.class, Error.class, CrmebException.class})
    public HashMap<String, Object> payOrder(OrderPayRequest request, String ip) {
        HashMap<String, Object> resultMap = new HashMap<>();
        User currentUser = userService.getInfo();
        StoreOrder storeOrderPram = new StoreOrder();
        storeOrderPram.setOrderId(request.getUni());
        storeOrderPram.setUid(currentUser.getUid());
        storeOrderPram.setIsDel(false);
        StoreOrder existStoreOrder = storeOrderService.getInfoByEntity(storeOrderPram);
        if(null == existStoreOrder) throw new CrmebException("???????????????");
        if(existStoreOrder.getPaid()) throw new CrmebException("??????????????????");
        // ??????????????????????????????
        if(!existStoreOrder.getPayType().equals(request.getPaytype())){
            boolean changePayTypeResult = changePayType(request.getPaytype(), existStoreOrder.getOrderId());
            if(!changePayTypeResult) throw new CrmebException("??????????????????????????????");
        }
        // ??????
        if (doPayOrder(request, ip, resultMap, existStoreOrder)) return resultMap;
        throw new CrmebException("??????????????????");
    }

    /**
     * ????????????
     * @param status ??????
     * @param pageRequest ??????
     */
    @Override
    public List<OrderAgainVo> list(Integer status, PageParamRequest pageRequest) {
        List<OrderAgainVo> listResponses = new ArrayList<>();
        User currentUser = userService.getInfo();
        StoreOrder storeOrderPram = new StoreOrder();
        storeOrderPram.setUid(currentUser.getUid());
        storeOrderPram.setStatus(status);

        List<StoreOrder> orderList = storeOrderService.getUserOrderList(storeOrderPram, pageRequest);
        for (StoreOrder storeOrder : orderList) {
            OrderAgainVo orderAgainVo = new OrderAgainVo();
            StoreOrder storeOrderResult = new StoreOrder();
            BeanUtils.copyProperties(storeOrder, storeOrderResult);
            orderAgainVo.setStoreOrder(storeOrderResult);
            orderAgainVo = orderUtils.tidyOrder(storeOrder, true, false);
            if(null != orderAgainVo.getStatus() && orderAgainVo.getStatus().getType() == 3){
                for (StoreOrderInfoVo storeOrderInfoVo : orderAgainVo.getCartInfo()) {
                    if(orderAgainVo.getStatus().getType() == 3){
                        storeOrderInfoVo.getInfo().setIsReply(
                                storeProductReplyService.isReply(storeOrderInfoVo.getUnique(),"product",storeOrderInfoVo.getOrderId()).size());
                        storeOrderInfoVo.getInfo().setAddTime(storeOrderInfoVo.getInfo().getAddTime());
                    }
                }
            }
            listResponses.add(orderAgainVo);
        }
        return listResponses;
    }

    /**
     * ????????????
     * @param orderId ??????id
     */
    @Override
    public StoreOrderDetailResponse detailOrder(String orderId) {
        StoreOrderDetailResponse storeOrderDetailResponse = new StoreOrderDetailResponse();
        User currentUser = userService.getInfo();
        StoreOrder storeOrderPram = new StoreOrder();
        storeOrderPram.setUid(currentUser.getUid());
        storeOrderPram.setUnique(orderId);
        storeOrderPram.setIsDel(false);
        StoreOrder storeOrderResult = storeOrderService.getInfoJustOrderInfo(storeOrderPram);
        if(null == storeOrderResult) throw new CrmebException("???????????????");

        BeanUtils.copyProperties(storeOrderResult, storeOrderDetailResponse);
        // ????????????????????????
        String storeSelfMention = systemConfigService.getValueByKey("store_self_mention");
        if(!Boolean.valueOf(storeSelfMention)) storeOrderResult.setShippingType(1);
        if(storeOrderResult.getVerifyCode().length()>0){
            String verifyCode = storeOrderResult.getVerifyCode();
            List<String> verifyCodeList = new ArrayList<>();
            verifyCodeList.add(verifyCode.substring(0,4));
            verifyCodeList.add(verifyCode.substring(4,4));
            verifyCodeList.add(verifyCode.substring(8));
            storeOrderDetailResponse.setPVerifyCodes(verifyCodeList);
        }
        Date dateY = DateUtil.strToDate(storeOrderResult.getCreateTime().toString(), Constants.DATE_FORMAT_DATE);
        Date dateH = DateUtil.strToDate(storeOrderResult.getCreateTime().toString(), Constants.DATE_FORMAT_HHMMSS);
        storeOrderDetailResponse.setAddTimeH(dateY);
        storeOrderDetailResponse.setAddTimeH(dateH);
        SystemStore systemStorePram = new SystemStore();
//        systemStorePram.setIsShow(true);
//        systemStorePram.setIsDel(false);
        systemStorePram.setId(storeOrderResult.getStoreId());
        storeOrderDetailResponse.setSystemStore(systemStoreService.getByCondition(systemStorePram));

        if(storeOrderResult.getShippingType() == 3 && storeOrderResult.getVerifyCode().length() > 0){
            String name = storeOrderResult.getVerifyCode()+".jpg";
            SystemAttachment systemAttachmentPram = new SystemAttachment();
            systemAttachmentPram.setName(name);
            // todo ?????????????????????
        }
        storeOrderDetailResponse.setMapKey(systemConfigService.getValueByKey("tengxun_map_key"));
//        StoreOrder storeOrder = new StoreOrder();
//        BeanUtils.copyProperties(storeOrderDetailResponse,storeOrder);
        OrderAgainVo orderAgainVo = orderUtils.tidyOrder(storeOrderResult, true, true);
        BeanUtils.copyProperties(orderAgainVo.getStoreOrder(), storeOrderDetailResponse);
        storeOrderDetailResponse.setCartInfo(orderAgainVo.getCartInfo());
        storeOrderDetailResponse.setPStatus(orderAgainVo.getStatus());
        storeOrderDetailResponse.setStatusPic(orderAgainVo.getStatusPic());
        return storeOrderDetailResponse;
    }

    /**
     * ??????tap data
     * @return ?????????????????????
     */
    @Override
    public OrderDataResponse orderData() {
        OrderDataResponse result = new OrderDataResponse();
        User currentUser = userService.getInfo();

        StoreOrder storeOrderOrderCountPram = new StoreOrder();
        storeOrderOrderCountPram.setIsDel(false).setPaid(true).setUid(currentUser.getUid()).setRefundStatus(0);
        result.setOrderCount(storeOrderService.getByEntity(storeOrderOrderCountPram).size());

        StoreOrder storeOrderSumPricePram = new StoreOrder();
        storeOrderSumPricePram.setIsDel(false).setPaid(true).setUid(currentUser.getUid()).setRefundStatus(0);
        List<StoreOrder> storeOrdersSumPriceResult = storeOrderService.getByEntity(storeOrderSumPricePram);
        double sumPrice = storeOrdersSumPriceResult.stream().mapToDouble(e -> e.getPayPrice().doubleValue()).sum();
        result.setSumPrice(BigDecimal.valueOf(sumPrice));

        // ?????????
        result.setUnPaidCount(storeOrderService.getTopDataUtil(Constants.ORDER_STATUS_H5_UNPAID, currentUser.getUid()).size());
        // ?????????
        result.setUnShippedCount(storeOrderService.getTopDataUtil(Constants.ORDER_STATUS_H5_NOT_SHIPPED, currentUser.getUid()).size());
        // ?????????
        result.setReceivedCount(storeOrderService.getTopDataUtil(Constants.ORDER_STATUS_H5_SPIKE, currentUser.getUid()).size());
        // ?????????
        result.setEvaluatedCount(storeOrderService.getTopDataUtil(Constants.ORDER_STATUS_H5_JUDGE, currentUser.getUid()).size());
        // ?????????
        result.setCompleteCount(storeOrderService.getTopDataUtil(Constants.ORDER_STATUS_H5_COMPLETE, currentUser.getUid()).size());
        // ?????????????????????
        result.setRefundCount(storeOrderService.getTopDataUtil(Constants.ORDER_STATUS_H5_REFUND, currentUser.getUid()).size());
        return result;
    }

    /**
     * ??????????????????
     * @return ??????????????????
     */
    @Override
    public List<String> getRefundReason(){
        String reasonString = systemConfigService.getValueByKey("stor_reason");
        reasonString = CrmebUtil.UnicodeToCN(reasonString);
        reasonString = reasonString.replace("rn", "n");
        return Arrays.asList(reasonString.split("\\n"));
    }

    /**
     * ??????????????????
     * @param request ??????????????????
     * @param orderKey ??????key
     * @return ??????????????????
     */
    @Override
    public HashMap<String, Object> computedOrder(OrderComputedRequest request, String orderKey) {
        HashMap<String, Object> resultMap = new HashMap<>();
        User currentUser = userService.getInfoException();
        // ???????????????????????? --?????????????????????
        if(orderUtils.checkOrderExist(orderKey, currentUser.getUid())) {
            OrderPayItemResponse itemResponse = new OrderPayItemResponse(orderKey,orderKey);
            OrderPayResponse orderPayResponse = new OrderPayResponse("extend_order",itemResponse);
            resultMap.put("result", orderPayResponse);
            return  resultMap;
        }

        // ????????????????????????????????????
        String existCacheOrder = orderUtils.cacheGetOrderInfo(userService.getUserIdException(), orderKey);
        if(null == existCacheOrder) throw new CrmebException("???????????????,?????????????????????!");
        ConfirmOrderResponse cor = JSONObject.parseObject(existCacheOrder,ConfirmOrderResponse.class);

        // ????????????????????????
        if(null != cor.getSecKillId() && cor.getSecKillId()>0){
            StoreCart storeCartPram = new StoreCart()
                    .setSeckillId(cor.getCartInfo().get(0).getSeckillId())
                    .setUid(currentUser.getUid())
                    .setProductAttrUnique(cor.getCartInfo().get(0).getProductAttrUnique());
            orderUtils.validSecKill(storeCartPram, currentUser);
        }

        // ????????????????????????
        if (ObjectUtil.isNotNull(cor.getBargainId()) && cor.getBargainId() > 0) {
            StoreCart storeCartPram = new StoreCart()
                    .setBargainId(cor.getCartInfo().get(0).getBargainId())
                    .setUid(currentUser.getUid())
                    .setProductAttrUnique(cor.getCartInfo().get(0).getProductAttrUnique());
            orderUtils.validBargain(storeCartPram, currentUser);
        }

        // ????????????????????????
        if (ObjectUtil.isNotNull(cor.getCombinationId()) && cor.getCombinationId() >0) {
            StoreCart storeCartPram = new StoreCart()
                    .setCombinationId(cor.getCartInfo().get(0).getCombinationId())
                    .setUid(currentUser.getUid())
                    .setCartNum(cor.getCartInfo().get(0).getCartNum())
                    .setProductAttrUnique(cor.getCartInfo().get(0).getProductAttrUnique());
            orderUtils.validCombination(storeCartPram, currentUser);
        }

        OrderCreateRequest orderCreateRequest = new OrderCreateRequest();
        BeanUtils.copyProperties(request,orderCreateRequest);
        ComputeOrderResponse priceGroup = orderUtils.computedOrder(orderCreateRequest, cor, orderKey);
        if(null == priceGroup){
            throw new CrmebException("????????????");
        }else{
            OrderPayItemResponse resultItemResponse = new OrderPayItemResponse(orderKey,priceGroup);
            OrderPayResponse resultOrderPayResponse = new OrderPayResponse("NONE",resultItemResponse);
            resultMap.put("result", resultOrderPayResponse);
            return resultMap;
        }
    }

    /**
     * ??????????????????
     * @param orderId ??????id
     */
    @Override
    public Object expressOrder(String orderId) {
        HashMap<String,Object> resultMap = new HashMap<>();
        StoreOrder storeOrderPram = new StoreOrder();
        storeOrderPram.setOrderId(orderId);
        StoreOrder existOrder = storeOrderService.getByEntityOne(storeOrderPram);
        if(null== existOrder) throw new CrmebException("????????????????????????");
        if(!existOrder.getDeliveryType().equals(Constants.ORDER_LOG_EXPRESS) || StringUtils.isBlank(existOrder.getDeliveryType()))
            throw new CrmebException("?????????????????????????????????");

        LogisticsResultVo expressInfo = logisticsService.info(existOrder.getDeliveryId(), null, Optional.ofNullable(existOrder.getDeliveryCode()).orElse(""), storeOrderPram.getUserPhone());

        PageParamRequest page = new PageParamRequest();
        page.setPage(1); page.setLimit(999);
        StoreOrderInfoSearchRequest storeOrderInfoPram = new StoreOrderInfoSearchRequest();
        storeOrderInfoPram.setOrderId(existOrder.getId());
        List<StoreOrderInfo> list = storeOrderInfoService.getList(storeOrderInfoPram, page);
        List<HashMap<String, Object>> cartInfos = new ArrayList<>();
        for (StoreOrderInfo storeInfo : list) {
            HashMap<String, Object> cartInfo = new HashMap<>();
            StoreCartResponse scr = JSONObject.parseObject(storeInfo.getInfo(), StoreCartResponse.class);
            cartInfo.put("cartNum", scr.getCartNum());
            cartInfo.put("truePrice", scr.getTruePrice());
            cartInfo.put("productInfo", scr.getProductInfo());
            cartInfos.add(cartInfo);
        }
        HashMap<String, Object> orderInfo = new HashMap<>();
        orderInfo.put("deliveryId", existOrder.getDeliveryId());
        orderInfo.put("deliveryName", existOrder.getDeliveryName());
        orderInfo.put("deliveryType", existOrder.getDeliveryType());
        orderInfo.put("cartInfo", cartInfos);

        resultMap.put("order", orderInfo);
        resultMap.put("express", expressInfo);
        return resultMap;
//        expressService.getExpressInfo();

    }

    /**
     *?????????????????????
     * @return
     */
    @Override
    public Object getReplyProduct(GetProductReply productReply) {
        HashMap<String,Object> resultMap = new HashMap<>();
//        StoreOrder storeOrderPram = new StoreOrder();
//        storeOrderPram.setUnique(unique);
//        StoreOrder existOrder = storeOrderService.getByEntityOne(storeOrderPram);
//        if(null== existOrder) throw new CrmebException("????????????????????????");
        StoreOrderInfoSearchRequest soinfoRequest = new StoreOrderInfoSearchRequest();
        soinfoRequest.setUnique(productReply.getUni());
        soinfoRequest.setOrderId(productReply.getOrderId());
        PageParamRequest pageRequest = new PageParamRequest();
        pageRequest.setLimit(999); pageRequest.setPage(1);
        List<StoreOrderInfo> orderInfos = storeOrderInfoService.getList(soinfoRequest, pageRequest);
        // ????????????????????????????????????
        for (StoreOrderInfo storeInfo : orderInfos) {
            HashMap<String, Object> cartInfo = new HashMap<>();
            StoreCartResponse scr = JSONObject.parseObject(storeInfo.getInfo(), StoreCartResponse.class);
            resultMap.put("cartNum", scr.getCartNum());
            cartInfo.put("truePrice", scr.getTruePrice());
            resultMap.put("productInfo", scr.getProductInfo());
//            resultMap.put("orderInfo", existOrder);
            resultMap.put("productId", scr.getProductInfo().getId());
        }
        return resultMap;
    }

    /**
     * ??????????????????
     * @param payType ????????????
     */
    @Override
    public boolean changePayType(String payType,String orderId) {
        StoreOrder storeOrderPram = new StoreOrder();
        storeOrderPram.setOrderId(orderId);
        StoreOrder existOrder = storeOrderService.getByEntityOne(storeOrderPram);
        if(null == existOrder) throw new CrmebException("?????????????????????");
        existOrder.setPayType(payType);
        return storeOrderService.updateById(existOrder);
    }

    ///////////////////////////////////////////////////////////////////// ???????????????

    /**
     * ????????????
     * ???????????????????????????????????? ????????????????????? ?????????????????????????????????
     * ORDEREEXIST, EXTENDORDER, PAYERROR, SUCCESS, WECHATPAY, PAYDEFICIENCY, WECHATH5PAY
     * @param request           ??????????????????
     * @param ip                ????????????IP
     * @param resultMap         ???????????????????????????
     * @param existStoreOrder   ????????????
     * @return                  ????????????
     */
    @Transactional(rollbackFor = {RuntimeException.class, Error.class, CrmebException.class})
    public boolean doPayOrder(OrderPayRequest request, String ip, HashMap<String, Object> resultMap, StoreOrder existStoreOrder) {
        existStoreOrder.setPayType(request.getPaytype());
        CreateOrderResponseVo orderPayResult = orderPayService.payOrder(existStoreOrder.getId(), request.getFrom(), ip);
        // ??????????????????????????????
        switch (existStoreOrder.getPayType()){
            case Constants.PAY_TYPE_WE_CHAT:
            case Constants.PAY_TYPE_WE_CHAT_FROM_PROGRAM:
                if(existStoreOrder.getPayType().equals(Constants.PAY_TYPE_WE_CHAT_FROM_H5)){
                    OrderPayItemResponse itemResponse = new OrderPayItemResponse(orderPayResult.getPrepayId(), existStoreOrder.getOrderId());
                    OrderPayResponse orderPayResponse = new OrderPayResponse("WECHAT_H5_PAY",itemResponse);
                    resultMap.put("result", orderPayResponse.getResult());
                    resultMap.put("status","WECHAT_H5_PAY");
                    resultMap.put("jsConfig", orderPayResult.getTransJsConfig());
                    return true;
                }else{
                    OrderPayItemResponse itemResponse = new OrderPayItemResponse(orderPayResult.getPrepayId(), existStoreOrder.getOrderId());
                    OrderPayResponse orderPayResponse = new OrderPayResponse("WECHAT_PAY",itemResponse);
                    resultMap.put("result", orderPayResponse.getResult());
                    resultMap.put("status","WECHAT_PAY");
                    resultMap.put("jsConfig", orderPayResult.getTransJsConfig());
                    return true;
                }
            case Constants.PAY_TYPE_YUE:
                OrderPayResponse orderPayResponseY = new OrderPayResponse("SUCCESS",
                        new OrderPayItemResponse(request.getUni(), existStoreOrder.getOrderId()));
                resultMap.put("result", orderPayResponseY.getResult());
                resultMap.put("status","SUCCESS");
                return true;
            case Constants.PAY_TYPE_OFFLINE:
                StoreOrder storeOrderOffLinePram = new StoreOrder();
                storeOrderOffLinePram.setOrderId(existStoreOrder.getOrderId());
                storeOrderOffLinePram.setPayType(Constants.PAY_TYPE_OFFLINE);
                boolean offlineResult = storeOrderService.updateByEntity(storeOrderOffLinePram);
                if(offlineResult){
                    resultMap.put("result", "??????????????????");
                    return true;
                }else{
                    throw new CrmebException("????????????");
                }
        }
        return false;
    }

}
