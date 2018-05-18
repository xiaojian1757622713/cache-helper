package com.hty.util.cachehelper.cacher;

import redis.clients.jedis.Jedis;

import java.util.Map;
import java.util.Set;

/**
 * JedisHelper封装实现了Jedis常用的一些API，
 * 方便大多数情况下的缓存数据的存取操作(String, Hash, List, Set[无序])，
 * 在少数不满足的情况下需要自行调用Jedis原生的API来进行操作。<br>
 * <pre><strong>注意：所有事务和管道内不可以进行读操作。</strong></pre>
 * @author Hetainyi 2017/12/30
 * @version 1.0
 */
public interface JedisCacheHelper extends CacheHelper {

	void info();
	/**
	 * 标记一个新的事务开始，如果上一次操作的事务未提交的情况下又开启一个新事务，<br>
	 * 则上次的事务回滚，从当前点开启一个新事务。<br>
	 * 同一个事务中只能有读或者写一种操作。
	 */
	boolean startTransaction();
	/**
	 * 提交一个新的事务
	 */
	boolean commit();
	/**
	 * 打开管道
	 */
	boolean openPipeline();
	/**
	 * 管道同步数据
	 */
	void sync();
	/**
	 * 关闭管道
	 */
	boolean closePipeline();
	
	/**
	 * 从JedisPool获取一个新的Jedis实例
	 * @return Jedis
	 */
	Jedis getNewJedis();
	/**
	 * 该方法相当于一个标记命令，将当前线程绑定一个Jedis实例，当前线程所有的操作均由该实例执行，在大量命令下操作减少了Jedis频繁地获取和关闭重置。<br>
	 * <pre><strong>注意：使用此模式不能同时开启事务模式、管道模式和本地线程绑定模式</strong></pre>
	 */
	boolean boundJedis();
	/**
	 * 将本地线程的Jedis解绑，返还给JedisPool
	 */
	void unboundJedis();
	/**
	 * 回滚事务
	 */
	boolean discard();
	
	
	//////////////////////   Object Set

	/**
	 * 获得几个Object类型的集合的交集，并以指定的集合类型返回
	 */
	<T> Set<T> getInterObjectSet(Class<T> type, byte[]... keys) ;

	/**
	 * 获得几个Object类型的集合之间的差集，并以指定的集合类型返回
	 */
	<T> Set<T> getDiffObjectSet(Class<T> type, byte[]... keys);

	/**
	 * 获得几个Object类型的集合之间的并集，并以指定的集合类型返回
	 */
	<T> Set<T> getUnionObjectSet(Class<T> type, byte[]... keys);
	/**
	 * 获得几个Object类型的集合之间的并集，并以指定的集合类型返回
	 */
	int moveObjectSetMember(byte[] source, byte[] dest, Object member);

	/**
	 * 判断 member 元素是否集合 key 的成员。
	 */
	boolean isObjectSetMember(byte[] key, Object member);
	
	
	//////////////////////   String Set
	
	
	/**
	 * 获得几个String类型的集合的交集
	 */
	Set<String> getInterStringSet(String... key) ;
	/**
	 * 获得几个String类型的集合之间的差集
	 */
	Set<String> getDiffStringSet(String... keys);
	/**
	 * 获得几个String类型的集合之间的并集
	 */
	Set<String> getUnionObjectSet(String... keys);
	/**
	 * 获得几个Object类型的集合之间的并集，并以指定的集合类型返回
	 */
	int moveStringSetMember(String source, String dest, String member);
	/**
	 * 判断 member 元素是否集合 key 的成员。
	 */
	boolean isStringSetMember(String key, String member);
	
	///////////////////////////////////////////////////////////// SortedSet
		
	/**
	* 设置有序集合，如果key存在，则覆盖原值<br>
	* 如果一次性添加的元素较多，建议使用Pipe或者事务模式
	*/
	<T> void setSortedObjectSet(String key, Map<Long, T> set);
	/**
	 * 向有序集合添加元素。<br>
	 * 如果key存在且不是SortedSet类型，则抛出异常
	 */
	<T> void appendSortedObjectSetMember(String key, Map<Long, T> set);
	/**
	 * 从有序集合中移除元素
	 */
	<T> void removeSortedObjectSetMember(String key, T... members);
	
	/**
	 * 获取有序集合的大小
	 */
	long getSortedSetSize(String key);
}
