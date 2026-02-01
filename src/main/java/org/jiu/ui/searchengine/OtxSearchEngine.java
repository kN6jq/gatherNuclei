package org.jiu.ui.searchengine;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.icons.FlatSearchIcon;
import org.jiu.core.OtxCore;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.concurrent.atomic.AtomicInteger;

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
    private JButton statusBtn = new JButton();
    private JTabbedPane resultsTabbedPane; // 用于显示多个搜索结果的标签页
    private AtomicInteger tabCounter = new AtomicInteger(0); // 标签页计数器

    public OtxSearchEngine() {
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

    /**
     * 初始化表格
     */
    private JTable createTable() {
        JTable table = new JTable();
        DefaultTableModel tableModel = new DefaultTableModel() {
            // 可编辑
            @Override
            public boolean isCellEditable(int row, int column) {
                return true;
            }
        };

        tableModel.setColumnIdentifiers(columnNames);
        table.setModel(tableModel);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        // 设置表格内容居中
        DefaultTableCellRenderer tcr = new DefaultTableCellRenderer();
        tcr.setHorizontalAlignment(JLabel.CENTER);
        table.setDefaultRenderer(Object.class, tcr);

        // 设置第一列的列宽
        table.getColumnModel().getColumn(0).setPreferredWidth(5);
        // 设置第三列的列宽
        table.getColumnModel().getColumn(2).setPreferredWidth(250);
        // 添加右键功能
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == 3) {
                    // 模板面板右键功能
                    JPopupMenu popupMenu = createPopupMenu(table);
                    popupMenu.show(table, e.getX(), e.getY());
                }
            }
        });

        return table;
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
            }
        });


        popupMenu.add(copyRowItem);
        return popupMenu;

    }

    private void initToolBar() {
        JToolBar toolBar = new JToolBar();
        inputField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "OTX Search... & Enter");
        inputField.putClientProperty(FlatClientProperties.TEXT_FIELD_TRAILING_ICON, new FlatSearchIcon());
        searchBtn.setText("搜索");
        searchBtn.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        searchBtn.setPreferredSize(new Dimension(80, 25));
        searchBtn.setBackground(new Color(0, 123, 255));
        searchBtn.setForeground(Color.WHITE);
        searchBtn.setFocusPainted(false);
        statusBtn.setToolTipText("搜索状态提示灯");

        toolBar.add(inputField);
        toolBar.add(searchBtn);
        toolBar.add(statusBtn);
        toolBar.addSeparator();

        // 搜索按钮
        searchBtn.addActionListener(e -> {
            search(1);
        });

        this.add(toolBar, BorderLayout.NORTH);
    }

    private void search(int page) {
        String searchData = inputField.getText().trim();
        if (searchData.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请输入搜索内容");
            return;
        }

        // 创建新的标签页和表格
        JTable newTable = createTable();
        JScrollPane scrollPane = new JScrollPane(newTable);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        // 生成标签页标题
        String tabTitle = searchData.substring(0, Math.min(searchData.length(), 20)) + (searchData.length() > 20 ? "..." : "");
        int tabNumber = tabCounter.incrementAndGet();
        tabTitle = "[" + tabNumber + "] " + tabTitle;

        // 创建分页状态Holder
        final PageState pageState = new PageState();
        pageState.lastQuery[0] = searchData;

        // 创建包含表格和分页控件的面板
        JPanel contentPanel = new JPanel(new BorderLayout());

        // 创建分页面板，传递分页状态
        JPanel pagingPanel = createPagingPanel(newTable, searchData, tabTitle, pageState);

        contentPanel.add(scrollPane, BorderLayout.CENTER);
        contentPanel.add(pagingPanel, BorderLayout.SOUTH);
        
        // 添加到标签页
        resultsTabbedPane.addTab(null, new JScrollPane(contentPanel)); // 先添加标签页
        
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
            // 动态获取当前tab的索引
            for (int i = 0; i < resultsTabbedPane.getTabCount(); i++) {
                if (resultsTabbedPane.getTabComponentAt(i) == tabWithCloseButton) {
                    resultsTabbedPane.removeTabAt(i);
                    break;
                }
            }
        });
        
        resultsTabbedPane.setTabComponentAt(resultsTabbedPane.getTabCount() - 1, tabWithCloseButton); // 然后设置自定义组件
        resultsTabbedPane.setSelectedIndex(resultsTabbedPane.getTabCount() - 1); // 选择新标签页

        // 获取当前标签页上的表格和模型
        DefaultTableModel tableModel = (DefaultTableModel) newTable.getModel();

        String data = inputField.getText();
        new Thread(() -> {
            JSONObject responseJson = JSONUtil.parseObj(OtxCore.getData(data, page, 100));
            // 获取总数
            String full_size = responseJson.getStr("full_size");
            int sizePage = Integer.parseInt(full_size);
            int totalPage = sizePage / 100;
            if (sizePage % 100 != 0) {
                totalPage++;
            }
            
            int finalTotalPage = totalPage;
            int finalSizePage = sizePage;
            SwingUtilities.invokeLater(() -> {
                resetTableRows(responseJson.getJSONArray("url_list"), tableModel, pagingPanel, page, finalTotalPage, finalSizePage, pageState);
            });
        }).start();
    }

    /**
     * 创建分页面板
     */
    private JPanel createPagingPanel(JTable table, String searchData, String tabTitle, PageState pageState) {
        JPanel pagingPanel = new JPanel();
        pagingPanel.setLayout(new FlowLayout());
        
        JButton predBtn = new JButton("上一页");
        JButton nextBtn = new JButton("下一页");
        JButton pageCurrentLabel = new JButton("1");
        JButton pageTotalLabel = new JButton("总页");
        JButton sizeLabel = new JButton("总数");

        // 设置按钮的固定标识符
        pageCurrentLabel.setName("pageCurrentLabel");
        pageTotalLabel.setName("pageTotalLabel");
        sizeLabel.setName("sizeLabel");

        // 初始化分页组件
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

        // 为分页按钮添加事件，使用传入的pageState
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
     * 执行分页搜索
     */
    private void performPagingSearch(JTable table, String searchData, int page, int totalPages, int[] currentPageRef, int[] totalPagesRef, JPanel pagingPanel, PageState pageState) {
        new Thread(() -> {
            JSONObject responseJson = JSONUtil.parseObj(OtxCore.getData(searchData, page, 100));
            // 获取总数
            String full_size = responseJson.getStr("full_size");
            int sizePage = Integer.parseInt(full_size);
            int totalPage = sizePage / 100;
            if (sizePage % 100 != 0) {
                totalPage++;
            }
            
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
     * 为分页搜索重置表格行
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

        // 更新分页组件
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

        // 更新分页状态
        if (pageState != null) {
            pageState.currentPage[0] = currentPage;
            pageState.totalPages[0] = totalPage;
        }
        }

        // 滚动到顶部
        SwingUtilities.invokeLater(() -> {
            JScrollPane scrollPane = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, table);
            if (scrollPane != null) {
                scrollPane.getVerticalScrollBar().setValue(0);
            }
        });
    }

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
        
        // 更新分页组件
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

        // 更新分页状态
        if (pageState != null) {
            pageState.currentPage[0] = currentPage;
            pageState.totalPages[0] = totalPage;
        }
        }

        // 更新分页状态
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
        return null;
    }

    @Override
    public String getTips() {
        return "获取历史信息";
    }
}
