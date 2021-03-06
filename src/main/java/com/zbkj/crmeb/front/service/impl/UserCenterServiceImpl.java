package com.zbkj.crmeb.front.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.common.CommonPage;
import com.common.PageParamRequest;
import com.constants.Constants;
import com.exception.CrmebException;
import com.github.pagehelper.PageInfo;
import com.utils.CrmebUtil;
import com.utils.DateUtil;
import com.zbkj.crmeb.finance.model.UserExtract;
import com.zbkj.crmeb.finance.model.UserRecharge;
import com.zbkj.crmeb.finance.request.FundsMonitorSearchRequest;
import com.zbkj.crmeb.finance.request.UserExtractRequest;
import com.zbkj.crmeb.finance.service.UserExtractService;
import com.zbkj.crmeb.finance.service.UserRechargeService;
import com.zbkj.crmeb.front.request.UserRechargeRequest;
import com.zbkj.crmeb.front.request.UserSpreadPeopleRequest;
import com.zbkj.crmeb.front.response.*;
import com.zbkj.crmeb.front.service.UserCenterService;
import com.zbkj.crmeb.payment.service.RechargePayService;
import com.zbkj.crmeb.payment.vo.wechat.CreateOrderResponseVo;
import com.zbkj.crmeb.store.model.StoreOrder;
import com.zbkj.crmeb.store.service.StoreOrderService;
import com.zbkj.crmeb.system.model.SystemUserLevel;
import com.zbkj.crmeb.system.request.SystemUserLevelSearchRequest;
import com.zbkj.crmeb.system.service.SystemConfigService;
import com.zbkj.crmeb.system.service.SystemGroupDataService;
import com.zbkj.crmeb.system.service.SystemUserLevelService;
import com.zbkj.crmeb.system.vo.SystemGroupDataRechargeConfigVo;
import com.zbkj.crmeb.user.dao.UserDao;
import com.zbkj.crmeb.user.model.User;
import com.zbkj.crmeb.user.model.UserBill;
import com.zbkj.crmeb.user.model.UserToken;
import com.zbkj.crmeb.user.request.RegisterThirdUserRequest;
import com.zbkj.crmeb.user.request.UserOperateFundsRequest;
import com.zbkj.crmeb.user.service.UserBillService;
import com.zbkj.crmeb.user.service.UserService;
import com.zbkj.crmeb.user.service.UserTokenService;
import com.zbkj.crmeb.wechat.response.WeChatAuthorizeLoginGetOpenIdResponse;
import com.zbkj.crmeb.wechat.response.WeChatAuthorizeLoginUserInfoResponse;
import com.zbkj.crmeb.wechat.response.WeChatProgramAuthorizeLoginGetOpenIdResponse;
import com.zbkj.crmeb.wechat.service.WeChatService;
import com.zbkj.crmeb.wechat.service.impl.WechatSendMessageForMinService;
import com.zbkj.crmeb.wechat.vo.WechatSendMessageForTopped;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * ???????????? ???????????????
 */
@Service
public class UserCenterServiceImpl extends ServiceImpl<UserDao, User> implements UserCenterService {

    @Autowired
    private UserService userService;

    @Autowired
    private UserBillService userBillService;

    @Autowired
    private UserExtractService userExtractService;

    @Autowired
    private SystemConfigService systemConfigService;

    @Autowired
    private SystemUserLevelService systemUserLevelService;

    @Autowired
    private SystemGroupDataService systemGroupDataService;

    @Autowired
    private StoreOrderService storeOrderService;

    @Autowired
    private UserRechargeService userRechargeService;

    @Autowired
    private RechargePayService rechargePayService;

    @Autowired
    private UserTokenService userTokenService;

    @Autowired
    private WeChatService weChatService;

    @Autowired
    private WechatSendMessageForMinService wechatSendMessageForMinService;



