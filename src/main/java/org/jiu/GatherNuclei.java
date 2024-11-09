package org.jiu;

import com.formdev.flatlaf.FlatLightLaf;
import org.jiu.ui.InitUI;

import javax.swing.*;
import java.awt.*;

import static org.jiu.utils.YamlUtils.parseConfig;

public class GatherNuclei extends JFrame {

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {
            try {
                initFlatLaf();
                parseConfig();
                createAndShowGUI();
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null,
                        "启动失败: " + e.getMessage(),
                        "错误",
                        JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        });
    }

    /**
     * 创建并显示主窗口
     */
    private static void createAndShowGUI() {
        JFrame frame = new GatherNuclei();
        frame.setLayout(new BorderLayout());
        frame.setTitle("gatherNuclei");

        // 设置窗口大小
        Dimension size = new Dimension(1200, 700);
        frame.setSize(size);
        frame.setPreferredSize(size);
        frame.setMinimumSize(new Dimension(800, 600)); // 设置最小窗口大小

        // 居中显示
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        // 初始化UI组件
        try {
            InitUI.initMenuBar(frame);
            InitUI.initToolBar(frame);
            InitUI.initTabbedPane(frame);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(frame,
                    "UI初始化失败: " + e.getMessage(),
                    "错误",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 创建底部面板
        JPanel bottomPanel = createBottomPanel();
        frame.add(bottomPanel, BorderLayout.SOUTH);

        // 显示窗口
        frame.pack();
        frame.setVisible(true);
    }

    /**
     * 创建底部面板
     */
    private static JPanel createBottomPanel() {
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10)); // 添加边距

        JLabel leftLabel = new JLabel("本工具只用于授权测试,禁止未授权渗透");
        JLabel rightLabel = new JLabel("Code by Xm17");

        // 设置标签字体和颜色
        Font labelFont = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
        Color labelColor = new Color(102, 102, 102);

        leftLabel.setFont(labelFont);
        leftLabel.setForeground(labelColor);
        rightLabel.setFont(labelFont);
        rightLabel.setForeground(labelColor);

        bottomPanel.add(leftLabel, BorderLayout.WEST);
        bottomPanel.add(rightLabel, BorderLayout.EAST);

        return bottomPanel;
    }

    /**
     * 初始化FlatLaf主题
     */
    private static void initFlatLaf() throws UnsupportedLookAndFeelException {
        // 设置系统属性以优化渲染
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        // 初始化FlatLaf
        if (!FlatLightLaf.setup()) {
            throw new UnsupportedLookAndFeelException("无法初始化FlatLightLaf主题");
        }

        // 自定义UI属性
        UIManager.put("TextComponent.arc", 5);
        // 可以添加其他UI定制
        UIManager.put("Button.arc", 5);
        UIManager.put("Component.focusWidth", 1);
        UIManager.put("Component.innerFocusWidth", 1);
        UIManager.put("ScrollBar.width", 14);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollBar.thumbInsets", new Insets(2, 2, 2, 2));
    }
}