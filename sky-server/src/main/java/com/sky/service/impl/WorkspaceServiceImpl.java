package com.sky.service.impl;

import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.Orders;
import com.sky.mapper.DishMapper;
import com.sky.mapper.OrdersMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.WorkspaceService;
import com.sky.vo.BusinessDataVO;
import com.sky.vo.DishOverViewVO;
import com.sky.vo.OrderOverViewVO;
import com.sky.vo.SetmealOverViewVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
public class WorkspaceServiceImpl implements WorkspaceService {
    @Autowired
    private UserMapper userMapper;

    @Autowired
    private OrdersMapper ordersMapper;

    @Autowired
    private SetmealMapper setmealMapper;

    @Autowired
    private DishMapper dishMapper;

    @Override
    public BusinessDataVO businessData() {
        //获取新增用户数
        LocalDate now = LocalDate.now();
        Long newUsers = userMapper.getUserData(LocalDateTime.of(now, LocalTime.MIN),
                LocalDateTime.of(now, LocalTime.MAX));

        //获取订单完成率

        //构造查询条件
        OrdersPageQueryDTO condition = new OrdersPageQueryDTO();
        condition.setBeginTime(LocalDateTime.of(now, LocalTime.MIN));
        condition.setEndTime(LocalDateTime.of(now, LocalTime.MAX));
        condition.setStatus(Orders.COMPLETED);

        //统计有效订单个数
        Integer finish = ordersMapper.query(condition).size();

        //统计订单总数并计算完成率
        Integer total = ordersMapper.getSum(LocalDateTime.of(now, LocalTime.MIN),
                LocalDateTime.of(now, LocalTime.MAX));
        Double orderCompletionRate = (double) 0;
        if (total == null) {
            total = 0;
        }
        if (total != 0) {
            orderCompletionRate = Double.valueOf(finish) / Double.valueOf(total);
        }

        //获取今日营业额
        Double turnover = ordersMapper.getTurnover(Orders.COMPLETED,
                LocalDateTime.of(now, LocalTime.MIN), LocalDateTime.of(now, LocalTime.MAX));

        if (turnover == null) {
            turnover = Double.valueOf("0");
        }

        //获取平均客单价
        Double unitPrice = (double) 0;
        if (finish != 0) {
            unitPrice = turnover / finish;
        }

        return BusinessDataVO.builder()
                .newUsers(Math.toIntExact(newUsers))
                .orderCompletionRate(orderCompletionRate)
                .turnover(turnover)
                .unitPrice(unitPrice)
                .validOrderCount(finish)
                .build();

    }


    @Override
    public SetmealOverViewVO overviewSetmeals() {
        Integer discontinued = setmealMapper.getSumByStatus(0);
        Integer sold = setmealMapper.getSumByStatus(1);

        if (sold == null) {
            sold = 0;
        }

        if (discontinued == null) {
            discontinued = 0;
        }
        return SetmealOverViewVO.builder()
                .discontinued(discontinued)
                .sold(sold)
                .build();

    }

    @Override
    public DishOverViewVO overviewDishes() {
        Integer discontinued = dishMapper.getSumByStatus(0);
        Integer sold = dishMapper.getSumByStatus(1);

        if (sold == null) {
            sold = 0;
        }

        if (discontinued == null) {
            discontinued = 0;
        }

        return DishOverViewVO.builder()
                .discontinued(discontinued)
                .sold(sold)
                .build();
    }

    @Override
    public OrderOverViewVO overviewOrders() {
        Integer allOrders = ordersMapper.getAll();
        if (allOrders == null) {
            allOrders = 0;
        }

        Integer cancelledOrders = ordersMapper.statistics(Orders.CANCELLED);
        if (cancelledOrders == null) {
            cancelledOrders = 0;
        }

        Integer completedOrders = ordersMapper.statistics(Orders.COMPLETED);
        if (completedOrders == null) {
            completedOrders = 0;
        }

        Integer deliveredOrders = ordersMapper.statistics(Orders.DELIVERY_IN_PROGRESS);
        if (deliveredOrders == null) {
            deliveredOrders = 0;
        }

        Integer waitingOrders = ordersMapper.statistics(Orders.TO_BE_CONFIRMED);
        if (waitingOrders == null) {
            waitingOrders = 0;
        }

        return OrderOverViewVO.builder()
                .allOrders(allOrders)
                .cancelledOrders(cancelledOrders)
                .completedOrders(completedOrders)
                .deliveredOrders(deliveredOrders)
                .waitingOrders(waitingOrders)
                .build();

    }
}
