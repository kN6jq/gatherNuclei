package org.jiu.utils;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;

public class UIUtils {
    // 定义统一的颜色方案
    public static final Color PRIMARY_COLOR = new Color(0, 123, 255);
    public static final Color SUCCESS_COLOR = new Color(40, 167, 69);
    public static final Color WARNING_COLOR = new Color(255, 193, 7);
    public static final Color DANGER_COLOR = new Color(220, 53, 69);
    
    // 显示标准对话框
    public static void showStandardDialog(Component parent, String title, String message, int messageType) {
        JOptionPane.showMessageDialog(
            parent,
            message,
            title,
            messageType
        );
    }
    
    public static String showInputDialog(Component parent, String title, String message) {
        return JOptionPane.showInputDialog(parent, message, title, JOptionPane.QUESTION_MESSAGE);
    }
    
    // 创建进度条
    public static JProgressBar createProgressBar() {
        JProgressBar progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        progressBar.setVisible(false);
        return progressBar;
    }
    
    // 复制到剪贴板的改进方法
    public static void copyToClipboard(String text) {
        Toolkit.getDefaultToolkit()
                .getSystemClipboard()
                .setContents(new StringSelection(text), null);
    }
}