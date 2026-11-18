package org.jiu.ui.searchengine;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import org.jiu.core.OtxCore;
import org.jiu.ui.InitUI;
import org.jiu.ui.component.Icons;
import org.jiu.utils.UIUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * OTX（AlienVault Open Threat Exchange）搜索引擎面板：按关键词查询历史 URL 资产，多标签页展示并支持分页。
 */
public class OtxSearchEngine extends JPanel implements SearchEngine {

    private final String[] columnNames = {
            "#",
            "Domain",
            "URL",
            "hostname",
            "ip",
            "http_code",
            "date"
    };
    private JTextField inputField = new JTextField();
    private JButton searchBtn = new JButton();
    private JLabel statusBtn = new JLabel();
    private JTabbedPane resultsTabbedPane;
    private AtomicInteger tabCounter = new AtomicInteger(0);

    public OtxSearchEngine() {
        this.setLayout(new BorderLayout());
        initToolBar();
        initTabbedPane();
    }

    private void initTabbedPane() {
        resultsTabbedPane = new JTabbedPane();
        InitUI.styleTabbedPane(resultsTabbedPane, false);
        add(resultsTabbedPane, BorderLayout.CENTER);
        addWelcomePanel();
    }

    private void addWelcomePanel() {
        JPanel welcomePanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(10, 10, 10, 10);

        JLabel welcomeLabel = new JLabel("欢迎使用OTX搜索引擎");
        welcomeLabel.setFont(new Font("Dialog", Font.BOLD, 24));
        welcomePanel.add(welcomeLabel, gbc);

        gbc.gridy++;
        JLabel instructionLabel = new JLabel("在上方工具栏中输入搜索关键词，然后按回车或点击搜索按钮");
        instructionLabel.setFont(new Font("Dialog", Font.PLAIN, 14));
        welcomePanel.add(instructionLabel, gbc);

        gbc.gridy++;
        JLabel exampleLabel = new JLabel("示例: 输入域名或关键词");
        exampleLabel.setFont(new Font("Dialog", Font.ITALIC, 12));
        exampleLabel.setForeground(Color.GRAY);
        welcomePanel.add(exampleLabel, gbc);

        resultsTabbedPane.addTab("欢迎", welcomePanel);
    }

