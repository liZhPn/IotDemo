package com.lizhpn.iot.app;

import com.lizhpn.iot.config.Config;

import java.util.Scanner;

public class TestApps {
    public static void main(String[] args) throws InterruptedException {
        // 烧录设备证书
        Temperature device_1 = new Temperature(Config.produceKey, Config.deviceName, Config.deviceSecret);

        // 循环模拟设备属性上报
        while (true) {
            operation(device_1);
        }
    }

    /**
     * 操作设备
     *
     * @param device_1 被操作的设备
     * @throws InterruptedException
     */
    private static void operation(Temperature device_1) {
        System.out.println("\n************************************************************************\n");
        System.out.println("请选择下面的操作命令编号（如 ‘1’）以查看或设置当前设备的状态：");
        System.out.println
                ("1、启动设备； 2、关闭设备； 3、上报属性； 4、显示当前设备的温度； 5、显示温度正常范围； " +
                        "6、更新设备影子； 7、退出");
        System.out.println("\n************************************************************************\n");
        Scanner scan = new Scanner(System.in);
        int choice = scan.nextInt();

        try {
            switch (choice) {
                case 3: // 设备上报属性
                    System.out.println("\n*** 3、上报属性 ***\n");
                    if (device_1.isOnline()) {  // 设备在线才能上报
                        DeviceSettingTemperature(device_1);

                        // 休眠两秒等待云端消息返回
                        System.out.println("信息：等待云端消息返回...");
                        Thread.sleep(2000);
                    } else {
                        System.out.println("错误：设备尚未启动.");
                    }
                    System.out.println("\n*** 上报属性完成 ***\n");
                    break;

                case 4: // 显示当前设备的温度,若设备已经关闭则无法获取
                    System.out.println("\n*** 4、显示当前设备的温度 ***\n");
                    System.out.println("当前设备的温度：" + (device_1.isOnline() ? device_1.getTemperature() : "设备已关闭，无法获取"));

                    // 造成等待的错觉，给用户一点缓神的时间
                    Thread.sleep(1000);
                    System.out.println("\n*** 显示当前设备的温度完成 ***\n");
                    break;

                case 5: // 显示温度正常范围
                    System.out.println("\n*** 5、显示温度正常范围 ***\n");
                    System.out.println("当前设备的正常温度范围是： " + (!device_1.isOnline() ? "设备已关闭，无法获取" : "[" + device_1.getMinTemperature() + "," + device_1.getMaxTemperature() + "] ."));

                    // 造成等待的错觉，给用户一点缓神的时间
                    Thread.sleep(1000);
                    System.out.println("\n*** 显示温度正常范围 完成 ***\n");
                    break;

                case 1: // 启动设备
                    System.out.println("\n*** 1、启动设备 ***\n");
                    // 打开和云端的连接
                    device_1.openClient();

                    System.out.println("\n*** 启动设备完成 ***\n");
                    break;

                case 2: // 关闭设备
                    System.out.println("\n*** 2、关闭设备 ***\n");
                    // 关闭和云端的连接
                    device_1.closeClient();

                    System.out.println("\n*** 关闭设备完成 ***\n");
                    break;

                case 6:     // 更新设备影子
                    System.out.println("\n*** 6、更新设备影子 ***\n");
                    if (device_1.isOnline()) {  // 设备在线才能更新
                        // 更新设备影子
                        device_1.PubToDeviceShadow(device_1.GetShadowByDeviceStatus());
                    }
                    else{
                        System.out.println("错误：设备尚未启动.");
                    }

                    System.out.println("\n*** 更新设备影子完成 ***\n");
                    break;

                case 7:
                    // 直接退出
                    System.exit(0);
                default:
                    choice = -1;
                    break;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 设备主动上报属性 -- 温度读数
     *
     * @param device 要上报属性的设备
     * @throws InterruptedException
     */
    private static void DeviceSettingTemperature(Temperature device) throws InterruptedException {
        Integer temp = 0;
        System.out.println("请输入设备温度：");
        Scanner scan = new Scanner(System.in);
        temp = scan.nextInt();

        // 设备保存当前温度
        device.setTemperature(temp);

        // 如果当前温度是在报警温度范围内，设备上报温度超出正常范围事件
        if (device.isInWarningTemperature(temp)) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    device.PubWarning();
                }
            }).start();
        }

        System.out.println("信息：上报中...");

        // 设备向云端上报属性 -- 温度的实时值
        device.pubTemperature(temp);

        // 同时更新设备影子数据，以便服务端可以正确获取到设备的正确状态
        new Thread(new Runnable() {
            @Override
            public void run() {
                device.PubToDeviceShadow(device.GetShadowByDeviceStatus());
            }
        }).start();
    }
}
