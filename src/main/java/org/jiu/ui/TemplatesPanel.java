package org.jiu.ui;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.file.FileReader;
import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.icons.FlatSearchIcon;
import org.jiu.core.TemplatesCore;
import org.jiu.utils.Utils;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.jiu.core.TemplatesCore.generateNucleiConfigFile;
import static org.jiu.core.TemplatesCore.templates;

public class TemplatesPanel extends JPanel {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final JLabel statusLabel;
    private final JTable templatesTable;
    private final DefaultTableModel tableModel;
    private final TableRowSorter<DefaultTableModel> sorter;
    private final LinkedList<String> filterList = new LinkedList<>();
    private final JTextField searchField = new JTextField(20);

    // 按钮组件
    private final JToggleButton infoBtn = new JToggleButton("信息");
    private final JToggleButton lowBtn = new JToggleButton("低");
    private final JToggleButton mediumBtn = new JToggleButton("中");
    private final JToggleButton highBtn = new JToggleButton("高");
    private final JToggleButton criticalBtn = new JToggleButton("危");
    private final JButton refreshBtn = new JButton("刷新");

    // 表格列名
    private final String[] columnNames = {
            "#",
            "templates_id",
            "templates_name",
            "templates_severity",
            "templates_tags",
            "templates_author",
            "templates_description",
            "templates_reference"
    };

    public TemplatesPanel() {
        setLayout(new BorderLayout());

        // 初始化基本组件
        statusLabel = new JLabel("正在初始化...");
        tableModel = createTableModel();
        templatesTable = createTable();
        sorter = new TableRowSorter<>(tableModel);
        templatesTable.setRowSorter(sorter);

        // 初始化UI和数据
        initUI();
        loadDataInBackground();
    }

    private void initUI() {
        // 工具栏
        add(createToolBar(), BorderLayout.NORTH);

        // 表格面板
        JScrollPane scrollPane = new JScrollPane(templatesTable);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        add(scrollPane, BorderLayout.CENTER);

        // 初始化过滤器和按钮状态
        initFiltersAndButtons();
    }

    private DefaultTableModel createTableModel() {
        return new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private JTable createTable() {
        JTable table = new JTable(tableModel);

        // 设置表格属性
        configureTableProperties(table);

        // 添加右键菜单监听
        table.addMouseListener(new PopupListener(table));

        return table;
    }

    private void configureTableProperties(JTable table) {
        // 创建居中渲染器
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
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        // 设置列宽
        table.getColumn("#").setPreferredWidth(20);
        table.getColumn("templates_id").setPreferredWidth(60);
        table.getColumn("templates_name").setPreferredWidth(100);
        table.getColumn("templates_severity").setPreferredWidth(40);
        table.getColumn("templates_author").setPreferredWidth(30);
    }

    private void initFiltersAndButtons() {
        // 初始化过滤器列表
        filterList.clear();
        filterList.add("info");
        filterList.add("low");
        filterList.add("medium");
        filterList.add("high");
        filterList.add("critical");

        // 初始化按钮状态
        infoBtn.setSelected(true);
        lowBtn.setSelected(true);
        mediumBtn.setSelected(true);
        highBtn.setSelected(true);
        criticalBtn.setSelected(true);

        // 设置按钮提示
        infoBtn.setToolTipText("信息级别模板");
        lowBtn.setToolTipText("低危级别模板");
        mediumBtn.setToolTipText("中危级别模板");
        highBtn.setToolTipText("高危级别模板");
        criticalBtn.setToolTipText("严重级别模板");
        refreshBtn.setToolTipText("刷新模板列表");
    }

    private JToolBar createToolBar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        // 添加状态标签
        toolBar.add(statusLabel);
        toolBar.addSeparator();

        // 添加过滤按钮
        toolBar.add(infoBtn);
        toolBar.add(lowBtn);
        toolBar.add(mediumBtn);
        toolBar.add(highBtn);
        toolBar.add(criticalBtn);
        toolBar.addSeparator();

        // 添加刷新按钮
        toolBar.add(refreshBtn);
        toolBar.addSeparator();

        // 配置搜索框
        configureSearchField();
        toolBar.add(searchField);

        // 添加按钮事件监听
        addButtonListeners();

        return toolBar;
    }

