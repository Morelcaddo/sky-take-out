package com.sky.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.sky.constant.MessageConstant;
import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;
import com.sky.exception.LoginFailedException;
import com.sky.mapper.UserMapper;
import com.sky.properties.JwtProperties;
import com.sky.properties.WeChatProperties;
import com.sky.service.UserService;
import com.sky.utils.HttpClientUtil;
import com.sky.utils.JwtUtil;
import com.sky.vo.UserLoginVO;
import org.apache.http.client.HttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private WeChatProperties weChatProperties;
    @Autowired
    private JwtProperties jwtProperties;
    @Autowired
    private UserMapper userMapper;

    @Override
    public UserLoginVO login(UserLoginDTO userLoginDTO) {
        //code2Session接口请求路径
        String url = "https://api.weixin.qq.com/sns/jscode2session";
        //构造请求数据
        Map<String, String> paramMap = new HashMap<>();
        paramMap.put("appid", weChatProperties.getAppid());
        paramMap.put("secret", weChatProperties.getSecret());
        paramMap.put("js_code", userLoginDTO.getCode());
        paramMap.put("grant_type", "authorization_code");
        //向微信接口发送请求
        String result = HttpClientUtil.doGet(url, paramMap);
        //处理返回结果
        JSONObject jsonObject = JSONObject.parseObject(result);
        //获取openId
        String openid = jsonObject.getString("openid");

        //判断openid是否为空，如果为空，则抛出异常，表示登录失败
        if (openid == null) {
            throw new LoginFailedException(MessageConstant.LOGIN_FAILED);
        }

        //判断当前用户是否为新用户
        User user = userMapper.getUserByOpenId(openid);

        //如果没有注册则自动完成注册
        if (user == null) {
            user = User.builder()
                    .openid(openid)
                    .createTime(LocalDateTime.now())
                    .build();
            userMapper.insert(user);
        }

        //为微信用户生成jwt令牌
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        String token = JwtUtil.createJWT(jwtProperties.getUserSecretKey(), jwtProperties.getUserTtl(), claims);


        return UserLoginVO.builder()
                .id(user.getId())
                .openid(openid)
                .token(token)
                .build();
    }
}
