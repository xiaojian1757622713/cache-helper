package com.hty.util.cachehelper.cacher;

import java.util.Map;
import java.util.Set;

import redis.clients.jedis.Jedis;

/**
 * JedisHelper封装实现了Jedis常用的一些API，
 * 方便大多数情况下的缓存数据的存取操作(String, Hash, List, Set[无序])，
 * 在少数不满足的情况下需要自行调用Jedis原生的API来进行操作。<br>
 * <pre><strong>注意：所有事务和管道内不可以进行读操作。</strong></pre>
 * @author Hetainyi
 * @date 2017年3月13日 上午11:28:08
 */
public abstract interface JedisCacheHelper extends CacheHelper {
	public void info();
	/**
	 * 标记一个新的事务开始，如果上一次操作的事务未提交的情况下又开启一个新事务，<br>
	 * 则上次的事务回滚，从当前点开启一个新事务。<br>
	 * 同一个事务中只能有读或者写一种操作。
	 */
	public abstract boolean startTransaction();
	/**
	 * 提交一个新的事务
	 */
	public abstract boolean commit();
	/**
	 * 打开管道
	 * @return
	 */
	public abstract boolean openPipeline();
	/**
	 * 管道同步数据
	 * @return
	 */
	public abstract void sync();
	/**
	 * 关闭管道
	 * @return
	 */
	public abstract boolean closePipeline();
	
	/**
	 * 从JedisPool获取一个新的Jedis实例
	 * @return Jedis
	 */
	public abstract Jedis getNewJedis();
	/**
	 * 该方法相当于一个标记命令，将当前线程绑定一个Jedis实例，当前线程所有的操作均由该实例执行，在大量命令下操作减少了Jedis频繁地获取和关闭重置。<br>
	 * <pre><strong>注意：使用此模式不能同时开启事务模式、管道模式和本地线程绑定模式</strong></pre>
	 * @return
	 */
	public abstract boolean boundJedis();
	/**
	 * 将本地线程的Jedis解绑，返还给JedisPool
	 * @return
	 */
	public abstract void unboundJedis();
	/**
	 * 回滚事务
	 */
	public abstract boolean discard();
	
	
	//////////////////////   Object Set

	/**
	 * 获得几个Object类型的集合的交集，并以指定的集合类型返回
	 * @param <T>
	 * @param key
	 * @return
	 */
	public abstract <T> Set<T> getInterObjectSet(Class<T> type, byte[]... keys) ;
	
	
	//////////////////////   String Set
	
	
	/**
	 * 获得几个String类型的集合的交集
	 * @param key
	 * @return
	 */
	public abstract Set<String> getInterStringSet(String... key) ;
	
	///////////////////////////////////////////////////////////// SortedSet
		
	/**
	* 设置有序集合，如果key存在，则覆盖原值<br>
	* 如果一次性添加的元素较多，建议使用Pipe或者事务模式
	 * @param <T>
	* @param key
	* @param set
	*/
	public abstract <T> void setSortedObjectSet(String key, Map<Long, T> set);
	/**
	 * 向有序集合添加元素。<br>
	 * 如果key存在且不是SortedSet类型，则抛出异常
	 * @param <T>
	 * @param key
	 * @param score
	 * @param object
	 */
	public abstract <T> void appendSortedObjectSetMember(String key, Map<Long, T> set);
	/**
	 * 从有序集合中移除元素
	 * @param <T>
	 * @param key
	 * @param withscore
	 * @param members
	 */
	public abstract <T> void removeSortedObjectSetMember(String key, T... members);
	
	/**
	 * 获取有序集合的大小
	 * @param key
	 * @return
	 */
	public abstract long getSortedSetSize(String key);
}