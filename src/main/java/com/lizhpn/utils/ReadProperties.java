package com.lizhpn.utils;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;

/**
 * 可以读取单个配置文件，
 * 也可以一次读取多个配置文件，但需要通过建立中间配置文件
 */
public class ReadProperties {

    private ReadProperties() {

    }

    /**
     * 设置文件后缀名
     */
//    public void setFileModer(String fileModer) {
//        readProperties.fileModer = "."+fileModer;
//    }


    /**
     * 读取配置文件的底层方法
     *
     * @param file 文件名
     * @return 配置文件中的名值对
     */
    private static Map<String, String> read(String file) {
        Map<String, String> res = new HashMap<String, String>();
        InputStream f = null;

        ClassLoader classLoader = ReadProperties.class.getClassLoader();
        URL path = classLoader.getResource(file);
        if(path != null) {
            try {
                f = path.openStream();
            } catch (IOException e) {
                System.out.println("错误：资源文件 " + file + " 读取错误.");
                e.printStackTrace();
            }
        }
        else{
            System.out.println("错误：资源文件 " + file + " 读取错误.");
            return null;
        }

        Properties pt = new Properties();
        try {
            pt.load(f);
        } catch (IOException e) {
            System.out.println("错误：读取配置文件 - " + file + " 失败.");
            return null;
        }

        Enumeration names = pt.propertyNames();
        while (names.hasMoreElements()) {
            String temp = (String) names.nextElement();
            res.put(temp, pt.getProperty(temp));
        }

        if (!res.isEmpty()) {
            return res;
        }
        return null;
    }

    /**
     * 读一批配置文件，并返回每个配置文件的信息
     *
     * @param files 批量的配置文件,需要完整路径，但不需要文件后缀名，否则返回null
     * @return 配置文件内容数组
     */
    public static List<Map<String, String>> readProperties(Collection<String> files) {
        if (files.size() > 0) {
            List<Map<String, String>> res = new ArrayList<Map<String, String>>();

            for (String file : files) {
                res.add(read(file));
            }

            if (!res.isEmpty()) {
                return res;
            }
        }

        return null;
    }

    /**
     * 读一个配置文件
     *
     * @param file 配置文件名，需要完整路径，但不需要文件后缀名，否则返回null
     * @return
     */
    public static Map<String, String> readProperties(String file) {
        return read(file);
    }

    /**
     * 提取Map中的关键字
     *
     * @param res Map
     * @return 所有的Key值的字符串数组
     */
    public static String[] toKeyArray(Map<String, String> res) {
        if (res.size() > 0) {
            Set<String> temp = res.keySet();
            String[] result = new String[temp.size()];

            temp.toArray(result);

            return result;
        }
        return null;
    }

    /**
     * 读取根配置文件，以达到读取所有配置文件的目的
     * 即将多个子配置文件写在一个根配置文件中，再通过读取这个根配置文件来达到读取所有子配置文件的目的；
     * 注意：子配置文件和根配置文件在同级目录中
     *
     * @param file 根配置文件
     * @return 所有子配置文件的内容
     */
    public static Map<String, Map<String, String>> readAllPropertiesByOne(String file) {
        String parentFilePath = "";
        ArrayList<String> coll = new ArrayList<String>();

        // 获取根配置文件下的所有子配置文件名
        String[] key = toKeyArray(readProperties(file));

        String[] parents = file.split("/");
        if (parents.length > 1) {
            for (int i = 0; i < parents.length - 1; i++) {
                parentFilePath += parents[i] + "/";
            }

            if (null != key) {
                for (String s : key) {
                    coll.add(parentFilePath + s);
                }
            }

            // 返回结果类型，键值对表示 文件名/此文件内的配置内容
            Map<String, Map<String, String>> result = new HashMap<>();
            List<Map<String, String>> list = readProperties(coll);
            if (list != null) {
                for (int i = 0; i < list.size(); i++) {
                    result.put(coll.get(i), list.get(i));
                }
                return result;
            }
            else
                return null;
        }
        System.out.println("请用符号'/'作为根配置文件的文件路径分隔符。");
        return null;
    }
}
