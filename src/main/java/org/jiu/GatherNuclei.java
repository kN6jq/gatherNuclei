package org.jiu;

import com.formdev.flatlaf.FlatLightLaf;
import org.jiu.ui.InitUI;
import org.jiu.ui.component.Icons;
import org.jiu.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

import static org.jiu.utils.YamlUtils.parseConfig;

/**
 * 程序主窗口，负责启动流程：加载主题与配置、组装 UI 并显示底部状态栏。
 */
public class GatherNuclei extends JFrame {
    private static final String TITLE = "gatherNuclei";
    private static final int DEFAULT_WIDTH = 1200;
    private static final int DEFAULT_HEIGHT = 700;
    private static final int MIN_WIDTH = 800;
    private static final int MIN_HEIGHT = 600;

    public GatherNuclei() {
        initFrame();
    }

    /**
     * 程序入口，UI 的创建与展示必须在 Swing 事件分发线程（EDT）上执行。
     */
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

    /**
     * 安装 FlatLaf 主题并解析配置文件，主题初始化失败时抛出异常。
     */
    private static void setupUI() throws Exception {
        if (!FlatLightLaf.setup()) {
            throw new Exception("无法初始化FlatLightLaf主题");
        }

        parseConfig();
    }

    /**
     * 组装并显示主窗口，初始化失败时弹窗提示。
     */
    private static void createAndShowGUI() {
        GatherNuclei frame = new GatherNuclei();

        try {
            InitUI.initializeUI(frame);

            frame.add(createStatusBar(), BorderLayout.SOUTH);

            frame.setVisible(true);

        } catch (Exception e) {
            showError(frame, "UI初始化失败", e);
        }
    }

    /**
     * 创建底部状态栏：左侧状态文案，右侧版本信息。
     */
    private static JPanel createStatusBar() {
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBorder(BorderFactory.createLoweredSoftBevelBorder());

        JLabel leftLabel = new JLabel("就绪");
        leftLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
        leftLabel.setFont(UIUtils.LABEL_FONT);

        JLabel rightLabel = new JLabel("gatherNuclei v1.0.0");
        rightLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
        rightLabel.setFont(UIUtils.LABEL_FONT);

        statusBar.add(leftLabel, BorderLayout.WEST);
        statusBar.add(rightLabel, BorderLayout.EAST);

        return statusBar;
    }

    /**
     * 弹出错误对话框；传入异常时打印堆栈并把异常信息附加到提示文案。
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
     * 设置窗口标题、图标、默认与最小尺寸、居中位置及关闭行为。
     */
    private void initFrame() {
        setTitle(TITLE);
        setLayout(new BorderLayout());

        // setIconImage 同时决定标题栏与任务栏图标
        setIconImage(Icons.appLogo().getImage());

        setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        setMinimumSize(new Dimension(MIN_WIDTH, MIN_HEIGHT));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    }
}