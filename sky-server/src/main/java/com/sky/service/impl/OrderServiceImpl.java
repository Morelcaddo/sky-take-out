package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.AddressBook;
import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import com.sky.entity.ShoppingCart;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.AddressBookMapper;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrdersMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.properties.BaiduMapProperties;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.BaiduMapUtil;
import com.sky.utils.HttpClientUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import com.sky.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

    @Autowired
    private BaiduMapProperties baiduMapProperties;
    @Autowired
    private OrdersMapper ordersMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private WebSocketServer webSocketServer;

    @Override
    @Transactional
    public OrderSubmitVO submit(OrdersSubmitDTO ordersSubmitDTO) {

        log.info(ordersSubmitDTO.toString());
        //处理业务异常（地址薄为空，购物车数据为空）
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if (addressBook == null) {
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }

        Long userId = BaseContext.getCurrentId();
        List<ShoppingCart> carts = shoppingCartMapper.getByUserId(userId);
        if (carts == null || carts.isEmpty()) {
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        //检查配送距离
        checkOfRange(addressBook.getProvinceName() +
                addressBook.getCityName() + addressBook.getDetail());


        //向订单表插入1条数据
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, orders);
        orders.setOrderTime(LocalDateTime.now());
        orders.setPayStatus(Orders.UN_PAID);
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setNumber(String.valueOf(System.currentTimeMillis()));
        orders.setPhone(addressBook.getPhone());
        orders.setConsignee(addressBook.getConsignee());
        orders.setUserId(userId);
        orders.setAddress(addressBook.getProvinceName() +
                addressBook.getCityName() + addressBook.getDistrictName()
                + addressBook.getDetail());

        ordersMapper.insert(orders);

        List<OrderDetail> orderDetails = new ArrayList<>();
        //向订单明细表插入n条数据
        for (ShoppingCart cart : carts) {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart, orderDetail);
            orderDetail.setOrderId(orders.getId());
            orderDetails.add(orderDetail);
        }

        orderDetailMapper.insertBatch(orderDetails);

        //清空购物车的数据
        shoppingCartMapper.deleteByUserId(userId);

        //封装VO返回结果

        return OrderSubmitVO.builder()
                .id(orders.getId())
                .orderTime(orders.getOrderTime())
                .orderNumber(orders.getNumber())
                .orderAmount(orders.getAmount())
                .build();
    }

    @Override
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) {
        OrderPaymentVO orderPaymentVO = new OrderPaymentVO();
        orderPaymentVO.setTimeStamp(String.valueOf(System.currentTimeMillis()));
        Orders orders = new Orders();
        orders.setNumber(ordersPaymentDTO.getOrderNumber());
        orders.setPayStatus(Orders.PAID);
        orders.setCheckoutTime(LocalDateTime.now());
        orders.setPayMethod(ordersPaymentDTO.getPayMethod());
        orders.setStatus(Orders.TO_BE_CONFIRMED);
        ordersMapper.update(orders);

        //通过WebSocket向客户端浏览器推送消息 type orderId Content
        Map data = new HashMap();
        data.put("type", 1);//表示来单提醒,2表示客户催单
        data.put("orderId", orders.getId());
        data.put("content", "订单号：" + ordersPaymentDTO.getOrderNumber());
        String json = JSON.toJSONString(data);
        webSocketServer.sendToAllClient(json);
        return orderPaymentVO;
    }

    @Override
    @Transactional
    public PageResult historyOrders(OrdersPageQueryDTO ordersPageQueryDTO) {
        //获取用户id
        Long userId = BaseContext.getCurrentId();
        ordersPageQueryDTO.setUserId(userId);

        //获取用户订单数据
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        Page<Orders> p = ordersMapper.query(ordersPageQueryDTO);
        List<OrderVO> result = new ArrayList<>();

        List<Orders> ordersList = p.getResult();

        //获取每条订单的订单详情数据
        for (Orders order : ordersList) {
            List<OrderDetail> orderDetails = orderDetailMapper.queryByOrderId(order.getId());
            OrderVO orderVO = new OrderVO();
            BeanUtils.copyProperties(order, orderVO);
            orderVO.setOrderDetailList(orderDetails);
            result.add(orderVO);
        }

        log.info(result.toString());


        return new PageResult(p.getTotal(), result);
    }

    @Override
    @Transactional
    public OrderVO orderDetail(Long id) {
        Orders order = ordersMapper.getById(id);
        OrderVO result = new OrderVO();
        BeanUtils.copyProperties(order, result);
        result.setOrderDetailList(orderDetailMapper.queryByOrderId(id));
        return result;
    }

    @Override
    public void cancel(Long id) {
        Orders orders = ordersMapper.getById(id);
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelTime(LocalDateTime.now());
        orders.setPayStatus(Orders.REFUND);
        log.info(orders.toString());
        ordersMapper.update(orders);
    }

    @Override
    public void repetition(Long id) {
        Long userId = BaseContext.getCurrentId();
        List<OrderDetail> orderDetails = orderDetailMapper.queryByOrderId(id);
        List<ShoppingCart> shoppingCarts = new ArrayList<>();
        for (OrderDetail orderDetail : orderDetails) {
            ShoppingCart cart = new ShoppingCart();
            BeanUtils.copyProperties(orderDetail, cart);
            cart.setUserId(userId);
            cart.setCreateTime(LocalDateTime.now());
            log.info(cart.toString());
            shoppingCarts.add(cart);
        }
        shoppingCartMapper.insertBatch(shoppingCarts);
    }

    @Override
    @Transactional
    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {

        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        Page<Orders> p = ordersMapper.query(ordersPageQueryDTO);

        List<Orders> orders = p.getResult();

        List<OrderVO> result = new ArrayList<>();
        for (Orders order : orders) {
            OrderVO orderVO = new OrderVO();
            BeanUtils.copyProperties(order, orderVO);
            orderVO.setOrderDishes(getOrderDishesStr(order));
            result.add(orderVO);
        }


        return new PageResult(p.getTotal(), result);
    }


    private String getOrderDishesStr(Orders orders) {
        // 查询订单菜品详情信息（订单中的菜品和数量）
        List<OrderDetail> orderDetailList = orderDetailMapper.queryByOrderId(orders.getId());

        // 将每一条订单菜品信息拼接为字符串（格式：宫保鸡丁*3；）
        List<String> orderDishList = orderDetailList.stream().map(x -> x.getName() + "*" + x.getNumber() + ";").collect(Collectors.toList());

        // 将该订单对应的所有菜品信息拼接在一起
        return String.join("", orderDishList);
    }

    @Override
    public OrderStatisticsVO statistics() {
        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        orderStatisticsVO.setConfirmed(ordersMapper.statistics(Orders.CONFIRMED));
        orderStatisticsVO.setToBeConfirmed(ordersMapper.statistics(Orders.TO_BE_CONFIRMED));
        orderStatisticsVO.setDeliveryInProgress(ordersMapper.statistics(Orders.DELIVERY_IN_PROGRESS));
        return orderStatisticsVO;
    }

    @Override
    public void delivery(Long id) {
        Orders orders = new Orders();
        orders.setId(id);
        orders.setStatus(Orders.DELIVERY_IN_PROGRESS);
        ordersMapper.update(orders);
    }

    @Override
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
        ordersConfirmDTO.setStatus(Orders.CONFIRMED);
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersConfirmDTO, orders);
        ordersMapper.update(orders);
    }

    @Override
    public void rejection(OrdersRejectionDTO ordersRejectionDTO) {
        Orders order = new Orders();
        BeanUtils.copyProperties(ordersRejectionDTO, order);
        order.setStatus(Orders.CANCELLED);
        ordersMapper.update(order);
    }

    @Override
    public void complete(Long id) {
        Orders order = new Orders();
        order.setId(id);
        order.setStatus(Orders.COMPLETED);
        order.setDeliveryTime(LocalDateTime.now());
        ordersMapper.update(order);
    }

    @Override
    public void reminder(Long id) {
        Orders order = ordersMapper.getById(id);

        if (order == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        Integer status = order.getStatus();
        if (status == 1 || status == 5 || status == 6 || status == 7) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Map data = new HashMap();
        data.put("type", 2);//1表示来单提醒，2表示客户催单
        data.put("orderId", order.getId());
        data.put("content", "订单号：" + order.getNumber());
        webSocketServer.sendToAllClient(JSON.toJSONString(data));


    }

    @Override
    public void cancelAdmin(OrdersCancelDTO ordersCancelDTO) {
        Orders order = new Orders();
        BeanUtils.copyProperties(ordersCancelDTO, order);
        order.setStatus(Orders.CANCELLED);
        ordersMapper.update(order);
    }


    private void checkOfRange(String address) {
        //百度地图请求接口url
        String url = "https://api.map.baidu.com/geocoding/v3/";

        //构造商家地址坐标的请求数据
        Map<String, String> paramsMap = new LinkedHashMap<>();
        paramsMap.put("address", baiduMapProperties.getAddress());
        paramsMap.put("ak", baiduMapProperties.getAk());
        paramsMap.put("output", baiduMapProperties.getOutput());
        String sn;
        try {
            sn = BaiduMapUtil.getSn(paramsMap, baiduMapProperties.getSk(), "/geocoding/v3/?");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }

        paramsMap.put("sn", sn);

        //向百度地图接口发送请求,获取商家地理位置信息
        String resultMerchant = HttpClientUtil.doGet(url, paramsMap);
        log.info(resultMerchant);

        //处理商家位置信息的返回结果
        JSONObject merchantJSONObject = JSONObject.parseObject(resultMerchant);

        if (!merchantJSONObject.getString("status").equals("0")) {
            throw new OrderBusinessException("商家地址信息解析失败");
        }

        JSONObject merchantLocation = merchantJSONObject.getJSONObject("result")
                .getJSONObject("location");

        String latMerchant = merchantLocation.getString("lat");
        String lngMerchant = merchantLocation.getString("lng");
        String merchantPoint = latMerchant + "," + lngMerchant;


        //修改paramsMap中的地址为用户下单地址
        paramsMap.clear();
        paramsMap.put("address", address);
        paramsMap.put("ak", baiduMapProperties.getAk());
        paramsMap.put("output", baiduMapProperties.getOutput());

        try {
            sn = BaiduMapUtil.getSn(paramsMap, baiduMapProperties.getSk(), "/geocoding/v3/?");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }

        paramsMap.put("sn", sn);

        log.info(paramsMap.toString());

        //向百度地图接口发送请求,获取用户下单地理位置信息
        String resultUser = HttpClientUtil.doGet(url, paramsMap);
        log.info(resultUser);


        //处理用户位置信息返回结果
        JSONObject userJSONObject = JSONObject.parseObject(resultUser);

        if (!userJSONObject.getString("status").equals("0")) {
            throw new OrderBusinessException("用户地址信息解析失败");
        }

        JSONObject userLocation = userJSONObject.getJSONObject("result")
                .getJSONObject("location");

        String latUser = userLocation.getString("lat");
        String lngUser = userLocation.getString("lng");
        String userPoint = latUser + "," + lngUser;

        log.info(userPoint);

        //构造百度地图路劲规划api请求数据
        paramsMap.clear();
        paramsMap.put("ak", baiduMapProperties.getAk());
        paramsMap.put("origin", merchantPoint);
        paramsMap.put("destination", userPoint);
        paramsMap.put("output", baiduMapProperties.getOutput());
        paramsMap.put("steps_info", "0");
        paramsMap.put("timestamp", String.valueOf(System.currentTimeMillis()));
        log.info(paramsMap.toString());


        try {
            sn = BaiduMapUtil.getSn(paramsMap, baiduMapProperties.getSk(), "/direction/v2/driving?");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }


        paramsMap.put("sn", sn);

        url = "https://api.map.baidu.com/direction/v2/driving";
        String resultRouting = HttpClientUtil.doGet(url, paramsMap);

        log.info(resultRouting);

        //处理路径规划接口的返回结果
        JSONObject routingJSONObject = JSON.parseObject(resultRouting);

        if (!routingJSONObject.getString("status").equals("0")) {
            throw new OrderBusinessException("路径规划失败");
        }

        JSONArray jsonArray = routingJSONObject.getJSONObject("result")
                .getJSONArray("routes");
        Integer distance = (Integer) ((JSONObject) jsonArray.get(0)).get("distance");

        if (distance > 5000) {
            //配送距离超过5000米
            throw new OrderBusinessException("超出配送范围");
        }

    }
}