    private JTable createTable() {
        JTable table = new JTable();
        DefaultTableModel tableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return true;
            }
        };

        tableModel.setColumnIdentifiers(columnNames);
        table.setModel(tableModel);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        DefaultTableCellRenderer tcr = new DefaultTableCellRenderer();
        tcr.setHorizontalAlignment(JLabel.CENTER);
        table.setDefaultRenderer(Object.class, tcr);

        table.getColumnModel().getColumn(0).setPreferredWidth(5);
        table.getColumnModel().getColumn(2).setPreferredWidth(250);
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
     * 构建表格右键菜单：将选中行各列以制表符拼接复制到系统剪贴板。
     */
    private JPopupMenu createPopupMenu(JTable table) {
        JPopupMenu popupMenu = new JPopupMenu();


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
            }
        });


        popupMenu.add(copyRowItem);
        return popupMenu;

    }

    private void initToolBar() {
        JToolBar toolBar = new JToolBar();
        UIUtils.setupSearchField(inputField, "OTX Search... & Enter");
        UIUtils.setupSearchButton(searchBtn, "搜索");
        UIUtils.setupStatusLabel(statusBtn);

        toolBar.add(inputField);
        toolBar.add(searchBtn);
        toolBar.addSeparator();
        toolBar.add(statusBtn);

        searchBtn.addActionListener(e -> {
            search(1);
        });

        this.add(toolBar, BorderLayout.NORTH);
    }

    /**
     * 发起一次 OTX 查询：新建结果标签页与分页面板，后台线程请求指定页（每页 100 条），完成后在 EDT 填充表格。
     *
     * @param page 页码，从 1 开始
     */
    private void search(int page) {
        String searchData = inputField.getText().trim();
        if (searchData.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请输入搜索内容");
            return;
        }

        JTable newTable = createTable();
        JScrollPane scrollPane = new JScrollPane(newTable);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        String tabTitle = searchData.substring(0, Math.min(searchData.length(), 20)) + (searchData.length() > 20 ? "..." : "");
        int tabNumber = tabCounter.incrementAndGet();
        tabTitle = "[" + tabNumber + "] " + tabTitle;

        final PageState pageState = new PageState();
        pageState.lastQuery[0] = searchData;

        JPanel contentPanel = new JPanel(new BorderLayout());

        JPanel pagingPanel = createPagingPanel(newTable, searchData, tabTitle, pageState);

        contentPanel.add(scrollPane, BorderLayout.CENTER);
        contentPanel.add(pagingPanel, BorderLayout.SOUTH);

        resultsTabbedPane.addTab(null, new JScrollPane(contentPanel)); // 先占位添加，随后才能设置自定义标签组件

        JButton closeButton = new JButton("×");
        closeButton.setFont(new Font("Dialog", Font.BOLD, 12));
        closeButton.setPreferredSize(new Dimension(20, 20));
        closeButton.setBorderPainted(false);
        closeButton.setContentAreaFilled(false);

        final JPanel tabWithCloseButton = new JPanel(new BorderLayout());
        tabWithCloseButton.add(new JLabel(tabTitle), BorderLayout.CENTER);
        tabWithCloseButton.add(closeButton, BorderLayout.EAST);

        closeButton.addActionListener(e -> {
            // 标签索引随关闭操作变化，按组件引用动态查找
            for (int i = 0; i < resultsTabbedPane.getTabCount(); i++) {
                if (resultsTabbedPane.getTabComponentAt(i) == tabWithCloseButton) {
                    resultsTabbedPane.removeTabAt(i);
                    break;
                }
            }
        });

        addTabPopupMenu(tabWithCloseButton);

        resultsTabbedPane.setTabComponentAt(resultsTabbedPane.getTabCount() - 1, tabWithCloseButton);
        resultsTabbedPane.setSelectedIndex(resultsTabbedPane.getTabCount() - 1);

        DefaultTableModel tableModel = (DefaultTableModel) newTable.getModel();

        String data = inputField.getText();
        new Thread(() -> {
            JSONObject responseJson = JSONUtil.parseObj(OtxCore.getData(data, page, 100));
            String full_size = responseJson.getStr("full_size");
            int sizePage = Integer.parseInt(full_size);
            int totalPage = (int) Math.ceil((double) sizePage / 100);

            int finalTotalPage = totalPage;
            int finalSizePage = sizePage;
            SwingUtilities.invokeLater(() -> {
                resetTableRows(responseJson.getJSONArray("url_list"), tableModel, pagingPanel, page, finalTotalPage, finalSizePage, pageState);
            });
        }).start();
    }

    /**
     * 创建分页控件面板；页码/总页/总数按钮以固定 name 标识，供填充结果时按名查找更新。
     */
    private JPanel createPagingPanel(JTable table, String searchData, String tabTitle, PageState pageState) {
        JPanel pagingPanel = new JPanel();
        pagingPanel.setLayout(new FlowLayout());

        JButton predBtn = new JButton("上一页");
        JButton nextBtn = new JButton("下一页");
        JButton pageCurrentLabel = new JButton("1");
        JButton pageTotalLabel = new JButton("总页");
        JButton sizeLabel = new JButton("总数");

        // 固定 name 标识，供 resetTableRows 系列方法按名查找更新
        pageCurrentLabel.setName("pageCurrentLabel");
        pageTotalLabel.setName("pageTotalLabel");
        sizeLabel.setName("sizeLabel");

        predBtn.setSelected(true);
        pageCurrentLabel.setEnabled(false);
        pageCurrentLabel.setSelected(true);
        pageCurrentLabel.setToolTipText("当前页 1");
        pageTotalLabel.setToolTipText("总页 1");
        pageTotalLabel.setEnabled(false);
        pageTotalLabel.setSelected(true);
        sizeLabel.setEnabled(false);
        sizeLabel.setSelected(true);
        nextBtn.setSelected(true);

        pagingPanel.add(predBtn);
        pagingPanel.add(pageCurrentLabel);
        pagingPanel.add(pageTotalLabel);
        pagingPanel.add(sizeLabel);
        pagingPanel.add(nextBtn);

        predBtn.addActionListener(e -> {
            if (pageState.currentPage[0] > 1) {
                int newPage = pageState.currentPage[0] - 1;
                performPagingSearch(table, pageState.lastQuery[0], newPage, pageState.totalPages[0], pageState.currentPage, pageState.totalPages, pagingPanel, pageState);
            }
        });

        nextBtn.addActionListener(e -> {
            if (pageState.currentPage[0] < pageState.totalPages[0]) {
                int newPage = pageState.currentPage[0] + 1;
                performPagingSearch(table, pageState.lastQuery[0], newPage, pageState.totalPages[0], pageState.currentPage, pageState.totalPages, pagingPanel, pageState);
            }
        });

        return pagingPanel;
    }

    /**
     * 翻页查询：后台线程请求指定页，完成后在 EDT 重置表格行并回写当前页/总页。
     *
     * @param page 目标页码，从 1 开始
     */
    private void performPagingSearch(JTable table, String searchData, int page, int totalPages, int[] currentPageRef, int[] totalPagesRef, JPanel pagingPanel, PageState pageState) {
        new Thread(() -> {
            JSONObject responseJson = JSONUtil.parseObj(OtxCore.getData(searchData, page, 100));
            String full_size = responseJson.getStr("full_size");
            int sizePage = Integer.parseInt(full_size);
            int totalPage = (int) Math.ceil((double) sizePage / 100);

            int finalTotalPage = totalPage;
            int finalSizePage = sizePage;
            SwingUtilities.invokeLater(() -> {
                DefaultTableModel tableModel = (DefaultTableModel) table.getModel();
                resetTableRowsForPaging(responseJson.getJSONArray("url_list"), tableModel, pagingPanel, page, finalTotalPage, finalSizePage, pageState, table);
                currentPageRef[0] = page;
                totalPagesRef[0] = finalTotalPage;
            });
        }).start();
    }

    /**
     * 翻页结果填充：重建表格行、更新分页按钮显示与 pageState，并将表格滚动到顶部（需在 EDT 调用）。
     */
    private void resetTableRowsForPaging(JSONArray jsonArray, DefaultTableModel tableModel, JPanel pagingPanel, int currentPage, int totalPage, int sizePage, PageState pageState, JTable table) {
        tableModel.setRowCount(0);
        for (int i = 0; i < jsonArray.size(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            String domain = jsonObject.getStr("domain");
            String url = jsonObject.getStr("url");
            String hostname = jsonObject.getStr("hostname");
            String ip = jsonObject.getJSONObject("result").getJSONObject("urlworker").getStr("ip");
            String http_code = jsonObject.getJSONObject("result").getJSONObject("urlworker").getStr("http_code");
            String date = jsonObject.getStr("date");
            tableModel.addRow(new Object[]{i + 1, domain, url, hostname, ip, http_code, date});
        }

        Component[] components = pagingPanel.getComponents();
        JButton pageCurrentLabel = null;
        JButton pageTotalLabel = null;
        JButton sizeLabel = null;

        for (Component comp : components) {
            if (comp instanceof JButton) {
                JButton btn = (JButton) comp;
                String name = btn.getName();
                if (name != null) {
                    if (name.equals("pageCurrentLabel")) {
                        pageCurrentLabel = btn;
                    } else if (name.equals("pageTotalLabel")) {
                        pageTotalLabel = btn;
                    } else if (name.equals("sizeLabel")) {
                        sizeLabel = btn;
                    }
                }
            }
        }

        if (pageCurrentLabel != null) {
            pageCurrentLabel.setText(String.valueOf(currentPage));
            pageCurrentLabel.setToolTipText("当前页 " + currentPage);
        }
        if (pageTotalLabel != null) {
            pageTotalLabel.setText(String.valueOf(totalPage));
            pageTotalLabel.setToolTipText("总页 " + totalPage);
        }
        if (sizeLabel != null) {
            sizeLabel.setText(String.valueOf(sizePage));
            sizeLabel.setToolTipText("总数 " + sizePage);

            if (pageState != null) {
                pageState.currentPage[0] = currentPage;
                pageState.totalPages[0] = totalPage;
            }
        }

        SwingUtilities.invokeLater(() -> {
            JScrollPane scrollPane = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, table);
            if (scrollPane != null) {
                scrollPane.getVerticalScrollBar().setValue(0);
            }
        });
    }

    /**
     * 首次查询结果填充：重建表格行、更新分页按钮显示与分页状态。
     */
    private void resetTableRows(JSONArray jsonArray, DefaultTableModel tableModel, JPanel pagingPanel, int currentPage, int totalPage, int sizePage, PageState pageState) {
        tableModel.setRowCount(0);
        for (int i = 0; i < jsonArray.size(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            String domain = jsonObject.getStr("domain");
            String url = jsonObject.getStr("url");
            String hostname = jsonObject.getStr("hostname");
            String ip = jsonObject.getJSONObject("result").getJSONObject("urlworker").getStr("ip");
            String http_code = jsonObject.getJSONObject("result").getJSONObject("urlworker").getStr("http_code");
            String date = jsonObject.getStr("date");
            tableModel.addRow(new Object[]{i + 1, domain, url, hostname, ip, http_code, date});
        }

        Component[] components = pagingPanel.getComponents();
        JButton pageCurrentLabel = null;
        JButton pageTotalLabel = null;
        JButton sizeLabel = null;

        for (Component comp : components) {
            if (comp instanceof JButton) {
                JButton btn = (JButton) comp;
                String name = btn.getName();
                if (name != null) {
                    if (name.equals("pageCurrentLabel")) {
                        pageCurrentLabel = btn;
                    } else if (name.equals("pageTotalLabel")) {
                        pageTotalLabel = btn;
                    } else if (name.equals("sizeLabel")) {
                        sizeLabel = btn;
                    }
                }
            }
        }

        if (pageCurrentLabel != null) {
            pageCurrentLabel.setText(String.valueOf(currentPage));
            pageCurrentLabel.setToolTipText("当前页 " + currentPage);
        }
        if (pageTotalLabel != null) {
            pageTotalLabel.setText(String.valueOf(totalPage));
            pageTotalLabel.setToolTipText("总页 " + totalPage);
        }
        if (sizeLabel != null) {
            sizeLabel.setText(String.valueOf(sizePage));
            sizeLabel.setToolTipText("总数 " + sizePage);

            if (pageState != null) {
                pageState.currentPage[0] = currentPage;
                pageState.totalPages[0] = totalPage;
            }
        }

        if (pageState != null) {
            pageState.currentPage[0] = currentPage;
            pageState.totalPages[0] = totalPage;
        }
    }

    @Override
    public String getTitle() {
        return "otx";
    }

    @Override
    public FlatSVGIcon getIcon() {
        return Icons.create("otx");
    }

    @Override
    public String getTips() {
        return "获取历史信息";
    }

    /**
     * 为标签页组件绑定右键菜单（关闭当前/其他/所有）及左键切换选中。
     */
    private void addTabPopupMenu(JPanel tabComponent) {
        JPopupMenu popupMenu = new JPopupMenu();

        JMenuItem closeCurrentItem = new JMenuItem("关闭当前");
        closeCurrentItem.addActionListener(e -> {
            for (int i = 0; i < resultsTabbedPane.getTabCount(); i++) {
                if (resultsTabbedPane.getTabComponentAt(i) == tabComponent) {
                    resultsTabbedPane.removeTabAt(i);
                    break;
                }
            }
        });

        JMenuItem closeOthersItem = new JMenuItem("关闭其他");
        closeOthersItem.addActionListener(e -> {
            // 从后向前遍历，避免索引变化问题
            for (int i = resultsTabbedPane.getTabCount() - 1; i >= 0; i--) {
                if (resultsTabbedPane.getTabComponentAt(i) != tabComponent) {
                    resultsTabbedPane.removeTabAt(i);
                }
            }
        });

        JMenuItem closeAllItem = new JMenuItem("关闭所有");
        closeAllItem.addActionListener(e -> {
            resultsTabbedPane.removeAll();
        });

        popupMenu.add(closeCurrentItem);
        popupMenu.add(closeOthersItem);
        popupMenu.addSeparator();
        popupMenu.add(closeAllItem);

        tabComponent.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    popupMenu.show(e.getComponent(), e.getX(), e.getY());
                } else if (e.getButton() == MouseEvent.BUTTON1) {
                    // 自定义标签组件不会自动选中，左键点击时手动切换到对应页
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
