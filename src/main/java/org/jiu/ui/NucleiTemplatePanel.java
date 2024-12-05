package org.jiu.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Nuclei模板生成器面板
 */
public class NucleiTemplatePanel extends JPanel {
    // 基础信息部分的组件
    private JTextField idField, nameField, authorField, tagsField;
    private JTextArea descriptionArea, referenceArea;
    private JComboBox<String> severityCombo;
    private JButton confirmBasicInfoBtn;

    // 匹配条件部分的组件
    private JComboBox<String> matcherConditionCombo;
    private JComboBox<String> matchTypeCombo;
    private JTextField matchValue1, matchValue2, matchValue3;
    private JButton addMatcherBtn;
    private JTextArea matchersResultArea;

    // HTTP请求和结果显示部分的组件
    private JTextArea httpRequestArea;
    private JButton generateBtn;
    private JButton resetBtn; // 移动到这里作为结果面板的按钮
    private JTextArea resultArea;

    // 存储所有添加的匹配器
    private List<Map<String, Object>> matchers = new ArrayList<>();

    public NucleiTemplatePanel() {
        // 使用边界布局
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 分割面板：左边是输入区域，右边是结果区域
        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplitPane.setResizeWeight(0.6); // 设置分割比例

        // 左侧面板包含基础信息和匹配条件
        JPanel leftPanel = new JPanel(new BorderLayout(10, 10));
        leftPanel.add(createBasicInfoPanel(), BorderLayout.NORTH);
        leftPanel.add(createMatchersPanel(), BorderLayout.CENTER);
        leftPanel.add(createHttpRequestPanel(), BorderLayout.SOUTH);

        // 右侧面板显示生成的结果
        JPanel rightPanel = createResultPanel();

        mainSplitPane.setLeftComponent(new JScrollPane(leftPanel));
        mainSplitPane.setRightComponent(rightPanel);

        add(mainSplitPane, BorderLayout.CENTER);

        // 初始化事件监听器
        initializeListeners();
    }


