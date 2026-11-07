package org.jiu.ui;

import com.formdev.flatlaf.FlatClientProperties;
import org.jiu.ui.component.Icons;
import org.jiu.ui.dialog.ProxyConfigDialog;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

/**
 * 主界面装配工具类：创建各功能面板、顶部菜单与主标签页，并对外提供面板实例访问。
 */
public class InitUI {
    private static final String VERSION = "v1.0.0";
    private static final String AUTHOR = "Xm17";
    /**
     * 主标签页图标名，顺序与标签页一致
     */
    private static final String[] MAIN_TAB_ICONS = {
            "template", "workflow", "search", "settings", "newtemplate", "generator"
    };
    /**
     * 工作流标签页在主标签页中的索引
     */
    private static final int WORKFLOW_TAB_INDEX = 1;
    private static JTabbedPane mainTabbedPane;
    private static TemplatesPanel templatesPanel;
    private static SearchPanel searchPanel;
    private static ConfigPanel configPanel;
    private static YamlPanel yamlPanel;
    private static NucleiTemplatePanel nucleiTemplatePanel;
    private static WorkflowPanel workflowPanel;

    private InitUI() {
        throw new AssertionError("InitUI类不应该被实例化");
    }

    /**
     * 装配主窗口界面：面板、菜单栏、主标签页；任一步骤异常时弹窗报错。
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
     * 创建各功能面板实例，供主标签页装配与外部 getter 访问。
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
     * 构建窗口顶部区域：左侧应用 logo，右侧文件、设置、关于菜单。
     */
    private static void initMenuBar(JFrame frame) {
        JMenuBar menuBar = new JMenuBar();
        menuBar.add(createFileMenu(frame));
        menuBar.add(createSettingsMenu(frame));
        menuBar.add(createAboutMenu(frame));

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 0));

        JLabel logo = new JLabel(Icons.appLogo(20));
        logo.setToolTipText("gatherNuclei");
        logo.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 8));
        topBar.add(logo, BorderLayout.WEST);
        topBar.add(menuBar, BorderLayout.CENTER);

        frame.add(topBar, BorderLayout.NORTH);
    }

    /**
     * 构建标题为「打开」的菜单，提供打开系统终端入口。
     */
    private static JMenu createFileMenu(JFrame frame) {
        JMenu fileMenu = new JMenu("打开");
        JMenuItem shellItem = new JMenuItem("shell");
        shellItem.addActionListener(e -> SwingUtilities.invokeLater(() -> openSystemShell(frame)));
        fileMenu.add(shellItem);
        return fileMenu;
    }

    private static JMenu createSettingsMenu(JFrame frame) {
        JMenu settingsMenu = new JMenu("设置");

        JMenuItem proxyItem = new JMenuItem("代理配置");
        proxyItem.setToolTipText("配置 HTTP/SOCKS5 代理");
        proxyItem.addActionListener(e -> {
            ProxyConfigDialog dialog = new ProxyConfigDialog(frame);
            dialog.setVisible(true);
        });
        settingsMenu.add(proxyItem);

        return settingsMenu;
    }

    private static JMenu createAboutMenu(JFrame frame) {
        JMenu aboutMenu = new JMenu("关于");
        JMenuItem aboutItem = new JMenuItem("关于");
        aboutItem.addActionListener(e -> showAboutDialog(frame));
        aboutMenu.add(aboutItem);
        return aboutMenu;
    }

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
     * 按操作系统启动系统终端；Linux 下依次尝试常见终端，全部失败或系统不支持时弹窗提示。
     */
    private static void openSystemShell(JFrame frame) {
        String os = System.getProperty("os.name").toLowerCase();
        try {
            if (os.startsWith("win")) {
                Runtime.getRuntime().exec("cmd /c start cmd");
            } else if (os.startsWith("mac")) {
                Runtime.getRuntime().exec("open -a Terminal");
            } else if (os.contains("linux") || os.contains("unix")) {
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
     * 在事件分发线程上创建主标签页、装入六个功能面板并加入窗口中部；异常时弹窗报错。
     */
    private static void initTabbedPane(JFrame frame) {
        SwingUtilities.invokeLater(() -> {
            try {
                mainTabbedPane = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
                mainTabbedPane.setFont(new Font("微软雅黑", Font.PLAIN, 12));
                styleTabbedPane(mainTabbedPane);

                addTab("模板管理", "template", templatesPanel, "浏览、过滤并生成 Nuclei 模板的运行命令");
                addTab("工作流", "workflow", workflowPanel, "将多个模板组织成工作流并批量生成运行命令");
                addTab("搜索引擎", "search", searchPanel, "多空间测绘引擎集成搜索");
                addTab("配置", "settings", configPanel, "Nuclei 路径参数与各引擎 API Key 配置");
                addTab("新建模板", "newtemplate", yamlPanel, "使用 YAML 编辑器新建或编辑模板");
                addTab("Nuclei", "generator", nucleiTemplatePanel, "通过表单快速生成 Nuclei 模板");

                // 选中项图标用强调色，未选中项用灰色，区分更明显
                Icons.applyTabIcons(mainTabbedPane, MAIN_TAB_ICONS);

                // 切换到工作流标签页时自动刷新，保证新建的工作流能立即显示
                mainTabbedPane.addChangeListener(e -> {
                    if (mainTabbedPane.getSelectedIndex() == WORKFLOW_TAB_INDEX) {
                        workflowPanel.refreshWorkflows();
                    }
                });

                frame.add(mainTabbedPane, BorderLayout.CENTER);
                frame.revalidate();
                frame.repaint();
            } catch (Exception e) {
                showError(frame, "初始化选项卡失败", e);
            }
        });
    }

    /**
     * 统一选项卡外观：下划线选中态、标签分隔线、统一的标签高度与内边距。
     */
    public static void styleTabbedPane(JTabbedPane tabbedPane) {
        styleTabbedPane(tabbedPane, true);
    }

    /**
     * 统一选项卡外观：下划线选中态、固定标签高度与内边距、标签左对齐。
     *
     * @param separators 是否显示标签之间的分隔线
     */
    public static void styleTabbedPane(JTabbedPane tabbedPane, boolean separators) {
        tabbedPane.putClientProperty(FlatClientProperties.TABBED_PANE_TAB_TYPE,
                FlatClientProperties.TABBED_PANE_TAB_TYPE_UNDERLINED);
        tabbedPane.putClientProperty(FlatClientProperties.TABBED_PANE_SHOW_TAB_SEPARATORS, separators);
        tabbedPane.putClientProperty(FlatClientProperties.TABBED_PANE_TAB_HEIGHT, 30);
        tabbedPane.putClientProperty(FlatClientProperties.TABBED_PANE_TAB_INSETS, new Insets(0, 12, 0, 12));
        tabbedPane.putClientProperty(FlatClientProperties.TABBED_PANE_TAB_WIDTH_MODE,
                FlatClientProperties.TABBED_PANE_TAB_WIDTH_MODE_PREFERRED);
        tabbedPane.putClientProperty(FlatClientProperties.TABBED_PANE_TAB_ALIGNMENT,
                FlatClientProperties.TABBED_PANE_ALIGN_LEADING);

        // 选中文字用强调色配合下划线突出选中态；该键写入全局 UIManager，会影响所有选项卡面板
        UIManager.put("TabbedPane.selectedForeground", Icons.SELECTED_COLOR);
    }

    /**
     * 向主标签页追加带图标与提示的标签页；component 为 null 时只打印警告并跳过。
     */
    private static void addTab(String title, String iconName, JComponent component, String tip) {
        if (component != null) {
            mainTabbedPane.addTab(title, Icons.create(iconName), component, tip);
        } else {
            System.err.println("Warning: Attempted to add null component for tab: " + title);
        }
    }

    /**
     * 弹窗展示错误信息；e 非空时打印堆栈并把异常信息拼接到消息末尾。
     *
     * @param e 可为 null，表示无异常详情的纯提示
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

    private static void showMessage(Component parent, String message, String title) {
        JOptionPane.showMessageDialog(
                parent,
                message,
                title,
                JOptionPane.INFORMATION_MESSAGE
        );
    }

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