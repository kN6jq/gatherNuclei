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
    private JButton predBtn = new JButton("上一页");
    private JButton nextBtn = new JButton("下一页");
    private JButton pageCurrentLabel = new JButton("1");
    private JButton pageTotalLabel = new JButton("总页");
    private JButton sizeLabel = new JButton("总数");
    private int currentPage = 1;
    private int totalPage = 0;
    private int sizePage = 0; // 查询总数量
    private JTable table;
    private DefaultTableModel tableModel;
    private TableRowSorter<DefaultTableModel> sorter;
    private JPopupMenu popupMenu; // 表格右键组

    public OtxSearchEngine() {
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


        popupMenu.add(copyRowItem);
        return popupMenu;

    }

    private void initToolBar() {
        JToolBar toolBar = new JToolBar();
        inputField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "OTX Search... & Enter");
        inputField.putClientProperty(FlatClientProperties.TEXT_FIELD_TRAILING_ICON, new FlatSearchIcon());
        searchBtn.setText("搜索");
        statusBtn.setToolTipText("搜素状态提示灯");

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

        toolBar.add(inputField);
        toolBar.add(searchBtn);
        toolBar.add(statusBtn);
        toolBar.addSeparator();
        toolBar.add(predBtn);
        toolBar.add(pageCurrentLabel);
        toolBar.add(pageTotalLabel);
        toolBar.add(sizeLabel);
        toolBar.add(nextBtn);

        // 搜索按钮
        searchBtn.addActionListener(e -> {
            search(1);
        });
        // 上一页
        predBtn.addActionListener(e -> {
            if (currentPage > 1) {
                currentPage--;
                search(currentPage);
            }
        });
        // 下一页
        nextBtn.addActionListener(e -> {
            if (currentPage < totalPage) {
                currentPage++;
                search(currentPage);
            }
        });


        this.add(toolBar, BorderLayout.NORTH);
    }

    private void search(int page) {

        String data = inputField.getText();
        new Thread(() -> {
            JSONObject jsonObject = JSONUtil.parseObj(OtxCore.getData(data, page, 100));
            // 获取总数
            String full_size = jsonObject.getStr("full_size");
            sizePage = Integer.parseInt(full_size);
            sizeLabel.setText(full_size);
            sizeLabel.setToolTipText("总数 " + sizePage);
            // 计算总页数
            totalPage = sizePage / 100;
            if (sizePage % 100 != 0) {
                totalPage++;
            }
            // 设置当前页
            currentPage = page;
            pageCurrentLabel.setText(String.valueOf(currentPage));
            pageTotalLabel.setText(String.valueOf(totalPage));
            pageTotalLabel.setToolTipText("总页 " + totalPage);

            // 渲染数据
            resetTableRows(jsonObject.getJSONArray("url_list"));
        }).start();
    }


    private void resetTableRows(JSONArray jsonArray) {
        tableModel.setRowCount(0);
        table.setRowSorter(sorter);
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
