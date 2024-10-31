package com.sky.mapper;

import com.sky.annotation.AutoFill;
import com.sky.entity.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

@Mapper
public interface UserMapper {
    @Select("select * from user where openid = #{openId}")
    User getUserByOpenId(String openId);

    void insert(User user);

    @Select("select count(*) from user where create_time <= #{end} and create_time >= #{begin} ")
    Long getUserData(LocalDateTime begin, LocalDateTime end);

}
