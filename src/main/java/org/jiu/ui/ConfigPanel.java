package org.jiu.ui;

import org.jiu.utils.UIUtils;
import org.jiu.utils.Utils;
import org.jiu.utils.YamlUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * 配置面板：维护 Nuclei 运行参数与各测绘引擎的 API 地址、Key，并写入 YAML 配置文件。
 * 各配置区块共用同一构建方法，字体、尺寸与按钮样式统一由 UIUtils 提供。
 */
public class ConfigPanel extends JPanel {
    private final JPanel mainPanel;

    private JTextField templatePathField;
    private JTextField templateArgField;

    private JTextField fofaUrlField;
    private JPasswordField fofaKeyField;

    private JTextField hunterUrlField;
    private JPasswordField hunterKeyField;

    private JTextField zoneUrlField;
    private JPasswordField zoneKeyField;

    private JTextField daydaymapUrlField;
    private JPasswordField daydaymapKeyField;

    /**
     * 构建可滚动的配置界面，并异步回填已保存的配置值。
     */
    public ConfigPanel() {
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(15, 15, 15, 15));

        mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        initializeComponents();
        loadSavedConfig();

        JScrollPane scrollPane = new JScrollPane(mainPanel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);
    }

    /**
     * 按 Nuclei、FOFA、Hunter、Zone、DayDayMap 的顺序纵向排列各配置区块。
     */
    private void initializeComponents() {
        mainPanel.add(createNucleiConfigPanel());
        mainPanel.add(Box.createVerticalStrut(10));

        fofaUrlField = new JTextField();
        fofaKeyField = new JPasswordField();
        mainPanel.add(createEngineConfigPanel("Fofa 配置", fofaUrlField, fofaKeyField,
                "保存FOFA配置", this::saveFofaConfig));
        mainPanel.add(Box.createVerticalStrut(10));

        hunterUrlField = new JTextField();
        hunterKeyField = new JPasswordField();
        mainPanel.add(createEngineConfigPanel("Hunter 配置", hunterUrlField, hunterKeyField,
                "保存Hunter配置", this::saveHunterConfig));
        mainPanel.add(Box.createVerticalStrut(10));

        zoneUrlField = new JTextField();
        zoneKeyField = new JPasswordField();
        mainPanel.add(createEngineConfigPanel("Zone 配置", zoneUrlField, zoneKeyField,
                "保存Zone配置", this::saveZoneConfig));
        mainPanel.add(Box.createVerticalStrut(10));

        daydaymapUrlField = new JTextField();
        daydaymapKeyField = new JPasswordField();
        mainPanel.add(createEngineConfigPanel("DayDayMap 配置", daydaymapUrlField, daydaymapKeyField,
                "保存DayDayMap配置", this::saveDayDayMapConfig));
    }

    /**
     * 创建带标题边框与内边距的配置区块容器。
     */
    private JPanel createConfigPanel(String title) {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(UIUtils.createTitledBorder(title));
        return panel;
    }

    /**
     * 向区块追加一行「右对齐标签 + 输入框」，两者分别占据第 0、1 列。
     * 输入框高度统一 30，首选宽度置 0 以便随面板横向拉伸。
     */
    private void addConfigField(JPanel panel, String labelText, Component field,
                                GridBagConstraints gbc, int row) {
        gbc.insets = new Insets(5, 10, 5, 10);
        gbc.gridy = row;

        JLabel label = new JLabel(labelText);
        UIUtils.styleLabel(label);
        label.setPreferredSize(new Dimension(120, UIUtils.INPUT_HEIGHT));
        label.setHorizontalAlignment(SwingConstants.RIGHT);

        gbc.gridx = 0;
        gbc.weightx = 0.0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(label, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;

        if (field instanceof JTextField) {
            UIUtils.styleTextField((JTextField) field, 760);
        }
        panel.add(field, gbc);
    }

    /**
     * 构建 Nuclei 区块：模板路径、运行参数输入框与保存按钮。
     */
    private JPanel createNucleiConfigPanel() {
        JPanel panel = createConfigPanel("Nuclei 配置");
        GridBagConstraints gbc = new GridBagConstraints();
        JPanel inputPanel = new JPanel(new GridBagLayout());

        templatePathField = new JTextField();
        addConfigField(inputPanel, "模板路径:", templatePathField, gbc, 0);

        templateArgField = new JTextField();
        addConfigField(inputPanel, "Nuclei参数:", templateArgField, gbc, 1);

        JButton saveButton = new JButton();
        UIUtils.stylePrimaryButton(saveButton, "保存Nuclei配置");
        saveButton.addActionListener(e -> saveNucleiConfig());

        // 保存按钮放在输入框右下方，右缘与输入框对齐
        GridBagConstraints buttonGbc = new GridBagConstraints();
        buttonGbc.gridx = 1;
        buttonGbc.gridy = 2;
        buttonGbc.weightx = 0.0;
        buttonGbc.fill = GridBagConstraints.NONE;
        buttonGbc.anchor = GridBagConstraints.EAST;
        buttonGbc.insets = new Insets(10, 10, 0, 10);
        inputPanel.add(saveButton, buttonGbc);

        panel.add(inputPanel, BorderLayout.CENTER);
        return panel;
    }

    /**
     * 构建引擎配置区块：API 地址、Key 与保存按钮，各引擎共用此结构。
     */
    private JPanel createEngineConfigPanel(String title, JTextField urlField, JPasswordField keyField,
                                           String saveText, Runnable onSave) {
        JPanel panel = createConfigPanel(title);
        GridBagConstraints gbc = new GridBagConstraints();
        JPanel inputPanel = new JPanel(new GridBagLayout());

        addConfigField(inputPanel, "API URL:", urlField, gbc, 0);
        addConfigField(inputPanel, "API Key:", keyField, gbc, 1);

        JButton saveButton = new JButton();
        UIUtils.stylePrimaryButton(saveButton, saveText);
        saveButton.addActionListener(e -> onSave.run());

        // 保存按钮放在输入框右下方，右缘与输入框对齐
        GridBagConstraints buttonGbc = new GridBagConstraints();
        buttonGbc.gridx = 1;
        buttonGbc.gridy = 2;
        buttonGbc.weightx = 0.0;
        buttonGbc.fill = GridBagConstraints.NONE;
        buttonGbc.anchor = GridBagConstraints.EAST;
        buttonGbc.insets = new Insets(10, 10, 0, 10);
        inputPanel.add(saveButton, buttonGbc);

        panel.add(inputPanel, BorderLayout.CENTER);
        return panel;
    }

    /**
     * 在事件分发线程上把 Utils 中当前的配置值回填到各输入框。
     */
    private void loadSavedConfig() {
        SwingUtilities.invokeLater(() -> {
            templatePathField.setText(Utils.templatePath);
            templateArgField.setText(Utils.templateArg);

            fofaUrlField.setText(Utils.fofaUrl);
            fofaKeyField.setText(Utils.fofaKey);

            hunterUrlField.setText(Utils.hunterUrl);
            hunterKeyField.setText(Utils.hunterKey);

            zoneUrlField.setText(Utils.zoneUrl);
            zoneKeyField.setText(Utils.zoneKey);

            daydaymapUrlField.setText(Utils.daydaymapUrl);
            daydaymapKeyField.setText(Utils.daydaymapKey);
        });
    }

    /**
     * 写入 Nuclei 路径与参数（去除首尾空白），同步到 Utils 后刷新模板列表。
     */
    private void saveNucleiConfig() {
        String path = templatePathField.getText().trim();
        String arg = templateArgField.getText().trim();

        saveConfig("nucleipath", path);
        saveConfig("nucleiarg", arg);

        Utils.templatePath = path;
        Utils.templateArg = arg;

        refreshTemplates();
    }

    /**
     * 写入 FOFA 地址与 Key 并同步到 Utils，随后弹出保存成功提示。
     */
    private void saveFofaConfig() {
        String url = fofaUrlField.getText().trim();
        String key = new String(fofaKeyField.getPassword());

        saveConfig("fofaurl", url);
        saveConfig("fofakey", key);

        Utils.fofaUrl = url;
        Utils.fofaKey = key;

        showSaveSuccess();
    }

    /**
     * 写入 Hunter 地址与 Key 并同步到 Utils，随后弹出保存成功提示。
     */
    private void saveHunterConfig() {
        String url = hunterUrlField.getText().trim();
        String key = new String(hunterKeyField.getPassword());

        saveConfig("hunterurl", url);
        saveConfig("hunterkey", key);

        Utils.hunterUrl = url;
        Utils.hunterKey = key;

        showSaveSuccess();
    }

    /**
     * 写入 Zone 地址与 Key 并同步到 Utils，随后弹出保存成功提示。
     */
    private void saveZoneConfig() {
        String url = zoneUrlField.getText().trim();
        String key = new String(zoneKeyField.getPassword());

        saveConfig("zoneurl", url);
        saveConfig("zonekey", key);

        Utils.zoneUrl = url;
        Utils.zoneKey = key;

        showSaveSuccess();
    }

    /**
     * 写入 DayDayMap 地址与 Key 并同步到 Utils，随后弹出保存成功提示。
     */
    private void saveDayDayMapConfig() {
        String url = daydaymapUrlField.getText().trim();
        String key = new String(daydaymapKeyField.getPassword());

        saveConfig("daydaymapurl", url);
        saveConfig("daydaymapkey", key);

        Utils.daydaymapUrl = url;
        Utils.daydaymapKey = key;

        showSaveSuccess();
    }

    /**
     * 把单个配置项写入 YAML 文件，异常时弹窗报错且不向上抛出。
     *
     * @param key YAML 中的配置键，如 fofaurl、nucleipath
     */
    private void saveConfig(String key, String value) {
        try {
            YamlUtils.modifyYaml(key, value);
        } catch (Exception e) {
            showError("保存配置失败", e);
        }
    }

    /**
     * 在事件分发线程上刷新模板面板，成功后提示保存成功，异常时弹窗报错。
     */
    private void refreshTemplates() {
        SwingUtilities.invokeLater(() -> {
            try {
                InitUI.getTemplatesPanel().refreshTemplates();
                showSaveSuccess();
            } catch (Exception e) {
                showError("刷新模板失败", e);
            }
        });
    }

    private void showSaveSuccess() {
        SwingUtilities.invokeLater(() ->
                JOptionPane.showMessageDialog(
                        this,
                        "配置保存成功",
                        "提示",
                        JOptionPane.INFORMATION_MESSAGE
                )
        );
    }

    private void showError(String message, Exception e) {
        e.printStackTrace();
        SwingUtilities.invokeLater(() ->
                JOptionPane.showMessageDialog(
                        this,
                        message + ": " + e.getMessage(),
                        "错误",
                        JOptionPane.ERROR_MESSAGE
                )
        );
    }
}
