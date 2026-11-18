package org.jiu.ui.searchengine;

import cn.hutool.core.date.DateUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import org.jiu.core.HunterCore;
import org.jiu.ui.InitUI;
import org.jiu.ui.SearchPanel;
import org.jiu.ui.component.Icons;
import org.jiu.ui.component.MultiComboBox;
import org.jiu.utils.TelnetUtils;
import org.jiu.utils.UIUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Hunter 测绘搜索引擎面板：采集资产并以多标签页表格展示，支持资产类型、时间范围、字段筛选与分页。
 */
public class HunterSearchEngine extends JPanel implements SearchEngine {
    private static JComboBox searchTypeComboBox = new JComboBox();
    private static JComboBox typeComboBox = new JComboBox();
    private static JComboBox timeComboBox = new JComboBox();
    private static MultiComboBox comboxstatus;
    private final JTextField inputField = new JTextField();
    private final JLabel statusBtn = new JLabel();
    private final String[] columnNames = {
            "#",
            "IP",
            "URL",
            "Port",
            "Title",
            "Domain",
            "ICP",
            "Company",
            "City",
            "Province",
            "Country",
            "ISP",
            "Status",
            "Protocol"
    };
    private JButton searchBtn = new JButton("搜索");
    private int type = 3; // 资产类型，1代表”web资产“，2代表”非web资产“，3代表”全部“
    private String start_time;
    private String end_time;
    private JTabbedPane resultsTabbedPane; 
    private AtomicInteger tabCounter = new AtomicInteger(0); 

    public HunterSearchEngine() {
        this.setLayout(new BorderLayout());
        // 默认查询时间范围为最近一年
        String today = DateUtil.today();
        end_time = today;
        start_time = DateUtil.formatDate(DateUtil.offsetMonth(DateUtil.parse(today), -12));
        initToolBar();
        initTabbedPane();
    }


    /**
     * 创建结果标签页容器并加入欢迎页。
     */
    private void initTabbedPane() {
        resultsTabbedPane = new JTabbedPane();
        InitUI.styleTabbedPane(resultsTabbedPane, false);
        add(resultsTabbedPane, BorderLayout.CENTER);
        addWelcomePanel();
    }

    /**
     * 构建欢迎页，提示用户输入 Hunter 语法发起搜索。
     */
    private void addWelcomePanel() {
        JPanel welcomePanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(10, 10, 10, 10);

        JLabel welcomeLabel = new JLabel("欢迎使用Hunter搜索引擎");
        welcomeLabel.setFont(new Font("Dialog", Font.BOLD, 24));
        welcomePanel.add(welcomeLabel, gbc);

        gbc.gridy++;
        JLabel instructionLabel = new JLabel("在上方工具栏中输入搜索关键词，然后按回车或点击搜索按钮");
        instructionLabel.setFont(new Font("Dialog", Font.PLAIN, 14));
        welcomePanel.add(instructionLabel, gbc);

        gbc.gridy++;
        JLabel exampleLabel = new JLabel("示例: domain.suffix=\"baidu.com\"");
        exampleLabel.setFont(new Font("Dialog", Font.ITALIC, 12));
        exampleLabel.setForeground(Color.GRAY);
        welcomePanel.add(exampleLabel, gbc);

        resultsTabbedPane.addTab("欢迎", welcomePanel);
    }

