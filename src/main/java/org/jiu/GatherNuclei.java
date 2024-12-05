package org.jiu;

import com.formdev.flatlaf.FlatLightLaf;
import org.jiu.ui.InitUI;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

import static org.jiu.utils.YamlUtils.parseConfig;

public class GatherNuclei extends JFrame {
    private static final String TITLE = "gatherNuclei";
    private static final int DEFAULT_WIDTH = 1200;
    private static final int DEFAULT_HEIGHT = 700;
    private static final int MIN_WIDTH = 800;
    private static final int MIN_HEIGHT = 600;

    public GatherNuclei() {
        initFrame();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                setupUI();
                createAndShowGUI();
            } catch (Exception e) {
                showError(null, "启动失败", e);
                System.exit(1);
            }
        });
    }

    private static void setupUI() throws Exception {
        // 设置UI主题
        if (!FlatLightLaf.setup()) {
            throw new Exception("无法初始化FlatLightLaf主题");
        }

        // 解析配置文件
        parseConfig();
    }

    private static void createAndShowGUI() {
        GatherNuclei frame = new GatherNuclei();

        try {
            // 初始化所有UI组件
            InitUI.initializeUI(frame);

            // 添加底部面板
            frame.add(createBottomPanel(), BorderLayout.SOUTH);

            // 显示窗口
            frame.setVisible(true);

        } catch (Exception e) {
            showError(frame, "UI初始化失败", e);
        }
    }

    private void initFrame() {
        setTitle(TITLE);
        setLayout(new BorderLayout());

        // 设置窗口大小
        setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        setMinimumSize(new Dimension(MIN_WIDTH, MIN_HEIGHT));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    }

    private static JPanel createBottomPanel() {
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBorder(new EmptyBorder(5, 10, 5, 10));

        // 创建标签
        JLabel leftLabel = createLabel("本工具只用于授权测试,禁止未授权渗透", SwingConstants.LEFT);
        JLabel rightLabel = createLabel("Code by Xm17", SwingConstants.RIGHT);

        // 添加标签
        bottomPanel.add(leftLabel, BorderLayout.WEST);
        bottomPanel.add(rightLabel, BorderLayout.EAST);

        return bottomPanel;
    }

    private static JLabel createLabel(String text, int alignment) {
        JLabel label = new JLabel(text);
        label.setHorizontalAlignment(alignment);
        return label;
    }

    private static void showError(Component parent, String message, Exception e) {
        if (e != null) {
            e.printStackTrace();
            message = message + ": " + e.getMessage();
        }
        JOptionPane.showMessageDialog(
                parent,
                message,
                "错误",
                JOptionPane.ERROR_MESSAGE
        );
    }
}