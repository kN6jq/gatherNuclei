package org.jiu.ui;

import org.jiu.ui.dialog.ProxyConfigDialog;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class InitUI {
    private static final String VERSION = "v1";
    private static final String AUTHOR = "Xm17";

    // UI组件
    private static JTabbedPane mainTabbedPane;
    private static TemplatesPanel templatesPanel;
    private static SearchPanel searchPanel;
    private static ConfigPanel configPanel;
    private static YamlPanel yamlPanel;
    private static NucleiTemplatePanel nucleiTemplatePanel;
    private static WorkflowPanel workflowPanel;

    // 私有构造函数，防止实例化
    private InitUI() {
        throw new AssertionError("InitUI类不应该被实例化");
    }

    /**
     * 初始化所有UI组件
     * @param frame 主窗口
     */
    public static void initializeUI(JFrame frame) {
        try {
            initPanels();
            initMenuBar(frame);
            initTabbedPane(frame);
        } catch (Exception e) {
            showError(frame, "UI初始化失败", e);
        }
    }

    /**
     * 初始化所有面板
     */
    private static void initPanels() {
        templatesPanel = new TemplatesPanel();
        searchPanel = new SearchPanel();
        configPanel = new ConfigPanel();
        yamlPanel = new YamlPanel();
        nucleiTemplatePanel = new NucleiTemplatePanel();
        workflowPanel = new WorkflowPanel();
    }

    /**
     * 菜单栏初始化
     */
    private static void initMenuBar(JFrame frame) {
        JMenuBar menuBar = new JMenuBar();
        menuBar.add(createFileMenu(frame));
        menuBar.add(createSettingsMenu(frame));
        menuBar.add(createAboutMenu(frame));
        frame.setJMenuBar(menuBar);
    }

    /**
     * 创建文件菜单
     */
    private static JMenu createFileMenu(JFrame frame) {
        JMenu fileMenu = new JMenu("打开");
        JMenuItem shellItem = new JMenuItem("shell");
        shellItem.addActionListener(e -> SwingUtilities.invokeLater(() -> openSystemShell(frame)));
        fileMenu.add(shellItem);
        return fileMenu;
    }

    /**
     * 创建设置菜单
     */
    private static JMenu createSettingsMenu(JFrame frame) {
        JMenu settingsMenu = new JMenu("设置");
        
        // 代理配置
        JMenuItem proxyItem = new JMenuItem("代理配置");
        proxyItem.setToolTipText("配置 HTTP/SOCKS5 代理");
        proxyItem.addActionListener(e -> {
            ProxyConfigDialog dialog = new ProxyConfigDialog(frame);
            dialog.setVisible(true);
        });
        settingsMenu.add(proxyItem);
        
        return settingsMenu;
    }

    /**
     * 创建关于菜单
     */
    private static JMenu createAboutMenu(JFrame frame) {
        JMenu aboutMenu = new JMenu("关于");
        JMenuItem aboutItem = new JMenuItem("关于");
        aboutItem.addActionListener(e -> showAboutDialog(frame));
        aboutMenu.add(aboutItem);
        return aboutMenu;
    }

    /**
     * 显示关于对话框
     */
    private static void showAboutDialog(JFrame frame) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(
                    frame,
                    String.format("gatherNuclei %s by %s", VERSION, AUTHOR),
                    "关于",
                    JOptionPane.INFORMATION_MESSAGE
            );
        });
    }

    /**
     * 打开系统终端
     */
    private static void openSystemShell(JFrame frame) {
        String os = System.getProperty("os.name").toLowerCase();
        try {
            if (os.startsWith("win")) {
                Runtime.getRuntime().exec("cmd /c start cmd");
            } else if (os.startsWith("mac")) {
                Runtime.getRuntime().exec("open -a Terminal");
            } else if (os.contains("linux") || os.contains("unix")) {
                // 尝试常见的Linux终端
                String[] terminals = {"gnome-terminal", "konsole", "xterm"};
                for (String terminal : terminals) {
                    try {
                        Runtime.getRuntime().exec(terminal);
                        return;
                    } catch (IOException ignored) {
                        // 继续尝试下一个终端
                    }
                }
                showError(frame, "未找到可用的终端程序", null);
            } else {
                showMessage(frame, "暂不支持该系统", "提示");
            }
        } catch (Exception e) {
            showError(frame, "打开终端失败", e);
        }
    }

    /**
     * 选项卡面板初始化
     */
    private static void initTabbedPane(JFrame frame) {
        SwingUtilities.invokeLater(() -> {
            try {
                mainTabbedPane = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
                mainTabbedPane.setFont(new Font("微软雅黑", Font.PLAIN, 12));

                // 添加标签页 - 使用更兼容的标签
                addTab("模板管理", templatesPanel);
                addTab("工作流", workflowPanel);
                addTab("搜索引擎", searchPanel);
                addTab("配置", configPanel);
                addTab("新建模板", yamlPanel);
                addTab("Nuclei", nucleiTemplatePanel);

                frame.add(mainTabbedPane, BorderLayout.CENTER);
                frame.revalidate();
                frame.repaint();
            } catch (Exception e) {
                showError(frame, "初始化选项卡失败", e);
            }
        });
    }

    /**
     * 添加标签页的辅助方法
     */
    private static void addTab(String title, JComponent component) {
        if (component != null) {
            mainTabbedPane.addTab(title, component);
        } else {
            System.err.println("Warning: Attempted to add null component for tab: " + title);
        }
    }

    /**
     * 显示错误消息
     */
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

    /**
     * 显示提示消息
     */
    private static void showMessage(Component parent, String message, String title) {
        JOptionPane.showMessageDialog(
                parent,
                message,
                title,
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    // Getter方法
    public static JTabbedPane getMainTabbedPane() {
        return mainTabbedPane;
    }

    public static TemplatesPanel getTemplatesPanel() {
        return templatesPanel;
    }

    public static YamlPanel getYamlPanel() {
        return yamlPanel;
    }

    public static SearchPanel getSearchPanel() {
        return searchPanel;
    }

    public static ConfigPanel getConfigPanel() {
        return configPanel;
    }

    public static NucleiTemplatePanel getNucleiTemplatePanel() {
        return nucleiTemplatePanel;
    }
    public static WorkflowPanel getWorkflowPanel() {
        return workflowPanel;
    }
}