    /**
     * ??????????????????(??????????????? ?????????????????? ????????????)
     */
    @Override
    public UserCommissionResponse getCommission() {
        UserCommissionResponse userCommissionResponse = new UserCommissionResponse();
        //???????????????
        userCommissionResponse.setLastDayCount(userBillService.getSumBigDecimal(0, userService.getUserIdException(), Constants.USER_BILL_CATEGORY_BROKERAGE_PRICE, Constants.SEARCH_DATE_YESTERDAY, null));

//        userCommissionResponse.setExtractCount(userBillService.getSumBigDecimal(0, userService.getUserIdException(), Constants.USER_BILL_CATEGORY_BROKERAGE_PRICE, null, Constants.USER_BILL_TYPE_EXTRACT));
        //?????????????????????
        userCommissionResponse.setExtractCount(userExtractService.getExtractTotalMoney(userService.getUserIdException()));

        userCommissionResponse.setCommissionCount(userService.getInfo().getBrokeragePrice());
        return userCommissionResponse;
    }

    /**
     * ??????????????????
     * @return UserSpreadCommissionResponse
     */
    @Override
    public PageInfo<UserSpreadCommissionResponse> getSpreadCommissionByType(int type, PageParamRequest pageParamRequest) {
        String category = Constants.USER_BILL_CATEGORY_MONEY;
        List<String> typeList = CollUtil.newArrayList();
        switch (type){
            case 0:
                typeList.add(Constants.USER_BILL_TYPE_RECHARGE);
                typeList.add(Constants.USER_BILL_TYPE_PAY_MONEY);
                typeList.add(Constants.USER_BILL_TYPE_SYSTEM_ADD);
                typeList.add(Constants.USER_BILL_TYPE_PAY_PRODUCT_REFUND);
                typeList.add(Constants.USER_BILL_TYPE_SYSTEM_SUB);
                typeList.add(Constants.USER_BILL_TYPE_PAY_MEMBER);
                typeList.add(Constants.USER_BILL_TYPE_OFFLINE_SCAN);
                break;
            case 1:
                typeList.add(Constants.USER_BILL_TYPE_PAY_MONEY);
                typeList.add(Constants.USER_BILL_TYPE_PAY_MEMBER);
                typeList.add(Constants.USER_BILL_TYPE_OFFLINE_SCAN);
                typeList.add(Constants.USER_BILL_TYPE_USER_RECHARGE_REFUND);
                break;
            case 2:
                typeList.add(Constants.USER_BILL_TYPE_RECHARGE);
                typeList.add(Constants.USER_BILL_TYPE_SYSTEM_ADD);
                break;
            case 3:
                category = Constants.USER_BILL_CATEGORY_BROKERAGE_PRICE;
                typeList.add(Constants.USER_BILL_TYPE_BROKERAGE);
                break;
            case 4:
                typeList.add(Constants.USER_BILL_TYPE_EXTRACT);
                break;
            default:
                break;

        }
        return userBillService.getListGroupByMonth(userService.getUserIdException(), typeList, pageParamRequest, category);
    }

    /**
     * ????????????/????????????
     * @return BigDecimal
     */
    @Override
    public BigDecimal getSpreadCountByType(int type) {
        //????????????/????????????
        Integer userId = userService.getUserIdException();
        if(type == 3){
            BigDecimal count = userBillService.getSumBigDecimal(null, userId, Constants.USER_BILL_CATEGORY_MONEY, null, Constants.USER_BILL_TYPE_BROKERAGE);
            BigDecimal withdraw = userBillService.getSumBigDecimal(1, userId, Constants.USER_BILL_CATEGORY_MONEY, null, Constants.USER_BILL_TYPE_BROKERAGE); //??????
            return count.subtract(withdraw);
        }

        //????????????
        if(type == 4){
            return userExtractService.getWithdrawn(null,null);
        }

        return BigDecimal.ZERO;
    }

    /**
     * ????????????
     * @return Boolean
     */
    @Override
    public Boolean extractCash(UserExtractRequest request) {
        return userExtractService.create(request, userService.getUserIdException());
    }

