package org.jiu.ui.component;

import java.awt.*;

public class Gbc extends GridBagConstraints {
    /**此构造器gridwidth和gridheight使用默认值1**/
    public Gbc(int gridx,int gridy) {
        this.gridx = gridx;
        this.gridy = gridy;
    }

    public Gbc(int gridx,int gridy,int gridwidth,int gridheight) {
        this.gridx = gridx;
        this.gridy = gridy;
        this.gridwidth = gridwidth;
        this.gridheight = gridheight;
    }
    //设置外边距
    public Gbc setInsets(int top,int left,int bottom,int right) {
        this.insets = new Insets(top, left, bottom, right);
        return this;
    }
}