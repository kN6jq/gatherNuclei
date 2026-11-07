package org.jiu.ui;

import org.jiu.ui.component.Icons;
import org.jiu.ui.searchengine.*;

import javax.swing.*;
import java.awt.*;

/**
 * 搜索引擎标签页容器：为 FOFA、Hunter、DayDayMap、RapidDNS、OTX、Zone0、Shodan 各提供独立子标签页。
 */
public class SearchPanel extends JTabbedPane {
    /**
     * 各搜索引擎标签页的图标名，顺序与标签页一致
     */
    private static final String[] ENGINE_ICONS = {
            "fofa", "hunter", "daydaymap", "rapiddns", "otx", "zone0", "shodan"
    };
    private RapidDnsSearchEngine rapidDns = new RapidDnsSearchEngine();
    private FofaSearchEngine fofa = new FofaSearchEngine();
    private HunterSearchEngine hunter = new HunterSearchEngine();
    private DaydaymapEngine daydaymap = new DaydaymapEngine();
    private OtxSearchEngine otx = new OtxSearchEngine();
    private Zone0SearchEngine zone0 = new Zone0SearchEngine();
    private ShodanPortSearchEngine shodanPort = new ShodanPortSearchEngine();

    public SearchPanel() {
        this.addTab(fofa.getTitle(), fofa.getIcon(), fofa, fofa.getTips());
        this.addTab(hunter.getTitle(), hunter.getIcon(), hunter, hunter.getTips());
        this.addTab(daydaymap.getTitle(), daydaymap.getIcon(), daydaymap, daydaymap.getTips());
        this.addTab(rapidDns.getTitle(), rapidDns.getIcon(), rapidDns, rapidDns.getTips());
        this.addTab(otx.getTitle(), otx.getIcon(), otx, otx.getTips());
        this.addTab(zone0.getTitle(), zone0.getIcon(), zone0, zone0.getTips());
        this.addTab(shodanPort.getTitle(), shodanPort.getIcon(), shodanPort, shodanPort.getTips());

        // 统一标签页外观，并按选中状态着色图标
        InitUI.styleTabbedPane(this);
        Icons.applyTabIcons(this, ENGINE_ICONS);
    }

    /**
     * 跳转到「搜索引擎」标签页下的 Shodan 子页；找不到主窗口时不做任何事。
     */
    public static void switchToShodanPort() {
        JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(getSearchPanelInstance());
        if (frame != null) {
            InitUI.getMainTabbedPane().setSelectedIndex(2); // 搜索标签页在索引2
            // 延迟切换到ShodanPort子标签页，等待主标签页切换完成
            SwingUtilities.invokeLater(() -> {
                SearchPanel searchPanel = getSearchPanelInstance();
                if (searchPanel != null) {
                    // ShodanPort是第7个tab，索引为6
                    searchPanel.setSelectedIndex(6);
                }
            });
        }
    }

    /**
     * 遍历主窗口的标签页查找 SearchPanel 实例，未找到时返回 null。
     */
    private static SearchPanel getSearchPanelInstance() {
        JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(InitUI.getMainTabbedPane());
        if (frame != null) {
            Component[] components = frame.getContentPane().getComponents();
            for (Component comp : components) {
                if (comp instanceof JTabbedPane) {
                    JTabbedPane tabbedPane = (JTabbedPane) comp;
                    for (int i = 0; i < tabbedPane.getTabCount(); i++) {
                        if (tabbedPane.getComponentAt(i) instanceof SearchPanel) {
                            return (SearchPanel) tabbedPane.getComponentAt(i);
                        }
                    }
                }
            }
        }
        return null;
    }
}
