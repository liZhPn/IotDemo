package com.lizhpn.iot.app;

import com.lizhpn.utils.CryptoUtil;

/**
 * 用于计算设备接入物联网平台的 MQTT 连接参数 -- 用户名、密码、用户账号
 */
public class MqttSign {

    // MQTT 连接参数 -- 用户名
    private String username = "";

    // MQTT 连接参数 -- 密码
    private String password = "";

    // MQTT 连接参数 -- 用户账户
    private String clientid = "";

    public String getUsername() { return this.username;}

    public String getPassword() { return this.password;}

    public String getClientid() { return this.clientid;}

    /**
     * 根据设备的产品密钥、设备名称、设备密钥计算出 MQTT 连接参数
     * @param productKey    产品标志
     * @param deviceName    设备标志
     * @param deviceSecret  设备密钥
     */
    public void calculate(String productKey, String deviceName, String deviceSecret) {

        // 不允许存在一个参数为 null 的情况，否则计算失败
        if (productKey == null || deviceName == null || deviceSecret == null) {
            return;
        }

        try {
            // 计算 MQTT 连接参数 -- 用户名
            this.username = deviceName + "&" + productKey;


            // 表示当前时刻
            String timestamp = Long.toString(System.currentTimeMillis());
            // 表示密码明文
            String plainPasswd = "clientId" + productKey + "." + deviceName + "deviceName" +
                    deviceName + "productKey" + productKey + "timestamp" + timestamp;
            // 计算 MQTT 连接参数 -- 密码
            this.password = CryptoUtil.hmacSha256(plainPasswd, deviceSecret);


            // 计算 MQTT 连接参数 -- 用户账户
            this.clientid = productKey + "." + deviceName + "|" + "timestamp=" + timestamp +
                    ",_v=paho-java-1.0.0,securemode=2,signmethod=hmacsha256|";

        }catch (Exception e) {
            e.printStackTrace();
        }
    }
}