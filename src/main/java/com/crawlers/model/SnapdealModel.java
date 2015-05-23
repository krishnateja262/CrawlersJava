package com.crawlers.model;

import java.util.ArrayList;

/**
 * Created by krishna on 5/23/15.
 */
public class SnapdealModel {

    String url;
    String title;
    int mrp;
    int sp;
    String discount;
    double ratings;
    String productDetails;
    ArrayList<String> categories;
    boolean soldOut;
    long productId;

    public SnapdealModel(String url, String title, int mrp, int sp, String discount, double ratings, String productDetails, ArrayList<String> categories, boolean soldOut, long productId) {
        this.url = url;
        this.title = title;
        this.mrp = mrp;
        this.sp = sp;
        this.discount = discount;
        this.ratings = ratings;
        this.productDetails = productDetails;
        this.categories = categories;
        this.soldOut = soldOut;
        this.productId = productId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getMrp() {
        return mrp;
    }

    public void setMrp(int mrp) {
        this.mrp = mrp;
    }

    public int getSp() {
        return sp;
    }

    public void setSp(int sp) {
        this.sp = sp;
    }

    public String getDiscount() {
        return discount;
    }

    public void setDiscount(String discount) {
        this.discount = discount;
    }

    public double getRatings() {
        return ratings;
    }

    public void setRatings(double ratings) {
        this.ratings = ratings;
    }

    public String getProductDetails() {
        return productDetails;
    }

    public void setProductDetails(String productDetails) {
        this.productDetails = productDetails;
    }

    public ArrayList<String> getCategories() {
        return categories;
    }

    public void setCategories(ArrayList<String> categories) {
        this.categories = categories;
    }

    public boolean isSoldOut() {
        return soldOut;
    }

    public void setSoldOut(boolean soldOut) {
        this.soldOut = soldOut;
    }

    public long getProductId() {
        return productId;
    }

    public void setProductId(long productId) {
        this.productId = productId;
    }
}
