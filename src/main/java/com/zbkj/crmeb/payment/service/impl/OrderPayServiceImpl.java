package com.zbkj.crmeb.payment.service.impl;

import com.constants.Constants;
import com.exception.CrmebException;
import com.utils.DateUtil;
import com.zbkj.crmeb.front.response.UserRechargePaymentResponse;
import com.zbkj.crmeb.marketing.request.StoreCouponUserRequest;
import com.zbkj.crmeb.marketing.service.StoreCouponUserService;
import com.zbkj.crmeb.payment.service.OrderPayService;
import com.zbkj.crmeb.payment.service.PayService;
import com.zbkj.crmeb.payment.vo.wechat.AttachVo;
import com.zbkj.crmeb.payment.vo.wechat.CreateOrderResponseVo;
import com.zbkj.crmeb.payment.vo.wechat.PayParamsVo;
import com.zbkj.crmeb.payment.wechat.WeChatPayService;
import com.zbkj.crmeb.sms.service.SmsService;
import com.zbkj.crmeb.store.model.StoreOrder;
import com.zbkj.crmeb.store.model.StoreProductCoupon;
import com.zbkj.crmeb.store.service.StoreOrderInfoService;
import com.zbkj.crmeb.store.service.StoreOrderService;
import com.zbkj.crmeb.store.service.StoreOrderStatusService;
import com.zbkj.crmeb.store.service.StoreProductCouponService;
import com.zbkj.crmeb.store.utilService.OrderUtils;
import com.zbkj.crmeb.store.vo.StoreOrderInfoVo;
import com.zbkj.crmeb.user.model.User;
import com.zbkj.crmeb.user.model.UserBill;
import com.zbkj.crmeb.user.model.UserToken;
import com.zbkj.crmeb.user.service.UserBillService;
import com.zbkj.crmeb.user.service.UserService;
import com.zbkj.crmeb.wechat.service.TemplateMessageService;
import com.zbkj.crmeb.wechat.service.WeChatService;
import com.zbkj.crmeb.wechat.vo.WechatSendMessageForPaySuccess;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;


