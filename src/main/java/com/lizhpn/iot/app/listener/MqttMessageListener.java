package com.lizhpn.iot.app.listener;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.lizhpn.iot.app.Temperature;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttMessage;


/**
 * 设备订阅 Topic 后，用于处理云端返回消息的监听处理类
 */
public class MqttMessageListener implements IMqttMessageListener {

    // 设备
    private Temperature device;

    public MqttMessageListener(Temperature device) {
        this.device = device;
    }

    /**
     * 处理到达的云端消息
     *
     * @param var1 该消息所属的 topic
     * @param var2 消息主体
     * @throws Exception
     */
    @Override
    public void messageArrived(String var1, MqttMessage var2) throws Exception {

        System.out.println("\n*** 收到云端下发消息，内容及处理结果如下： ***\n");
        System.out.println("信息：云端发布消息的 topic  - " + var1);
        System.out.println("信息：云端发布消息的 payload- " + var2.toString());

        if (var1.contains("/thing/event/property/post_reply")) {                // 属性上报的 topic 订阅
            PropertyPostReply(var1, var2);
        } else if (var1.contains("/thing/service/property/set")) {              // 属性设置的 topic 订阅
            PropertySetReply(var1, var2);
        } else if (var1.contains("/thing/event/TempOutWarning/post_reply")) {   // 事件上报的 topic 订阅
            EventTempOutWarningReply(var1, var2);
        } else if (var1.contains("/shadow/get")) {                              // 设备影子的 topic 订阅
            ShadowGetReply(var1, var2);
        } else if (var1.contains("/thing/service/SetWarningTemp")) {            // 设备服务调用的 topic 订阅
            ServiceSetWarningTempReply(var1, var2);
        }

        System.out.println("\n*** 云端消息接收完毕 ***\n");
    }

    /**
     * /shadow/get/  --设备影子 订阅 Topic 的消息处理
     * {  "method": "reply",   "payload": {    "status": "success",     "state": {      "reported": {        "color": "red"      },       "desired": {        "color": "green"      }    },     "metadata": {      "reported": {        "color": {          "timestamp": 1469564492        }      },       "desired": {        "color": {          "timestamp": 1469564492        }      }    }  },   "version": 2,   "timestamp": 1469564576 }
     */
    private void ShadowGetReply(String topic, MqttMessage message) throws InterruptedException {
        // 原始的 JSON 消息
        JSONObject json = JSON.parseObject(message.toString());

        // 如果消息里面没有 state 这个键名，表明是 method = update ，即更新设备影子后云端的响应消息
        if (!(message.toString().contains("state"))) {
            JSONObject payload = json.getJSONObject("payload");

            // 获取 method = update 这条指令的执行结果
            String result = payload.getString("status");
            System.out.println("信息：更新设备影子结果：" + result);

            if (result.equals("success")) {      // 只有成功执行的指令才会有 version 这个键
                // 记录当前设备影子的版本号
                device.setShadowVersion(payload.getLong("version"));
            } else {    // 指令执行失败，获取相关的失败信息
                JSONObject content = payload.getJSONObject("content");
                String errorCode = content.getString("errorcode");
                String errorMessage = content.getString("errormessage");
                System.out.println(errorCode + " 错误：" + errorMessage);
            }
        }
        // 否则，表明是 method = get 即获取云端设备影子信息的响应消息或者是 method = control 的云端下发控制指令消息
        else {
            // 更新设备的状态
            updateDeviceState(json);
        }
    }

    // 根据设备影子的信息更新设备的状态，并且同步更新设备影子信息
    private void updateDeviceState(JSONObject json) throws InterruptedException {
        // 更新 shadowVersion
        device.setShadowVersion(json.getLong("version"));

        // 获取 payload
        JSONObject payload = json.getJSONObject("payload");

        // 获取 state，并获取到设备影子的当前状态
        JSONObject state = payload.getJSONObject("state");

        // 获取 reported ，读取设备影子的温度
        JSONObject reported = state.getJSONObject("reported");
        // temp 表示设备初次启动时的初始化温度,这里先从设备影子的状态中获取
        Integer temp = reported.getInteger("temperature");

        if (state.containsKey("desired")) {    // 如果此 topic 中的消息含有 desired 键值，表明有期待属性需要设备更新
            // 获取 desired
            JSONObject desired = state.getJSONObject("desired");

            // 这里设备的初始化温度更新为设备影子中的期望属性
            try{
                temp = desired.getInteger("temperature");
            }catch (Exception e){
                e.printStackTrace();
            }

            // 因为期望值已经被设备获取到，所以需要清除设备影子中的期望值
            // 以新线程开始，发布以更新设备影子
            new Thread(new Runnable() {
                @Override
                public void run() {
                    device.PubToDeviceShadow(device.GetClearDeviceShadowDesiredJSON());
                }
            }).start();

            // 等待云端更新设备影子
            Thread.sleep(3000);
        }

        // 更新设备的温度
        device.setTemperature(temp);

        // 属性改变
        AttributeChangedDoIt();

    }


