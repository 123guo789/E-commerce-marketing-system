package com.zbkj.crmeb.user.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.common.CommonPage;
import com.common.PageParamRequest;
import com.constants.Constants;
import com.exception.CrmebException;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.utils.DateUtil;
import com.utils.vo.dateLimitUtilVo;
import com.zbkj.crmeb.finance.request.FundsMonitorSearchRequest;
import com.zbkj.crmeb.front.response.UserSpreadCommissionResponse;
import com.zbkj.crmeb.store.request.StoreOrderRefundRequest;
import com.zbkj.crmeb.user.dao.UserBillDao;
import com.zbkj.crmeb.user.model.User;
import com.zbkj.crmeb.user.model.UserBill;
import com.zbkj.crmeb.user.request.UserBillDetailListRequest;
import com.zbkj.crmeb.user.response.BillType;
import com.zbkj.crmeb.user.response.UserBillResponse;
import com.zbkj.crmeb.user.service.UserBillService;
import com.zbkj.crmeb.user.service.UserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * UserBillServiceImpl 接口实现
 */
@Service
public class UserBillServiceImpl extends ServiceImpl<UserBillDao, UserBill> implements UserBillService {

    @Resource
    private UserBillDao dao;

    @Autowired
    private UserService userService;

    private Page<UserBill> userBillPage;


    /**
    * 列表
    * @param request 请求参数
    * @param pageParamRequest 分页类参数
    * @return List<UserBill>
    */
    @Override
    public List<UserBill> getList(FundsMonitorSearchRequest request, PageParamRequest pageParamRequest) {
        userBillPage = PageHelper.startPage(pageParamRequest.getPage(), pageParamRequest.getLimit());
        QueryWrapper<UserBill> queryWrapper = new QueryWrapper<>();
        getMonthSql(request, queryWrapper);

        //排序
        if(request.getSort() == null){
            queryWrapper.orderByDesc("create_time");
        }else{
            if(request.getSort().equals("asc")){
                queryWrapper.orderByAsc("number");
            }else{
                queryWrapper.orderByDesc("number");
            }
        }

        // 查询类型
        if(StringUtils.isNotBlank(request.getCategory())){
            queryWrapper.eq("category", request.getCategory());
        }
        if (ObjectUtil.isNotNull(request.getPm())) {
            queryWrapper.eq("pm", request.getPm());
        }

        return dao.selectList(queryWrapper);
    }

    /**
     * 列表
     * @param request 请求参数
     * @param monthList List<String> 分页类参数
     * @return List<UserBill>
     */
    @Override
    public Map<String, Integer> getCountListByMonth(FundsMonitorSearchRequest request, List<String> monthList) {

        HashMap<String, Integer> map = new HashMap<>();
        QueryWrapper<UserBill> queryWrapper = new QueryWrapper<>();
        getMonthSql(request, queryWrapper);

        queryWrapper.select("count(id) as uid, create_time");
        queryWrapper.apply("left(create_time, 7) in (" + StringUtils.join(monthList, ',') + ")");
        queryWrapper.groupBy("left(create_time, 7)");
        List<UserBill> userBillList = dao.selectList(queryWrapper);

        if(userBillList.size() < 1){
            return map;
        }

        for (UserBill userBill : userBillList) {
            map.put(DateUtil.dateToStr(userBill.getCreateTime(), Constants.DATE_FORMAT_MONTH), userBill.getUid());
        }
        return map;
    }

    /**
     * 佣金排行榜
     * @param type  String 时间范围
     * @param pageParamRequest PageParamRequest 分页
     * @return List<LoginResponse>
     */
    @Override
    public List<UserBill> getTopBrokerageListByDate(String type, PageParamRequest pageParamRequest) {
        QueryWrapper<UserBill> queryWrapper = new QueryWrapper<>();

        dateLimitUtilVo dateLimit = DateUtil.getDateLimit(type);
        queryWrapper.select("uid", "sum(IF((pm=0), -number, number))AS number").eq("status", true);
        if(!StringUtils.isBlank(dateLimit.getStartTime())){
            queryWrapper.between("create_time", dateLimit.getStartTime(), dateLimit.getEndTime());
        }
        queryWrapper.groupBy("uid").orderByDesc("number");
        return dao.selectList(queryWrapper);
    }

