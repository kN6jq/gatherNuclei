package org.jiu.ui.searchengine;

import com.formdev.flatlaf.extras.FlatSVGIcon;

/**
 * 搜索引擎面板的统一接口，提供标题、图标与提示信息供主界面标签页展示。
 */
public interface SearchEngine {
    String getTitle();

    FlatSVGIcon getIcon();

    String getTips();
}

/**
 * 单个结果标签页的分页状态；字段用单元素数组，便于在 lambda 中就地修改。
 */
class PageState {
    int[] currentPage = {1};
    int[] totalPages = {1};
    String[] lastQuery = {""};
    String[] lastSearchType = {""}; // Zone0特殊：有site和apk两种搜索类型
}
