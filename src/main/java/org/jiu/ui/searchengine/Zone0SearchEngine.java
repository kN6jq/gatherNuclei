package org.jiu.ui.searchengine;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.icons.FlatSearchIcon;
import org.jiu.core.FofaCore;
import org.jiu.core.ShodanCore;
import org.jiu.core.ZoneCore;
import org.jiu.ui.SearchPanel;
import org.jiu.ui.component.MultiComboBox;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public class Zone0SearchEngine extends JPanel implements SearchEngine {
    private final JTextField inputField = new JTextField();
    private final JButton statusBtn = new JButton();
    private JButton searchBtn = new JButton("搜索");
    private static JComboBox searchTypeComboBox = new JComboBox();
    // Site (信息系统) - 扩展字段
    private final String[] sitecolumnNames = {"#", "IP", "Port", "URL", "Title", "Component", "Service", "Protocol", "City", "Operator", "Status", "Company", "Tags"};
    // APK (移动端应用) - 扩展字段，包含id字段(wechat_id或app_id)
    private final String[] apkcolumnNames = {"#", "Title", "Type", "ID", "Company/Group", "Introduction"};
    private JTabbedPane resultsTabbedPane; // 用于显示多个搜索结果的标签页
    private AtomicInteger tabCounter = new AtomicInteger(0); // 标签页计数器

    public Zone0SearchEngine() {
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
     * 初始化表格
     */
    private JTable createTable(String[] columnNames) {
        JTable table = new JTable();
        DefaultTableModel tableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return true; // 可编辑
            }
        };

        tableModel.setColumnIdentifiers(columnNames);
        table.setModel(tableModel);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JTextField.CENTER);
        table.getColumn("#").setCellRenderer(centerRenderer);

        table.getColumn("#").setPreferredWidth(10);

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
        // 复制选中行ip
        JMenuItem copyRowIpItem = new JMenuItem("复制当前选中行IP");
        copyRowIpItem.addActionListener(e -> {
            int[] selectedRows = table.getSelectedRows();
            if (selectedRows.length == 0) {
                JOptionPane.showMessageDialog(null, "请先选择数据");
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                int validCount = 0;
                for (int selectedRow : selectedRows) {
                    Object cellValue = table.getValueAt(selectedRow, 1);
                    if (cellValue != null && !cellValue.toString().trim().isEmpty()) {
                        if (validCount > 0) {
                            stringBuilder.append("\n");
                        }
                        stringBuilder.append(cellValue);
                        validCount++;
                    }
                }
                StringSelection stringSelection = new StringSelection(stringBuilder.toString());
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stringSelection, null);
            }
        });
        // 复制选中行url
        JMenuItem copyRowUrlItem = new JMenuItem("复制当前选中行URL");
        copyRowUrlItem.addActionListener(e -> {
            int[] selectedRows = table.getSelectedRows();
            if (selectedRows.length == 0) {
                JOptionPane.showMessageDialog(null, "请先选择数据");
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                int validCount = 0;
                for (int selectedRow : selectedRows) {
                    Object cellValue = table.getValueAt(selectedRow, 3);
                    if (cellValue != null && !cellValue.toString().trim().isEmpty()) {
                        if (validCount > 0) {
                            stringBuilder.append("\n");
                        }
                        stringBuilder.append(cellValue);
                        validCount++;
                    }
                }
                StringSelection stringSelection = new StringSelection(stringBuilder.toString());
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stringSelection, null);
            }
        });
        // 将选中的ip发送到shodan
        JMenuItem sendToShodanItem = new JMenuItem("使用shodan扫描选中的ip");
        sendToShodanItem.addActionListener(e -> {
            int[] selectedRows = table.getSelectedRows();
            if (selectedRows.length == 0) {
                JOptionPane.showMessageDialog(null, "请先选择数据");
            } else {
                Set<String> ips = new HashSet<>();
                for (int selectedRow : selectedRows) {
                    String ip = (String) table.getValueAt(selectedRow, 1);
                    ips.add(ip);
                }
                ShodanPortSearchEngine.inputArea.setText(String.join("\n", ips));
                // 切换到ShodanPort标签页
                SearchPanel.switchToShodanPort();
            }
        });


        popupMenu.add(copyRowItem);
        popupMenu.add(copyRowIpItem);
        popupMenu.add(copyRowUrlItem);
        popupMenu.add(sendToShodanItem);
        return popupMenu;
    }

    private void initToolBar() {
        JToolBar toolBar = new JToolBar();
        inputField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "0Zone Search... & Enter");
        inputField.putClientProperty(FlatClientProperties.TEXT_FIELD_TRAILING_ICON, new FlatSearchIcon());
        statusBtn.setToolTipText("搜索状态提示灯");
        searchTypeComboBox.setModel(new DefaultComboBoxModel(new String[]{"信息系统", "移动端应用", "域名"}));

        // 回车搜索事件
        inputField.registerKeyboardAction(e -> {
            search(1);
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, false), JComponent.WHEN_FOCUSED);

        // 搜索按钮事件
        searchBtn.addActionListener(e -> {
            search(1);
        });

        // 设置搜索按钮样式
        searchBtn.setText("搜索");
        searchBtn.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        searchBtn.setPreferredSize(new Dimension(80, 25));
        searchBtn.setBackground(new Color(0, 123, 255));
        searchBtn.setForeground(Color.WHITE);
        searchBtn.setFocusPainted(false);

        toolBar.add(inputField);
        toolBar.add(searchBtn);
        toolBar.add(statusBtn);
        toolBar.add(searchTypeComboBox);
        this.add(toolBar, BorderLayout.NORTH);
    }

    private void search(int p) {
        String searchData = inputField.getText().trim();
        if (searchData.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请输入搜索内容");
            return;
        }

        String type = searchTypeComboBox.getSelectedItem().toString();
        String input = inputField.getText();
        String searchType;
        if ("信息系统".equals(type)) {
            searchType = "site";
        } else if ("移动端应用".equals(type)) {
            searchType = "apk";
        } else if ("域名".equals(type)) {
            searchType = "domain";
        } else {
            searchType = "site";
        }

        if (searchType.equals("domain")) {
            JOptionPane.showMessageDialog(null, "暂不支持域名搜索,没有会员");
            return;
        }

        // 创建新的标签页和表格
        String[] columnNames = searchType.equals("site") ? sitecolumnNames : apkcolumnNames;
        JTable newTable = createTable(columnNames);
        JScrollPane scrollPane = new JScrollPane(newTable);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        // 生成标签页标题
        String tabTitle = searchData.substring(0, Math.min(searchData.length(), 20)) + (searchData.length() > 20 ? "..." : "");
        int tabNumber = tabCounter.incrementAndGet();
        tabTitle = "[" + tabNumber + "] " + tabTitle + " - " + type;

        // 创建分页状态Holder
        final PageState pageState = new PageState();
        pageState.lastQuery[0] = searchData;
        pageState.lastSearchType[0] = searchType;

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
        
        // 为标签页添加右键菜单
        addTabPopupMenu(tabWithCloseButton);
        
        resultsTabbedPane.setTabComponentAt(resultsTabbedPane.getTabCount() - 1, tabWithCloseButton); // 然后设置自定义组件
        resultsTabbedPane.setSelectedIndex(resultsTabbedPane.getTabCount() - 1); // 选择新标签页

        // 获取当前标签页上的表格和模型
        DefaultTableModel tableModel = (DefaultTableModel) newTable.getModel();

        new Thread(() -> {
            try {
                JSONObject responseJson;
                if (searchType.equals("site")) {
                    String sitedata = String.format("(company==%s)||(title==%s)||(banner==%s)||(component==%s)||(ssl_info.detail==%s)", input, input, input, input, input, input);
                    responseJson = JSONUtil.parseObj(ZoneCore.getData(sitedata, searchType, p, 100));
                    int sizePage = Integer.parseInt(responseJson.getStr("total"));
                    int totalPage = (int) Math.ceil((double) sizePage / 100);
                    JSONArray jsonArray = responseJson.getJSONArray("data");
                    SwingUtilities.invokeLater(() -> {
                        resetTableRowsSite(jsonArray, tableModel, pagingPanel, p, totalPage, sizePage, pageState);
                    });
                } else { // apk
                    String apkdata = String.format("(group==%s)||(company==%s)||(title==%s)", input, input, input);
                    responseJson = JSONUtil.parseObj(ZoneCore.getData(apkdata, searchType, p, 100));
                    int sizePage = Integer.parseInt(responseJson.getStr("total"));
                    int totalPage = (int) Math.ceil((double) sizePage / 100);
                    JSONArray jsonArray = responseJson.getJSONArray("data");
                    SwingUtilities.invokeLater(() -> {
                        resetTableRowsApk(jsonArray, tableModel, pagingPanel, p, totalPage, sizePage, pageState);
                    });
                }
            } catch (Exception e) {
                JSONObject errorJson = JSONUtil.parseObj("{\"message\":\"请求失败: " + e.getMessage() + "\"}");
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(null, errorJson.getStr("message"));
                    // 移除空的标签页
                    for (int i = 0; i < resultsTabbedPane.getTabCount(); i++) {
                        if (resultsTabbedPane.getTabComponentAt(i) == tabWithCloseButton) {
                            resultsTabbedPane.removeTabAt(i);
                            break;
                        }
                    }
                });
            }
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
                performPagingSearch(table, pageState.lastQuery[0], newPage, pageState.totalPages[0], pageState.currentPage, pageState.totalPages, pageState.lastSearchType[0], pagingPanel, pageState);
            }
        });

        nextBtn.addActionListener(e -> {
            if (pageState.currentPage[0] < pageState.totalPages[0]) {
                int newPage = pageState.currentPage[0] + 1;
                performPagingSearch(table, pageState.lastQuery[0], newPage, pageState.totalPages[0], pageState.currentPage, pageState.totalPages, pageState.lastSearchType[0], pagingPanel, pageState);
            }
        });

        return pagingPanel;
    }

    /**
     * 执行分页搜索
     */
    private void performPagingSearch(JTable table, String searchData, int page, int totalPages, int[] currentPageRef, int[] totalPagesRef, String searchType, JPanel pagingPanel, PageState pageState) {
        String input = searchData;
        new Thread(() -> {
            try {
                JSONObject responseJson;
                if (searchType.equals("site")) {
                    String sitedata = String.format("(company==%s)||(title==%s)||(banner==%s)||(component==%s)||(ssl_info.detail==%s)", input, input, input, input, input, input);
                    responseJson = JSONUtil.parseObj(ZoneCore.getData(sitedata, searchType, page, 100));
                    int sizePage = Integer.parseInt(responseJson.getStr("total"));
                    int totalPage = (int) Math.ceil((double) sizePage / 100);
                    JSONArray jsonArray = responseJson.getJSONArray("data");
                    SwingUtilities.invokeLater(() -> {
                        DefaultTableModel tableModel = (DefaultTableModel) table.getModel();
                        resetTableRowsSiteForPaging(jsonArray, tableModel, pagingPanel, page, totalPage, sizePage, pageState, table);
                        currentPageRef[0] = page;
                        totalPagesRef[0] = totalPage;
                    });
                } else { // apk
                    String apkdata = String.format("(group==%s)||(company==%s)||(title==%s)", input, input, input);
                    responseJson = JSONUtil.parseObj(ZoneCore.getData(apkdata, searchType, page, 100));
                    int sizePage = Integer.parseInt(responseJson.getStr("total"));
                    int totalPage = (int) Math.ceil((double) sizePage / 100);
                    JSONArray jsonArray = responseJson.getJSONArray("data");
                    SwingUtilities.invokeLater(() -> {
                        DefaultTableModel tableModel = (DefaultTableModel) table.getModel();
                        resetTableRowsApkForPaging(jsonArray, tableModel, pagingPanel, page, totalPage, sizePage, pageState, table);
                        currentPageRef[0] = page;
                        totalPagesRef[0] = totalPage;
                    });
                }
            } catch (Exception e) {
                JSONObject errorJson = JSONUtil.parseObj("{\"message\":\"请求失败: " + e.getMessage() + "\"}");
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(null, errorJson.getStr("message"));
                });
            }
        }).start();
    }
    
    private void resetTableRowsSite(JSONArray jsonArray, DefaultTableModel tableModel, JPanel pagingPanel, int currentPage, int totalPage, int sizePage, PageState pageState) {
        tableModel.setRowCount(0);
        for (int i = 0; i < jsonArray.size(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            String ip = jsonObject.getStr("ip");
            String port = jsonObject.getStr("port");
            String url = jsonObject.getStr("url");
            String title = jsonObject.getStr("title");
            String city = jsonObject.getStr("city");
            String operator = jsonObject.getStr("operator");
            String component = jsonObject.getStr("component");
            String service = jsonObject.getStr("service");
            String protocol = jsonObject.getStr("protocol");
            String statusCode = jsonObject.getStr("status_code");
            // company格式 ["111"]
            String company = jsonObject.getStr("company");
            // 删除company前2个字符和后2个字符
            if (company != null && company.length() > 4) {
                company = company.substring(2, company.length() - 2);
            }
            // tags处理
            JSONArray tags = jsonObject.getJSONArray("tags");
            StringBuilder tag = new StringBuilder();
            if (tags != null) {
                for (int j = 0; j < tags.size(); j++) {
                    tag.append(tags.getStr(j)).append(",");
                }
                if (tag.length() > 0) {
                    tag.deleteCharAt(tag.length() - 1);
                }
            }
            tableModel.addRow(new Object[]{i + 1, ip, port, url, title, component, service, protocol, city, operator, statusCode, company, tag.toString()});
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

    private void resetTableRowsApk(JSONArray jsonArray, DefaultTableModel tableModel, JPanel pagingPanel, int currentPage, int totalPage, int sizePage, PageState pageState) {
        tableModel.setRowCount(0);
        for (int i = 0; i < jsonArray.size(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            String title = jsonObject.getStr("title");
            String type = jsonObject.getStr("type");
            String company = jsonObject.getStr("company");
            String group = jsonObject.getStr("group");
            
            // 合并company和group
            String companyGroup = "";
            if (company != null && !company.isEmpty()) {
                companyGroup = company;
            }
            if (group != null && !group.isEmpty() && !group.equals(company)) {
                if (!companyGroup.isEmpty()) {
                    companyGroup += " / ";
                }
                companyGroup += group;
            }
            
            // 获取msg对象
            JSONObject msgObj = jsonObject.getJSONObject("msg");
            String id = "";
            String introduction = "";
            
            if (msgObj != null) {
                // 根据类型获取不同的ID
                if ("微信公众号".equals(type) || "微信小程序".equals(type)) {
                    // 微信公众号/小程序：获取wechat_id
                    id = msgObj.getStr("wechat_id");
                    if (id == null || id.isEmpty()) {
                        id = msgObj.getStr("wechat_fakeid");
                    }
                } else if ("iOS".equals(type) || "Android".equals(type)) {
                    // iOS/Android：获取app_id
                    id = msgObj.getStr("app_id");
                }
                
                // 获取简介
                introduction = msgObj.getStr("introduction");
            }
            
            tableModel.addRow(new Object[]{i + 1, title, type, id, companyGroup, introduction});
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

    private void resetTableRowsSiteForPaging(JSONArray jsonArray, DefaultTableModel tableModel, JPanel pagingPanel, int currentPage, int totalPage, int sizePage, PageState pageState, JTable table) {
        tableModel.setRowCount(0);
        for (int i = 0; i < jsonArray.size(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            String ip = jsonObject.getStr("ip");
            String port = jsonObject.getStr("port");
            String url = jsonObject.getStr("url");
            String title = jsonObject.getStr("title");
            String city = jsonObject.getStr("city");
            String operator = jsonObject.getStr("operator");
            String component = jsonObject.getStr("component");
            String service = jsonObject.getStr("service");
            String protocol = jsonObject.getStr("protocol");
            String statusCode = jsonObject.getStr("status_code");
            // company格式 ["111"]
            String company = jsonObject.getStr("company");
            // 删除company前2个字符和后2个字符
            if (company != null && company.length() > 4) {
                company = company.substring(2, company.length() - 2);
            }
            // tags处理
            JSONArray tags = jsonObject.getJSONArray("tags");
            StringBuilder tag = new StringBuilder();
            if (tags != null) {
                for (int j = 0; j < tags.size(); j++) {
                    tag.append(tags.getStr(j)).append(",");
                }
                if (tag.length() > 0) {
                    tag.deleteCharAt(tag.length() - 1);
                }
            }
            tableModel.addRow(new Object[]{i + 1, ip, port, url, title, component, service, protocol, city, operator, statusCode, company, tag.toString()});
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

    private void resetTableRowsApkForPaging(JSONArray jsonArray, DefaultTableModel tableModel, JPanel pagingPanel, int currentPage, int totalPage, int sizePage, PageState pageState, JTable table) {
        tableModel.setRowCount(0);
        for (int i = 0; i < jsonArray.size(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            String title = jsonObject.getStr("title");
            String type = jsonObject.getStr("type");
            String company = jsonObject.getStr("company");
            String group = jsonObject.getStr("group");
            
            // 合并company和group
            String companyGroup = "";
            if (company != null && !company.isEmpty()) {
                companyGroup = company;
            }
            if (group != null && !group.isEmpty() && !group.equals(company)) {
                if (!companyGroup.isEmpty()) {
                    companyGroup += " / ";
                }
                companyGroup += group;
            }
            
            // 获取msg对象
            JSONObject msgObj = jsonObject.getJSONObject("msg");
            String id = "";
            String introduction = "";
            
            if (msgObj != null) {
                // 根据类型获取不同的ID
                if ("微信公众号".equals(type) || "微信小程序".equals(type)) {
                    // 微信公众号/小程序：获取wechat_id
                    id = msgObj.getStr("wechat_id");
                    if (id == null || id.isEmpty()) {
                        id = msgObj.getStr("wechat_fakeid");
                    }
                } else if ("iOS".equals(type) || "Android".equals(type)) {
                    // iOS/Android：获取app_id
                    id = msgObj.getStr("app_id");
                }
                
                // 获取简介
                introduction = msgObj.getStr("introduction");
            }
            
            tableModel.addRow(new Object[]{i + 1, title, type, id, companyGroup, introduction});
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


    /**
     * 添加欢迎面板
     */
    private void addWelcomePanel() {
        JPanel welcomePanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(10, 10, 10, 10);

        JLabel welcomeLabel = new JLabel("欢迎使用搜索引擎");
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
    @Override
    public String getTitle() {
        return "0zone";
    }

    @Override
    public FlatSVGIcon getIcon() {
        return null;
    }

    @Override
    public String getTips() {
        return "Zone0攻击面搜索引擎";
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
            // 从后向前遍历，避免索引变化问题
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
                    // 左键点击时切换到对应标签页
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
