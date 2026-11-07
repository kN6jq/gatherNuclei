package org.jiu.ui;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Nuclei 模板生成器面板：通过表单填写基础信息、匹配条件与 HTTP 请求，拼装生成 nuclei YAML 模板。
 */
public class NucleiTemplatePanel extends JPanel {
    private JTextField idField, nameField, authorField, tagsField;
    private JTextArea descriptionArea, referenceArea;
    private JComboBox<String> severityCombo;
    private JButton confirmBasicInfoBtn;

    private JComboBox<String> matcherConditionCombo;
    private JComboBox<String> matchTypeCombo;
    private JTextField matchValue1, matchValue2, matchValue3;
    private JButton addMatcherBtn;
    private JTextArea matchersResultArea;

    private JTextArea httpRequestArea;
    private JButton generateBtn;
    private JButton resetBtn;
    private JTextArea resultArea;

    private List<Map<String, Object>> matchers = new ArrayList<>();

    /**
     * 构建左右分栏界面：左侧输入基础信息、匹配条件与 HTTP 请求，右侧预览生成结果。
     */
    public NucleiTemplatePanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplitPane.setResizeWeight(0.6); // 调整大小时左侧分得 60% 的额外空间

        JPanel leftPanel = new JPanel(new BorderLayout(10, 10));
        leftPanel.add(createBasicInfoPanel(), BorderLayout.NORTH);
        leftPanel.add(createMatchersPanel(), BorderLayout.CENTER);
        leftPanel.add(createHttpRequestPanel(), BorderLayout.SOUTH);

        JPanel rightPanel = createResultPanel();

        mainSplitPane.setLeftComponent(new JScrollPane(leftPanel));
        mainSplitPane.setRightComponent(rightPanel);

        add(mainSplitPane, BorderLayout.CENTER);

