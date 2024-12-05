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

public class Zone0SearchEngine extends JPanel implements SearchEngine {
    private final JTextField inputField = new JTextField();
    private final JButton statusBtn = new JButton();
    private JButton predBtn = new JButton("上一页");
    private JButton nextBtn = new JButton("下一页");
    private JButton searchBtn = new JButton("搜索");
    private JButton pageCurrentLabel = new JButton("1");
    private JButton pageTotalLabel = new JButton("总页");
    private JButton sizeLabel = new JButton("总数");
    private int currentPage = 1;
    private int totalPage = 0;
    private int sizePage; // 查询总数量
    private static JComboBox searchTypeComboBox = new JComboBox();
    private final String[] sitecolumnNames = {"#", "IP", "Port", "URL", "Title", "city", "operator", "company", "tags"};
    private final String[] apkcolumnNames = {"#", "title", "type", "company"};
    private JTable table;
    private DefaultTableModel tableModel;
    private JPopupMenu popupMenu; // 表格右键组

    public Zone0SearchEngine() {
        this.setLayout(new BorderLayout());
        initToolBar();
        initTable();
    }

    private void initTable() {
        table = new JTable();
        tableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return true; // 可编辑
            }
        };

        tableModel.setColumnIdentifiers(sitecolumnNames);
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
        // 复制选中行ip
        JMenuItem copyRowIpItem = new JMenuItem("复制当前选中行IP");
        copyRowIpItem.addActionListener(e -> {
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
        // 复制选中行url
        JMenuItem copyRowUrlItem = new JMenuItem("复制当前选中行URL");
        copyRowUrlItem.addActionListener(e -> {
            int[] selectedRows = table.getSelectedRows();
            if (selectedRows.length == 0) {
                JOptionPane.showMessageDialog(null, "请先选择数据");
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                for (int selectedRow : selectedRows) {
                    stringBuilder.append(table.getValueAt(selectedRow, 3)).append("\n");
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
        popupMenu.add(copyRowIpItem);
        popupMenu.add(copyRowUrlItem);
        popupMenu.add(sendToShodanItem);
        return popupMenu;
    }

    private void initToolBar() {
        JToolBar toolBar = new JToolBar();
        inputField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "0Zone Search... & Enter");
        inputField.putClientProperty(FlatClientProperties.TEXT_FIELD_TRAILING_ICON, new FlatSearchIcon());
        statusBtn.setToolTipText("搜素状态提示灯");
        searchTypeComboBox.setModel(new DefaultComboBoxModel(new String[]{"信息系统", "移动端应用", "域名"}));
        predBtn.setSelected(true);
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
            tableModel.setRowCount(0);
            search(1);
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, false), JComponent.WHEN_FOCUSED);

        // 搜索按钮事件
        searchBtn.addActionListener(e -> {
            tableModel.setRowCount(0);
            search(1);
        });

        // 上一页按钮事件
        predBtn.addActionListener(e -> {
            if (currentPage > 1) {
                currentPage--;
                search(currentPage);
            }
        });
        // 下一页按钮事件
        nextBtn.addActionListener(e -> {
            if (currentPage < totalPage) {
                currentPage++;
                search(currentPage);
            }
        });


        toolBar.add(inputField);
        toolBar.add(searchBtn);
        toolBar.add(statusBtn);
        toolBar.add(searchTypeComboBox);
        toolBar.add(pageCurrentLabel);
        toolBar.add(pageTotalLabel);
        toolBar.add(sizeLabel);
        toolBar.add(predBtn);
        toolBar.add(nextBtn);
        this.add(toolBar, BorderLayout.NORTH);

    }

    private void search(int p) {
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
        switch (searchType) {
            case "domain":
                JOptionPane.showMessageDialog(null, "暂不支持域名搜索,没有会员");
                break;
            case "site":
                tableModel.setColumnIdentifiers(sitecolumnNames);
                table.setModel(tableModel);
                String sitedata = String.format("(company==%s)||(title==%s)||(banner==%s)||(component==%s)||(ssl_info.detail==%s)", input, input, input, input, input, input);
                new Thread(() -> {
                    JSONObject jsonObject = new JSONObject();
                    try {
                        jsonObject = JSONUtil.parseObj(ZoneCore.getData(sitedata, searchType, p, 100));
                        sizePage = Integer.parseInt(jsonObject.getStr("total"));
                        totalPage = sizePage / 100;
                        pageTotalLabel.setText(String.valueOf(totalPage));
                        pageTotalLabel.setToolTipText("总页 " + totalPage);
                        sizeLabel.setText(String.valueOf(sizePage));
                        sizeLabel.setToolTipText("总数 " + sizePage);
                        currentPage = p;
                        pageCurrentLabel.setText(String.valueOf(currentPage));
                        JSONArray jsonArray = jsonObject.getJSONArray("data");
                        resetTableRowsSite(jsonArray);
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(null, jsonObject.getStr("message"));
                    }
                }).start();
                break;
            case "apk":
                tableModel.setColumnIdentifiers(apkcolumnNames);
                table.setModel(tableModel);
                String apkdata = String.format("(group==%s)||(company==%s)||(title==%s)", input, input, input);
                new Thread(() -> {
                    JSONObject jsonObject = new JSONObject();
                    try {
                        jsonObject = JSONUtil.parseObj(ZoneCore.getData(apkdata, searchType, p, 100));
                        sizePage = Integer.parseInt(jsonObject.getStr("total"));
                        totalPage = sizePage / 100;
                        pageTotalLabel.setText(String.valueOf(totalPage));
                        pageTotalLabel.setToolTipText("总页 " + totalPage);
                        sizeLabel.setText(String.valueOf(sizePage));
                        sizeLabel.setToolTipText("总数 " + sizePage);
                        currentPage = p;
                        pageCurrentLabel.setText(String.valueOf(currentPage));
                        JSONArray jsonArray = jsonObject.getJSONArray("data");
                        resetTableRowsApk(jsonArray);
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(null, jsonObject.getStr("message"));
                    }
                }).start();
                break;
        }


    }

    private void resetTableRowsSite(JSONArray jsonArray) {
        tableModel.setRowCount(0);
        for (int i = 0; i < jsonArray.size(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            String ip = jsonObject.getStr("ip");
            String port = jsonObject.getStr("port");
            String url = jsonObject.getStr("url");
            String title = jsonObject.getStr("title");
            String city = jsonObject.getStr("city");
            String operator = jsonObject.getStr("operator");
            // company格式 ["111"]
            String company = jsonObject.getStr("company");
            // 删除company前2个字符和后2个字符
            if (company.length() > 4) {
                company = company.substring(2, company.length() - 2);
            }
            JSONArray tags = jsonObject.getJSONArray("tags");
            StringBuilder tag = new StringBuilder();
            for (int j = 0; j < tags.size(); j++) {
                tag.append(tags.getStr(j)).append(",");
            }
            // 去掉最后一个逗号
            if (tag.length() > 0) {
                tag.deleteCharAt(tag.length() - 1);
            }
            tableModel.addRow(new Object[]{i + 1, ip, port, url, title, city, operator, company, tag.toString()});
        }

    }

    private void resetTableRowsApk(JSONArray jsonArray) {
        tableModel.setRowCount(0);
        for (int i = 0; i < jsonArray.size(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            String title = jsonObject.getStr("title");
            String type = jsonObject.getStr("type");
            String company = jsonObject.getStr("company");
            tableModel.addRow(new Object[]{i + 1, title, type, company});
        }
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
}
