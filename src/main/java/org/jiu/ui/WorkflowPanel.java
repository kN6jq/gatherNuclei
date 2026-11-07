package org.jiu.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.icons.FlatSearchIcon;
import org.jiu.utils.Utils;
import org.yaml.snakeyaml.Yaml;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 工作流管理面板：扫描工作流目录下的 YAML 文件，按严重程度统计模板数量，
 * 并支持查看模板详情、复制 nuclei 运行命令与删除工作流。
 */
public class WorkflowPanel extends JPanel {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final JLabel statusLabel;
    private final JTable workflowTable;
    private final DefaultTableModel tableModel;
    private final TableRowSorter<DefaultTableModel> sorter;
    private final JTextField searchField = new JTextField(20);
    /**
     * 后台线程写入、事件分发线程读取
     */
    private final Map<String, List<LinkedHashMap<String, String>>> workflows = new ConcurrentHashMap<>();
    /**
     * 加载中标记，避免重复提交加载任务
     */
    private final AtomicBoolean loading = new AtomicBoolean(false);

    private final String[] columnNames = {
            "#",
            "workflow_name",
            "template_count",
            "info_count",
            "low_count",
            "medium_count",
            "high_count",
            "critical_count",
            "last_modified"
    };

    public WorkflowPanel() {
        setLayout(new BorderLayout());

        statusLabel = new JLabel("工作流管理面板");
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        workflowTable = createTable();
        sorter = new TableRowSorter<>(tableModel);
        workflowTable.setRowSorter(sorter);

        initUI();
        loadWorkflows();
    }