        initializeListeners();
    }


    /**
     * 创建基础信息表单：ID、名称、作者、标签、严重程度、描述、参考链接及确认按钮。
     */
    private JPanel createBasicInfoPanel() {
        // 用 GridBagLayout 使标签与输入框对齐
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                "基础信息",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("微软雅黑", Font.BOLD, 14),
                new Color(50, 50, 50)
        ));
        panel.setBorder(BorderFactory.createCompoundBorder(
                panel.getBorder(),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        idField = new JTextField(20);
        idField.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        idField.setPreferredSize(new Dimension(0, 25));

        nameField = new JTextField(20);
        nameField.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        nameField.setPreferredSize(new Dimension(0, 25));

        authorField = new JTextField(20);
        authorField.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        authorField.setPreferredSize(new Dimension(0, 25));

        tagsField = new JTextField(20);
        tagsField.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        tagsField.setPreferredSize(new Dimension(0, 25));

        severityCombo = new JComboBox<>(new String[]{"critical", "high", "medium", "low"});
        severityCombo.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        severityCombo.setPreferredSize(new Dimension(0, 25));

        descriptionArea = new JTextArea(4, 20);
        descriptionArea.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        JScrollPane descScrollPane = new JScrollPane(descriptionArea);

        referenceArea = new JTextArea(3, 20);
        referenceArea.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        referenceArea.setLineWrap(true);
        referenceArea.setWrapStyleWord(true);
        JScrollPane refScrollPane = new JScrollPane(referenceArea);

        confirmBasicInfoBtn = new JButton("确认基础信息");
        confirmBasicInfoBtn.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        confirmBasicInfoBtn.setPreferredSize(new Dimension(120, 30));
        confirmBasicInfoBtn.setBackground(new Color(0, 123, 255));
        confirmBasicInfoBtn.setForeground(Color.WHITE);
        confirmBasicInfoBtn.setFocusPainted(false);

        addFieldToPanel(panel, "ID:", idField, gbc, 0);
        addFieldToPanel(panel, "名称:", nameField, gbc, 1);
        addFieldToPanel(panel, "作者:", authorField, gbc, 2);
        addFieldToPanel(panel, "标签:", tagsField, gbc, 3);
        addFieldToPanel(panel, "严重程度:", severityCombo, gbc, 4);

        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 1;
        JLabel descLabel = new JLabel("描述:");
        descLabel.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        panel.add(descLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        panel.add(descScrollPane, gbc);

        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 1;
        JLabel refLabel = new JLabel("参考链接:");
        refLabel.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        panel.add(refLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        panel.add(refScrollPane, gbc);

        gbc.gridx = 0;
        gbc.gridy = 7;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        panel.add(confirmBasicInfoBtn, gbc);

        return panel;
    }

    /**
     * 创建匹配条件输入表单与已添加匹配器的 YAML 预览区。
     */
    private JPanel createMatchersPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                "匹配条件",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("微软雅黑", Font.BOLD, 14),
                new Color(50, 50, 50)
        ));
        panel.setBorder(BorderFactory.createCompoundBorder(
                panel.getBorder(),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        JPanel inputPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        matcherConditionCombo = new JComboBox<>(new String[]{"", "and", "or"}); // 空选项表示不指定多值匹配条件
        matcherConditionCombo.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        matcherConditionCombo.setPreferredSize(new Dimension(0, 25));

        matchTypeCombo = new JComboBox<>(new String[]{
                "body", "header", "status", "regex_body", "regex_header",
                "time_check", "http_interaction", "dns_interaction"
        });
        matchTypeCombo.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        matchTypeCombo.setPreferredSize(new Dimension(0, 25));

        matchValue1 = new JTextField(15);
        matchValue1.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        matchValue1.setPreferredSize(new Dimension(0, 25));

        matchValue2 = new JTextField(15);
        matchValue2.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        matchValue2.setPreferredSize(new Dimension(0, 25));

        matchValue3 = new JTextField(15);
        matchValue3.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        matchValue3.setPreferredSize(new Dimension(0, 25));

        addMatcherBtn = new JButton("添加匹配条件");
        addMatcherBtn.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        addMatcherBtn.setPreferredSize(new Dimension(120, 30));
        addMatcherBtn.setBackground(new Color(40, 167, 69));
        addMatcherBtn.setForeground(Color.WHITE);
        addMatcherBtn.setFocusPainted(false);

        matchValue1.setToolTipText("必填项");
        matchValue2.setToolTipText("选填项");
        matchValue3.setToolTipText("选填项");

        gbc.gridx = 0;
        gbc.gridy = 0;
        JLabel conditionLabel = new JLabel("多个结果匹配条件类型:");
        conditionLabel.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        inputPanel.add(conditionLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        inputPanel.add(matcherConditionCombo, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.0;
        JLabel typeLabel = new JLabel("匹配类型:");
        typeLabel.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        inputPanel.add(typeLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        inputPanel.add(matchTypeCombo, gbc);

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

        matchersResultArea = new JTextArea(8, 40);
        matchersResultArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        matchersResultArea.setEditable(false);

        panel.add(inputPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(matchersResultArea), BorderLayout.CENTER);

        return panel;
    }

    /**
     * 创建原始 HTTP 请求报文输入区。
     */
    private JPanel createHttpRequestPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                "HTTP请求",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("微软雅黑", Font.BOLD, 14),
                new Color(50, 50, 50)
        ));
        panel.setBorder(BorderFactory.createCompoundBorder(
                panel.getBorder(),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        httpRequestArea = new JTextArea(10, 40);
        httpRequestArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        panel.add(new JScrollPane(httpRequestArea), BorderLayout.CENTER);

        return panel;
    }

    /**
     * 创建生成结果预览区及生成、重置按钮。
     */
    private JPanel createResultPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                "生成结果",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("微软雅黑", Font.BOLD, 14),
                new Color(50, 50, 50)
        ));
        panel.setBorder(BorderFactory.createCompoundBorder(
                panel.getBorder(),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        resetBtn = new JButton("重新填写");
        resetBtn.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        resetBtn.setPreferredSize(new Dimension(100, 30));
        resetBtn.setBackground(new Color(220, 53, 69));
        resetBtn.setForeground(Color.WHITE);
        resetBtn.setFocusPainted(false);

        generateBtn = new JButton("生成模板");
        generateBtn.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        generateBtn.setPreferredSize(new Dimension(100, 30));
        generateBtn.setBackground(new Color(0, 123, 255));
        generateBtn.setForeground(Color.WHITE);
        generateBtn.setFocusPainted(false);

        buttonPanel.add(generateBtn);
        buttonPanel.add(resetBtn);

        panel.add(buttonPanel, BorderLayout.NORTH);

        resultArea = new JTextArea(30, 40);
        resultArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        resultArea.setEditable(false);
        panel.add(new JScrollPane(resultArea), BorderLayout.CENTER);

        return panel;
    }


    /**
     * 以标签在左、输入框在右的两列布局添加一行字段。
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
     * 绑定各按钮动作，并在切换匹配类型时刷新输入框提示。
     */
    private void initializeListeners() {
        confirmBasicInfoBtn.addActionListener(e -> validateAndConfirmBasicInfo());
        addMatcherBtn.addActionListener(e -> addMatcher());
        generateBtn.addActionListener(e -> generateTemplate());
        resetBtn.addActionListener(e -> resetAllFields());

        // 所有匹配类型均不禁用输入框，仅刷新提示文本
        matchTypeCombo.addActionListener(e -> {
            matchValue1.setEnabled(true);
            matchValue2.setEnabled(true);
            matchValue3.setEnabled(true);

            matchValue1.setBackground(Color.WHITE);
            matchValue2.setBackground(Color.WHITE);
            matchValue3.setBackground(Color.WHITE);

            matchValue1.setToolTipText("主要匹配值");
            matchValue2.setToolTipText("附加匹配值（可选）");
            matchValue3.setToolTipText("附加匹配值（可选）");
        });
    }

    /**
     * 经用户确认后清空全部输入、匹配器列表与生成结果。
     */
    private void resetAllFields() {
        int result = JOptionPane.showConfirmDialog(this,
                "确定要重新填写吗？这将清空所有已填写的内容。",
                "确认重置",
                JOptionPane.YES_NO_OPTION);

        if (result == JOptionPane.YES_OPTION) {
            idField.setText("");
            nameField.setText("");
            authorField.setText("");
            tagsField.setText("");
            descriptionArea.setText("");
            referenceArea.setText("");
            severityCombo.setSelectedIndex(0);

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
     * 校验基础信息全部非空，仅弹窗提示结果，不保存任何状态。
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
     * 按所选匹配类型构建匹配器追加到列表，刷新预览后清空输入框；首个匹配值必填。
     */
    private void addMatcher() {
        if (matchValue1.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "至少需要填写一个匹配值！");
            return;
        }

        Map<String, Object> matcher = new HashMap<>();
        String matchType = matchTypeCombo.getSelectedItem().toString();
        String condition = matcherConditionCombo.getSelectedItem().toString();

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

        switch (matchType) {
            case "status":
                matcher.put("type", "status");
                matcher.put("status", values);
                if (!condition.isEmpty() && values.size() > 1) {
                    matcher.put("condition", condition);
                }
                break;

            case "body":
            case "header":
                matcher.put("type", "word");
                matcher.put("part", matchType);
                matcher.put("words", values);
                if (!condition.isEmpty() && values.size() > 1) {
                    matcher.put("condition", condition);
                }
                break;

            case "regex_body":
            case "regex_header":
                // regex_ 前缀仅用于区分 UI 选项，part 取 body/header
                matcher.put("type", "regex");
                matcher.put("part", matchType.replace("regex_", ""));
                matcher.put("regex", values);
                if (!condition.isEmpty() && values.size() > 1) {
                    matcher.put("condition", condition);
                }
                break;

            case "time_check":
                // 延时判断转为 dsl 表达式，仅取首个匹配值作为秒数阈值
                matcher.put("type", "dsl");
                matcher.put("dsl", Arrays.asList("duration>=" + values.get(0)));
                break;

            case "http_interaction":
                // 外带检测固定匹配 interactsh_protocol，忽略用户输入的匹配值
                matcher.put("type", "word");
                matcher.put("part", "interactsh_protocol");
                matcher.put("words", Arrays.asList("http"));
                break;

            case "dns_interaction":
                matcher.put("type", "word");
                matcher.put("part", "interactsh_protocol");
                matcher.put("words", Arrays.asList("dns"));
                break;
        }

        matchers.add(matcher);
        updateMatchersResult();

        matchValue1.setText("");
        matchValue2.setText("");
        matchValue3.setText("");
    }

    /**
     * 将匹配器列表拼成 YAML 片段显示，缩进与最终模板的 matchers 层级对齐。
     */
    private void updateMatchersResult() {
        StringBuilder sb = new StringBuilder();
        for (Map<String, Object> matcher : matchers) {
            sb.append("      - type: ").append(matcher.get("type")).append("\n");

            if (matcher.get("status") != null) {
                sb.append("        status:\n");
                ((List<String>) matcher.get("status")).forEach(s ->
                        sb.append("          - ").append(s).append("\n"));
            }

            if (matcher.get("words") != null) {
                sb.append("        words:\n");
                ((List<String>) matcher.get("words")).forEach(w ->
                        sb.append("          - \"").append(w).append("\"\n"));
            }

            if (matcher.get("regex") != null) {
                sb.append("        regex:\n");
                ((List<String>) matcher.get("regex")).forEach(r ->
                        sb.append("          - \"").append(r).append("\"\n"));
            }

            if (matcher.get("dsl") != null) {
                sb.append("        dsl:\n");
                ((List<String>) matcher.get("dsl")).forEach(d ->
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
     * 拼接生成 nuclei YAML 模板并显示在结果区；仅要求 HTTP 请求非空，Host 头统一替换为 {{Hostname}} 占位符。
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

        // 描述用 YAML 块标量，每行固定缩进 4 空格
        template.append("  description: |\n");
        String[] descLines = descriptionArea.getText().split("\n");
        for (String line : descLines) {
            template.append("    ").append(line).append("\n");
        }

        // 参考链接按行拆成 YAML 列表，跳过空行
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