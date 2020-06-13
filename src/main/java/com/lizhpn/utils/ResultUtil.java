package com.lizhpn.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 结果处理工具
 */
public class ResultUtil {
    private ResultUtil() {
    }

    // 属性相关 topic
    private static List<JSONObject> propertyJsons;

    // 设备状态相关 topic
    private static List<JSONObject> statusJsons;

    // 设备事件上报相关 topic
    private static List<JSONObject> eventJsons;

    // 服务调用上报相关 topic
    private static List<JSONObject> serviceJsons;

    static {
        propertyJsons = new ArrayList<>();
        statusJsons = new ArrayList<>();
        eventJsons = new ArrayList<>();
        serviceJsons = new ArrayList<>();
    }

    public static void addObject(String type, String topic, String messageId, String jsonString) {
        JSONObject json = JSON.parseObject(jsonString);

        // 添加消息内容
        if (topic != null) {
            json.put("topic", topic);
        }
        if (messageId != null) {
            json.put("messageId", messageId);
        }

        System.out.println("有消息：" + json);
        if ("property".equals(type)) {
            propertyJsons.add(json);
        } else if ("status".equals(type)) {
            statusJsons.add(json);
        } else if ("event".equals(type)) {
            eventJsons.add(json);
        } else if ("service".equals(type)) {
            String source = json.getString("source");
            if(source.equals("SYSTEM")){     // 如果服务调用的返回消息来自平台而非设备,修改数据以兼容设备返回的消息
                json.put("data",json.getString("message"));
            }
            serviceJsons.add(json);
        }
    }

    /**
     * 返回相应类型的 topic 消息
     * @param type  topic 的类型
     * @return
     */
    public static List<JSONObject> getJsons(String type) {

        if ("property".equals(type)) {
            return propertyJsons;
        } else if ("status".equals(type)) {
            return statusJsons;
        } else if ("event".equals(type)) {
            return eventJsons;
        } else if ("service".equals(type)) {
            return serviceJsons;
        }

        return null;
    }
}
