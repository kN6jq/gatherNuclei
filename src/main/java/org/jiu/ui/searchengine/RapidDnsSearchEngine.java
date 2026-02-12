package org.jiu.ui.searchengine;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import org.jiu.core.RapidDnsCore;
import org.jiu.ui.SearchPanel;

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
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * RapidDNS 子域名搜索引擎
 */
public class RapidDnsSearchEngine extends JPanel implements SearchEngine {
    private final JTextField inputField = new JTextField();
    private final String[] columnNames = {
            "#",
            "Domain",
            "Address",
            "Type",
            "Date"
    };
    private JButton searchBtn = new JButton("查询");
    private JTabbedPane resultsTabbedPane;
    private AtomicInteger tabCounter = new AtomicInteger(0);

    public RapidDnsSearchEngine() {
        this.setLayout(new BorderLayout());
        initToolBar();
        initTabbedPane();
    }

    /**
     * 初始化标签页容器
     */
    private void initTabbedPane() {
        resultsTabbedPane = new JTabbedPane();
        add(resultsTabbedPane, BorderLayout.CENTER);
        addWelcomePanel();
    }

    /**
     * 添加欢迎面板
     */
    private void addWelcomePanel() {
        JPanel welcomePanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(10, 10, 10, 10);

        JLabel welcomeLabel = new JLabel("欢迎使用 RapidDNS 子域名查询");
        welcomeLabel.setFont(new Font("Dialog", Font.BOLD, 24));
        welcomePanel.add(welcomeLabel, gbc);

        gbc.gridy++;
        JLabel instructionLabel = new JLabel("在上方工具栏中输入域名，然后按回车或点击查询按钮");
        instructionLabel.setFont(new Font("Dialog", Font.PLAIN, 14));
        welcomePanel.add(instructionLabel, gbc);

        gbc.gridy++;
        JLabel exampleLabel = new JLabel("示例: baidu.com 或 google.com");
        exampleLabel.setFont(new Font("Dialog", Font.ITALIC, 12));
        exampleLabel.setForeground(Color.GRAY);
        welcomePanel.add(exampleLabel, gbc);

        resultsTabbedPane.addTab("欢迎", welcomePanel);
    }

    /**
     * 初始化表格
     */
    private JTable createTable() {
        JTable table = new JTable();
        DefaultTableModel tableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tableModel.setColumnIdentifiers(columnNames);
        table.setModel(tableModel);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JTextField.CENTER);
        table.getColumn("#").setCellRenderer(centerRenderer);
        table.getColumn("Type").setCellRenderer(centerRenderer);

        table.getColumn("#").setPreferredWidth(10);
        table.getColumn("Type").setPreferredWidth(10);
        table.getColumn("Date").setPreferredWidth(15);

