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

        JLabel templateArgLabel = new JLabel("nuclei参数");
        JTextField templateArgTextField = new JTextField();
        templateArgTextField.setColumns(30);

        templatePanel.add(templateLabel);
        templatePanel.add(templateTextField);
        templatePanel.add(templateArgLabel);
        templatePanel.add(templateArgTextField);
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
        JButton hunterButton = new JButton("选择");
        hunterPanel.add(hunterurlLabel);
        hunterPanel.add(hunterurlTextField);
        hunterPanel.add(hunterkeyLabel);
        hunterPanel.add(hunterkeyTextField);
        hunterPanel.add(hunterButton);

        // zone
        JPanel zonePanel = new JPanel();
        JLabel zoneurlLabel = new JLabel("zoneurl");
        JTextField zoneurlTextField = new JTextField();
        zoneurlTextField.setColumns(20);
        JLabel zonekeyLabel = new JLabel("zonekey");
        JPasswordField zonekeyTextField = new JPasswordField();
        zonekeyTextField.setColumns(20);
        JButton zoneButton = new JButton("选择");
        zonePanel.add(zoneurlLabel);
        zonePanel.add(zoneurlTextField);
        zonePanel.add(zonekeyLabel);
        zonePanel.add(zonekeyTextField);
        zonePanel.add(zoneButton);

        // daydaymap
        JPanel daydaymapPanel = new JPanel();
        JLabel daydaymapurlLabel = new JLabel("daydaymapurl");
        JTextField daydaymapurlTextField = new JTextField();
        daydaymapurlTextField.setColumns(20);
        JLabel daydaymapkeyLabel = new JLabel("daydaymapkey");
        JPasswordField daydaymapkeyTextField = new JPasswordField();
        daydaymapkeyTextField.setColumns(20);
        JButton daydaymapButton = new JButton("选择");
        daydaymapPanel.add(daydaymapurlLabel);
        daydaymapPanel.add(daydaymapurlTextField);
        daydaymapPanel.add(daydaymapkeyLabel);
        daydaymapPanel.add(daydaymapkeyTextField);
        daydaymapPanel.add(daydaymapButton);


        // 添加组件
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(templatePanel);
        panel.add(fofaPanel);
        panel.add(hunterPanel);
        panel.add(zonePanel);
        panel.add(daydaymapPanel);
        this.add(panel, BorderLayout.NORTH);


        // 事件
        templateButton.addActionListener(e -> {
            String templateTextFieldText = templateTextField.getText();
            String templateArgTextFieldText = templateArgTextField.getText();
            YamlUtils.modifyYaml("nucleipath", templateTextFieldText);
            YamlUtils.modifyYaml("nucleiarg", templateArgTextFieldText);
            Utils.templatePath = templateTextFieldText;
            Utils.templateArg = templateArgTextFieldText;
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
        zoneButton.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String zoneurlTextFieldText = zoneurlTextField.getText();
                String zonekeyTextFieldText = zonekeyTextField.getText();
                Utils.zoneUrl = zoneurlTextFieldText;
                Utils.zoneKey = zonekeyTextFieldText;
                YamlUtils.modifyYaml("zoneurl", zoneurlTextFieldText);
                YamlUtils.modifyYaml("zonekey", zonekeyTextFieldText);
                templatesPanel.filterData();
            }
        });
        daydaymapButton.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String daydaymapurlTextFieldText = daydaymapurlTextField.getText();
                String daydaymapkeyTextFieldText = daydaymapkeyTextField.getText();
                Utils.daydaymapUrl = daydaymapurlTextFieldText;
                Utils.daydaymapKey = daydaymapkeyTextFieldText;
                YamlUtils.modifyYaml("daydaymapurl", daydaymapurlTextFieldText);
                YamlUtils.modifyYaml("daydaymapkey", daydaymapkeyTextFieldText);
                templatesPanel.filterData();
            }
        });
        // 初始化数据
        templateTextField.setText(Utils.templatePath);
        templateArgTextField.setText(Utils.templateArg);
        fofaurlTextField.setText(Utils.fofaUrl);
        fofaemailTextField.setText(Utils.fofaEmail);
        fofakeyTextField.setText(Utils.fofaKey);
        hunterurlTextField.setText(Utils.hunterUrl);
        hunterkeyTextField.setText(Utils.hunterKey);
        zoneurlTextField.setText(Utils.zoneUrl);
        zonekeyTextField.setText(Utils.zoneKey);
        daydaymapurlTextField.setText(Utils.daydaymapUrl);
        daydaymapkeyTextField.setText(Utils.daydaymapKey);


    }

    private void initToolBar() {

    }
}