    private void configureSearchField() {
        searchField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Search, Enter");
        searchField.putClientProperty(FlatClientProperties.TEXT_FIELD_TRAILING_ICON, new FlatSearchIcon());
        searchField.registerKeyboardAction(
                e -> performSearch(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
                JComponent.WHEN_FOCUSED
        );
    }

    private void performSearch() {
        String searchKeyWord = searchField.getText().trim();
        sorter.setRowFilter(RowFilter.regexFilter("(?i)" + searchKeyWord));
    }

    private void addButtonListeners() {
        ActionListener filterListener = e -> filterData();
        infoBtn.addActionListener(filterListener);
        lowBtn.addActionListener(filterListener);
        mediumBtn.addActionListener(filterListener);
        highBtn.addActionListener(filterListener);
        criticalBtn.addActionListener(filterListener);
        refreshBtn.addActionListener(e -> loadDataInBackground());
    }

    private void loadDataInBackground() {
        updateStatus("正在加载模板...");
        executor.submit(() -> {
            try {
                String customPath = Utils.templatePath;
                if (!isValidPath(customPath)) {
                    SwingUtilities.invokeLater(() ->
                            updateStatus("模板路径无效或不存在"));
                    return;
                }

                // 清理现有数据
                SwingUtilities.invokeLater(() -> {
                    tableModel.setRowCount(0);
                    templates.clear();
                });

                // 加载新数据
                TemplatesCore.getAllTemplatesFromPath(customPath);

                // 更新UI
                SwingUtilities.invokeLater(() -> {
                    updateTableData();
                    updateStatus("模板已成功加载: " + (templates.size() - 1) + "个");
                });

            } catch (Exception e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() ->
                        updateStatus("加载失败: " + e.getMessage()));
            }
        });
    }

    private boolean isValidPath(String path) {
        return path != null && !path.isEmpty() && new File(path).exists();
    }

    private void updateStatus(String message) {
        statusLabel.setText(message);
    }

    private void updateTableData() {
        int count = 0;
        for (LinkedHashMap<String, String> templateInfo : templates) {
            count++;
            if (!filterList.contains(templateInfo.get("severity"))) {
                continue;
            }
            addTemplateRow(count, templateInfo);
        }
    }

    private void addTemplateRow(int count, LinkedHashMap<String, String> template) {
        tableModel.addRow(new Object[]{
                String.valueOf(count),
                template.get("id"),
                template.get("name"),
                template.get("severity"),
                template.get("tags"),
                template.get("author"),
                template.get("description"),
                template.get("reference")
        });
    }

    void filterData() {
        filterList.clear();

        if (infoBtn.isSelected()) filterList.add("info");
        if (lowBtn.isSelected()) filterList.add("low");
        if (mediumBtn.isSelected()) filterList.add("medium");
        if (highBtn.isSelected()) filterList.add("high");
        if (criticalBtn.isSelected()) filterList.add("critical");

        loadDataInBackground();
    }

    private JPopupMenu createTablePopMenu() {
        JPopupMenu popupMenu = new JPopupMenu();

        // 编辑选项
        addEditMenuItem(popupMenu);

        // 删除选项
        addDeleteMenuItem(popupMenu);

        // 选中数量显示
        popupMenu.add(new JMenuItem("已选中POC数量: " +
                templatesTable.getSelectedRowCount()));

        // 打开文件夹选项
        addOpenFolderMenuItem(popupMenu);

        // 创建项目选项
        addCreateProjectMenuItem(popupMenu);

        // 创建工作流选项
        addCreateWorkflowMenuItem(popupMenu);

        return popupMenu;
    }