    /**
     * 创建基础信息面板
     */
    private JPanel createBasicInfoPanel() {
        // 使用表格布局使字段对齐
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("基础信息"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // 初始化组件
        idField = new JTextField(20);
        nameField = new JTextField(20);
        authorField = new JTextField(20);
        tagsField = new JTextField(20);
        severityCombo = new JComboBox<>(new String[]{"critical", "high", "medium", "low"});
        descriptionArea = new JTextArea(4, 20);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        JScrollPane descScrollPane = new JScrollPane(descriptionArea);

        referenceArea = new JTextArea(3, 20);
        referenceArea.setLineWrap(true);
        referenceArea.setWrapStyleWord(true);
        JScrollPane refScrollPane = new JScrollPane(referenceArea);
        confirmBasicInfoBtn = new JButton("确认基础信息");

        // 添加组件到面板
        addFieldToPanel(panel, "ID:", idField, gbc, 0);
        addFieldToPanel(panel, "名称:", nameField, gbc, 1);
        addFieldToPanel(panel, "作者:", authorField, gbc, 2);
        addFieldToPanel(panel, "标签:", tagsField, gbc, 3);
        addFieldToPanel(panel, "严重程度:", severityCombo, gbc, 4);

        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 1;
        panel.add(new JLabel("描述:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        panel.add(descScrollPane, gbc);

        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 1;
        panel.add(new JLabel("参考链接:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        panel.add(refScrollPane, gbc);

        // 确认按钮
        gbc.gridx = 0;
        gbc.gridy = 7;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        panel.add(confirmBasicInfoBtn, gbc);

        return panel;
    }

    /**
     * 创建匹配条件面板
     */
    private JPanel createMatchersPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("匹配条件"));

        // 匹配条件输入区域
        JPanel inputPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // 初始化组件
        matcherConditionCombo = new JComboBox<>(new String[]{"", "and", "or"}); // 添加空选项
        matchTypeCombo = new JComboBox<>(new String[]{
                "body", "header", "status", "regex_body", "regex_header",
                "time_check", "http_interaction", "dns_interaction"
        });

        matchValue1 = new JTextField(15);
        matchValue2 = new JTextField(15);
        matchValue3 = new JTextField(15);
        addMatcherBtn = new JButton("添加匹配条件");

        // 输入框提示文本
        matchValue1.setToolTipText("必填项");
        matchValue2.setToolTipText("选填项");
        matchValue3.setToolTipText("选填项");

        // 添加组件
        gbc.gridx = 0;
        gbc.gridy = 0;
        inputPanel.add(new JLabel("多个结果匹配条件类型:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        inputPanel.add(matcherConditionCombo, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.0;
        inputPanel.add(new JLabel("匹配类型:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        inputPanel.add(matchTypeCombo, gbc);

        // 匹配值输入框面板
        JPanel valuePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        valuePanel.add(new JLabel("匹配值:"));
        valuePanel.add(matchValue1);
        valuePanel.add(matchValue2);
        valuePanel.add(matchValue3);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        inputPanel.add(valuePanel, gbc);

        gbc.gridy = 3;
        gbc.anchor = GridBagConstraints.CENTER;
        inputPanel.add(addMatcherBtn, gbc);

        // 匹配结果显示区域
        matchersResultArea = new JTextArea(8, 40);
        matchersResultArea.setEditable(false);

        panel.add(inputPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(matchersResultArea), BorderLayout.CENTER);

        return panel;
    }

    /**
     * 创建HTTP请求面板
     */
    private JPanel createHttpRequestPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("HTTP请求"));

        httpRequestArea = new JTextArea(10, 40);
        panel.add(new JScrollPane(httpRequestArea), BorderLayout.CENTER);

        return panel;
    }

    /**
     * 创建结果显示面板
     */
    private JPanel createResultPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("生成结果"));

        // 创建按钮面板
        JPanel buttonPanel = new JPanel(new GridLayout(2, 1, 0, 5)); // 使用网格布局，2行1列
        resetBtn = new JButton("重新填写");
        generateBtn = new JButton("生成模板");

        // 按顺序添加按钮
        buttonPanel.add(resetBtn);
        buttonPanel.add(generateBtn);

        panel.add(buttonPanel, BorderLayout.NORTH);

        resultArea = new JTextArea(30, 40);
        resultArea.setEditable(false);
        panel.add(new JScrollPane(resultArea), BorderLayout.CENTER);

        return panel;
    }


    /**
     * 添加字段到面板的辅助方法
     */
    private void addFieldToPanel(JPanel panel, String label, JComponent field,
                                 GridBagConstraints gbc, int row) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 1;
        gbc.weightx = 0.0;
        panel.add(new JLabel(label), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        panel.add(field, gbc);
    }

    /**
     * 初始化所有事件监听器
     */
    private void initializeListeners() {
        confirmBasicInfoBtn.addActionListener(e -> validateAndConfirmBasicInfo());
        addMatcherBtn.addActionListener(e -> addMatcher());
        generateBtn.addActionListener(e -> generateTemplate());
        resetBtn.addActionListener(e -> resetAllFields());

        // 移除对可选输入框的限制
        matchTypeCombo.addActionListener(e -> {
            // 所有输入框都保持启用状态
            matchValue1.setEnabled(true);
            matchValue2.setEnabled(true);
            matchValue3.setEnabled(true);

            // 所有输入框背景都保持为白色
            matchValue1.setBackground(Color.WHITE);
            matchValue2.setBackground(Color.WHITE);
            matchValue3.setBackground(Color.WHITE);

            // 更新提示文本
            matchValue1.setToolTipText("主要匹配值");
            matchValue2.setToolTipText("附加匹配值（可选）");
            matchValue3.setToolTipText("附加匹配值（可选）");
        });
    }

    /**
     * 重置所有字段的方法
     */
    private void resetAllFields() {
        int result = JOptionPane.showConfirmDialog(this,
                "确定要重新填写吗？这将清空所有已填写的内容。",
                "确认重置",
                JOptionPane.YES_NO_OPTION);

        if (result == JOptionPane.YES_OPTION) {
            // 清空基础信息
            idField.setText("");
            nameField.setText("");
            authorField.setText("");
            tagsField.setText("");
            descriptionArea.setText("");
            referenceArea.setText("");
            severityCombo.setSelectedIndex(0);

            // 清空其他字段...
            matcherConditionCombo.setSelectedIndex(0);
            matchTypeCombo.setSelectedIndex(0);
            matchValue1.setText("");
            matchValue2.setText("");
            matchValue3.setText("");
            matchersResultArea.setText("");
            matchers.clear();
            httpRequestArea.setText("");
            resultArea.setText("");

            JOptionPane.showMessageDialog(this, "所有内容已清空，可以重新填写。");
        }
    }

    /**
     * 验证并确认基础信息
     */
    private void validateAndConfirmBasicInfo() {
        if (idField.getText().trim().isEmpty() ||
                nameField.getText().trim().isEmpty() ||
                authorField.getText().trim().isEmpty() ||
                tagsField.getText().trim().isEmpty() ||
                descriptionArea.getText().trim().isEmpty() ||
                referenceArea.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "所有字段都必须填写！");
            return;
        }
        JOptionPane.showMessageDialog(this, "基础信息确认成功！");
    }

    /**
     * 添加新的匹配条件
     */
    private void addMatcher() {
        if (matchValue1.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "至少需要填写一个匹配值！");
            return;
        }

        Map<String, Object> matcher = new HashMap<>();
        String matchType = matchTypeCombo.getSelectedItem().toString();
        String condition = matcherConditionCombo.getSelectedItem().toString();

        // 获取所有非空的匹配值
        List<String> values = new ArrayList<>();
        if (!matchValue1.getText().trim().isEmpty()) {
            values.add(matchValue1.getText().trim());
        }
        if (!matchValue2.getText().trim().isEmpty()) {
            values.add(matchValue2.getText().trim());
        }
        if (!matchValue3.getText().trim().isEmpty()) {
            values.add(matchValue3.getText().trim());
        }

        // 根据不同的匹配类型构建不同的匹配器配置
        switch (matchType) {
            case "status":
                // 状态码匹配
                matcher.put("type", "status");
                matcher.put("status", values);
                if (!condition.isEmpty() && values.size() > 1) {
                    matcher.put("condition", condition);
                }
                break;

            case "body":
            case "header":
                // 关键字匹配
                matcher.put("type", "word");
                matcher.put("part", matchType);
                matcher.put("words", values);
                if (!condition.isEmpty() && values.size() > 1) {
                    matcher.put("condition", condition);
                }
                break;

            case "regex_body":
            case "regex_header":
                // 正则表达式匹配
                matcher.put("type", "regex");
                matcher.put("part", matchType.replace("regex_", ""));
                matcher.put("regex", values);
                if (!condition.isEmpty() && values.size() > 1) {
                    matcher.put("condition", condition);
                }
                break;

            case "time_check":
                // 延时判断
                matcher.put("type", "dsl");
                matcher.put("dsl", Arrays.asList("duration>=" + values.get(0)));
                break;

            case "http_interaction":
                // HTTP外带
                matcher.put("type", "word");
                matcher.put("part", "interactsh_protocol");
                matcher.put("words", Arrays.asList("http"));
                break;

            case "dns_interaction":
                // DNS外带
                matcher.put("type", "word");
                matcher.put("part", "interactsh_protocol");
                matcher.put("words", Arrays.asList("dns"));
                break;
        }

        matchers.add(matcher);
        updateMatchersResult();

        // 清除输入框
        matchValue1.setText("");
        matchValue2.setText("");
        matchValue3.setText("");
    }

    /**
     * 更新匹配结果显示区域
     */
    private void updateMatchersResult() {
        StringBuilder sb = new StringBuilder();
        for (Map<String, Object> matcher : matchers) {
            sb.append("      - type: ").append(matcher.get("type")).append("\n");

            if (matcher.get("status") != null) {
                sb.append("        status:\n");
                ((List<String>)matcher.get("status")).forEach(s ->
                        sb.append("          - ").append(s).append("\n"));
            }

            if (matcher.get("words") != null) {
                sb.append("        words:\n");
                ((List<String>)matcher.get("words")).forEach(w ->
                        sb.append("          - \"").append(w).append("\"\n"));
            }

            if (matcher.get("regex") != null) {
                sb.append("        regex:\n");
                ((List<String>)matcher.get("regex")).forEach(r ->
                        sb.append("          - \"").append(r).append("\"\n"));
            }

            if (matcher.get("dsl") != null) {
                sb.append("        dsl:\n");
                ((List<String>)matcher.get("dsl")).forEach(d ->
                        sb.append("          - \"").append(d).append("\"\n"));
            }

            if (matcher.get("part") != null) {
                sb.append("        part: ").append(matcher.get("part")).append("\n");
            }

            if (matcher.get("condition") != null) {
                sb.append("        condition: ").append(matcher.get("condition")).append("\n");
            }
        }
        matchersResultArea.setText(sb.toString());
    }

    /**
     * 生成最终的模板
     */
    private void generateTemplate() {
        if (httpRequestArea.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "HTTP请求不能为空！");
            return;
        }

        String httpRequest = httpRequestArea.getText();
        httpRequest = httpRequest.replaceAll("Host: .*", "Host: {{Hostname}}");

        StringBuilder template = new StringBuilder();
        template.append("id: ").append(idField.getText()).append("\n\n");
        template.append("info:\n");
        template.append("  name: ").append(nameField.getText()).append("\n");
        template.append("  author: ").append(authorField.getText()).append("\n");
        template.append("  severity: ").append(severityCombo.getSelectedItem()).append("\n");

        // 添加description
        template.append("  description: |\n");
        String[] descLines = descriptionArea.getText().split("\n");
        for (String line : descLines) {
            template.append("    ").append(line).append("\n");
        }

        // 添加reference
        template.append("  reference:\n");
        String[] refLines = referenceArea.getText().split("\n");
        for (String line : refLines) {
            if (!line.trim().isEmpty()) {
                template.append("    - ").append(line.trim()).append("\n");
            }
        }

        template.append("  tags: ").append(tagsField.getText()).append("\n\n");
        template.append("http:\n");
        template.append("  - raw:\n");
        template.append("      - |\n");

        String[] lines = httpRequest.split("\n");
        for (String line : lines) {
            template.append("        ").append(line).append("\n");
        }

        template.append("\n    matchers-condition: ")
                .append(matcherConditionCombo.getSelectedItem()).append("\n");
        template.append("    matchers:\n");
        template.append(matchersResultArea.getText());

        resultArea.setText(template.toString());
    }
}