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
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;

/**
 * YAML 模板编辑面板：提供语法高亮编辑器、新建与保存工具栏、快捷键及状态栏。
 */
public class YamlPanel extends JPanel {
    private final Font editorFont = new Font("Consolas", Font.PLAIN, 14);
    private final Color statusBarBackground = new Color(235, 235, 235);
    private RSyntaxTextArea textArea;
    private String absolutePath;
    private JButton addTemplateButton;
    private JButton saveTemplateButton;
    private JTextField statusField;

    public YamlPanel() {
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(0, 0, 0, 0));
        initComponents();
    }

    private void initComponents() {
        initToolBar();

        initEditor();

        initStatusBar();

        registerShortcuts();
    }

    private void initToolBar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY),
                new EmptyBorder(3, 5, 3, 5)
        ));

        addTemplateButton = createToolBarButton("新建模板", "/icons/new.png");
        addTemplateButton.addActionListener(e -> newTemplate());

        saveTemplateButton = createToolBarButton("保存", "/icons/save.png");
        saveTemplateButton.addActionListener(e -> saveFile());

        toolBar.add(addTemplateButton);
        toolBar.addSeparator();
        toolBar.add(saveTemplateButton);

        add(toolBar, BorderLayout.NORTH);
    }

    /**
     * 创建工具栏按钮，图标缩放为 16x16；图标资源缺失时回退为纯文本按钮。
     *
     * @param iconPath classpath 下的图标资源路径，如 /icons/save.png
     */
    private JButton createToolBarButton(String text, String iconPath) {
        JButton button = new JButton(text);
        button.setFocusable(false);
        try {
            ImageIcon icon = new ImageIcon(getClass().getResource(iconPath));
            Image img = icon.getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH);
            button.setIcon(new ImageIcon(img));
        } catch (Exception e) {
            button.setText(text);
        }
        return button;
    }

    /**
     * 创建带行号滚动槽的编辑器并置于面板中部。
     */
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

    /**
     * 配置 YAML 语法高亮、缩进与括号匹配等编辑特性，应用 monokai 主题，
     * 并监听文档变化以刷新状态栏；主题加载失败时仅打印堆栈，保留默认外观。
     */
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

        try {
            Theme theme = Theme.load(getClass().getResourceAsStream(
                    "/org/fife/ui/rsyntaxtextarea/themes/monokai.xml"));
            theme.apply(textArea);
        } catch (IOException e) {
            e.printStackTrace();
        }

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

    /**
     * 构建底部状态栏：左侧状态文本，右侧实时光标行列（对外显示从 1 开始计数）。
     */
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

    /**
     * 注册窗口级快捷键（焦点在本面板所在窗口内即生效）：Ctrl+S 保存、Ctrl+N 新建。
     */
    private void registerShortcuts() {
        KeyStroke ctrlS = KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK);
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(ctrlS, "save");
        getActionMap().put("save", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveFile();
            }
        });

        KeyStroke ctrlN = KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK);
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(ctrlN, "new");
        getActionMap().put("new", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                newTemplate();
            }
        });
    }

    /**
     * 清空编辑器与当前文件路径以新建模板；已有内容时先确认，用户取消则不做任何改动。
     */
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

    /**
     * 保存编辑器内容到当前文件；首次保存需已配置模板目录并输入文件名，同名文件询问是否覆盖。
     * 写盘在 SwingWorker 后台线程执行，完成后回到事件分发线程提示成功或弹窗报错。
     */
    private void saveFile() {
        if (absolutePath == null) {
            if (Utils.templatePath == null || Utils.templatePath.trim().isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "请先在配置面板设置模板路径",
                        "提示",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            String filename = JOptionPane.showInputDialog(this, "请输入文件名:");
            if (filename == null || filename.trim().isEmpty()) {
                return;
            }
            filename = filename.trim();

            // 已带 .yaml 后缀则不重复追加，避免保存成 xxx.yaml.yaml
            String yamlName = filename.toLowerCase().endsWith(".yaml") ? filename : filename + ".yaml";
            String templatePath = Utils.templatePath + File.separator + yamlName;
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

    public void setFilePath(String path) {
        this.absolutePath = path;
        updateStatus("正在编辑: " + path);
    }

    public void clearEditor() {
        textArea.setText("");
        absolutePath = null;
        updateStatus("新建模板");
    }

    public String getEditorContent() {
        return textArea.getText();
    }

    public void setEditorContent(String content) {
        textArea.setText(content);
        updateStatus("正在编辑: " + (absolutePath != null ? absolutePath : "新文件"));
    }

    public String getCurrentFilePath() {
        return absolutePath;
    }
}