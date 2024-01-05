package org.jiu.ui;

import org.jiu.ui.searchengine.FofaSearchEngine;
import org.jiu.ui.searchengine.HunterSearchEngine;
import org.jiu.ui.searchengine.OtxSearchEngine;
import org.jiu.ui.searchengine.Zone0SearchEngine;

import javax.swing.*;

public class SearchPanel extends JTabbedPane {
    private FofaSearchEngine fofa = new FofaSearchEngine();
    private HunterSearchEngine hunter = new HunterSearchEngine();
    private OtxSearchEngine otx = new OtxSearchEngine();
    private Zone0SearchEngine zone0 = new Zone0SearchEngine();

    public SearchPanel() {
        this.addTab(fofa.getTitle(), fofa.getIcon(), fofa, fofa.getTips());
        this.addTab(hunter.getTitle(), hunter.getIcon(), hunter, hunter.getTips());
        this.addTab(otx.getTitle(), otx.getIcon(), otx, otx.getTips());
        this.addTab(zone0.getTitle(), zone0.getIcon(), zone0, zone0.getTips());
    }
}
