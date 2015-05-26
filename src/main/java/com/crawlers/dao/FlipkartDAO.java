package com.crawlers.dao;

import com.crawlers.model.FlipkartCategoryModel;
import com.crawlers.model.FlipkartDAOModel;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

import java.util.ArrayList;
import java.util.Map;

/**
 * Created by krishna on 5/25/15.
 */
public interface FlipkartDAO {

    public boolean insertCategoryUrls(ArrayList<FlipkartCategoryModel> categoryUrls);

    public boolean updateCategoryCrawlStatus(String url);

    public boolean bulkUpsertProducts(ArrayList<BasicDBObject> productObjects);

    public Map<String,Integer> getCrawlStatus();

    public ArrayList<FlipkartCategoryModel> getUnparsedUrls();

    public boolean insertCategoryStatus();
}