    private void addEditMenuItem(JPopupMenu menu) {
        JMenuItem editItem = new JMenuItem("编辑选中模板");
        editItem.addActionListener(e -> {
            String path = getSelectedTemplatePath();
            if (!path.isEmpty()) {
                try {
                    FileReader fileReader = new FileReader(path);
                    YamlPanel.textArea.setText("");
                    YamlPanel.absolutePath = path;
                    YamlPanel.textArea.setText(fileReader.readString());

                    // 切换到模板编辑标签页
                    JTabbedPane tabbedPane = InitUI.getMainTabbedPane();
                    // 假设"模板编辑"是第4个标签页（索引为3）
                    tabbedPane.setSelectedIndex(3);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    showError("编辑失败", ex);
                }
            }
        });
        menu.add(editItem);
    }

    private void addDeleteMenuItem(JPopupMenu menu) {
        JMenuItem deleteItem = new JMenuItem("删除选中模板");
        deleteItem.addActionListener(e -> {
            String path = getSelectedTemplatePath();
            if (!path.isEmpty()) {
                try {
                    FileUtil.del(path);
                    JOptionPane.showMessageDialog(this,
                            "删除成功", "提示",
                            JOptionPane.INFORMATION_MESSAGE);
                    loadDataInBackground();
                } catch (Exception ex) {
                    showError("删除失败", ex);
                }
            }
        });
        menu.add(deleteItem);
    }

    private void addOpenFolderMenuItem(JPopupMenu menu) {
        JMenuItem openFolderItem = new JMenuItem("打开选中POC所在文件夹");
        openFolderItem.addActionListener(e -> {
            int[] selectedRows = templatesTable.getSelectedRows();
            for (int selectedRow : selectedRows) {
                String path = getTemplatePathByRow(selectedRow);
                try {
                    Runtime.getRuntime().exec(
                            "rundll32 SHELL32.DLL,ShellExec_RunDLL " +
                                    "Explorer.exe /select," + path);
                } catch (IOException ex) {
                    showError("打开文件夹失败", ex);
                }
            }
        });
        menu.add(openFolderItem);
    }

    private void addCreateProjectMenuItem(JPopupMenu menu) {
        JMenuItem createProjectItem = new JMenuItem("为选中的POC创建项目");
        createProjectItem.addActionListener(e -> createProject(""));
        menu.add(createProjectItem);
    }

    private void addCreateWorkflowMenuItem(JPopupMenu menu) {
        JMenuItem createWorkFlowItem = new JMenuItem("为选中的POC创建工作流");
        createWorkFlowItem.addActionListener(e -> {
            String engineName = JOptionPane.showInputDialog(this,
                    "请输入工作流名称");
            if (engineName != null && !engineName.isEmpty()) {
                createProject(engineName);
            }
        });
        menu.add(createWorkFlowItem);
    }

    private void createProject(String engineName) {
        LinkedList<String> workflows = new LinkedList<>();
        int[] selectedRows = templatesTable.getSelectedRows();

        for (int row : selectedRows) {
            workflows.add(getTemplatePathByRow(row));
        }

        String nucleiConfigFile = generateNucleiConfigFile(engineName, workflows);
        String command = "nuclei -config " + nucleiConfigFile + " " +
                Utils.templateArg + " -u ";

        copyToClipboard(command);
        JOptionPane.showMessageDialog(this,
                "创建成功,已复制到粘贴板", "提示",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private String getSelectedTemplatePath() {
        int[] selectedRows = templatesTable.getSelectedRows();
        if (selectedRows.length > 0) {
            return getTemplatePathByRow(selectedRows[0]);
        }
        return "";
    }

    private String getTemplatePathByRow(int row) {
        String id = (String) templatesTable.getValueAt(row, 0);
        return templates.get(Integer.parseInt(id) - 1).get("path");
    }

    private void copyToClipboard(String text) {
        Toolkit.getDefaultToolkit()
                .getSystemClipboard()
                .setContents(new StringSelection(text), null);
    }

    private void showError(String message, Exception e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(this,
                message + ": " + e.getMessage(),
                "错误",
                JOptionPane.ERROR_MESSAGE);
    }

    // 清理资源
    @Override
    public void removeNotify() {
        super.removeNotify();
        executor.shutdown();
    }

    private class PopupListener extends MouseAdapter {
        private final JTable table;

        PopupListener(JTable table) {
            this.table = table;
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
            createTablePopMenu().show(e.getComponent(), e.getX(), e.getY());
        }
    }
}