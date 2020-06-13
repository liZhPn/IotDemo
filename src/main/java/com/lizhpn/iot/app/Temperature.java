package com.lizhpn.iot.app;

import com.alibaba.fastjson.JSONObject;
import com.lizhpn.iot.app.listener.MqttMessageListener;
import com.lizhpn.iot.config.Config;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.nio.charset.StandardCharsets;

public class Temperature {
    // 产品密钥
    private String productKey;

    // 设备名称
    private String deviceName;

    // 设备密钥
    private String deviceSecret;

    // 最高报警温度
    private int maxTemperature = 35;

    // 最低报警温度
    private int minTemperature = -10;

    // 当前设备的温度
    private Integer temperature;

    // 当前设备影子版本号
    private long shadowVersion = -1;

    // 当前设备与云端的连接标识 -- true 表示已连接； false 表示已断开
    private boolean online;

    // Paho Mqtt 客户端
    private MqttClient sampleClient;

    /**
     * 传入设备证书
     *
     * @param productKey   产品标识
     * @param deviceName   设备名称
     * @param deviceSecret 设备密钥
     */
    Temperature(String productKey, String deviceName, String deviceSecret) {
        this(productKey,deviceName,deviceSecret,false);
    }

    /**
     * 传入设备证书
     *
     * @param productKey   产品标识
     * @param deviceName   设备名称
     * @param deviceSecret 设备密钥
     * @param connect      是否连接物联网平台
     */
    Temperature(String productKey, String deviceName, String deviceSecret, boolean connect) {
        this.productKey = productKey;
        this.deviceName = deviceName;
        this.deviceSecret = deviceSecret;

        System.out.println(productKey);
        System.out.println(deviceName);
        System.out.println(deviceSecret);

        // 设备启动时初始温度，可以另外增加一些专业的操作，这里直接赋值
        setTemperature(-100);

        // 如果需要连接物联网平台
        if(connect) {
            // 设备连接云端
            openClient();
        }
    }

    /**
     * 判断参数是否在报警的温度范围内
     *
     * @param temperature 要判断的温度
     * @return 判断结果
     */
    public boolean isInWarningTemperature(Integer temperature) {
        return temperature < minTemperature || temperature > maxTemperature;
    }

    /**
     * 打开设备与云端连接，并订阅一些默认订阅的 topic
     */
    public void openClient() {
        if (!online) {  // 如果设备不在线，打开设备与云端的连接
            // 这里做一些准备工作，包括根据设备证书来计算 Mqtt 连接参数等
            //计算Mqtt建联参数
            MqttSign sign = new MqttSign();
            sign.calculate(productKey, deviceName, deviceSecret);

            //使用Paho连接阿里云物联网平台
            String port = "443";
            String broker = "ssl://" + productKey + ".iot-as-mqtt."+ Config.regionId+".aliyuncs.com" + ":" + port;
            // MemoryPersistence 设置 clientId 的保存形式，默认是为内存保存
            MemoryPersistence persistence = new MemoryPersistence();

            // 这里根据上面的准备工作进行Paho Mqtt 客户端的正式连接
            try {
                //Paho Mqtt 客户端
                sampleClient = new MqttClient(broker, sign.getClientid(), persistence);

                //Paho Mqtt 连接参数
                MqttConnectOptions connOpts = new MqttConnectOptions();
                connOpts.setCleanSession(true);
                connOpts.setKeepAliveInterval(180);
                connOpts.setUserName(sign.getUsername());
                connOpts.setPassword(sign.getPassword().toCharArray());

                // 连接
                sampleClient.connect(connOpts);
                System.out.println("信息：设备已成功连接到云端.");

                // 标识设备在线
                online = true;

                // 设备订阅一些默认的 topic
                subscribe(this);

                // 获取云端设备影子信息，以便和最后下线状态保持一致
                /**
                 * 发布此 JSON 去主动获取设备影子
                 * {
                 *  "method": "get"
                 * }
                 */
                JSONObject json = new JSONObject();
                json.put("method", "get");
                PubToDeviceShadow(json);
            } catch (MqttException e) {
                System.out.println("错误：设备启动过程中出错.");
                System.out.println("信息：reason - " + e.getReasonCode());
                System.out.println("信息：msg - " + e.getMessage());
                System.out.println("信息：loc - " + e.getLocalizedMessage());
                System.out.println("信息：cause - " + e.getCause());
                System.out.println("信息：excep - " + e);
            }
        } else {
            System.out.println("错误： 设备已经启动了.");
        }
    }