    /**
     * ????????????/??????????????????
     * @return UserExtractCashResponse
     */
    @Override
    public UserExtractCashResponse minExtractCash() {
        String bank = systemConfigService.getValueByKeyException(Constants.CONFIG_BANK_LIST).replace("\r\n", "\n");
        String minPrice = systemConfigService.getValueByKeyException(Constants.CONFIG_EXTRACT_MIN_PRICE);

        String extractTime = systemConfigService.getValueByKey(Constants.CONFIG_EXTRACT_FREEZING_TIME);

        BigDecimal brokeragePrice = userService.getInfo().getBrokeragePrice();
        BigDecimal freeze = userExtractService.getFreeze(userService.getUserIdException());
        List<String> bankArr = new ArrayList<>();
        if(bank.indexOf("\n") > 0){
            for (String s : bank.split("\n")) {
                bankArr.add(s);
            }
        }else{
            bankArr.add(bank);
        }
        return new UserExtractCashResponse(
                bankArr,
                minPrice,
                brokeragePrice.subtract(freeze),
                freeze,
                extractTime
        );
    }

    /**
     * ??????????????????
     * @return List<UserLevel>
     */
    @Override
    public List<SystemUserLevel> getUserLevelList() {
        SystemUserLevelSearchRequest request = new SystemUserLevelSearchRequest();
        request.setIsShow(true);
        request.setIsDel(false);
        return systemUserLevelService.getList(request, new PageParamRequest());
    }

    /**
     * ??????????????? ??????????????????????????????
     * @return UserSpreadPeopleResponse
     */
    @Override
    public UserSpreadPeopleResponse getSpreadPeopleList(UserSpreadPeopleRequest request, PageParamRequest pageParamRequest) {
        //??????????????????????????????????????????
        UserSpreadPeopleResponse userSpreadPeopleResponse = new UserSpreadPeopleResponse();
        List<Integer> userIdList = new ArrayList<>();
        userIdList.add(userService.getUserIdException());
        userIdList = userService.getSpreadPeopleIdList(userIdList); //????????????????????????id??????

        if (CollUtil.isEmpty(userIdList)) {//??????????????????????????????????????????
            userSpreadPeopleResponse.setCount(0);
            userSpreadPeopleResponse.setTotal(0);
            userSpreadPeopleResponse.setTotalLevel(0);
            return userSpreadPeopleResponse;
        }

        userSpreadPeopleResponse.setTotal(userIdList.size()); //???????????????
        //?????????????????????
        List<Integer> secondSpreadIdList = CollUtil.newArrayList();
        if (CollUtil.isNotEmpty(userIdList)) {
            secondSpreadIdList = userService.getSpreadPeopleIdList(userIdList);
        }
        userSpreadPeopleResponse.setTotalLevel(secondSpreadIdList.size());
        userSpreadPeopleResponse.setCount(userIdList.size() + secondSpreadIdList.size());

        if(request.getGrade() == 1){
            //???????????????
//            userIdList.addAll(userService.getSpreadPeopleIdList(userIdList));
//            userSpreadPeopleResponse.setTotalLevel(userIdList.size()); //?????????????????????id?????????????????????????????????
            userIdList.clear();
            userIdList.addAll(secondSpreadIdList);
        }

        if(userIdList.size() > 0){
            List<UserSpreadPeopleItemResponse> userSpreadPeopleItemResponseList = userService.getSpreadPeopleList(userIdList, request.getKeyword(), request.getSortKey(), request.getIsAsc(), pageParamRequest);
            userSpreadPeopleResponse.setSpreadPeopleList(userSpreadPeopleItemResponseList);
        }

        return userSpreadPeopleResponse;
    }

    /**
     * ????????????
     * @return List<UserBill>
     */
    @Override
    public List<UserBill> getUserBillList(String category, PageParamRequest pageParamRequest) {
        FundsMonitorSearchRequest request = new FundsMonitorSearchRequest();
        request.setUid(userService.getUserIdException());
        request.setCategory(category);
        return userBillService.getList(request, pageParamRequest);
    }