    private void getMonthSql(FundsMonitorSearchRequest request, QueryWrapper<UserBill> queryWrapper){
        queryWrapper.gt("status", 0); // -1无效
        if(!StringUtils.isBlank(request.getKeywords())){
            queryWrapper.and(i -> i.
                    or().eq("id", request.getKeywords()).   //用户账单id
                    or().eq("uid", request.getKeywords()). //用户uid
                    or().eq("link_id", request.getKeywords()). //关联id
                    or().like("title", request.getKeywords()) //账单标题
            );
        }

        //时间范围
        if(StringUtils.isNotBlank(request.getDateLimit())){
            dateLimitUtilVo dateLimit = DateUtil.getDateLimit(request.getDateLimit());
            //判断时间
            int compareDateResult = DateUtil.compareDate(dateLimit.getEndTime(), dateLimit.getStartTime(), Constants.DATE_FORMAT);
            if(compareDateResult == -1){
                throw new CrmebException("开始时间不能大于结束时间！");
            }

            queryWrapper.between("create_time", dateLimit.getStartTime(), dateLimit.getEndTime());

            //资金范围
            if(request.getMax() != null && request.getMin() != null){
                //判断时间
                if(request.getMax().compareTo(request.getMin()) < 0){
                    throw new CrmebException("最大金额不能小于最小金额！");
                }
                queryWrapper.between("number", request.getMin(), request.getMax());
            }
        }


        //关联id
        if(StringUtils.isNotBlank(request.getLinkId())){
            if(request.getLinkId().equals("gt")){
                queryWrapper.ne("link_id", 0);
            }else{
                queryWrapper.eq("link_id", request.getLinkId());
            }
        }

        //用户id集合
        if(null != request.getUserIdList() && request.getUserIdList().size() > 0){
            queryWrapper.in("uid", request.getUserIdList());
        } else if (ObjectUtil.isNotNull(request.getUid())) {
            queryWrapper.eq("uid", request.getUid());
        }



        if(StringUtils.isNotBlank(request.getCategory())){
            queryWrapper.eq("category", request.getCategory());
        }

        if(StringUtils.isNotBlank(request.getType())){
            queryWrapper.eq("type", request.getType());
        }
    }

    /**
     * 列表
     * @param request 请求参数
     * @param pageParamRequest 分页类参数
     * @return List<UserBill>
     */
    @Override
    public PageInfo<UserBillResponse> getListAdmin(FundsMonitorSearchRequest request, PageParamRequest pageParamRequest) {
//        List<UserBill> userBillList = getList(request, pageParamRequest);
//        if(userBillList.size() < 1){
//            return new PageInfo<>();
//        }
//
//        List<UserBillResponse> responses = new ArrayList<>();
//
//        //用户信息
//        List<Integer> userIdList = userBillList.stream().map(UserBill::getUid).distinct().collect(Collectors.toList());
//        HashMap<Integer, User> mapListInUid = userService.getMapListInUid(userIdList);
//
//        for (UserBill userBill : userBillList) {
//            UserBillResponse userBillResponse = new UserBillResponse();
//            BeanUtils.copyProperties(userBill, userBillResponse);
//            userBillResponse.setNickName(mapListInUid.get(userBill.getUid()).getNickname());
//            responses.add(userBillResponse);
//        }

//        return CommonPage.copyPageInfo(userBillPage, responses);
        userBillPage = PageHelper.startPage(pageParamRequest.getPage(), pageParamRequest.getLimit());
        Map<String, Object> map = new HashMap<>();
        if (StrUtil.isNotBlank(request.getKeywords())) {
            map.put("keywords", "%"+request.getKeywords()+"%");
        }
        if (StrUtil.isNotBlank(request.getCategory())) {
            map.put("category", request.getCategory());
        }
        //时间范围
        if(StrUtil.isNotBlank(request.getDateLimit())){
            dateLimitUtilVo dateLimit = DateUtil.getDateLimit(request.getDateLimit());
            //判断时间
            int compareDateResult = DateUtil.compareDate(dateLimit.getEndTime(), dateLimit.getStartTime(), Constants.DATE_FORMAT);
            if(compareDateResult == -1){
                throw new CrmebException("开始时间不能大于结束时间！");
            }

            map.put("startTime", dateLimit.getStartTime());
            map.put("endTime", dateLimit.getEndTime());
        }

        List<UserBillResponse> responses = dao.getListAdminAndIntegeal(map);
        return CommonPage.copyPageInfo(userBillPage, responses);
    }

