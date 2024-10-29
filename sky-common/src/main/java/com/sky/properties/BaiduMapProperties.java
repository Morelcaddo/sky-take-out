package com.sky.properties;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "sky.baidumap")
@Data
public class BaiduMapProperties {
    private String address;
    private String ak;
    private String output;
    private String sk;
}
