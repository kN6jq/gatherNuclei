package org.jiu.ui;

import org.jiu.utils.Utils;
import org.jiu.utils.YamlUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;


public class ConfigPanel extends JPanel {
    // 面板组件
    private final JPanel mainPanel;
    private final Map<String, JTextField> textFields;

    // Nuclei配置组件
    private JTextField templatePathField;
    private JTextField templateArgField;

    // Fofa配置组件
    private JTextField fofaUrlField;
    private JPasswordField fofaEmailField;
    private JPasswordField fofaKeyField;

    // Hunter配置组件
    private JTextField hunterUrlField;
    private JPasswordField hunterKeyField;

    // Zone配置组件
    private JTextField zoneUrlField;
    private JPasswordField zoneKeyField;

    // DayDayMap配置组件
    private JTextField daydaymapUrlField;
    private JPasswordField daydaymapKeyField;

    public ConfigPanel() {
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(10, 10, 10, 10));

        mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        textFields = new HashMap<>();

        initializeComponents();
        loadSavedConfig();

        JScrollPane scrollPane = new JScrollPane(mainPanel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);
    }

    private void initializeComponents() {
        // Nuclei配置部分
        mainPanel.add(createNucleiConfigPanel());
        mainPanel.add(Box.createVerticalStrut(10));

        // Fofa配置部分
        mainPanel.add(createFofaConfigPanel());
        mainPanel.add(Box.createVerticalStrut(10));

        // Hunter配置部分
        mainPanel.add(createHunterConfigPanel());
        mainPanel.add(Box.createVerticalStrut(10));

        // Zone配置部分
        mainPanel.add(createZoneConfigPanel());
        mainPanel.add(Box.createVerticalStrut(10));

        // DayDayMap配置部分
        mainPanel.add(createDayDayMapConfigPanel());
    }

    private JPanel createNucleiConfigPanel() {
        JPanel panel = createConfigPanel("Nuclei 配置");

        // 模板路径配置
        templatePathField = addConfigField(panel, "模板路径:", 30);
        textFields.put("nucleipath", templatePathField);

        // 参数配置
        templateArgField = addConfigField(panel, "Nuclei参数:", 30);
        textFields.put("nucleiarg", templateArgField);

        // 保存按钮
        JButton saveButton = new JButton("保存");
        saveButton.addActionListener(e -> saveNucleiConfig());
        panel.add(saveButton);

        return panel;
    }

    private JPanel createFofaConfigPanel() {
        JPanel panel = createConfigPanel("Fofa 配置");

        // URL配置
        fofaUrlField = addConfigField(panel, "API URL:", 20);
        textFields.put("fofaurl", fofaUrlField);

        // Email配置
        fofaEmailField = addPasswordField(panel, "Email:", 10);
        textFields.put("fofaemail", fofaEmailField);

        // Key配置
        fofaKeyField = addPasswordField(panel, "API Key:", 10);
        textFields.put("fofakey", fofaKeyField);

        // 保存按钮
        JButton saveButton = new JButton("保存");
        saveButton.addActionListener(e -> saveFofaConfig());
        panel.add(saveButton);

        return panel;
    }

    private JPanel createHunterConfigPanel() {
        JPanel panel = createConfigPanel("Hunter 配置");

        // URL配置
        hunterUrlField = addConfigField(panel, "API URL:", 20);
        textFields.put("hunterurl", hunterUrlField);

        // Key配置
        hunterKeyField = addPasswordField(panel, "API Key:", 30);
        textFields.put("hunterkey", hunterKeyField);

        // 保存按钮
        JButton saveButton = new JButton("保存");
        saveButton.addActionListener(e -> saveHunterConfig());
        panel.add(saveButton);

        return panel;
    }

    private JPanel createZoneConfigPanel() {
        JPanel panel = createConfigPanel("Zone 配置");

        // URL配置
        zoneUrlField = addConfigField(panel, "API URL:", 20);
        textFields.put("zoneurl", zoneUrlField);

        // Key配置
        zoneKeyField = addPasswordField(panel, "API Key:", 20);
        textFields.put("zonekey", zoneKeyField);

        // 保存按钮
        JButton saveButton = new JButton("保存");
        saveButton.addActionListener(e -> saveZoneConfig());
        panel.add(saveButton);

        return panel;
    }

    private JPanel createDayDayMapConfigPanel() {
        JPanel panel = createConfigPanel("DayDayMap 配置");

        // URL配置
        daydaymapUrlField = addConfigField(panel, "API URL:", 20);
        textFields.put("daydaymapurl", daydaymapUrlField);

        // Key配置
        daydaymapKeyField = addPasswordField(panel, "API Key:", 20);
        textFields.put("daydaymapkey", daydaymapKeyField);

        // 保存按钮
        JButton saveButton = new JButton("保存");
        saveButton.addActionListener(e -> saveDayDayMapConfig());
        panel.add(saveButton);

        return panel;
    }

    private JPanel createConfigPanel(String title) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        panel.setBorder(BorderFactory.createCompoundBorder(
                new TitledBorder(title),
                new EmptyBorder(5, 5, 5, 5)
        ));
        return panel;
    }

    private JTextField addConfigField(JPanel panel, String labelText, int columns) {
        JLabel label = new JLabel(labelText);
        label.setPreferredSize(new Dimension(80, 25));
        panel.add(label);

        JTextField textField = new JTextField(columns);
        textField.setPreferredSize(new Dimension(textField.getPreferredSize().width, 25));
        panel.add(textField);

        return textField;
    }

    private JPasswordField addPasswordField(JPanel panel, String labelText, int columns) {
        JLabel label = new JLabel(labelText);
        label.setPreferredSize(new Dimension(80, 25));
        panel.add(label);

        JPasswordField passwordField = new JPasswordField(columns);
        passwordField.setPreferredSize(new Dimension(passwordField.getPreferredSize().width, 25));
        panel.add(passwordField);

        return passwordField;
    }

    private void loadSavedConfig() {
        SwingUtilities.invokeLater(() -> {
            // Nuclei配置
            templatePathField.setText(Utils.templatePath);
            templateArgField.setText(Utils.templateArg);

            // Fofa配置
            fofaUrlField.setText(Utils.fofaUrl);
            fofaEmailField.setText(Utils.fofaEmail);
            fofaKeyField.setText(Utils.fofaKey);

            // Hunter配置
            hunterUrlField.setText(Utils.hunterUrl);
            hunterKeyField.setText(Utils.hunterKey);

            // Zone配置
            zoneUrlField.setText(Utils.zoneUrl);
            zoneKeyField.setText(Utils.zoneKey);

            // DayDayMap配置
            daydaymapUrlField.setText(Utils.daydaymapUrl);
            daydaymapKeyField.setText(Utils.daydaymapKey);
        });
    }

    private void saveNucleiConfig() {
        String path = templatePathField.getText().trim();
        String arg = templateArgField.getText().trim();

        saveConfig("nucleipath", path);
        saveConfig("nucleiarg", arg);

        Utils.templatePath = path;
        Utils.templateArg = arg;

        refreshTemplates();
    }

    private void saveFofaConfig() {
        String url = fofaUrlField.getText().trim();
        String email = new String(fofaEmailField.getPassword());
        String key = new String(fofaKeyField.getPassword());

        saveConfig("fofaurl", url);
        saveConfig("fofaemail", email);
        saveConfig("fofakey", key);

        Utils.fofaUrl = url;
        Utils.fofaEmail = email;
        Utils.fofaKey = key;

        showSaveSuccess();
    }

    private void saveHunterConfig() {
        String url = hunterUrlField.getText().trim();
        String key = new String(hunterKeyField.getPassword());

        saveConfig("hunterurl", url);
        saveConfig("hunterkey", key);

        Utils.hunterUrl = url;
        Utils.hunterKey = key;

        showSaveSuccess();
    }

    private void saveZoneConfig() {
        String url = zoneUrlField.getText().trim();
        String key = new String(zoneKeyField.getPassword());

        saveConfig("zoneurl", url);
        saveConfig("zonekey", key);

        Utils.zoneUrl = url;
        Utils.zoneKey = key;

        showSaveSuccess();
    }

    private void saveDayDayMapConfig() {
        String url = daydaymapUrlField.getText().trim();
        String key = new String(daydaymapKeyField.getPassword());

        saveConfig("daydaymapurl", url);
        saveConfig("daydaymapkey", key);

        Utils.daydaymapUrl = url;
        Utils.daydaymapKey = key;

        showSaveSuccess();
    }

    private void saveConfig(String key, String value) {
        try {
            YamlUtils.modifyYaml(key, value);
        } catch (Exception e) {
            showError("保存配置失败", e);
        }
    }

    private void refreshTemplates() {
        SwingUtilities.invokeLater(() -> {
            try {
                InitUI.getTemplatesPanel().filterData();
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