    /**
     * 新增/消耗 总数
     * @param pm Integer 0 = 支出 1 = 获得
     * @param userId Integer 用户uid
     * @param category String 类型
     * @param date String 时间范围
     * @param type String 小类型
     * @return UserBill
     */
    @Override
    public Integer getSumInteger(Integer pm, Integer userId, String category, String date, String type) {
        QueryWrapper<UserBill> queryWrapper = new QueryWrapper<>();


        queryWrapper.select("sum(number) as number").
                eq("category", category).
                eq("uid", userId).
                eq("status", 1);
        if(null != pm){
            queryWrapper.eq("pm", pm);
        }
        if(null != type){
            queryWrapper.eq("type", type);
        }
        if(null != date){
            dateLimitUtilVo dateLimit = DateUtil.getDateLimit(date);
            queryWrapper.between("create_time", dateLimit.getStartTime(), dateLimit.getEndTime());
        }
        UserBill userBill = dao.selectOne(queryWrapper);
        if(null == userBill || null == userBill.getNumber()){
            return 0;
        }
        return userBill.getNumber().intValue();
    }

    /**
     * 新增/消耗  总金额
     * @param pm Integer 0 = 支出 1 = 获得
     * @param userId Integer 用户uid
     * @param category String 类型
     * @param date String 时间范围
     * @param type String 小类型
     * @return UserBill
     */
    @Override
    public BigDecimal getSumBigDecimal(Integer pm, Integer userId, String category, String date, String type) {
        QueryWrapper<UserBill> queryWrapper = new QueryWrapper<>();


        queryWrapper.select("sum(number) as number").
                eq("category", category).
                eq("status", 1);
        if (ObjectUtil.isNotNull(userId)) {
            queryWrapper.eq("uid", userId);
        }
        if(null != pm){
            queryWrapper.eq("pm", pm);
        }
        if(null != type){
            queryWrapper.eq("type", type);
        }
        if(null != date){
            dateLimitUtilVo dateLimit = DateUtil.getDateLimit(date);
            queryWrapper.between("create_time", dateLimit.getStartTime(), dateLimit.getEndTime());
        }
        UserBill userBill = dao.selectOne(queryWrapper);
        if(null == userBill || null == userBill.getNumber()){
            return BigDecimal.ZERO;
        }
        return userBill.getNumber();
    }

    /**
     * 按照月份分组, 余额
     * @return CommonPage<UserBill>
     */
    @Override
    public PageInfo<UserSpreadCommissionResponse> getListGroupByMonth(Integer userId, List<String> typeList, PageParamRequest pageParamRequest, String category) {
        Page<UserBill> userBillPage = PageHelper.startPage(pageParamRequest.getPage(), pageParamRequest.getLimit());
        ArrayList<UserSpreadCommissionResponse> userSpreadCommissionResponseList = new ArrayList<>();

        QueryWrapper<UserBill> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("uid", userId).eq("status", 1).eq("category", category);
        if(CollUtil.isNotEmpty(typeList)){
            queryWrapper.in("type", typeList);
        }

        queryWrapper.groupBy("left(create_time, 7)");
        queryWrapper.orderByDesc("left(create_time, 7)");
        List<UserBill> list = dao.selectList(queryWrapper);
        if(list.size() < 1){
            return new PageInfo<>();
        }

        for (UserBill userBill : list) {
            String date = DateUtil.dateToStr(userBill.getCreateTime(), Constants.DATE_FORMAT_MONTH);
            userSpreadCommissionResponseList.add(new UserSpreadCommissionResponse(date, getListByMonth(userId, typeList, date, category)));
        }
       return CommonPage.copyPageInfo(userBillPage, userSpreadCommissionResponseList);
    }

