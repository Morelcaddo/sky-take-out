package com.sky.service.impl;

import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrdersMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
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

    @Override
    public void export(HttpServletResponse response) {
        //查询数据库获取营业数据
        //获取开始时间和结束时间
        LocalDate begin = LocalDate.now().minusDays(30);
        LocalDate end = LocalDate.now().minusDays(1);

        //获取近30天的营业额数据
        Double turnover = ordersMapper.getTurnover(Orders.COMPLETED,
                LocalDateTime.of(begin, LocalTime.MIN), LocalDateTime.of(end, LocalTime.MAX));

        if (turnover == null) {
            turnover = Double.valueOf("0");
        }

        //获取近30天的有效订单
        OrdersPageQueryDTO condition = new OrdersPageQueryDTO();
        condition.setBeginTime(LocalDateTime.of(begin, LocalTime.MIN));
        condition.setEndTime(LocalDateTime.of(end, LocalTime.MAX));
        condition.setStatus(Orders.COMPLETED);
        Integer ordersCount = ordersMapper.query(condition).size();

        //获取近30天订单完成率
        Integer total = ordersMapper.getSum(LocalDateTime.of(begin, LocalTime.MIN),
                LocalDateTime.of(end, LocalTime.MAX));
        Double orderCompletionRate = (double) 0;
        if (total == null) {
            total = 0;
        }
        if (total != 0) {
            orderCompletionRate = Double.valueOf(ordersCount) / Double.valueOf(total);
        }

        //近30天平均客单价
        Double unitPrice = (double) 0;
        if (ordersCount != 0) {
            unitPrice = turnover / ordersCount;
        }

        //近30天新增用户数
        Long newUsers = userMapper.getUserData(LocalDateTime.of(begin, LocalTime.MIN),
                LocalDateTime.of(end, LocalTime.MAX));

        //通过POI将数据写入Excel文件中
        InputStream in = this.getClass().getClassLoader()
                .getResourceAsStream("template/data-excel-template.xlsx");
        try {
            if (in != null) {
                XSSFWorkbook excel = new XSSFWorkbook(in);

                //填充数据
                //时间
                XSSFSheet sheet = excel.getSheet("Sheet1");
                sheet.getRow(1).getCell(1).setCellValue(
                        "时间：" + begin + "至" + end);

                //概览数据填充
                sheet.getRow(3).getCell(2).setCellValue(turnover);
                sheet.getRow(3).getCell(4).setCellValue(orderCompletionRate);
                sheet.getRow(3).getCell(6).setCellValue(newUsers);
                sheet.getRow(4).getCell(2).setCellValue(ordersCount);
                sheet.getRow(4).getCell(4).setCellValue(unitPrice);

                //明细数据填充
                //遍历时间，通过LocalDate
                for (int i = 0; i < 30; i++) {
                    LocalDate date = begin.plusDays(i);
                    BusinessDataVO data = new BusinessDataVO();
                    //获取新增用户数
                    newUsers = userMapper.getUserData(LocalDateTime.of(date, LocalTime.MIN),
                            LocalDateTime.of(date, LocalTime.MAX));

                    //获取订单完成率

                    //构造查询条件
                    condition.setBeginTime(LocalDateTime.of(date, LocalTime.MIN));
                    condition.setEndTime(LocalDateTime.of(date, LocalTime.MAX));
                    condition.setStatus(Orders.COMPLETED);

                    //统计有效订单个数
                    ordersCount = ordersMapper.query(condition).size();

                    //统计订单总数并计算完成率
                    total = ordersMapper.getSum(LocalDateTime.of(date, LocalTime.MIN),
                            LocalDateTime.of(date, LocalTime.MAX));
                    orderCompletionRate = (double) 0;
                    if (total == null) {
                        total = 0;
                    }
                    if (total != 0) {
                        orderCompletionRate = Double.valueOf(ordersCount) / Double.valueOf(total);
                    }

                    //获取今日营业额
                    turnover = ordersMapper.getTurnover(Orders.COMPLETED,
                            LocalDateTime.of(date, LocalTime.MIN), LocalDateTime.of(date, LocalTime.MAX));

                    if (turnover == null) {
                        turnover = Double.valueOf("0");
                    }

                    //获取平均客单价
                    unitPrice = (double) 0;
                    if (ordersCount != 0) {
                        unitPrice = turnover / ordersCount;
                    }

                    sheet.getRow(7 + i).getCell(1).setCellValue(date.toString());
                    sheet.getRow(7 + i).getCell(2).setCellValue(turnover);
                    sheet.getRow(7 + i).getCell(3).setCellValue(ordersCount);
                    sheet.getRow(7 + i).getCell(4).setCellValue(orderCompletionRate);
                    sheet.getRow(7 + i).getCell(5).setCellValue(unitPrice);
                    sheet.getRow(7 + i).getCell(6).setCellValue(newUsers);


                }


                //通过输出流将文件下载至客户端浏览器
                ServletOutputStream out = response.getOutputStream();
                excel.write(out);

                //关闭资源
                out.close();
                excel.close();
                in.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }
}
