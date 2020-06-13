package com.lizhpn.iot.server;

import com.alibaba.fastjson.JSONObject;
import com.lizhpn.utils.ResultUtil;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

@Controller("Server")
public class AmqpController {

    // 设备影子版本号
    private long shadowVersion = -1;

    // 用于独立运行客户端的线程
    private Thread now = null;

    @RequestMapping("/getMessage")
    public ModelAndView getMessage() {
        // 保证线程不重复
        if (now == null) {
            now = new Thread(new Runnable() {
                @Override
                public void run() {
                    AmqpService.initQpidJMSClient();
                }
            });
            now.start();
        }

        ModelAndView mv = new ModelAndView();
        mv.addObject("property", ResultUtil.getJsons("property"));
        mv.addObject("status", ResultUtil.getJsons("status"));
        mv.addObject("event", ResultUtil.getJsons("event"));
        mv.addObject("service", ResultUtil.getJsons("service"));
        mv.setViewName("message");
        return mv;
    }

    @RequestMapping("/setProperty")
    public ModelAndView setProperty(@RequestParam("temperature") Integer temp) {
        ModelAndView mv = new ModelAndView();

        JSONObject json = AmqpService.sendUpdateAttributeCommandToDevice("PqvpolPkzIRm0VxKxoqC", temp);

        mv.addObject("result", json);
        mv.setViewName("success");
        return mv;
    }

    @RequestMapping("/invokeService")
    public ModelAndView invokeService(@RequestParam("serviceName") String service, Integer min, Integer max) {
        ModelAndView mv = new ModelAndView();

        JSONObject json = AmqpService.invokingDeviceService("PqvpolPkzIRm0VxKxoqC", service, min, max);

        mv.addObject("result", json);
        mv.setViewName("success");
        return mv;
    }

    @RequestMapping("/getShadowTemp")
    @ResponseBody
    public Integer getDeviceShadowTemp() {
        // 向云端请求设备影子
        JSONObject json = AmqpService.GetDeviceShadow("PqvpolPkzIRm0VxKxoqC");

        if (json != null && json.containsKey("shadow")) {     // 如果包含 shadow 关键字，说明请求设备影子成功
            JSONObject shadow = json.getJSONObject("shadow");
            // 获取最新的设备影子版本号
            shadowVersion = shadow.getLong("version");
            JSONObject state = shadow.getJSONObject("state");
            if (state.containsKey("desired")) {   // 如果返回的设备影子中有 desired 键名，表明设备处于离线状态，这时设备的温度实时值应该是 desired 值
                return state.getJSONObject("desired").getInteger("temperature");
            } else {
                return state.getJSONObject("reported").getInteger("temperature");
            }
        } else {  // 没有 shadow 关键字，说明请求失败
            return -100;  // 表示请求失败
        }
    }

    @RequestMapping("/updateShadowTemp")
    public ModelAndView updateDeviceShadowTemp(@RequestParam("temperature") Integer temp) {

        // 将当前设备影子版本号加 1，防止云端因版本号一致而拒绝更新设备影子
        shadowVersion += 1;
        // 向云端请求更新设备影子
        JSONObject json = AmqpService.UpdateDeviceShadow("PqvpolPkzIRm0VxKxoqC", temp, shadowVersion);


        ModelAndView mv = new ModelAndView();
        mv.addObject("result", json);
        mv.setViewName("success");
        return mv;
    }

    @RequestMapping("/getStatus")
    @ResponseBody
    public String getDeviceStatus() {
        // 向云端请求设备影子
        JSONObject json = AmqpService.GetDeviceStatus("PqvpolPkzIRm0VxKxoqC");
        if (json != null) {
            if (json.containsKey("status")) {     // 如果包含 status 关键字，说明请求设备状态成功
                return json.getString("status");
            } else {  // 没有 shadow 关键字，说明请求失败
                return json.getString("error");  // 表示请求失败
            }
        }
        else return "ERROR";
    }
}
