package org.jiu.ui;

import javax.swing.*;
import java.awt.*;

public class InitUI extends Component {
    private static JTabbedPane mainTabbedPane;
    private static TemplatesPanel templatesPanel;
    private static SearchPanel searchPanel;
    private static ConfigPanel configPanel;
    private static YamlPanel yamlPanel;

    public static TemplatesPanel getTemplatesPanel() {
        return templatesPanel;
    }

    public static JTabbedPane getMainTabbedPane() {
        return mainTabbedPane;
    }
    /**
     * 初始化所有面板
     */
    private static void initPanels() {
        templatesPanel = new TemplatesPanel();
        searchPanel = new SearchPanel();
        configPanel = new ConfigPanel();
        yamlPanel = new YamlPanel();
    }

    /**
     * 菜单栏初始化
     */
    public static void initMenuBar(JFrame jFrame) {
        JMenuBar menuBar = new JMenuBar();

        // 文件菜单
        JMenu fileMenu = new JMenu("打开");
        JMenuItem shellItem = new JMenuItem("shell");
        fileMenu.add(shellItem);

        // 关于菜单
        JMenu aboutMenu = new JMenu("关于");
        JMenuItem aboutItem = new JMenuItem("关于");

        // 关于对话框事件
        aboutItem.addActionListener(e -> {
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(
                        jFrame,
                        "gatherNuclei v1.0.0 by xm17",
                        "关于",
                        JOptionPane.INFORMATION_MESSAGE
                );
            });
        });

        // Shell打开事件
        shellItem.addActionListener(e -> {
            SwingUtilities.invokeLater(() -> {
                openSystemShell(jFrame);
            });
        });

        aboutMenu.add(aboutItem);
        menuBar.add(fileMenu);
        menuBar.add(aboutMenu);

        jFrame.setJMenuBar(menuBar);
        System.out.println("MenuBar created and added to frame");
    }

    /**
     * 打开系统终端
     */
    private static void openSystemShell(JFrame parent) {
        String os = System.getProperty("os.name").toLowerCase();
        try {
            if (os.startsWith("win")) {
                Runtime.getRuntime().exec("cmd /c start cmd");
            } else if (os.startsWith("mac")) {
                Runtime.getRuntime().exec("open -a Terminal");
            } else {
                JOptionPane.showMessageDialog(
                        parent,
                        "暂不支持该系统",
                        "提示",
                        JOptionPane.INFORMATION_MESSAGE
                );
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(
                    parent,
                    "打开终端失败: " + ex.getMessage(),
                    "错误",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    /**
     * 工具栏初始化
     */
    public static void initToolBar(JFrame jFrame) {

    }

    /**
     * 选项卡面板初始化
     */
    public static void initTabbedPane(JFrame jFrame) {

        // 确保面板已初始化
        initPanels();

        SwingUtilities.invokeLater(() -> {
            try {
                // 初始化mainTabbedPane
                mainTabbedPane = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);

                mainTabbedPane.addTab("仓库", templatesPanel);
                mainTabbedPane.addTab("搜索", searchPanel);
                mainTabbedPane.addTab("配置", configPanel);
                mainTabbedPane.addTab("模板编辑", yamlPanel);

                jFrame.add(mainTabbedPane, BorderLayout.CENTER);

                // 强制重新布局
                jFrame.revalidate();
                jFrame.repaint();

            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("Failed to initialize TabbedPane: " + e.getMessage());
                JOptionPane.showMessageDialog(
                        jFrame,
                        "初始化选项卡失败: " + e.getMessage(),
                        "错误",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        });
    }
}