package org.dc.test.ehcache;

import java.io.File;

import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.PersistentUserManagedCache;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.builders.UserManagedCacheBuilder;
import org.ehcache.config.units.EntryUnit;
import org.ehcache.config.units.MemoryUnit;
import org.ehcache.core.spi.service.LocalPersistenceService;
import org.ehcache.impl.config.persistence.DefaultPersistenceConfiguration;
import org.ehcache.impl.config.persistence.UserManagedPersistenceContext;
import org.ehcache.impl.persistence.DefaultLocalPersistenceService;

public class Test1 {
	public static void main(String[] args) {
		/*LocalPersistenceService persistenceService = new DefaultLocalPersistenceService(  
                new DefaultPersistenceConfiguration(new File("C:/Ehcache", "myUserData")));  
  
        PersistentUserManagedCache<String, byte[]> cache = UserManagedCacheBuilder  
                .newUserManagedCacheBuilder(String.class, byte[].class)  
                .with(new UserManagedPersistenceContext<String, byte[]>(  
                        "cache-name", persistenceService)) //通过持久性服务建造一个id缓存——  
                        //这将产生一个更具体的类型:PersistentUserManagedCache  
                .withResourcePools(  
                        ResourcePoolsBuilder.newResourcePoolsBuilder()  
                                .heap(10L, EntryUnit.ENTRIES)  
                                .disk(10L, MemoryUnit.MB, true)).build(true); //声明这里如果数据应该被缓存
  
        // Work with the cache  
        cache.put("42", "The Answer!".getBytes());  
        System.out.println(new String(cache.get("42")));  
        //关闭缓存时不会删除它保存在磁盘上的数据标记为持久性。  
        cache.close();  
        try {  
            //cache.destroy(); //显示的去销毁缓存数据和记录  
        } catch (Exception e) {  
            e.printStackTrace();  
        }  */
        
        
        CacheManager cacheManager = CacheManagerBuilder  
                .newCacheManagerBuilder()  
                .withCache(  
                        "preConfigured",  
                        CacheConfigurationBuilder.newCacheConfigurationBuilder(  
                                Long.class, String.class,  
                                ResourcePoolsBuilder.heap(100)).build())  
                .build(true);  
  
        Cache<Long, String> preConfigured = cacheManager.getCache(  
                "preConfigured", Long.class, String.class);  
  
        Cache<Long, String> myCache = cacheManager.createCache(  
                "myCache",  
                CacheConfigurationBuilder.newCacheConfigurationBuilder(  
                        Long.class, String.class,  
                        ResourcePoolsBuilder.heap(100)).build());  
          
       // preConfigured.put(2L, "hello Ehcache");  
        String value1=preConfigured.get(2L);  
        System.out.println(value1);  
        /*myCache.put(1L, "da one!");  
        String value = myCache.get(1L);  
        System.out.println(value);  */
        cacheManager.close();  
	}
}
