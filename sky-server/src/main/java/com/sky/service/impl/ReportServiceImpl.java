package com.sky.service.impl;

import com.sky.entity.Orders;
import com.sky.mapper.OrdersMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
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
        List<Long>totalUserList = new ArrayList<>();
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
            if(newUser < 0){
                newUser = 0L;
            }
            log.info("新增用户：{}", newUserNow - newUserBefore);
            newUserList.add(newUser);
            Long totalUser = userMapper.getUserData(LocalDateTime.of(date, LocalTime.MIN),
                    LocalDateTime.of(date, LocalTime.MAX));

            if(totalUser == null){
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
}
