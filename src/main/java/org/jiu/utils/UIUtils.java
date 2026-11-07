package org.jiu.utils;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.icons.FlatSearchIcon;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;

/**
 * Swing UI 样式工具类：提供统一字体、尺寸、配色及组件样式设置。
 * 对外提供搜索框/按钮/下拉框/状态灯样式、标准对话框、进度条与剪贴板操作。
 */
public class UIUtils {
    public static final Font TEXT_FONT = new Font("微软雅黑", Font.PLAIN, 13);
    public static final Font LABEL_FONT = new Font("微软雅黑", Font.PLAIN, 12);

    public static final int INPUT_HEIGHT = 30;
    public static final int BUTTON_HEIGHT = 30;
    public static final int COMBO_HEIGHT = 30;

    public static final Color PRIMARY_COLOR = new Color(0, 123, 255);
    public static final Color SUCCESS_COLOR = new Color(40, 167, 69);
    public static final Color WARNING_COLOR = new Color(255, 193, 7);
    public static final Color DANGER_COLOR = new Color(220, 53, 69);

    /**
     * 应用统一搜索框样式：字体、尺寸、占位文案、尾部搜索图标与清空按钮。
     */
    public static void setupSearchField(JTextField field, String placeholder) {
        field.setFont(TEXT_FONT);
        field.setPreferredSize(new Dimension(300, INPUT_HEIGHT));
        field.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, placeholder);
        field.putClientProperty(FlatClientProperties.TEXT_FIELD_TRAILING_ICON, new FlatSearchIcon());
        field.putClientProperty(FlatClientProperties.TEXT_FIELD_SHOW_CLEAR_BUTTON, true);
    }

    /**
     * 应用统一按钮样式（主色背景、白色文字）并设置按钮文本。
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
     * 应用统一下拉框样式与首选尺寸。
     */
    public static void setupComboBox(JComboBox<?> comboBox, int width) {
        comboBox.setFont(TEXT_FONT);
        comboBox.setPreferredSize(new Dimension(width, COMBO_HEIGHT));
    }

    /**
     * 初始化 API 状态指示灯按钮：无边框纯色方块，默认红色。
     */
    public static void setupStatusLabel(JButton statusBtn) {
        statusBtn.setPreferredSize(new Dimension(25, 25));
        statusBtn.setBorderPainted(false);
        statusBtn.setContentAreaFilled(false);
        statusBtn.setOpaque(true);
        statusBtn.setBackground(DANGER_COLOR);
        statusBtn.setToolTipText("API连接状态");
    }

    /**
     * 弹出标准消息对话框。
     */
    public static void showStandardDialog(Component parent, String title, String message, int messageType) {
        JOptionPane.showMessageDialog(
                parent,
                message,
                title,
                messageType
        );
    }

    /**
     * 弹出输入对话框，返回用户输入；取消或关闭时返回 null。
     */
    public static String showInputDialog(Component parent, String title, String message) {
        return JOptionPane.showInputDialog(parent, message, title, JOptionPane.QUESTION_MESSAGE);
    }

    /**
     * 创建进度条：显示进度文本，初始隐藏。
     */
    public static JProgressBar createProgressBar() {
        JProgressBar progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        progressBar.setVisible(false);
        return progressBar;
    }

    /**
     * 将文本写入系统剪贴板。
     */
    public static void copyToClipboard(String text) {
        Toolkit.getDefaultToolkit()
                .getSystemClipboard()
                .setContents(new StringSelection(text), null);
    }
}