    /**
     * /thing/event/property/post_reply --属性上报 订阅 Topic 的消息处理
     */
    private void PropertyPostReply(String topic, MqttMessage message) {

        JSONObject json = JSON.parseObject(message.toString());
        if ("success".equals(json.getString("message"))) {
            System.out.println("信息：成功上报本设备的温度，云端返回的消息如下：");
            System.out.println("信息：reply topic  - " + topic);
            System.out.println("信息：reply payload- " + message.toString());
        } else {
            System.out.println("错误：设备上报属性失败");
        }
    }

    /**
     * /thing/service/property/set  --属性设置 订阅 Topic 的消息处理
     */
    private void PropertySetReply(String topic, MqttMessage message) throws InterruptedException {
        JSONObject json = JSON.parseObject(message.toString());

        // 获取云端下发的属性期望值
        String params = json.getString("params");
        JSONObject param = JSON.parseObject(params);
        int temperature = param.getInteger("temperature");

        // 检查云端下发属性的合法性,假设 [-10,38] 范围内是合法的数据
        if (temperature >= -10 && temperature <= 38) {  // 合法
            System.out.println("信息：收到云端下发的属性设置命令,开始更新相应属性...");

            // 设备根据云端下发的属性值更新当前设备的属性值
            device.setTemperature(temperature);

            // 属性值改变，启动相应线程对最新属性进行必要的处理
            AttributeChangedDoIt();

            // 等待设备上报温度完成
            Thread.sleep(3000);
            System.out.println("信息：设备属性更新完成.");
        } else {                                           // 不合法
            System.out.println("错误：云端下发属性不合法，设备拒绝更新属性值. (ERROR : 预期属性值超出合法数据范围 [-10,38] ).");
        }
    }

    /**
     * 每当属性值改变时，都应该调用此方法；
     * 此方法做了以下三件事：
     * 1、向云端上报最新属性；
     * 2、更新云端设备影子文档；
     * 3、判断最新属性是否触发报警；
     */
    private void AttributeChangedDoIt(){
        // 启动新线程上报设备温度，防止线程监听类阻塞
        new Thread(new Runnable() {
            @Override
            public void run() {
                device.pubTemperature(device.getTemperature());
            }
        }).start();

        // 启动新线程更新设备影子，防止线程监听类阻塞
        // 更新设备影子是为了保证服务端能够正确获取设备属性，因为服务端访问的都是设备影子的数据
        new Thread(new Runnable() {
            @Override
            public void run() {
                device.PubToDeviceShadow(device.GetShadowByDeviceStatus());
            }
        }).start();

        // 如果云端下发的指令中的温度超出正常范围，报警
        if(device.isInWarningTemperature(device.getTemperature())){
            // 启动新线程上报事件，防止线程监听类阻塞
            new Thread(new Runnable() {
                @Override
                public void run() {
                    device.PubWarning();
                }
            }).start();
        }
    }

    /**
     * /thing/event/TempOutWarning/post_reply  --事件上报 订阅 Topic 的消息处理
     */
    private void EventTempOutWarningReply(String topic, MqttMessage message) {
        JSONObject json = JSON.parseObject(message.toString());
    }


    /**
     * /thing/service/SetWarningTemp  --设备服务调用 订阅 Topic 的消息处理
     */
    private void ServiceSetWarningTempReply(String topic, MqttMessage message) throws InterruptedException {
        System.out.println("信息：设备收到云端下发指令，开始更新设备的正常温度范围...");
        JSONObject json = JSON.parseObject(message.toString());
        JSONObject params = JSON.parseObject(json.getString("params"));
        if (params.containsKey("minWarningTemp") && params.containsKey("maxWarningTemp")) {
            int min = params.getInteger("minWarningTemp");
            int max = params.getInteger("maxWarningTemp");
            if (min <= max) {
                device.setMinTemperature(min);
                device.setMaxTemperature(max);

                System.out.println("信息：设备正常温度范围更新完成.\n");
            } else {
                System.out.println("错误：数据异常，设备拒绝更新. (ERROR: min(" + min + ") > max(" + max + "). )");
            }

            // 响应云端服务调用
            new Thread(new Runnable() {
                @Override
                public void run() {
                    device.AsynPubSetWarningTemp(min <= max ? 1 : 0);
                }
            }).start();

            // 等待设备响应服务调用
            Thread.sleep(3000);
        } else {
            System.out.println("错误： 服务调用缺少必要的参数.");
        }
    }
}
