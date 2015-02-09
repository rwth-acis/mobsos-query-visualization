package i5.las2peer.services.queryVisualization.caching;

import i5.las2peer.services.queryVisualization.encoding.MethodResult;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.MapMaker;

/**
 * MethodResultCache.java
 *<br>
 * A cache that provides methods to store and fetch MethodResults.
 * Note that this does not store anything in some database, it is just a cache.
 * The cache timeout can be set in the service configuration file.
 */
public class MethodResultCache {
	private static MethodResultCache resultCache;
	private ConcurrentMap<String, MethodResult> resultCacheMap = null;
	
	protected MethodResultCache(int timeout) {
		resultCacheMap = new MapMaker().softKeys().softValues().expireAfterWrite(timeout, TimeUnit.MINUTES).makeMap();
	}
	
	public static MethodResultCache getInstance(int timeout) {
		if(resultCache == null) {
			resultCache = new MethodResultCache(timeout);
		}
		
		return resultCache;
	}
	
	/**
	 * Caches the methodResult
	 * 
	 * @param key the key to the result
	 * @param methodResult the methodResult to be stored
	 */
	public synchronized void cache(String key, MethodResult methodResult) {
		try {
			resultCacheMap.put(key, methodResult);
		}
		catch(Exception e) {
			System.out.println("MethodResultCache caused an Exeception: " + e.getMessage());
		}
	}
	
	/**
	 * Returns a cached methodResult (if available).
	 * 
	 * @param key the key to the method result
	 * 
	 * @return the cached methodResult
	 */
	public synchronized MethodResult get(String key) {
		try {
			return resultCacheMap.get(key);
		}
		catch(Exception e) {
			System.out.println("MethodResultCache caused an Exeception: " + e.getMessage());
		}
		
		return null;
	}
}
