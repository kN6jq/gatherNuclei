package org.jiu.utils;

import javax.swing.*;
import java.awt.*;
import java.io.*;

public class Utils {
    public static String templatePath = "";
    public static String templateArg = "";
    public static String fofaUrl = "";
    public static String fofaKey = "";
    public static String hunterUrl = "";
    public static String hunterKey = "";
    public static String zoneUrl = "";
    public static String zoneKey = "";
    public static String daydaymapUrl = "";
    public static String daydaymapKey = "";

    // 获取程序运行目录
    public static String getRootPath() {
        return System.getProperty("user.dir");
    }

    public static String workflowPath = getRootPath() + File.separator + "workflows";  // 在程序运行目录下创建workflows文件夹

    public static void copyToClipboard(String text) {
        UIUtils.copyToClipboard(text);
        JOptionPane.showMessageDialog(null,
                "已复制到剪贴板",
                "提示",
                JOptionPane.INFORMATION_MESSAGE);
    }
}
