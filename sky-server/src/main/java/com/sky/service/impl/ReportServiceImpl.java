package com.sky.service.impl;

import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrdersMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class ReportServiceImpl implements ReportService {
    @Autowired
    private OrdersMapper ordersMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Override
    public TurnoverReportVO turnoverStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dates = new ArrayList<>();
        for (LocalDate date = begin; date.isBefore(end); date = date.plusDays(1)) {
            dates.add(date);
        }
        dates.add(end);

        List<Double> turnovers = new ArrayList<>();

        for (LocalDate date : dates) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            log.info("开始时间：{}，结束时间：{}", beginTime, endTime);
            Double turnover = ordersMapper.getTurnover(Orders.COMPLETED, beginTime, endTime);

            if (turnover == null) {
                turnover = Double.valueOf("0");
            }


            turnovers.add(turnover);
        }

        return TurnoverReportVO.builder()
                .dateList(StringUtils.join(dates, ","))
                .turnoverList(StringUtils.join(turnovers, ","))
                .build();

    }

    @Override
    public UserReportVO userStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dates = new ArrayList<>();
        for (LocalDate date = begin; date.isBefore(end); date = date.plusDays(1)) {
            dates.add(date);
        }
        dates.add(end);

        List<Long> newUserList = new ArrayList<>();
        List<Long> totalUserList = new ArrayList<>();
        for (LocalDate date : dates) {
            LocalDate before = date.minusDays(1);

            Long newUserNow = userMapper.getUserData(LocalDateTime.of(date, LocalTime.MIN),
                    LocalDateTime.of(date, LocalTime.MAX));

            Long newUserBefore = userMapper.getUserData(LocalDateTime.of(before, LocalTime.MIN),
                    LocalDateTime.of(before, LocalTime.MAX));


            if (newUserNow == null) {
                newUserNow = 0L;
            }
            if (newUserBefore == null) {
                newUserBefore = 0L;
            }

            Long newUser = newUserNow - newUserBefore;
            if (newUser < 0) {
                newUser = 0L;
            }
            log.info("新增用户：{}", newUserNow - newUserBefore);
            newUserList.add(newUser);
            Long totalUser = userMapper.getUserData(LocalDateTime.of(date, LocalTime.MIN),
                    LocalDateTime.of(date, LocalTime.MAX));

            if (totalUser == null) {
                totalUser = 0L;
            }

            log.info("总用户：{}", totalUser);
            totalUserList.add(totalUser);
        }


        return UserReportVO.builder()
                .dateList(StringUtils.join(dates, ","))
                .newUserList(StringUtils.join(newUserList, ","))
                .totalUserList(StringUtils.join(totalUserList, ","))
                .build();
    }

    @Override
    public OrderReportVO orderStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dates = new ArrayList<>();
        for (LocalDate date = begin; date.isBefore(end); date = date.plusDays(1)) {
            dates.add(date);
        }
        dates.add(end);

        //构造查询条件
        OrdersPageQueryDTO condition = new OrdersPageQueryDTO();
        condition.setBeginTime(LocalDateTime.of(begin, LocalTime.MIN));
        condition.setEndTime(LocalDateTime.of(end, LocalTime.MAX));
        condition.setStatus(Orders.COMPLETED);

        //统计有效订单个数
        Integer finish = ordersMapper.query(condition).size();

        //统计订单总数并计算完成率
        Integer total = ordersMapper.getSum(LocalDateTime.of(begin, LocalTime.MIN),
                LocalDateTime.of(end, LocalTime.MAX));

        if (total == null) {
            total = 0;
        }

        Double orderCompletionRate = (double) 0;
        if (total != 0) {
            orderCompletionRate = Double.valueOf(finish) / Double.valueOf(total);
        }

        List<Integer> validOrderCountList = new ArrayList<>();
        List<Integer> orderCountList = new ArrayList<>();
        for (LocalDate date : dates) {
            condition.setBeginTime(LocalDateTime.of(date, LocalTime.MIN));
            condition.setEndTime(LocalDateTime.of(date, LocalTime.MAX));
            validOrderCountList.add(ordersMapper.query(condition).size());
            condition.setStatus(null);
            orderCountList.add(ordersMapper.query(condition).size());
        }

        return OrderReportVO.builder()
                .dateList(StringUtils.join(dates, ","))
                .orderCountList(StringUtils.join(validOrderCountList, ","))
                .validOrderCountList(StringUtils.join(orderCountList, ","))
                .totalOrderCount(total)
                .validOrderCount(finish)
                .orderCompletionRate(orderCompletionRate)
                .build();
    }

    @Override
    public SalesTop10ReportVO top10(LocalDate begin, LocalDate end) {
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);
        List<OrderDetail> result = orderDetailMapper.getTop10(beginTime, endTime);
        List<String> nameList = new ArrayList<>();
        List<Integer> numberList = new ArrayList<>();
        for (OrderDetail orderDetail : result) {
            nameList.add(orderDetail.getName());
            numberList.add(orderDetail.getNumber());
        }
        return SalesTop10ReportVO.builder()
                .nameList(StringUtils.join(nameList, ","))
                .numberList(StringUtils.join(numberList, ","))
                .build();

    }
}
