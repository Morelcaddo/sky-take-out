package com.sky.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;
import java.util.Map.Entry;

@Slf4j
@Component
public class BaiduMapUtil {

    public static String getSn(Map<String, String> paramsMap, String sk, String urlPrefix) throws UnsupportedEncodingException {

        // 计算sn跟参数对出现顺序有关，get请求请使用LinkedHashMap保存<key,value>，
        // 该方法根据key的插入顺序排序；post请使用TreeMap保存<key,value>，
        // 该方法会自动将key按照字母a-z顺序排序。
        // 所以get请求可自定义参数顺序（sn参数必须在最后）发送请求，
        // 但是post请求必须按照字母a-z顺序填充body（sn参数必须在最后）。
        // 以get请求为例：https://api.map.baidu.com/geocoder/v2/?address=
        // 百度大厦&output=json&ak=yourak，
        // paramsMap中先放入address，再放output，然后放ak，放入顺序必须跟get请求中对应参数的出现顺序保持一致。


        // 调用下面的toQueryString方法，对LinkedHashMap内所有value作utf8编码，
        // 拼接返回结果address=%E7%99%BE%E5%BA%A6%E5%A4%A7%E5%8E%A6&output=json&ak=yourak
        log.info(paramsMap.toString());
        String paramsStr = toQueryString(paramsMap);

        // 对paramsStr前面拼接上/geocoder/v2/?，
        // 后面直接拼接yoursk得到/geocoder/v2/?
        // address=%E7%99%BE%E5%BA%A6%E5%A4%A7%E5%8E%A6&output=json&ak=yourakyoursk
        String wholeStr = urlPrefix + paramsStr + sk;
        log.info(wholeStr);

        // 对上面wholeStr再作utf8编码
        String tempStr = URLEncoder.encode(wholeStr, "UTF-8");
        log.info(tempStr);
        // 调用下面的MD5方法得到最后的sn签名
        return MD5(tempStr);
    }


    // 对Map内所有value作utf8编码，拼接返回结果
    public static String toQueryString(Map<?, ?> data) throws UnsupportedEncodingException {
        StringBuilder queryString = new StringBuilder();
        for (Entry<?, ?> pair : data.entrySet()) {
            queryString.append(pair.getKey()).append("=");
            queryString.append(URLEncoder.encode((String) pair.getValue(),
                    "UTF-8")).append("&");
        }
        if (queryString.length() > 0) {
            queryString.deleteCharAt(queryString.length() - 1);
        }
        return queryString.toString();
    }

    // 来自stackoverflow的MD5计算方法，调用了MessageDigest库函数，并把byte数组结果转换成16进制
    public static String MD5(String md5) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest
                    .getInstance("MD5");
            byte[] array = md.digest(md5.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : array) {
                sb.append(Integer.toHexString((b & 0xFF) | 0x100), 1, 3);
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }
}