    /**
     * 保存退款日志
     * @return boolean
     */
    @Override
    public boolean saveRefundBill(StoreOrderRefundRequest request, User user) {
        try{
            UserBill userBill = new UserBill();
            userBill.setTitle("商品退款");
            userBill.setUid(user.getUid());
            userBill.setCategory(Constants.USER_BILL_CATEGORY_MONEY);
            userBill.setType(Constants.USER_BILL_TYPE_PAY_PRODUCT_REFUND);
            userBill.setNumber(request.getAmount());
            userBill.setLinkId(request.getOrderId().toString());
            userBill.setBalance(user.getNowMoney().add(request.getAmount()));
            userBill.setMark("订单退款到余额" + request.getAmount() + "元");
            return save(userBill);
        }catch (Exception e){
            throw new CrmebException(e.getMessage());
        }
    }

    /**
     * 反还佣金日志
     */
    @Override
    public void saveRefundBrokeragePriceBill(StoreOrderRefundRequest request, User user) {
        try{
            UserBill userBill = new UserBill();
            userBill.setTitle("退款退佣金");
            userBill.setUid(user.getUid());
            userBill.setCategory(Constants.USER_BILL_CATEGORY_MONEY);
            userBill.setType(Constants.USER_BILL_TYPE_BROKERAGE);
            userBill.setNumber(request.getAmount());
            userBill.setLinkId(request.getOrderId().toString());
            userBill.setBalance(user.getNowMoney().subtract(request.getAmount()));
            userBill.setPm(0);
            userBill.setMark("订单退款扣除佣金" + request.getAmount() + "元");
            save(userBill);
        }catch (Exception e){
            throw new CrmebException(e.getMessage());
        }
    }

    /**
     * 反还积分日志
     */
    @Override
    public void saveRefundIntegralBill(StoreOrderRefundRequest request, User user) {
        try{
            UserBill userBill = new UserBill();
            userBill.setTitle("退款扣除积分");
            userBill.setUid(user.getUid());
            userBill.setCategory(Constants.USER_BILL_CATEGORY_INTEGRAL);
            userBill.setType(Constants.USER_BILL_TYPE_GAIN);
            userBill.setNumber(request.getAmount());
            userBill.setLinkId(request.getOrderId().toString());
            userBill.setBalance(user.getNowMoney().subtract(request.getAmount()));
            userBill.setPm(0);
            userBill.setMark("订单退款扣除积分" + request.getAmount() + "积分");
            save(userBill);
        }catch (Exception e){
            throw new CrmebException(e.getMessage());
        }
    }

    /**
     * 根据用户id获取对应的佣金数据 分销using
     * @param userId 用户id
     * @return 佣金数据
     */
    @Override
    public BigDecimal getDataByUserId(Integer userId) {
        QueryWrapper<UserBill> qw = new QueryWrapper<>();
        qw.ge("status", 1);
        qw.eq("type", "brokerage");
        qw.eq("pm", 1);
        qw.eq("uid", userId);
        qw.select("sum(number) as number");
        qw.groupBy("uid");
        BigDecimal number = BigDecimal.valueOf(0);
        UserBill ub = dao.selectOne(qw);
        if(null != ub){
            number = ub.getNumber();
        }
        return number;
    }

