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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

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
    private JButton predBtn = new JButton("上一页");
    private JButton nextBtn = new JButton("下一页");
    private JButton searchBtn = new JButton("搜索");
    private JButton pageCurrentLabel = new JButton("1");
    private JButton pageTotalLabel = new JButton("总页");
    private JButton sizeLabel = new JButton("总数");
    private int currentPage = 1;
    private int totalPage = 1;
    private int sizePage; // 查询总数量
    private MultiComboBox comboxstatus;
    private JTable table;
    private JPopupMenu popupMenu; // 表格右键组
    private DefaultTableModel tableModel;
    private TableRowSorter<DefaultTableModel> sorter;
    private String fields = "ip,host,port,title,domain,icp,city";

    public FofaSearchEngine() {
        this.setLayout(new BorderLayout());
        initToolBar();
        initTable();
    }

    /**
     * 表格
     */
    private void initTable() {
        table = new JTable();
        tableModel = new DefaultTableModel() {
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
                    popupMenu = createPopupMenu();
                    popupMenu.show(table, e.getX(), e.getY());
                }
            }
        });

        this.add(scrollPane, BorderLayout.CENTER);
    }

    /**
     * 工具栏
     */
    private void initToolBar() {
        JToolBar toolBar = new JToolBar();
        inputField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Fofa Search... & Enter");
        inputField.putClientProperty(FlatClientProperties.TEXT_FIELD_TRAILING_ICON, new FlatSearchIcon());
        statusBtn.setToolTipText("搜素状态提示灯");
        searchTypeComboBox.setModel(new DefaultComboBoxModel(new String[]{"custom", "domain", "ip"}));
        predBtn.setSelected(true);
        String[] values = new String[]{"全选", "title", "domain", "icp", "city", "product", "lastupdatetime"};
        comboxstatus = new MultiComboBox(values);
        pageCurrentLabel.setEnabled(false);
        pageCurrentLabel.setSelected(true);
        pageCurrentLabel.setToolTipText("当前页 " + currentPage);
        pageTotalLabel.setToolTipText("总页 " + totalPage);
        sizeLabel.setToolTipText("总数 " + sizePage);
        pageTotalLabel.setEnabled(false);
        pageTotalLabel.setSelected(true);

        sizeLabel.setEnabled(false);
        sizeLabel.setSelected(true);

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
        toolBar.addSeparator();
        toolBar.add(statusBtn);
        toolBar.addSeparator();
        toolBar.add(comboxstatus);
        toolBar.add(predBtn);
        toolBar.add(pageCurrentLabel);
        toolBar.add(pageTotalLabel);
        toolBar.add(sizeLabel);
        toolBar.add(nextBtn);
        this.add(toolBar, BorderLayout.NORTH);
    }

    private void search(int p) {
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
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject = JSONUtil.parseObj(FofaCore.getData(qbase64, fields, p, 100, false));
                sizePage = jsonObject.getInt("size");
                JSONArray results = jsonObject.getJSONArray("results");
                if (results != null) {
                    currentPage = p;
                    totalPage = (int) Math.ceil((double) sizePage / 100); // 总页数
                    resetTableRows(results);
                } else {
                    JOptionPane.showMessageDialog(null, jsonObject.getStr("errmsg"));
                }
            } catch (Exception e) {

                JOptionPane.showMessageDialog(null, jsonObject.getStr("errmsg"));
            }
        }).start();
    }

    private void resetTableRows(JSONArray jsonArray) {
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
        table.setModel(tableModel);
        sorter = new TableRowSorter<>(tableModel);
        table.setRowSorter(sorter);
        tableModel.setRowCount(0);
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
        pageCurrentLabel.setText(String.valueOf(currentPage)); // 当前页
        pageTotalLabel.setText(String.valueOf(totalPage)); // 总页数
        sizeLabel.setText(String.valueOf(sizePage)); // 总数量
        pageCurrentLabel.setToolTipText("当前页 " + currentPage);
        pageTotalLabel.setToolTipText("总页 " + totalPage);
        sizeLabel.setToolTipText("总数 " + sizePage);
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
                ShodanPortSearchEngine.inputArea.setText("");
                ShodanPortSearchEngine.inputArea.setText(String.join("\n", ips));
                ShodanPortSearchEngine.searchBtn.doClick();
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