    /**
     * ??????????????????
     * @return UserRechargeResponse
     */
    @Override
    public UserRechargeResponse getRechargeConfig() {
        UserRechargeResponse userRechargeResponse = new UserRechargeResponse();
        userRechargeResponse.setRechargeQuota(systemGroupDataService.getListByGid(Constants.GROUP_DATA_ID_RECHARGE_LIST, UserRechargeItemResponse.class));
        String rechargeAttention = systemConfigService.getValueByKey(Constants.CONFIG_RECHARGE_ATTENTION);
        List<String> rechargeAttentionList = new ArrayList<>();
        if(StringUtils.isNotBlank(rechargeAttention)){
            rechargeAttentionList = CrmebUtil.stringToArrayStrRegex(rechargeAttention, "\n");
        }
        userRechargeResponse.setRechargeAttention(rechargeAttentionList);
        return userRechargeResponse;
    }

    /**
     * ??????????????????
     * @return UserBalanceResponse
     */
    @Override
    public UserBalanceResponse getUserBalance() {
        User info = userService.getInfo();
        BigDecimal recharge = userBillService.getSumBigDecimal(1, info.getUid(), Constants.USER_BILL_CATEGORY_MONEY, null, null);
        BigDecimal orderStatusSum = storeOrderService.getSumBigDecimal(info.getUid(), null);
        return new UserBalanceResponse(info.getNowMoney(), recharge, orderStatusSum);
    }

    /**
     * ????????????
     * @return UserSpreadOrderResponse;
     */
    @Override
    public UserSpreadOrderResponse getSpreadOrder(PageParamRequest pageParamRequest) {
        UserSpreadOrderResponse userSpreadOrderResponse = new UserSpreadOrderResponse();
        Integer userId = userService.getUserIdException();
        List<Integer> userIdList = new ArrayList<>();
        userIdList.add(userId);
        userIdList = userService.getSpreadPeopleIdList(userIdList); //??????????????????id
        if(null == userIdList){
            return userSpreadOrderResponse;
        }
        //???????????????????????????????????????????????????????????????

        FundsMonitorSearchRequest request = new FundsMonitorSearchRequest();
//        request.setCategory(Constants.USER_BILL_CATEGORY_MONEY);
//        request.setType(Constants.USER_BILL_TYPE_PAY_MONEY);
        request.setCategory(Constants.USER_BILL_CATEGORY_BROKERAGE_PRICE);
        request.setType(Constants.USER_BILL_TYPE_BROKERAGE);
        request.setUserIdList(CollUtil.newArrayList(userId));
        request.setLinkId("gt");
        request.setPm(1);
        List<UserBill> list = userBillService.getList(request, pageParamRequest);
        if(null == list){
            return userSpreadOrderResponse;
        }
        CommonPage<UserBill> userBillCommonPage = CommonPage.restPage(list); //??????????????????
        userSpreadOrderResponse.setCount(userBillCommonPage.getTotal()); //??????

        //????????????id, ??????????????????
        List<Integer> orderIdList = list.stream().map(i -> Integer.parseInt(i.getLinkId())).distinct().collect(Collectors.toList());
        Map<Integer, StoreOrder> orderList = storeOrderService.getMapInId(orderIdList);

        //????????????
//        userIdList = list.stream().map(UserBill::getUid).distinct().collect(Collectors.toList());
        List<StoreOrder> storeOrderList = new ArrayList<>(orderList.values());
        userIdList = storeOrderList.stream().map(StoreOrder::getUid).distinct().collect(Collectors.toList());
        HashMap<Integer, User> userList = userService.getMapListInUid(userIdList);

        //?????????????????????
        List<UserSpreadOrderItemResponse> userSpreadOrderItemResponseList = new ArrayList<>();
        for (UserBill userBill : list) {
            String date = DateUtil.dateToStr(userBill.getCreateTime(), Constants.DATE_FORMAT_MONTH);
            boolean isAdd = false;
            String orderId = "";
            Integer linkId = Integer.parseInt(userBill.getLinkId());
            if(null != orderList && orderList.containsKey(linkId)){
                orderId = orderList.get(linkId).getOrderId();
            }

            UserSpreadOrderItemChildResponse userSpreadOrderItemChildResponse = new UserSpreadOrderItemChildResponse(
                    orderId, //?????????
                    userBill.getCreateTime(),
                    (userBill.getStatus() == 1) ? userBill.getNumber() : BigDecimal.ZERO,
//                    userList.get(userBill.getUid()).getAvatar(),
//                    userList.get(userBill.getUid()).getNickname(),
                    userList.get(orderList.get(linkId).getUid()).getAvatar(),
                    userList.get(orderList.get(linkId).getUid()).getNickname(),
                    userBill.getType()
            );

            //????????????????????????????????????????????????????????????
            for (UserSpreadOrderItemResponse userSpreadOrderItemResponse: userSpreadOrderItemResponseList) {
                if(userSpreadOrderItemResponse.getTime().equals(date)){
                    userSpreadOrderItemResponse.getChild().add(userSpreadOrderItemChildResponse);
                    isAdd = true;
                    break;
                }
            }

            //?????????????????????????????????
            if(!isAdd){
                //????????????
                UserSpreadOrderItemResponse userSpreadOrderItemResponse = new UserSpreadOrderItemResponse();
                userSpreadOrderItemResponse.setTime(date);
                userSpreadOrderItemResponse.getChild().add(userSpreadOrderItemChildResponse);
                userSpreadOrderItemResponseList.add(userSpreadOrderItemResponse);
            }
        }

        List<String> monthList = userSpreadOrderItemResponseList.stream().map(s -> "'" + s.getTime() + "'").collect(Collectors.toList());

        if(monthList.size() < 1){
            return userSpreadOrderResponse;
        }

        //???????????????????????????
        Map<String, Integer> countMap = userBillService.getCountListByMonth(request, monthList);

        //???????????????????????????
        for (UserSpreadOrderItemResponse userSpreadOrderItemResponse: userSpreadOrderItemResponseList) {
            userSpreadOrderItemResponse.setCount(countMap.get(userSpreadOrderItemResponse.getTime()));
        }

        userSpreadOrderResponse.setList(userSpreadOrderItemResponseList);

        return userSpreadOrderResponse;
    }

