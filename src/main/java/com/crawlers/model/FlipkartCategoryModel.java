package com.crawlers.model;

/**
 * Created by krishna on 5/25/15.
 */
public class FlipkartCategoryModel {

    String url;
    String apiName;

    public FlipkartCategoryModel(String url, String apiName) {
        this.url = url;
        this.apiName = apiName;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getApiName() {
        return apiName;
    }

    public void setApiName(String apiName) {
        this.apiName = apiName;
    }
}
