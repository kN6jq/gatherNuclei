package org.jiu.utils;

import javax.swing.*;
import java.io.File;

/**
 * 全局配置与通用操作工具类：持有 gatherConfig.yaml 加载出的各引擎配置项。
 * 对外提供运行目录获取与带提示的剪贴板复制。
 */
public class Utils {
    public static String templatePath = "";
    public static String templateArg = "";
    public static String fofaUrl = "";
    public static String fofaKey = "";
    public static String fofaTimeRange = "all";
    public static String hunterUrl = "";
    public static String hunterKey = "";
    public static String zoneUrl = "";
    public static String zoneKey = "";
    public static String daydaymapUrl = "";
    public static String daydaymapKey = "";
    public static String workflowPath = getRootPath() + File.separator + "workflows";  // 工作流目录路径，位于程序运行目录下

    /**
     * 返回程序运行目录（user.dir）。
     */
    public static String getRootPath() {
        return System.getProperty("user.dir");
    }

    /**
     * 复制文本到剪贴板，并弹出"已复制"提示框。
     */
    public static void copyToClipboard(String text) {
        UIUtils.copyToClipboard(text);
        JOptionPane.showMessageDialog(null,
                "已复制到剪贴板",
                "提示",
                JOptionPane.INFORMATION_MESSAGE);
    }
}
