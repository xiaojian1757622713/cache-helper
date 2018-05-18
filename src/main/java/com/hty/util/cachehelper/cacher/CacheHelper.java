package com.hty.util.cachehelper.cacher;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface CacheHelper {
	
	/**
	 * 设置key的过期时间
	 * @param key
	 * @param sec
	 */
	void exKey(String key, int sec);
	/**
	 * 设置一个string类型的K-V键值对
	 */
	void set(String key, String value);
	/**
	 * 根据一个string类型的键获取一个string类型的value
	 */
	String get(String key);
	/**
	 * 根据若干个string类型的键获取若干个String类型的value
	 */
	List<String> mget(String... keys);
	
	
	//---Hash(Map)↓---$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$
	
	
	/**
	 * 从Hash结构中获取缓存数据，对应获取方法：hset(String key, String field, String value)
	 * @param key
	 * @param field
	 * @return
	 */
	String hget(String key, String field);
	/**
	 * 设置Hash结构的缓存数据
	 * @param key Hash结构的键
	 * @param field Hash结构的字段
	 * @param value Hash结构字段的值
	 */
	void hset(String key, String field, String value);
	/**
	 * 设置字符串在指定时间后过期
	 * @param key
	 * @param value
	 * @param sec
	 */
	void setEX(String key, String value, int sec);
	/**
	 * 设置一个Object类型的K-V键值对，该键值对在指定秒后过期
	 */
	void setObjectEX(String key, Object value, int second);
	/**
	 * 设置Hash结构的缓存数据
	 * @param key Hash结构的键
	 * @param field Hash结构的字段
	 * @param value Hash结构字段的值
	 */
	//void hset(String key, byte[] field, byte[] value);
	
	/**
	 * 将Map对象缓存，使用第三方的序列化工具将Map序列化为字节数组<br>
	 * 对应的获取方法：getMap(String key)
	 */
	//void setMap(String key, Map<?, ?> map);
	/**
	 * 将Map对象拆分缓存，使用第三方的序列化工具将Map序列化为字节数组。<br>
	 * @param key
	 * @param map
	 * @param split 标志map是否拆分保存
	 */
	void setMap(String key, Map<?, ?> map);
	
	/**
	 * 从缓存中的字节数组恢复成Map对象
	 */
	//Map<?, ?> getMap(String key);
	/**
	 * 从缓存中的字节Map对象根据fieldKey获取map的一个元素。<br>
	 * <strong><i>注意：fieldKey基本类型转成Object类型要精确控制数据类型。<br>
	 * 如fieldKey原本为long类型，如果直接传入8，会转型为Integer类型，导致获取失败，此时要写为"8L"或new Long(8)。
	 * </i></strong>
	 */
	<T> T getMapValue(String mapKey, Object fieldKey, Class<T> type);
	/**
	 * 向指定Hash结构添加键值对
	 */
	void appendMapItem(String mapKey, Object fieldKey, Object value);
	/**
	 * 返回哈希表 key 中所有域的值
	 */
	<T> List<T> getMapValues(String mapKey, Class<T> type);
	
	
	//---Hash(Map)↑---$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$
	
	
	/**
	 * 缓存一个和数据库映射的对象，这个对象有以下要求：<br>
	 * <strong>1).有主键</strong><br>
	 * <strong>2).成员变量必须是基本数据类型，例如int, String, double等</strong><br>
	 * <br>
	 * 在redis中的缓存形式是Hash（哈希表）结构，key为对象类名+对象主键，<br>
	 * 例如：com.xxx.Person.13288<br>
	 * 对应的获取方法：getObject(String key, Class<T> type)
	 */
	void setObject(String key, Object obj);
	/**
	 * 根据key从缓存字节获取对象
	 */
	<T> T getObject(String key, Class<T> type);
	
	
	//---List↓---$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$

	/**
	 * 设置String列表，该操作不会将List中的String封装序列化。<br>
	 * 如果key存在，此设置会将将原key的值覆盖而不是追加。
	 */
	void setStringList(String key, List<String> list);
	/**
	 * 将String数组追加到String列表（追加到尾部）。
	 * @param key
	 * @param tail 是否追加到尾部，true:追加到尾部，false:追加到头部
	 * @param strings
	 */
	void appendStringListItem(String key, boolean tail, String... strings);
	/**
	 * 获取String列表的一个String
	 */
	String getStringListItem(String key, int index);
	/**
	 * 获取String列表的一组String，不包括end<br>
	 * 如果需要返回整个列表，可以将end设置为-1
	 */
	List<String> getStringListItems(String key, int start, int end);
	
	/**
	 * 缓存List，拆分的List使用list结构，可以进行更加细粒度的操作。<br>
	 * 如果key存在，此设置会将将原key的值覆盖而不是追加。
	 * @param key
	 * @param list
	 */
	void setObjectList(String key, List<?> list);

	/**
	 * 从列表中获取一个元素
	 */
	<T> T getObjectListItem(String key, int index, Class<T> type);

	/**
	 * 从列表中获取几个元素，不包括end<br>
	 * 如果需要返回整个列表，可以将end设置为-1
	 */
	<T> List<T> getObjectListItems(String key, int start, int end, Class<T> type);
	/**
	 * 向列表追加若干元素，请确保元素均不为空（追加到尾部）。
	 * @param key
	 * @param tail 是否追加到尾部，true:追加到尾部，false:追加到头部
	 * @param items
	 */
	void appendObjectListItem(String key, boolean tail, Object... items);
	
	/**
	 * 对列表进行裁剪，保留start-end(不包括)
	 */
	void trimList(String key, int start , int end);
	/**
	 * 从String列表移除与value值相同的元素<br>
	 * count 的值可以是以下几种：<br>
	 * 	count > 0 : 从表头开始向表尾搜索，移除与 value 相等的元素，数量为 count 。<br>
	 * 	count < 0 : 从表尾开始向表头搜索，移除与 value 相等的元素，数量为 count 的绝对值。<br>
	 * 	count = 0 : 移除表中所有与 value 相等的值。
	 */
	void removeRepeatStringListItem(String key, int count , String value);
	/**
	 * 从Object列表移除与value值相同的元素<br>
	 * count 的值可以是以下几种：<br>
	 * 	count > 0 : 从表头开始向表尾搜索，移除与 value 相等的元素，数量为 count 。<br>
	 * 	count < 0 : 从表尾开始向表头搜索，移除与 value 相等的元素，数量为 count 的绝对值。<br>
	 * 	count = 0 : 移除表中所有与 value 相等的值。
	 */
	void removeRepeatObjectListItem(String key, int count , Object value);
	
	/**
	 * 获取列表的长度
	 */
	long getListLength(String key);
	
	//---List↑---$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$
	
	
	//////////////////////   Object Set

	
	
	/**
	 * 设置一个无序对象集合，集合的值是不重复的。 <br>
	 * 如果key存在，此设置会将将原key的值覆盖而不是追加。
	 */
	<T> void setObjectSet(String key, Set<T> sets) ;
	/**
	 * 根据key获取对象集合所有元素，并按照指定的类型返回
	 */
	<T> Set<T> getObjectSetAll(String key, Class<T> type) ;
	/**
	 * 向对象集合添加一个元素
	 */
	<T> void appendObjectSetMember(String key, T... objects);
	/**
	 * 从对象集合移除一个元素
	 */
	<T> void removeObjectSetMember(String key, T... objects);
	/**
	 * 获得几个Object类型的集合的交集，并以指定的集合类型返回
	 */
	<T> Set<T> getInterObjectSet(Class<T> type, byte[]... keys);

	
	//////////////////////   String Set
	
	
	/**
	 * 设置一个无序String集合，集合的值是不重复的。 
	 */
	void setStringSet(String key, Set<String> sets) ;
	/**
	 * 根据key获取String集合所有元素，并按照指定的类型返回
	 */
	Set<String> getStringSetAll(String key) ;
	/**
	 * 向String集合添加一个元素
	 */
	void appendStringSetMember(String key, String... strings);
	/**
	 * 从String集合移除一个元素
	 */
	void removeStringSetMember(String key, String... strings);
	
	
	////////  SortedSet
	
	
	<T> void setSortedObjectSet(String key, Map<Long, T> set);
	void setStringSortedObjectSet(String key, Map<Long, String> set);
	/**
	 * 向有序集合添加元素。<br>
	 * 如果key存在且不是SortedSet类型，则抛出异常
	 */
	<T> void appendSortedObjectSetMember(String key, Map<Long, T> set);
	void appendStringSortedObjectSetMember(String key, Map<Long, String> set);
	
	/**
	 * 从有序集合获取一段元素
	 * @param key 键
	 * @param start 起始位置
	 * @param end 终止位置(不包括)
	 * @param type 返回Set元素的类型
	 * @return
	 */
	<T> List<T> getSortedObjectSetMember(String key, long start, long end, Class<T> type);

	List<String> getStringSortedObjectSetMember(String key, long start, long end);

	<T> List<T> getReverseSortedObjectSetMember(String key, long start, long end, Class<T> type);

	List<String> getReverseStringSortedObjectSetMember(String key, long start, long end);
	/**
	 * 获取有序集合的大小
	 */
	long getSortedSetSize(String key);
	/**
	 * 移除有序集合的元素
	 */
	<T> void removeSortedObjectSetMember(String key, T... members);
	void removeStringSortedObjectSetMember(String key, String... members);
	/**
	 * 移除有序集合的元素
	 */
	<T> void removeSortedObjectSetMember(String key, long start, long end);
	void removeStringSortedObjectSetMember(String key, long start, long end);
	/**
	 * 将有序集合元素的score增加incr
	 */
	<T> Long increSortedObjectSetMemberScore(String key, T member, long incr);
	Long increStringSortedObjectSetMemberScore(String key, String member, long incr);
	
	/**
	 * 检测key是否存在
	 */
	boolean existsKey(String key);
	/**
	 * 获取Set集合的大小（集合的元素数量）
	 */
	long getSetSize(String key);
	
	/**
	 * 根据键删除K-V结构值
	 */
	void del(String key);
	/**
	 * 根据键和字段删除Hash结构值
	 */
	void hdel(String key, String field);
	/**
	 * 删除hash表对象
	 */
	void hdel(String key, Object field);
	/**
	 * 将指定的整数值加上整数value
	 * @param key
	 */
	void incr(String key, Long value);
	/**
	 * 将指定的整数值加上整数value
	 * @param key
	 */
	void hincr(String key, String field, Long value);
	
	/**
	 * 匹配指定模式的key并返回<br>
	 * 注意local和jedis方式pattern写法不同，local要完全按照java正则表达式写法写。
	 * @param pattern
	 * @return
	 */
	Set<String> keys(String pattern) ;
}
