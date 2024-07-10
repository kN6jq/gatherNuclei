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
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedList;

import static org.jiu.core.TemplatesCore.generateNucleiConfigFile;
import static org.jiu.core.TemplatesCore.templates;
import static org.jiu.ui.InitUI.yamlPanel;

public class TemplatesPanel extends JPanel {
    private final JLabel jLabel = new JLabel();
    private final JButton refreshBtn = new JButton("刷新");
    private final JToggleButton infoBtn = new JToggleButton("信息");
    private final JToggleButton lowBtn = new JToggleButton("低");
    private final JToggleButton mediumBtn = new JToggleButton("中");
    private final JToggleButton highBtn = new JToggleButton("高");
    private final JToggleButton criticalBtn = new JToggleButton("危");
    private final JTextField searchField = new JTextField();
    private final LinkedList<String> filterList = new LinkedList<>(); // 过滤器
    String customPath = "";
    String[] columnNames = {
            "#",
            "templates_id",
            "templates_name",
            "templates_severity",
            "templates_tags",
            "templates_author",
            "templates_description",
            "templates_reference"};
    private DefaultTableModel tableModel;
    private JTable templatesTable;
    private JPopupMenu tablePopMenu;
    private TableRowSorter<DefaultTableModel> sorter;

    public TemplatesPanel() {
        this.setLayout(new BorderLayout());
        initToolBar();
        initTable();
    }

    private void initTable() {

        templatesTable = new JTable();
        tableModel = new DefaultTableModel() {
            // 不可编辑
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tableModel.setColumnIdentifiers(columnNames);
        templatesTable.setModel(tableModel);


        // 创建一个自定义的单元格渲染器
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                label.setHorizontalAlignment(JLabel.CENTER);
                label.setHorizontalTextPosition(JLabel.CENTER);
                label.setIconTextGap(0);
                label.setMaximumSize(new Dimension(Integer.MAX_VALUE, label.getPreferredSize().height));
                label.setToolTipText((String) value); // 设置鼠标悬停时显示的提示文本
                return label;
            }
        };

        // 设置自定义渲染器到表格的每一列
        for (int i = 0; i < templatesTable.getColumnCount(); i++) {
            templatesTable.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }

