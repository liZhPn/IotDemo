package com.lizhpn.iot.config;

import com.lizhpn.utils.ReadProperties;

import java.net.URL;
import java.util.Map;

public class Config {
    private Config() {
    }

    public static String produceKey;
    public static String deviceName;
    public static String deviceSecret;
    public static String accessKey;
    public static String accessSecret;
    public static String consumerGroupId;
    public static String regionId;
    public static String IoTId;

    static {
        Map<String, String> config = ReadProperties.readProperties("setting.properties");
        if (config != null) {
            produceKey = config.get("productKey");
            deviceName = config.get("deviceName");
            deviceSecret = config.get("deviceSecret");
            accessKey = config.get("accessKey");
            accessSecret = config.get("accessSecret");
            consumerGroupId = config.get("consumerGroupId");
            regionId = config.get("regionId");
            IoTId = config.get("IoTId");

        } else System.out.println("错误：未正确读取配置文件.");
    }
}
