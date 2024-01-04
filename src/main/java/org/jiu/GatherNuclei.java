package org.jiu;

import com.formdev.flatlaf.FlatLightLaf;
import org.jiu.ui.InitUI;

import javax.swing.*;
import java.awt.*;

import static org.jiu.utils.YamlUtils.parseConfig;

public class GatherNuclei extends JFrame {


    public static void main(String[] args) {
        // 加载ui
        initFlatLaf();
        // 加载yaml配置文件
        parseConfig();
        // 创建窗口
        JFrame jFrame = new GatherNuclei();
        jFrame.setLayout(new BorderLayout());
        jFrame.setTitle("gatherNuclei");
        jFrame.setSize(new Dimension(1200, 700));
        jFrame.setPreferredSize(new Dimension(1200, 700));
        jFrame.setLocationRelativeTo(null);
        jFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);


        InitUI.initMenuBar(jFrame);
        InitUI.initToolBar(jFrame);
        InitUI.initTabbedPane(jFrame);

        jFrame.setVisible(true);
    }

    /**
     * 加载flatlaf主题
     */
    private static void initFlatLaf() {
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (UnsupportedLookAndFeelException e) {
            throw new RuntimeException(e);
        }
        UIManager.put("TextComponent.arc", 5);

    }


}