    /**
     * 设备消息订阅
     *
     * @param topic    设备需要订阅的 Topic
     * @param listener 与该 Topic 对应的消息处理接口
     */
    public void subscribe(String topic, IMqttMessageListener listener) {
        try {
            //Paho Mqtt 消息订阅
            sampleClient.subscribe(topic, listener);
            System.out.println("信息：设备订阅了此 Topic : " + topic);

        } catch (MqttException e) {
            System.out.println("错误：设备订阅 " + topic + "出错.");
            System.out.println("信息：reason - " + e.getReasonCode());
            System.out.println("信息：msg - " + e.getMessage());
            System.out.println("信息：loc - " + e.getLocalizedMessage());
            System.out.println("信息：cause - " + e.getCause());
            System.out.println("信息：excep - " + e);
        }
    }


    /**
     * 设备订阅内定的 topic
     * @param device_1  需要订阅 topic 的设备
     */
    private static void subscribe(Temperature device_1){
        // 设备属性上报 topic 订阅
        device_1.subscribe(
                "/sys/" + device_1.getProductKey() + "/" + device_1.getDeviceName() + "/thing/event/property/post_reply",
                new MqttMessageListener(device_1)
        );

        // 设备属性设置 topic 订阅
        device_1.subscribe(
                "/sys/" + device_1.getProductKey() + "/" + device_1.getDeviceName() + "/thing/service/property/set",
                new MqttMessageListener(device_1)
        );

        // 设备事件上报 topic 订阅
        device_1.subscribe(
                "/sys/" + device_1.getProductKey() + "/" + device_1.getDeviceName() + "/thing/event/TempOutWarning/post_reply",
                new MqttMessageListener(device_1)
        );

        // 设备服务调用 topic 订阅
        device_1.subscribe(
                "/sys/" + device_1.getProductKey() + "/" + device_1.getDeviceName() + "/thing/service/SetWarningTemp",
                new MqttMessageListener(device_1)
        );

        // 设备影子 topic 订阅
        device_1.subscribe(
                "/shadow/get/" + device_1.getProductKey() + "/" + device_1.getDeviceName(),
                new MqttMessageListener(device_1)
        );
    }

    /**
     * 根据当前设备的属性更新云端的影子信息
     */
    public void PubToDeviceShadow(JSONObject json){

        // 设备将当前设备的状态发布到此 topic
        String topic = "/shadow/update/" + productKey + "/" + deviceName;

        MqttMessage message = new MqttMessage(json.toString().getBytes(StandardCharsets.UTF_8));
        message.setQos(0);

        // 发布
        try {
            sampleClient.publish(topic,message);
            System.out.println("信息：设备向 " + topic + " 发布的内容 : " + json.toString());
        } catch (MqttException e) {
            System.out.println("错误：设备更新云端设备影子出错.");
            System.out.println("信息：reason - " + e.getReasonCode());
            System.out.println("信息：msg - " + e.getMessage());
            System.out.println("信息：loc - " + e.getLocalizedMessage());
            System.out.println("信息：cause - " + e.getCause());
            System.out.println("信息：excep - " + e);
        }

        // 发布成功后当前设备影子的版本号加 1；
        shadowVersion += 1;
        if(shadowVersion + 1 >= Long.MAX_VALUE) {  // 如果下一个版本号不合法，就重置版本号，因为版本号是 long 数据类型
            shadowVersion = -2;     // shadowVersion = -1 表示重置云端设备影子的版本号
        }

    }

