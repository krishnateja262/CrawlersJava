package com.crawlers;

import com.crawlers.model.ConcurrentModel;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mongodb.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Author : krishna teja nemani
 *
 */
public class App 
{

    private static final int stopped = 0;
    private static final int crawling = 1;
    private static final int crawled = 2;

    private static final Logger logger = LoggerFactory.getLogger(App.class);

    public static Document getUrlDocument(String body) throws IOException, ParserConfigurationException, SAXException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(body.getBytes("UTF-8"));
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        return documentBuilder.parse(inputStream);
    }

    public static ArrayList<String> getNodeValues(Document doc, String tag){
        NodeList nodeList = doc.getElementsByTagName(tag);
        ArrayList<String> categorySitemapUrls = new ArrayList<String>();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            categorySitemapUrls.add(node.getTextContent());
        }
        return categorySitemapUrls;
    }

    public static void main( String[] args ) throws UnknownHostException {

        logger.info("Starting snapdeal parser");

        MongoClient mongoClient = new MongoClient( "localhost" , 27017 );
        DB db = mongoClient.getDB( "snapdealSitemap" );
        DBCollection coll = db.getCollection("java_urls");
        DBCollection crawlStatus = db.getCollection("java_crawl_status");
        DBCollection products = db.getCollection("java_products");
        DBCollection failedUrls= db.getCollection("java_failed_urls");

        DBCursor cursor = crawlStatus.find(new BasicDBObject("name", "status"));
        int productUrlState = 0;
        int crawlingState = 0;

        if(cursor.hasNext()) {
            while (cursor.hasNext()) {
                DBObject statusObject = cursor.next();
                productUrlState = Integer.parseInt(statusObject.get("productUrlState").toString());
                crawlingState = Integer.parseInt(statusObject.get("crawlingState").toString());
            }
        }else{
            DBObject modifiedCrawlStatus = new BasicDBObject();
            modifiedCrawlStatus.put("$set", new BasicDBObject().append("productUrlState", stopped).append("crawlingState", stopped));
            crawlStatus.update(new BasicDBObject("name", "status"), modifiedCrawlStatus, true, false);
        }
        if(productUrlState<2) {

            DBObject modifiedCrawlStatus = new BasicDBObject();
            modifiedCrawlStatus.put("$set", new BasicDBObject().append("productUrlState", crawling).append("crawlingState", stopped));
            crawlStatus.update(new BasicDBObject("name", "status"), modifiedCrawlStatus, true, false);

            HttpResponse<String> sitemapResponse = null;
            try {
                sitemapResponse = Unirest.get("http://www.snapdeal.com/sitemap/sitemap.xml").asString();
            } catch (UnirestException e) {
                e.printStackTrace();
            }

            if (sitemapResponse == null) {
                logger.info("Received null response from starting sitemap page");
                System.exit(0);
            }

            ArrayList<String> categoryUrls = null;

            try {
                categoryUrls = getNodeValues(getUrlDocument(sitemapResponse.getBody()), "loc");
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
            } catch (SAXException e) {
                e.printStackTrace();
            }

            if (categoryUrls == null) {
                logger.error("Bad Xml parsing for category urls");
                System.exit(0);
            }

            logger.info("Total number of category urls:"+categoryUrls.size());

            Long start = System.currentTimeMillis();

            for (String url : categoryUrls) {

                BulkWriteOperation builder = coll.initializeOrderedBulkOperation();

                ArrayList<String> productUrls = null;

                try {
                    productUrls = getNodeValues(getUrlDocument(Unirest.get(url).asString().getBody()), "loc");
                    BasicDBObject doc = new BasicDBObject("urls", productUrls).append("crawled", false);
                    coll.insert(doc);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ParserConfigurationException e) {
                    e.printStackTrace();
                } catch (SAXException e) {
                    e.printStackTrace();
                } catch (UnirestException e) {
                    e.printStackTrace();
                }

                if (productUrls == null) {
                    logger.error("unable to fetch product urls for this category:"+url);
                    System.exit(0);
                }

                logger.info(productUrls.size()+": Inserted these urls");
            }//end of for loop

            DBObject modifiedCrawlStatus1 = new BasicDBObject();
            modifiedCrawlStatus1.put("$set", new BasicDBObject().append("productUrlState", crawled).append("crawlingState", stopped));
            crawlStatus.update(new BasicDBObject("name", "status"), modifiedCrawlStatus1, true, false);

            logger.info(((System.currentTimeMillis() - start) / (1000 * 60)) + ":minutes");
        }
        if(crawlingState < 2){
            logger.info("Started crawling!!");

            DBCursor unparsedUrls = coll.find(new BasicDBObject("crawled",false)).addOption(Bytes.QUERYOPTION_NOTIMEOUT);

            DBObject modifiedCrawlStatus = new BasicDBObject();
            modifiedCrawlStatus.put("$set", new BasicDBObject().append("productUrlState", crawled).append("crawlingState", crawling));
            crawlStatus.update(new BasicDBObject("name", "status"), modifiedCrawlStatus, true, false);

            if(unparsedUrls.hasNext()){
                while (unparsedUrls.hasNext()){
                    DBObject object = unparsedUrls.next();
                    ArrayList<String> productUrls = (ArrayList<String>) object.get("urls");
                    BulkWriteOperation builder = products.initializeOrderedBulkOperation();
                    ConcurrentModel concurrentModel = ConcurrentAsyncHttpClient.getUrlBody(productUrls);

                    Iterator productsIterator = concurrentModel.getConcurrentLinkedQueue().iterator();
                    boolean hasNext = false;
                    while (productsIterator.hasNext()){
                        hasNext = true;
                        BasicDBObject dbObject = (BasicDBObject) productsIterator.next();
                        builder.find(new BasicDBObject("url", dbObject.get("url").toString())).upsert().update(new BasicDBObject("$set", dbObject));
                    }

                    if(hasNext){
                        builder.execute();
                        DBObject modifiedState = new BasicDBObject();
                        modifiedState.put("$set",new BasicDBObject().append("crawled",true));
                        coll.update(object,modifiedState);
                    }

                    Iterator failedUrlIterator = concurrentModel.getUrlFailedConcurrentLinkedQueue().iterator();
                    while (failedUrlIterator.hasNext()){
                        BasicDBObject failedObject = new BasicDBObject().append("url",failedUrlIterator.next().toString());
                        failedUrls.insert(failedObject);
                    }
                }
            }
            logger.info("Crawling Done!!");

        }//main crawling block
    }
}
