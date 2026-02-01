package org.jiu.ui.searchengine;

import cn.hutool.core.codec.Base64;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.icons.FlatSearchIcon;
import org.jiu.core.FofaCore;
import org.jiu.core.ShodanCore;
import org.jiu.ui.SearchPanel;
import org.jiu.ui.component.MultiComboBox;
import org.jiu.utils.TelnetUtils;

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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 分页状态Holder类的包装，用于在lambda中修改final变量
 */
class PageStateHolder {
    PageState state;
    PageStateHolder(PageState state) {
        this.state = state;
    }
}

public class FofaSearchEngine extends JPanel implements SearchEngine {
    private static JComboBox searchTypeComboBox = new JComboBox();
    private final JTextField inputField = new JTextField();
    private final JButton statusBtn = new JButton();
    private final String[] columnNames = {
            "#",
            "IP",
            "URL",
            "Port",
            "Title",
            "Domain",
            "ICP",
            "City"
    };
    private JButton searchBtn = new JButton("搜索");
    private MultiComboBox comboxstatus;
    private JTabbedPane resultsTabbedPane; // 用于显示多个搜索结果的标签页
    private AtomicInteger tabCounter = new AtomicInteger(0); // 标签页计数器
    private String fields = "ip,host,port,title,domain,icp,city";

