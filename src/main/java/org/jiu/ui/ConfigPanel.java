package org.jiu.ui;

import org.jiu.utils.Utils;
import org.jiu.utils.YamlUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

import static org.jiu.ui.InitUI.templatesPanel;

public class ConfigPanel extends JPanel {

    private final FlowLayout flowLayout = new FlowLayout();

    public ConfigPanel() {
        flowLayout.setAlignment(FlowLayout.LEFT);
        initToolBar();
        initTable();
    }

    private void initTable() {
        this.setLayout(new BorderLayout());
        // 模板路径
        JPanel templatePanel = new JPanel();
        JLabel templateLabel = new JLabel("模板路径");
        JTextField templateTextField = new JTextField();
        templateTextField.setColumns(30);
        JButton templateButton = new JButton("选择");
        templatePanel.add(templateLabel);
        templatePanel.add(templateTextField);
        templatePanel.add(templateButton);

        // fofa
        JPanel fofaPanel = new JPanel();
        JLabel fofaurlLabel = new JLabel("fofaurl");
        JTextField fofaurlTextField = new JTextField();
        fofaurlTextField.setColumns(20);
        JLabel fofaemailLabel = new JLabel("fofaemail");
        JPasswordField fofaemailTextField = new JPasswordField();
        fofaemailTextField.setColumns(10);
        JLabel fofakeyLabel = new JLabel("fofakey");
        JPasswordField fofakeyTextField = new JPasswordField();
        fofakeyTextField.setColumns(10);
        JButton fofaButton = new JButton("选择");
        fofaPanel.add(fofaurlLabel);
        fofaPanel.add(fofaurlTextField);
        fofaPanel.add(fofaemailLabel);
        fofaPanel.add(fofaemailTextField);
        fofaPanel.add(fofakeyLabel);
        fofaPanel.add(fofakeyTextField);
        fofaPanel.add(fofaButton);

        // hunter
        JPanel hunterPanel = new JPanel();
        JLabel hunterurlLabel = new JLabel("hunterurl");
        JTextField hunterurlTextField = new JTextField();
        hunterurlTextField.setColumns(20);
        JLabel hunterkeyLabel = new JLabel("hunterkey");
        JPasswordField hunterkeyTextField = new JPasswordField();
        hunterkeyTextField.setColumns(30);
        // 模糊显示

        JButton hunterButton = new JButton("选择");
        hunterPanel.add(hunterurlLabel);
        hunterPanel.add(hunterurlTextField);
        hunterPanel.add(hunterkeyLabel);
        hunterPanel.add(hunterkeyTextField);
        hunterPanel.add(hunterButton);

        // 添加组件
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(templatePanel);
        panel.add(fofaPanel);
        panel.add(hunterPanel);
        this.add(panel, BorderLayout.NORTH);


        // 事件
        templateButton.addActionListener(e -> {
            String templateTextFieldText = templateTextField.getText();
            YamlUtils.modifyYaml("nucleipath", templateTextFieldText);
            Utils.templatePath = templateTextFieldText;
            templatesPanel.filterData();
        });
        fofaButton.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String fofaurlTextFieldText = fofaurlTextField.getText();
                String fofaemailTextFieldText = fofaemailTextField.getText();
                String fofakeyTextFieldText = fofakeyTextField.getText();
                Utils.fofaUrl = fofaurlTextFieldText;
                Utils.fofaEmail = fofaemailTextFieldText;
                Utils.fofaKey = fofakeyTextFieldText;
                YamlUtils.modifyYaml("fofaurl", fofaurlTextFieldText);
                YamlUtils.modifyYaml("fofaemail", fofaemailTextFieldText);
                YamlUtils.modifyYaml("fofakey", fofakeyTextFieldText);
                templatesPanel.filterData();
            }
        });
        hunterButton.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String hunterurlTextFieldText = hunterurlTextField.getText();
                String hunterkeyTextFieldText = hunterkeyTextField.getText();
                Utils.hunterUrl = hunterurlTextFieldText;
                Utils.hunterKey = hunterkeyTextFieldText;
                YamlUtils.modifyYaml("hunterurl", hunterurlTextFieldText);
                YamlUtils.modifyYaml("hunterkey", hunterkeyTextFieldText);
                templatesPanel.filterData();
            }
        });
        // 初始化数据
        templateTextField.setText(Utils.templatePath);
        fofaurlTextField.setText(Utils.fofaUrl);
        fofaemailTextField.setText(Utils.fofaEmail);
        fofakeyTextField.setText(Utils.fofaKey);
        hunterurlTextField.setText(Utils.hunterUrl);
        hunterkeyTextField.setText(Utils.hunterKey);

    }

    private void initToolBar() {

    }
}