        // 添加右键功能
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == 3) {
                    JPopupMenu popupMenu = createPopupMenu(table);
                    popupMenu.show(table, e.getX(), e.getY());
                }
            }
        });

        return table;
    }

    /**
     * 工具栏
     */
    private void initToolBar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        // 输入框
        inputField.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        inputField.setPreferredSize(new Dimension(300, 25));
        inputField.putClientProperty("JTextField.placeholderText", "输入域名，例如: baidu.com");
        inputField.putClientProperty(FlatClientProperties.TEXT_FIELD_SHOW_CLEAR_BUTTON, true);

        // 搜索按钮
        searchBtn.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        searchBtn.setPreferredSize(new Dimension(80, 25));
        searchBtn.setBackground(new Color(0, 123, 255));
        searchBtn.setForeground(Color.WHITE);
        searchBtn.setFocusPainted(false);

        // 搜索按钮事件
        searchBtn.addActionListener(e -> search());

        // 添加快捷键
        toolBar.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "search");
        toolBar.getActionMap().put("search", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                search();
            }
        });

        // 组装工具栏
        toolBar.add(new JLabel(" 域名: "));
        toolBar.add(inputField);
        toolBar.add(Box.createHorizontalStrut(5));
        toolBar.add(searchBtn);
        toolBar.addSeparator();
        toolBar.add(new JLabel("无需配置，直接查询"));

        this.add(toolBar, BorderLayout.NORTH);
    }

    private void search() {
        String domain = inputField.getText().trim();
        if (domain.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请输入域名");
            return;
        }

        // 简单验证域名格式
        if (!domain.matches("^[a-zA-Z0-9]([a-zA-Z0-9-]*[a-zA-Z0-9])?(\\.[a-zA-Z0-9]([a-zA-Z0-9-]*[a-zA-Z0-9])?)*$")) {
            JOptionPane.showMessageDialog(this, "域名格式不正确");
            return;
        }

        // 创建新的标签页和表格
        JTable newTable = createTable();
        JScrollPane scrollPane = new JScrollPane(newTable);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        // 生成标签页标题
        String tabTitle = domain.substring(0, Math.min(domain.length(), 20)) + (domain.length() > 20 ? "..." : "");
        int tabNumber = tabCounter.incrementAndGet();
        tabTitle = "[" + tabNumber + "] " + tabTitle;

        // 创建包含表格的面板
        JPanel contentPanel = new JPanel(new BorderLayout());

        // 创建统计面板
        JPanel statsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel totalLabel = new JLabel("总计: 0");
        totalLabel.setName("totalLabel");
        totalLabel.setFont(new Font("微软雅黑", Font.BOLD, 12));
        statsPanel.add(totalLabel);
        
        contentPanel.add(scrollPane, BorderLayout.CENTER);
        contentPanel.add(statsPanel, BorderLayout.SOUTH);

        // 添加到标签页
        resultsTabbedPane.addTab(null, new JScrollPane(contentPanel));

        // 添加自定义标签
        JButton closeButton = new JButton("×");
        closeButton.setFont(new Font("Dialog", Font.BOLD, 12));
        closeButton.setPreferredSize(new Dimension(20, 20));
        closeButton.setBorderPainted(false);
        closeButton.setContentAreaFilled(false);

        final JPanel tabWithCloseButton = new JPanel(new BorderLayout());
        tabWithCloseButton.add(new JLabel(tabTitle), BorderLayout.CENTER);
        tabWithCloseButton.add(closeButton, BorderLayout.EAST);

        closeButton.addActionListener(e -> {
            for (int i = 0; i < resultsTabbedPane.getTabCount(); i++) {
                if (resultsTabbedPane.getTabComponentAt(i) == tabWithCloseButton) {
                    resultsTabbedPane.removeTabAt(i);
                    break;
                }
            }
        });

        // 为标签页添加右键菜单
        addTabPopupMenu(tabWithCloseButton);

        resultsTabbedPane.setTabComponentAt(resultsTabbedPane.getTabCount() - 1, tabWithCloseButton);
        resultsTabbedPane.setSelectedIndex(resultsTabbedPane.getTabCount() - 1);

        // 获取当前标签页上的表格和模型
        DefaultTableModel tableModel = (DefaultTableModel) newTable.getModel();

        new Thread(() -> {
            try {
                RapidDnsCore.RapidDnsResult result = RapidDnsCore.getSubdomains(domain);

                SwingUtilities.invokeLater(() -> {
                    if (result.hasError()) {
                        JOptionPane.showMessageDialog(null, result.error);
                        // 移除空的标签页
                        for (int idx = 0; idx < resultsTabbedPane.getTabCount(); idx++) {
                            if (resultsTabbedPane.getTabComponentAt(idx) == tabWithCloseButton) {
                                resultsTabbedPane.removeTabAt(idx);
                                break;
                            }
                        }
                        return;
                    }

                    if (!result.hasData()) {
                        JOptionPane.showMessageDialog(null, "未找到子域名数据");
                        return;
                    }

                    // 清空表格
                    tableModel.setRowCount(0);

                    // 添加数据
                    for (RapidDnsCore.SubdomainRecord record : result.records) {
                        Object[] rowData = {
                            record.index,
                            record.domain,
                            record.address,
                            record.type,
                            record.date
                        };
                        tableModel.addRow(rowData);
                    }

                    // 更新统计
                    totalLabel.setText("总计: " + result.total);

                    // 滚动到顶部
                    SwingUtilities.invokeLater(() -> {
                        JScrollPane parentScrollPane = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, newTable);
                        if (parentScrollPane != null) {
                            parentScrollPane.getVerticalScrollBar().setValue(0);
                        }
                    });
                });

            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(null, "查询失败: " + e.getMessage());
                    // 移除空的标签页
                    for (int idx = 0; idx < resultsTabbedPane.getTabCount(); idx++) {
                        if (resultsTabbedPane.getTabComponentAt(idx) == tabWithCloseButton) {
                            resultsTabbedPane.removeTabAt(idx);
                            break;
                        }
                    }
                });
            }
        }).start();
    }

    private JPopupMenu createPopupMenu(JTable table) {
        JPopupMenu popupMenu = new JPopupMenu();

        // 复制当前选中行数据
        JMenuItem copyRowItem = new JMenuItem("复制当前选中行数据");
        copyRowItem.addActionListener(e -> {
            int[] selectedRows = table.getSelectedRows();
            if (selectedRows.length == 0) {
                JOptionPane.showMessageDialog(null, "请先选择数据");
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                int rowCount = 0;
                for (int selectedRow : selectedRows) {
                    if (rowCount > 0) {
                        stringBuilder.append("\n");
                    }
                    for (int i = 0; i < table.getColumnCount(); i++) {
                        Object cellValue = table.getValueAt(selectedRow, i);
                        if (cellValue != null) {
                            stringBuilder.append(cellValue);
                        }
                        if (i < table.getColumnCount() - 1) {
                            stringBuilder.append("\t");
                        }
                    }
                    rowCount++;
                }
                StringSelection stringSelection = new StringSelection(stringBuilder.toString());
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stringSelection, null);
                JOptionPane.showMessageDialog(null, "已复制到剪贴板");
            }
        });

        // 复制选中行域名
        JMenuItem copyDomainItem = new JMenuItem("复制选中行域名");
        copyDomainItem.addActionListener(e -> {
            int[] selectedRows = table.getSelectedRows();
            if (selectedRows.length == 0) {
                JOptionPane.showMessageDialog(null, "请先选择数据");
            } else {
                Set<String> domains = new HashSet<>();
                for (int selectedRow : selectedRows) {
                    Object cellValue = table.getValueAt(selectedRow, 1);
                    if (cellValue != null && !cellValue.toString().trim().isEmpty()) {
                        domains.add(cellValue.toString());
                    }
                }
                StringSelection stringSelection = new StringSelection(String.join("\n", domains));
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stringSelection, null);
                JOptionPane.showMessageDialog(null, "已复制 " + domains.size() + " 个域名到剪贴板");
            }
        });

        // 复制选中行IP
        JMenuItem copyIpItem = new JMenuItem("复制选中行IP");
        copyIpItem.addActionListener(e -> {
            int[] selectedRows = table.getSelectedRows();
            if (selectedRows.length == 0) {
                JOptionPane.showMessageDialog(null, "请先选择数据");
            } else {
                Set<String> ips = new HashSet<>();
                for (int selectedRow : selectedRows) {
                    Object cellValue = table.getValueAt(selectedRow, 2);
                    if (cellValue != null && !cellValue.toString().trim().isEmpty()) {
                        ips.add(cellValue.toString());
                    }
                }
                StringSelection stringSelection = new StringSelection(String.join("\n", ips));
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stringSelection, null);
                JOptionPane.showMessageDialog(null, "已复制 " + ips.size() + " 个IP到剪贴板");
            }
        });

        // 导出所有数据
        JMenuItem exportItem = new JMenuItem("导出所有数据");
        exportItem.addActionListener(e -> {
            exportToClipboard(table);
        });

        popupMenu.add(copyRowItem);
        popupMenu.add(copyDomainItem);
        popupMenu.add(copyIpItem);
        popupMenu.addSeparator();
        popupMenu.add(exportItem);
        
        return popupMenu;
    }

    /**
     * 导出表格数据到剪贴板
     */
    private void exportToClipboard(JTable table) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        StringBuilder sb = new StringBuilder();

        // 表头
        for (int i = 0; i < model.getColumnCount(); i++) {
            sb.append(model.getColumnName(i));
            if (i < model.getColumnCount() - 1) {
                sb.append("\t");
            }
        }
        sb.append("\n");

        // 数据
        for (int row = 0; row < model.getRowCount(); row++) {
            for (int col = 0; col < model.getColumnCount(); col++) {
                Object value = model.getValueAt(row, col);
                sb.append(value != null ? value.toString() : "");
                if (col < model.getColumnCount() - 1) {
                    sb.append("\t");
                }
            }
            sb.append("\n");
        }

        StringSelection stringSelection = new StringSelection(sb.toString());
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stringSelection, null);
        JOptionPane.showMessageDialog(null, "已导出 " + model.getRowCount() + " 条数据到剪贴板");
    }

    @Override
    public String getTitle() {
        return "RapidDNS";
    }

    @Override
    public FlatSVGIcon getIcon() {
        return null;
    }

    @Override
    public String getTips() {
        return "RapidDNS 子域名查询（免费，无需配置）";
    }

    /**
     * 为标签页添加右键菜单
     */
    private void addTabPopupMenu(JPanel tabComponent) {
        JPopupMenu popupMenu = new JPopupMenu();

        // 关闭当前
        JMenuItem closeCurrentItem = new JMenuItem("关闭当前");
        closeCurrentItem.addActionListener(e -> {
            for (int i = 0; i < resultsTabbedPane.getTabCount(); i++) {
                if (resultsTabbedPane.getTabComponentAt(i) == tabComponent) {
                    resultsTabbedPane.removeTabAt(i);
                    break;
                }
            }
        });

        // 关闭其他
        JMenuItem closeOthersItem = new JMenuItem("关闭其他");
        closeOthersItem.addActionListener(e -> {
            for (int i = resultsTabbedPane.getTabCount() - 1; i >= 0; i--) {
                if (resultsTabbedPane.getTabComponentAt(i) != tabComponent) {
                    resultsTabbedPane.removeTabAt(i);
                }
            }
        });

        // 关闭所有
        JMenuItem closeAllItem = new JMenuItem("关闭所有");
        closeAllItem.addActionListener(e -> {
            resultsTabbedPane.removeAll();
        });

        popupMenu.add(closeCurrentItem);
        popupMenu.add(closeOthersItem);
        popupMenu.addSeparator();
        popupMenu.add(closeAllItem);

        // 为标签页组件添加鼠标监听器
        tabComponent.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    popupMenu.show(e.getComponent(), e.getX(), e.getY());
                } else if (e.getButton() == MouseEvent.BUTTON1) {
                    for (int i = 0; i < resultsTabbedPane.getTabCount(); i++) {
                        if (resultsTabbedPane.getTabComponentAt(i) == tabComponent) {
                            resultsTabbedPane.setSelectedIndex(i);
                            break;
                        }
                    }
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    popupMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
    }
}
