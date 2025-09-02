package org.jiu.ui.searchengine;

import java.nio.charset.StandardCharsets;
import java.util.*;

import cn.hutool.core.date.DateUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.icons.FlatSearchIcon;
import org.jiu.core.HunterCore;
import org.jiu.core.ShodanCore;
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
import java.net.URLEncoder;
import java.util.concurrent.CompletableFuture;

public class HunterSearchEngine extends JPanel implements SearchEngine {
    private static JComboBox searchTypeComboBox = new JComboBox();
    private static JComboBox typeComboBox = new JComboBox();
    private static JComboBox timeComboBox = new JComboBox();
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
    private JButton predBtn = new JButton("上一页");
    private JButton nextBtn = new JButton("下一页");
    private JButton searchBtn = new JButton("搜索");
    private JButton pageCurrentLabel = new JButton("1");
    private JButton pageTotalLabel = new JButton("总页");
    private JButton sizeLabel = new JButton("总数");
    private int currentPage = 1;
    private int totalPage = 0;
    private int sizePage = 0; // 查询总数量
    private int type = 3; // 资产类型，1代表”web资产“，2代表”非web资产“，3代表”全部“
    private String start_time;
    private String end_time;
    private JTable table;
    private JPopupMenu popupMenu; // 表格右键组
    private DefaultTableModel tableModel;
    private TableRowSorter<DefaultTableModel> sorter;
    private JSONObject jsonObject;

    public HunterSearchEngine() {
        this.setLayout(new BorderLayout());
        initToolBar();
        initTable();
    }