/**
 * OrderPayService ?????????
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Service
public class OrderPayServiceImpl extends PayService implements OrderPayService {
    private static final Logger logger = LoggerFactory.getLogger(OrderPayServiceImpl.class);

    @Autowired
    private StoreOrderService storeOrderService;

    @Autowired
    private StoreOrderStatusService storeOrderStatusService;

    @Autowired
    private StoreOrderInfoService storeOrderInfoService;

    @Lazy
    @Autowired
    private WeChatPayService weChatPayService;

    @Autowired
    private TemplateMessageService templateMessageService;

    @Autowired
    private UserBillService userBillService;

    @Lazy
    @Autowired
    private SmsService smsService;

    @Autowired
    private UserService userService;

    @Autowired
    private StoreProductCouponService storeProductCouponService;

    @Autowired
    private StoreCouponUserService storeCouponUserService;

    @Autowired
    private WeChatService weChatService;

    @Autowired
    private OrderUtils orderUtils;

    //?????????
    private StoreOrder order;

    //??????????????????
    private PayParamsVo payParamsVo;

    @Autowired
    private TransactionTemplate transactionTemplate;


    /**
     * ????????????
     * @param orderId Integer ?????????
     * @param from String ???????????????
     * @return PayResponseVo
     */
    @Override
    public CreateOrderResponseVo payOrder(Integer orderId, String from, String clientIp) {
        CreateOrderResponseVo responseVo = new CreateOrderResponseVo();
        StoreOrder storeOrder = storeOrderService.getById(orderId);
        setOrder(storeOrder);
        //??????order????????????, ?????????????????????
        beforePay();
        try{
            switch (storeOrder.getPayType()){
                case Constants.PAY_TYPE_WE_CHAT: //????????????
                case Constants.PAY_TYPE_WE_CHAT_FROM_PROGRAM:
                    PayParamsVo payParamsVoRouter = getPayParamsVo(from, clientIp, storeOrder);
                    responseVo = weChatPayService.create(payParamsVoRouter);//???????????????
                    UserRechargePaymentResponse response = weChatService.response(responseVo);
                    responseVo.setTransJsConfig(response);
                    break;
                case Constants.PAY_TYPE_ALI_PAY: //?????????
                    throw new CrmebException("????????????????????????");
                case Constants.PAY_TYPE_OFFLINE: //????????????
                    throw new CrmebException("?????????????????????");
                case Constants.PAY_TYPE_YUE: //???????????? ????????????????????????CreateOrderResponseVo.ResultCode = 1;
//                    boolean yuePay = storeOrderService.yuePay(storeOrder, userService.getInfo(),"");
                    boolean yuePay = storeOrderService.yuePay(storeOrder, userService.getInfo(),"");
                    responseVo = responseVo.setResultCode(yuePay + "");
                    break;
            }
            // ???????????????????????????

        }catch (Exception e){
            e.printStackTrace();
            throw new CrmebException("?????????????????????");
        }
        return responseVo;
    }

    /**
     *  ????????????????????????
     * @param fromType
     * @param clientIp
     * @param storeOrder
     * @return
     */
    private PayParamsVo getPayParamsVo(String fromType, String clientIp, StoreOrder storeOrder) {
        //?????????????????????
        return new PayParamsVo(
                storeOrder.getOrderId(),
                fromType,
                clientIp,
                getProductName(),
                storeOrder.getPayPrice(),
                storeOrder.getUid(),
                new AttachVo(Constants.SERVICE_PAY_TYPE_ORDER, storeOrder.getUid())
        );
    }


    /**
     * ????????????
     */
    private void beforePay() {
        checkOrderUnPay();
    }


    /**
     * ????????????
     * @param orderId String ?????????
     * @param userId Integer ??????id
     * @param payType String ????????????
     */
    @Transactional(rollbackFor = {RuntimeException.class, Error.class, CrmebException.class})
    @Override
    public boolean success(String orderId, Integer userId, String payType) {
        try{
            StoreOrder storeOrder = new StoreOrder();
            storeOrder.setOrderId(orderId);
            storeOrder.setUid(userId);

            storeOrder = storeOrderService.getInfoByEntity(storeOrder);
            setOrder(storeOrder);
            checkOrderUnPay();

            afterPaySuccess();
            return true;
        }catch (Exception e){
            throw new CrmebException("???????????????????????????" + e.getMessage());
        }
    }

    /**
     * ??????????????????, ??????????????????
     */
    @Override
    public void afterPaySuccess() {
        //??????????????????
        orderUpdate();

        //????????????
        orderStatusCreate();

        //????????????
        userBillCreate();

        //??????????????????
        pushTempMessage();

        // ???????????????????????????????????????
//        autoSendCoupons();

        // ????????????????????????
        updateUserPayCount();

        //?????????????????????
        updateFounds();
    }

    /**
     * ??????????????????
     * @param storeOrder ??????
     * @param user  ??????
     * @param userToken ??????Token
     */
    @Override
    public Boolean paySuccess(StoreOrder storeOrder, User user, UserToken userToken) {
        storeOrder.setPaid(true);
        storeOrder.setPayTime(DateUtil.nowDateTime());

        if (storeOrder.getPayType().equals(Constants.PAY_TYPE_YUE)) {
            user.setNowMoney(user.getNowMoney().subtract(storeOrder.getPayPrice()));

        }

        UserBill userBill = userBillInit(storeOrder, user);

        Boolean execute = transactionTemplate.execute(e -> {
            //??????????????????
            storeOrderService.updateById(storeOrder);

            //????????????
            storeOrderStatusService.addLog(storeOrder.getId(), Constants.ORDER_LOG_PAY_SUCCESS, Constants.ORDER_LOG_MESSAGE_PAY_SUCCESS);

            // ????????????????????????????????????,??????????????????????????????
            if (storeOrder.getPayType().equals(Constants.PAY_TYPE_YUE)) {
                userService.updateNowMoney(user.getUid(), user.getNowMoney());
                UserBill userBillYueInit = userBillYueInit(storeOrder, user);
                userBillService.save(userBillYueInit);
            }

            //????????????
            userBillService.save(userBill);

            // ????????????????????????
            userService.userPayCountPlus(user);

            //?????????????????????
            userService.consumeAfterUpdateUserFounds(storeOrder.getUid(), storeOrder.getPayPrice(), Constants.USER_BILL_TYPE_PAY_ORDER);
            return Boolean.TRUE;
        });

        if (execute) {
            try {
                //??????????????????
                pushTempMessageOrder(storeOrder);

                // ???????????????????????????????????????
                autoSendCoupons(storeOrder);
            } catch (Exception e) {
                e.printStackTrace();
                logger.error("??????????????????????????????");
            }
        }
        return execute;
    }

    private UserBill userBillYueInit(StoreOrder storeOrder, User user) {
        UserBill userBill = new UserBill();
        userBill.setTitle("????????????");
        userBill.setUid(user.getUid());
        userBill.setCategory(Constants.USER_BILL_CATEGORY_MONEY);
        userBill.setType(Constants.USER_BILL_TYPE_PAY_PRODUCT);
        userBill.setNumber(storeOrder.getPayPrice());
        userBill.setLinkId(storeOrder.getId()+"");
        userBill.setBalance(user.getNowMoney());
        userBill.setMark("????????????" + storeOrder.getPayPrice() + "???????????????");
        return userBill;
    }

    private UserBill userBillInit(StoreOrder order, User user) {
        UserBill userBill = new UserBill();
        userBill.setPm(0);
        userBill.setUid(order.getUid());
        userBill.setLinkId(order.getId().toString());
        userBill.setTitle("????????????");
        userBill.setCategory(Constants.USER_BILL_CATEGORY_MONEY);
        userBill.setType(Constants.USER_BILL_TYPE_PAY_MONEY);
        userBill.setNumber(order.getPayPrice());
        userBill.setBalance(user.getNowMoney());
        userBill.setMark("??????" + order.getPayPrice() + "???????????????");
        return userBill;
    }

    /**
     * ????????????????????????
     */
    private void pushTempMessageOrder(StoreOrder storeOrder) {
        // ???????????????????????????
        String storeNameAndCarNumString = orderUtils.getStoreNameAndCarNumString(storeOrder.getId());
        if(StringUtils.isNotBlank(storeNameAndCarNumString)){
            WechatSendMessageForPaySuccess paySuccess = new WechatSendMessageForPaySuccess(
                    storeOrder.getId()+"",storeOrder.getPayPrice()+"",storeOrder.getPayTime()+"","??????",
                    storeOrder.getTotalPrice()+"",storeNameAndCarNumString);
            orderUtils.sendWeiChatMiniMessageForPaySuccess(paySuccess, userService.getById(storeOrder).getUid());
        }
    }

    /**
     * ????????????????????????
     */
    private void updateFounds() {
        userService.consumeAfterUpdateUserFounds(getOrder().getUid(), getOrder().getPayPrice(), Constants.USER_BILL_TYPE_PAY_ORDER);
    }

    /**
     * ????????????????????????
     */
    private void updateUserPayCount() {
        userService.userPayCountPlus(userService.getInfo());
    }

    /**
     * ?????????????????????????????????
     */
    private void autoSendCoupons(StoreOrder storeOrder){
        // ????????????????????????????????????
        List<StoreOrderInfoVo> orders = storeOrderInfoService.getOrderListByOrderId(storeOrder.getId());
        if(null == orders){
            return;
        }
        for (StoreOrderInfoVo order : orders) {
            List<StoreProductCoupon> couponsForGiveUser = storeProductCouponService.getListByProductId(order.getProductId());
            User currentUser = userService.getById(storeOrder.getUid());
            for (StoreProductCoupon storeProductCoupon : couponsForGiveUser) {
                StoreCouponUserRequest crp = new StoreCouponUserRequest();
                crp.setUid(currentUser.getUid()+"");
                crp.setCouponId(storeProductCoupon.getIssueCouponId());
                storeCouponUserService.receive(crp);
            }
        }
    }

    private void userBillCreate() {
        UserBill userBill = new UserBill();
        userBill.setPm(0);
        userBill.setUid(getOrder().getUid());
        userBill.setLinkId(getOrder().getId().toString());
        userBill.setTitle("????????????");
        userBill.setCategory(Constants.USER_BILL_CATEGORY_MONEY);
        userBill.setType(Constants.USER_BILL_TYPE_PAY_MONEY);
        userBill.setNumber(getOrder().getPayPrice());
        userBill.setBalance(userService.getById(getOrder().getUid()).getNowMoney());
        userBill.setMark("??????" + getOrder().getPayPrice() + "???????????????");
        userBillService.save(userBill);
    }

    /**
     * ????????????
     */
    private void orderStatusCreate() {
        storeOrderStatusService.createLog(getOrder().getId(), Constants.ORDER_LOG_PAY_SUCCESS, Constants.ORDER_LOG_MESSAGE_PAY_SUCCESS);
    }

    /**
     * ??????????????????
     */
    private void orderUpdate() {
        //??????????????????
        getOrder().setPaid(true);
        getOrder().setPayTime(DateUtil.nowDateTime());
        storeOrderService.updateById(getOrder());
    }


    /**
     * ????????????????????????
     */
    private void pushTempMessage() {
        // ???????????????????????????
        String storeNameAndCarNumString = orderUtils.getStoreNameAndCarNumString(getOrder().getId());
        if(StringUtils.isNotBlank(storeNameAndCarNumString)){
            WechatSendMessageForPaySuccess paySuccess = new WechatSendMessageForPaySuccess(
                    getOrder().getId()+"",getOrder().getPayPrice()+"",getOrder().getPayTime()+"","??????",
                    getOrder().getTotalPrice()+"",storeNameAndCarNumString);
            orderUtils.sendWeiChatMiniMessageForPaySuccess(paySuccess, userService.getById(getOrder()).getUid());
        }
    }


    /**
     * ?????????????????????
     */
    private void checkOrderUnPay() {
        if(null == getOrder()){
            throw new CrmebException("????????????????????????");
        }

        if(getOrder().getPaid()){
            throw new CrmebException("?????????????????????????????????????????????");
        }
    }

    /**
     * ????????????????????????
     * @return List<StoreOrderInfoVo>
     */
    private List<StoreOrderInfoVo> getStoreOrderInfoList(){
        //????????????
        return storeOrderInfoService.getOrderListByOrderId(getOrder().getId());
    }

    /**
     * ????????????????????????
     * @return String
     */
    private String getProductName(){

        List<StoreOrderInfoVo> orderList = getStoreOrderInfoList();
        if(orderList.size() < 1){
            throw new CrmebException("????????????????????????????????????");
        }
//        return orderList.get(0).getInfo().getJSONObject("productInfo").getString("store_name");
        return orderList.get(0).getInfo().getProductInfo().getStoreName();
    }
}
