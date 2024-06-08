package org.jiu.ui.searchengine;

import cn.hutool.core.codec.Base64;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.icons.FlatSearchIcon;
import org.jiu.core.DaydaymapCore;
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
    private JButton predBtn = new JButton("上一页");
    private JButton nextBtn = new JButton("下一页");
    private JButton searchBtn = new JButton("搜索");
    private JButton pageCurrentLabel = new JButton("1");
    private JButton pageTotalLabel = new JButton("总页");
    private JButton sizeLabel = new JButton("总数");
    private int currentPage = 1;
    private int totalPage = 0;
    private int sizePage = 0; // 查询总数量
    private JTable table;
    private JPopupMenu popupMenu; // 表格右键组
    private DefaultTableModel tableModel;
    private TableRowSorter<DefaultTableModel> sorter;
    private JSONObject jsonObject;

    public DaydaymapEngine() {
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
                for (int selectedRow : selectedRows) {
                    for (int i = 0; i < table.getColumnCount(); i++) {
                        stringBuilder.append(table.getValueAt(selectedRow, i)).append("\t");
                    }
                    stringBuilder.append("\n");
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
                for (int selectedRow : selectedRows) {
                    stringBuilder.append(table.getValueAt(selectedRow, 1)).append("\n");
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
                ShodanPortSearchEngine.inputArea.setText("");
                ShodanPortSearchEngine.inputArea.setText(String.join("\n", ips));
                ShodanPortSearchEngine.searchBtn.doClick();
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
        statusBtn.setToolTipText("搜素状态提示灯");
        searchTypeComboBox.setModel(new DefaultComboBoxModel(new String[]{"custom", "domain", "ip"}));
        String[] values = new String[]{"全选", "header", "server", "service", "tags", "cert", "icp_reg_name"};
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


        // 添加组件
        toolBar.add(inputField);
        toolBar.add(searchBtn);
        toolBar.addSeparator();
        toolBar.add(searchTypeComboBox);
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
            data = "domain=\"" + inputField.getText() + "\"";
        } else if (i == 2) {
            data = "ip=\"" + inputField.getText() + "\"";
        }

        String qbase64 = Base64.encode(data);
        new Thread(() -> {
            try {
                jsonObject = JSONUtil.parseObj(DaydaymapCore.getData(qbase64, p, 100));
                JSONObject jsonObject1 = jsonObject.getJSONObject("data");
                sizePage = jsonObject1.getInt("total");
                JSONArray results = jsonObject1.getJSONArray("list");
                if (results != null) {
                    currentPage = p;    // 当前页
                    totalPage = (int) Math.ceil((double) sizePage / 100); // 总页数
                    resetTableRows(results);
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, jsonObject.getStr("msg"));
            }

        }).start();


    }

    private void resetTableRows(JSONArray jsonArray) {
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
}
