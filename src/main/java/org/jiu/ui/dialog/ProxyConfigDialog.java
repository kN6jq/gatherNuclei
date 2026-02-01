package org.jiu.ui.dialog;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.util.Properties;

/**
 * 代理配置对话框
 * 支持 SOCKS5 和 HTTP 代理配置
 */
public class ProxyConfigDialog extends JDialog {
    
    private static final String CONFIG_FILE = "proxy.properties";
    
    // 代理设置组件
    private JCheckBox enableProxyCheckBox;
    private JComboBox<String> proxyTypeComboBox;
    private JTextField hostField;
    private JTextField portField;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton testButton;
    private JButton saveButton;
    private JButton cancelButton;
    
    // 代理配置属性
    private static Properties proxyProps = new Properties();
    
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
    
    private void initComponents() {
        // 启用代理复选框
        enableProxyCheckBox = new JCheckBox("启用代理");
        enableProxyCheckBox.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        
        // 代理类型选择
        proxyTypeComboBox = new JComboBox<>(new String[]{"HTTP", "SOCKS5"});
        proxyTypeComboBox.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        
        // 主机和端口
        hostField = new JTextField(15);
        hostField.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        hostField.setToolTipText("代理服务器地址，如: 127.0.0.1");
        
        portField = new JTextField(6);
        portField.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        portField.setToolTipText("代理服务器端口，如: 1080");
        
        // 认证信息
        usernameField = new JTextField(15);
        usernameField.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        usernameField.setToolTipText("可选，代理认证用户名");
        
        passwordField = new JPasswordField(15);
        passwordField.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        passwordField.setToolTipText("可选，代理认证密码");
        
        // 按钮
        testButton = new JButton("测试连接");
        testButton.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        testButton.setPreferredSize(new Dimension(100, 28));
        
        saveButton = new JButton("保存");
        saveButton.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        saveButton.setPreferredSize(new Dimension(80, 28));
        saveButton.setBackground(new Color(0, 123, 255));
        saveButton.setForeground(Color.WHITE);
        saveButton.setFocusPainted(false);
        
        cancelButton = new JButton("取消");
        cancelButton.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        cancelButton.setPreferredSize(new Dimension(80, 28));
    }
    
    private void setupLayout() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        
        // 代理设置面板
        JPanel proxyPanel = new JPanel(new GridBagLayout());
        proxyPanel.setBorder(new TitledBorder("代理服务器设置"));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 8, 5, 8);
        gbc.anchor = GridBagConstraints.WEST;
        
        // 启用代理
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.gridwidth = 2;
        proxyPanel.add(enableProxyCheckBox, gbc);
        
        // 代理类型
        gbc.gridx = 0; gbc.gridy = 1;
        gbc.gridwidth = 1;
        proxyPanel.add(new JLabel("代理类型:"), gbc);
        gbc.gridx = 1;
        proxyPanel.add(proxyTypeComboBox, gbc);
        
        // 主机地址
        gbc.gridx = 0; gbc.gridy = 2;
        proxyPanel.add(new JLabel("主机地址:"), gbc);
        gbc.gridx = 1;
        proxyPanel.add(hostField, gbc);
        
        // 端口
        gbc.gridx = 0; gbc.gridy = 3;
        proxyPanel.add(new JLabel("端口:"), gbc);
        gbc.gridx = 1;
        proxyPanel.add(portField, gbc);
        
        // 认证信息面板
        JPanel authPanel = new JPanel(new GridBagLayout());
        authPanel.setBorder(new TitledBorder("认证信息 (可选)"));
        
        GridBagConstraints authGbc = new GridBagConstraints();
        authGbc.insets = new Insets(5, 8, 5, 8);
        authGbc.anchor = GridBagConstraints.WEST;
        
        authGbc.gridx = 0; authGbc.gridy = 0;
        authPanel.add(new JLabel("用户名:"), authGbc);
        authGbc.gridx = 1;
        authPanel.add(usernameField, authGbc);
        
        authGbc.gridx = 0; authGbc.gridy = 1;
        authPanel.add(new JLabel("密码:"), authGbc);
        authGbc.gridx = 1;
        authPanel.add(passwordField, authGbc);
        
        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.add(testButton);
        buttonPanel.add(Box.createHorizontalStrut(20));
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        
        // 组装面板
        JPanel centerPanel = new JPanel(new GridLayout(2, 1, 0, 10));
        centerPanel.add(proxyPanel);
        centerPanel.add(authPanel);
        
        mainPanel.add(centerPanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        setContentPane(mainPanel);
    }
    
    private void setupListeners() {
        // 启用/禁用代理时更新组件状态
        enableProxyCheckBox.addActionListener(e -> updateComponentState());
        
        // 测试连接
        testButton.addActionListener(this::testProxyConnection);
        
        // 保存配置
        saveButton.addActionListener(e -> saveConfig());
        
        // 取消
        cancelButton.addActionListener(e -> dispose());
        
        // 初始状态
        updateComponentState();
    }
    
    private void updateComponentState() {
        boolean enabled = enableProxyCheckBox.isSelected();
        proxyTypeComboBox.setEnabled(enabled);
        hostField.setEnabled(enabled);
        portField.setEnabled(enabled);
        usernameField.setEnabled(enabled);
        passwordField.setEnabled(enabled);
        testButton.setEnabled(enabled);
    }
    
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
        
        // 在后台线程测试代理连接
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
            
            // 尝试连接测试
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
    
    private void saveConfig() {
        // 验证输入
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
        
        // 保存配置
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
    
    /**
     * 获取代理配置
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
     * 检查是否启用了代理
     */
    public static boolean isProxyEnabled() {
        Properties props = getProxyConfig();
        return Boolean.parseBoolean(props.getProperty("proxy.enabled", "false"));
    }
    
    /**
     * 获取代理对象
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
}
