package org.jiu.ui;

import org.jiu.ui.searchengine.*;

import javax.swing.*;

public class SearchPanel extends JTabbedPane {
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
        this.addTab(otx.getTitle(), otx.getIcon(), otx, otx.getTips());
        this.addTab(zone0.getTitle(), zone0.getIcon(), zone0, zone0.getTips());
        this.addTab(shodanPort.getTitle(), shodanPort.getIcon(), shodanPort, shodanPort.getTips());
    }
}
