package org.jiu.ui.searchengine;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import org.jiu.core.ShodanCore;
import org.jiu.ui.component.Gbc;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class ShodanPortSearchEngine extends JPanel implements SearchEngine{
    public static JTextArea inputArea = new JTextArea();
    public JTextArea outputArea = new JTextArea();
    public static JButton searchBtn = new JButton("扫描");
    public JButton clearBtn = new JButton("清空");

    public ShodanPortSearchEngine() {
        this.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();


        Gbc gbcinputArea = new Gbc(0, 0,1,2);
        gbcinputArea.weightx = 1;
        gbcinputArea.weighty = 1;
        gbcinputArea.fill = GridBagConstraints.BOTH;
        JScrollPane inputAreascrollPane = new JScrollPane(inputArea);
        this.add(inputAreascrollPane, gbcinputArea);

        Gbc gbcsearchBtn = new Gbc(1, 0);
        gbcsearchBtn.weighty = 1;
//        gbcsearchBtn.setInsets(5,0,0,0);
//        gbcsearchBtn.fill = GridBagConstraints.NONE;
        this.add(searchBtn, gbcsearchBtn);

        Gbc gbcclearBtn = new Gbc(1, 1);
        gbcclearBtn.weighty = 1;
//        gbcclearBtn.fill = GridBagConstraints.NONE;
//        gbcclearBtn.setInsets(0,0,5,0);
        this.add(clearBtn, gbcclearBtn);


        Gbc gbcoutputArea = new Gbc(2, 0,1,2);
        gbcoutputArea.weightx = 1;
        gbcoutputArea.weighty = 1;
        gbcoutputArea.fill = GridBagConstraints.BOTH;
        JScrollPane inputAreaoutputArea = new JScrollPane(outputArea);
        this.add(inputAreaoutputArea, gbcoutputArea);

        searchBtn.addActionListener(e -> {
            String input = inputArea.getText();
            input = input.replaceAll("\r\n", "\n");
            String[] lines = input.split("\n");
            Set<String> ips = new HashSet<>(Arrays.asList(lines));

            for (String ip : ips) {
                CompletableFuture.supplyAsync(() -> {
                    Set<String> data = ShodanCore.getData(ip);
                    StringBuilder stringBuilder = new StringBuilder();
                    for (String s : data) {
                        stringBuilder.append(s).append("\n");
                    }
                    // 使用SwingUtilities.invokeLater更新界面
                    SwingUtilities.invokeLater(() -> {
                        outputArea.append(stringBuilder.toString());
                    });

                    return data;
                });
            }

        });
        clearBtn.addActionListener(e -> {
            outputArea.setText("");
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
        return "shodan端口扫描";
    }
}

