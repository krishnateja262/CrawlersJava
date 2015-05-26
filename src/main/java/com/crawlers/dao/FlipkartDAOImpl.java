package com.crawlers.dao;

import com.crawlers.model.FlipkartCategoryModel;
import com.crawlers.model.FlipkartDAOModel;
import com.mongodb.*;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by krishna on 5/25/15.
 */
public class FlipkartDAOImpl implements FlipkartDAO {


    private static final int stopped = 0;
    private static final int crawling = 1;
    private static final int crawled = 2;

    MongoClient mongoClient;
    DB db;
    DBCollection url_coll;
    DBCollection crawlStatus;
    DBCollection products;
    DBCollection failedUrls;

    public FlipkartDAOImpl() throws UnknownHostException {

        mongoClient = new MongoClient( "localhost" , 27017 );
        db = mongoClient.getDB( "flipkartAPI" );
        url_coll = db.getCollection("java_urls");
        crawlStatus = db.getCollection("java_crawl_status");
        products = db.getCollection("java_products");
        failedUrls= db.getCollection("java_failed_urls");
    }

    @Override
    public boolean insertCategoryUrls(ArrayList<FlipkartCategoryModel> categoryUrls) {

        BulkWriteOperation builder = url_coll.initializeOrderedBulkOperation();
        for(FlipkartCategoryModel categoryModel:categoryUrls){
            BasicDBObject dbObject = new BasicDBObject().append("category",categoryModel.getUrl()).append("crawled",false).append("name",categoryModel.getApiName());
            builder.find(new BasicDBObject("category",categoryModel.getUrl())).upsert().update(new BasicDBObject("$set", dbObject));
        }
        builder.execute();
        crawlStatus.update(new BasicDBObject("name", "status"), new BasicDBObject("$set",new BasicDBObject().append("categoryCrawlStatus",crawled).append("productCrawlStatus",stopped)));
        return true;
    }

    @Override
    public boolean updateCategoryCrawlStatus(String url) {
        url_coll.update(new BasicDBObject("category",url), new BasicDBObject("$set", new BasicDBObject("crawled",true)));
        return true;
    }

    @Override
    public boolean bulkUpsertProducts(ArrayList<BasicDBObject> productObjects) {
        BulkWriteOperation builder = products.initializeOrderedBulkOperation();
        for (BasicDBObject dbObject:productObjects){
            builder.find(new BasicDBObject("productId",dbObject.get("productId"))).upsert().update(new BasicDBObject("$set",dbObject));
        }
        builder.execute();
        return true;
    }

    @Override
    public Map<String, Integer> getCrawlStatus() {

        Map<String,Integer> statuses = new HashMap<>();
        DBObject basicDBObject = crawlStatus.findOne(new BasicDBObject("name", "status"));
        statuses.put("categoryCrawlStatus", (Integer) basicDBObject.get("categoryCrawlStatus"));
        statuses.put("productCrawlStatus", (Integer) basicDBObject.get("productCrawlStatus"));

        return statuses;
    }

    @Override
    public ArrayList<FlipkartCategoryModel> getUnparsedUrls() {
        ArrayList<FlipkartCategoryModel> unparsedUrls = new ArrayList<>();
        DBCursor cursor = url_coll.find(new BasicDBObject("crawled",false));
        while(cursor.hasNext()){
            DBObject dbObject = cursor.next();
            FlipkartCategoryModel model = new FlipkartCategoryModel((String)dbObject.get("category"),(String)dbObject.get("name"));
            unparsedUrls.add(model);
        }
        return unparsedUrls;
    }

    @Override
    public boolean insertCategoryStatus() {
        crawlStatus.update(new BasicDBObject("name", "status"), new BasicDBObject("$set",new BasicDBObject().append("categoryCrawlStatus",crawled).append("productCrawlStatus",stopped)),true,false);
        return false;
    }
}