    /**
     * 通过订单获取
     * @param id 订单id
     * @param userId 用户id
     * @param pm 类型
     * @return
     */
    @Override
    public BigDecimal getIntegerByOrder(Integer id, Integer userId, int pm) {
        UserBill userBill = new UserBill();
        userBill.setCategory(Constants.USER_BILL_CATEGORY_INTEGRAL);
        userBill.setType(Constants.USER_BILL_TYPE_ORDER);
        userBill.setLinkId(id.toString());
        userBill.setUid(userId);
        userBill.setPm(pm);
        return getIntegerByEntity(userBill);
    }

    /**
     * 获取资金操作类型
     *
     * @return 操作类型集合，从数据库group by(type)查询获取
     */
    @Override
    public List<UserBill> getBillGroupType() {
        LambdaQueryWrapper<UserBill> lqw = Wrappers.lambdaQuery();
        lqw.groupBy(UserBill::getType);
        return dao.selectList(lqw);
    }

    /**
     * 返回资金操作类型 仅仅转换数据用
     *
     * @return 操作类型
     */
    @Override
    public List<BillType> getBillType() {
        List<BillType> responses = new ArrayList<>();
        List<UserBill> billGroupType = getBillGroupType();
        if(null != billGroupType && billGroupType.size() > 0){
            billGroupType.stream().map(e->{
                BillType b = new BillType(e.getTitle(),e.getCategory());
                responses.add(b);
                return e;
            }).collect(Collectors.toList());
        }
        return responses;
    }

    /**
     * 获取佣金总额
     *
     * @return 佣金总额
     */
    @Override
    public BigDecimal getSumBrokerage() {
        LambdaQueryWrapper<UserBill> lqw = Wrappers.lambdaQuery();
        lqw.eq(UserBill::getType,"brokerage")
                .eq(UserBill::getCategory,"now_money")
                .eq(UserBill::getStatus,1)
                .eq(UserBill::getPm,0)
                .eq(UserBill::getUid,0);
        List<UserBill> userBills = dao.selectList(lqw);
        double sum = 0;
        if(null != userBills || userBills.size() >0)
            sum = userBills.stream().mapToDouble(e->e.getNumber().doubleValue()).sum();
        return BigDecimal.valueOf(sum);
    }

    /**
     * 根据基本条件查询
     *
     * @param bill 基本参数
     * @return 查询结果
     */
    @Override
    public List<UserBill> getByEntity(UserBill bill) {
        LambdaQueryWrapper<UserBill> lqw = Wrappers.lambdaQuery();
        lqw.setEntity(bill);
        return dao.selectList(lqw);
    }

    /**
     * Base serch
     *
     * @param request          查询参数
     * @param pageParamRequest 分页参数
     * @return 查询结果
     */
    @Override
    public PageInfo<UserBillResponse> getByBaseSearch(Integer userId, UserBillDetailListRequest request, PageParamRequest pageParamRequest) {
        Page<UserBill> startPage = PageHelper.startPage(pageParamRequest.getPage(), pageParamRequest.getLimit());
        LambdaQueryWrapper<UserBill> lqw = Wrappers.lambdaQuery();
        if(null!= userId && userId > 0) lqw.eq(UserBill::getUid, userId);
        if(StringUtils.isNotBlank(request.getKeywords())){
            lqw.like(UserBill::getLinkId, "%"+request.getKeywords()+"%");
        }
        if(StringUtils.isNotBlank(request.getDateLimit())){
            dateLimitUtilVo dateLimit = DateUtil.getDateLimit(request.getDateLimit());
            lqw.between(UserBill::getCreateTime,dateLimit.getStartTime(),dateLimit.getEndTime());
        }
//        lqw.eq(UserBill::getCategory, Constants.USER_BILL_CATEGORY_MONEY);
        lqw.eq(UserBill::getCategory, Constants.USER_BILL_CATEGORY_BROKERAGE_PRICE);
        lqw.eq(UserBill::getType, Constants.USER_BILL_TYPE_BROKERAGE);
        lqw.orderByDesc(UserBill::getCreateTime).orderByDesc(UserBill::getId);
        List<UserBill> userBillsResults = dao.selectList(lqw);
        List<UserBillResponse> userBillResponseResults = new ArrayList<>();
        userBillsResults.stream().map(e->{
            UserBillResponse ub = new UserBillResponse();
            BeanUtils.copyProperties(e,ub);
            ub.setNickName(userService.getById(e.getUid()).getNickname());
            userBillResponseResults.add(ub);
            return e;
        }).collect(Collectors.toList());
        return CommonPage.copyPageInfo(startPage, userBillResponseResults);
    }

