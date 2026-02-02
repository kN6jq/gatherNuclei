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
            // 设置菜单栏
            frame.setJMenuBar(createMenuBar());
            
            // 初始化所有UI组件
            InitUI.initializeUI(frame);

            // 添加底部状态栏
            frame.add(createStatusBar(), BorderLayout.SOUTH);

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

    private static JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        
        // 文件菜单
        JMenu fileMenu = new JMenu("文件");
        fileMenu.add(new JMenuItem("打开模板目录"));
        fileMenu.add(new JMenuItem("导入模板"));
        fileMenu.add(new JMenuItem("导出模板"));
        fileMenu.addSeparator();
        fileMenu.add(new JMenuItem("退出"));
        
        // 工具菜单
        JMenu toolsMenu = new JMenu("工具");
        toolsMenu.add(new JMenuItem("模板验证"));
        toolsMenu.add(new JMenuItem("批量处理"));
        
        // 帮助菜单
        JMenu helpMenu = new JMenu("帮助");
        helpMenu.add(new JMenuItem("使用说明"));
        helpMenu.add(new JMenuItem("关于"));
        
        menuBar.add(fileMenu);
        menuBar.add(toolsMenu);
        menuBar.add(helpMenu);
        
        return menuBar;
    }

    private static JPanel createStatusBar() {
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBorder(BorderFactory.createLoweredSoftBevelBorder());
        
        // 左侧信息
        JLabel leftLabel = new JLabel("就绪");
        leftLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
        leftLabel.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        
        // 右侧信息
        JLabel rightLabel = new JLabel("gatherNuclei v1");
        rightLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
        rightLabel.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        
        statusBar.add(leftLabel, BorderLayout.WEST);
        statusBar.add(rightLabel, BorderLayout.EAST);
        
        return statusBar;
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