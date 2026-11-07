package org.jiu.ui.component;


import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 多选下拉框组件：编辑框展示已选值，点击按钮弹出复选框列表进行多选。
 */
public class MultiComboBox extends JComponent implements ActionListener {
    protected JButton arrowButton;
    private Object[] values;
    private MultiPopup popup;
    private JTextField editor;

    public MultiComboBox(Object[] value) {
        values = value;
        initComponent();
    }

    /**
     * 组装编辑框与下拉按钮，并构建复选框弹出菜单。
     */
    private void initComponent() {
        this.setLayout(new GridLayout(1, 2));
        popup = new MultiPopup(values);
        editor = new JTextField();
        // 编辑框尺寸固定，不随内容变化
        editor.setPreferredSize(new Dimension(100, 20));
        editor.setEditable(false);
        editor.addActionListener(this);
        arrowButton = new JButton("字段▼");
        arrowButton.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        arrowButton.setBackground(new Color(0, 123, 255));
        arrowButton.setForeground(Color.WHITE);
        arrowButton.setFocusPainted(false);
        arrowButton.setMargin(new Insets(0, 6, 0, 6));
        arrowButton.addActionListener(this);
        this.add(editor);
        this.add(arrowButton);

        // 组件在工具栏中空间不足时不应被压扁，否则按钮文字会被截断
        Dimension preferred = new Dimension(140, 30);
        setPreferredSize(preferred);
        setMinimumSize(preferred);
    }

    public Object[] getSelectedValues() {
        return popup.getSelectedValues();
    }

    public void setSelectValues(Object[] selectvalues) {
        popup.setSelectValues(selectvalues);
        setText(selectvalues);
    }

    /**
     * 将选中值以逗号分隔显示在编辑框，空数组则清空。
     */
    private void setText(Object[] values) {
        if (values.length > 0) {
            String value = Arrays.toString(values);
            value = value.replace("[", "");
            value = value.replace("]", "");
            editor.setText(value);
        } else {
            editor.setText("");
        }
    }


    /**
     * 点击编辑框或下拉按钮时弹出选择菜单。
     */
    @Override
    public void actionPerformed(ActionEvent arg0) {
        if (!popup.isVisible()) {
            popup.show(this, 0, getHeight());
        }
    }


    /**
     * 多选弹出菜单：复选框列表加确定/取消按钮，首项为"全选"时联动其余选项。
     */

    public class MultiPopup extends JPopupMenu implements ActionListener {
        private Object[] values;
        private List<JCheckBox> checkBoxList = new ArrayList<JCheckBox>();
        private JButton commitButton;
        private JButton cancelButton;

        public MultiPopup(Object[] value) {
            super();
            values = value;
            initComponent();
        }

        /**
         * 构建复选框列表与确定/取消按钮，首项"全选"可联动勾选或清空其余项。
         */
        private void initComponent() {
            JPanel checkboxPane = new JPanel();
            JPanel buttonPane = new JPanel();
            this.setLayout(new BorderLayout());
            for (Object v : values) {
                JCheckBox temp = new JCheckBox(v.toString());
                checkBoxList.add(temp);
            }

            if (checkBoxList.get(0).getText().equals("全选")) {
                checkBoxList.get(0).addItemListener(new ItemListener() {
                    public void itemStateChanged(ItemEvent e) {
                        if (checkBoxList.get(0).isSelected()) {
                            for (int i = 1; i < checkBoxList.size(); i++) {
                                if (!checkBoxList.get(i).isSelected()) {
                                    checkBoxList.get(i).setSelected(true);
                                }
                            }
                        } else {
                            for (int i = 1; i < checkBoxList.size(); i++) {
                                if (checkBoxList.get(i).isSelected()) {
                                    checkBoxList.get(i).setSelected(false);
                                }
                            }
                        }
                    }
                });
            }

            checkboxPane.setLayout(new GridLayout(checkBoxList.size(), 1, 3, 3));
            for (JCheckBox box : checkBoxList) {
                checkboxPane.add(box);
            }

            commitButton = new JButton("确定");
            commitButton.setFont(new Font("微软雅黑", Font.PLAIN, 12));
            commitButton.setBackground(new Color(0, 123, 255));
            commitButton.setForeground(Color.WHITE);
            commitButton.setFocusPainted(false);
            commitButton.addActionListener(this);

            cancelButton = new JButton("取消");
            cancelButton.setFont(new Font("微软雅黑", Font.PLAIN, 12));
            cancelButton.setBackground(new Color(108, 117, 125));
            cancelButton.setForeground(Color.WHITE);
            cancelButton.setFocusPainted(false);
            cancelButton.addActionListener(this);

            buttonPane.add(commitButton);
            buttonPane.add(cancelButton);
            this.add(checkboxPane, BorderLayout.CENTER);
            this.add(buttonPane, BorderLayout.SOUTH);

        }

        /**
         * 按显示文本匹配并勾选对应复选框（不会取消其他已勾选项），同时刷新编辑框。
         */
        public void setSelectValues(Object[] values) {
            if (values.length > 0) {
                for (int i = 0; i < values.length; i++) {
                    for (int j = 0; j < checkBoxList.size(); j++) {
                        if (values[i].equals(checkBoxList.get(j).getText())) {
                            checkBoxList.get(j).setSelected(true);
                        }
                    }
                }
                setText(getSelectedValues());
            }
        }


        /**
         * 收集勾选的值；首项"全选"勾选时返回除"全选"外的全部项。
         */
        public Object[] getSelectedValues() {
            List<Object> selectedValues = new ArrayList<Object>();

            if (checkBoxList.get(0).getText().equals("全选")) {
                if (checkBoxList.get(0).isSelected()) {
                    for (int i = 1; i < checkBoxList.size(); i++) {
                        selectedValues.add(values[i]);
                    }
                } else {
                    for (int i = 1; i < checkBoxList.size(); i++) {
                        if (checkBoxList.get(i).isSelected()) {
                            selectedValues.add(values[i]);
                        }
                    }
                }
            } else {
                for (int i = 0; i < checkBoxList.size(); i++) {
                    if (checkBoxList.get(i).isSelected()) {
                        selectedValues.add(values[i]);
                    }
                }
            }

            return selectedValues.toArray(new Object[selectedValues.size()]);
        }


        /**
         * 确定：回填编辑框并收起菜单；取消：仅收起菜单。
         */
        @Override
        public void actionPerformed(ActionEvent arg0) {
            Object source = arg0.getSource();
            if (source instanceof JButton) {
                JButton button = (JButton) source;
                if (button.equals(commitButton)) {
                    setText(getSelectedValues());
                    popup.setVisible(false);
                } else if (button.equals(cancelButton)) {
                    popup.setVisible(false);
                }
            }
        }

    }


}
