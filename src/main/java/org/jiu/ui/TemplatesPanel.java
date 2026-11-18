package org.jiu.ui;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.jiu.utils.UIUtils;
import org.jiu.utils.Utils;
import org.jiu.utils.YamlUtils;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 模板管理面板：加载并以表格管理 YAML 格式的 nuclei 模板，支持过滤、搜索、编辑及工作流操作。
 */
public class TemplatesPanel extends JPanel {
    private static final String[] COLUMN_NAMES = {
            "#",
            "templates_id",
            "templates_name",
            "templates_severity",
            "templates_tags",
            "templates_author",
            "templates_description",
            "templates_reference"
    };
    private static final String[] SEVERITY_LEVELS = {
            "info", "low", "medium", "high", "critical", "unknown"
    };

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final JLabel statusLabel;
    private final JTable templateTable;
    private final DefaultTableModel tableModel;
    private final TableRowSorter<DefaultTableModel> sorter;
    private final JTextField searchField;
    // 加载互斥锁，确保后台加载任务串行执行
    private final Object loadLock = new Object();

    private final JToggleButton infoBtn;
    private final JToggleButton lowBtn;
    private final JToggleButton mediumBtn;
    private final JToggleButton highBtn;
    private final JToggleButton criticalBtn;
    private final JButton refreshBtn;

    private final List<Map<String, Object>> templates;
    private final Set<String> activeFilters;
    // 表格序号（# 列，从 1 开始）到模板文件路径的映射
    private final Map<Integer, String> templatePathMap = new HashMap<>();
    // 加载失败的文件及异常，每轮加载前清空，供状态栏点击查看
    private Map<Path, Exception> failedFiles = Collections.synchronizedMap(new HashMap<>());

    /**
     * 构建面板界面并触发首次后台加载。
     */
    public TemplatesPanel() {
        setLayout(new BorderLayout());

        templates = new ArrayList<>();
        activeFilters = new HashSet<>();

        statusLabel = new JLabel("正在初始化...");

        tableModel = createTableModel();
        templateTable = createTable();
        sorter = new TableRowSorter<>(tableModel);
        templateTable.setRowSorter(sorter);

        searchField = createSearchField();

        infoBtn = createFilterButton("信息", "info");
        lowBtn = createFilterButton("低", "low");
        mediumBtn = createFilterButton("中", "medium");
        highBtn = createFilterButton("高", "high");
        criticalBtn = createFilterButton("危", "critical");

        refreshBtn = createRefreshButton();

        initUI();

        loadTemplatesInBackground();
    }

