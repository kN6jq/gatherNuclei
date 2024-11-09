package org.jiu.ui;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.file.FileWriter;
import com.formdev.flatlaf.FlatClientProperties;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.jiu.utils.Utils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.Objects;

/**
 * @Author Xm17
 * @Date 2024-07-10 15:16
 */
public class YamlPanel extends JPanel {
    public static RSyntaxTextArea textArea;
    public static String absolutePath;
    private static JButton addTemplateButton;
    private static JButton saveTemplateButton;
    private static JTextField tipsTextField;


    public YamlPanel() {
        this.setLayout(new BorderLayout());
        initToolBar();
        initTextArea();
    }

    private void initTextArea() {
        textArea = new RSyntaxTextArea();
        textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_YAML);
        textArea.setCodeFoldingEnabled(true);
        RTextScrollPane sp = new RTextScrollPane(textArea);



        textArea.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_S && e.isControlDown()) {
                    saveFile();
                }
            }
        });

        this.add(sp,BorderLayout.CENTER);
    }

    private void initToolBar() {
        JToolBar toolBar = new JToolBar();

        // 新建模板按钮
        addTemplateButton = new JButton("新建模板");
        addTemplateButton.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                textArea.setText("");
                absolutePath = null;
            }
        });
        // 保存
        saveTemplateButton = new JButton("保存");
        saveTemplateButton.addActionListener(new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                saveFile();
            }
        });


        tipsTextField = new JTextField("Tips: 模板编辑中...");

        toolBar.add(addTemplateButton);
        toolBar.addSeparator();
        toolBar.add(saveTemplateButton);
        toolBar.addSeparator();
        toolBar.add(tipsTextField);
        add(toolBar, BorderLayout.NORTH);
    }

    public void saveFile()  {
        FileWriter writer = null;
        String filename = "";
        if (absolutePath == null) {
            try {
                filename = JOptionPane.showInputDialog("请输入文件名");
                if (Objects.equals(filename, "") || filename == null){
                    return;
                }
            }catch (Exception e ){
                e.printStackTrace();
            }

            String templatePath = Utils.templatePath+"\\"+filename+".yaml";
            if (FileUtil.exist(templatePath)){
                int result = JOptionPane.showConfirmDialog(null, "文件已存在，是否覆盖？", "提示", JOptionPane.YES_NO_OPTION);
                if (result == JOptionPane.NO_OPTION) {
                    return;
                }
            }

            writer = new FileWriter(templatePath);
            writer.write(textArea.getText());
            JOptionPane.showMessageDialog(null, "保存成功", "提示", JOptionPane.INFORMATION_MESSAGE);
        }
        // 使用SwingWorker异步保存文件
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                FileWriter writer = null;
                writer = new FileWriter(absolutePath);
                writer.write(textArea.getText());
                return null;
            }

            @Override
            protected void done() {
                // 保存完成后，设置提示信息恢复原状，并启动计时器
                tipsTextField.setText("Tips: 模板保存中...");
                Timer timer = new Timer(3000, new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        tipsTextField.setText("Tips: 模板编辑中");
                    }
                });
                timer.setRepeats(false); // 使计时器只执行一次
                timer.start();
            }
        };
        worker.execute();
    }
}