    /**
     * 查询搜索明细类型参数
     *
     * @return 明细类型集合
     */
    @Override
    public List<UserBill> getSearchOption() {

//        LambdaQueryWrapper<UserBill> lqw = Wrappers.lambdaQuery();
//        lqw.select("DISTINCT title, type");
//        lqw.notIn(UserBill::getType,"gain", "system_sub", "deduction", "sign");
//        lqw.notIn(UserBill::getCategory,"exp", "integral");
//        lqw.groupBy(UserBill::getType);
//        List<UserBill> userBills = dao.selectList(lqw);
//        List<UserBill> result = new ArrayList<>();
//        for (UserBill userBill : userBills) {
//            List<UserBill> existUserBills = result.stream().filter(e ->
//                    e.getTitle().equals(userBill.getTitle())).collect(Collectors.toList());
//            if(existUserBills.size() == 0) result.add(userBill);
//        }
//        return result;
        QueryWrapper<UserBill> qw = new QueryWrapper<>();
        qw.select("DISTINCT title, type");
        qw.notIn("type","gain", "system_sub", "deduction", "sign");
        qw.notIn("category","exp", "integral");
        return dao.selectList(qw);
    }

    /////////////////////////////////////////////////////////////////////// 自定义方法

    private BigDecimal getIntegerByEntity(UserBill userBill) {
        LambdaQueryWrapper<UserBill> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.setEntity(userBill);
        List<UserBill> userBillList = dao.selectList(lambdaQueryWrapper);
        if(null == userBillList || userBillList.size() < 1){
            return BigDecimal.ZERO;
        }

        return userBillList.stream().map(UserBill::getNumber).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 按照月份获取数据
     * @return List<UserBill>
     */
    private List<UserBill> getListByMonth(Integer userId, List<String> typeList, String month, String category) {
        QueryWrapper<UserBill> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("pm,title,number,create_time").eq("uid", userId). eq("status", 1).eq("left(create_time, 7)", month).eq("category", category);
        if(CollUtil.isNotEmpty(typeList)){
            queryWrapper.in("type", typeList);
        }

        queryWrapper.orderByDesc("create_time");
        return dao.selectList(queryWrapper);
    }

    private ArrayList<String> getTypeList(int type){
        //0=全部,1=消费,2=充值,3=返佣,4=提现
        ArrayList<String> typeList = new ArrayList<>();
        switch (type){
            case 1:
                typeList.add(Constants.USER_BILL_TYPE_RECHARGE);
                typeList.add(Constants.USER_BILL_TYPE_BROKERAGE);
                typeList.add(Constants.USER_BILL_TYPE_PAY_MONEY);
                typeList.add(Constants.USER_BILL_TYPE_SYSTEM_ADD);
                typeList.add(Constants.USER_BILL_TYPE_PAY_PRODUCT_REFUND);
                typeList.add(Constants.USER_BILL_TYPE_SYSTEM_SUB);
                break;
            case 2:
                typeList.add(Constants.USER_BILL_TYPE_PAY_MONEY);
                break;
            case 3:
                typeList.add(Constants.USER_BILL_TYPE_RECHARGE);
                typeList.add(Constants.USER_BILL_TYPE_SYSTEM_ADD);
                break;
            case 4:
                typeList.add(Constants.USER_BILL_TYPE_EXTRACT);
                typeList.add(Constants.USER_BILL_TYPE_RECHARGE);
                break;
            default:
                break;
        }
        return typeList;
    }

}

