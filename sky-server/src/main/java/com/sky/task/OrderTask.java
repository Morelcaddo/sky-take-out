package com.sky.task;

import com.sky.entity.Orders;
import com.sky.mapper.OrdersMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Slf4j
@Component
public class OrderTask {
    @Autowired
    private OrdersMapper ordersMapper;

    /*
        处理超时订单的方法
     */
    @Scheduled(cron = "0 * * * * ? ")//每分钟触发一次
    public void processTimeoutOrder() {
        log.info("定时处理超时订单：{}", new Date());
        //select * from orders where status = ? and order_time < ?
        List<Orders> list = ordersMapper.getByStatusAdnOrderTimeLT(Orders.PENDING_PAYMENT,
                LocalDateTime.now().plusMinutes(-15));

        if (list != null && !list.isEmpty()) {
            for (Orders orders : list) {
                orders.setStatus(Orders.CANCELLED);
                orders.setCancelReason("订单超时，自动取消");
                orders.setCancelTime(LocalDateTime.now());
                ordersMapper.update(orders);
            }
        }

    }

    /*
    处理一直在派送中的订单
     */
    @Scheduled(cron = "0 0 3 * * ?")//每天凌晨三点触发一次
    public void processDeliveryOrder() {
        log.info("定时处理处于派送中的订单:{}", new Date());

        //select * from orders where status = ? and order_time < ?
        List<Orders> list = ordersMapper.getByStatusAdnOrderTimeLT(Orders.DELIVERY_IN_PROGRESS,
                LocalDateTime.now().plusMinutes(-180));

        if (list != null && !list.isEmpty()) {
            for (Orders orders : list) {
                orders.setStatus(Orders.COMPLETED);
                ordersMapper.update(orders);
            }
        }
    }
}