    /**
     * 解析计数单元格的值，null 或非数字时按 0 处理。
     */
    private static int parseCount(Object value) {
        if (value == null) {
            return 0;
        }
        try {
            return Integer.parseInt(value.toString().trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * 解析 nuclei 模板 YAML，提取 id 及 info 中的元信息，多值字段统一转为逗号分隔字符串。
     *
     * @return 模板信息映射；文件不存在、内容为空或解析异常时返回 null
     */
    public static LinkedHashMap<String, String> getTemplateInfo(String templatePath) {
        try {
            File templateFile = new File(templatePath);
            if (!templateFile.exists()) {
                return null;
            }

            Yaml yaml = new Yaml();
            Map<String, Object> templateData;
            try (FileReader reader = new FileReader(templateFile)) {
                templateData = yaml.load(reader);
            }
            if (templateData == null) {
                return null;
            }

            LinkedHashMap<String, String> templateInfo = new LinkedHashMap<>();
            templateInfo.put("path", templatePath);
            templateInfo.put("id", (String) templateData.get("id"));

            // 模板元信息位于嵌套的 info 节点下
            Map<String, Object> info = (Map<String, Object>) templateData.get("info");
            if (info != null) {
                templateInfo.put("name", (String) info.get("name"));

                // 统一小写，便于后续按级别统计
                Object severity = info.get("severity");
                templateInfo.put("severity", severity != null ? severity.toString().toLowerCase() : "unknown");

                Object description = info.get("description");
                templateInfo.put("description", description != null ? description.toString() : "");

                // reference/tags/author 在 YAML 中可能是字符串或列表，统一转成逗号分隔字符串
                Object reference = info.get("reference");
                if (reference instanceof List) {
                    templateInfo.put("reference", String.join(",", (List<String>) reference));
                } else if (reference instanceof String) {
                    templateInfo.put("reference", (String) reference);
                } else {
                    templateInfo.put("reference", "");
                }

                Object tags = info.get("tags");
                if (tags instanceof List) {
                    templateInfo.put("tags", String.join(",", (List<String>) tags));
                } else if (tags instanceof String) {
                    templateInfo.put("tags", (String) tags);
                } else {
                    templateInfo.put("tags", "");
                }

                Object author = info.get("author");
                if (author instanceof List) {
                    templateInfo.put("author", String.join(",", (List<String>) author));
                } else if (author instanceof String) {
                    templateInfo.put("author", (String) author);
                } else {
                    templateInfo.put("author", "");
                }
            } else {
                // info 缺失时填充默认值，避免下游取到 null
                templateInfo.put("name", "未知");
                templateInfo.put("severity", "unknown");
                templateInfo.put("description", "");
                templateInfo.put("reference", "");
                templateInfo.put("tags", "");
                templateInfo.put("author", "");
            }

            return templateInfo;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 组装工具栏与可滚动的工作流表格。
     */
    private void initUI() {
        add(createToolBar(), BorderLayout.NORTH);
        JScrollPane scrollPane = new JScrollPane(workflowTable);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        add(scrollPane, BorderLayout.CENTER);
    }

    /**
     * 构建工具栏：左侧状态标签与刷新按钮，右侧搜索框（回车触发过滤）。
     */
    private JToolBar createToolBar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JPanel mainPanel = new JPanel(new BorderLayout());

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        statusLabel.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        leftPanel.add(statusLabel);

        leftPanel.add(Box.createHorizontalStrut(10));

        JButton refreshBtn = new JButton("刷新");
        refreshBtn.setFont(new Font("微软雅黑", Font.PLAIN, 11));
        refreshBtn.setFocusPainted(false);
        refreshBtn.setPreferredSize(new Dimension(60, 25));
        refreshBtn.setBackground(new Color(0, 123, 255));
        refreshBtn.setForeground(Color.WHITE);
        refreshBtn.addActionListener(e -> loadWorkflows());
        leftPanel.add(refreshBtn);

        leftPanel.add(Box.createHorizontalStrut(10));

        JPanel rightPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 0, 0);
        searchField.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        searchField.putClientProperty("JTextField.placeholderText", "搜索工作流...");
        searchField.addActionListener(e -> performSearch());
        rightPanel.add(searchField, gbc);

        mainPanel.add(leftPanel, BorderLayout.WEST);
        mainPanel.add(rightPanel, BorderLayout.EAST);

        toolBar.add(mainPanel);
        return toolBar;
    }

    /**
     * 为搜索框设置 FlatLaf 占位提示、尾部搜索图标并绑定回车搜索（当前无调用方）。
     */
    private void configureSearchField() {
        searchField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Search workflows, Enter");
        searchField.putClientProperty(FlatClientProperties.TEXT_FIELD_TRAILING_ICON, new FlatSearchIcon());
        searchField.addActionListener(e -> performSearch());
    }

    /**
     * 按搜索框内容过滤表格行，关键词作为正则表达式处理且不区分大小写。
     */
    private void performSearch() {
        String searchKeyWord = searchField.getText().trim();
        sorter.setRowFilter(RowFilter.regexFilter("(?i)" + searchKeyWord));
    }

    /**
     * 创建工作流表格：全列居中，严重程度计数列按级别着色，并绑定双击与右键监听。
     */
    private JTable createTable() {
        JTable table = new JTable(tableModel);

        table.setRowHeight(25);
        table.setFont(new Font("微软雅黑", Font.PLAIN, 12));

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);

        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        // 列顺序：0=# 1=workflow_name 2=template_count 3=info 4=low 5=medium 6=high 7=critical 8=last_modified
        table.getColumnModel().getColumn(3).setCellRenderer(createSeverityRenderer("info"));
        table.getColumnModel().getColumn(4).setCellRenderer(createSeverityRenderer("low"));
        table.getColumnModel().getColumn(5).setCellRenderer(createSeverityRenderer("medium"));
        table.getColumnModel().getColumn(6).setCellRenderer(createSeverityRenderer("high"));
        table.getColumnModel().getColumn(7).setCellRenderer(createSeverityRenderer("critical"));

        table.addMouseListener(new PopupListener(table));
        return table;
    }

    /**
     * 创建严重程度计数列渲染器：未选中且计数大于 0 时按级别着色，否则恢复默认配色。
     *
     * @param severityType 取值 info、low、medium、high、critical，其余值不着色
     */
    private DefaultTableCellRenderer createSeverityRenderer(String severityType) {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                if (!isSelected) {
                    int count = parseCount(value);
                    if (count > 0) {
                        switch (severityType) {
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
                    } else {
                        label.setBackground(null);
                        label.setForeground(null);
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
     * 在后台单线程中重新扫描工作流目录并重建表格，加载期间忽略重复调用。
     */
    private void loadWorkflows() {
        // 防止重复提交导致任务堆积
        if (!loading.compareAndSet(false, true)) {
            return;
        }
        updateStatus("正在加载工作流...");
        executor.submit(() -> {
            try {
                workflows.clear();
                // 表格模型必须在事件分发线程上修改
                SwingUtilities.invokeLater(() -> tableModel.setRowCount(0));

                File workflowDir = new File(Utils.workflowPath);
                if (!workflowDir.exists()) {
                    workflowDir.mkdirs();
                }

                File[] files = workflowDir.listFiles((dir, name) -> name.endsWith(".yaml"));
                if (files != null) {
                    for (File file : files) {
                        processWorkflowFile(file.toPath());
                    }
                }

                updateStatus("工作流加载完成: " + workflows.size() + "个");
            } catch (Exception e) {
                e.printStackTrace();
                updateStatus("加载失败: " + e.getMessage());
            } finally {
                loading.set(false);
            }
        });
    }

    /**
     * 刷新工作流列表，供外部（如切换到该标签页时）调用。
     */
    public void refreshWorkflows() {
        loadWorkflows();
    }

    /**
     * 解析单个工作流 YAML，统计其模板各严重程度的数量，写入缓存并在事件分发线程追加表格行；
     * 解析失败仅打印堆栈，不影响其他工作流。
     */
    private void processWorkflowFile(Path path) {
        try {
            Yaml yaml = new Yaml();
            Map<String, List<String>> workflowData;
            try (FileReader reader = new FileReader(path.toFile())) {
                workflowData = yaml.load(reader);
            }
            if (workflowData == null) {
                return;
            }
            String workflowName = path.getFileName().toString().replace(".yaml", "");

            List<String> templates = workflowData.get("templates");
            final List<String> templatePaths = templates != null ? templates : new ArrayList<>();
            List<LinkedHashMap<String, String>> templateInfoList = new ArrayList<>();

            int[] counts = new int[5]; // 下标依次为 info, low, medium, high, critical

            for (String templatePath : templatePaths) {
                LinkedHashMap<String, String> templateInfo = getTemplateInfo(templatePath);
                if (templateInfo != null) {
                    templateInfoList.add(templateInfo);
                    switch (templateInfo.get("severity").toLowerCase()) {
                        case "info":
                            counts[0]++;
                            break;
                        case "low":
                            counts[1]++;
                            break;
                        case "medium":
                            counts[2]++;
                            break;
                        case "high":
                            counts[3]++;
                            break;
                        case "critical":
                            counts[4]++;
                            break;
                    }
                }
            }

            workflows.put(workflowName, templateInfoList);

            SwingUtilities.invokeLater(() -> {
                tableModel.addRow(new Object[]{
                        tableModel.getRowCount() + 1,
                        workflowName,
                        templatePaths.size(),
                        counts[0],
                        counts[1],
                        counts[2],
                        counts[3],
                        counts[4],
                        new Date(path.toFile().lastModified()).toString()
                });
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 构建表格右键菜单：查看详情、复制运行命令、删除工作流。
     */
    private JPopupMenu createPopupMenu() {
        JPopupMenu popup = new JPopupMenu();

        JMenuItem viewItem = new JMenuItem("查看工作流详情");
        viewItem.addActionListener(e -> showTemplateDetails());
        popup.add(viewItem);

        JMenuItem executeItem = new JMenuItem("复制运行命令");
        executeItem.addActionListener(e -> executeWorkflow());
        popup.add(executeItem);

        JMenuItem deleteItem = new JMenuItem("删除工作流");
        deleteItem.addActionListener(e -> deleteWorkflow());
        popup.add(deleteItem);

        return popup;
    }

    /**
     * 以模态对话框展示选中工作流的模板明细；未选中行时不处理，无模板或读取失败时弹窗提示。
     */
    private void showTemplateDetails() {
        int row = workflowTable.getSelectedRow();
        if (row != -1) {
            // 表格存在排序与过滤，视图索引必须先转换为模型索引
            row = workflowTable.convertRowIndexToModel(row);
            String workflowName = (String) tableModel.getValueAt(row, 1);
            List<LinkedHashMap<String, String>> templates = workflows.get(workflowName);

            if (templates == null || templates.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "该工作流没有模板或模板信息读取失败",
                        "提示",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
                    workflowName + " - 模板详情", true);
            dialog.setLayout(new BorderLayout());

            // 列名与 TemplatesPanel 的模板表格保持一致
            String[] detailColumns = {
                    "#",
                    "templates_id",
                    "templates_name",
                    "templates_severity",
                    "templates_tags",
                    "templates_author",
                    "templates_description",
                    "templates_reference"
            };

            DefaultTableModel detailModel = new DefaultTableModel(detailColumns, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };

            JTable detailTable = new JTable(detailModel);

            DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(
                        JTable table, Object value, boolean isSelected, boolean hasFocus,
                        int row, int column) {
                    JLabel label = (JLabel) super.getTableCellRendererComponent(
                            table, value, isSelected, hasFocus, row, column);
                    label.setHorizontalAlignment(JLabel.CENTER);
                    label.setToolTipText((String) value);
                    return label;
                }
            };

            for (int i = 0; i < detailTable.getColumnCount(); i++) {
                detailTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
            }

            detailTable.getColumn("#").setPreferredWidth(20);
            detailTable.getColumn("templates_id").setPreferredWidth(60);
            detailTable.getColumn("templates_name").setPreferredWidth(100);
            detailTable.getColumn("templates_severity").setPreferredWidth(40);
            detailTable.getColumn("templates_author").setPreferredWidth(30);

            int count = 1;
            for (LinkedHashMap<String, String> template : templates) {
                detailModel.addRow(new Object[]{
                        String.valueOf(count++),
                        template.get("id"),
                        template.get("name"),
                        template.get("severity"),
                        template.get("tags"),
                        template.get("author"),
                        template.get("description"),
                        template.get("reference")
                });
            }

            dialog.add(new JScrollPane(detailTable), BorderLayout.CENTER);
            dialog.setSize(800, 600);
            dialog.setLocationRelativeTo(this);
            dialog.setVisible(true);
        }
    }

    /**
     * 把选中工作流的 nuclei 运行命令复制到剪贴板，命令以 -u 结尾，需由使用者补上目标。
     */
    private void executeWorkflow() {
        int row = workflowTable.getSelectedRow();
        if (row != -1) {
            row = workflowTable.convertRowIndexToModel(row);
            String workflowName = (String) tableModel.getValueAt(row, 1);

            // 工作流文件本身就是 nuclei 配置文件（templates 列表），
            // 直接用 -config 指向该文件即可，无需展开成逗号分隔的模板列表
            String command = "nuclei -config " + Paths.get(Utils.workflowPath, workflowName + ".yaml")
                    + " " + Utils.templateArg + " -u ";
            Utils.copyToClipboard(command);
        }
    }

    /**
     * 确认后删除选中工作流文件并重新加载列表；文件不存在或删除失败时弹窗提示。
     */
    private void deleteWorkflow() {
        int row = workflowTable.getSelectedRow();
        if (row != -1) {
            row = workflowTable.convertRowIndexToModel(row);
            String workflowName = (String) tableModel.getValueAt(row, 1);
            int confirm = JOptionPane.showConfirmDialog(this,
                    "确定要删除工作流 " + workflowName + " 吗？",
                    "确认删除",
                    JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    File file = new File(Utils.workflowPath, workflowName + ".yaml");
                    if (!file.exists()) {
                        JOptionPane.showMessageDialog(this,
                                "工作流文件不存在",
                                "错误",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    // 文件可能仍被未回收的句柄占用，先触发 GC 并留出释放时间
                    System.gc();
                    Thread.sleep(100);

                    boolean deleted = false;
                    int maxRetries = 3;
                    int retryCount = 0;
                    Exception lastException = null;

                    // 逐级退化重试：Files.delete -> File.delete -> 解除只读后再删，应对文件占用与权限问题
                    while (!deleted && retryCount < maxRetries) {
                        try {
                            Files.delete(file.toPath());
                            deleted = true;
                        } catch (Exception e) {
                            lastException = e;
                            if (file.delete()) {
                                deleted = true;
                            } else {
                                file.setWritable(true);
                                if (file.delete()) {
                                    deleted = true;
                                }
                            }
                        }
                        if (!deleted) {
                            retryCount++;
                            Thread.sleep(100); // 等待占用释放后再重试
                        }
                    }

                    if (deleted) {
                        workflows.remove(workflowName);
                        loadWorkflows();
                        JOptionPane.showMessageDialog(this,
                                "删除成功",
                                "提示",
                                JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        throw new IOException("无法删除文件，可能被其他程序占用", lastException);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    String errorMessage = "删除失败: " + e.getMessage() +
                            "\n请检查文件是否被其他程序占用，或尝试重启应用后再试";
                    JOptionPane.showMessageDialog(this,
                            errorMessage,
                            "错误",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    /**
     * 更新状态文本，内部切回事件分发线程，可安全地从后台加载线程调用。
     */
    private void updateStatus(String message) {
        SwingUtilities.invokeLater(() -> statusLabel.setText(message));
    }

    /**
     * 面板从容器移除时关闭后台加载线程池，避免线程泄漏。
     */
    @Override
    public void removeNotify() {
        super.removeNotify();
        executor.shutdown();
    }

    /**
     * 表格鼠标监听：双击查看模板详情；各平台右键触发时机不同，故 pressed 与 released 都检查 popupTrigger。
     */
    private class PopupListener extends MouseAdapter {
        private final JTable table;

        PopupListener(JTable table) {
            this.table = table;
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2) {
                showTemplateDetails();
            } else if (e.isPopupTrigger()) {
                showPopup(e);
            }
        }

        @Override
        public void mousePressed(MouseEvent e) {
            if (e.isPopupTrigger()) {
                showPopup(e);
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (e.isPopupTrigger()) {
                showPopup(e);
            }
        }

        private void showPopup(MouseEvent e) {
            if (table.getSelectedRow() != -1) {
                createPopupMenu().show(e.getComponent(), e.getX(), e.getY());
            }
        }
    }

}