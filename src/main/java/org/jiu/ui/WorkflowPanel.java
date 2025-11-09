package org.jiu.ui;

import cn.hutool.core.io.FileUtil;
import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.icons.FlatSearchIcon;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.DumperOptions;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.jiu.utils.Utils;
import org.yaml.snakeyaml.Yaml;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WorkflowPanel extends JPanel {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final JLabel statusLabel;
    private final JTable workflowTable;
    private final DefaultTableModel tableModel;
    private final TableRowSorter<DefaultTableModel> sorter;
    private final JTextField searchField = new JTextField(20);
    private final Map<String, List<LinkedHashMap<String, String>>> workflows = new HashMap<>();

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

    private void initUI() {
        add(createToolBar(), BorderLayout.NORTH);
        JScrollPane scrollPane = new JScrollPane(workflowTable);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        add(scrollPane, BorderLayout.CENTER);
    }

    private JToolBar createToolBar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // 创建主面板
        JPanel mainPanel = new JPanel(new BorderLayout());
        
        // 左侧控件：状态标签
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        statusLabel.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        leftPanel.add(statusLabel);
        
        // 分隔符
        leftPanel.add(Box.createHorizontalStrut(10));
        
        // 刷新按钮
        JButton refreshBtn = new JButton("刷新");
        refreshBtn.setFont(new Font("微软雅黑", Font.PLAIN, 11));
        refreshBtn.setFocusPainted(false);
        refreshBtn.setPreferredSize(new Dimension(60, 25));
        refreshBtn.setBackground(new Color(0, 123, 255));
        refreshBtn.setForeground(Color.WHITE);
        refreshBtn.addActionListener(e -> loadWorkflows());
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
        searchField.putClientProperty("JTextField.placeholderText", "搜索工作流...");
        searchField.addActionListener(e -> performSearch());
        rightPanel.add(searchField, gbc);
        
        // 将左右面板添加到主面板
        mainPanel.add(leftPanel, BorderLayout.WEST);
        mainPanel.add(rightPanel, BorderLayout.EAST);
        
        toolBar.add(mainPanel);
        return toolBar;
    }

    private void configureSearchField() {
        searchField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Search workflows, Enter");
        searchField.putClientProperty(FlatClientProperties.TEXT_FIELD_TRAILING_ICON, new FlatSearchIcon());
        searchField.addActionListener(e -> performSearch());
    }

    private void performSearch() {
        String searchKeyWord = searchField.getText().trim();
        sorter.setRowFilter(RowFilter.regexFilter("(?i)" + searchKeyWord));
    }

    private JTable createTable() {
        JTable table = new JTable(tableModel);
        
        // 设置表格属性
        table.setRowHeight(25);
        table.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        
        // 设置表格渲染器
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);

        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        // 为严重程度相关列设置特殊渲染器
        table.getColumnModel().getColumn(4).setCellRenderer(createSeverityRenderer("info")); // info列
        table.getColumnModel().getColumn(5).setCellRenderer(createSeverityRenderer("low"));  // low列
        table.getColumnModel().getColumn(6).setCellRenderer(createSeverityRenderer("medium")); // medium列
        table.getColumnModel().getColumn(7).setCellRenderer(createSeverityRenderer("high")); // high列
        table.getColumnModel().getColumn(8).setCellRenderer(createSeverityRenderer("critical")); // critical列

        table.addMouseListener(new PopupListener(table));
        return table;
    }
    
    /**
     * 创建严重程度渲染器
     */
    private DefaultTableCellRenderer createSeverityRenderer(String severityType) {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                
                if (!isSelected) {
                    String severity = severityType;
                    switch (severity) {
                        case "critical":
                            if (Integer.parseInt(value.toString()) > 0) {
                                label.setBackground(new Color(220, 53, 69));
                                label.setForeground(Color.WHITE);
                            } else {
                                label.setBackground(null);
                                label.setForeground(null);
                            }
                            break;
                        case "high":
                            if (Integer.parseInt(value.toString()) > 0) {
                                label.setBackground(new Color(255, 193, 7));
                                label.setForeground(Color.BLACK);
                            } else {
                                label.setBackground(null);
                                label.setForeground(null);
                            }
                            break;
                        case "medium":
                            if (Integer.parseInt(value.toString()) > 0) {
                                label.setBackground(new Color(255, 133, 27));
                                label.setForeground(Color.WHITE);
                            } else {
                                label.setBackground(null);
                                label.setForeground(null);
                            }
                            break;
                        case "low":
                            if (Integer.parseInt(value.toString()) > 0) {
                                label.setBackground(new Color(40, 167, 69));
                                label.setForeground(Color.WHITE);
                            } else {
                                label.setBackground(null);
                                label.setForeground(null);
                            }
                            break;
                        case "info":
                            if (Integer.parseInt(value.toString()) > 0) {
                                label.setBackground(new Color(108, 117, 125));
                                label.setForeground(Color.WHITE);
                            } else {
                                label.setBackground(null);
                                label.setForeground(null);
                            }
                            break;
                        default:
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

    // 在WorkflowPanel类的loadWorkflows方法中
    private void loadWorkflows() {
        executor.submit(() -> {
            try {
                updateStatus("正在加载工作流...");
                workflows.clear();
                tableModel.setRowCount(0);

                // 确保目录存在
                File workflowDir = new File(Utils.workflowPath);
                if (!workflowDir.exists()) {
                    workflowDir.mkdirs();  // 使用mkdirs而不是Files.createDirectories
                }

                // 使用File API遍历文件而不是Files.list
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
            }
        });
    }

    private void processWorkflowFile(Path path) {
        try {
            Yaml yaml = new Yaml();
            Map<String, List<String>> workflowData = yaml.load(new FileReader(path.toFile()));
            String workflowName = path.getFileName().toString().replace(".yaml", "");

            List<String> templatePaths = workflowData.get("templates");
            List<LinkedHashMap<String, String>> templateInfoList = new ArrayList<>();

            // 统计不同级别的模板数量
            int[] counts = new int[5]; // info, low, medium, high, critical

            for (String templatePath : templatePaths) {
                LinkedHashMap<String, String> templateInfo = getTemplateInfo(templatePath);
                if (templateInfo != null) {
                    templateInfoList.add(templateInfo);
                    switch (templateInfo.get("severity").toLowerCase()) {
                        case "info": counts[0]++; break;
                        case "low": counts[1]++; break;
                        case "medium": counts[2]++; break;
                        case "high": counts[3]++; break;
                        case "critical": counts[4]++; break;
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

    private JPopupMenu createPopupMenu() {
        JPopupMenu popup = new JPopupMenu();

        // 查看详情
        JMenuItem viewItem = new JMenuItem("查看工作流详情");
        viewItem.addActionListener(e -> showTemplateDetails());
        popup.add(viewItem);

        // 执行工作流
        JMenuItem executeItem = new JMenuItem("复制运行命令");
        executeItem.addActionListener(e -> executeWorkflow());
        popup.add(executeItem);

        // 删除工作流
        JMenuItem deleteItem = new JMenuItem("删除工作流");
        deleteItem.addActionListener(e -> deleteWorkflow());
        popup.add(deleteItem);

        return popup;
    }

    private void showTemplateDetails() {
        int row = workflowTable.getSelectedRow();
        if (row != -1) {
            // 注意：需要将视图索引转换为模型索引
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

            // 创建新窗口显示模板详情
            JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
                    workflowName + " - 模板详情", true);
            dialog.setLayout(new BorderLayout());

            // 使用与 TemplatesPanel 相同的列名
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

            // 设置表格渲染器，使其居中显示并支持工具提示
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

            // 应用渲染器到所有列
            for (int i = 0; i < detailTable.getColumnCount(); i++) {
                detailTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
            }

            // 设置列宽
            detailTable.getColumn("#").setPreferredWidth(20);
            detailTable.getColumn("templates_id").setPreferredWidth(60);
            detailTable.getColumn("templates_name").setPreferredWidth(100);
            detailTable.getColumn("templates_severity").setPreferredWidth(40);
            detailTable.getColumn("templates_author").setPreferredWidth(30);

            // 添加数据行
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

    private void executeWorkflow() {
        int row = workflowTable.getSelectedRow();
        if (row != -1) {
            String workflowName = (String) workflowTable.getValueAt(row, 1);
            String command = "nuclei -config " + Paths.get(Utils.workflowPath, workflowName + ".yaml") +" "+Utils.templateArg+ " -u ";

            Utils.copyToClipboard(command);
        }
    }
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
                    // 获取工作流文件
                    File file = new File(Utils.workflowPath, workflowName + ".yaml");
                    if (!file.exists()) {
                        JOptionPane.showMessageDialog(this,
                                "工作流文件不存在",
                                "错误",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    // 确保关闭所有可能的文件句柄
                    System.gc();
                    Thread.sleep(100); // 给系统一点时间进行GC

                    // 尝试多次删除文件
                    boolean deleted = false;
                    int maxRetries = 3;
                    int retryCount = 0;
                    Exception lastException = null;

                    while (!deleted && retryCount < maxRetries) {
                        try {
                            // 先尝试使用Files.delete
                            Files.delete(file.toPath());
                            deleted = true;
                        } catch (Exception e) {
                            lastException = e;
                            // 如果Files.delete失败，尝试使用File.delete
                            if (file.delete()) {
                                deleted = true;
                            } else {
                                // 如果还是失败，标记文件为可删除并再次尝试
                                file.setWritable(true);
                                if (file.delete()) {
                                    deleted = true;
                                }
                            }
                        }
                        if (!deleted) {
                            retryCount++;
                            Thread.sleep(100); // 在重试之间稍作等待
                        }
                    }

                    if (deleted) {
                        // 删除成功后重新加载列表
                        workflows.remove(workflowName);
                        loadWorkflows();
                        JOptionPane.showMessageDialog(this,
                                "删除成功",
                                "提示",
                                JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        // 如果所有尝试都失败了，则抛出最后一个异常
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

    private void updateStatus(String message) {
        SwingUtilities.invokeLater(() -> statusLabel.setText(message));
    }

    @Override
    public void removeNotify() {
        super.removeNotify();
        executor.shutdown();
    }

    public static LinkedHashMap<String, String> getTemplateInfo(String templatePath) {
        try {
            File templateFile = new File(templatePath);
            if (!templateFile.exists()) {
                return null;
            }

            Yaml yaml = new Yaml();
            Map<String, Object> templateData = yaml.load(new FileReader(templateFile));

            LinkedHashMap<String, String> templateInfo = new LinkedHashMap<>();
            templateInfo.put("path", templatePath);
            templateInfo.put("id", (String) templateData.get("id")); // 修改：正确设置id字段

            // 处理嵌套的info字段
            Map<String, Object> info = (Map<String, Object>) templateData.get("info");
            if (info != null) {
                // 设置name字段
                templateInfo.put("name", (String) info.get("name")); // 修改：从info中获取name

                // 设置severity字段
                templateInfo.put("severity", ((String) info.get("severity")).toLowerCase());

                // 设置description字段
                Object description = info.get("description");
                templateInfo.put("description", description != null ? description.toString() : ""); // 添加：设置description

                // 设置reference字段
                Object reference = info.get("reference");
                if (reference instanceof List) {
                    templateInfo.put("reference", String.join(",", (List<String>) reference));
                } else if (reference instanceof String) {
                    templateInfo.put("reference", (String) reference);
                } else {
                    templateInfo.put("reference", "");
                }

                // 处理tags字段
                Object tags = info.get("tags");
                if (tags instanceof List) {
                    templateInfo.put("tags", String.join(",", (List<String>) tags));
                } else if (tags instanceof String) {
                    templateInfo.put("tags", (String) tags);
                } else {
                    templateInfo.put("tags", "");
                }

                // 处理author字段
                Object author = info.get("author");
                if (author instanceof List) {
                    templateInfo.put("author", String.join(",", (List<String>) author));
                } else if (author instanceof String) {
                    templateInfo.put("author", (String) author);
                } else {
                    templateInfo.put("author", "");
                }
            } else {
                // 如果info为空，设置默认值
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

}