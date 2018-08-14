package com.ninjaflip.androidrevenge.core.service.cache;

import com.ninjaflip.androidrevenge.beans.containers.FixedSizeLinkedHashMap;
import org.apache.log4j.Logger;
import org.apache.tinkerpop.gremlin.structure.Graph;

import java.util.Map;

/**
 * Created by Solitario on 24/08/2017.
 *
 * cache for graphs
 * Graphs are very expensive to load in memory, especially when user triggers a jstree search.
 * This class makes it possible to keep an instance of graph object in memory and save time and I/O operations
 */
public class GraphCache {
    private final static Logger LOGGER = Logger.getLogger(GraphCache.class);
    private static GraphCache INSTANCE;
    // key = projectuuid, object = graph instance
    private static Map<String , Object> cachedGraphs;
    private static final int MAX_CACHE_SIZE = 5;

    private GraphCache() {
        // cache is a FIFO Map with limited elements (size = MAX_CACHE_SIZE)
        // So until the element number is <= MAX_CACHE_SIZE new elements are simply put in the map.
        // For element number > MAX_CACHE_SIZE the first inserted element is removed and the newest is put in the map
        /*cachedGraphs = new LinkedHashMap<String , Object>(MAX_CACHE_SIZE) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Object> eldest) {
                LOGGER.debug("Removing cached graph...");
                return size() > MAX_CACHE_SIZE;
            }
        };*/
        cachedGraphs = new FixedSizeLinkedHashMap<>(MAX_CACHE_SIZE);
    }

    public static GraphCache getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new GraphCache();
        }
        return INSTANCE;
    }


    public Graph getGraph(String key){
        return (Graph)cachedGraphs.get(key);
    }

    public void cacheGraph(String key,Graph graph){
        cachedGraphs.put(key,graph);
    }

    public void removeGraph(String key){
        cachedGraphs.remove(key);
    }

    public void removeAll(){
        cachedGraphs.clear();
    }
}
