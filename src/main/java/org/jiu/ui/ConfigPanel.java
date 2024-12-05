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
        setBorder(new EmptyBorder(15, 15, 15, 15));

        mainPanel = new JPanel();
        mainPanel.setLayout(new GridBagLayout());

        textFields = new HashMap<>();

        initializeComponents();
        loadSavedConfig();

        JScrollPane scrollPane = new JScrollPane(mainPanel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);
    }

    private void initializeComponents() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 10, 0);

        mainPanel.add(createNucleiConfigPanel(), gbc);

        gbc.gridy++;
        mainPanel.add(createFofaConfigPanel(), gbc);

        gbc.gridy++;
        mainPanel.add(createHunterConfigPanel(), gbc);

        gbc.gridy++;
        mainPanel.add(createZoneConfigPanel(), gbc);

        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 0, 0);
        mainPanel.add(createDayDayMapConfigPanel(), gbc);
    }

    private JPanel createConfigPanel(String title) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY), title),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        return panel;
    }

    private Component addConfigField(JPanel panel, String labelText, Component field, GridBagConstraints gbc) {
        JLabel label = new JLabel(labelText);
        label.setPreferredSize(new Dimension(100, 28));

        gbc.gridx = 0;
        gbc.weightx = 0.0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(label, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        if (field instanceof JTextField) {
            ((JTextField) field).setPreferredSize(new Dimension(0, 28));
        }
        panel.add(field, gbc);

        return field;
    }

    private JPanel createNucleiConfigPanel() {
        JPanel panel = createConfigPanel("Nuclei 配置");
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 5, 0);

        templatePathField = new JTextField();
        addConfigField(panel, "模板路径:", templatePathField, gbc);
        textFields.put("nucleipath", templatePathField);

        gbc.gridy++;
        templateArgField = new JTextField();
        addConfigField(panel, "Nuclei参数:", templateArgField, gbc);
        textFields.put("nucleiarg", templateArgField);

        gbc.gridy++;
        gbc.insets = new Insets(10, 0, 0, 0);
        gbc.anchor = GridBagConstraints.EAST;
        JButton saveButton = new JButton("保存配置");
        saveButton.setPreferredSize(new Dimension(100, 30));
        saveButton.addActionListener(e -> saveNucleiConfig());
        panel.add(saveButton, gbc);

        return panel;
    }

    private JPanel createFofaConfigPanel() {
        JPanel panel = createConfigPanel("Fofa 配置");
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 5, 0);

        fofaUrlField = new JTextField();
        addConfigField(panel, "API URL:", fofaUrlField, gbc);
        textFields.put("fofaurl", fofaUrlField);

        gbc.gridy++;
        fofaEmailField = new JPasswordField();
        addConfigField(panel, "Email:", fofaEmailField, gbc);
        textFields.put("fofaemail", fofaEmailField);

        gbc.gridy++;
        fofaKeyField = new JPasswordField();
        addConfigField(panel, "API Key:", fofaKeyField, gbc);
        textFields.put("fofakey", fofaKeyField);

        gbc.gridy++;
        gbc.insets = new Insets(10, 0, 0, 0);
        gbc.anchor = GridBagConstraints.EAST;
        JButton saveButton = new JButton("保存配置");
        saveButton.setPreferredSize(new Dimension(100, 30));
        saveButton.addActionListener(e -> saveFofaConfig());
        panel.add(saveButton, gbc);

        return panel;
    }

    private JPanel createHunterConfigPanel() {
        JPanel panel = createConfigPanel("Hunter 配置");
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 5, 0);

        hunterUrlField = new JTextField();
        addConfigField(panel, "API URL:", hunterUrlField, gbc);
        textFields.put("hunterurl", hunterUrlField);

        gbc.gridy++;
        hunterKeyField = new JPasswordField();
        addConfigField(panel, "API Key:", hunterKeyField, gbc);
        textFields.put("hunterkey", hunterKeyField);

        gbc.gridy++;
        gbc.insets = new Insets(10, 0, 0, 0);
        gbc.anchor = GridBagConstraints.EAST;
        JButton saveButton = new JButton("保存配置");
        saveButton.setPreferredSize(new Dimension(100, 30));
        saveButton.addActionListener(e -> saveHunterConfig());
        panel.add(saveButton, gbc);

        return panel;
    }

    private JPanel createZoneConfigPanel() {
        JPanel panel = createConfigPanel("Zone 配置");
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 5, 0);

        zoneUrlField = new JTextField();
        addConfigField(panel, "API URL:", zoneUrlField, gbc);
        textFields.put("zoneurl", zoneUrlField);

        gbc.gridy++;
        zoneKeyField = new JPasswordField();
        addConfigField(panel, "API Key:", zoneKeyField, gbc);
        textFields.put("zonekey", zoneKeyField);

        gbc.gridy++;
        gbc.insets = new Insets(10, 0, 0, 0);
        gbc.anchor = GridBagConstraints.EAST;
        JButton saveButton = new JButton("保存配置");
        saveButton.setPreferredSize(new Dimension(100, 30));
        saveButton.addActionListener(e -> saveZoneConfig());
        panel.add(saveButton, gbc);

        return panel;
    }

    private JPanel createDayDayMapConfigPanel() {
        JPanel panel = createConfigPanel("DayDayMap 配置");
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 5, 0);

        daydaymapUrlField = new JTextField();
        addConfigField(panel, "API URL:", daydaymapUrlField, gbc);
        textFields.put("daydaymapurl", daydaymapUrlField);

        gbc.gridy++;
        daydaymapKeyField = new JPasswordField();
        addConfigField(panel, "API Key:", daydaymapKeyField, gbc);
        textFields.put("daydaymapkey", daydaymapKeyField);

        gbc.gridy++;
        gbc.insets = new Insets(10, 0, 0, 0);
        gbc.anchor = GridBagConstraints.EAST;
        JButton saveButton = new JButton("保存配置");
        saveButton.setPreferredSize(new Dimension(100, 30));
        saveButton.addActionListener(e -> saveDayDayMapConfig());
        panel.add(saveButton, gbc);

        return panel;
    }

    private void loadSavedConfig() {
        SwingUtilities.invokeLater(() -> {
            templatePathField.setText(Utils.templatePath);
            templateArgField.setText(Utils.templateArg);

            fofaUrlField.setText(Utils.fofaUrl);
            fofaEmailField.setText(Utils.fofaEmail);
            fofaKeyField.setText(Utils.fofaKey);

            hunterUrlField.setText(Utils.hunterUrl);
            hunterKeyField.setText(Utils.hunterKey);

            zoneUrlField.setText(Utils.zoneUrl);
            zoneKeyField.setText(Utils.zoneKey);

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