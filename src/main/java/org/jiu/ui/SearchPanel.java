package org.jiu.ui;

import org.jiu.ui.searchengine.*;

import javax.swing.*;
import java.awt.*;

public class SearchPanel extends JTabbedPane {
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
    }

    /**
     * 切换到ShodanPort标签页
     */
    public static void switchToShodanPort() {
        // 获取当前活动窗口的SearchPanel实例
        JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(getSearchPanelInstance());
        if (frame != null) {
            // 切换到搜索标签页
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
     * 获取SearchPanel实例
     */
    private static SearchPanel getSearchPanelInstance() {
        JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(InitUI.getMainTabbedPane());
        if (frame != null) {
            Component[] components = frame.getContentPane().getComponents();
            for (Component comp : components) {
                if (comp instanceof JTabbedPane) {
                    JTabbedPane tabbedPane = (JTabbedPane) comp;
                    // 查找包含ShodanPort的标签页
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
