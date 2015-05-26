package com.crawlers.model;

/**
 * Created by krishna on 5/25/15.
 */
public class FlipkartDAOModel {

    String categoryUrl;
    String crawlingUrl;

    public FlipkartDAOModel(String categoryUrl, String crawlingUrl) {
        this.categoryUrl = categoryUrl;
        this.crawlingUrl = crawlingUrl;
    }

    public String getCategoryUrl() {
        return categoryUrl;
    }

    public void setCategoryUrl(String categoryUrl) {
        this.categoryUrl = categoryUrl;
    }

    public String getCrawlingUrl() {
        return crawlingUrl;
    }

    public void setCrawlingUrl(String crawlingUrl) {
        this.crawlingUrl = crawlingUrl;
    }
}
