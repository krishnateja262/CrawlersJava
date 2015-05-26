package com.crawlers;

import com.crawlers.dao.FlipkartDAO;
import com.crawlers.dao.FlipkartDAOImpl;
import com.crawlers.model.FlipkartCategoryModel;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.util.JSON;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.CountDownLatch;

/**
 * Created by krishna on 5/25/15.
 */
public class FlipkartApp {

    private static final Logger logger = LoggerFactory.getLogger(FlipkartApp.class);

    private static final String idHeaderName = "Fk-Affiliate-Id";
    private static final String idHeaderValue = "krishnate7";
    private static final String tokenHeaderName = "Fk-Affiliate-Token";
    private static final String tokenHeaderValue = "f31320b91aeb4d01b60be8d87d5d738a";

    public static void main(String[] args) {
        FlipkartDAO flipkartDAO = null;
        Map<String, Integer> statuses = new HashMap<>();
        statuses.put("productCrawlStatus", 0);
        statuses.put("categoryCrawlStatus", 0);
        // this part is for initiating dao layer and generating a crawl status collection if not present
        try {
            flipkartDAO = new FlipkartDAOImpl();
            statuses = flipkartDAO.getCrawlStatus();
        } catch (UnknownHostException e) {
            logger.error(e.getMessage());
            System.exit(0);
        } catch (NullPointerException noCollection) {
            logger.error("The collection doesnt exist");
            if (flipkartDAO != null) {
                flipkartDAO.insertCategoryStatus();
                logger.info("Category status url is stored!!!");
            }
        }
        // if the crawl status is 0 or 1 that is stopped or crawling then reissue to get all category urls
        //should be done every day as the flipkart api keeps changing, change the collection value to 0 or 1 to use this block of code
        if (statuses.get("categoryCrawlStatus") < 2) {
            try {
                HttpResponse<JsonNode> response = Unirest.get("https://affiliate-api.flipkart.net/affiliate/api/krishnate7.json").asJson();
                JSONObject listings = response.getBody().getObject().getJSONObject("apiGroups").getJSONObject("affiliate").getJSONObject("apiListings");
                Iterator apiListIterator = listings.keys();
                ArrayList<FlipkartCategoryModel> categoryModels = new ArrayList<FlipkartCategoryModel>();

                Set<String> keySet = listings.keySet();
                for (String key : keySet) {
                    JSONObject apiObject = listings.getJSONObject(key);
                    String url = apiObject.getJSONObject("availableVariants").getJSONObject("v0.1.0").get("get").toString();
                    String apiName = apiObject.get("apiName").toString();
                    FlipkartCategoryModel model = new FlipkartCategoryModel(url, apiName);
                    categoryModels.add(model);
                }
                flipkartDAO.insertCategoryUrls(categoryModels);
                //we have all the list of category names and their urls
                callAndStoreProducts(categoryModels, flipkartDAO);
            } catch (UnirestException e) {
                e.printStackTrace();
            }
        }
        // if the crawl status is 2 then the products are fetched fro the api's
        if (statuses.get("productCrawlStatus") < 2) {
            //get remaining categories which are to be fetched
            try {
                callAndStoreProducts(flipkartDAO.getUnparsedUrls(), flipkartDAO);
            } catch (UnirestException e) {
                e.printStackTrace();
            }
        }
    }

    private static void callAndStoreProducts(ArrayList<FlipkartCategoryModel> categoryModels, FlipkartDAO flipkartDAO) throws UnirestException {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put(idHeaderName, idHeaderValue);
        headers.put(tokenHeaderName, tokenHeaderValue);
        CountDownLatch latch = new CountDownLatch(categoryModels.size());
        for (FlipkartCategoryModel model : categoryModels) {
            logger.info("Category API name:" + model.getApiName() + " => Calling category:" + model.getUrl());
            recursiveCaller(model.getUrl(), headers, flipkartDAO);
            flipkartDAO.updateCategoryCrawlStatus(model.getUrl());
            logger.info(model.getApiName() + " : category Done!!");
        }
        logger.info("Done!!");
    }

    private static void recursiveCaller(String url, Map<String, String> headers, FlipkartDAO flipkartDAO) throws UnirestException {
        JSONObject object = Unirest.get(url).headers(headers).asJson().getBody().getObject();
        Object nextUrl = object.get("nextUrl");
        if (object.getJSONArray("productInfoList").length() == 0) {
            return;
        } else {
            JSONArray productInfoList = object.getJSONArray("productInfoList");
            ArrayList<BasicDBObject> dbObjects = new ArrayList<>();
            for (int i = 0; i < productInfoList.length(); i++) {
                JSONObject dbObject = (JSONObject) productInfoList.get(i);
                JSONObject jsonObject = (JSONObject) dbObject.get("productBaseInfo");
                String productId = jsonObject.getJSONObject("productIdentifier").getString("productId");
                BasicDBObject basicDBObject = new BasicDBObject().append("productId", productId).append("productBaseInfo", (DBObject) JSON.parse(jsonObject.toString()));
                dbObjects.add(basicDBObject);
            }
            flipkartDAO.bulkUpsertProducts(dbObjects);
            logger.info((String) nextUrl);
            recursiveCaller((String) nextUrl, headers, flipkartDAO);
        }
    }
}
