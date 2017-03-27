package org.akvo.flow.api.gae;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.PreGet;
import com.google.appengine.api.datastore.PreGetContext;
import com.google.appengine.api.datastore.PostLoadContext;
import com.google.appengine.api.datastore.PostLoad;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;

class DatastoreCallbacks {

    @PreGet
    void preGet(PreGetContext context) {
	MemcacheService ms = MemcacheServiceFactory.getMemcacheService();
	Key key = context.getCurrentElement();
	Entity found = (Entity) ms.get(key);
	if (found != null) {
	    System.out.println(String.format("%s found in cache", key));
	    context.setResultForCurrentElement(found);
	}
    }

    @PostLoad
    public void postLoad(PostLoadContext context) {
	MemcacheService ms = MemcacheServiceFactory.getMemcacheService();
	Entity e = context.getCurrentElement();
	ms.put(e.getKey(), e);
    }
}
