package org.jiu.ui.searchengine;

import cn.hutool.core.codec.Base64;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.icons.FlatSearchIcon;
import org.jiu.core.DaydaymapCore;
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
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Author Xm17
 * @Date 2024-06-02 21:52
 */
public class DaydaymapEngine extends JPanel implements SearchEngine {
    private static JComboBox searchTypeComboBox = new JComboBox();
    private static MultiComboBox comboxstatus;
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
            "Company",
            "City"
    };
    private JButton searchBtn = new JButton("搜索");
    private JTabbedPane resultsTabbedPane; // 用于显示多个搜索结果的标签页
    private AtomicInteger tabCounter = new AtomicInteger(0); // 标签页计数器

    public DaydaymapEngine() {
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


        // telnet端口测试
        JMenuItem telnetItem = new JMenuItem("telnet端口测试");
        telnetItem.addActionListener(e -> {
            int[] selectedRows = table.getSelectedRows();
            if (selectedRows.length == 0) {
                JOptionPane.showMessageDialog(null, "请先选择数据");
            } else {
                for (int selectedRow : selectedRows) {
                    String ip = (String) table.getValueAt(selectedRow, 1);
                    String port = (String) table.getValueAt(selectedRow, 2);
                    try {
                        TelnetUtils.telnet(ip, port);
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                }
            }
        });

        // 复制选中行的ip
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
        popupMenu.add(telnetItem);
        popupMenu.add(sendToShodanItem);
        return popupMenu;
    }


    private void initToolBar() {
        JToolBar toolBar = new JToolBar();
        inputField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "DayDayMap Search... & Enter");
        inputField.putClientProperty(FlatClientProperties.TEXT_FIELD_TRAILING_ICON, new FlatSearchIcon());
        searchBtn.setText("搜索");
        searchBtn.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        searchBtn.setPreferredSize(new Dimension(80, 25));
        searchBtn.setBackground(new Color(0, 123, 255));
        searchBtn.setForeground(Color.WHITE);
        searchBtn.setFocusPainted(false);
        statusBtn.setToolTipText("搜索状态提示灯");
        searchTypeComboBox.setModel(new DefaultComboBoxModel(new String[]{"custom", "domain", "ip"}));
        String[] values = new String[]{"全选", "header", "server", "service", "tags", "cert", "icp_reg_name"};
        comboxstatus = new MultiComboBox(values);

        // 回车搜索事件
        inputField.registerKeyboardAction(e -> {
            search(1);
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, false), JComponent.WHEN_FOCUSED);

        // 搜索按钮搜索事件
        searchBtn.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                search(1);
            }
        });

        // 添加组件
        toolBar.add(inputField);
        toolBar.add(searchBtn);
        toolBar.addSeparator();
        toolBar.add(searchTypeComboBox);
        toolBar.add(comboxstatus);
        toolBar.addSeparator();
        toolBar.add(statusBtn);
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
        
        // 为标签页添加右键菜单
        addTabPopupMenu(tabWithCloseButton);
        
        resultsTabbedPane.setTabComponentAt(resultsTabbedPane.getTabCount() - 1, tabWithCloseButton); // 然后设置自定义组件
        resultsTabbedPane.setSelectedIndex(resultsTabbedPane.getTabCount() - 1); // 选择新标签页

        // 获取当前标签页上的表格和模型
        DefaultTableModel tableModel = (DefaultTableModel) newTable.getModel();

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
                JSONObject responseJson = JSONUtil.parseObj(DaydaymapCore.getData(qbase64, p, 100));
                JSONObject jsonObject1 = responseJson.getJSONObject("data");
                int sizePage = jsonObject1.getInt("total");
                JSONArray results = jsonObject1.getJSONArray("list");
                if (results != null) {
                    int currentPage = p;    // 当前页
                    int totalPage = (int) Math.ceil((double) sizePage / 100); // 总页数
                    SwingUtilities.invokeLater(() -> {
                        resetTableRows(results, tableModel, pagingPanel, currentPage, totalPage, sizePage, pageState);
                    });
                } else {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(null, responseJson.getStr("msg"));
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
                JSONObject errorJson = JSONUtil.parseObj("{\"msg\":\"请求失败: " + e.getMessage() + "\"}");
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(null, errorJson.getStr("msg"));
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
        String data = searchData;
        int i = searchTypeComboBox.getSelectedIndex();
        if (i == 1) {
            data = "domain=\"" + searchData + "\"";
        } else if (i == 2) {
            data = "ip=\"" + searchData + "\"";
        }
        
        String qbase64 = Base64.encode(data);
        
        new Thread(() -> {
            try {
                JSONObject responseJson = JSONUtil.parseObj(DaydaymapCore.getData(qbase64, page, 100));
                JSONObject jsonObject1 = responseJson.getJSONObject("data");
                int sizePage = jsonObject1.getInt("total");
                JSONArray results = jsonObject1.getJSONArray("list");
                if (results != null) {
                    SwingUtilities.invokeLater(() -> {
                        DefaultTableModel tableModel = (DefaultTableModel) table.getModel();
                        resetTableRowsForPaging(results, tableModel, pagingPanel, page, totalPages, sizePage, pageState, table);
                        currentPageRef[0] = page;
                        totalPagesRef[0] = (int) Math.ceil((double) sizePage / 100);
                    });
                } else {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(null, responseJson.getStr("msg"));
                    });
                }
            } catch (Exception e) {
                JSONObject errorJson = JSONUtil.parseObj("{\"msg\":\"请求失败: " + e.getMessage() + "\"}");
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(null, errorJson.getStr("msg"));
                });
            }
        }).start();
    }
    
    /**
     * 为分页搜索重置表格行
     */
    private void resetTableRowsForPaging(JSONArray jsonArray, DefaultTableModel tableModel, JPanel pagingPanel, int currentPage, int totalPage, int sizePage, PageState pageState, JTable table) {
        // 清空现有数据
        tableModel.setRowCount(0);

        Object[] selectedValues = comboxstatus.getSelectedValues();
        Object[] originalArray = new Object[]{"ip", "port", "domain", "title"};
        Object[] result = new Object[]{};
        // 根据selectedValues的值来获取对应的jsonObject的值
        if (selectedValues.length == 0) {
            result = new Object[]{"ip", "port", "domain", "title"};
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
        if (jsonArray != null) {
            for (Object obj : jsonArray) {
                JSONObject jsonObject = (JSONObject) obj;
                String[] values = new String[]{};
                ArrayList<String> tempList = new ArrayList<>(Arrays.asList(values));
                // 给vuales赋值
                tempList.add(String.valueOf(num));
                // 给selectedValues插入值

                for (Object o : result) {
                    tempList.add(jsonObject.getStr((String) o));
                }
                values = tempList.toArray(new String[0]);
                tableModel.addRow(values);
                num++;
            }
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
            pageCurrentLabel.setText(String.valueOf(currentPage)); // 显示当前页
            pageCurrentLabel.setToolTipText("当前页 " + currentPage);
        }
        if (pageTotalLabel != null) {
            pageTotalLabel.setText(String.valueOf(totalPage)); // 显示总页数
            pageTotalLabel.setToolTipText("总页 " + totalPage);
        }
        if (sizeLabel != null) {
            sizeLabel.setText(String.valueOf(sizePage)); // 显示总数量
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
        // 清空现有数据
        tableModel.setRowCount(0);
        
        Object[] selectedValues = comboxstatus.getSelectedValues();
        Object[] originalArray = new Object[]{"ip", "port", "domain", "title"};
        Object[] result = new Object[]{};
        // 根据selectedValues的值来获取对应的jsonObject的值
        if (selectedValues.length == 0) {
            result = new Object[]{"ip", "port", "domain", "title"};
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
        if (jsonArray != null) {
            for (Object obj : jsonArray) {
                JSONObject jsonObject = (JSONObject) obj;
                String[] values = new String[]{};
                ArrayList<String> tempList = new ArrayList<>(Arrays.asList(values));
                // 给vuales赋值
                tempList.add(String.valueOf(num));
                // 给selectedValues插入值

                for (Object o : result) {
                    tempList.add(jsonObject.getStr((String) o));
                }
                values = tempList.toArray(new String[0]);
                tableModel.addRow(values);
                num++;
            }
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
            pageCurrentLabel.setText(String.valueOf(currentPage)); // 显示当前页
            pageCurrentLabel.setToolTipText("当前页 " + currentPage);
        }
        if (pageTotalLabel != null) {
            pageTotalLabel.setText(String.valueOf(totalPage)); // 显示总页数
            pageTotalLabel.setToolTipText("总页 " + totalPage);
        }
        if (sizeLabel != null) {
            sizeLabel.setText(String.valueOf(sizePage)); // 显示总数量
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
        return "daydaymap";
    }

    @Override
    public FlatSVGIcon getIcon() {
        return null;
    }

    @Override
    public String getTips() {
        return "daydaymap搜索引擎";
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
