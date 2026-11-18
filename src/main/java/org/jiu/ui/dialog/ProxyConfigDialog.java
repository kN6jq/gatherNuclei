package org.jiu.ui.dialog;

import org.jiu.utils.UIUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.util.Properties;

/**
 * 代理配置对话框，支持 SOCKS5 和 HTTP 代理配置。
 * 配置持久化到运行目录的 proxy.properties，并提供静态方法供全局读取。
 */
public class ProxyConfigDialog extends JDialog {

    private static final String CONFIG_FILE = "proxy.properties";
    private static Properties proxyProps = new Properties();
    private JCheckBox enableProxyCheckBox;
    private JComboBox<String> proxyTypeComboBox;
    private JTextField hostField;
    private JTextField portField;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton testButton;
    private JButton saveButton;
    private JButton cancelButton;

    public ProxyConfigDialog(JFrame parent) {
        super(parent, "代理配置", true);
        initComponents();
        loadConfig();
        setupLayout();
        setupListeners();
        pack();
        setLocationRelativeTo(parent);
        setResizable(false);
    }

    /**
     * 获取代理配置属性；首次访问时从 proxy.properties 懒加载，
     * 文件不存在或读取失败时返回空配置。
     */
    public static Properties getProxyConfig() {
        if (proxyProps.isEmpty()) {
            File configFile = new File(CONFIG_FILE);
            if (configFile.exists()) {
                try (InputStream in = new FileInputStream(configFile)) {
                    proxyProps.load(in);
                } catch (IOException e) {
                    System.err.println("加载代理配置失败: " + e.getMessage());
                }
            }
        }
        return proxyProps;
    }

    /**
     * 是否启用代理（proxy.enabled，默认 false）。
     */
    public static boolean isProxyEnabled() {
        Properties props = getProxyConfig();
        return Boolean.parseBoolean(props.getProperty("proxy.enabled", "false"));
    }

    /**
     * 按配置构建代理对象，未启用时返回 NO_PROXY；端口格式非法时抛出 NumberFormatException。
     */
    public static java.net.Proxy getProxy() {
        Properties props = getProxyConfig();
        if (!isProxyEnabled()) {
            return java.net.Proxy.NO_PROXY;
        }

        String type = props.getProperty("proxy.type", "HTTP");
        String host = props.getProperty("proxy.host", "");
        int port = Integer.parseInt(props.getProperty("proxy.port", "0"));

        if ("SOCKS5".equals(type)) {
            return new java.net.Proxy(java.net.Proxy.Type.SOCKS,
                    new java.net.InetSocketAddress(host, port));
        } else {
            return new java.net.Proxy(java.net.Proxy.Type.HTTP,
                    new java.net.InetSocketAddress(host, port));
        }
    }

    /**
     * 创建代理开关、类型、地址端口、认证输入框及操作按钮。
     */
    private void initComponents() {
        enableProxyCheckBox = new JCheckBox("启用代理");
        enableProxyCheckBox.setFont(UIUtils.LABEL_FONT);

        proxyTypeComboBox = new JComboBox<>(new String[]{"HTTP", "SOCKS5"});
        UIUtils.setupComboBox(proxyTypeComboBox, 90, UIUtils.INPUT_HEIGHT);

        hostField = new JTextField(15);
        UIUtils.styleTextField(hostField, 180);
        hostField.setToolTipText("代理服务器地址，如: 127.0.0.1");

        portField = new JTextField(6);
        UIUtils.styleTextField(portField, 80);
        portField.setToolTipText("代理服务器端口，如: 1080");

        usernameField = new JTextField(15);
        UIUtils.styleTextField(usernameField, 180);
        usernameField.setToolTipText("可选，代理认证用户名");

        passwordField = new JPasswordField(15);
        UIUtils.styleTextField(passwordField, 180);
        passwordField.setToolTipText("可选，代理认证密码");

        testButton = new JButton();
        UIUtils.styleButton(testButton, "测试连接", null);

        saveButton = new JButton();
        UIUtils.stylePrimaryButton(saveButton, "保存");

        cancelButton = new JButton();
        UIUtils.styleButton(cancelButton, "取消", null);
    }

