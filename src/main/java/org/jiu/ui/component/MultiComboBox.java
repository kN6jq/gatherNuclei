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

public class MultiComboBox extends JComponent implements ActionListener {
    protected JButton arrowButton;
    private Object[] values;
    private MultiPopup popup;
    private JTextField editor;

    public MultiComboBox(Object[] value) {
        values = value;
        initComponent();
    }

    private void initComponent() {
        this.setLayout(new GridLayout(1, 2));
        popup = new MultiPopup(values);
        editor = new JTextField();
        //editor 编辑框的长度不可变 固定
        editor.setPreferredSize(new Dimension(100, 20));
        editor.setEditable(false);
        editor.addActionListener(this);
        arrowButton = new JButton("字段▼");
        arrowButton.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        arrowButton.setBackground(new Color(0, 123, 255));
        arrowButton.setForeground(Color.WHITE);
        arrowButton.setFocusPainted(false);
        arrowButton.addActionListener(this);
        this.add(editor);
        this.add(arrowButton);
    }

    //获取选中的数据
    public Object[] getSelectedValues() {
        return popup.getSelectedValues();
    }

    //设置需要选中的值
    public void setSelectValues(Object[] selectvalues) {
        popup.setSelectValues(selectvalues);
        setText(selectvalues);
    }

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


    @Override
    public void actionPerformed(ActionEvent arg0) {
        // TODO Auto-generated method stub
        if (!popup.isVisible()) {
            popup.show(this, 0, getHeight());
        }
    }


    //内部类MultiPopup

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


        @Override
        public void actionPerformed(ActionEvent arg0) {
            // TODO Auto-generated method stub
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
