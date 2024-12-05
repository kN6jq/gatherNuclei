package org.jiu.ui;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.file.FileWriter;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.jiu.utils.Utils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

public class YamlPanel extends JPanel {
    private RSyntaxTextArea textArea;
    private String absolutePath;
    private JButton addTemplateButton;
    private JButton saveTemplateButton;
    private JTextField statusField;
    private final Font editorFont = new Font("Consolas", Font.PLAIN, 14);
    private final Color statusBarBackground = new Color(235, 235, 235);

    public YamlPanel() {
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(0, 0, 0, 0));
        initComponents();
    }

    private void initComponents() {
        // 初始化工具栏
        initToolBar();

        // 初始化编辑器
        initEditor();

        // 初始化状态栏
        initStatusBar();

        // 注册快捷键
        registerShortcuts();
    }

    private void initToolBar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY),
                new EmptyBorder(3, 5, 3, 5)
        ));

        // 新建模板按钮
        addTemplateButton = createToolBarButton("新建模板", "/icons/new.png");
        addTemplateButton.addActionListener(e -> newTemplate());

        // 保存按钮
        saveTemplateButton = createToolBarButton("保存", "/icons/save.png");
        saveTemplateButton.addActionListener(e -> saveFile());

        toolBar.add(addTemplateButton);
        toolBar.addSeparator();
        toolBar.add(saveTemplateButton);

        add(toolBar, BorderLayout.NORTH);
    }

    private JButton createToolBarButton(String text, String iconPath) {
        JButton button = new JButton(text);
        button.setFocusable(false);
        // 如果有图标资源，则设置图标
        try {
            ImageIcon icon = new ImageIcon(getClass().getResource(iconPath));
            Image img = icon.getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH);
            button.setIcon(new ImageIcon(img));
        } catch (Exception e) {
            // 如果没有图标资源，使用文本
            button.setText(text);
        }
        return button;
    }

    private void initEditor() {
        textArea = new RSyntaxTextArea();
        configureTextArea();

        RTextScrollPane scrollPane = new RTextScrollPane(textArea);
        scrollPane.setBorder(null);
        scrollPane.setLineNumbersEnabled(true);
        scrollPane.getGutter().setBackground(new Color(245, 245, 245));
        scrollPane.getGutter().setBorderColor(new Color(220, 220, 220));

        add(scrollPane, BorderLayout.CENTER);
    }

    private void configureTextArea() {
        textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_YAML);
        textArea.setCodeFoldingEnabled(true);
        textArea.setFont(editorFont);
        textArea.setTabSize(2);
        textArea.setAnimateBracketMatching(true);
        textArea.setBracketMatchingEnabled(true);
        textArea.setAutoIndentEnabled(true);
        textArea.setCloseCurlyBraces(true);
        textArea.setPaintTabLines(true);
        textArea.setMarkOccurrences(true);

        // 加载编辑器主题
        try {
            Theme theme = Theme.load(getClass().getResourceAsStream(
                    "/org/fife/ui/rsyntaxtextarea/themes/monokai.xml"));
            theme.apply(textArea);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 添加编辑器监听器
        textArea.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                updateStatus();
            }
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                updateStatus();
            }
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                updateStatus();
            }
        });
    }

    private void initStatusBar() {
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY),
                new EmptyBorder(3, 10, 3, 10)
        ));
        statusBar.setBackground(statusBarBackground);

        statusField = new JTextField("就绪");
        statusField.setEditable(false);
        statusField.setBorder(null);
        statusField.setBackground(statusBarBackground);
        statusField.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));

        // 添加光标位置显示
        JLabel positionLabel = new JLabel("行 1, 列 1");
        positionLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        textArea.addCaretListener(e -> {
            int lineNum = textArea.getCaretLineNumber() + 1;
            int columnNum = textArea.getCaretOffsetFromLineStart() + 1;
            positionLabel.setText(String.format("行 %d, 列 %d", lineNum, columnNum));
        });

        statusBar.add(statusField, BorderLayout.WEST);
        statusBar.add(positionLabel, BorderLayout.EAST);

        add(statusBar, BorderLayout.SOUTH);
    }

    private void registerShortcuts() {
        // Ctrl + S 保存
        KeyStroke ctrlS = KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK);
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(ctrlS, "save");
        getActionMap().put("save", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveFile();
            }
        });

        // Ctrl + N 新建
        KeyStroke ctrlN = KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK);
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(ctrlN, "new");
        getActionMap().put("new", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                newTemplate();
            }
        });
    }

    private void newTemplate() {
        if (!textArea.getText().isEmpty()) {
            int result = JOptionPane.showConfirmDialog(
                    this,
                    "当前文件未保存，是否继续？",
                    "确认",
                    JOptionPane.YES_NO_OPTION
            );
            if (result != JOptionPane.YES_OPTION) {
                return;
            }
        }
        textArea.setText("");
        absolutePath = null;
        updateStatus();
    }

    private void saveFile() {
        if (absolutePath == null) {
            String filename = JOptionPane.showInputDialog(this, "请输入文件名:");
            if (filename == null || filename.trim().isEmpty()) {
                return;
            }

            String templatePath = Utils.templatePath + "\\" + filename + ".yaml";
            if (FileUtil.exist(templatePath)) {
                int result = JOptionPane.showConfirmDialog(
                        this,
                        "文件已存在，是否覆盖？",
                        "确认",
                        JOptionPane.YES_NO_OPTION
                );
                if (result != JOptionPane.YES_OPTION) {
                    return;
                }
            }
            absolutePath = templatePath;
        }

        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                FileWriter writer = new FileWriter(absolutePath);
                writer.write(textArea.getText());
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    updateStatus("文件已保存");
                    Timer timer = new Timer(2000, e -> updateStatus());
                    timer.setRepeats(false);
                    timer.start();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(
                            YamlPanel.this,
                            "保存文件失败: " + ex.getMessage(),
                            "错误",
                            JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        };
        worker.execute();
    }

    private void updateStatus() {
        updateStatus("就绪");
    }

    private void updateStatus(String status) {
        statusField.setText(status);
    }

    public RSyntaxTextArea getTextArea() {
        return textArea;
    }

    public void setContent(String content) {
        textArea.setText(content);
        updateStatus();
    }


    public void setEditorContent(String content) {
        textArea.setText(content);
        updateStatus("正在编辑: " + (absolutePath != null ? absolutePath : "新文件"));
    }

    public void setFilePath(String path) {
        this.absolutePath = path;
        updateStatus("正在编辑: " + path);
    }

    public void clearEditor() {
        textArea.setText("");
        absolutePath = null;
        updateStatus("新建模板");
    }

    // 如果需要获取内容
    public String getEditorContent() {
        return textArea.getText();
    }

    // 如果需要获取当前文件路径
    public String getCurrentFilePath() {
        return absolutePath;
    }
}