    /**
     * 创建只读表格模型，所有列统一为字符串类型。
     */
    private DefaultTableModel createTableModel() {
        return new DefaultTableModel(COLUMN_NAMES, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return String.class;
            }
        };
    }

    /**
     * 创建模板表格，配置多选、渲染器、列宽与右键菜单。
     */
    private JTable createTable() {
        JTable table = new JTable(tableModel);

        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.setAutoCreateRowSorter(true);
        table.setFillsViewportHeight(true);
        UIUtils.styleTable(table);

        DefaultTableCellRenderer centerRenderer = createCenterRenderer();
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        // 第 3 列为严重程度，使用颜色渲染器
        table.getColumnModel().getColumn(3).setCellRenderer(createSeverityRenderer());

        configureColumnWidths(table);

        table.addMouseListener(new PopupListener(table));

        return table;
    }

    /**
     * 创建居中渲染器，悬浮提示显示完整单元格内容。
     */
    private DefaultTableCellRenderer createCenterRenderer() {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable table, Object value, boolean isSelected, boolean hasFocus,
                    int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, column);
                label.setHorizontalAlignment(JLabel.CENTER);
                if (value != null) {
                    label.setToolTipText(value.toString());
                }
                return label;
            }
        };
    }

    /**
     * 按严重程度着色渲染，选中状态下不着色。
     */
    private DefaultTableCellRenderer createSeverityRenderer() {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                if (!isSelected) {
                    String severity = (String) value;
                    if (severity != null) {
                        switch (severity.toLowerCase()) {
                            case "critical":
                                label.setBackground(new Color(220, 53, 69));
                                label.setForeground(Color.WHITE);
                                break;
                            case "high":
                                label.setBackground(new Color(255, 193, 7));
                                label.setForeground(Color.BLACK);
                                break;
                            case "medium":
                                label.setBackground(new Color(255, 133, 27));
                                label.setForeground(Color.WHITE);
                                break;
                            case "low":
                                label.setBackground(new Color(40, 167, 69));
                                label.setForeground(Color.WHITE);
                                break;
                            case "info":
                                label.setBackground(new Color(108, 117, 125));
                                label.setForeground(Color.WHITE);
                                break;
                            default:
                                label.setBackground(null);
                                label.setForeground(null);
                        }
                    }
                }

                label.setHorizontalAlignment(JLabel.CENTER);
                if (value != null) {
                    label.setToolTipText(value.toString());
                }

                return label;
            }
        };
    }

    /**
     * 配置各列首选宽度：总宽控制在常见窗口宽度内，表头完整显示且不出现横向滚动条。
     */
    private void configureColumnWidths(JTable table) {
        table.getColumnModel().getColumn(0).setPreferredWidth(40);   // #
        table.getColumnModel().getColumn(1).setPreferredWidth(130);  // ID
        table.getColumnModel().getColumn(2).setPreferredWidth(180);  // Name
        table.getColumnModel().getColumn(3).setPreferredWidth(132);  // Severity - 使用颜色标识
        table.getColumnModel().getColumn(4).setPreferredWidth(150);  // Tags
        table.getColumnModel().getColumn(5).setPreferredWidth(116);  // Author
        table.getColumnModel().getColumn(6).setPreferredWidth(240);  // Description
        table.getColumnModel().getColumn(7).setPreferredWidth(170);  // Reference
    }

    /**
     * 创建搜索框，文本变化经文档监听器防抖后触发过滤。
     * 复用统一搜索框样式：300x30 尺寸、占位文案、尾部搜索图标与清空按钮。
     */
    private JTextField createSearchField() {
        JTextField field = new JTextField(20);
        UIUtils.setupSearchField(field, "搜索模板 (Ctrl+F)...");
        field.getDocument().addDocumentListener(new SearchDocumentListener());
        return field;
    }

    /**
     * 创建默认选中的严重程度过滤按钮：选中时使用对应级别的浅色底与深色文字，
     * 取消选中时恢复白底灰字，让过滤状态一眼可辨。
     */
    private JToggleButton createFilterButton(String text, String severity) {
        JToggleButton button = new JToggleButton(text);
        button.setSelected(true);
        button.setToolTipText(severity + "级别模板");
        button.setFont(UIUtils.LABEL_FONT);
        button.setFocusPainted(false);

        Color selectedBackground = severityBackground(text);
        Color selectedForeground = severityForeground(text);

        Runnable applyStyle = () -> {
            if (button.isSelected()) {
                button.setBackground(selectedBackground);
                button.setForeground(selectedForeground);
            } else {
                button.setBackground(Color.WHITE);
                button.setForeground(new Color(73, 80, 87));
            }
        };
        applyStyle.run();

        button.addItemListener(e -> applyStyle.run());
        button.addActionListener(e -> filterTemplates());
        return button;
    }

    /**
     * 严重程度对应的选中底色（浅色系，避免饱和色块造成视觉噪音）。
     */
    private Color severityBackground(String text) {
        switch (text) {
            case "信息":
                return new Color(233, 236, 239);
            case "低":
                return new Color(212, 237, 218);
            case "中":
                return new Color(255, 243, 205);
            case "高":
                return new Color(255, 232, 210);
            case "危":
                return new Color(248, 215, 218);
            default:
                return new Color(222, 235, 255);
        }
    }

    /**
     * 严重程度对应的选中文字色，与底色保持同色系且保证对比度。
     */
    private Color severityForeground(String text) {
        switch (text) {
            case "信息":
                return new Color(73, 80, 87);
            case "低":
                return new Color(30, 126, 52);
            case "中":
                return new Color(138, 109, 0);
            case "高":
                return new Color(178, 89, 0);
            case "危":
                return new Color(176, 42, 55);
            default:
                return new Color(11, 94, 215);
        }
    }

    /**
     * 创建刷新按钮，点击重新加载模板列表。
     */
    private JButton createRefreshButton() {
        JButton button = new JButton();
        UIUtils.stylePrimaryButton(button, "刷新");
        button.setPreferredSize(new Dimension(85, UIUtils.SEARCH_INPUT_HEIGHT));
        button.setToolTipText("刷新模板列表");
        button.addActionListener(e -> refreshTemplates());
        return button;
    }

    /**
     * 组装工具栏与表格滚动面板，并初始化过滤器。
     */
    private void initUI() {
        JToolBar toolBar = createToolBar();
        add(toolBar, BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane(templateTable);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(new EmptyBorder(0, 10, 10, 10));
        add(scrollPane, BorderLayout.CENTER);

        initializeFilters();
    }

    /**
     * 创建工具栏：左侧为状态标签、过滤与刷新按钮，右侧为搜索框。
     */
    private JToolBar createToolBar() {
        JToolBar toolBar = new JToolBar();
        UIUtils.styleToolBar(toolBar);

        JPanel mainPanel = new JPanel(new BorderLayout());

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));

        statusLabel.setPreferredSize(new Dimension(200, UIUtils.SEARCH_INPUT_HEIGHT));
        UIUtils.styleLabel(statusLabel);
        leftPanel.add(statusLabel);

        leftPanel.add(Box.createHorizontalStrut(10));

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        JLabel filterLabel = new JLabel("过滤:");
        UIUtils.styleLabel(filterLabel);
        filterLabel.setPreferredSize(new Dimension(44, UIUtils.SEARCH_INPUT_HEIGHT));
        filterPanel.add(filterLabel);

        Dimension buttonSize = new Dimension(52, UIUtils.SEARCH_INPUT_HEIGHT);
        for (JToggleButton btn : new JToggleButton[]{infoBtn, lowBtn, mediumBtn, highBtn, criticalBtn}) {
            btn.setPreferredSize(buttonSize);
            btn.setFont(UIUtils.LABEL_FONT);
            btn.setFocusPainted(false);
            filterPanel.add(btn);
        }

        leftPanel.add(filterPanel);

        leftPanel.add(refreshBtn);

        leftPanel.add(Box.createHorizontalStrut(10));

        JPanel rightPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 10, 0, 0);
        rightPanel.add(searchField, gbc);

        mainPanel.add(leftPanel, BorderLayout.WEST);
        // 搜索框放入 CENTER：由 GridBag 横向填满左侧工具栏之后的剩余空间，随窗口变宽自动拉长
        mainPanel.add(rightPanel, BorderLayout.CENTER);

        toolBar.add(mainPanel);
        return toolBar;
    }

    /**
     * 默认启用全部严重程度过滤（含 unknown）。
     */
    private void initializeFilters() {
        activeFilters.clear();
        activeFilters.addAll(Arrays.asList(SEVERITY_LEVELS));
    }

    /**
     * 异步加载模板：加载期间禁用刷新按钮，任务经 loadLock 串行化，异常回到 EDT 处理。
     */
    private void loadTemplatesInBackground() {
        updateStatus("正在加载模板...");
        refreshBtn.setEnabled(false);

        executor.submit(() -> {
            try {
                synchronized (loadLock) {
                    String templatePath = Utils.templatePath;
                    if (templatePath == null || templatePath.trim().isEmpty()) {
                        SwingUtilities.invokeLater(() -> {
                            updateStatus("未配置模板路径");
                            refreshBtn.setEnabled(true);
                        });
                        return;
                    }
                    loadTemplates();
                }
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> handleLoadError(e));
            }
        });
    }

    /**
     * 在后台线程递归加载目录下所有 .yaml 模板，完成后回到 EDT 刷新表格；路径无效时抛出 IOException。
     */
    private void loadTemplates() throws IOException {
        String templatePath = Utils.templatePath;

        if (templatePath == null || templatePath.trim().isEmpty()) {
            return;
        }
        if (!isValidPath(templatePath)) {
            throw new IOException("无效的模板路径: " + templatePath);
        }

        SwingUtilities.invokeLater(() -> {
            synchronized (templates) {
                templates.clear();
                templatePathMap.clear();
                tableModel.setRowCount(0);
            }
        });

        // 新一轮加载前清空上次的失败记录，避免残留旧错误
        failedFiles.clear();

        // 先在后台线程收集全部模板，最后一次性发布到 UI，避免逐条刷新
        List<Map<String, Object>> tempTemplates = new ArrayList<>();
        final int[] stats = new int[]{0, 0, 0}; // 依次为总数、成功、失败

        Files.walkFileTree(Paths.get(templatePath), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.toString().endsWith(".yaml")) {
                    stats[0]++;
                    try {
                        Map<String, Object> template = loadYamlFile(file);
                        if (template != null && isValidTemplate(template)) {
                            template.put("path", file.toString()); // 注入路径字段，供序号到路径映射使用
                            tempTemplates.add(template);
                            stats[1]++;
                            updateProgressStatus(stats);
                        } else {
                            stats[2]++;
                            handleTemplateLoadError(file, new Exception("无效的模板格式"));
                        }
                    } catch (Exception e) {
                        stats[2]++;
                        handleTemplateLoadError(file, e);
                    }
                    updateProgressStatus(stats);
                }
                return FileVisitResult.CONTINUE;
            }
        });

        SwingUtilities.invokeLater(() -> {
            synchronized (templates) {
                templates.clear();
                templates.addAll(tempTemplates);

                updateTableDataWithMapping();
                updateFinalStatus(stats);
                refreshBtn.setEnabled(true);
            }
        });
    }

    /**
     * 按过滤条件重建表格行并维护序号到文件路径的映射，须在 EDT 上且持有 templates 锁时调用。
     */
    private void updateTableDataWithMapping() {
        tableModel.setRowCount(0);
        templatePathMap.clear();
        List<Map<String, Object>> visibleTemplates = new ArrayList<>();

        for (Map<String, Object> template : templates) {
            if (isTemplateVisible(template)) {
                visibleTemplates.add(template);
            }
        }

        for (int i = 0; i < visibleTemplates.size(); i++) {
            Map<String, Object> template = visibleTemplates.get(i);
            int rowNum = i + 1;

            String path = (String) template.get("path");
            templatePathMap.put(rowNum, path);

            Object[] rowData = createRowData(rowNum, template);
            tableModel.addRow(rowData);
        }

        updateStatus(String.format("显示 %d 个模板（共 %d 个）", visibleTemplates.size(), templates.size()));
    }


    private boolean isValidPath(String path) {
        return path != null && !path.isEmpty() && Files.exists(Paths.get(path));
    }

    /**
     * 解析 YAML 模板并附加 path 字段；空文件返回 null，读取失败抛出 IOException。
     */
    private Map<String, Object> loadYamlFile(Path file) throws IOException {
        try (InputStream is = Files.newInputStream(file)) {
            // 放宽 SnakeYAML 默认 3MB 文档上限，体积大的指纹模板（如 WordPress 插件检测）才能解析
            Yaml yaml = YamlUtils.newYamlReader();
            Map<String, Object> template = yaml.load(is);
            if (template != null) {
                template.put("path", file.toString());
            }
            return template;
        }
    }

    /**
     * 校验模板包含非空的 info.name 与 info.severity。
     */
    private boolean isValidTemplate(Map<String, Object> template) {
        if (!template.containsKey("info")) {
            return false;
        }

        Map<String, Object> info = (Map<String, Object>) template.get("info");
        return info != null &&
                info.containsKey("name") &&
                info.containsKey("severity") &&
                info.get("name") != null &&
                info.get("severity") != null;
    }

    /**
     * 在 EDT 上刷新加载进度文本。
     *
     * @param stats 依次为总数、成功数、失败数
     */
    private void updateProgressStatus(int[] stats) {
        SwingUtilities.invokeLater(() ->
                statusLabel.setText(String.format("正在加载... 总数: %d, 成功: %d, 失败: %d",
                        stats[0], stats[1], stats[2]))
        );
    }

    /**
     * 显示加载汇总；存在失败文件时状态标签可点击查看失败详情。
     *
     * @param stats 依次为总数、成功数、失败数
     */
    private void updateFinalStatus(int[] stats) {
        StringBuilder status = new StringBuilder();
        status.append(String.format("加载完成 - 总数: %d, 成功: %d, 失败: %d",
                stats[0], stats[1], stats[2]));

        if (stats[2] > 0 && !failedFiles.isEmpty()) {
            status.append(" (点击查看失败详情)");

            // 先移除旧监听器，避免多次加载后点击事件重复叠加
            for (MouseListener listener : statusLabel.getMouseListeners()) {
                statusLabel.removeMouseListener(listener);
            }

            statusLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            statusLabel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    showFailedFilesDialog();
                }
            });
        } else {
            statusLabel.setCursor(Cursor.getDefaultCursor());
            for (MouseListener listener : statusLabel.getMouseListeners()) {
                statusLabel.removeMouseListener(listener);
            }
        }

        String statusText = status.toString();
        statusLabel.setText(statusText);
        statusLabel.setToolTipText(statusText);
    }

    /**
     * 弹窗展示失败文件及异常堆栈，支持一键复制；无失败记录时仅给出提示。
     */
    private void showFailedFilesDialog() {
        if (failedFiles.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "没有发现加载失败的文件\n" +
                            "注意：如果您看到状态栏显示有失败的文件，但此处显示没有，\n" +
                            "这可能是因为错误信息在刷新过程中被清除了。",
                    "提示",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        StringBuilder message = new StringBuilder();
        message.append("加载失败的文件列表:\n\n");

        failedFiles.forEach((path, ex) -> {
            message.append("文件: ").append(path).append("\n");
            message.append("错误: ").append(ex.getMessage()).append("\n");
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            ex.printStackTrace(pw);
            message.append("详细信息:\n").append(sw.toString()).append("\n");
            message.append("----------------------------------------\n\n");
        });

        JTextArea textArea = new JTextArea(message.toString());
        textArea.setEditable(false);
        textArea.setCaretPosition(0);
        textArea.setFont(UIUtils.CODE_FONT);

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(800, 400));

        JButton copyButton = new JButton("复制错误信息");
        copyButton.addActionListener(e -> {
            textArea.selectAll();
            textArea.copy();
            textArea.select(0, 0);
            JOptionPane.showMessageDialog(this,
                    "错误信息已复制到剪贴板",
                    "提示",
                    JOptionPane.INFORMATION_MESSAGE);
        });

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(copyButton, BorderLayout.SOUTH);

        JOptionPane.showMessageDialog(this,
                panel,
                "加载失败详情",
                JOptionPane.ERROR_MESSAGE);
    }

    /**
     * 记录单个文件的加载失败并输出日志。
     */
    private void handleTemplateLoadError(Path file, Exception e) {
        // 在后台线程写入失败记录，failedFiles 为同步 Map，可安全跨线程访问
        failedFiles.put(file, e);

        SwingUtilities.invokeLater(() -> {
            System.err.println("加载模板失败: " + file);
            System.err.println("错误信息: " + e.getMessage());
            e.printStackTrace();
        });
    }

    /**
     * 在 EDT 上汇总整体加载失败信息，弹窗展示后清空失败记录。
     */
    private void handleLoadError(Exception e) {
        StringBuilder errorMessage = new StringBuilder("加载模板失败:\n\n");

        errorMessage.append("主要错误: ").append(e.getMessage()).append("\n\n");

        if (!failedFiles.isEmpty()) {
            errorMessage.append("以下文件加载失败:\n");
            failedFiles.forEach((path, ex) -> {
                errorMessage.append("文件: ").append(path).append("\n");
                errorMessage.append("原因: ").append(ex.getMessage()).append("\n\n");
            });
        }

        String statusText = String.format("加载失败 - %d 个文件出错", failedFiles.size());
        statusLabel.setText(statusText);
        statusLabel.setToolTipText(statusText);

        showErrorDialog("加载模板失败", errorMessage.toString(), e);

        failedFiles.clear();
    }

    /**
     * 按过滤条件重建表格行，不维护序号到路径的映射。
     */
    private void updateTableData() {
        tableModel.setRowCount(0);
        int displayedCount = 0;

        List<Map<String, Object>> visibleTemplates = new ArrayList<>();

        for (Map<String, Object> template : templates) {
            if (isTemplateVisible(template)) {
                visibleTemplates.add(template);
            }
        }

        for (int i = 0; i < visibleTemplates.size(); i++) {
            Map<String, Object> template = visibleTemplates.get(i);
            displayedCount++;
            Object[] rowData = createRowData(i + 1, template);
            tableModel.addRow(rowData);
        }

        updateStatus(String.format("显示 %d 个模板（共 %d 个）", displayedCount, templates.size()));
    }

    /**
     * 将模板字段组装为一行表格数据。
     *
     * @param rowNum 从 1 开始的显示序号
     */
    private Object[] createRowData(int rowNum, Map<String, Object> template) {
        Map<String, Object> info = (Map<String, Object>) template.get("info");
        return new Object[]{
                String.valueOf(rowNum),
                getTemplateValue(template, "id"),
                getTemplateValue(info, "name"),
                getTemplateValue(info, "severity"),
                formatTags(info.get("tags")),
                getTemplateValue(info, "author"),
                getTemplateValue(info, "description"),
                getTemplateValue(info, "reference")
        };
    }

    /**
     * 取字段的字符串值，缺失或为 null 时返回空串。
     */
    private String getTemplateValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : "";
    }

    /**
     * 将 tags 格式化为逗号分隔字符串，兼容列表与单值，null 返回空串。
     */
    private String formatTags(Object tags) {
        if (tags == null) {
            return "";
        }
        if (tags instanceof List) {
            return String.join(", ", ((List<?>) tags).stream()
                    .map(Object::toString)
                    .toArray(String[]::new));
        }
        return tags.toString();
    }

    /**
     * 按严重程度与搜索关键词判断模板是否可见，severity 缺失按 unknown 处理。
     */
    private boolean isTemplateVisible(Map<String, Object> template) {
        if (template == null || !template.containsKey("info")) {
            return false;
        }

        Map<String, Object> info = (Map<String, Object>) template.get("info");
        String severity = (String) info.get("severity");
        if (severity == null) {
            severity = "unknown";
        }

        if (!activeFilters.contains(severity.toLowerCase())) {
            return false;
        }

        String searchText = searchField.getText().toLowerCase().trim();
        if (!searchText.isEmpty()) {
            return matchesSearch(template, info, searchText);
        }

        return true;
    }

    /**
     * 在 id、名称、作者、描述、标签中匹配关键词。
     */
    private boolean matchesSearch(Map<String, Object> template, Map<String, Object> info, String searchText) {
        return containsIgnoreCase(getTemplateValue(template, "id"), searchText) ||
                containsIgnoreCase(getTemplateValue(info, "name"), searchText) ||
                containsIgnoreCase(getTemplateValue(info, "author"), searchText) ||
                containsIgnoreCase(getTemplateValue(info, "description"), searchText) ||
                containsIgnoreCase(formatTags(info.get("tags")), searchText);
    }

    /**
     * 包含匹配，仅对 text 转小写，调用方须保证 searchText 已为小写。
     */
    private boolean containsIgnoreCase(String text, String searchText) {
        return text.toLowerCase().contains(searchText);
    }

    /**
     * 按过滤按钮选中状态重建 activeFilters（unknown 始终保留）并刷新表格。
     */
    private void filterTemplates() {
        synchronized (templates) {
            activeFilters.clear();
            if (infoBtn.isSelected()) activeFilters.add("info");
            if (lowBtn.isSelected()) activeFilters.add("low");
            if (mediumBtn.isSelected()) activeFilters.add("medium");
            if (highBtn.isSelected()) activeFilters.add("high");
            if (criticalBtn.isSelected()) activeFilters.add("critical");
            activeFilters.add("unknown");

            updateTableDataWithMapping();
        }
    }

    /**
     * 重新后台加载模板，并在 3 秒后强制恢复刷新按钮。
     */
    public void refreshTemplates() {
        refreshBtn.setEnabled(false);
        loadTemplatesInBackground();

        Timer timer = new Timer(3000, e -> refreshBtn.setEnabled(true));
        timer.setRepeats(false);
        timer.start();
    }

    private void updateStatus(String message) {
        statusLabel.setText(message);
        statusLabel.setToolTipText(message);
    }

    /**
     * 弹出含异常堆栈的错误对话框。
     */
    private void showErrorDialog(String title, String message, Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);

        JTextArea textArea = new JTextArea(sw.toString());
        textArea.setEditable(false);
        textArea.setRows(10);

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(600, 400));

        JOptionPane.showMessageDialog(this,
                new Object[]{message, scrollPane},
                title,
                JOptionPane.ERROR_MESSAGE);
    }

    /**
     * 组装右键菜单：编辑、删除、选中数量、打开目录、复制命令及工作流操作。
     */
    private JPopupMenu createPopupMenu() {
        JPopupMenu menu = new JPopupMenu();

        addEditMenuItem(menu);

        addDeleteMenuItem(menu);

        menu.add(new JMenuItem("已选中POC数量: " +
                templateTable.getSelectedRowCount()));

        addOpenFolderMenuItem(menu);

        addCopyCommandMenuItem(menu);

        addWorkflowMenuItem(menu);

        addAddToWorkflowMenuItem(menu);

        return menu;
    }

    /**
     * 添加"加入到已有工作流"菜单项：列出工作流目录下的 .yaml 文件供选择。
     */
    private void addAddToWorkflowMenuItem(JPopupMenu menu) {
        JMenuItem addToWorkflowItem = new JMenuItem("加入到已有工作流");
        addToWorkflowItem.addActionListener(e -> {
            File workflowDir = new File(Utils.workflowPath);
            if (!workflowDir.exists() || !workflowDir.isDirectory()) {
                JOptionPane.showMessageDialog(this,
                        "工作流目录不存在",
                        "错误",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            File[] workflowFiles = workflowDir.listFiles((dir, name) -> name.endsWith(".yaml"));
            if (workflowFiles == null || workflowFiles.length == 0) {
                JOptionPane.showMessageDialog(this,
                        "当前没有可用的工作流",
                        "提示",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            String[] workflowNames = new String[workflowFiles.length];
            for (int i = 0; i < workflowFiles.length; i++) {
                workflowNames[i] = workflowFiles[i].getName().replace(".yaml", "");
            }

            String selectedWorkflow = (String) JOptionPane.showInputDialog(
                    this,
                    "请选择要加入的工作流:",
                    "选择工作流",
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    workflowNames,
                    workflowNames[0]);

            if (selectedWorkflow != null) {
                addTemplatesToExistingWorkflow(selectedWorkflow);
            }
        });
        menu.add(addToWorkflowItem);
    }

    /**
     * 将选中模板路径追加到指定工作流的 templates 列表，重复项经确认后跳过，最后写回文件。
     */
    private void addTemplatesToExistingWorkflow(String workflowName) {
        try {
            int[] selectedRows = templateTable.getSelectedRows();
            List<String> templatePaths = new ArrayList<>();

            for (int viewRow : selectedRows) {
                String path = getTemplatePath(viewRow);
                if (path != null) {
                    templatePaths.add(path);
                }
            }

            if (templatePaths.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "未能获取有效的模板路径",
                        "错误",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            File workflowFile = new File(Utils.workflowPath, workflowName + ".yaml");
            if (!workflowFile.exists()) {
                JOptionPane.showMessageDialog(this,
                        "工作流文件不存在",
                        "错误",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            Yaml yaml = YamlUtils.newYamlReader();
            Map<String, List<String>> workflowData;
            try (FileReader reader = new FileReader(workflowFile)) {
                workflowData = yaml.load(reader);
                if (workflowData == null) {
                    workflowData = new HashMap<>();
                }
                if (!workflowData.containsKey("templates")) {
                    workflowData.put("templates", new ArrayList<>());
                }
            }

            List<String> existingTemplates = workflowData.get("templates");
            List<String> duplicateTemplates = new ArrayList<>();
            List<String> templatestoAdd = new ArrayList<>();

            for (String newPath : templatePaths) {
                if (existingTemplates.contains(newPath)) {
                    duplicateTemplates.add(newPath);
                } else {
                    templatestoAdd.add(newPath);
                }
            }

            if (!duplicateTemplates.isEmpty()) {
                StringBuilder message = new StringBuilder("以下模板已存在于工作流中：\n");
                for (String path : duplicateTemplates) {
                    message.append(path).append("\n");
                }
                message.append("\n是否继续添加其他非重复模板？");

                int response = JOptionPane.showConfirmDialog(this,
                        message.toString(),
                        "发现重复模板",
                        JOptionPane.YES_NO_OPTION);

                if (response != JOptionPane.YES_OPTION) {
                    return;
                }
            }

            if (templatestoAdd.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "没有新的模板可以添加",
                        "提示",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            existingTemplates.addAll(templatestoAdd);

            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            options.setPrettyFlow(true);

            yaml = new Yaml(options);
            try (FileWriter writer = new FileWriter(workflowFile)) {
                yaml.dump(workflowData, writer);
            }

            JOptionPane.showMessageDialog(this,
                    "成功添加 " + templatestoAdd.size() + " 个模板到工作流",
                    "提示",
                    JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception e) {
            showErrorDialog("添加模板到工作流失败", "操作失败: " + e.getMessage(), e);
        }
    }

    private void addEditMenuItem(JPopupMenu menu) {
        JMenuItem editItem = new JMenuItem("编辑选中模板");
        editItem.addActionListener(e -> {
            int row = templateTable.getSelectedRow();
            if (row != -1) {
                editTemplate(row);
            }
        });
        menu.add(editItem);
    }

    private void addDeleteMenuItem(JPopupMenu menu) {
        JMenuItem deleteItem = new JMenuItem("删除选中模板");
        deleteItem.addActionListener(e -> {
            int[] rows = templateTable.getSelectedRows();
            if (rows.length > 0) {
                deleteTemplates(rows);
            }
        });
        menu.add(deleteItem);
    }

    private void addOpenFolderMenuItem(JPopupMenu menu) {
        JMenuItem openFolderItem = new JMenuItem("打开模板所在文件夹");
        openFolderItem.addActionListener(e -> {
            int row = templateTable.getSelectedRow();
            if (row != -1) {
                openTemplateFolder(row);
            }
        });
        menu.add(openFolderItem);
    }

    private void addCopyCommandMenuItem(JPopupMenu menu) {
        JMenuItem copyCommandItem = new JMenuItem("复制运行命令");
        copyCommandItem.addActionListener(e -> {
            int[] rows = templateTable.getSelectedRows();
            if (rows.length > 0) {
                copyRunCommand(rows);
            }
        });
        menu.add(copyCommandItem);
    }

    private void addWorkflowMenuItem(JPopupMenu menu) {
        JMenuItem workflowItem = new JMenuItem("创建工作流");
        workflowItem.addActionListener(e -> {
            int[] rows = templateTable.getSelectedRows();
            if (rows.length > 0) {
                createWorkflow(rows);
            }
        });
        menu.add(workflowItem);
    }

    /**
     * 读取模板文件内容并打开编辑器。
     *
     * @param row 视图行号（排序/过滤后的索引）
     */
    private void editTemplate(int row) {
        try {
            String path = getTemplatePath(row);
            if (path != null) {
                String content = new String(Files.readAllBytes(Paths.get(path)),
                        StandardCharsets.UTF_8);
                showTemplateEditor(path, content);
            }
        } catch (IOException e) {
            showErrorDialog("编辑失败", "无法打开模板文件", e);
        }
    }

    /**
     * 经确认后删除选中的模板文件并重新加载列表。
     *
     * @param rows 视图行号数组
     */
    private void deleteTemplates(int[] rows) {
        int confirm = JOptionPane.showConfirmDialog(this,
                "确定要删除选中的 " + rows.length + " 个模板吗？",
                "确认删除",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            for (int row : rows) {
                try {
                    String path = getTemplatePath(row);
                    if (path != null) {
                        Files.delete(Paths.get(path));
                    }
                } catch (IOException e) {
                    showErrorDialog("删除失败", "无法删除模板文件", e);
                }
            }
            loadTemplatesInBackground();
        }
    }

    /**
     * 在系统文件管理器中定位模板文件，按操作系统分别处理。
     *
     * @param row 视图行号
     */
    private void openTemplateFolder(int row) {
        try {
            String path = getTemplatePath(row);
            if (path != null) {
                Path templatePath = Paths.get(path);
                File templateFile = templatePath.toFile();

                if (!templateFile.exists()) {
                    JOptionPane.showMessageDialog(this,
                            "模板文件不存在: " + templateFile.getAbsolutePath(),
                            "错误",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // Windows 用 explorer /select 定位并选中文件
                if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                    Runtime.getRuntime().exec("explorer.exe /select," + templateFile.getAbsolutePath());
                }
                // macOS 用 open -R 在 Finder 中显示文件
                else if (System.getProperty("os.name").toLowerCase().contains("mac")) {
                    Runtime.getRuntime().exec(new String[]{"open", "-R", templateFile.getAbsolutePath()});
                }
                // 其他系统只能打开父目录，无法定位到具体文件
                else {
                    Desktop.getDesktop().open(templateFile.getParentFile());
                }
            }
        } catch (IOException e) {
            showErrorDialog("打开失败", "无法打开文件夹: " + e.getMessage(), e);
        }
    }

    /**
     * 为选中模板生成 nuclei 运行命令并复制到剪贴板。
     */
    private void copyRunCommand(int[] rows) {
        List<String> templatePaths = new ArrayList<>();
        for (int row : rows) {
            String path = getTemplatePath(row);
            if (path != null) {
                templatePaths.add(path);
            }
        }

        if (!templatePaths.isEmpty()) {
            String command = generateRunCommand(templatePaths);
            copyToClipboard(command);
            JOptionPane.showMessageDialog(this,
                    "命令已复制到剪贴板",
                    "提示",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * 弹窗输入工作流名称，将选中模板保存为新工作流文件。
     */
    private void createWorkflow(int[] rows) {
        String name = JOptionPane.showInputDialog(this,
                "请输入工作流名称：",
                "创建工作流",
                JOptionPane.QUESTION_MESSAGE);

        if (name != null && !name.trim().isEmpty()) {
            List<String> templatePaths = new ArrayList<>();
            for (int row : rows) {
                String path = getTemplatePath(row);
                if (path != null) {
                    templatePaths.add(path);
                }
            }

            if (!templatePaths.isEmpty()) {
                saveWorkflow(name.trim(), templatePaths);
            }
        }
    }

    /**
     * 由视图行号查模板文件路径，行号无效或未映射时返回 null。
     */
    private String getTemplatePath(int viewRow) {
        if (viewRow < 0) return null;

        int modelRow = templateTable.convertRowIndexToModel(viewRow);
        if (modelRow < 0 || modelRow >= tableModel.getRowCount()) return null;

        String numStr = (String) tableModel.getValueAt(modelRow, 0);
        try {
            int num = Integer.parseInt(numStr);
            return templatePathMap.get(num);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 拼接 nuclei 批量运行命令，末尾 -u 后留空待补充目标。
     */
    private String generateRunCommand(List<String> templatePaths) {
        return "nuclei -t " + String.join(",", templatePaths) + " " + Utils.templateArg + " -u ";
    }

    private void copyToClipboard(String text) {
        Toolkit.getDefaultToolkit()
                .getSystemClipboard()
                .setContents(new StringSelection(text), null);
    }

    /**
     * 以块式 YAML 将模板路径列表写入工作流目录下的 name.yaml，失败时弹错误框。
     */
    private void saveWorkflow(String name, List<String> templatePaths) {
        try {
            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            options.setPrettyFlow(true);

            Map<String, Object> workflow = new HashMap<>();
            workflow.put("templates", templatePaths);

            Yaml yaml = new Yaml(options);
            String workflowPath = Utils.workflowPath + File.separator + name + ".yaml";

            try (FileWriter writer = new FileWriter(workflowPath)) {
                yaml.dump(workflow, writer);
            }

            JOptionPane.showMessageDialog(this,
                    "工作流已保存：" + workflowPath,
                    "成功",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            showErrorDialog("保存失败", "无法保存工作流", e);
        }
    }

    /**
     * 弹出模态 YAML 编辑器；Ctrl+S 或保存按钮以 UTF-8 覆盖原文件并重新加载列表，Esc/取消关闭。
     */
    private void showTemplateEditor(String path, String content) {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
                "编辑模板", true);

        RSyntaxTextArea textArea = new RSyntaxTextArea(20, 60);
        textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_YAML);
        textArea.setCodeFoldingEnabled(true);
        textArea.setAntiAliasingEnabled(true);
        textArea.setText(content);
        textArea.setFont(UIUtils.CODE_FONT);

        try {
            Theme theme = Theme.load(getClass().getResourceAsStream(
                    "/org/fife/ui/rsyntaxtextarea/themes/monokai.xml"));
            theme.apply(textArea);
        } catch (Exception e) {
            // 主题加载失败时继续使用默认主题
            e.printStackTrace();
        }

        RTextScrollPane scrollPane = new RTextScrollPane(textArea);
        scrollPane.setFoldIndicatorEnabled(true);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton("保存");
        JButton cancelButton = new JButton("取消");

        KeyStroke ctrlS = KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK);
        KeyStroke escKey = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);

        dialog.getRootPane().registerKeyboardAction(e -> {
            try {
                Files.write(Paths.get(path),
                        textArea.getText().getBytes(StandardCharsets.UTF_8));
                dialog.dispose();
                loadTemplatesInBackground();
            } catch (IOException ex) {
                showErrorDialog("保存失败", "无法保存模板文件", ex);
            }
        }, ctrlS, JComponent.WHEN_IN_FOCUSED_WINDOW);

        dialog.getRootPane().registerKeyboardAction(e -> dialog.dispose(),
                escKey, JComponent.WHEN_IN_FOCUSED_WINDOW);

        saveButton.addActionListener(e -> {
            try {
                Files.write(Paths.get(path),
                        textArea.getText().getBytes(StandardCharsets.UTF_8));
                dialog.dispose();
                loadTemplatesInBackground();
            } catch (IOException ex) {
                showErrorDialog("保存失败", "无法保存模板文件", ex);
            }
        });

        cancelButton.addActionListener(e -> dialog.dispose());

        buttonPanel.add(saveButton);
        buttonPanel.add(Box.createHorizontalStrut(10));
        buttonPanel.add(cancelButton);

        dialog.setLayout(new BorderLayout());
        dialog.add(scrollPane, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setSize(1000, 800);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    /**
     * 组件从容器移除时关闭后台加载线程池。
     */
    @Override
    public void removeNotify() {
        super.removeNotify();
        shutdownExecutor();
    }

    /**
     * 关闭线程池，最多等待 3 秒，超时或被中断则强制终止。
     */
    private void shutdownExecutor() {
        try {
            executor.shutdown();
            if (!executor.awaitTermination(3, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 搜索框防抖监听：输入停止 300ms 后在 EDT 上重建表格。
     */
    private class SearchDocumentListener implements DocumentListener {
        private final Timer timer = new Timer(300, e -> {
            synchronized (templates) {
                updateTableDataWithMapping();
            }
        });

        public SearchDocumentListener() {
            timer.setRepeats(false);
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
            timer.restart();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            timer.restart();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            timer.restart();
        }
    }

    /**
     * 右键菜单监听：按下与释放都检查触发，兼容不同平台的弹出时机，且仅在有选中行时显示。
     */
    private class PopupListener extends MouseAdapter {
        private final JTable table;

        PopupListener(JTable table) {
            this.table = table;
        }

        @Override
        public void mousePressed(MouseEvent e) {
            showPopupIfTriggered(e);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            showPopupIfTriggered(e);
        }

        private void showPopupIfTriggered(MouseEvent e) {
            if (e.isPopupTrigger() && table.getSelectedRow() != -1) {
                createPopupMenu().show(e.getComponent(), e.getX(), e.getY());
            }
        }
    }


}