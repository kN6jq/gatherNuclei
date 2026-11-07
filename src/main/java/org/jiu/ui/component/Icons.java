package org.jiu.ui.component;

import com.formdev.flatlaf.extras.FlatSVGIcon;

import javax.swing.*;
import java.awt.*;

/**
 * SVG 图标工具类
 * 统一加载 resources/icons 下的 SVG 图标，并按主题着色
 */
public final class Icons {

    /**
     * 标签页图标尺寸
     */
    public static final int TAB_ICON_SIZE = 16;

    /**
     * 选中标签的强调色
     */
    public static final Color SELECTED_COLOR = new Color(0, 123, 255);

    /**
     * 未选中标签的图标色
     */
    public static final Color UNSELECTED_COLOR = new Color(140, 140, 140);

    private Icons() {
        throw new AssertionError("Icons类不应该被实例化");
    }

    /**
     * 应用图标（窗口标题栏与菜单栏使用）
     * 该图标为多色图形，不做单色着色处理
     */
    public static FlatSVGIcon appLogo() {
        return appLogo(32);
    }

    /**
     * 应用图标（多色，不做单色着色）。
     *
     * @param size 边长，单位像素
     */
    public static FlatSVGIcon appLogo(int size) {
        return new FlatSVGIcon("icons/app.svg", size, size);
    }

    /**
     * 创建使用主题前景色的图标
     */
    public static FlatSVGIcon create(String name) {
        return create(name, TAB_ICON_SIZE, null);
    }

    /**
     * 创建指定颜色的图标
     */
    public static FlatSVGIcon create(String name, Color color) {
        return create(name, TAB_ICON_SIZE, color);
    }

    /**
     * 创建图标。
     *
     * @param name  资源名（不含扩展名），位于 icons 目录下
     * @param size  边长，单位像素
     * @param color 指定颜色，为 null 时使用主题前景色
     */
    public static FlatSVGIcon create(String name, int size, Color color) {
        FlatSVGIcon icon = new FlatSVGIcon("icons/" + name + ".svg", size, size);
        icon.setColorFilter(colorFilter(color));
        return icon;
    }

    /**
     * 为选项卡批量设置图标，选中项与未选中项用不同颜色区分，
     * 并注册切换监听在选中变化时自动刷新颜色。
     *
     * @param iconNames 与选项卡顺序对应的图标名
     */
    public static void applyTabIcons(JTabbedPane tabbedPane, String[] iconNames) {
        for (int i = 0; i < iconNames.length && i < tabbedPane.getTabCount(); i++) {
            tabbedPane.setIconAt(i, create(iconNames[i], colorFor(tabbedPane, i)));
        }
        tabbedPane.addChangeListener(e -> refreshTabIcons(tabbedPane, iconNames));
    }

    /**
     * 刷新选项卡图标颜色（选中为强调色，未选中为灰色）
     */
    private static void refreshTabIcons(JTabbedPane tabbedPane, String[] iconNames) {
        for (int i = 0; i < iconNames.length && i < tabbedPane.getTabCount(); i++) {
            tabbedPane.setIconAt(i, create(iconNames[i], colorFor(tabbedPane, i)));
        }
    }

    private static Color colorFor(JTabbedPane tabbedPane, int index) {
        return tabbedPane.getSelectedIndex() == index ? SELECTED_COLOR : UNSELECTED_COLOR;
    }

    /**
     * 把 SVG 中的纯黑色替换为目标颜色，其余颜色保持不变。
     */
    private static FlatSVGIcon.ColorFilter colorFilter(Color target) {
        return new FlatSVGIcon.ColorFilter(color -> {
            if (color == null) {
                return null;
            }
            if (color.getRed() == 0 && color.getGreen() == 0 && color.getBlue() == 0) {
                return target != null ? target : themeForeground();
            }
            return color;
        });
    }

    /**
     * 获取主题前景色，取不到时回退为深灰色。
     */
    private static Color themeForeground() {
        Color color = UIManager.getColor("Label.foreground");
        return color != null ? color : new Color(60, 60, 60);
    }
}