        JScrollPane tableScroll = new JScrollPane(templatesTable);
        tableScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JTextField.CENTER);
        templatesTable.getColumn("#").setCellRenderer(centerRenderer);
        templatesTable.getColumn("templates_severity").setCellRenderer(centerRenderer);

        templatesTable.getColumn("#").setPreferredWidth(20);
        templatesTable.getColumn("templates_id").setPreferredWidth(60);
        templatesTable.getColumn("templates_name").setPreferredWidth(100);
        templatesTable.getColumn("templates_severity").setPreferredWidth(40);
        templatesTable.getColumn("templates_author").setPreferredWidth(30);

        refreshDataForTable();
        templatesTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == 3) {
                    // 模板面板右键功能
                    tablePopMenu = createTablePopMenu();
                    tablePopMenu.show(templatesTable, e.getX(), e.getY());
                }
            }
        });


        this.add(tableScroll, BorderLayout.CENTER);
    }

    // 刷新数据
    public void refreshDataForTable() {
        // 搜索功能
        sorter = new TableRowSorter<>(tableModel);
        templatesTable.setRowSorter(sorter);
        customPath = Utils.templatePath;
        // 如果为空或者目录不存在,则不加载
        if (customPath == null || customPath.equals("") || !new File(customPath).exists()) {
            return;
        }
        new Thread(() -> {
            tableModel.setRowCount(0);
            templates.clear();
            // 初始化列表
            TemplatesCore.getAllTemplatesFromPath(customPath);
            int count = 0;
            for (LinkedHashMap<String, String> templateInfo : templates) {
                count++;
                // Filter custom
                if (!filterList.contains(templateInfo.get("severity"))) {
                    continue;
                }
                String path = templateInfo.get("path");
                String id = templateInfo.get("id");
                String name = templateInfo.get("name");
                String severity = templateInfo.get("severity");
                String author = templateInfo.get("author");
                String description = templateInfo.get("description");
                String reference = templateInfo.get("reference");
                String tags = templateInfo.get("tags");
                tableModel.addRow(new String[]{String.valueOf(count), id, name, severity, tags, author, description, reference});
            }
            jLabel.setText("模板已成功加载: " + (count-1) + "个");
        }).start();
    }

    public void filterData() {
        filterList.clear();

        if (infoBtn.isSelected()) {
            filterList.add("info");
        }
        if (lowBtn.isSelected()) {
            filterList.add("low");
        }
        if (mediumBtn.isSelected()) {
            filterList.add("medium");
        }
        if (highBtn.isSelected()) {
            filterList.add("high");
        }
        if (criticalBtn.isSelected()) {
            filterList.add("critical");
        }
        refreshDataForTable();
    }


    private JPopupMenu createTablePopMenu() {
        JPopupMenu popupMenu = new JPopupMenu();

        // 编辑
        JMenuItem editItem = new JMenuItem("编辑选中模板");
        editItem.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // 当编辑被点击获取到该模板的路径并发送到YamlPanel中
                // 获取选中的模板,提取path参数
                String path = "";
                int[] selectedRows = templatesTable.getSelectedRows();
                for (int selectedRow : selectedRows) {
                    String id = (String) templatesTable.getValueAt(selectedRow, 0);
                    path= templates.get(Integer.parseInt(id) - 1).get("path");
                }
                if (!path.isEmpty()) {
                    try {
                        // 默认UTF-8编码，可以在构造中传入第二个参数做为编码
                        FileReader fileReader = new FileReader(path);
                        YamlPanel.textArea.setText("");
                        YamlPanel.absolutePath = path;
                        YamlPanel.textArea.setText(fileReader.readString());
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });

        // 删除模板
        JMenuItem deleteItem = new JMenuItem("删除选中模板");
        deleteItem.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String path = "";
                int[] selectedRows = templatesTable.getSelectedRows();
                for (int selectedRow : selectedRows) {
                    String id = (String) templatesTable.getValueAt(selectedRow, 0);
                    path= templates.get(Integer.parseInt(id) - 1).get("path");
                }
                if (!path.isEmpty()) {
                    FileUtil.del(path);
                    JOptionPane.showMessageDialog(null, "删除成功", "提示", JOptionPane.INFORMATION_MESSAGE);
                    refreshDataForTable();
                }else {
                    JOptionPane.showMessageDialog(null, "删除失败", "提示", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        // 获取选中的模板数量
        JMenuItem openItem = new JMenuItem("已选中POC数量: " + templatesTable.getSelectedRowCount());

        // 打开选中的POC文件夹
        JMenuItem openFolderItem = new JMenuItem("打开选中POC所在文件夹");
        openFolderItem.addActionListener(e -> {
            // 获取选中的模板,提取path参数
            int[] selectedRows = templatesTable.getSelectedRows();
            for (int selectedRow : selectedRows) {
                String id = (String) templatesTable.getValueAt(selectedRow, 0);
                String path = templates.get(Integer.parseInt(id) - 1).get("path");
                try {
                    // https://blog.csdn.net/jazywoo123/article/details/7884094
                    // 别造谣这里有rce
                    Runtime.getRuntime().exec(
                            "rundll32 SHELL32.DLL,ShellExec_RunDLL "
                                    + "Explorer.exe /select," + path);
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        });

        // 为选中的POC创建项目
        JMenuItem createProjectItem = new JMenuItem("为选中的POC创建项目");
        createProjectItem.addActionListener(e -> {
            // 存储path
            LinkedList<String> workflows = new LinkedList<>();

            // 获取选中的模板,提取path参数
            int[] selectedRows = templatesTable.getSelectedRows();
            for (int selectedRow : selectedRows) {
                String id = (String) templatesTable.getValueAt(selectedRow, 0);
                workflows.add(templates.get(Integer.parseInt(id) - 1).get("path"));
            }
            String nucleiConfigFile = generateNucleiConfigFile("", workflows);
            String tipsNote = "nuclei -config " + nucleiConfigFile+" " + Utils.templateArg + " -u ";

            // 复制到剪切板
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(tipsNote), null);
            // 弹窗提示创建成功
            JOptionPane.showMessageDialog(null, "创建成功,已复制到粘贴板", "提示", JOptionPane.INFORMATION_MESSAGE);
        });

        // 为选中的POC创建工作流
        JMenuItem createWorkFlowItem = new JMenuItem("为选中的POC创建工作流");
        createWorkFlowItem.addActionListener(e -> {
            // 弹窗获取模板名称
            String engineName = JOptionPane.showInputDialog("请输入工作流名称");
            if (engineName == null || engineName.equals("")) {
                JOptionPane.showMessageDialog(null, "工作流名称不能为空", "错误", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // 存储path
            LinkedList<String> workflows = new LinkedList<>();

            // 获取选中的模板,提取path参数
            int[] selectedRows = templatesTable.getSelectedRows();
            for (int selectedRow : selectedRows) {
                String id = (String) templatesTable.getValueAt(selectedRow, 0);
                workflows.add(templates.get(Integer.parseInt(id) - 1).get("path"));
            }


            String nucleiConfigFile = generateNucleiConfigFile(engineName, workflows);
            String tipsNote = "nuclei -config " + nucleiConfigFile+" "  + Utils.templateArg + " -u ";
            // 复制到剪切板
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(tipsNote), null);
            // 弹窗提示创建成功
            JOptionPane.showMessageDialog(null, "创建成功,已复制到粘贴板", "提示", JOptionPane.INFORMATION_MESSAGE);
        });


        popupMenu.add(editItem);
        popupMenu.add(deleteItem);
        popupMenu.add(openItem);
        popupMenu.add(openFolderItem);
        popupMenu.add(createProjectItem);
        popupMenu.add(createWorkFlowItem);
        return popupMenu;
    }

    private void initToolBar() {
        JToolBar toolBar = new JToolBar();
        toolBar.add(jLabel);

        toolBar.addSeparator();
        toolBar.add(infoBtn);
        toolBar.add(lowBtn);
        toolBar.add(mediumBtn);
        toolBar.add(highBtn);
        toolBar.add(criticalBtn);
        toolBar.addSeparator();
        toolBar.add(refreshBtn);
        toolBar.addSeparator();
        toolBar.add(searchField);

        // 初始化按钮
        jLabel.setText("模板已成功加载: 0个");
        infoBtn.setSelected(true);
        infoBtn.setToolTipText("信息");
        lowBtn.setSelected(true);
        lowBtn.setToolTipText("低");
        mediumBtn.setSelected(true);
        mediumBtn.setToolTipText("中");
        highBtn.setSelected(true);
        highBtn.setToolTipText("高");
        criticalBtn.setSelected(true);
        criticalBtn.setToolTipText("危");


        // 初始化过滤器
        filterList.add("info");
        filterList.add("low");
        filterList.add("medium");
        filterList.add("high");
        filterList.add("critical");

        // 搜索功能
        searchField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Search, Enter");
        searchField.putClientProperty(FlatClientProperties.TEXT_FIELD_TRAILING_ICON, new FlatSearchIcon());
        searchField.registerKeyboardAction(e -> {
                    String searchKeyWord = searchField.getText().trim();
                    // 忽略大小写
                    sorter.setRowFilter(RowFilter.regexFilter("(?i)" + searchKeyWord));
//                    sorter.setRowFilter(RowFilter.regexFilter(searchKeyWord));
                },
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, false),
                JComponent.WHEN_FOCUSED
        );

        infoBtn.addActionListener(e -> {
            filterData();
        });
        lowBtn.addActionListener(e -> {
            filterData();
        });
        mediumBtn.addActionListener(e -> {
            filterData();
        });
        highBtn.addActionListener(e -> {
            filterData();
        });
        criticalBtn.addActionListener(e -> {
            filterData();
        });


        refreshBtn.setToolTipText("表格数据出错时可以点击刷新按钮重新加载");
        refreshBtn.addActionListener(e -> {
            filterData();
        });

        this.add(toolBar, BorderLayout.NORTH);
    }
}