    /**
     * ??????
     * @return UserSpreadOrderResponse;
     */
    @Override
    @Transactional(rollbackFor = {RuntimeException.class, Error.class, CrmebException.class})
    public UserRechargePaymentResponse recharge(UserRechargeRequest request) {
        request.setPayType(Constants.PAY_TYPE_WE_CHAT);

        //?????????????????????????????????
        String rechargeMinAmountStr = systemConfigService.getValueByKeyException(Constants.CONFIG_KEY_RECHARGE_MIN_AMOUNT);
        BigDecimal rechargeMinAmount = new BigDecimal(rechargeMinAmountStr);
        int compareResult = rechargeMinAmount.compareTo(request.getPrice());
        if(compareResult > 0){
            throw new CrmebException("????????????????????????" + rechargeMinAmountStr);
        }
        request.setPrice(request.getPrice());
        request.setGivePrice(BigDecimal.ZERO);

        if(request.getGroupDataId() > 0){
            SystemGroupDataRechargeConfigVo systemGroupData = systemGroupDataService.getNormalInfo(request.getGroupDataId(), SystemGroupDataRechargeConfigVo.class);
            if(null == systemGroupData){
                throw new CrmebException("?????????????????????????????????");
            }

            //???????????????
            request.setPrice(systemGroupData.getPrice());
            request.setGivePrice(systemGroupData.getGiveMoney());

        }
        User currentUser = userService.getInfoException();
        //??????????????????
        request.setUserId(currentUser.getUid());

        UserRecharge userRecharge = userRechargeService.create(request);

        //????????????
        try{
            CreateOrderResponseVo responseVo = rechargePayService.payOrder(userRecharge.getId(), request.getPayType(), request.getClientIp());
            if(null == responseVo){
                throw new CrmebException("???????????????");
            }
            // ????????????????????? ??????????????????
            WechatSendMessageForTopped topped = new WechatSendMessageForTopped(
                    userRecharge.getOrderId(),userRecharge.getOrderId(),userRecharge.getPrice()+"",
                    currentUser.getNowMoney()+"",userRecharge.getCreateTime()+"", userRecharge.getGivePrice()+"",
                    "??????",userRecharge.getPrice()+"","CRMEB","??????"
            );
            wechatSendMessageForMinService.sendToppedMessage(topped, currentUser.getUid());

            return weChatService.response(responseVo);
        }catch (Exception e){
            throw new CrmebException(e.getMessage());
        }
    }

