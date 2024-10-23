package com.sky.mapper;

import com.sky.entity.Orders;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OrdersMapper {
    void insert(Orders orders);

    void update(Orders orders);

    List<Orders> query(Orders order);
}
