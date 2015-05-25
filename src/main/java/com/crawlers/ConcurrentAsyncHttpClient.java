package com.crawlers;

import com.crawlers.model.ConcurrentModel;
import com.crawlers.model.SnapdealModel;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.async.Callback;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.util.JSON;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;

/**
 * Created by krishna on 5/23/15.
 */
public class ConcurrentAsyncHttpClient {

    private static Logger logger = LoggerFactory.getLogger(ConcurrentAsyncHttpClient.class);

    public static void main(final String[] args) throws Exception {
        ArrayList<String> urls = new ArrayList<String>();
        urls.add("http://www.snapdeal.com/product/centy-ritz-bolero-scorpio-innova/411070725");
        urls.add("http://www.snapdeal.com/product/svt-leather-flip-cover-for/681106224250");
        urls.add("http://www.snapdeal.com/product/cubix-ultra-thin-rubberized-matte/642859110852");

    }

    public static ConcurrentModel getUrlBody(ArrayList<String> urls, final DBCollection products){

        Unirest.setTimeouts(100000,100000);
        Unirest.setConcurrency(200,200);

        final ConcurrentModel concurrentModel = new ConcurrentModel();

        final CountDownLatch latch = new CountDownLatch(urls.size());
        final ConcurrentLinkedQueue<BasicDBObject> concurrentLinkedQueue = new ConcurrentLinkedQueue<BasicDBObject>();

        try {
            for (final String url : urls) {
                Unirest.get(url).asStringAsync(new Callback<String>() {
                    public void completed(com.mashape.unirest.http.HttpResponse<String> httpResponse) {
                        SnapdealModel snapdealModel = getProduct(httpResponse.getBody(),url);
                        if(snapdealModel!=null) {
                            JSONObject jsonObject = new JSONObject(snapdealModel);
                            BasicDBObject dbObject = (BasicDBObject) JSON.parse(jsonObject.toString());
                            DBObject modifiedObj = new BasicDBObject("$set",new BasicDBObject(dbObject));
                            products.update(new BasicDBObject("url",url),modifiedObj,true,false);
                            concurrentModel.getConcurrentLinkedQueue().add(dbObject);
                        }else{
                            concurrentModel.getUrlFailedConcurrentLinkedQueue().add(url);
                        }
                        logger.info("Done parsing url:"+url);
                        latch.countDown();
                    }

                    public void failed(UnirestException e) {
                        logger.error("Failed to parse URL:"+url);
                        logger.error(e.getMessage());
                        concurrentModel.getUrlFailedConcurrentLinkedQueue().add(url);
                        latch.countDown();
                    }

                    public void cancelled() {
                        logger.error("Cancelled to parse URL:"+url);
                        concurrentModel.getUrlFailedConcurrentLinkedQueue().add(url);
                        latch.countDown();
                    }
                });
            }
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            logger.info("Done with one category!!!");
            try {
                Unirest.shutdown();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return concurrentModel;
        }
    }

    private static SnapdealModel getProduct(String body, String productUrl) {
        org.jsoup.nodes.Document doc = Jsoup.parse(body);
        Element title = doc.select("h1[itemprop=name]").first();
        Element mrp = doc.select("#selling-price-id").first();
        Element sp = doc.select("#original-price-id").first();
        Element discount = doc.select("#discount-id").first();
        String ratings = doc.select("div.lfloat > div.pdpRatingStars").attr("ratings");
        Element productDetails = doc.select("div.detailssubbox > div.details-content > p").first();
        Element categoriesParent = doc.select("#breadCrumbWrapper2").first();
        Elements categories = categoriesParent.select("div.containerBreadcrumb").select("span");
        boolean soldOut = false;
        ArrayList<String> categoryList = new ArrayList<String>();
        String productId = productUrl.substring(productUrl.lastIndexOf("/")+1);

        for (int i = 0; i < categories.size(); i++) {
            if(categories.get(i)!=null) {
                categoryList.add(categories.get(i).text());
            }else{
                categoryList.add("N/A");
            }
        }

        if(doc.select("div.notifyMe-soldout")!=null && doc.select("div.notifyMe-soldout").text().length()>0){
            soldOut = true;
        }
        return cleanData(title,mrp,sp,discount,ratings,productDetails,soldOut,categoryList,productId,productUrl);
    }

    private static SnapdealModel cleanData(Element title, Element mrp, Element sp, Element discount, String ratings, Element productDetails, boolean soldOut, ArrayList<String> categoryList, String productId, String productUrl) {
        try {
            String title1 = title != null ? title.text() : "N/A";
            int mrp1 = mrp != null ? Integer.parseInt(mrp.text()) : -999;
            int sp1 = sp != null ? Integer.parseInt(sp.text()) : -999;
            String discount1 = discount != null ? discount.text() : "N/A";
            String productDetails1 = productDetails != null ? productDetails.text() : "N/A";
            long prodId = productId != null && productId.length() > 0 ? Long.parseLong(productId.replaceAll(" ", "")) : -999;
            double ratings1 = ratings != null && ratings.length() > 0 ? Double.parseDouble(ratings) : -999;

            return new SnapdealModel(productUrl, title1, mrp1, sp1, discount1, ratings1, productDetails1, categoryList, soldOut, prodId);
        }catch (Exception e){
            logger.error("Exception in parsing html page:"+e.getMessage());
            return null;
        }
    }
}
