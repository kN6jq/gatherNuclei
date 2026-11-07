package org.jiu.ui.searchengine;

import com.formdev.flatlaf.extras.FlatSVGIcon;

public interface SearchEngine {
    String getTitle();

    FlatSVGIcon getIcon();

    String getTips();
}

/**
 * 分页状态Holder类
 */
class PageState {
    int[] currentPage = {1};
    int[] totalPages = {1};
    String[] lastQuery = {""};
    String[] lastSearchType = {""}; // Zone0特殊：有site和apk两种搜索类型
}