    /**
     * 创建统一字体的表单标签。
     */
    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        UIUtils.styleLabel(label);
        return label;
    }

    /**
     * 以 GridBag 布局组装代理设置、认证信息与按钮面板。
     */
    private void setupLayout() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JPanel proxyPanel = new JPanel(new GridBagLayout());
        proxyPanel.setBorder(new TitledBorder("代理服务器设置"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 8, 5, 8);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        proxyPanel.add(enableProxyCheckBox, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        proxyPanel.add(createLabel("代理类型:"), gbc);
        gbc.gridx = 1;
        proxyPanel.add(proxyTypeComboBox, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        proxyPanel.add(createLabel("主机地址:"), gbc);
        gbc.gridx = 1;
        proxyPanel.add(hostField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        proxyPanel.add(createLabel("端口:"), gbc);
        gbc.gridx = 1;
        proxyPanel.add(portField, gbc);

        JPanel authPanel = new JPanel(new GridBagLayout());
        authPanel.setBorder(new TitledBorder("认证信息 (可选)"));

        GridBagConstraints authGbc = new GridBagConstraints();
        authGbc.insets = new Insets(5, 8, 5, 8);
        authGbc.anchor = GridBagConstraints.WEST;

        authGbc.gridx = 0;
        authGbc.gridy = 0;
        authPanel.add(createLabel("用户名:"), authGbc);
        authGbc.gridx = 1;
        authPanel.add(usernameField, authGbc);

        authGbc.gridx = 0;
        authGbc.gridy = 1;
        authPanel.add(createLabel("密码:"), authGbc);
        authGbc.gridx = 1;
        authPanel.add(passwordField, authGbc);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.add(testButton);
        buttonPanel.add(Box.createHorizontalStrut(20));
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);

        JPanel centerPanel = new JPanel(new GridLayout(2, 1, 0, 10));
        centerPanel.add(proxyPanel);
        centerPanel.add(authPanel);

        mainPanel.add(centerPanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }

    /**
     * 绑定复选框与各按钮事件，并按当前开关状态初始化组件可用性。
     */
    private void setupListeners() {
        enableProxyCheckBox.addActionListener(e -> updateComponentState());

        testButton.addActionListener(this::testProxyConnection);

        saveButton.addActionListener(e -> saveConfig());

        cancelButton.addActionListener(e -> dispose());

        updateComponentState();
    }

    /**
     * 按代理开关状态统一启用/禁用输入组件与测试按钮。
     */
    private void updateComponentState() {
        boolean enabled = enableProxyCheckBox.isSelected();
        proxyTypeComboBox.setEnabled(enabled);
        hostField.setEnabled(enabled);
        portField.setEnabled(enabled);
        usernameField.setEnabled(enabled);
        passwordField.setEnabled(enabled);
        testButton.setEnabled(enabled);
    }

    /**
     * 校验地址与端口（1-65535）后在后台线程测试代理连通性，
     * 期间禁用测试按钮，完成后弹窗提示结果；校验不通过仅弹窗提示。
     */
    private void testProxyConnection(ActionEvent e) {
        String host = hostField.getText().trim();
        String portStr = portField.getText().trim();

        if (host.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请输入代理服务器地址", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (portStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请输入代理服务器端口", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int port;
        try {
            port = Integer.parseInt(portStr);
            if (port < 1 || port > 65535) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "端口号必须是 1-65535 之间的数字", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String proxyType = (String) proxyTypeComboBox.getSelectedItem();

        testButton.setEnabled(false);
        testButton.setText("测试中...");

        SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                return testProxy(host, port, proxyType);
            }

            @Override
            protected void done() {
                testButton.setEnabled(true);
                testButton.setText("测试连接");
                try {
                    if (get()) {
                        JOptionPane.showMessageDialog(ProxyConfigDialog.this,
                                proxyType + " 代理连接成功!",
                                "测试成功",
                                JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(ProxyConfigDialog.this,
                                "代理连接失败，请检查配置",
                                "测试失败",
                                JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(ProxyConfigDialog.this,
                            "测试过程发生错误: " + ex.getMessage(),
                            "错误",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    /**
     * 通过代理向 www.baidu.com 发 HEAD 请求验证连通性（连接/读取超时各 10 秒），
     * 响应码 2xx/3xx 视为成功，任何异常返回 false。
     */
    private boolean testProxy(String host, int port, String proxyType) {
        try {
            java.net.Proxy proxy;
            if ("SOCKS5".equals(proxyType)) {
                proxy = new java.net.Proxy(java.net.Proxy.Type.SOCKS,
                        new java.net.InetSocketAddress(host, port));
            } else {
                proxy = new java.net.Proxy(java.net.Proxy.Type.HTTP,
                        new java.net.InetSocketAddress(host, port));
            }

            java.net.URL url = new java.net.URL("http://www.baidu.com");
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection(proxy);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setRequestMethod("HEAD");
            int responseCode = conn.getResponseCode();
            return responseCode >= 200 && responseCode < 400;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 启用代理时先校验地址与端口，通过后把配置写入 proxy.properties 并关闭对话框；
     * 校验或写入失败弹窗提示，不关闭。
     */
    private void saveConfig() {
        if (enableProxyCheckBox.isSelected()) {
            String host = hostField.getText().trim();
            String portStr = portField.getText().trim();

            if (host.isEmpty()) {
                JOptionPane.showMessageDialog(this, "请输入代理服务器地址", "提示", JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (portStr.isEmpty()) {
                JOptionPane.showMessageDialog(this, "请输入代理服务器端口", "提示", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try {
                int port = Integer.parseInt(portStr);
                if (port < 1 || port > 65535) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "端口号必须是 1-65535 之间的数字", "错误", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        proxyProps.setProperty("proxy.enabled", String.valueOf(enableProxyCheckBox.isSelected()));
        proxyProps.setProperty("proxy.type", (String) proxyTypeComboBox.getSelectedItem());
        proxyProps.setProperty("proxy.host", hostField.getText().trim());
        proxyProps.setProperty("proxy.port", portField.getText().trim());
        proxyProps.setProperty("proxy.username", usernameField.getText().trim());
        proxyProps.setProperty("proxy.password", new String(passwordField.getPassword()));

        try (OutputStream out = new FileOutputStream(CONFIG_FILE)) {
            proxyProps.store(out, "Proxy Configuration");
            JOptionPane.showMessageDialog(this, "代理配置已保存", "成功", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "保存配置失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * 从 proxy.properties 回填界面组件；文件不存在时保持默认值。
     */
    private void loadConfig() {
        File configFile = new File(CONFIG_FILE);
        if (!configFile.exists()) {
            return;
        }

        try (InputStream in = new FileInputStream(configFile)) {
            proxyProps.load(in);

            enableProxyCheckBox.setSelected(
                    Boolean.parseBoolean(proxyProps.getProperty("proxy.enabled", "false")));
            proxyTypeComboBox.setSelectedItem(proxyProps.getProperty("proxy.type", "HTTP"));
            hostField.setText(proxyProps.getProperty("proxy.host", ""));
            portField.setText(proxyProps.getProperty("proxy.port", ""));
            usernameField.setText(proxyProps.getProperty("proxy.username", ""));
            passwordField.setText(proxyProps.getProperty("proxy.password", ""));

            updateComponentState();
        } catch (IOException e) {
            System.err.println("加载代理配置失败: " + e.getMessage());
        }
    }
}