    /**
     * 根据设备当前状态进行更新的最新设备影子数据
     * @return  最新设备影子数据
     */
    public JSONObject GetShadowByDeviceStatus(){
        /**
         * 发布此 JSON 去更新云端设备影子：
         * {
         *  "method": "update",
         *  "state": {
         *      "reported": {
         *          "temperature": ${temperature}
         *      }
         *  },
         *  "version": 1
         * }
         */
        JSONObject json = new JSONObject();
        json.put("method","update");
        JSONObject state = new JSONObject();
        JSONObject reported = new JSONObject();
        reported.put("temperature",temperature);
        state.put("reported",reported);
        json.put("state",state);
        json.put("version",shadowVersion + 1);

        return json;
    }

    public JSONObject GetClearDeviceShadowDesiredJSON(){
        /**
         * 发布此 JSON 消息去清除 desired 属性
         * {
         *  "method": "update",
         *  "state": {
         *      "desired": "null"
         *  },
         *  "version": 1
         * }
         */
        JSONObject obj = new JSONObject();
        obj.put("method", "update");
        JSONObject stateTemp = new JSONObject();
        stateTemp.put("desired", "null");
        obj.put("state", stateTemp);
        obj.put("version", shadowVersion + 1);

        return obj;
    }

    /**
     * 设备上报属性 -- 温度
     *
     * @param temp
     */
    public void pubTemperature(Integer temp) {
        try {
            //Paho Mqtt 消息发布

            // 发布内容
            String topic = "/sys/" + productKey + "/" + deviceName + "/thing/event/property/post";
            String content = "{\"id\":\"1\",\"version\":\"1.0\",\"params\":{\"temperature\":" + temp + "}}";
            MqttMessage message = new MqttMessage(content.getBytes());
            message.setQos(0);  // 消息质量等级为 0，即此消息最多只发送一次

            // 发布
            sampleClient.publish(topic, message);
            System.out.println("信息：设备向 " + topic + " 发布的内容 : " + content);
        } catch (MqttException e) {
            System.out.println("错误：设备上报温度出错.");
            System.out.println("信息：reason - " + e.getReasonCode());
            System.out.println("信息：msg - " + e.getMessage());
            System.out.println("信息：loc - " + e.getLocalizedMessage());
            System.out.println("信息：cause - " + e.getCause());
            System.out.println("信息：excep - " + e);
        }
    }

    /**
     * 设备响应云端服务调用 -- 温度
     * 异步调用服务
     *
     * @param result
     */
    public void AsynPubSetWarningTemp(Integer result) {
        try {
            //Paho Mqtt 消息发布

            // 发布内容
            String topic = "/sys/" + productKey + "/" + deviceName + "/thing/service/SetWarningTemp_reply";

            // 服务调用结果
            JSONObject json = new JSONObject();
            json.put("Result", result);

            // 设备响应云端服务调用的响应消息
            JSONObject response = new JSONObject();
            response.put("id", "2");
            response.put("version", "1.0");
            response.put("data", json);

            MqttMessage message = new MqttMessage(response.toString().getBytes(StandardCharsets.UTF_8));
            message.setQos(0);

            // 发布
            sampleClient.publish(topic, message);
            System.out.println("信息：设备向 " + topic + " 发布的内容 : " + response.toString());
        } catch (MqttException e) {
            System.out.println("错误：设备响应云端服务调用出错.");
            System.out.println("信息：reason - " + e.getReasonCode());
            System.out.println("信息：msg - " + e.getMessage());
            System.out.println("信息：loc - " + e.getLocalizedMessage());
            System.out.println("信息：cause - " + e.getCause());
            System.out.println("信息：excep - " + e);
        }
    }

