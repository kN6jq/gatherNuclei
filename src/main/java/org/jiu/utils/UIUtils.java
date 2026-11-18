package org.jiu.utils;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.icons.FlatSearchIcon;

import javax.swing.*;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.datatransfer.StringSelection;

/**
 * Swing UI 样式工具类：提供统一字体、尺寸、配色及组件样式设置。
 * 对外提供搜索框/按钮/下拉框/状态灯样式、标准对话框、进度条与剪贴板操作。
 */
public class UIUtils {
    public static final Font TEXT_FONT = new Font("微软雅黑", Font.PLAIN, 11);
    public static final Font LABEL_FONT = new Font("微软雅黑", Font.PLAIN, 11);

    /**
     * 表单类控件（配置项、对话框、面板按钮/输入框）的统一高度
     */
    public static final int INPUT_HEIGHT = 24;
    public static final int BUTTON_HEIGHT = 24;
    public static final int COMBO_HEIGHT = 24;
    /**
     * 搜索工具栏区域（搜索框、搜索按钮、字段多选）的统一高度，比表单略高便于输入
     */
    public static final int SEARCH_INPUT_HEIGHT = 28;

    public static final Color PRIMARY_COLOR = new Color(0, 123, 255);
    public static final Color SUCCESS_COLOR = new Color(40, 167, 69);
    public static final Color WARNING_COLOR = new Color(255, 193, 7);
    public static final Color DANGER_COLOR = new Color(220, 53, 69);
    public static final Color SECONDARY_COLOR = new Color(108, 117, 125);

    /**
     * 报文、匹配器等代码类文本的统一等宽字体
     */
    public static final Font CODE_FONT = new Font("Consolas", Font.PLAIN, 12);

    /**
     * 分组面板标题字体
     */
    public static final Font TITLE_FONT = new Font("微软雅黑", Font.BOLD, 12);

    /**
     * 小号辅助文字字体（徽标、提示等）
     */
    public static final Font SMALL_FONT = new Font("微软雅黑", Font.PLAIN, 10);

    /**
     * 应用统一搜索框样式：13px 字体、搜索区高度、占位文案、尾部搜索图标与清空按钮。
     */
    public static void setupSearchField(JTextField field, String placeholder) {
        field.setFont(TEXT_FONT);
        field.setPreferredSize(new Dimension(300, SEARCH_INPUT_HEIGHT));
        field.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, placeholder);
        field.putClientProperty(FlatClientProperties.TEXT_FIELD_TRAILING_ICON, new FlatSearchIcon());
        field.putClientProperty(FlatClientProperties.TEXT_FIELD_SHOW_CLEAR_BUTTON, true);
    }

    /**
     * 应用统一按钮样式（主色背景、白色文字）并设置按钮文本，高度取搜索区规格。
     */
    public static void setupSearchButton(JButton button, String text) {
        stylePrimaryButton(button, text);
        button.setPreferredSize(new Dimension(80, SEARCH_INPUT_HEIGHT));
    }

    /**
     * 统一按钮样式：13px 字体、30px 高度、无焦点框；
     * 传入语义色背景时文字设为白色，宽度随文字自适应且不小于 80。
     *
     * @param background 按钮底色，传 null 表示保留默认外观（用于次要按钮之外的场景）
     */
    public static void styleButton(JButton button, String text, Color background) {
        button.setText(text);
        button.setFont(TEXT_FONT);
        if (background != null) {
            button.setBackground(background);
            button.setForeground(Color.WHITE);
        }
        button.setFocusPainted(false);
        Dimension size = button.getPreferredSize();
        button.setPreferredSize(new Dimension(Math.max(size.width, 72), BUTTON_HEIGHT));
    }

    public static void stylePrimaryButton(JButton button, String text) {
        styleButton(button, text, PRIMARY_COLOR);
    }

    public static void styleSuccessButton(JButton button, String text) {
        styleButton(button, text, SUCCESS_COLOR);
    }

    public static void styleDangerButton(JButton button, String text) {
        styleButton(button, text, DANGER_COLOR);
    }

    public static void styleSecondaryButton(JButton button, String text) {
        styleButton(button, text, SECONDARY_COLOR);
    }

    /**
     * 统一输入框样式：12px 字体、26px 表单高度，宽度由调用方按布局需要指定。
     */
    public static void styleTextField(JTextField field, int width) {
        field.setFont(LABEL_FONT);
        field.setPreferredSize(new Dimension(width, INPUT_HEIGHT));
    }

    /**
     * 统一标签样式（12px 微软雅黑）。
     */
    public static void styleLabel(JLabel label) {
        label.setFont(LABEL_FONT);
    }

    /**
     * 统一分组面板边框：灰色细线标题边框（14px 粗体标题）加 10px 内边距。
     */
    public static javax.swing.border.Border createTitledBorder(String title) {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(new Color(200, 200, 200)),
                        title,
                        javax.swing.border.TitledBorder.LEFT,
                        javax.swing.border.TitledBorder.TOP,
                        TITLE_FONT,
                        new Color(50, 50, 50)
                ),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        );
    }

    /**
     * 应用统一下拉框样式与首选尺寸（搜索区高度，用于引擎工具栏）。
     */
    public static void setupComboBox(JComboBox<?> comboBox, int width) {
        setupComboBox(comboBox, width, SEARCH_INPUT_HEIGHT);
    }

    /**
     * 应用统一下拉框样式与指定高度（表单场景传 26，工具栏场景传 30）。
     */
    public static void setupComboBox(JComboBox<?> comboBox, int width, int height) {
        comboBox.setFont(TEXT_FONT);
        comboBox.setPreferredSize(new Dimension(width, height));
    }

    /**
     * 初始化 API 状态指示灯按钮：无边框纯色方块，默认红色。
     */
    /**
     * 应用统一表格样式：24px 行高、11px 字体、仅横向分隔线，
     * 表头加粗、28px 高、浅灰底与底部分隔线，便于与内容区分。
     */
    public static void styleTable(JTable table) {
        table.setRowHeight(24);
        table.setFont(LABEL_FONT);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(new Color(238, 240, 242));
        table.setIntercellSpacing(new Dimension(0, 1));

        JTableHeader header = table.getTableHeader();
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 28));
        header.setFont(LABEL_FONT.deriveFont(Font.BOLD));
        header.setBackground(new Color(247, 248, 250));
        header.setForeground(new Color(33, 37, 41));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(222, 226, 230)));
        header.setOpaque(true);
    }

    /**
     * 统一页面工具栏：不可浮动、4px 上下与 10px 左右内边距，与面板内容边缘对齐。
     */
    public static void styleToolBar(JToolBar toolBar) {
        toolBar.setFloatable(false);
        toolBar.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
    }

    /**
     * API 连接状态指示：显示为彩色圆点，默认红色（未连接），无背景与边框。
     */
    public static void setupStatusLabel(JLabel statusLabel) {
        statusLabel.setText("●");
        statusLabel.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        statusLabel.setForeground(DANGER_COLOR);
        statusLabel.setPreferredSize(new Dimension(18, 20));
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        statusLabel.setToolTipText("API 连接状态");
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