    /**
     * ????????????
     * @return UserSpreadOrderResponse;
     */
    @Override
    @Transactional(rollbackFor = {RuntimeException.class, Error.class, CrmebException.class})
    public LoginResponse weChatAuthorizeLogin(String code, Integer spreadUid) {
        try{
            System.out.println("code = " + code);
            WeChatAuthorizeLoginGetOpenIdResponse response = weChatService.authorizeLogin(code);
            System.out.println("response = " + response);
            User user = publicLogin(response.getOpenId(), response.getAccessToken());
            System.out.println("user = " + user);
            //????????????id????????????token??????
            LoginResponse loginResponse = new LoginResponse();
            loginResponse.setToken(userService.token(user));
            user.setPwd(null);
            //??????????????????
            userService.bindSpread(user, spreadUid);
            loginResponse.setUser(user);

            return loginResponse;
        }catch (Exception e){
            throw new CrmebException(e.getMessage());
        }
    }

    /**
     * ??????openId??????
     * @param openId  String ?????????????????????code?????????openId??????
     * @param token String code?????????token
     * @return List<LoginResponse>
     */
    private User publicLogin(String openId, String token) {
        try {
            //??????????????????
            System.out.println("openId = " + openId);
            System.out.println("token = " + token);
            UserToken userToken = userTokenService.checkToken(openId,  Constants.THIRD_LOGIN_TOKEN_TYPE_PUBLIC);
            System.out.println("userToken = " + userToken);
            if(null != userToken){
                return userService.getById(userToken.getUid());
            }

            //??????????????? ??????????????????????????? ?????????????????????????????????????????????
            WeChatAuthorizeLoginUserInfoResponse userInfo = weChatService.getUserInfo(openId, token);
            System.out.println("userInfo = " + userInfo);
            RegisterThirdUserRequest registerThirdUserRequest = new RegisterThirdUserRequest();
            BeanUtils.copyProperties(userInfo, registerThirdUserRequest);
            System.out.println("registerThirdUserRequest = " + registerThirdUserRequest);
            String unionId = userInfo.getUnionId();

            //???unionid??????????????????
            if(StringUtils.isNotBlank(unionId)){
                userToken = userTokenService.checkToken(userInfo.getUnionId(), Constants.THIRD_LOGIN_TOKEN_TYPE_UNION_ID);
                if(null != userToken){
                    return userService.getById(userToken.getUid());
                }
            }

            //TODO ?????????????????????????????????1 ???????????????2 ???????????????????????????????????????????????????????????????
            User user = userService.registerByThird(registerThirdUserRequest, Constants.USER_LOGIN_TYPE_PUBLIC);

            userTokenService.bind(openId, Constants.THIRD_LOGIN_TOKEN_TYPE_PUBLIC, user.getUid());
            if(StringUtils.isNotBlank(unionId)) {
                //????????????
                userTokenService.bind(unionId, Constants.THIRD_LOGIN_TOKEN_TYPE_UNION_ID, user.getUid());
            }
            return user;
        }catch (Exception e){
            throw new CrmebException(e.getMessage());
        }
    }