    /**
     * 创建结果表格，设置列渲染并绑定右键菜单。
     */
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
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JTextField.CENTER);
        table.getColumn("#").setCellRenderer(centerRenderer);
        table.getColumn("Port").setCellRenderer(centerRenderer);
        table.getColumn("Status").setCellRenderer(centerRenderer);

        table.getColumn("#").setPreferredWidth(10);
        table.getColumn("Port").setPreferredWidth(10);
        table.getColumn("Status").setPreferredWidth(10);
        table.getColumn("URL").setPreferredWidth(150);


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
     * 构建结果表格右键菜单：复制行/IP/URL、打开 URL、telnet 测试、发送到 Shodan。
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
        JMenuItem openBrowserItem = new JMenuItem("浏览器打开");
        openBrowserItem.addActionListener(e -> {
            int[] selectedRows = table.getSelectedRows();
            if (selectedRows.length == 0) {
                JOptionPane.showMessageDialog(null, "请先选择数据");
            } else {
                for (int selectedRow : selectedRows) {
                    String url = (String) table.getValueAt(selectedRow, 2);
                    try {
                        Desktop.getDesktop().browse(new URI(url));
                    } catch (IOException | URISyntaxException ioException) {
                        ioException.printStackTrace();
                    }
                }
            }
        });

        JMenuItem telnetItem = new JMenuItem("telnet端口测试");
        telnetItem.addActionListener(e -> {
            int[] selectedRows = table.getSelectedRows();
            if (selectedRows.length == 0) {
                JOptionPane.showMessageDialog(null, "请先选择数据");
            } else {
                for (int selectedRow : selectedRows) {
                    String ip = (String) table.getValueAt(selectedRow, 1);
                    String port = (String) table.getValueAt(selectedRow, 3);
                    try {
                        TelnetUtils.telnet(ip, port);
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                }
            }
        });

        JMenuItem copyIpItem = new JMenuItem("复制选中行的ip");
        copyIpItem.addActionListener(e -> {
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

        JMenuItem copyUrlItem = new JMenuItem("复制选中行的url");
        copyUrlItem.addActionListener(e -> {
            int[] selectedRows = table.getSelectedRows();
            if (selectedRows.length == 0) {
                JOptionPane.showMessageDialog(null, "请先选择数据");
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                int validCount = 0;
                for (int selectedRow : selectedRows) {
                    Object cellValue = table.getValueAt(selectedRow, 2);
                    if (cellValue != null && !cellValue.toString().trim().isEmpty()) {
                        if (validCount > 0) {
                            stringBuilder.append("\n");
                        }
                        stringBuilder.append(cellValue);
                        validCount++;
                    }
                }
                if (stringBuilder.length() > 0) {
                    StringSelection stringSelection = new StringSelection(stringBuilder.toString());
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stringSelection, null);
                }
            }
        });
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
                SearchPanel.switchToShodanPort();
            }
        });
        popupMenu.add(copyRowItem);
        popupMenu.add(copyIpItem);
        popupMenu.add(copyUrlItem);
        popupMenu.add(openBrowserItem);
        popupMenu.add(telnetItem);
        popupMenu.add(sendToShodanItem);
        return popupMenu;
    }

    /**
     * 构建顶部工具栏：搜索框、搜索类型/资产类型/时间范围/字段下拉框及搜索与回车事件。
     */
    private void initToolBar() {

        JToolBar toolBar = new JToolBar();
        UIUtils.setupSearchField(inputField, "Hunter Search... & Enter");
        UIUtils.setupSearchButton(searchBtn, "搜索");
        UIUtils.setupStatusLabel(statusBtn);

        searchTypeComboBox.setModel(new DefaultComboBoxModel(new String[]{"custom", "domain", "ip"}));
        UIUtils.setupComboBox(searchTypeComboBox, 100);

        typeComboBox.setModel(new DefaultComboBoxModel(new String[]{"全部", "web", "非web"}));
        UIUtils.setupComboBox(typeComboBox, 100);

        timeComboBox.setModel(new DefaultComboBoxModel(new String[]{"一年", "半年", "一个月"}));
        UIUtils.setupComboBox(timeComboBox, 100);

        String[] values = new String[]{"全选", "status_code", "web_title", "domain", "protocol", "number", "company", "city", "province", "country", "updated_at", "isp", "header_server", "as_org"};
        comboxstatus = new MultiComboBox(values);
        comboxstatus.setFont(UIUtils.TEXT_FONT);
        comboxstatus.setPreferredSize(new Dimension(140, UIUtils.SEARCH_INPUT_HEIGHT));

        String today = DateUtil.today();
        end_time = today;
        start_time = DateUtil.formatDate(DateUtil.offsetMonth(DateUtil.parse(today), -12));
        timeComboBox.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String time = (String) timeComboBox.getSelectedItem();

                switch (Objects.requireNonNull(time)) {
                    case "一年":
                        end_time = today;
                        start_time = DateUtil.formatDate(DateUtil.offsetMonth(DateUtil.parse(today), -12));
                        break;
                    case "半年":
                        end_time = today;
                        start_time = DateUtil.formatDate(DateUtil.offsetMonth(DateUtil.parse(today), -6));
                        break;
                    case "一个月":
                        end_time = today;
                        start_time = DateUtil.formatDate(DateUtil.offsetDay(DateUtil.parse(today), -29));
                        break;
                }
            }
        });
        typeComboBox.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String typeStr = (String) typeComboBox.getSelectedItem();
                assert typeStr != null;
                switch (typeStr) {
                    case "全部":
                        type = 3;
                        break;
                    case "web":
                        type = 1;
                        break;
                    case "非web":
                        type = 2;
                        break;
                }
            }
        });

        // 仅在输入框聚焦时响应回车，避免影响其他面板
        inputField.registerKeyboardAction(e -> {
            search(1);
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, false), JComponent.WHEN_FOCUSED);

        searchBtn.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                search(1);
            }
        });

        toolBar.add(inputField);
        toolBar.add(searchBtn);
        toolBar.addSeparator();
        toolBar.add(searchTypeComboBox);
        toolBar.add(typeComboBox);
        toolBar.add(timeComboBox);
        toolBar.add(comboxstatus);
        toolBar.addSeparator();
        toolBar.add(statusBtn);
        this.add(toolBar, BorderLayout.NORTH);
    }

    /**
     * 发起搜索：新建结果标签页，后台线程拉取数据后回到 EDT 填充表格；无结果或异常时移除空标签页。
     *
     * @param p 页码，从 1 开始
     */
    private void search(int p) {
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

        resultsTabbedPane.addTab(null, new JScrollPane(contentPanel)); 

        JButton closeButton = new JButton("×");
        closeButton.setFont(new Font("Dialog", Font.BOLD, 12));
        closeButton.setPreferredSize(new Dimension(20, 20));
        closeButton.setBorderPainted(false);
        closeButton.setContentAreaFilled(false);

        final JPanel tabWithCloseButton = new JPanel(new BorderLayout());
        tabWithCloseButton.add(new JLabel(tabTitle), BorderLayout.CENTER);
        tabWithCloseButton.add(closeButton, BorderLayout.EAST);

        closeButton.addActionListener(e -> {
            // 标签索引随增删变化，按组件引用动态定位当前页
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

        int i = searchTypeComboBox.getSelectedIndex();
        String data = "";
        if (i == 0) {
            data = inputField.getText();
        } else if (i == 1) {
            data = "domain.suffix=\"" + inputField.getText() + "\"";
        } else if (i == 2) {
            data = "ip=\"" + inputField.getText() + "\"";
        }

        // Hunter 接口要求 search 为 URL 安全 Base64（无填充）编码
        String qbase64 = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(data.getBytes(StandardCharsets.UTF_8));
        new Thread(() -> {
            try {
                JSONObject responseJson = JSONUtil.parseObj(HunterCore.getData(qbase64, p, 100, type, start_time, end_time));
                JSONObject jsonObject1 = responseJson.getJSONObject("data");
                int sizePage = jsonObject1.getInt("total");
                JSONArray results = jsonObject1.getJSONArray("arr");
                if (results != null) {
                    int currentPage = p;    
                    int totalPage = (int) Math.ceil((double) sizePage / 100); 
                    SwingUtilities.invokeLater(() -> {
                        resetTableRows(results, tableModel, pagingPanel, currentPage, totalPage, sizePage, pageState);
                    });
                } else {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(null, responseJson.getStr("message"));
                        // 移除空的标签页
                        for (int idx = 0; idx < resultsTabbedPane.getTabCount(); idx++) {
                            if (resultsTabbedPane.getTabComponentAt(idx) == tabWithCloseButton) {
                                resultsTabbedPane.removeTabAt(idx);
                                break;
                            }
                        }
                    });
                }
            } catch (Exception e) {
                JSONObject errorJson = JSONUtil.parseObj("{\"message\":\"请求失败: " + e.getMessage() + "\"}");
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(null, errorJson.getStr("message"));
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

    /**
     * 创建分页面板，上一页/下一页按钮基于 pageState 触发翻页查询。
     */
    private JPanel createPagingPanel(JTable table, String searchData, String tabTitle, PageState pageState) {
        JPanel pagingPanel = new JPanel();
        pagingPanel.setLayout(new FlowLayout());

        JButton predBtn = new JButton("上一页");
        JButton nextBtn = new JButton("下一页");
        JButton pageCurrentLabel = new JButton("1");
        JButton pageTotalLabel = new JButton("总页");
        JButton sizeLabel = new JButton("总数");

        // 固定 name，供刷新结果时按名称定位这些标签按钮
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
     * 翻到指定页：后台重新查询，回到 EDT 刷新表格与分页状态。
     *
     * @param page 目标页码，从 1 开始
     */
    private void performPagingSearch(JTable table, String searchData, int page, int totalPages, int[] currentPageRef, int[] totalPagesRef, JPanel pagingPanel, PageState pageState) {
        String data = searchData;
        int i = searchTypeComboBox.getSelectedIndex();
        if (i == 1) {
            data = "domain.suffix=\"" + searchData + "\"";
        } else if (i == 2) {
            data = "ip=\"" + searchData + "\"";
        }

        // Hunter 接口要求 search 为 URL 安全 Base64（无填充）编码
        String qbase64 = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(data.getBytes(StandardCharsets.UTF_8));

        new Thread(() -> {
            try {
                JSONObject responseJson = JSONUtil.parseObj(HunterCore.getData(qbase64, page, 100, type, start_time, end_time));
                JSONObject jsonObject1 = responseJson.getJSONObject("data");
                int sizePage = jsonObject1.getInt("total");
                JSONArray results = jsonObject1.getJSONArray("arr");
                if (results != null) {
                    SwingUtilities.invokeLater(() -> {
                        DefaultTableModel tableModel = (DefaultTableModel) table.getModel();
                        resetTableRowsForPaging(results, tableModel, pagingPanel, page, totalPages, sizePage, pageState, table);
                        currentPageRef[0] = page;
                        totalPagesRef[0] = (int) Math.ceil((double) sizePage / 100);
                    });
                } else {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(null, responseJson.getStr("message"));
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

    /**
     * 用分页结果重建表格列与行并刷新分页标签，需在 EDT 调用。
     */
    private void resetTableRowsForPaging(JSONArray jsonArray, DefaultTableModel tableModel, JPanel pagingPanel, int currentPage, int totalPage, int sizePage, PageState pageState, JTable table) {
        tableModel.setRowCount(0);

        Object[] selectedValues = comboxstatus.getSelectedValues();
        Object[] originalArray = new Object[]{"ip", "url", "port"};
        Object[] result = new Object[]{};
        if (selectedValues.length == 0) {
            result = new Object[]{"ip", "url", "port", "web_title", "domain", "number", "company", "city", "province", "country", "isp", "status_code", "protocol"};
        } else {
            result = new Object[selectedValues.length + originalArray.length];
            System.arraycopy(originalArray, 0, result, 0, originalArray.length);
            System.arraycopy(selectedValues, 0, result, originalArray.length, selectedValues.length);
        }

        Object newValueId = "#";
        Object[] newArray = new Object[result.length + 1];
        newArray[0] = newValueId;
        System.arraycopy(result, 0, newArray, 1, result.length);

        tableModel.setColumnIdentifiers(newArray);

        int num = 1;
        if (jsonArray != null) {
            for (Object obj : jsonArray) {
                JSONObject jsonObject = (JSONObject) obj;
                String[] values = new String[]{};
                ArrayList<String> tempList = new ArrayList<>(Arrays.asList(values));
                tempList.add(String.valueOf(num));

                for (Object o : result) {
                    tempList.add(jsonObject.getStr((String) o));
                }
                values = tempList.toArray(new String[0]);
                tableModel.addRow(values);
                num++;
            }
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
     * 用首次搜索的结果重建表格列与行并刷新分页标签，需在 EDT 调用。
     */
    private void resetTableRows(JSONArray jsonArray, DefaultTableModel tableModel, JPanel pagingPanel, int currentPage, int totalPage, int sizePage, PageState pageState) {
        tableModel.setRowCount(0);

        Object[] selectedValues = comboxstatus.getSelectedValues();
        Object[] originalArray = new Object[]{"ip", "url", "port"};
        Object[] result = new Object[]{};
        if (selectedValues.length == 0) {
            result = new Object[]{"ip", "url", "port", "web_title", "domain", "number", "company", "city", "province", "country", "isp", "status_code", "protocol"};
        } else {
            result = new Object[selectedValues.length + originalArray.length];
            System.arraycopy(originalArray, 0, result, 0, originalArray.length);
            System.arraycopy(selectedValues, 0, result, originalArray.length, selectedValues.length);
        }

        Object newValueId = "#";
        Object[] newArray = new Object[result.length + 1];
        newArray[0] = newValueId;
        System.arraycopy(result, 0, newArray, 1, result.length);

        tableModel.setColumnIdentifiers(newArray);

        int num = 1;
        if (jsonArray != null) {
            for (Object obj : jsonArray) {
                JSONObject jsonObject = (JSONObject) obj;
                String[] values = new String[]{};
                ArrayList<String> tempList = new ArrayList<>(Arrays.asList(values));
                tempList.add(String.valueOf(num));

                for (Object o : result) {
                    tempList.add(jsonObject.getStr((String) o));
                }
                values = tempList.toArray(new String[0]);
                tableModel.addRow(values);
                num++;
            }
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
        return "hunter";
    }

    @Override
    public FlatSVGIcon getIcon() {
        return Icons.create("hunter");
    }

    @Override
    public String getTips() {
        return "hunter搜索引擎";
    }

    /**
     * 为标签页组件绑定右键菜单（关闭当前/其他/所有）与左键切换选中。
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
                    // 自定义标签组件不会自动切换选中，左键需手动定位并选中
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
