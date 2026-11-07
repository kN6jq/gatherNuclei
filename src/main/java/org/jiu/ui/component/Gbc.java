package org.jiu.ui.component;

import java.awt.*;

/**
 * GridBagLayout 约束便捷类，简化网格位置构造并支持链式设置外边距。
 */
public class Gbc extends GridBagConstraints {
    /**
     * gridwidth 与 gridheight 使用默认值 1。
     */
    public Gbc(int gridx, int gridy) {
        this.gridx = gridx;
        this.gridy = gridy;
    }

    public Gbc(int gridx, int gridy, int gridwidth, int gridheight) {
        this.gridx = gridx;
        this.gridy = gridy;
        this.gridwidth = gridwidth;
        this.gridheight = gridheight;
    }

    public Gbc setInsets(int top, int left, int bottom, int right) {
        this.insets = new Insets(top, left, bottom, right);
        return this;
    }
}