    /**
     * ??????????????????logo
     * @return String;
     */
    @Override
    public String getLogo() {
        String url = systemConfigService.getValueByKey(Constants.CONFIG_KEY_SITE_LOGO);
//        if(StringUtils.isNotBlank(url) && !url.contains("http")){
//            url = systemConfigService.getValueByKey(Constants.CONFIG_KEY_SITE_URL) + url;
//            url = url.replace("\\", "/");
//        }
        return url;
    }

    /**
     * ???????????????
     * @param code String ??????????????????code
     * @param request RegisterThirdUserRequest ????????????
     * @return UserSpreadOrderResponse;
     */
    @Override
    public LoginResponse weChatAuthorizeProgramLogin(String code, RegisterThirdUserRequest request) {
        try{
            WeChatProgramAuthorizeLoginGetOpenIdResponse response = weChatService.programAuthorizeLogin(code);
            System.out.println("????????????????????? = " + JSON.toJSONString(response));
            User user = programLogin(response.getOpenId(), response.getUnionId(), request);
            //????????????id????????????token??????
            LoginResponse loginResponse = new LoginResponse();
            loginResponse.setToken(userService.token(user));
            user.setPwd(null);
            //??????????????????
            userService.bindSpread(user, request.getSpreadPid());
            loginResponse.setUser(user);

            return loginResponse;
        }catch (Exception e){
            throw new CrmebException(e.getMessage());
        }
    }

    /**
     * ??????????????????
     * @param type  String ????????????
     * @param pageParamRequest PageParamRequest ??????
     * @return List<LoginResponse>
     */
    @Override
    public List<User> getTopSpreadPeopleListByDate(String type, PageParamRequest pageParamRequest) {
        return userService.getTopSpreadPeopleListByDate(type, pageParamRequest);
    }

    /**
     * ???????????????
     * @param type  String ????????????
     * @param pageParamRequest PageParamRequest ??????
     * @return List<LoginResponse>
     */
    @Override
    public List<User> getTopBrokerageListByDate(String type, PageParamRequest pageParamRequest) {
        List<UserBill> userBillVoList = userBillService.getTopBrokerageListByDate(type, pageParamRequest);
        if(userBillVoList.size() < 1){
            return null;
        }

        List<Integer> uidList = userBillVoList.stream().map(UserBill::getUid).collect(Collectors.toList());
        if(uidList.size() < 1){
            return null;
        }

        ArrayList<User> userList = new ArrayList<>();
        //????????????
        HashMap<Integer, User> userVoList = userService.getMapListInUid(uidList);

        //??????????????????
        for (UserBill userBillVo: userBillVoList) {
            User user = new User();
            User userVo = userVoList.get(userBillVo.getUid());

            user.setUid(userBillVo.getUid());
            user.setAvatar(userVo.getAvatar());
            user.setBrokeragePrice(userBillVo.getNumber());
            if(StringUtils.isBlank(userVo.getNickname())){
                user.setNickname(userVo.getPhone().substring(0, 2) + "****" + userVo.getPhone().substring(7));
            }else{
                user.setNickname(userVo.getNickname());
            }

            userList.add(user);
        }

        return userList;
    }

    /**
     * ???????????????
     * @return List<SystemGroupData>
     */
    @Override
    public List<UserSpreadBannerResponse> getSpreadBannerList(PageParamRequest pageParamRequest) {
        return systemGroupDataService.getListByGid(Constants.GROUP_DATA_ID_SPREAD_BANNER_LIST, UserSpreadBannerResponse.class);
    }