    /**
     * 设备事件上报  -- 控制台已经配置事件 -- TempOutWarning
     * 事件上报没有输入参数
     */
    public void PubWarning() {
        // 约定 温度处于 [minTemperature,maxTemperature] 内不报警，是正常状态
        if (temperature > maxTemperature || temperature < minTemperature) {
            try {
                //Paho Mqtt 消息发布

                // 发布的 topic
                String topic = "/sys/" + productKey + "/" + deviceName + "/thing/event/TempOutWarning/post";

                // 发布消息内容
                // 报警输出参数
                JSONObject result = new JSONObject();
                result.put("high", temperature > maxTemperature ? 1 : 0);   // 当前温度大于最高温度，即温度过高
                result.put("low", temperature < minTemperature ? 1 : 0);    // 当前温度小于最低温度，即温度过低
                JSONObject response = new JSONObject();
                response.put("id", "1");
                response.put("version", "1.0");
                response.put("params",result);
                MqttMessage message = new MqttMessage(response.toString().getBytes(StandardCharsets.UTF_8));
                message.setQos(0);

                // 发布
                sampleClient.publish(topic, message);
                System.out.println("信息：设备向 " + topic + " 发布的内容 : " + response.toString());
            } catch (MqttException e) {
                System.out.println("错误：设备上报温度超出正常范围事件出错.");
                System.out.println("信息：reason - " + e.getReasonCode());
                System.out.println("信息：msg - " + e.getMessage());
                System.out.println("信息：loc - " + e.getLocalizedMessage());
                System.out.println("信息：cause - " + e.getCause());
                System.out.println("信息：excep - " + e);
            }
        } else {
            System.out.println("错误：事件 - 温度超出告警范围 上报失败，因为当前温度是 " + temperature +
                    " ，属于约定的正常范围 [" + minTemperature + "," + maxTemperature + "]");
        }
    }

    public boolean isOnline(){
        return online;
    }

    /**
     * 关闭与云端的连接
     */
    public void closeClient() {
        if (online) {    // 如果设备连接还未断开，断开
            // Paho Mqtt 断开连接
            try {
                // 断开连接之前设备根据当前状态更新设备影子
                /**
                 * 发布此 JSON 去更新云端设备影子：
                 * {
                 *  "method": "update",
                 *  "state": {
                 *      "reported": {
                 *          "temperature": ${temperature}
                 *      }
                 *  },
                 *  "version": 1
                 * }
                 */
                JSONObject json = new JSONObject();
                json.put("method","update");
                JSONObject state = new JSONObject();
                JSONObject reported = new JSONObject();
                reported.put("temperature",temperature);
                state.put("reported",reported);
                json.put("state",state);
                json.put("version",shadowVersion + 1);
                PubToDeviceShadow(json);

                sampleClient.disconnect();
            } catch (MqttException e) {
                System.out.println("错误：关闭设备的过程中出错.");
                System.out.println("信息：reason - " + e.getReasonCode());
                System.out.println("信息：msg - " + e.getMessage());
                System.out.println("信息：loc - " + e.getLocalizedMessage());
                System.out.println("信息：cause - " + e.getCause());
                System.out.println("信息：excep - " + e);
            }

            // 标识设备离线
            online = false;
            System.out.println("信息：设备已成功断开与云端的连接.");
        } else {
            System.out.println("错误: 设备与云端的连接已经断开.\n");
        }
    }

    public String getProductKey() {
        return productKey;
    }

    public void setProductKey(String productKey) {
        this.productKey = productKey;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public void setDeviceSecret(String deviceSecret) {
        this.deviceSecret = deviceSecret;
    }

    public int getMaxTemperature() {
        return maxTemperature;
    }

    public void setMaxTemperature(int maxTemperature) {
        this.maxTemperature = maxTemperature;
    }

    public int getMinTemperature() {
        return minTemperature;
    }

    public void setMinTemperature(int minTemperature) {
        this.minTemperature = minTemperature;
    }

    public Integer getTemperature() {
        return temperature;
    }

    public void setTemperature(Integer temperature) {
        this.temperature = temperature;
    }

    public long getShadowVersion() {
        return shadowVersion;
    }

    public void setShadowVersion(long shadowVersion) {
        this.shadowVersion = shadowVersion;
    }
}