    private void initTable() {
        table = new JTable();
        tableModel = new DefaultTableModel() {
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
                    popupMenu = createPopupMenu();
                    popupMenu.show(table, e.getX(), e.getY());
                }
            }
        });

        this.add(scrollPane, BorderLayout.CENTER);
    }

    private JPopupMenu createPopupMenu() {
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
        // 通过浏览器打开当前选中行第三列数据
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

        // 复制选中行的url
        JMenuItem copyUrlItem = new JMenuItem("复制选中行的url");
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
                // 检查是否有有效内容需要复制
                if (stringBuilder.length() > 0) {
                    StringSelection stringSelection = new StringSelection(stringBuilder.toString());
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stringSelection, null);
                }
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
                ShodanPortSearchEngine.inputArea.setText("");
                ShodanPortSearchEngine.inputArea.setText(String.join("\n", ips));
                ShodanPortSearchEngine.searchBtn.doClick();
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

    private void initToolBar() {

        JToolBar toolBar = new JToolBar();
        inputField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Hunter Search... & Enter");
        inputField.putClientProperty(FlatClientProperties.TEXT_FIELD_TRAILING_ICON, new FlatSearchIcon());
        searchBtn.setText("搜索");
        statusBtn.setToolTipText("搜素状态提示灯");
        searchTypeComboBox.setModel(new DefaultComboBoxModel(new String[]{"custom", "domain", "ip"}));
        typeComboBox.setModel(new DefaultComboBoxModel(new String[]{"全部", "web", "非web"}));
        timeComboBox.setModel(new DefaultComboBoxModel(new String[]{"一年", "半年", "一个月"}));
        String[] values = new String[]{"全选", "status_code", "web_title", "domain", "protocol", "icp", "company", "number", "city", "updated_at", "isp"};
        comboxstatus = new MultiComboBox(values);

        predBtn.setSelected(true);
        pageCurrentLabel.setEnabled(false);
        pageCurrentLabel.setSelected(true);
        pageCurrentLabel.setToolTipText("当前页 " + currentPage);
        pageTotalLabel.setEnabled(false);
        pageTotalLabel.setSelected(true);
        pageTotalLabel.setToolTipText("总页 " + totalPage);
        sizeLabel.setEnabled(false);
        sizeLabel.setSelected(true);
        sizeLabel.setToolTipText("总数 " + sizePage);
        nextBtn.setSelected(true);


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

        // 上一页
        predBtn.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentPage > 1) {
                    search(currentPage - 1);
                }
            }
        });
        // 下一页
        nextBtn.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentPage < totalPage) {
                    search(currentPage + 1);
                }
            }
        });
        String today = DateUtil.today();
        end_time = today;
        start_time = DateUtil.formatDate(DateUtil.offsetMonth(DateUtil.parse(today), -12));
        // 时间计算
        timeComboBox.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String time = (String) timeComboBox.getSelectedItem();

                switch (Objects.requireNonNull(time)) {
                    case "一年":
                        // end_time 是今天 start_time 是一年前
                        end_time = today;
                        start_time = DateUtil.formatDate(DateUtil.offsetMonth(DateUtil.parse(today), -12));
                        break;
                    case "半年":
                        end_time = today;
                        start_time = DateUtil.formatDate(DateUtil.offsetMonth(DateUtil.parse(today), -6));
                        break;
                    case "一个月":
                        end_time = today;
                        // 修改为固定29天前
                        start_time = DateUtil.formatDate(DateUtil.offsetDay(DateUtil.parse(today), -29));
                        break;
                }
            }
        });
        // 资产类型
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


        // 添加组件
        toolBar.add(inputField);
        toolBar.add(searchBtn);
        toolBar.addSeparator();
        toolBar.add(searchTypeComboBox);
        toolBar.add(typeComboBox);
        toolBar.add(timeComboBox);
        toolBar.add(comboxstatus);
        toolBar.addSeparator();
        toolBar.add(statusBtn);
        toolBar.add(predBtn);
        toolBar.add(pageCurrentLabel);
        toolBar.add(pageTotalLabel);
        toolBar.add(sizeLabel);
        toolBar.add(nextBtn);
        this.add(toolBar, BorderLayout.NORTH);
    }

    private void search(int p) {
        int i = searchTypeComboBox.getSelectedIndex();
        String data = "";
        if (i == 0) {
            data = inputField.getText();
        } else if (i == 1) {
            data = "domain.suffix=\"" + inputField.getText() + "\"";
        } else if (i == 2) {
            data = "ip=\"" + inputField.getText() + "\"";
        }

        String qbase64 = Base64.getUrlEncoder().encodeToString(data.getBytes(StandardCharsets.UTF_8));
        new Thread(() -> {
            try {
                jsonObject = JSONUtil.parseObj(HunterCore.getData(qbase64, p, 100, type, start_time, end_time));
                JSONObject jsonObject1 = jsonObject.getJSONObject("data");
                sizePage = jsonObject1.getInt("total");
                JSONArray results = jsonObject1.getJSONArray("arr");
                if (results != null) {
                    currentPage = p;    // 当前页
                    totalPage = (int) Math.ceil((double) sizePage / 100); // 总页数
                    resetTableRows(results);
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, jsonObject.getStr("message"));
            }

        }).start();

    }

    /**
     * 结果填充表格
     *
     * @param jsonArray
     */
    private void resetTableRows(JSONArray jsonArray) {
        Object[] selectedValues = comboxstatus.getSelectedValues();
        Object[] originalArray = new Object[]{"ip", "url", "port"};
        Object[] result = new Object[]{};
        // 根据selectedValues的值来获取对应的jsonObject的值
        if (selectedValues.length == 0) {
            result = new Object[]{"ip", "url", "port", "web_title", "domain", "icp", "company", "city"};
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
        table.setModel(tableModel);
        sorter = new TableRowSorter<>(tableModel);
        table.setRowSorter(sorter);
        tableModel.setRowCount(0);
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
        pageCurrentLabel.setText(String.valueOf(currentPage)); // 显示当前页
        pageTotalLabel.setText(String.valueOf(totalPage)); // 显示总页数
        sizeLabel.setText(String.valueOf(sizePage)); // 显示总数量
        pageCurrentLabel.setToolTipText("当前页 " + currentPage);
        pageTotalLabel.setToolTipText("总页 " + totalPage);
        sizeLabel.setToolTipText("总数 " + sizePage);
    }

    @Override
    public String getTitle() {
        return "hunter";
    }

    @Override
    public FlatSVGIcon getIcon() {
        return null;
    }

    @Override
    public String getTips() {
        return "hunter搜索引擎";
    }
}