    /**
     * ????????????????????????????????????
     * @param type  String ????????????
     * @return ???????????????
     */
    @Override
    public Integer getNumberByTop(String type) {
        int number = 0;
        Integer userId = userService.getUserIdException();
        PageParamRequest pageParamRequest = new PageParamRequest();
        pageParamRequest.setLimit(100);

        List<UserBill> userBillVoList = userBillService.getTopBrokerageListByDate(type, pageParamRequest);
        if(userBillVoList.size() < 1){
            return number;
        }

        List<Integer> uidList = userBillVoList.stream().map(UserBill::getUid).collect(Collectors.toList());
        if(uidList.size() < 1){
            return number;
        }

        int i = 1;
        for (UserBill userBill : userBillVoList) {
            if(userBill.getUid().equals(userId)){
                number = i;
                break;
            }
            i++;
        }

        return number;
    }

    /**
     * ??????????????????
     * @return Boolean
     */
    @Override
    @Transactional(rollbackFor = {RuntimeException.class, Error.class, CrmebException.class})
    public Boolean transferIn(BigDecimal price) {
        try{
            //?????????????????????
            User user = userService.getInfo();
            BigDecimal freeze = userExtractService.getFreeze(user.getUid()); //????????????
            BigDecimal subtract = user.getBrokeragePrice().subtract(freeze);

            if(subtract.compareTo(price) < 0){
                throw new CrmebException("??????????????????????????? " + subtract + "???");
            }

            UserOperateFundsRequest request = new UserOperateFundsRequest();
            request.setFoundsCategory(Constants.USER_BILL_CATEGORY_BROKERAGE_PRICE);
            request.setType(0);
            request.setFoundsType(Constants.USER_BILL_TYPE_TRANSFER_IN);
            request.setUid(user.getUid());
            request.setValue(price);
            request.setTitle("???????????????");
            userService.updateFounds(request, true); //????????????


            UserOperateFundsRequest money = new UserOperateFundsRequest();
            money.setFoundsCategory(Constants.USER_BILL_CATEGORY_MONEY);
            money.setType(1);
            money.setFoundsType(Constants.USER_BILL_TYPE_TRANSFER_IN);
            money.setUid(user.getUid());
            money.setValue(price);
            money.setTitle("???????????????");
            userService.updateFounds(money, true); //????????????
            return true;
        }catch (Exception e){
            throw new CrmebException("???????????????" + e.getMessage());
        }

    }

    /**
     * ???????????????
     * @param openId  String ?????????????????????code?????????openId??????
     * @param unionId String unionId
     * @return List<LoginResponse>
     */
    private User programLogin(String openId, String unionId, RegisterThirdUserRequest request) {
        try {
            //??????????????????
            UserToken userToken = userTokenService.checkToken(openId, Constants.THIRD_LOGIN_TOKEN_TYPE_PROGRAM);
            if(null != userToken){
                return userService.getById(userToken.getUid());
            }

            if(StringUtils.isNotBlank(unionId)) {
                userToken = userTokenService.checkToken(unionId, Constants.THIRD_LOGIN_TOKEN_TYPE_PROGRAM);
                if (null != userToken) {
                    return userService.getById(userToken.getUid());
                }
            }

            //TODO ?????????????????????????????????1 ???????????????2 ???????????????????????????????????????????????????????????????
            User user = userService.registerByThird(request, Constants.USER_LOGIN_TYPE_PROGRAM);

            userTokenService.bind(openId, Constants.THIRD_LOGIN_TOKEN_TYPE_PROGRAM, user.getUid());
            if(StringUtils.isNotBlank(unionId)) {
                //????????????
                userTokenService.bind(unionId, Constants.THIRD_LOGIN_TOKEN_TYPE_UNION_ID, user.getUid());
            }
            return user;
        }catch (Exception e){
            throw new CrmebException(e.getMessage());
        }
    }

    /**
     * ????????????
     * @return
     */
    @Override
    public PageInfo<UserExtractRecordResponse> getExtractRecord(PageParamRequest pageParamRequest) {
        return userExtractService.getExtractRecord(userService.getUserIdException(), pageParamRequest);
    }

    /**
     * ???????????????
     * @return
     */
    @Override
    public BigDecimal getExtractTotalMoney(){
        return userExtractService.getExtractTotalMoney(userService.getUserIdException());
    }
}