    public FofaSearchEngine() {
        this.setLayout(new BorderLayout());
        System.out.println("FofaSearchEngine: 开始初始化...");
        initToolBar();
        System.out.println("FofaSearchEngine: 工具栏已添加");
        initTabbedPane();
        System.out.println("FofaSearchEngine: 初始化完成，组件数量: " + this.getComponentCount());
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

        JLabel welcomeLabel = new JLabel("欢迎使用FOFA搜索引擎");
        welcomeLabel.setFont(new Font("Dialog", Font.BOLD, 24));
        welcomePanel.add(welcomeLabel, gbc);

        gbc.gridy++;
        JLabel instructionLabel = new JLabel("在上方工具栏中输入搜索关键词，然后按回车或点击搜索按钮");
        instructionLabel.setFont(new Font("Dialog", Font.PLAIN, 14));
        welcomePanel.add(instructionLabel, gbc);

        gbc.gridy++;
        JLabel exampleLabel = new JLabel("示例: domain=\"baidu.com\" 或 ip=\"1.1.1.1\"");
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
        table.getColumn("Port").setCellRenderer(centerRenderer);

        table.getColumn("#").setPreferredWidth(10);
        table.getColumn("Port").setPreferredWidth(10);


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

    /**
     * 工具栏
     */
    private void initToolBar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        
        // 初始化多选框 - 这是关键修复
        String[] values = new String[]{"全选", "title", "domain", "icp", "city", "product", "lastupdatetime"};
        comboxstatus = new MultiComboBox(values);
        
        // 输入框
        inputField.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        inputField.setPreferredSize(new Dimension(300, 25));
        inputField.putClientProperty("JTextField.placeholderText", "输入FOFA搜索语法...");
        
        // 状态按钮
        statusBtn.setPreferredSize(new Dimension(25, 25));
        statusBtn.setBorderPainted(false);
        statusBtn.setContentAreaFilled(false);
        statusBtn.setOpaque(true);
        statusBtn.setBackground(Color.RED);
        statusBtn.setToolTipText("API连接状态");
        
        // 搜索类型选择
        searchTypeComboBox.setModel(new DefaultComboBoxModel(new String[]{"custom", "domain", "ip"}));
        searchTypeComboBox.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        searchTypeComboBox.setPreferredSize(new Dimension(100, 25));
        
        // 多选框
        comboxstatus.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        comboxstatus.setPreferredSize(new Dimension(120, 25));
        
        // 搜索按钮
        searchBtn.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        searchBtn.setPreferredSize(new Dimension(80, 25));
        searchBtn.setBackground(new Color(0, 123, 255));
        searchBtn.setForeground(Color.WHITE);
        searchBtn.setFocusPainted(false);

        // 搜索按钮事件
        searchBtn.addActionListener(e -> search(1));

        // 添加快捷键
        toolBar.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "search");
        toolBar.getActionMap().put("search", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                search(1);
            }
        });
        
        // 组装工具栏
        toolBar.add(inputField);
        toolBar.add(Box.createHorizontalStrut(5));
        toolBar.add(searchBtn);
        toolBar.addSeparator();
        toolBar.add(searchTypeComboBox);
        toolBar.addSeparator();
        toolBar.add(statusBtn);
        toolBar.addSeparator();
        toolBar.add(comboxstatus);
        
        this.add(toolBar, BorderLayout.NORTH);
    }

    private void search(int p) {
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

        Object[] selectedValues = comboxstatus.getSelectedValues();
        Object[] originalArray = new Object[]{"ip", "host", "port"};
        // ip,host,port,title,domain,icp,city
        Object[] result = new Object[]{};
        // 根据selectedValues的值来获取对应的jsonObject的值
        if (selectedValues.length == 0) {
            result = new Object[]{"ip", "host", "port", "title", "domain", "icp", "city"};
        } else {
            // 将new Object[]{"ip","url", "port"}插入到selectedValues的前面
            result = new Object[selectedValues.length + originalArray.length];
            System.arraycopy(originalArray, 0, result, 0, originalArray.length);
            System.arraycopy(selectedValues, 0, result, originalArray.length, selectedValues.length);
        }
        // 将result转换成字符串
        StringBuilder stringBuilder = new StringBuilder();
        for (Object obj : result) {
            stringBuilder.append(obj).append(",");
        }
        stringBuilder.deleteCharAt(stringBuilder.length() - 1);
        fields = stringBuilder.toString();
        int i = searchTypeComboBox.getSelectedIndex();
        String data = "";
        if (i == 0) {
            data = inputField.getText();
        } else if (i == 1) {
            data = "domain=\"" + inputField.getText() + "\"";
        } else if (i == 2) {
            data = "ip=\"" + inputField.getText() + "\"";
        }
        String qbase64 = Base64.encode(data);
        new Thread(() -> {
            try {
                JSONObject responseJson = JSONUtil.parseObj(FofaCore.getData(qbase64, fields, p, 100, false));
                int sizePage = responseJson.getInt("size");
                JSONArray results = responseJson.getJSONArray("results");
                if (results != null) {
                    int currentPage = p;
                    int totalPage = (int) Math.ceil((double) sizePage / 100); // 总页数
                    System.out.println("[FOFA] 第一次搜索完成 - 当前页: " + currentPage + ", 总页: " + totalPage + ", 总数: " + sizePage);
                    System.out.println("[FOFA] pageState对象: " + pageState);
                    SwingUtilities.invokeLater(() -> {
                        resetTableRows(results, tableModel, pagingPanel, currentPage, totalPage, sizePage, pageState);
                    });
                } else {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(null, responseJson.getStr("errmsg"));
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
                JSONObject errorJson = JSONUtil.parseObj("{\"errmsg\":\"请求失败: " + e.getMessage() + "\"}");
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(null, errorJson.getStr("errmsg"));
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
     * 创建分页面板
     */
    private JPanel createPagingPanel(JTable table, String searchData, String tabTitle, PageState pageState) {
        System.out.println("[FOFA] >>> 进入createPagingPanel方法");
        System.out.println("[FOFA] pageState: " + pageState);
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
            System.out.println("=== [FOFA] 点击上一页按钮 ===");
            System.out.println("当前页码: " + pageState.currentPage[0]);
            if (pageState.currentPage[0] > 1) {
                int newPage = pageState.currentPage[0] - 1;
                System.out.println("准备翻到页码: " + newPage);
                performPagingSearch(table, pageState.lastQuery[0], newPage, pageState.totalPages[0], pageState.currentPage, pageState.totalPages, pagingPanel, pageState);
            } else {
                System.out.println("已经是第一页，无法上一页");
            }
        });

        nextBtn.addActionListener(e -> {
            System.out.println("=== [FOFA] 点击下一页按钮 ===");
            System.out.println("当前页码: " + pageState.currentPage[0]);
            System.out.println("总页码: " + pageState.totalPages[0]);
            if (pageState.currentPage[0] < pageState.totalPages[0]) {
                int newPage = pageState.currentPage[0] + 1;
                System.out.println("准备翻到页码: " + newPage);
                performPagingSearch(table, pageState.lastQuery[0], newPage, pageState.totalPages[0], pageState.currentPage, pageState.totalPages, pagingPanel, pageState);
            } else {
                System.out.println("已经是最后一页，无法下一页");
            }
        });

        return pagingPanel;
    }

    /**
     * 执行分页搜索
     */
    private void performPagingSearch(JTable table, String searchData, int page, int totalPages, int[] currentPageRef, int[] totalPagesRef, JPanel pagingPanel, PageState pageState) {
        System.out.println("\n[FOFA] >>> 进入performPagingSearch方法");
        System.out.println("[FOFA] 参数: searchData=" + searchData + ", page=" + page + ", totalPages=" + totalPages);

        Object[] selectedValues = comboxstatus.getSelectedValues();
        Object[] originalArray = new Object[]{"ip", "host", "port"};
        Object[] result = new Object[]{};
        if (selectedValues.length == 0) {
            result = new Object[]{"ip", "host", "port", "title", "domain", "icp", "city"};
        } else {
            result = new Object[selectedValues.length + originalArray.length];
            System.arraycopy(originalArray, 0, result, 0, originalArray.length);
            System.arraycopy(selectedValues, 0, result, originalArray.length, selectedValues.length);
        }
        System.out.println("[FOFA] 查询字段: " + Arrays.toString(result));
        
        StringBuilder stringBuilder = new StringBuilder();
        for (Object obj : result) {
            stringBuilder.append(obj).append(",");
        }
        stringBuilder.deleteCharAt(stringBuilder.length() - 1);
        String fields = stringBuilder.toString();
        
        int i = searchTypeComboBox.getSelectedIndex();
        String data = "";
        if (i == 0) {
            data = searchData;
        } else if (i == 1) {
            data = "domain=\"" + searchData + "\"";
        } else if (i == 2) {
            data = "ip=\"" + searchData + "\"";
        }
        String qbase64 = Base64.encode(data);
        System.out.println("[FOFA] 搜索类型: " + i + ", 搜索内容: " + data);
        System.out.println("[FOFA] 发送HTTP请求到Fofa API...");

        new Thread(() -> {
            try {
                JSONObject responseJson = JSONUtil.parseObj(FofaCore.getData(qbase64, fields, page, 100, false));
                int sizePage = responseJson.getInt("size");
                JSONArray results = responseJson.getJSONArray("results");
                System.out.println("[FOFA] <<< HTTP请求完成");
                System.out.println("[FOFA] 总数量: " + sizePage + ", 数据条数: " + (results != null ? results.size() : 0));

                if (results != null) {
                    System.out.println("[FOFA] 准备更新表格和分页组件...");
                    SwingUtilities.invokeLater(() -> {
                        DefaultTableModel tableModel = (DefaultTableModel) table.getModel();
                        System.out.println("[FOFA] 调用resetTableRowsForPaging更新表格...");
                        resetTableRowsForPaging(results, tableModel, pagingPanel, page, totalPages, sizePage, pageState, table);
                        System.out.println("[FOFA] 更新currentPageRef和totalPagesRef");
                        currentPageRef[0] = page;
                        totalPagesRef[0] = (int) Math.ceil((double) sizePage / 100);
                        System.out.println("[FOFA] 更新完成! 当前页: " + currentPageRef[0] + ", 总页: " + totalPagesRef[0]);
                    });
                } else {
                    System.out.println("[FOFA] ERROR: 搜索结果为空 - " + responseJson.getStr("errmsg"));
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(null, responseJson.getStr("errmsg"));
                    });
                }
            } catch (Exception e) {
                System.out.println("[FOFA] ERROR: " + e.getMessage());
                JSONObject errorJson = JSONUtil.parseObj("{\"errmsg\":\"请求失败: " + e.getMessage() + "\"}");
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(null, errorJson.getStr("errmsg"));
                });
            }
        }).start();
    }
    
    /**
     * 为分页搜索重置表格行
     */
    private void resetTableRowsForPaging(JSONArray jsonArray, DefaultTableModel tableModel, JPanel pagingPanel, int currentPage, int totalPage, int sizePage, PageState pageState, JTable table) {
        System.out.println("\n[FOFA] >>> 进入resetTableRowsForPaging方法");
        System.out.println("[FOFA] 参数: currentPage=" + currentPage + ", totalPage=" + totalPage + ", sizePage=" + sizePage);
        System.out.println("[FOFA] 分页面板组件数量: " + pagingPanel.getComponentCount());

        // 清空现有数据
        tableModel.setRowCount(0);
        System.out.println("[FOFA] 已清空表格数据");

        Object[] selectedValues = comboxstatus.getSelectedValues();
        Object[] originalArray = new Object[]{"ip", "url", "port"};
        Object[] result = new Object[]{};
        if (selectedValues.length == 0) {
            result = new Object[]{"ip", "host", "port", "title", "domain", "icp", "city"};
        } else {
            result = new Object[selectedValues.length + originalArray.length];
            System.arraycopy(originalArray, 0, result, 0, originalArray.length);
            System.arraycopy(selectedValues, 0, result, originalArray.length, selectedValues.length);
        }

        // 给result最开始插入一个#号
        Object newValueId = "#";
        Object[] newArray = new Object[result.length + 1];
        newArray[0] = newValueId;
        System.arraycopy(result, 0, newArray, 1, result.length);

        tableModel.setColumnIdentifiers(newArray);
        System.out.println("[FOFA] 已设置表格列: " + Arrays.toString(newArray));

        int num = 1;
        System.out.println("[FOFA] 开始填充 " + jsonArray.size() + " 行数据");
        for (Object obj : jsonArray) {
            JSONArray array = (JSONArray) obj;
            array.add(0, String.valueOf(num));
            String url = (String) array.get(2);
            if (!url.startsWith("http")){
                array.set(2,"http://"+url);
            }
            tableModel.addRow(array.toArray());
            num++;
        }
        System.out.println("[FOFA] 表格数据填充完成");

        // 更新分页组件
        System.out.println("[FOFA] 开始查找分页按钮...");
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
                        System.out.println("[FOFA] 找到pageCurrentLabel按钮: " + btn.getText());
                    } else if (name.equals("pageTotalLabel")) {
                        pageTotalLabel = btn;
                        System.out.println("[FOFA] 找到pageTotalLabel按钮: " + btn.getText());
                    } else if (name.equals("sizeLabel")) {
                        sizeLabel = btn;
                        System.out.println("[FOFA] 找到sizeLabel按钮: " + btn.getText());
                    }
                }
            }
        }

        System.out.println("[FOFA] 按钮查找结果: pageCurrentLabel=" + (pageCurrentLabel != null) +
                           ", pageTotalLabel=" + (pageTotalLabel != null) +
                           ", sizeLabel=" + (sizeLabel != null));

        if (pageCurrentLabel != null) {
            String oldText = pageCurrentLabel.getText();
            pageCurrentLabel.setText(String.valueOf(currentPage)); // 当前页
            pageCurrentLabel.setToolTipText("当前页 " + currentPage);
            System.out.println("[FOFA] 更新pageCurrentLabel: " + oldText + " -> " + currentPage);
        } else {
            System.out.println("[FOFA] ERROR: pageCurrentLabel按钮未找到!");
        }

        if (pageTotalLabel != null) {
            String oldText = pageTotalLabel.getText();
            pageTotalLabel.setText(String.valueOf(totalPage)); // 总页数
            pageTotalLabel.setToolTipText("总页 " + totalPage);
            System.out.println("[FOFA] 更新pageTotalLabel: " + oldText + " -> " + totalPage);
        } else {
            System.out.println("[FOFA] ERROR: pageTotalLabel按钮未找到!");
        }

        if (sizeLabel != null) {
            String oldText = sizeLabel.getText();
            sizeLabel.setText(String.valueOf(sizePage)); // 总数量
            sizeLabel.setToolTipText("总数 " + sizePage);

        // 更新分页状态
        if (pageState != null) {
            pageState.currentPage[0] = currentPage;
            pageState.totalPages[0] = totalPage;
        }
            System.out.println("[FOFA] 更新sizeLabel: " + oldText + " -> " + sizePage);
        } else {
            System.out.println("[FOFA] ERROR: sizeLabel按钮未找到!");
        }

        // 滚动到顶部
        SwingUtilities.invokeLater(() -> {
            JScrollPane scrollPane = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, table);
            if (scrollPane != null) {
                scrollPane.getVerticalScrollBar().setValue(0);
            }
        });

        System.out.println("[FOFA] <<< resetTableRowsForPaging方法执行完成\n");
    }

    private void resetTableRows(JSONArray jsonArray, DefaultTableModel tableModel, JPanel pagingPanel, int currentPage, int totalPage, int sizePage, PageState pageState) {
        System.out.println("[FOFA] >>> 进入resetTableRows方法 (第一次搜索)");
        System.out.println("[FOFA] 更新分页状态: currentPage=" + currentPage + ", totalPage=" + totalPage);

        // 清空现有数据
        tableModel.setRowCount(0);
        
        Object[] selectedValues = comboxstatus.getSelectedValues();
        Object[] originalArray = new Object[]{"ip", "url", "port"};
        Object[] result = new Object[]{};
        // 根据selectedValues的值来获取对应的jsonObject的值
        if (selectedValues.length == 0) {
            result = new Object[]{"ip", "host", "port", "title", "domain", "icp", "city"};
        } else {
            // 将new Object[]{"ip","url", "port"}插入到selectedValues的前面
            result = new Object[selectedValues.length + originalArray.length];
            System.arraycopy(originalArray, 0, result, 0, originalArray.length);
            System.arraycopy(selectedValues, 0, result, originalArray.length, selectedValues.length);
        }

        // 给result最开始插入一个#号
        Object newValueId = "#";
        Object[] newArray = new Object[result.length + 1];
        newArray[0] = newValueId;
        System.arraycopy(result, 0, newArray, 1, result.length);

        tableModel.setColumnIdentifiers(newArray);

        int num = 1;
        for (Object obj : jsonArray) {
            JSONArray array = (JSONArray) obj;
            array.add(0, String.valueOf(num));
            String url = (String) array.get(2);
            if (!url.startsWith("http")){
                array.set(2,"http://"+url);
            }
            tableModel.addRow(array.toArray());
            num++;
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
            pageCurrentLabel.setText(String.valueOf(currentPage)); // 当前页
            pageCurrentLabel.setToolTipText("当前页 " + currentPage);
        }
        if (pageTotalLabel != null) {
            pageTotalLabel.setText(String.valueOf(totalPage)); // 总页数
            pageTotalLabel.setToolTipText("总页 " + totalPage);
        }
        if (sizeLabel != null) {
            sizeLabel.setText(String.valueOf(sizePage)); // 总数量
            sizeLabel.setToolTipText("总数 " + sizePage);

        // 更新分页状态
        if (pageState != null) {
            pageState.currentPage[0] = currentPage;
            pageState.totalPages[0] = totalPage;
        }
        }

        // 更新分页状态数组
        if (pageState != null) {
            pageState.currentPage[0] = currentPage;
            pageState.totalPages[0] = totalPage;
            System.out.println("[FOFA] 分页状态已更新: pageState.currentPage=" + pageState.currentPage[0] + ", pageState.totalPages=" + pageState.totalPages[0]);
        } else {
            System.out.println("[FOFA] WARNING: pageState为null");
        }
        System.out.println("[FOFA] <<< resetTableRows方法执行完成\n");
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

        // 打开当前行url
        JMenuItem openUrlItem = new JMenuItem("打开当前行url");
        openUrlItem.addActionListener(e -> {
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
        // telnet端口测试
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
        // 复制选中行ip
        JMenuItem copyIpItem = new JMenuItem("复制选中行ip");
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

        // 复制选中行url
        JMenuItem copyUrlItem = new JMenuItem("复制选中行url");
        copyUrlItem.addActionListener(e -> {
            int[] selectedRows = table.getSelectedRows();
            if (selectedRows.length == 0) {
                JOptionPane.showMessageDialog(null, "请先选择数据");
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                int validCount = 0;
                for (int selectedRow : selectedRows) {
                    // 获取单元格的值
                    Object cellValue = table.getValueAt(selectedRow, 2);
                    // 检查值是否为空或空字符串
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
        popupMenu.add(copyIpItem);
        popupMenu.add(copyUrlItem);
        popupMenu.add(openUrlItem);
        popupMenu.add(telnetItem);
        popupMenu.add(sendToShodanItem);
        return popupMenu;
    }

    @Override
    public String getTitle() {
        return "fofa";
    }

    @Override
    public FlatSVGIcon getIcon() {
        return null;
    }

    @Override
    public String getTips() {
        return "fofa空间搜索引擎";
    }
}
