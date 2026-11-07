package org.jiu.ui.searchengine;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import org.jiu.core.ShodanCore;
import org.jiu.ui.component.Icons;

import javax.swing.*;
import java.awt.*;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Shodan 端口扫描面板：批量输入 IP，异步查询各 IP 的端口等信息并按输入顺序汇总展示。
 */
public class ShodanPortSearchEngine extends JPanel implements SearchEngine {
    public static JTextArea inputArea = new JTextArea();
    public static JButton searchBtn = new JButton("Scan");
    public JTextArea outputArea = new JTextArea();
    public JButton clearBtn = new JButton("Clear");
    private JLabel statusLabel;
    private JSplitPane splitPane;

    public ShodanPortSearchEngine() {
        this.setLayout(new BorderLayout());

        JPanel leftPanel = createLeftPanel();

        JPanel rightPanel = createRightPanel();

        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        splitPane.setResizeWeight(0.5);
        splitPane.setDividerSize(5);
        splitPane.setContinuousLayout(true);

        JPanel bottomPanel = createBottomPanel();

        this.add(splitPane, BorderLayout.CENTER);
        this.add(bottomPanel, BorderLayout.SOUTH);

        // 监听尺寸变化与显示事件，保持分割线始终居中
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

        inputArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        inputArea.setLineWrap(true);
        JScrollPane scrollPane = new JScrollPane(inputArea);
        panel.add(scrollPane, BorderLayout.CENTER);

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

    /**
     * 解析输入框中的 IP（按行去重），为每个 IP 异步调用 Shodan 查询，全部完成后按输入顺序汇总到输出区。
     * 扫描期间禁用搜索按钮并刷新状态栏；单个 IP 查询异常时以 [Error] 行占位，不中断其余任务。
     */
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

        outputArea.setText("");
        searchBtn.setEnabled(false);
        searchBtn.setText("Scanning...");

        int total = ips.size();
        statusLabel.setText("Scanning: 0/" + total);

        // 异步完成顺序不定：用 List 固定 IP 顺序，Map 按序号暂存结果，最后按序拼接
        java.util.List<String> ipList = new java.util.ArrayList<>(ips);
        java.util.Map<Integer, String> resultsMap = new java.util.concurrent.ConcurrentHashMap<>();
        java.util.concurrent.atomic.AtomicInteger completed = new AtomicInteger(0);

        java.util.List<CompletableFuture<Void>> futures = new java.util.ArrayList<>();

        for (int i = 0; i < ipList.size(); i++) {
            final int index = i + 1; // 序号从 1 开始
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
        return Icons.create("shodan");
    }

    @Override
    public String getTips() {
        return "Shodan Port Scan";
    }
}

