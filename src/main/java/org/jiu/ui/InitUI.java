package org.jiu.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class InitUI {
    public static final TemplatesPanel templatesPanel = new TemplatesPanel();
    public static final SearchPanel searchPanel = new SearchPanel();
    public static final ConfigPanel configPanel = new ConfigPanel();
    public static final YamlPanel yamlPanel = new YamlPanel();

    /**
     * 菜单栏
     *
     * @param jFrame
     */
    public static void initMenuBar(JFrame jFrame) {
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("打开");
        JMenuItem shellItem = new JMenuItem("shell");
        fileMenu.add(shellItem);
        JMenu aboutMenu = new JMenu("关于");
        JMenuItem aboutItem = new JMenuItem("关于");
        aboutItem.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(null, "gatherNuclei v1.0.0 by xm17", "关于", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        shellItem.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
        // 如果是windows系统,打开cmd，如果是mac系统，打开终端
            String os = System.getProperty("os.name");
            if (os.toLowerCase().startsWith("win")) {
                try {
                    Runtime.getRuntime().exec("cmd /c start cmd");
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            } else if (os.toLowerCase().startsWith("mac")) {
                try {
                    Runtime.getRuntime().exec("open -a Terminal");
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            } else {
                JOptionPane.showMessageDialog(null, "暂不支持该系统", "提示", JOptionPane.INFORMATION_MESSAGE);
            }
            }
        });
        aboutMenu.add(aboutItem);
        menuBar.add(fileMenu);
        menuBar.add(aboutMenu);

        jFrame.setJMenuBar(menuBar);

    }

    /**
     * 工具栏
     *
     * @param jFrame
     */
    public static void initToolBar(JFrame jFrame) {

    }

    /**
     * 表单栏
     *
     * @param jFrame
     */
    public static void initTabbedPane(JFrame jFrame) {
        JTabbedPane jTabbedPane = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
        jTabbedPane.addTab("仓库", templatesPanel);
        jTabbedPane.addTab("搜索", searchPanel);
        jTabbedPane.addTab("配置", configPanel);
        jTabbedPane.addTab("模板编辑", yamlPanel);
        jFrame.add(jTabbedPane, BorderLayout.CENTER);
    }


}
