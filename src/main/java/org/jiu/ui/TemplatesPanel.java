package org.jiu.ui;

import javax.swing.*;
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

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.jiu.utils.Utils;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.DumperOptions;

/**
 * 模板管理面板
 * 用于显示和管理YAML格式的模板文件
 */
public class TemplatesPanel extends JPanel {
    // 常量定义
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

    // UI组件
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final JLabel statusLabel;
    private final JTable templateTable;
    private final DefaultTableModel tableModel;
    private final TableRowSorter<DefaultTableModel> sorter;
    private final JTextField searchField;
    private final Object loadLock = new Object();

    // 过滤按钮
    private final JToggleButton infoBtn;
    private final JToggleButton lowBtn;
    private final JToggleButton mediumBtn;
    private final JToggleButton highBtn;
    private final JToggleButton criticalBtn;
    private final JButton refreshBtn;

    // 数据存储
    private final List<Map<String, Object>> templates;
    private final Set<String> activeFilters;
    private final Map<Integer, String> templatePathMap = new HashMap<>();
    private Map<Path, Exception> failedFiles = Collections.synchronizedMap(new HashMap<>());
    /**
     * 构造函数
     */
    public TemplatesPanel() {
        setLayout(new BorderLayout());

        // 初始化数据存储
        templates = new ArrayList<>();
        activeFilters = new HashSet<>();

        // 初始化状态标签
        statusLabel = new JLabel("正在初始化...");

        // 初始化表格模型和表格
        tableModel = createTableModel();
        templateTable = createTable();
        sorter = new TableRowSorter<>(tableModel);
        templateTable.setRowSorter(sorter);

        // 初始化搜索框
        searchField = createSearchField();

        // 初始化过滤按钮
        infoBtn = createFilterButton("信息", "info");
        lowBtn = createFilterButton("低", "low");
        mediumBtn = createFilterButton("中", "medium");
        highBtn = createFilterButton("高", "high");
        criticalBtn = createFilterButton("危", "critical");

        // 初始化刷新按钮
        refreshBtn = createRefreshButton();

        // 初始化UI
        initUI();

        // 加载初始数据
        loadTemplatesInBackground();
    }

    /**
     * 创建表格模型
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
     * 创建表格
     */
    private JTable createTable() {
        JTable table = new JTable(tableModel);

        // 设置表格属性
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.setAutoCreateRowSorter(true);
        table.setFillsViewportHeight(true);
        table.setRowHeight(25);
        table.setFont(new Font("微软雅黑", Font.PLAIN, 12));

        // 设置表格渲染器
        DefaultTableCellRenderer centerRenderer = createCenterRenderer();
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }
        
        // 为严重程度列设置特殊渲染器
        table.getColumnModel().getColumn(3).setCellRenderer(createSeverityRenderer());

        // 设置列宽
        configureColumnWidths(table);

        // 添加右键菜单
        table.addMouseListener(new PopupListener(table));

