package org.jiu.utils;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.icons.FlatSearchIcon;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;

public class UIUtils {
    // 统一字体设置
    public static final Font TEXT_FONT = new Font("微软雅黑", Font.PLAIN, 13);
    public static final Font LABEL_FONT = new Font("微软雅黑", Font.PLAIN, 12);
    
    // 统一组件尺寸
    public static final int INPUT_HEIGHT = 30;
    public static final int BUTTON_HEIGHT = 30;
    public static final int COMBO_HEIGHT = 30;

    // 定义统一的颜色方案
    public static final Color PRIMARY_COLOR = new Color(0, 123, 255);
    public static final Color SUCCESS_COLOR = new Color(40, 167, 69);
    public static final Color WARNING_COLOR = new Color(255, 193, 7);
    public static final Color DANGER_COLOR = new Color(220, 53, 69);
    
    /**
     * 设置统一的文本框样式
     */
    public static void setupSearchField(JTextField field, String placeholder) {
        field.setFont(TEXT_FONT);
        field.setPreferredSize(new Dimension(300, INPUT_HEIGHT));
        field.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, placeholder);
        field.putClientProperty(FlatClientProperties.TEXT_FIELD_TRAILING_ICON, new FlatSearchIcon());
        field.putClientProperty(FlatClientProperties.TEXT_FIELD_SHOW_CLEAR_BUTTON, true);
    }

    /**
     * 设置统一的按钮样式
     */
    public static void setupSearchButton(JButton button, String text) {
        button.setText(text);
        button.setFont(TEXT_FONT);
        button.setPreferredSize(new Dimension(85, BUTTON_HEIGHT));
        button.setBackground(PRIMARY_COLOR);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
    }

    /**
     * 设置统一的下拉框样式
     */
    public static void setupComboBox(JComboBox<?> comboBox, int width) {
        comboBox.setFont(TEXT_FONT);
        comboBox.setPreferredSize(new Dimension(width, COMBO_HEIGHT));
    }

    /**
     * 设置统一的状态指示灯样式
     */
    public static void setupStatusLabel(JButton statusBtn) {
        statusBtn.setPreferredSize(new Dimension(25, 25));
        statusBtn.setBorderPainted(false);
        statusBtn.setContentAreaFilled(false);
        statusBtn.setOpaque(true);
        statusBtn.setBackground(DANGER_COLOR);
        statusBtn.setToolTipText("API连接状态");
    }
    
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