package com.crawlers.model;

import com.mongodb.BasicDBObject;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by krishna on 5/24/15.
 */
public class ConcurrentModel {
    ConcurrentLinkedQueue<BasicDBObject> concurrentLinkedQueue;
    ConcurrentLinkedQueue<String> urlFailedConcurrentLinkedQueue;

    public ConcurrentModel(ConcurrentLinkedQueue<BasicDBObject> concurrentLinkedQueue, ConcurrentLinkedQueue<String> urlFailedConcurrentLinkedQueue) {
        this.concurrentLinkedQueue = concurrentLinkedQueue;
        this.urlFailedConcurrentLinkedQueue = urlFailedConcurrentLinkedQueue;
    }

    public ConcurrentModel() {
        this.concurrentLinkedQueue = new ConcurrentLinkedQueue<BasicDBObject>();
        this.urlFailedConcurrentLinkedQueue = new ConcurrentLinkedQueue<String>();
    }

    public ConcurrentLinkedQueue<BasicDBObject> getConcurrentLinkedQueue() {
        return concurrentLinkedQueue;
    }

    public void setConcurrentLinkedQueue(ConcurrentLinkedQueue<BasicDBObject> concurrentLinkedQueue) {
        this.concurrentLinkedQueue = concurrentLinkedQueue;
    }

    public ConcurrentLinkedQueue<String> getUrlFailedConcurrentLinkedQueue() {
        return urlFailedConcurrentLinkedQueue;
    }

    public void setUrlFailedConcurrentLinkedQueue(ConcurrentLinkedQueue<String> urlFailedConcurrentLinkedQueue) {
        this.urlFailedConcurrentLinkedQueue = urlFailedConcurrentLinkedQueue;
    }
}