        return table;
    }

    /**
     * 创建居中渲染器
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
     * 创建严重程度渲染器
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
     * 配置表格列宽
     */
    private void configureColumnWidths(JTable table) {
        // 设置表格行高和字体
        table.setRowHeight(25);
        table.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        
        // 设置列宽
        table.getColumnModel().getColumn(0).setPreferredWidth(40);   // #
        table.getColumnModel().getColumn(1).setPreferredWidth(120);  // ID
        table.getColumnModel().getColumn(2).setPreferredWidth(200);  // Name
        table.getColumnModel().getColumn(3).setPreferredWidth(60);   // Severity - 使用颜色标识
        table.getColumnModel().getColumn(4).setPreferredWidth(150);  // Tags
        table.getColumnModel().getColumn(5).setPreferredWidth(80);   // Author
        table.getColumnModel().getColumn(6).setPreferredWidth(250);  // Description
        table.getColumnModel().getColumn(7).setPreferredWidth(180);  // Reference
    }

    /**
     * 创建搜索框
     */
    private JTextField createSearchField() {
        JTextField field = new JTextField(20);
        field.putClientProperty("JTextField.placeholderText", "输入关键词搜索...");
        field.getDocument().addDocumentListener(new SearchDocumentListener());
        return field;
    }

    /**
     * 创建过滤按钮
     */
    private JToggleButton createFilterButton(String text, String severity) {
        JToggleButton button = new JToggleButton(text);
        button.setSelected(true);
        button.setToolTipText(severity + "级别模板");
        button.addActionListener(e -> filterTemplates());

        // 根据严重级别设置颜色
        Color selectedColor = new Color(0, 123, 255); // 默认蓝色
        Color normalColor = Color.WHITE;

        if ("信息".equals(text)) {
            selectedColor = new Color(108, 117, 125); // 灰色
        } else if ("低".equals(text)) {
            selectedColor = new Color(40, 167, 69); // 绿色
        } else if ("中".equals(text)) {
            selectedColor = new Color(255, 193, 7); // 黄色
        } else if ("高".equals(text)) {
            selectedColor = new Color(255, 152, 0); // 橙色
        } else if ("危".equals(text)) {
            selectedColor = new Color(220, 53, 69); // 红色
        }

        button.setBackground(normalColor);
        button.setForeground(Color.BLACK);
        button.setSelected(true);
        button.setBackground(selectedColor);
        button.setForeground(Color.WHITE);

        return button;
    }

    /**
     * 创建刷新按钮
     */
    private JButton createRefreshButton() {
        JButton button = new JButton("刷新");
        button.setToolTipText("刷新模板列表");
        button.addActionListener(e -> refreshTemplates());
        button.setBackground(new Color(0, 123, 255));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        return button;
    }
    /**
     * 初始化UI组件
     */
    private void initUI() {
        // 创建主工具栏
        JToolBar toolBar = createToolBar();
        add(toolBar, BorderLayout.NORTH);

        // 创建表格滚动面板
        JScrollPane scrollPane = new JScrollPane(templateTable);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        add(scrollPane, BorderLayout.CENTER);

        // 初始化过滤器
        initializeFilters();
    }

    /**
     * 创建工具栏
     */
    private JToolBar createToolBar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // 创建主面板
        JPanel mainPanel = new JPanel(new BorderLayout());
        
        // 左侧控件：状态和过滤器
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        
        // 状态标签
        statusLabel.setPreferredSize(new Dimension(200, 25));
        statusLabel.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        leftPanel.add(statusLabel);
        
        // 分隔符
        leftPanel.add(Box.createHorizontalStrut(10));
        
        // 过滤器按钮
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        filterPanel.add(new JLabel("过滤:"));
        
        // 统一按钮样式
        Dimension buttonSize = new Dimension(50, 25);
        for (JToggleButton btn : new JToggleButton[]{infoBtn, lowBtn, mediumBtn, highBtn, criticalBtn}) {
            btn.setPreferredSize(buttonSize);
            btn.setFont(new Font("微软雅黑", Font.PLAIN, 11));
            btn.setFocusPainted(false);
            filterPanel.add(btn);
        }
        
        leftPanel.add(filterPanel);
        
        // 刷新按钮
        refreshBtn.setPreferredSize(new Dimension(60, 25));
        refreshBtn.setFont(new Font("微软雅黑", Font.PLAIN, 11));
        refreshBtn.setFocusPainted(false);
        leftPanel.add(refreshBtn);
        
        // 分隔符
        leftPanel.add(Box.createHorizontalStrut(10));
        
        // 右侧控件：搜索框
        JPanel rightPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 0, 0);
        searchField.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        searchField.putClientProperty("JTextField.placeholderText", "搜索模板 (Ctrl+F)...");
        rightPanel.add(searchField, gbc);
        
        // 将左右面板添加到主面板
        mainPanel.add(leftPanel, BorderLayout.WEST);
        mainPanel.add(rightPanel, BorderLayout.EAST);
        
        toolBar.add(mainPanel);
        return toolBar;
    }

    /**
     * 初始化过滤器
     */
    private void initializeFilters() {
        // 初始化活动过滤器集合
        activeFilters.clear();
        activeFilters.addAll(Arrays.asList(SEVERITY_LEVELS));
    }

    /**
     * 在后台加载模板数据
     */
    private void loadTemplatesInBackground() {
        updateStatus("正在加载模板...");
        refreshBtn.setEnabled(false);

        executor.submit(() -> {
            try {
                synchronized (loadLock) {
                    String templatePath = Utils.templatePath;
                    // 如果路径为空，直接更新状态并返回
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
     * 加载模板数据
     */
    private void loadTemplates() throws IOException {
        String templatePath = Utils.templatePath;

        if (templatePath == null || templatePath.trim().isEmpty()) {
            return;
        }
        // 检查路径是否有效
        if (!isValidPath(templatePath)) {
            throw new IOException("无效的模板路径: " + templatePath);
        }

        // 清空现有数据
        SwingUtilities.invokeLater(() -> {
            synchronized (templates) {
                templates.clear();
                templatePathMap.clear();
                tableModel.setRowCount(0);
            }
        });

        // 重要：在开始新的加载前清空失败记录
        failedFiles.clear();

        // 先收集所有模板
        List<Map<String, Object>> tempTemplates = new ArrayList<>();
        final int[] stats = new int[]{0, 0, 0}; // 总数，成功，失败

        Files.walkFileTree(Paths.get(templatePath), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.toString().endsWith(".yaml")) {
                    stats[0]++; // 总数增加
                    try {
                        Map<String, Object> template = loadYamlFile(file);
                        if (template != null && isValidTemplate(template)) {
                            template.put("path", file.toString()); // 保存文件路径
                            tempTemplates.add(template);
                            stats[1]++; // 成功数增加
                            updateProgressStatus(stats);
                        } else {
                            stats[2]++; // 失败数增加
                            handleTemplateLoadError(file, new Exception("无效的模板格式"));
                        }
                    } catch (Exception e) {
                        stats[2]++; // 失败数增加
                        handleTemplateLoadError(file, e);
                    }
                    updateProgressStatus(stats);
                }
                return FileVisitResult.CONTINUE;
            }
        });

        // 更新UI和数据结构
        SwingUtilities.invokeLater(() -> {
            synchronized (templates) {
                templates.clear();
                templates.addAll(tempTemplates);

                // 更新表格数据
                updateTableDataWithMapping();
                updateFinalStatus(stats);
                refreshBtn.setEnabled(true);
            }
        });
    }

    /**
     * 使用映射更新表格数据
     */
    private void updateTableDataWithMapping() {
        tableModel.setRowCount(0);
        templatePathMap.clear();
        List<Map<String, Object>> visibleTemplates = new ArrayList<>();

        // 过滤可见的模板
        for (Map<String, Object> template : templates) {
            if (isTemplateVisible(template)) {
                visibleTemplates.add(template);
            }
        }

        // 更新表格和映射
        for (int i = 0; i < visibleTemplates.size(); i++) {
            Map<String, Object> template = visibleTemplates.get(i);
            int rowNum = i + 1;

            // 保存映射关系
            String path = (String) template.get("path");
            templatePathMap.put(rowNum, path);

            // 添加表格数据
            Object[] rowData = createRowData(rowNum, template);
            tableModel.addRow(rowData);
        }

        updateStatus(String.format("显示 %d 个模板（共 %d 个）", visibleTemplates.size(), templates.size()));
    }



    /**
     * 验证模板路径
     */
    private boolean isValidPath(String path) {
        return path != null && !path.isEmpty() && Files.exists(Paths.get(path));
    }

    /**
     * 加载YAML文件
     */
    private Map<String, Object> loadYamlFile(Path file) throws IOException {
        try (InputStream is = Files.newInputStream(file)) {
            Yaml yaml = new Yaml();
            Map<String, Object> template = yaml.load(is);
            if (template != null) {
                template.put("path", file.toString());
            }
            return template;
        }
    }

    /**
     * 验证模板数据的有效性
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
     * 更新进度状态
     */
    private void updateProgressStatus(int[] stats) {
        SwingUtilities.invokeLater(() ->
                statusLabel.setText(String.format("正在加载... 总数: %d, 成功: %d, 失败: %d",
                        stats[0], stats[1], stats[2]))
        );
    }

    /**
     * 更新最终状态
     */
    private void updateFinalStatus(int[] stats) {
        StringBuilder status = new StringBuilder();
        status.append(String.format("加载完成 - 总数: %d, 成功: %d, 失败: %d",
                stats[0], stats[1], stats[2]));

        // 重要：同时检查 stats[2] 和 failedFiles.size()
        if (stats[2] > 0 && !failedFiles.isEmpty()) {
            status.append(" (点击查看失败详情)");

            // 清除之前的所有MouseListener
            for (MouseListener listener : statusLabel.getMouseListeners()) {
                statusLabel.removeMouseListener(listener);
            }

            // 添加新的点击事件监听器
            statusLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            statusLabel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    showFailedFilesDialog();
                }
            });
        } else {
            statusLabel.setCursor(Cursor.getDefaultCursor());
            // 移除之前的所有MouseListener
            for (MouseListener listener : statusLabel.getMouseListeners()) {
                statusLabel.removeMouseListener(listener);
            }
        }

        String statusText = status.toString();
        statusLabel.setText(statusText);
        statusLabel.setToolTipText(statusText);
    }

    /**
     * 显示失败文件详情对话框
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
            // 添加异常堆栈信息
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            ex.printStackTrace(pw);
            message.append("详细信息:\n").append(sw.toString()).append("\n");
            message.append("----------------------------------------\n\n");
        });

        // 创建一个更友好的显示界面
        JTextArea textArea = new JTextArea(message.toString());
        textArea.setEditable(false);
        textArea.setCaretPosition(0); // 滚动到顶部
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(800, 400));

        // 添加复制按钮
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

        // 创建一个包含滚动面板和按钮的面板
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(copyButton, BorderLayout.SOUTH);

        // 显示对话框
        JOptionPane.showMessageDialog(this,
                panel,
                "加载失败详情",
                JOptionPane.ERROR_MESSAGE);
    }

    /**
     * 处理模板加载错误
     */
    private void handleTemplateLoadError(Path file, Exception e) {
        // 在UI线程外添加错误记录
        failedFiles.put(file, e);

        // 在UI线程中打印错误日志
        SwingUtilities.invokeLater(() -> {
            System.err.println("加载模板失败: " + file);
            System.err.println("错误信息: " + e.getMessage());
            e.printStackTrace();
        });
    }

    /**
     * 处理整体加载错误
     */
    private void handleLoadError(Exception e) {
        StringBuilder errorMessage = new StringBuilder("加载模板失败:\n\n");

        // 添加主要错误信息
        errorMessage.append("主要错误: ").append(e.getMessage()).append("\n\n");

        // 如果有具体的文件加载失败信息，添加到错误信息中
        if (!failedFiles.isEmpty()) {
            errorMessage.append("以下文件加载失败:\n");
            failedFiles.forEach((path, ex) -> {
                errorMessage.append("文件: ").append(path).append("\n");
                errorMessage.append("原因: ").append(ex.getMessage()).append("\n\n");
            });
        }

        // 更新状态标签
        String statusText = String.format("加载失败 - %d 个文件出错", failedFiles.size());
        statusLabel.setText(statusText);
        statusLabel.setToolTipText(statusText);

        // 显示错误对话框
        showErrorDialog("加载模板失败", errorMessage.toString(), e);

        // 清除错误记录
        failedFiles.clear();
    }
    /**
     * 更新表格数据
     */
    private void updateTableData() {
        tableModel.setRowCount(0);
        int displayedCount = 0;

        // 创建一个临时的显示列表
        List<Map<String, Object>> visibleTemplates = new ArrayList<>();

        // 先收集所有应该显示的模板
        for (Map<String, Object> template : templates) {
            if (isTemplateVisible(template)) {
                visibleTemplates.add(template);
            }
        }

        // 再添加到表格中
        for (int i = 0; i < visibleTemplates.size(); i++) {
            Map<String, Object> template = visibleTemplates.get(i);
            displayedCount++;
            Object[] rowData = createRowData(i + 1, template);
            tableModel.addRow(rowData);
        }

        updateStatus(String.format("显示 %d 个模板（共 %d 个）", displayedCount, templates.size()));
    }

    /**
     * 创建表格行数据
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
     * 获取模板字段值
     */
    private String getTemplateValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : "";
    }

    /**
     * 格式化标签
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
     * 判断模板是否应该显示
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

        // 检查严重程度过滤器
        if (!activeFilters.contains(severity.toLowerCase())) {
            return false;
        }

        // 检查搜索过滤器
        String searchText = searchField.getText().toLowerCase().trim();
        if (!searchText.isEmpty()) {
            return matchesSearch(template, info, searchText);
        }

        return true;
    }

    /**
     * 检查模板是否匹配搜索条件
     */
    private boolean matchesSearch(Map<String, Object> template, Map<String, Object> info, String searchText) {
        // 检查所有相关字段
        return containsIgnoreCase(getTemplateValue(template, "id"), searchText) ||
                containsIgnoreCase(getTemplateValue(info, "name"), searchText) ||
                containsIgnoreCase(getTemplateValue(info, "author"), searchText) ||
                containsIgnoreCase(getTemplateValue(info, "description"), searchText) ||
                containsIgnoreCase(formatTags(info.get("tags")), searchText);
    }

    /**
     * 不区分大小写的包含检查
     */
    private boolean containsIgnoreCase(String text, String searchText) {
        return text.toLowerCase().contains(searchText);
    }

    /**
     * 执行模板过滤
     */
    private void filterTemplates() {
        synchronized (templates) {
            // 更新活动过滤器
            activeFilters.clear();
            if (infoBtn.isSelected()) activeFilters.add("info");
            if (lowBtn.isSelected()) activeFilters.add("low");
            if (mediumBtn.isSelected()) activeFilters.add("medium");
            if (highBtn.isSelected()) activeFilters.add("high");
            if (criticalBtn.isSelected()) activeFilters.add("critical");
            activeFilters.add("unknown");

            // 更新表格显示
            updateTableDataWithMapping();
        }
    }

    /**
     * 刷新模板
     */
    public void refreshTemplates() {
        refreshBtn.setEnabled(false);
        loadTemplatesInBackground();

        // 3秒后重新启用刷新按钮
        Timer timer = new Timer(3000, e -> refreshBtn.setEnabled(true));
        timer.setRepeats(false);
        timer.start();
    }

    /**
     * 更新状态标签
     */
    private void updateStatus(String message) {
        statusLabel.setText(message);
        statusLabel.setToolTipText(message);
    }

    /**
     * 搜索文档监听器
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
     * 错误对话框显示
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
     * 创建右键菜单
     */
    private JPopupMenu createPopupMenu() {
        JPopupMenu menu = new JPopupMenu();

        // 添加编辑选项
        addEditMenuItem(menu);

        // 添加删除选项
        addDeleteMenuItem(menu);

        // 添加选中数量显示
        menu.add(new JMenuItem("已选中POC数量: " +
                templateTable.getSelectedRowCount()));

        // 添加打开文件夹选项
        addOpenFolderMenuItem(menu);

        // 添加命令复制选项
        addCopyCommandMenuItem(menu);

        // 添加工作流选项
        addWorkflowMenuItem(menu);

        // 加入到已有工作流选项
        addAddToWorkflowMenuItem(menu);

        return menu;
    }

    /**
     *
     * @param menu
     */
    private void addAddToWorkflowMenuItem(JPopupMenu menu) {
        JMenuItem addToWorkflowItem = new JMenuItem("加入到已有工作流");
        addToWorkflowItem.addActionListener(e -> {
            // 获取已有工作流列表
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

            // 创建工作流名称列表
            String[] workflowNames = new String[workflowFiles.length];
            for (int i = 0; i < workflowFiles.length; i++) {
                workflowNames[i] = workflowFiles[i].getName().replace(".yaml", "");
            }

            // 显示工作流选择对话框
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

    private void addTemplatesToExistingWorkflow(String workflowName) {
        try {
            // 获取选中的模板路径
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

            // 读取现有工作流
            File workflowFile = new File(Utils.workflowPath, workflowName + ".yaml");
            if (!workflowFile.exists()) {
                JOptionPane.showMessageDialog(this,
                        "工作流文件不存在",
                        "错误",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            // 读取工作流数据
            Yaml yaml = new Yaml();
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

            // 检查重复模板
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

            // 处理重复模板
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

            // 检查是否有新模板可添加
            if (templatestoAdd.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "没有新的模板可以添加",
                        "提示",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            // 添加新模板
            existingTemplates.addAll(templatestoAdd);

            // 保存更新后的工作流
            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            options.setPrettyFlow(true);

            yaml = new Yaml(options);
            try (FileWriter writer = new FileWriter(workflowFile)) {
                yaml.dump(workflowData, writer);
            }

            // 显示成功消息
            JOptionPane.showMessageDialog(this,
                    "成功添加 " + templatestoAdd.size() + " 个模板到工作流",
                    "提示",
                    JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception e) {
            showErrorDialog("添加模板到工作流失败", "操作失败: " + e.getMessage(), e);
        }
    }

    /**
     * 添加编辑菜单项
     */
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

    /**
     * 添加删除菜单项
     */
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

    /**
     * 添加打开文件夹菜单项
     */
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

    /**
     * 添加复制命令菜单项
     */
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

    /**
     * 添加工作流菜单项
     */
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
     * 编辑模板
     */
    private void editTemplate(int row) {
        try {
            String path = getTemplatePath(row);
            if (path != null) {
                String content = new String(Files.readAllBytes(Paths.get(path)),
                        StandardCharsets.UTF_8);
                // 这里调用你的编辑器组件
                showTemplateEditor(path, content);
            }
        } catch (IOException e) {
            showErrorDialog("编辑失败", "无法打开模板文件", e);
        }
    }

    /**
     * 删除模板
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
     * 打开模板所在文件夹
     */
    private void openTemplateFolder(int row) {
        try {
            String path = getTemplatePath(row);
            if (path != null) {
                Path templatePath = Paths.get(path);
                File templateFile = templatePath.toFile();

                // 检查文件是否存在
                if (!templateFile.exists()) {
                    JOptionPane.showMessageDialog(this,
                            "模板文件不存在: " + templateFile.getAbsolutePath(),
                            "错误",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // 在Windows系统上使用explorer定位到文件
                if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                    Runtime.getRuntime().exec("explorer.exe /select," + templateFile.getAbsolutePath());
                }
                // 在macOS系统上使用Finder定位到文件
                else if (System.getProperty("os.name").toLowerCase().contains("mac")) {
                    Runtime.getRuntime().exec(new String[]{"open", "-R", templateFile.getAbsolutePath()});
                }
                // 在Linux系统上尝试使用默认文件管理器
                else {
                    // 首先尝试使用xdg-open打开父目录
                    Desktop.getDesktop().open(templateFile.getParentFile());
                }
            }
        } catch (IOException e) {
            showErrorDialog("打开失败", "无法打开文件夹: " + e.getMessage(), e);
        }
    }

    /**
     * 复制运行命令
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
     * 创建工作流
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
     * 获取模板路径
     */
    private String getTemplatePath(int viewRow) {
        if (viewRow < 0) return null;

        // 转换为模型索引
        int modelRow = templateTable.convertRowIndexToModel(viewRow);
        if (modelRow < 0 || modelRow >= tableModel.getRowCount()) return null;

        // 获取行号
        String numStr = (String) tableModel.getValueAt(modelRow, 0);
        try {
            int num = Integer.parseInt(numStr);
            return templatePathMap.get(num);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 生成运行命令
     */
    private String generateRunCommand(List<String> templatePaths) {
        // 这里根据你的需求生成具体的命令
        return "nuclei -t " + String.join(",", templatePaths)+ " " + Utils.templateArg + " -u ";
    }

    /**
     * 复制到剪贴板
     */
    private void copyToClipboard(String text) {
        Toolkit.getDefaultToolkit()
                .getSystemClipboard()
                .setContents(new StringSelection(text), null);
    }

    /**
     * 保存工作流
     */
    private void saveWorkflow(String name, List<String> templatePaths) {
        try {
            // 这里实现工作流保存逻辑
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
     * 弹出监听器类
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

    /**
     * 模板编辑器显示方法
     */
    private void showTemplateEditor(String path, String content) {
        // 创建编辑器对话框
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
                "编辑模板", true);

        // 创建 RSyntaxTextArea
        RSyntaxTextArea textArea = new RSyntaxTextArea(20, 60);
        textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_YAML);
        textArea.setCodeFoldingEnabled(true);
        textArea.setAntiAliasingEnabled(true);
        textArea.setText(content);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));

        // 设置主题
        try {
            Theme theme = Theme.load(getClass().getResourceAsStream(
                    "/org/fife/ui/rsyntaxtextarea/themes/monokai.xml"));
            theme.apply(textArea);
        } catch (Exception e) {
            // 如果主题加载失败，继续使用默认主题
            e.printStackTrace();
        }

        // 创建滚动面板
        RTextScrollPane scrollPane = new RTextScrollPane(textArea);
        scrollPane.setFoldIndicatorEnabled(true);

        // 创建按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton("保存");
        JButton cancelButton = new JButton("取消");

        // 添加快捷键支持
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

        // 设置按钮事件
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

        // 添加按钮到面板
        buttonPanel.add(saveButton);
        buttonPanel.add(Box.createHorizontalStrut(10));
        buttonPanel.add(cancelButton);

        // 设置对话框布局
        dialog.setLayout(new BorderLayout());
        dialog.add(scrollPane, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        // 设置对话框属性
        dialog.setSize(1000, 800);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    /**
     * 资源清理方法
     */
    @Override
    public void removeNotify() {
        super.removeNotify();
        // 关闭执行器
        shutdownExecutor();
    }

    /**
     * 关闭执行器
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


}