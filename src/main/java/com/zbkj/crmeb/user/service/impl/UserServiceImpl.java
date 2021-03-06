package com.zbkj.crmeb.user.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.common.CommonPage;
import com.common.PageParamRequest;
import com.constants.Constants;
import com.constants.SmsConstants;
import com.exception.CrmebException;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.utils.CrmebUtil;
import com.utils.DateUtil;
import com.utils.RedisUtil;
import com.utils.vo.dateLimitUtilVo;
import com.zbkj.crmeb.authorization.manager.TokenManager;
import com.zbkj.crmeb.authorization.model.TokenModel;
import com.zbkj.crmeb.finance.request.FundsMonitorSearchRequest;
import com.zbkj.crmeb.front.request.LoginRequest;
import com.zbkj.crmeb.front.request.PasswordRequest;
import com.zbkj.crmeb.front.request.RegisterRequest;
import com.zbkj.crmeb.front.request.UserBindingRequest;
import com.zbkj.crmeb.front.response.*;
import com.zbkj.crmeb.marketing.request.StoreCouponUserSearchRequest;
import com.zbkj.crmeb.marketing.response.StoreCouponUserResponse;
import com.zbkj.crmeb.marketing.service.StoreCouponService;
import com.zbkj.crmeb.marketing.service.StoreCouponUserService;
import com.zbkj.crmeb.store.model.StoreOrder;
import com.zbkj.crmeb.store.request.RetailShopStairUserRequest;
import com.zbkj.crmeb.store.request.StoreOrderSearchRequest;
import com.zbkj.crmeb.store.service.StoreOrderService;
import com.zbkj.crmeb.system.model.SystemUserLevel;
import com.zbkj.crmeb.system.request.SystemUserLevelSearchRequest;
import com.zbkj.crmeb.system.service.SystemConfigService;
import com.zbkj.crmeb.system.service.SystemUserLevelService;
import com.zbkj.crmeb.user.dao.UserDao;
import com.zbkj.crmeb.user.model.User;
import com.zbkj.crmeb.user.model.UserBill;
import com.zbkj.crmeb.user.model.UserLevel;
import com.zbkj.crmeb.user.model.UserSign;
import com.zbkj.crmeb.user.request.*;
import com.zbkj.crmeb.user.response.TopDetail;
import com.zbkj.crmeb.user.response.UserResponse;
import com.zbkj.crmeb.user.service.*;
import com.zbkj.crmeb.user.vo.UserOperateFundsVo;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ????????? ???????????????
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserDao, User> implements UserService {

    @Resource
    private UserDao userDao;

    @Autowired
    private UserBillService userBillService;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private TokenManager tokenManager;

    @Autowired
    private SystemConfigService systemConfigService;

    @Autowired
    private SystemUserLevelService systemUserLevelService;

    @Autowired
    private UserLevelService userLevelService;

    @Autowired
    private UserTagService userTagService;

    @Autowired
    private UserGroupService userGroupService;

    @Autowired
    private StoreOrderService storeOrderService;

    @Autowired
    private UserSignService userSignService;

    @Autowired
    private StoreCouponUserService storeCouponUserService;

    @Autowired
    private StoreCouponService storeCouponService;

    /**
     * ?????????????????????
     * @param request ????????????
     * @param pageParamRequest ????????????
     */
    @Override
    public PageInfo<UserResponse> getList(UserSearchRequest request, PageParamRequest pageParamRequest) {
        Page<User> pageUser = PageHelper.startPage(pageParamRequest.getPage(), pageParamRequest.getLimit());
        LambdaQueryWrapper<User> lambdaQueryWrapper = new LambdaQueryWrapper<>();

        if(request.getIsPromoter() != null){
            lambdaQueryWrapper.eq(User::getIsPromoter, request.getIsPromoter());
        }

        if(!StringUtils.isBlank(request.getGroupId())){
            lambdaQueryWrapper.apply(CrmebUtil.getFindInSetSql("group_id", request.getGroupId()));
        }

        if(!StringUtils.isBlank(request.getLabelId())){
            lambdaQueryWrapper.apply(CrmebUtil.getFindInSetSql("tag_id", request.getLabelId()));
        }

        if(!StringUtils.isBlank(request.getLevel())){
            lambdaQueryWrapper.in(User::getLevel, CrmebUtil.stringToArray(request.getLevel()));
        }

        if(StringUtils.isNotBlank(request.getUserType())){
            lambdaQueryWrapper.eq(User::getUserType, request.getUserType());
        }

        if(StringUtils.isNotBlank(request.getSex())){
            lambdaQueryWrapper.eq(User::getSex, request.getSex());
        }

        if(StringUtils.isNotBlank(request.getCountry())){
            lambdaQueryWrapper.eq(User::getCountry, request.getCountry());
            // ??????????????????
            if (StrUtil.isNotBlank(request.getCity())) {
                request.setProvince(request.getProvince().replace("???",""));
                request.setCity(request.getCity().replace("???",""));
                lambdaQueryWrapper.likeLeft(User::getAddres, request.getProvince() + "," + request.getCity());
            }
        }

        if (StrUtil.isNotBlank(request.getPayCount())) {
            if (request.getPayCount().equals("-1")) {
                lambdaQueryWrapper.eq(User::getPayCount, 0);
            } else {
                lambdaQueryWrapper.gt(User::getPayCount, Integer.valueOf(request.getPayCount()));
            }
        }

        if(request.getStatus() != null){
            lambdaQueryWrapper.eq(User::getStatus, request.getStatus());
        }

        dateLimitUtilVo dateLimit = DateUtil.getDateLimit(request.getDateLimit());

        if(!StringUtils.isBlank(dateLimit.getStartTime())){
            switch (request.getAccessType()){
                case 1://??????
//                    lambdaQueryWrapper.between(User::getUpdateTime, dateLimit.getStartTime(), dateLimit.getEndTime());
                    lambdaQueryWrapper.between(User::getCreateTime, dateLimit.getStartTime(), dateLimit.getEndTime());
                    lambdaQueryWrapper.apply(" create_time = last_login_time");
                    break;
                case 2://?????????
//                    lambdaQueryWrapper.notBetween(User::getUpdateTime, dateLimit.getStartTime(), dateLimit.getEndTime());
                    lambdaQueryWrapper.between(User::getLastLoginTime, dateLimit.getStartTime(), dateLimit.getEndTime());
                    break;
                case 3://?????????
//                    lambdaQueryWrapper.apply(" and create_time = last_login_time");
                    lambdaQueryWrapper.notBetween(User::getLastLoginTime, dateLimit.getStartTime(), dateLimit.getEndTime());
                    break;
                default://??????
                    lambdaQueryWrapper.between(User::getCreateTime, dateLimit.getStartTime(), dateLimit.getEndTime());
                    break;
            }
        }
        if(request.getKeywords() != null){
            String keywords = request.getKeywords();
            lambdaQueryWrapper.and(i -> i.or().like(User::getAccount, keywords) //????????????
                    .or().like(User::getRealName, keywords) //????????????
                    .or().like(User::getCardId, keywords) //???????????????
                    .or().like(User::getMark, keywords) //????????????
                    .or().like(User::getPhone, keywords) //????????????
                    .or().like(User::getAddIp, keywords) //??????IP
                    .or().like(User::getLastIp, keywords) //??????????????????ip
                    .or().like(User::getAddres, keywords) //????????????
                    .or().like(User::getNickname, keywords)); //????????????
        }
        lambdaQueryWrapper.orderByDesc(User::getUid);

        List<User> userList = userDao.selectList(lambdaQueryWrapper);
        List<UserResponse> userResponses = new ArrayList<>();
        for (User user : userList) {
            UserResponse userResponse = new UserResponse();
            BeanUtils.copyProperties(user, userResponse);
            // ??????????????????
            if(!StringUtils.isBlank(user.getGroupId())){
                userResponse.setGroupName(userGroupService.getGroupNameInId(user.getGroupId()));
            }

            // ??????????????????
            if(!StringUtils.isBlank(user.getTagId())){
                userResponse.setTagName(userTagService.getGroupNameInId(user.getTagId()));
            }

            //?????????????????????
            if(null == user.getSpreadUid() || user.getSpreadUid() == 0){
                userResponse.setSpreadNickname("???");
            }else{
                userResponse.setSpreadNickname(userDao.selectById(user.getSpreadUid()).getNickname());
            }
            userResponses.add(userResponse);
        }
        return CommonPage.copyPageInfo(pageUser,userResponses);
    }

    /**
     * ????????????
     * @author Mr.Zhang
     * @since 2020-04-10
     */
    @Override
    public boolean updateFounds(UserOperateFundsRequest request, boolean isSaveBill) {
        UserOperateFundsVo userOperateFundsVo = new UserOperateFundsVo(request.getUid(), request.getFoundsCategory(), getFounds(request));
        //??????????????? ?????????????????????????????????
        if(isSaveBill){
            createUserBill(request, getUserBalance(request));
        }
        Boolean result = userDao.updateFounds(userOperateFundsVo);    //????????????

        if(request.getFoundsCategory().equals(Constants.USER_BILL_CATEGORY_EXPERIENCE)){
            //????????????
            upLevel(request.getUid());
        }

        return result;
    }


    /**
     * ????????????
     */
    @Override
    public boolean updateIntegralMoney(UserOperateIntegralMoneyRequest request) {
        if(null == request.getMoneyValue() || null == request.getIntegralValue()){
            throw new CrmebException("????????????????????????");
        }

        if(request.getMoneyValue().compareTo(BigDecimal.ZERO) < 1 && request.getIntegralValue().compareTo(BigDecimal.ZERO) < 1){
            throw new CrmebException("????????????0.01");
        }

//        if (request.getMoneyValue().compareTo(BigDecimal.valueOf(1000000)) >= 0 || request.getIntegralValue().compareTo(BigDecimal.valueOf(1000000)) >= 0) {
//            throw new CrmebException("????????????999999");
//        }

        UserOperateFundsRequest userOperateFundsRequest = new UserOperateFundsRequest();
        userOperateFundsRequest.setTitle("????????????");
        userOperateFundsRequest.setUid(request.getUid());
        // ????????????
        if(request.getMoneyValue().compareTo(BigDecimal.ZERO) > 0){
            if(request.getMoneyType() == 1){
                userOperateFundsRequest.setFoundsType(Constants.USER_BILL_TYPE_SYSTEM_ADD);
                userOperateFundsRequest.setType(request.getMoneyType());
            }else{
                userOperateFundsRequest.setFoundsType(Constants.USER_BILL_TYPE_SYSTEM_SUB);
                userOperateFundsRequest.setType(0);
            }
            userOperateFundsRequest.setFoundsCategory(Constants.USER_BILL_CATEGORY_MONEY);
            userOperateFundsRequest.setValue(request.getMoneyValue());
            updateFounds(userOperateFundsRequest, true);
        }

        // ????????????
        if(request.getIntegralValue().compareTo(BigDecimal.ZERO) > 0){
            if(request.getIntegralType() == 1){
                userOperateFundsRequest.setFoundsType(Constants.USER_BILL_TYPE_SYSTEM_ADD);
                userOperateFundsRequest.setType(request.getIntegralType());
            }else{
                userOperateFundsRequest.setFoundsType(Constants.USER_BILL_TYPE_SYSTEM_SUB);
                userOperateFundsRequest.setType(0);
            }
            userOperateFundsRequest.setFoundsCategory(Constants.USER_BILL_CATEGORY_INTEGRAL);
            userOperateFundsRequest.setValue(request.getIntegralValue());
            updateFounds(userOperateFundsRequest, true);
        }

        return true;
    }

    /**
     * ????????????
     * @param userId Integer ??????id
     */
    public void upLevel(Integer userId) {
        //????????????????????????????????????
        SystemUserLevelSearchRequest systemUserLevelSearchRequest = new SystemUserLevelSearchRequest();
        systemUserLevelSearchRequest.setIsDel(false);
        systemUserLevelSearchRequest.setIsShow(true);
        List<SystemUserLevel> list = systemUserLevelService.getList(systemUserLevelSearchRequest, new PageParamRequest());

        User user = getById(userId);
        SystemUserLevel userLevelConfig = new SystemUserLevel();
        for (SystemUserLevel systemUserLevel : list) {
            if(user.getExperience() > systemUserLevel.getExperience()){
                userLevelConfig = systemUserLevel;
                continue;
            }
            break;
        }

        if(userLevelConfig.getId() == null){
            return;
        }

        //??????????????????
        userLevelService.level(userId, userLevelConfig.getId());
    }

    /**
     * ??????????????????
     * @param user ????????????
     * @return ????????????
     */
    @Override
    public boolean updateBase(User user) {
        LambdaUpdateWrapper<User> lambdaUpdateWrapper = new LambdaUpdateWrapper<>();
//        lambdaUpdateWrapper.setEntity(user);
        if(null == user.getUid()) return false;
        lambdaUpdateWrapper.eq(User::getUid,user.getUid());
        if(StringUtils.isNotBlank(user.getNickname())){
            lambdaUpdateWrapper.set(User::getNickname, user.getNickname());
        }
        if(StringUtils.isNotBlank(user.getAccount())){
            lambdaUpdateWrapper.set(User::getAccount, user.getAccount());
        }
        if(StringUtils.isNotBlank(user.getPwd())){
            lambdaUpdateWrapper.set(User::getPwd, user.getPwd());
        }
        if(StringUtils.isNotBlank(user.getRealName())){
            lambdaUpdateWrapper.set(User::getRealName, user.getRealName());
        }
        if(StringUtils.isNotBlank(user.getBirthday())){
            lambdaUpdateWrapper.set(User::getBirthday, user.getBirthday());
        }
        if(StringUtils.isNotBlank(user.getCardId())){
            lambdaUpdateWrapper.set(User::getCardId, user.getCardId());
        }
        if(StringUtils.isNotBlank(user.getMark())){
            lambdaUpdateWrapper.set(User::getMark, user.getMark());
        }
        if(null != user.getPartnerId()){
            lambdaUpdateWrapper.set(User::getPartnerId, user.getPartnerId());
        }
        if(StringUtils.isNotBlank(user.getGroupId())){
            lambdaUpdateWrapper.set(User::getGroupId, user.getGroupId());
        }
        if(StringUtils.isNotBlank(user.getTagId())){
            lambdaUpdateWrapper.set(User::getTagId, user.getTagId());
        }
        if(StringUtils.isNotBlank(user.getAvatar())){
            lambdaUpdateWrapper.set(User::getAvatar, user.getAvatar());
        }
        if(StringUtils.isNotBlank(user.getPhone())){
            lambdaUpdateWrapper.set(User::getPhone, user.getPhone());
        }
        if(StringUtils.isNotBlank(user.getAddIp())){
            lambdaUpdateWrapper.set(User::getAddIp, user.getAddIp());
        }
        if(StringUtils.isNotBlank(user.getLastIp())){
            lambdaUpdateWrapper.set(User::getLastIp, user.getLastIp());
        }
        if(null != user.getNowMoney() && user.getNowMoney().compareTo(BigDecimal.ZERO) > 0){
            lambdaUpdateWrapper.set(User::getNowMoney, user.getNowMoney());
        }
        if(null != user.getBrokeragePrice() && user.getBrokeragePrice().compareTo(BigDecimal.ZERO) > 0){
            lambdaUpdateWrapper.set(User::getBrokeragePrice, user.getBrokeragePrice());
        }
        if(null != user.getIntegral() && user.getIntegral().compareTo(BigDecimal.ZERO) >= 0){
            lambdaUpdateWrapper.set(User::getIntegral, user.getIntegral());
        }
        if(null != user.getExperience() && user.getExperience() > 0){
            lambdaUpdateWrapper.set(User::getExperience, user.getExperience());
        }
        if(null != user.getSignNum() && user.getSignNum() > 0){
            lambdaUpdateWrapper.set(User::getSignNum, user.getSignNum());
        }
        if(null != user.getStatus()){
            lambdaUpdateWrapper.set(User::getStatus, user.getStatus());
        }
        if(null != user.getLevel() && user.getLevel() > 0){
            lambdaUpdateWrapper.set(User::getLevel, user.getLevel());
        }
        if(null != user.getSpreadUid() && user.getSpreadUid() > 0){
            lambdaUpdateWrapper.set(User::getSpreadUid, user.getSpreadUid());
        }
        if(null != user.getSpreadTime()){
            lambdaUpdateWrapper.set(User::getSpreadTime, user.getSpreadTime());
        }
        if(StringUtils.isNotBlank(user.getUserType())){
            lambdaUpdateWrapper.set(User::getUserType, user.getUserType());
        }
        if(null != user.getIsPromoter()){
            lambdaUpdateWrapper.set(User::getIsPromoter, user.getIsPromoter());
        }
        if(null != user.getPayCount()){
            lambdaUpdateWrapper.set(User::getPayCount, user.getPayCount());
        }
        if(null != user.getSpreadCount()){
            lambdaUpdateWrapper.set(User::getSpreadCount, user.getSpreadCount());
        }
        if(StringUtils.isNotBlank(user.getAddres())){
            lambdaUpdateWrapper.set(User::getAddres, user.getAddres());
        }
        return update(lambdaUpdateWrapper);
    }

    @Override
    public boolean userPayCountPlus(User user) {
        LambdaUpdateWrapper<User> lambdaUpdateWrapper = new LambdaUpdateWrapper<>();
        lambdaUpdateWrapper.eq(User::getUid, user.getUid());
        lambdaUpdateWrapper.set(User::getPayCount, user.getPayCount()+1);
        return update(lambdaUpdateWrapper);
    }

    /**
     * ??????????????????
     * @param userId ??????id
     * @param price ??????
     * @return ????????????????????????
     */
    @Override
    public boolean updateNowMoney(int userId, BigDecimal price) {
        LambdaUpdateWrapper<User> lambdaUpdateWrapper = Wrappers.lambdaUpdate();
        lambdaUpdateWrapper.eq(User::getUid, userId);
        lambdaUpdateWrapper.set(User::getNowMoney, price);
        return update(lambdaUpdateWrapper);
    }

    /**
     * ????????????
     * @param id String id
     * @param groupIdValue Integer ??????Id
     */
    @Override
    public boolean group(String id, String groupIdValue) {
        ArrayList<User> userList = new ArrayList<>();
        //??????id??????
        List<User> list = getListInUid(CrmebUtil.stringToArray(id));
        if(list.size() < 1){
            throw new CrmebException("????????????????????????");
        }
        for (User user : list) {
            if(!StringUtils.isBlank(user.getGroupId())){
                groupIdValue = user.getGroupId() + groupIdValue;
            }
            //?????????????????????????????????id
            groupIdValue = userGroupService.clean(groupIdValue);
            if(StringUtils.isBlank(groupIdValue)){
                continue;
            }

            List<Integer> groupIdList = CrmebUtil.stringToArray(groupIdValue);
            user.setGroupId("," + StringUtils.join(groupIdList, ",") + ",");
            userList.add(user);
        }
        if(userList.size() < 1){
            throw new CrmebException("???????????????????????????");
        }
        return updateBatchById(userList);
    }

    /**
     * ??????id in list
     * @param uidList List<Integer> id
     */
    private List<User> getListInUid(List<Integer> uidList) {
        LambdaQueryWrapper<User> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.in(User::getUid, uidList);
        return userDao.selectList(lambdaQueryWrapper);
    }

    /**
     * ??????????????????
     * @param userId Integer id
     */
    @Override
    public boolean cleanLevel(Integer userId) {
        User user = new User();
        user.setUid(userId);
        user.setLevel(0);
        user.setCleanTime(DateUtil.nowDateTimeReturnDate(Constants.DATE_FORMAT));
        return updateById(user);
    }

    /**
     * ??????????????????, ?????????????????????????????????????????????
     * @param request RegisterRequest ????????????
     * @param ip String ip
     */
    @Override
    public LoginResponse register(RegisterRequest request, String ip) throws Exception {
        //???????????????
        checkValidateCode(request.getPhone(), request.getValidateCode());

        //???????????????
        redisUtil.remove(getValidateCodeRedisKey(request.getPhone()));

        //?????????????????????
        User user = getUserByAccount(request.getPhone());
        if(user == null){
            //????????????????????????????????????
            request.setPassword("Abc" + CrmebUtil.randomCount(10000, 99999));
            user = saveUser(request, ip);
        }

        //??????token
        LoginResponse loginResponse = new LoginResponse();
        loginResponse.setToken(token(user));

        //??????????????????
        bindSpread(user, request.getSpread());

        loginResponse.setUser(user);
        long time = Constants.TOKEN_EXPRESS_MINUTES * 60;

        loginResponse.setExpiresTime(DateUtil.addSecond(DateUtil.nowDateTime(), (int)time));

        return loginResponse;
    }

    /**
     * ??????????????????
     */
    @Override
    public LoginResponse login(LoginRequest request) throws Exception {
        User user = getUserByAccount(request.getPhone());
        if(user == null){
            throw new CrmebException("??????????????????");
        }

        if(!user.getStatus()){
            throw new CrmebException("??????????????????");
        }

        String password = CrmebUtil.encryptPassword(request.getPassword(), request.getPhone());
        if(!user.getPwd().equals(password)){
            throw new CrmebException("????????????");
        }

        LoginResponse loginResponse = new LoginResponse();
        loginResponse.setToken(token(user));
        user.setPwd(null);

        //??????????????????
        bindSpread(user, request.getSpreadPid());

        loginResponse.setUser(user);

        return loginResponse;
    }

    /**
     * ????????????
     * @param request PasswordRequest ??????
     * @return boolean
     */
    @Override
    public boolean password(PasswordRequest request) {
        //???????????????
        checkValidateCode(request.getPhone(), request.getValidateCode());

        LambdaQueryWrapper<User> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(User::getAccount, request.getPhone());
        User user = userDao.selectOne(lambdaQueryWrapper);

        //??????
        user.setPwd(CrmebUtil.encryptPassword(request.getPassword(), user.getAccount()));
        return update(user, lambdaQueryWrapper);
    }

    /**
     * ??????
     * @param token String token
     */
    @Override
    public void loginOut(String token) {
        tokenManager.deleteToken(token, Constants.USER_TOKEN_REDIS_KEY_PREFIX);
    }

    /**
     * ??????????????????
     * @return User
     */
    @Override
    public User getInfo() {
        if(getUserId() == 0){
            return null;
        }
        return getById(getUserId());
    }

    /**
     * ??????????????????
     * @return User
     */
    @Override
    public User getUserPromoter() {
        User user = getInfo();
        if(null == user){
            return null;
        }

        if(!user.getStatus()){
            user.setIsPromoter(false);
            return user;
        }

        //????????????
        //????????????????????????????????????
        String isOpen = systemConfigService.getValueByKey(Constants.CONFIG_KEY_STORE_BROKERAGE_IS_OPEN);
        if(StringUtils.isBlank(isOpen) || isOpen.equals("0")){
            user.setIsPromoter(false);
        }else{
            String type = systemConfigService.getValueByKey(Constants.CONFIG_KEY_STORE_BROKERAGE_MODEL);
            if(StringUtils.isBlank(type) || type.equals("2")){
                //????????????
                user.setIsPromoter(true);
            }
        }
        return user;
    }
    /**
     * ??????????????????
     * @return User
     */
    @Override
    public User getInfoException() {
        User user = getInfo();
        if(user == null){
            throw new CrmebException("????????????????????????");
        }

        if(!user.getStatus()){
            throw new CrmebException("????????????????????????");
        }
        return user;
    }

    @Override
    public User getInfoEmpty() {
        User user = getInfo();
        if(user == null){
            user = new User();
        }

//        if(!user.getStatus()){
//            throw new CrmebException("????????????????????????");
//        }
        return user;
    }

    /**
     * ??????????????????id
     * @return Integer
     */
    @Override
    public Integer getUserIdException() {
        return Integer.parseInt(tokenManager.getLocalInfoException("id"));
    }

    /**
     * ??????????????????id
     * @return Integer
     */
    @Override
    public Integer getUserId() {
        Object id = tokenManager.getLocalInfo("id");
        if(null == id){
            return 0;
        }
        return Integer.parseInt(id.toString());
    }

    /**
     * ?????????????????????????????????????????????
     * @param date String ????????????
     * @return HashMap<String, Object>
     */
    @Override
    public Integer getAddUserCountByDate(String date) {
        LambdaQueryWrapper<User> lambdaQueryWrapper = new LambdaQueryWrapper<>();

        if(StringUtils.isNotBlank(date)){
            dateLimitUtilVo dateLimit = DateUtil.getDateLimit(date);
            lambdaQueryWrapper.between(User::getCreateTime, dateLimit.getStartTime(), dateLimit.getEndTime());
        }
        return userDao.selectCount(lambdaQueryWrapper);
    }

    /**
     * ???????????????????????????????????????????????????
     * @param date String ????????????
     * @return HashMap<String, Object>
     */
    @Override
    public Map<Object, Object> getAddUserCountGroupDate(String date) {
        Map<Object, Object> map = new HashMap<>();
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("count(uid) as uid", "left(create_time, 10) as create_time");
        if(StringUtils.isNotBlank(date)){
            dateLimitUtilVo dateLimit = DateUtil.getDateLimit(date);
            queryWrapper.between("create_time", dateLimit.getStartTime(), dateLimit.getEndTime());
        }
        queryWrapper.groupBy("left(create_time, 10)").orderByAsc("create_time");
        List<User> list = userDao.selectList(queryWrapper);
        if(list.size() < 1){
            return map;
        }

        for (User user : list) {
            map.put(DateUtil.dateToStr(user.getCreateTime(), Constants.DATE_FORMAT_DATE), user.getUid());
        }
        return map;
    }

    /**
     * ???????????????
     * @return boolean
     */
    @Override
    public boolean bind(UserBindingRequest request) {
        //???????????????
        checkValidateCode(request.getPhone(), request.getValidateCode());

        //???????????????
        redisUtil.remove(getValidateCodeRedisKey(request.getPhone()));

        //??????????????????????????????????????????
        User user = getUserByAccount(request.getPhone());
        if(null != user){
            throw new CrmebException("???????????????????????????");
        }

        //?????????????????????
        User bindUser = getInfoException();
        bindUser.setAccount(request.getPhone());
        bindUser.setPhone(request.getPhone());

        return updateById(bindUser);
    }

    /**
     * ????????????
     * @return UserCenterResponse
     */
    @Override
    public UserCenterResponse getUserCenter() {
        UserCenterResponse userCenterResponse = new UserCenterResponse();
        User currentUser = getInfo();
        BeanUtils.copyProperties(currentUser, userCenterResponse);
        userCenterResponse.setIntegral(currentUser.getIntegral().intValue());
        UserCenterOrderStatusNumResponse userCenterOrderStatusNumResponse = new UserCenterOrderStatusNumResponse();
        userCenterOrderStatusNumResponse.setNoBuy(0);
        userCenterOrderStatusNumResponse.setNoPink(0);
        userCenterOrderStatusNumResponse.setNoPostage(0);
        userCenterOrderStatusNumResponse.setNoRefund(0);
        userCenterOrderStatusNumResponse.setNoReply(0);
        userCenterOrderStatusNumResponse.setNoTake(0);
        userCenterResponse.setOrderStatusNum(userCenterOrderStatusNumResponse);
        PageParamRequest pageParamRequest = new PageParamRequest();
        pageParamRequest.setPage(1); pageParamRequest.setLimit(999);
        List<StoreCouponUserResponse> storeCoupons = storeCouponUserService.getListFront(getUserIdException(), pageParamRequest);
        userCenterResponse.setCouponCount(null != storeCoupons ? storeCoupons.size():0);
        userCenterResponse.setLevel(currentUser.getLevel());

        // ??????????????????????????????
        Integer memberFuncStatus = Integer.valueOf(systemConfigService.getValueByKey("member_func_status"));
        if(memberFuncStatus == 0){
            userCenterResponse.setVip(false);
        }else{
            userCenterResponse.setVip(userCenterResponse.getLevel() > 0);
            UserLevel userLevel = userLevelService.getUserLevelByUserId(currentUser.getUid());
            if(null != userLevel){
                SystemUserLevel systemUserLevel = systemUserLevelService.getByLevelId(userLevel.getLevelId());
                userCenterResponse.setVipIcon(systemUserLevel.getIcon());
                userCenterResponse.setVipName(systemUserLevel.getName());
            }
        }
        String rechargeSwitch = systemConfigService.getValueByKey("recharge_switch");
        if (StrUtil.isNotBlank(rechargeSwitch)) {
            userCenterResponse.setRechargeSwitch(Boolean.valueOf(rechargeSwitch));
        }
        return userCenterResponse;
    }

    /**
     * ????????????id?????????????????? map??????
     * @return HashMap<Integer, User>
     */
    @Override
    public HashMap<Integer, User> getMapListInUid(List<Integer> uidList) {
        List<User> userList = getListInUid(uidList);
        return getMapByList(userList);
    }

    /**
     * ????????????id?????????????????? map??????
     * @return HashMap<Integer, User>
     */
    @Override
    public HashMap<Integer, User> getMapByList(List<User> list) {
        HashMap<Integer, User> map = new HashMap<>();
        if(null == list || list.size() < 1){
            return map;
        }

        for (User user: list) {
            map.put(user.getUid(), user);
        }

        return map;
    }

    /**
     * ????????????????????????
     * @param userId Integer ??????id
     */
    @Override
    public void repeatSignNum(Integer userId) {
        User user = new User();
        user.setUid(userId);
        user.setSignNum(0);
        updateById(user);
    }

    /**
     * ??????????????????(??????????????? ?????????????????? ????????????)
     * @author Mr.Zhang
     * @since 2020-06-08
     */
    @Override
    public UserCommissionResponse getCommission() {
        UserCommissionResponse userCommissionResponse = new UserCommissionResponse();
        //???????????????
        userCommissionResponse.setLastDayCount(userBillService.getSumBigDecimal(0, getUserId(), Constants.USER_BILL_CATEGORY_MONEY, Constants.SEARCH_DATE_YESTERDAY, null));
        userCommissionResponse.setExtractCount(userBillService.getSumBigDecimal(0, getUserId(), Constants.USER_BILL_CATEGORY_MONEY, null, Constants.USER_BILL_TYPE_EXTRACT));
        userCommissionResponse.setCommissionCount(getInfo().getBrokeragePrice());
        return userCommissionResponse;
    }

    /**
     * ????????????
     * @param id String id
     * @param tagIdValue Integer ??????Id
     */
    @Override
    public boolean tag(String id, String tagIdValue) {
        //??????id??????
        ArrayList<User> userList = new ArrayList<>();
        //??????id??????
        List<User> list = getListInUid(CrmebUtil.stringToArray(id));
        if(list.size() < 1){
            throw new CrmebException("????????????????????????");
        }
        for (User user : list) {
            if(!StringUtils.isBlank(user.getTagId())){
                tagIdValue = user.getTagId() + tagIdValue;
            }
            //?????????????????????????????????id
            tagIdValue = userTagService.clean(tagIdValue);
            if(StringUtils.isBlank(tagIdValue)){
                continue;
            }

            List<Integer> tagIdList = CrmebUtil.stringToArray(tagIdValue);
            user.setTagId("," + StringUtils.join(tagIdList, ",") + ",");
            userList.add(user);
        }
        if(userList.size() < 1){
            throw new CrmebException("???????????????????????????");
        }
        return updateBatchById(userList);
    }

    /**
     * ????????????id?????????????????????????????????
     * @param userIdList List<Integer> ??????id??????
     * @return List<User>
     */
    @Override
    public List<Integer> getSpreadPeopleIdList(List<Integer> userIdList) {
        LambdaQueryWrapper<User> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.select(User::getUid); //????????????id
        lambdaQueryWrapper.in(User::getSpreadUid, userIdList); //xx???????????????
        List<User> list = userDao.selectList(lambdaQueryWrapper);

        if(null == list || list.size() < 1){
            return new ArrayList<>();
        }
        return list.stream().map(User::getUid).distinct().collect(Collectors.toList());
    }

    /**
     * ????????????id?????????????????????????????????
     * @return List<User>
     */
    @Override
   public List<UserSpreadPeopleItemResponse> getSpreadPeopleList(
            List<Integer> userIdList, String keywords, String sortKey, String isAsc, PageParamRequest pageParamRequest) {

        PageHelper.startPage(pageParamRequest.getPage(), pageParamRequest.getLimit());

        Map<String, Object> map = new HashMap<>();
        map.put("userIdList", userIdList.stream().map(String::valueOf).distinct().collect(Collectors.joining(",")));
        if(StringUtils.isNotBlank(keywords)){
            map.put("keywords", "%"+keywords+"%");
        }
        map.put("sortKey", "create_time");
        if(StringUtils.isNotBlank(sortKey)){
            map.put("sortKey", sortKey);
        }
        map.put("sortValue", Constants.SORT_DESC);
        if(isAsc.toLowerCase().equals(Constants.SORT_ASC)){
            map.put("sortValue", Constants.SORT_ASC);
        }

        return userDao.getSpreadPeopleList(map);
    }



    /**
     * ??????????????????token
     */
    @Override
    public String token(User user) throws Exception {
        TokenModel token = tokenManager.createToken(user.getAccount(), user.getUid().toString(), Constants.USER_TOKEN_REDIS_KEY_PREFIX);
        return token.getToken();
    }

    /**
     * ??????????????????
     * @param request RegisterRequest ????????????
     * @param ip String ip
     * @return User
     */
    private User saveUser(RegisterRequest request, String ip) {
        //?????????????????????
        User user = new User();
        if(request.getSpread() != null && request.getSpread() > 0){

            User spread = getById(request.getSpread());
            if(spread != null){
                user.setSpreadUid(request.getSpread());
                user.setSpreadTime(DateUtil.nowDateTimeReturnDate(Constants.DATE_FORMAT));
            }
        }


        user.setAccount(request.getPhone());
        user.setPwd(CrmebUtil.encryptPassword(request.getPassword(), request.getPhone()));
        user.setPhone(request.getPhone());
        user.setUserType(Constants.USER_LOGIN_TYPE_H5);
        user.setAddIp(ip);
        user.setLastIp(ip);
        user.setNickname(
                DigestUtils.md5Hex(request.getPhone() + DateUtil.getNowTime()).
                        subSequence(0, 12).
                        toString()
        );
        user.setAvatar(systemConfigService.getValueByKey(Constants.USER_DEFAULT_AVATAR_CONFIG_KEY));
        save(user);

        // ??????????????????
        if(null != request.getSpread() && request.getSpread() > 0){
            spread(user.getUid(),request.getSpread());
        }
        return user;
    }

    /**
     * ?????????????????????
     */
    private void checkValidateCode(String phone, String value) {
        Object validateCode = redisUtil.get(getValidateCodeRedisKey(phone));
        if(validateCode == null){
            throw new CrmebException("??????????????????");
        }

        if(!validateCode.toString().equals(value)){
            throw new CrmebException("???????????????");
        }
    }

    /**
     * ?????????????????????
     * @param phone String ?????????
     * @return String
     */
    @Override
    public String getValidateCodeRedisKey(String phone) {
        return SmsConstants.SMS_VALIDATE_PHONE + phone;
    }

    /**
     * ??????????????????????????????
     */
    private User getUserByAccount(String account) {
        LambdaQueryWrapper<User> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(User::getAccount, account);
        return userDao.selectOne(lambdaQueryWrapper);
    }

    /**
     * ??????title
     * @return String
     */
    private String getTitle(UserOperateFundsRequest request) {
        String operate = (request.getType() == 1) ? "??????" : "??????";
        String founds = "";
        switch (request.getFoundsCategory()){
            case Constants.USER_BILL_CATEGORY_INTEGRAL:
                founds = "??????";
                break;
            case Constants.USER_BILL_CATEGORY_MONEY:
                founds = "??????";
                break;
            case Constants.USER_BILL_CATEGORY_EXPERIENCE:
                founds = "??????";
                break;
            case Constants.USER_BILL_CATEGORY_BROKERAGE_PRICE:
                founds = "??????";
                break;
        }

        return Constants.USER_BILL_OPERATE_LOG_TITLE.replace("{$title}", request.getTitle()).replace("{$operate}", operate).replace("{$founds}", founds);
    }


    /**
     * ????????????????????????
     */
    private void createUserBill(UserOperateFundsRequest request, BigDecimal balance) {
        UserBill userBill = new UserBill();
        userBill.setTitle(request.getTitle());
        userBill.setUid(request.getUid());
        userBill.setCategory(request.getFoundsCategory());
        userBill.setType(request.getFoundsType());
        userBill.setNumber(request.getValue());
        userBill.setLinkId(request.getLinkId());  //??????id
        userBill.setMark(getTitle(request).replace("{$value}", request.getValue().toString()));
        userBill.setPm(request.getType());
        userBill.setBalance(balance.add(request.getValue()));
        userBillService.save(userBill);
    }

    /**
     * ????????????
     */
    private String getFounds(UserOperateFundsRequest request) {
        if(request.getType() == 1){
            return " +" + request.getValue();
        }
        if(request.getType() == 0){
            checkFounds(request);
            return " -" + request.getValue();
        }

        return " +0.00";
    }

    /**
     * ????????????
     */
    private void checkFounds(UserOperateFundsRequest request) {
        BigDecimal value = getUserBalance(request);
        if(value == null){
            throw new CrmebException("??????????????????????????????");
        }

        int result = value.subtract(request.getValue()).compareTo(BigDecimal.ZERO);
        if(result < 0){
            throw new CrmebException("???????????????????????? " + value + "??? ????????????????????????????????? " + value);
        }
    }

    /**
     * ??????????????????
     */
    private BigDecimal getUserBalance(UserOperateFundsRequest request) {
        //??????????????????
        User user = getById(request.getUid());

        BigDecimal value = null;
        if(request.getFoundsCategory().equals(Constants.USER_BILL_CATEGORY_INTEGRAL)){
            value = user.getIntegral();
        }

        if(request.getFoundsCategory().equals(Constants.USER_BILL_CATEGORY_MONEY)){
            value = user.getNowMoney();
        }

        if(request.getFoundsCategory().equals(Constants.USER_BILL_CATEGORY_EXPERIENCE)){
            value = new BigDecimal(user.getExperience());
        }

        if(request.getFoundsCategory().equals(Constants.USER_BILL_CATEGORY_BROKERAGE_PRICE)){
            value = user.getBrokeragePrice();
        }


        return value;
    }

    /**
     * ???????????????????????????spread_uid???????????????????????????
     * @return
     */
    private List<User> getUserRelation(Integer userId){
        List<User> userList = new ArrayList<>();
        User currUser = userDao.selectById(userId);
        if(currUser.getSpreadUid() > 0){
            User spUser1 = userDao.selectById(currUser.getSpreadUid());
            if(null != spUser1){
                userList.add(spUser1);
                if(spUser1.getSpreadUid() > 0){
                    User spUser2 = userDao.selectById(spUser1.getSpreadUid());
                    if(null != spUser2){
                        userList.add(spUser2);
                    }
                }
            }
        }
        return userList;
    }

    /**
     * ??????????????????????????????????????????
     * @param userId
     * @param type 0=???????????????1=???????????????2=???????????????3=??????????????????4=???????????????5=????????????
     * @param pageParamRequest
     * @return
     */
    @Override
    public Object getInfoByCondition(Integer userId, Integer type, PageParamRequest pageParamRequest) {
        switch (type){
            case 0:
                StoreOrderSearchRequest sor = new StoreOrderSearchRequest();
                sor.setUid(userId);
                return storeOrderService.getList(sor, pageParamRequest);
            case 1:
                FundsMonitorSearchRequest fmsq = new FundsMonitorSearchRequest();
                fmsq.setUid(userId);
                fmsq.setCategory(Constants.USER_BILL_CATEGORY_INTEGRAL);
                return userBillService.getList(fmsq,pageParamRequest);
            case 2:
                UserSign userSign = new UserSign();
                userSign.setUid(userId);
                return userSignService.getListByCondition(userSign,pageParamRequest);
            case 3:
                StoreCouponUserSearchRequest scur = new StoreCouponUserSearchRequest();
                scur.setUid(userId);
                PageInfo<StoreCouponUserResponse> pageInfo = storeCouponUserService.getList(scur,pageParamRequest);
                if(null == pageInfo.getList() || pageInfo.getList().size() < 1){
                    return new ArrayList<>();
                }
                return pageInfo.getList();
            case 4:
                FundsMonitorSearchRequest fmsqq = new FundsMonitorSearchRequest();
                fmsqq.setUid(userId);
                fmsqq.setCategory(Constants.USER_BILL_CATEGORY_MONEY);
                return userBillService.getList(fmsqq,pageParamRequest);
            case 5:
                return getUserRelation(userId);
        }

        return new ArrayList<>();
    }

    /**
     * ????????????????????????
     * @param userId Integer ??????id
     * @return Object
     */
    @Override
    public TopDetail getTopDetail(Integer userId) {
        TopDetail topDetail = new TopDetail();
        User currentUser = userDao.selectById(userId);
        topDetail.setUser(currentUser);
        topDetail.setBalance(currentUser.getNowMoney());
        topDetail.setIntegralCount(currentUser.getIntegral());
        topDetail.setMothConsumeCount(storeOrderService.getSumBigDecimal(userId, Constants.SEARCH_DATE_MONTH));
        topDetail.setAllConsumeCount(storeOrderService.getSumBigDecimal(userId, null));
        topDetail.setMothOrderCount(storeOrderService.getOrderCount(userId, Constants.SEARCH_DATE_MONTH));
        topDetail.setAllOrderCount(storeOrderService.getOrderCount(userId, null));
        return topDetail;
    }

    /**
     * ??????????????????????????????
     * @param thirdUserRequest RegisterThirdUser ??????????????????????????????
     * @return User
     */
    @Override
    public User registerByThird(RegisterThirdUserRequest thirdUserRequest, String type) {
        User user = new User();
        user.setAccount(DigestUtils.md5Hex(CrmebUtil.getUuid() + DateUtil.getNowTime()));
        user.setUserType(type);
        user.setNickname(thirdUserRequest.getNickName());
        String _avatar = null;
        switch (type) {
            case Constants.USER_LOGIN_TYPE_PUBLIC:
                _avatar = thirdUserRequest.getHeadimgurl();
                break;
            case Constants.USER_LOGIN_TYPE_PROGRAM:
            case Constants.USER_LOGIN_TYPE_H5:
                _avatar = thirdUserRequest.getAvatar();
                break;
        }
        user.setAvatar(_avatar);
        user.setSpreadUid(thirdUserRequest.getSpreadPid());
        user.setSpreadTime(DateUtil.nowDateTime());
        user.setSex(Integer.parseInt(thirdUserRequest.getSex()));
        user.setAddres(thirdUserRequest.getCountry() + "," + thirdUserRequest.getProvince() + "," + thirdUserRequest.getCity());
        save(user);
        return user;
    }

     /**
      * ????????????id??????????????????????????????
      * @param userIds ??????id??????
      * @return ????????????
     */
    @Override
    public List<User> getSpreadPeopleList(List<Integer> userIds) {
        LambdaQueryWrapper<User> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.in(User::getUid, userIds);
        List<User> userList = userDao.selectList(lambdaQueryWrapper);
        return userList;
    }

    /**
     * ??????????????????
     * @param currentUserId ????????????id ????????????
     * @param spreadUserId ?????????id
     * @return ??????????????????????????????
     */
    @Override
    public boolean spread(Integer currentUserId, Integer spreadUserId) {
        // ????????????????????????
        User currentUser = userDao.selectById(currentUserId);
        if(null == currentUser) throw new CrmebException("??????id:"+currentUserId+"?????????");
        User spreadUser = userDao.selectById(spreadUserId);
        if(null == spreadUser) throw new CrmebException("??????id:"+spreadUserId+"?????????");
        // ????????????????????????
        if(!spreadUser.getIsPromoter()) throw new CrmebException("??????id:"+spreadUserId+"?????????????????????");
        // ?????????????????????????????????
        LambdaQueryWrapper<User> lmq = new LambdaQueryWrapper<>();
        lmq.like(User::getPath,spreadUserId);
        lmq.eq(User::getUid, currentUserId);
        List<User> spreadUsers = userDao.selectList(lmq);
        if(spreadUsers.size() > 0){
            throw new CrmebException("????????????????????????");
        }
        currentUser.setPath(currentUser.getPath()+spreadUser.getUid()+"/");
        currentUser.setSpreadUid(spreadUserId);
        currentUser.setSpreadTime(new Date());
        currentUser.setSpreadCount(currentUser.getSpreadCount()+1);
        return userDao.updateById(currentUser) >= 0;
    }

    /**
     * ???????????????????????????????????????????????????????????????
     * @param request ??????????????????
     * @return ??????????????????????????????
     */
    @Override
    public PageInfo<User> getUserListBySpreadLevel(RetailShopStairUserRequest request, PageParamRequest pageParamRequest) {
        Page<User> userPage = PageHelper.startPage(pageParamRequest.getPage(),pageParamRequest.getLimit());
        List<User> users = getUsersBySpreadLevel(request);
        return CommonPage.copyPageInfo(userPage, users);
    }

    // ???????????????????????????
    private List<User> getFirstSpreadUserListPage(RetailShopStairUserRequest request, PageParamRequest pageParamRequest) {
        Page<User> userPage = PageHelper.startPage(pageParamRequest.getPage(),pageParamRequest.getLimit());
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getSpreadUid, request.getUid());
        if (StrUtil.isNotBlank(request.getNickName())) {
            queryWrapper.and(e -> e.like(User::getNickname, request.getNickName()).or().eq(User::getUid, request.getNickName())
                    .or().eq(User::getPhone, request.getNickName()));

        }
        queryWrapper.eq(User::getSpreadUid, request.getUid());

        return null;
    };
    // ???????????????????????????
    private List<User> getSecondSpreadUserListPage() {return null;};
    // ???????????????????????????
    private List<User> getAllSpreadUserListPage() {return null;};


    /**
     * ???????????????????????????????????????????????????
     * @param request ?????????????????????????????????
     * @return ??????????????????
     */
    @Override
    public PageInfo<StoreOrder> getOrderListBySpreadLevel(RetailShopStairUserRequest request, PageParamRequest pageParamRequest) {
//        List<User> users = getUsersBySpreadLevel(request);
//        if(users.size() == 0){
//            return new PageInfo<>();
//        }
        // ?????????????????????
        if (ObjectUtil.isNull(request.getType())) {
            request.setType(0);
        }
        List<User> userList = getSpreadListBySpreadIdAndType(request.getUid(), request.getType());
        if (CollUtil.isEmpty(userList)) {
            return new PageInfo<>();
        }

        List<Integer> userIds = userList.stream().map(User::getUid).distinct().collect(Collectors.toList());

        return storeOrderService.findListByUserIdsForRetailShop(userIds, request, pageParamRequest);

//        Page<StoreOrder> storeOrderPage = PageHelper.startPage(pageParamRequest.getPage(), pageParamRequest.getLimit());
//        return CommonPage.copyPageInfo(storeOrderPage, storeOrderService.getOrderByUserIdsForRetailShop(userIds));
    }

    /**
     * ?????????????????????
     * @param spreadUid  ???Uid
     * @param type      ?????? 0 = ?????? 1=??????????????? 2=???????????????
     */
    private List<User> getSpreadListBySpreadIdAndType(Integer spreadUid, Integer type) {
        // ?????????????????????
        List<User> userList = getSpreadListBySpreadId(spreadUid);
        if (CollUtil.isEmpty(userList)) return userList;
        if (type.equals(1)) return userList;
        // ?????????????????????
        List<User> userSecondList = CollUtil.newArrayList();
        userList.forEach(user -> {
            if (user.getIsPromoter()) {
                List<User> childUserList = getSpreadListBySpreadId(user.getUid());
                if (CollUtil.isNotEmpty(childUserList)) {
                    userSecondList.addAll(childUserList);
                }
            }
        });
        if (type.equals(2)) {
            return userSecondList;
        }
        userList.addAll(userSecondList);
        return userList;
    }

    /**
     * ?????????????????????
     * @param spreadUid  ???Uid
     */
    private List<User> getSpreadListBySpreadId(Integer spreadUid) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getSpreadUid, spreadUid);
        return userDao.selectList(queryWrapper);
    }

    /**
     * ????????????id???????????????????????????
     * @param userId ???????????????id
     * @return ??????????????????
     */
    @Override
    public boolean clearSpread(Integer userId) {
        User user = new User();
        user.setUid(userId);
        user.setPath("/0/");
        user.setSpreadUid(0);
        user.setSpreadTime(null);
        return userDao.updateById(user) > 0;
    }

    /**
     * ?????? ???????????????????????????????????????????????????
     * @param request ????????????
     * @return ????????????
     */
    private List<User> getUsersBySpreadLevel(RetailShopStairUserRequest request) {
        LambdaQueryWrapper<User> lqw = new LambdaQueryWrapper<>();
//        lqw.eq(User::getUid,request.getUid());
//        lqw.like(User::getPath, request.getUid());
        lqw.eq(User::getSpreadUid, request.getUid());
        if(StringUtils.isNotBlank(request.getDateLimit())){
            dateLimitUtilVo dateLimit = DateUtil.getDateLimit(request.getDateLimit());
            lqw.between(User::getCreateTime,dateLimit.getStartTime(),dateLimit.getEndTime());
        }
        if(StringUtils.isNotBlank(request.getNickName())){
            lqw.like(User::getNickname, request.getNickName())
                    .or().eq(User::getPhone, request.getNickName());
        }
        String type ;
        if(null == request.getType()){
            type = ">= 3";
        }else {
            type = request.getType() == 1 ? "= 3":"= 4";
        }
        // path?????? ?????? ???/???????????????????????? = ?????? 2=?????? 3=??????????????? 4= ???????????????
        lqw.last("and (LENGTH(path) - LENGTH(REPLACE(path,'/','')))" + type);
        return userDao.selectList(lqw);
    }

    /**
     * ????????????????????????????????????
     * @param current ????????????
     * @return ???????????????
     */
    @Override
    public User updateForPromoter(User current) {
        if(current.getIsPromoter()){
            return current;
        }
        LambdaQueryWrapper<StoreOrder> lqw = new LambdaQueryWrapper<>();
        lqw.eq(StoreOrder::getPaid, 1);
        lqw.eq(StoreOrder::getRefundStatus, 0);
        lqw.eq(StoreOrder::getUid, current.getUid());
        List<StoreOrder> orders = storeOrderService.list(lqw);
        BigDecimal price = orders.stream().map(e->e.getPayPrice()).reduce(BigDecimal.ZERO, BigDecimal::add);
        // ??????????????????????????????
        boolean isPromoter = checkIsPromoter(price);
        if(isPromoter) {
            current.setIsPromoter(true);
            int updateCount = userDao.updateById(current);
            if(updateCount <= 0) throw new CrmebException("?????????????????? ????????????????????????");
            return current;
        }
        return current;
    }

    /**
     * ?????????????????????????????????
     * @param price ????????????
     * @return ???????????????
     */
    @Override
    public boolean checkIsPromoter(BigDecimal price) {
        String brokerageStatus = systemConfigService.getValueByKey(Constants.CONFIG_KEY_STORE_BROKERAGE_IS_OPEN);
        if(brokerageStatus.equals("1")){
            return false;
        }else{
            BigDecimal  brokeragePrice = BigDecimal.valueOf(Integer.parseInt(systemConfigService.getValueByKey("store_brokerage_price")));
            return price.compareTo(brokeragePrice) == 1;
        }
    }

    /**
     * ???????????????
     * @param type String ??????
     * @param pageParamRequest PageParamRequest ??????
     * @return List<User>
     */
    @Override
    public List<User> getTopSpreadPeopleListByDate(String type, PageParamRequest pageParamRequest) {
        PageHelper.startPage(pageParamRequest.getPage(), pageParamRequest.getLimit());
        dateLimitUtilVo dateLimit = DateUtil.getDateLimit(type);
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("count(spread_count) as spread_count, spread_uid")
                .gt("spread_uid", 0)
                .eq("status", true);
        if(!StringUtils.isBlank(dateLimit.getStartTime())){
            queryWrapper.between("create_time", dateLimit.getStartTime(), dateLimit.getEndTime());
        }
        queryWrapper.groupBy("spread_uid").orderByDesc("spread_count");
        List<User> spreadVoList = userDao.selectList(queryWrapper);
        if(spreadVoList.size() < 1){
            return null;
        }

        List<Integer> spreadIdList = spreadVoList.stream().map(User::getSpreadUid).collect(Collectors.toList());
        if(spreadIdList.size() < 1){
            return null;
        }

        ArrayList<User> userList = new ArrayList<>();
        //????????????
        HashMap<Integer, User> userVoList = getMapListInUid(spreadIdList);

        //??????????????????
        for (User spreadVo: spreadVoList) {
            User user = new User();
            User userVo = userVoList.get(spreadVo.getSpreadUid());
            user.setUid(spreadVo.getSpreadUid());
            user.setAvatar(userVo.getAvatar());
            user.setSpreadCount(userVo.getSpreadCount());
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
     * @param minPayCount int ??????????????????
     * @param maxPayCount int ??????????????????
     * @return Integer
     */
    @Override
    public Integer getCountByPayCount(int minPayCount, int maxPayCount) {
        LambdaQueryWrapper<User> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.between(User::getPayCount, minPayCount, maxPayCount);
        return userDao.selectCount(lambdaQueryWrapper);
    }

    @Override
    public List<User> getUserByEntity(User user) {
        LambdaUpdateWrapper<User> lambdaUpdateWrapper = new LambdaUpdateWrapper<>();
        lambdaUpdateWrapper.setEntity(user);
        return userDao.selectList(lambdaUpdateWrapper);
    }

    /**
     * ???????????????????????????????????????
     * @param uid Integer ??????id
     * @param price BigDecimal ??????????????????
     * @return void
     */
    @Override
    public void consumeAfterUpdateUserFounds(Integer uid, BigDecimal price, String type) {
        //??????????????????
        String integralStr = systemConfigService.getValueByKey(Constants.CONFIG_KEY_INTEGRAL_RATE);
        BigDecimal integral = new BigDecimal(integralStr);

        //????????????????????????
        UserOperateFundsRequest founds = new UserOperateFundsRequest();
        founds.setFoundsType(type);
        founds.setTitle(Constants.ORDER_LOG_MESSAGE_PAY_SUCCESS);

        founds.setUid(uid);
        founds.setFoundsCategory(Constants.USER_BILL_CATEGORY_INTEGRAL);
        founds.setType(1);

        //?????? CrmebUtil getRate??????
        founds.setValue(integral.multiply(price).setScale(0, BigDecimal.ROUND_DOWN));
        updateFounds(founds, true);

        //????????????????????????
        founds.setUid(uid);
        founds.setFoundsCategory(Constants.USER_BILL_CATEGORY_EXPERIENCE);
        founds.setType(1);
        founds.setValue(price.setScale(0, BigDecimal.ROUND_DOWN));
        updateFounds(founds, true);
    }

    /**
     * ??????????????????
     * @param user User ??????user???
     * @param spreadUid Integer ?????????id
     * @return void
     */
    @Override
    public void bindSpread(User user, Integer spreadUid) {
        //????????????????????????????????????????????????????????????????????????
        if(null == spreadUid || spreadUid == 0){
            return;
        }
        //????????????????????????????????????????????????????????????????????????????????????
        if(user.getSpreadUid() > 0){
            return;
        }

        String distribution = systemConfigService.getValueByKey(Constants.CONFIG_KEY_DISTRIBUTION_TYPE);

        //???????????? + ??????????????????id????????????id?????????
        if(StringUtils.isNotBlank(distribution) && distribution.equals("0") && !user.getSpreadUid().equals(spreadUid)){
            user.setSpreadUid(spreadUid);
            user.setSpreadTime(DateUtil.nowDateTime());
        }
        updateById(user);
    }

    @Override
    public boolean upadteBrokeragePrice(User user, BigDecimal newBrokeragePrice) {
        UpdateWrapper<User> updateWrapper = new UpdateWrapper<>();
        updateWrapper.set("brokerage_price", newBrokeragePrice)
                .eq("uid", user.getUid()).eq("brokerage_price", user.getBrokeragePrice());
        return userDao.update(user, updateWrapper) > 0;
    }

    /**
     * ???????????????????????????
     * @return
     */
    @Override
    public BigDecimal getUnCommissionPrice() {
        LambdaQueryWrapper<User> lq = Wrappers.lambdaQuery();
        lq.select(User::getBrokeragePrice);
        List<User> userList = userDao.selectList(lq);
        double sum = 0;
        if (CollUtil.isNotEmpty(userList)) {
            sum = userList.stream().mapToDouble(e -> e.getBrokeragePrice().doubleValue()).sum();
        }
        return BigDecimal.valueOf(sum);
    }

    /**
     * ???????????????
     * @param request
     * @return
     */
    @Override
    public Boolean editSpread(UserUpdateSpreadRequest request) {
        Integer userId = request.getUserId();
        Integer spreadUid = request.getSpreadUid();
        if (userId.equals(spreadUid)) {
            throw new CrmebException("??????????????????????????????");
        }
        User user = getById(userId);
        if (ObjectUtil.isNull(user)) {
            throw new CrmebException("???????????????");
        }
        if (user.getSpreadUid().equals(spreadUid)) {
            throw new CrmebException("?????????????????????????????????");
        }
        User spreadUser = getById(spreadUid);
        if (ObjectUtil.isNull(spreadUser)) {
            throw new CrmebException("?????????????????????");
        }

        User tempUser = new User();
        tempUser.setUid(userId);
        tempUser.setSpreadUid(spreadUid);
        tempUser.setSpreadTime(DateUtil.nowDateTime());
        return updateById(tempUser);
    }
}
