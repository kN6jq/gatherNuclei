package org.jiu.ui.searchengine;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import org.jiu.core.ShodanCore;

import javax.swing.*;
import java.awt.*;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public class ShodanPortSearchEngine extends JPanel implements SearchEngine {
    public static JTextArea inputArea = new JTextArea();
    public JTextArea outputArea = new JTextArea();
    public static JButton searchBtn = new JButton("Scan");
    public JButton clearBtn = new JButton("Clear");
    private JLabel statusLabel;
    private JSplitPane splitPane;

    public ShodanPortSearchEngine() {
        this.setLayout(new BorderLayout());

        // 创建左侧面板（输入区域）
        JPanel leftPanel = createLeftPanel();

        // 创建右侧面板（输出区域）
        JPanel rightPanel = createRightPanel();

        // 使用JSplitPane实现对半分布局
        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        splitPane.setResizeWeight(0.5); // 左右各占50%
        splitPane.setDividerSize(5);
        splitPane.setContinuousLayout(true);

        // 创建底部状态栏
        JPanel bottomPanel = createBottomPanel();

        // 组装主面板
        this.add(splitPane, BorderLayout.CENTER);
        this.add(bottomPanel, BorderLayout.SOUTH);

        // 添加组件监听器，确保分割线在正中间
        this.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                resetDividerLocation();
            }

            @Override
            public void componentShown(java.awt.event.ComponentEvent e) {
                resetDividerLocation();
            }
        });
    }

    private void resetDividerLocation() {
        if (splitPane != null && getWidth() > 0) {
            splitPane.setDividerLocation(0.5);
        }
    }

    private JPanel createLeftPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Input IPs"));

        // 输入区域
        inputArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        inputArea.setLineWrap(true);
        JScrollPane scrollPane = new JScrollPane(inputArea);
        panel.add(scrollPane, BorderLayout.CENTER);

        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));

        searchBtn.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        searchBtn.setPreferredSize(new Dimension(80, 28));
        searchBtn.setBackground(new Color(0, 123, 255));
        searchBtn.setForeground(Color.WHITE);
        searchBtn.setFocusPainted(false);

        clearBtn.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        clearBtn.setPreferredSize(new Dimension(80, 28));
        clearBtn.setBackground(new Color(220, 53, 69));
        clearBtn.setForeground(Color.WHITE);
        clearBtn.setFocusPainted(false);

        buttonPanel.add(searchBtn);
        buttonPanel.add(clearBtn);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        // 绑定事件
        searchBtn.addActionListener(e -> performScan());
        clearBtn.addActionListener(e -> {
            inputArea.setText("");
            outputArea.setText("");
            statusLabel.setText("Ready");
        });

        return panel;
    }

    private JPanel createRightPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Scan Results"));

        // 输出区域
        outputArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        outputArea.setEditable(false);
        outputArea.setLineWrap(false);
        JScrollPane scrollPane = new JScrollPane(outputArea);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createBottomPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createLoweredBevelBorder());

        statusLabel = new JLabel("Ready");
        statusLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 11));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
        panel.add(statusLabel, BorderLayout.WEST);

        return panel;
    }

    private void performScan() {
        String input = inputArea.getText().trim();
        if (input.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter IP address");
            return;
        }

        input = input.replaceAll("\r\n", "\n");
        String[] lines = input.split("\n");
        Set<String> ips = new HashSet<>();
        for (String line : lines) {
            line = line.trim();
            if (!line.isEmpty()) {
                ips.add(line);
            }
        }

        if (ips.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter valid IP address");
            return;
        }

        // 清空输出区域
        outputArea.setText("");
        searchBtn.setEnabled(false);
        searchBtn.setText("Scanning...");

        int total = ips.size();
        statusLabel.setText("Scanning: 0/" + total);

        // 使用List保持顺序，使用Map存储结果
        java.util.List<String> ipList = new java.util.ArrayList<>(ips);
        java.util.Map<Integer, String> resultsMap = new java.util.concurrent.ConcurrentHashMap<>();
        java.util.concurrent.atomic.AtomicInteger completed = new AtomicInteger(0);

        // 创建所有异步任务
        java.util.List<CompletableFuture<Void>> futures = new java.util.ArrayList<>();

        for (int i = 0; i < ipList.size(); i++) {
            final int index = i + 1; // 1-based index
            final String ip = ipList.get(i);

            CompletableFuture<Void> future = CompletableFuture.supplyAsync(() -> ShodanCore.getData(ip))
                .thenAccept(result -> {
                    String formatted = ShodanCore.formatResult(result, index, total);
                    resultsMap.put(index, formatted);
                })
                .exceptionally(ex -> {
                    resultsMap.put(index, "\n#[" + index + "/" + total + "] [Error] " + ip + ": " + ex.getMessage() + "\n");
                    return null;
                })
                .thenRun(() -> {
                    int done = completed.incrementAndGet();
                    SwingUtilities.invokeLater(() -> {
                        statusLabel.setText("Scanning: " + done + "/" + total);
                    });
                });

            futures.add(future);
        }

        // 等待所有任务完成后，按顺序显示结果
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenRun(() -> {
                SwingUtilities.invokeLater(() -> {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 1; i <= total; i++) {
                        String result = resultsMap.get(i);
                        if (result != null) {
                            sb.append(result);
                        }
                    }
                    outputArea.setText(sb.toString());
                    searchBtn.setEnabled(true);
                    searchBtn.setText("Scan");
                    statusLabel.setText("Done: " + total + " IP(s)");
                });
            });
    }


    @Override
    public String getTitle() {
        return "shodanPort";
    }

    @Override
    public FlatSVGIcon getIcon() {
        return null;
    }

    @Override
    public String getTips() {
        return "Shodan Port Scan";
    }
}

