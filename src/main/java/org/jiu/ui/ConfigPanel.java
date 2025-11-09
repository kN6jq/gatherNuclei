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
        mainPanel.add(createNucleiConfigPanel());
        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(createFofaConfigPanel());
        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(createHunterConfigPanel());
        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(createZoneConfigPanel());
        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(createDayDayMapConfigPanel());
    }

    private JPanel createConfigPanel(String title) {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)), 
            title,
            TitledBorder.LEFT,
            TitledBorder.TOP,
            new Font("微软雅黑", Font.BOLD, 14),
            new Color(50, 50, 50)
        ));
        panel.setBorder(BorderFactory.createCompoundBorder(
                panel.getBorder(),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        return panel;
    }

    private Component addConfigField(JPanel panel, String labelText, Component field, GridBagConstraints gbc) {
        gbc.insets = new Insets(5, 10, 5, 10);
        
        JLabel label = new JLabel(labelText);
        label.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        label.setPreferredSize(new Dimension(120, 25));
        label.setHorizontalAlignment(SwingConstants.RIGHT);

        gbc.gridx = 0;
        gbc.weightx = 0.0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(label, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        
        if (field instanceof JTextField) {
            ((JTextField) field).setPreferredSize(new Dimension(0, 25));
        } else if (field instanceof JPasswordField) {
            ((JPasswordField) field).setPreferredSize(new Dimension(0, 25));
        }
        
        panel.add(field, gbc);

        return field;
    }

    private JPanel createNucleiConfigPanel() {
        JPanel panel = createConfigPanel("Nuclei 配置");
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // 使用GridBagLayout添加配置项
        JPanel inputPanel = new JPanel(new GridBagLayout());
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // 模板路径
        JLabel pathLabel = new JLabel("模板路径:");
        pathLabel.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        pathLabel.setPreferredSize(new Dimension(100, 25));
        pathLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        gbc.gridx = 0;
        gbc.gridy = 0;
        inputPanel.add(pathLabel, gbc);
        
        templatePathField = new JTextField();
        templatePathField.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        templatePathField.setPreferredSize(new Dimension(300, 25));
        gbc.gridx = 1;
        inputPanel.add(templatePathField, gbc);
        textFields.put("nucleipath", templatePathField);
        
        // Nuclei参数
        JLabel argLabel = new JLabel("Nuclei参数:");
        argLabel.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        argLabel.setPreferredSize(new Dimension(100, 25));
        argLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        gbc.gridx = 0;
        gbc.gridy = 1;
        inputPanel.add(argLabel, gbc);
        
        templateArgField = new JTextField();
        templateArgField.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        templateArgField.setPreferredSize(new Dimension(300, 25));
        gbc.gridx = 1;
        inputPanel.add(templateArgField, gbc);
        textFields.put("nucleiarg", templateArgField);
        
        // 保存按钮
        JButton saveButton = new JButton("保存Nuclei配置");
        saveButton.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        saveButton.setPreferredSize(new Dimension(120, 30));
        saveButton.setBackground(new Color(0, 123, 255));
        saveButton.setForeground(Color.WHITE);
        saveButton.setFocusPainted(false);
        saveButton.addActionListener(e -> saveNucleiConfig());
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(saveButton);
        
        panel.add(inputPanel, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        return panel;
    }

    private JPanel createFofaConfigPanel() {
        JPanel panel = createConfigPanel("Fofa 配置");
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // 使用GridBagLayout添加配置项
        JPanel inputPanel = new JPanel(new GridBagLayout());
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // API URL
        JLabel urlLabel = new JLabel("API URL:");
        urlLabel.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        urlLabel.setPreferredSize(new Dimension(100, 25));
        urlLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        gbc.gridx = 0;
        gbc.gridy = 0;
        inputPanel.add(urlLabel, gbc);
        
        fofaUrlField = new JTextField();
        fofaUrlField.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        fofaUrlField.setPreferredSize(new Dimension(300, 25));
        gbc.gridx = 1;
        inputPanel.add(fofaUrlField, gbc);
        textFields.put("fofaurl", fofaUrlField);

        // API Key
        JLabel keyLabel = new JLabel("API Key:");
        keyLabel.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        keyLabel.setPreferredSize(new Dimension(100, 25));
        keyLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        gbc.gridx = 0;
        gbc.gridy = 1;
        inputPanel.add(keyLabel, gbc);
        
        fofaKeyField = new JPasswordField();
        fofaKeyField.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        fofaKeyField.setPreferredSize(new Dimension(300, 25));
        gbc.gridx = 1;
        inputPanel.add(fofaKeyField, gbc);
        textFields.put("fofakey", fofaKeyField);
        
        // 保存按钮
        JButton saveButton = new JButton("保存FOFA配置");
        saveButton.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        saveButton.setPreferredSize(new Dimension(120, 30));
        saveButton.setBackground(new Color(0, 123, 255));
        saveButton.setForeground(Color.WHITE);
        saveButton.setFocusPainted(false);
        saveButton.addActionListener(e -> saveFofaConfig());
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(saveButton);
        
        panel.add(inputPanel, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        return panel;
    }

    private JPanel createHunterConfigPanel() {
        JPanel panel = createConfigPanel("Hunter 配置");
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // 使用GridBagLayout添加配置项
        JPanel inputPanel = new JPanel(new GridBagLayout());
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // API URL
        JLabel urlLabel = new JLabel("API URL:");
        urlLabel.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        urlLabel.setPreferredSize(new Dimension(100, 25));
        urlLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        gbc.gridx = 0;
        gbc.gridy = 0;
        inputPanel.add(urlLabel, gbc);
        
        hunterUrlField = new JTextField();
        hunterUrlField.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        hunterUrlField.setPreferredSize(new Dimension(300, 25));
        gbc.gridx = 1;
        inputPanel.add(hunterUrlField, gbc);
        textFields.put("hunterurl", hunterUrlField);
        
        // API Key
        JLabel keyLabel = new JLabel("API Key:");
        keyLabel.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        keyLabel.setPreferredSize(new Dimension(100, 25));
        keyLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        gbc.gridx = 0;
        gbc.gridy = 1;
        inputPanel.add(keyLabel, gbc);
        
        hunterKeyField = new JPasswordField();
        hunterKeyField.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        hunterKeyField.setPreferredSize(new Dimension(300, 25));
        gbc.gridx = 1;
        inputPanel.add(hunterKeyField, gbc);
        textFields.put("hunterkey", hunterKeyField);
        
        // 保存按钮
        JButton saveButton = new JButton("保存Hunter配置");
        saveButton.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        saveButton.setPreferredSize(new Dimension(120, 30));
        saveButton.setBackground(new Color(0, 123, 255));
        saveButton.setForeground(Color.WHITE);
        saveButton.setFocusPainted(false);
        saveButton.addActionListener(e -> saveHunterConfig());
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(saveButton);
        
        panel.add(inputPanel, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        return panel;
    }

    private JPanel createZoneConfigPanel() {
        JPanel panel = createConfigPanel("Zone 配置");
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // 使用GridBagLayout添加配置项
        JPanel inputPanel = new JPanel(new GridBagLayout());
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // API URL
        JLabel urlLabel = new JLabel("API URL:");
        urlLabel.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        urlLabel.setPreferredSize(new Dimension(100, 25));
        urlLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        gbc.gridx = 0;
        gbc.gridy = 0;
        inputPanel.add(urlLabel, gbc);
        
        zoneUrlField = new JTextField();
        zoneUrlField.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        zoneUrlField.setPreferredSize(new Dimension(300, 25));
        gbc.gridx = 1;
        inputPanel.add(zoneUrlField, gbc);
        textFields.put("zoneurl", zoneUrlField);
        
        // API Key
        JLabel keyLabel = new JLabel("API Key:");
        keyLabel.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        keyLabel.setPreferredSize(new Dimension(100, 25));
        keyLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        gbc.gridx = 0;
        gbc.gridy = 1;
        inputPanel.add(keyLabel, gbc);
        
        zoneKeyField = new JPasswordField();
        zoneKeyField.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        zoneKeyField.setPreferredSize(new Dimension(300, 25));
        gbc.gridx = 1;
        inputPanel.add(zoneKeyField, gbc);
        textFields.put("zonekey", zoneKeyField);
        
        // 保存按钮
        JButton saveButton = new JButton("保存Zone配置");
        saveButton.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        saveButton.setPreferredSize(new Dimension(120, 30));
        saveButton.setBackground(new Color(0, 123, 255));
        saveButton.setForeground(Color.WHITE);
        saveButton.setFocusPainted(false);
        saveButton.addActionListener(e -> saveZoneConfig());
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(saveButton);
        
        panel.add(inputPanel, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        return panel;
    }

    private JPanel createDayDayMapConfigPanel() {
        JPanel panel = createConfigPanel("DayDayMap 配置");
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // 使用GridBagLayout添加配置项
        JPanel inputPanel = new JPanel(new GridBagLayout());
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // API URL
        JLabel urlLabel = new JLabel("API URL:");
        urlLabel.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        urlLabel.setPreferredSize(new Dimension(100, 25));
        urlLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        gbc.gridx = 0;
        gbc.gridy = 0;
        inputPanel.add(urlLabel, gbc);
        
        daydaymapUrlField = new JTextField();
        daydaymapUrlField.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        daydaymapUrlField.setPreferredSize(new Dimension(300, 25));
        gbc.gridx = 1;
        inputPanel.add(daydaymapUrlField, gbc);
        textFields.put("daydaymapurl", daydaymapUrlField);
        
        // API Key
        JLabel keyLabel = new JLabel("API Key:");
        keyLabel.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        keyLabel.setPreferredSize(new Dimension(100, 25));
        keyLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        gbc.gridx = 0;
        gbc.gridy = 1;
        inputPanel.add(keyLabel, gbc);
        
        daydaymapKeyField = new JPasswordField();
        daydaymapKeyField.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        daydaymapKeyField.setPreferredSize(new Dimension(300, 25));
        gbc.gridx = 1;
        inputPanel.add(daydaymapKeyField, gbc);
        textFields.put("daydaymapkey", daydaymapKeyField);
        
        // 保存按钮
        JButton saveButton = new JButton("保存DayDayMap配置");
        saveButton.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        saveButton.setPreferredSize(new Dimension(120, 30));
        saveButton.setBackground(new Color(0, 123, 255));
        saveButton.setForeground(Color.WHITE);
        saveButton.setFocusPainted(false);
        saveButton.addActionListener(e -> saveDayDayMapConfig());
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(saveButton);
        
        panel.add(inputPanel, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        return panel;
    }

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
        String key = new String(fofaKeyField.getPassword());

        saveConfig("fofaurl", url);
        saveConfig("fofakey", key);

        Utils.fofaUrl = url;
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