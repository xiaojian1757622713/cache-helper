/**  
* @Title: JedisHelperImpl.java
* @Package com.hty.util.jedis.impl
* @Description: TODO
* @author liugang  
* @date 2017年3月13日 下午3:28:02 
*/
package com.hty.util.cachehelper.cacher.impl;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;
import java.util.Set;

import com.hty.util.cachehelper.bean.JedisConfigBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Transaction;

import com.hty.util.cachehelper.SerializeUtil;
import com.hty.util.cachehelper.cacher.JedisCacheHelper;

/**
 * JedisHelperImpl使用有四种模式：<br>
 * 1.普通模式，每次请求都从JedisPool获取Jedis实例；<br>
 * 2.事务模式；<br>
 * 3.管道模式；<br>
 * 4.当前线程Jedis唯一模式，不同于事务模式和管道模式，此模式可同时读写。<br>
 * 以下是写入操作的一些测试数据。<br><br>
 * 使用事务模式的方式：
 * <pre>
 *  jedisHelper.startTransaction();
 *  jedisHelper.写操作();
 *  ...
 *  jedisHelper.commit();
 * </pre>
 * 使用管道模式的方式：
 * <pre>
 *  jedisHelper.openPipeline();
 *  jedisHelper.写操作();
 *  ...
 *  jedisHelper.closePipeline();
 * </pre>
 * 使用本地线程模式的方式：
 * <pre>
 *  jedisHelper.openPipeline();
 *  jedisHelper.写操作();
 *  ...
 *  jedisHelper.closePipeline();
 * </pre>
 * 测试10,000条数据的写入，使用事务耗时105ms，使用管道耗时109ms，使用普通模式耗时26506<br>
 * 测试1000,000条数据的写入，使用事务耗时5322ms，使用管道耗时6235ms，本地线程耗时564592ms
 *
 * @author Hetianyi 2017/12/30
 * @version 1.0
 */
public class JedisHelperImpl implements JedisCacheHelper {

	private static final Logger logger = LoggerFactory.getLogger(JedisHelperImpl.class);

	private JedisPool pool;

	private JedisConfigBean jedisConfigBean;

	/**
	 * 用于事务模式
	 */
	private static ThreadLocal<Transaction> currentTransaction = new ThreadLocal<Transaction>();
	/**
	 * 用于事务模式和管道模式，当事务模式或管道模式开启时绑定到此处
	 */
	private static ThreadLocal<Jedis> currentJedis = new ThreadLocal<Jedis>();
	/**
	 * 用于管道模式
	 */
	private static ThreadLocal<Pipeline> currentPipeline = new ThreadLocal<Pipeline>();
	/**
	 * 本地线程的Jedis实例，用于当前线程Jedis唯一模式
	 */
	private static ThreadLocal<Jedis> currentThreadLocalJedis = new ThreadLocal<Jedis>();
	/**
	 * 当前使用的模式
	 */
	private static ThreadLocal<Integer> mode = new ThreadLocal<Integer>();
	/**
	 * 事务模式
	 */
	private static final Integer MODE_TRANSACTION = 1;
	/**
	 * 管道模式
	 */
	private static final Integer MODE_PIPELINE = 2;
	/**
	 * 本地线程模式
	 */
	private static final Integer MODE_THREAD_LOCAL = 3;
	/**
	 * 普通模式
	 */
	private static final Integer MODE_PLAIN = 4;
	//初始化JedisPool连接池
	private void initJedisPool(JedisConfigBean jedisConfigBean) {
	    this.jedisConfigBean  =jedisConfigBean;
		logger.debug("Initial JedisPool...");
	    logger.debug("redis.pool.maxActive = {}", jedisConfigBean.getMaxActive());
	    logger.debug("redis.pool.maxIdle = {}", jedisConfigBean.getMaxIdle());
	    logger.debug("redis.pool.minIdle = {}", jedisConfigBean.getMinIdle());
	    logger.debug("redis.pool.maxWait = {}", jedisConfigBean.getMaxWait());
	    logger.debug("redis.pool.testOnBorrow = {}", jedisConfigBean.isTestOnBorrow());
	    logger.debug("redis.pool.testOnReturn = {}", jedisConfigBean.isTestOnReturn());
	    logger.debug("redis.pool.maxTotal = {}", jedisConfigBean.getMaxTotal());
	    logger.debug("redis.host = {}", jedisConfigBean.getHost());
	    logger.debug("redis.port = {}", jedisConfigBean.getPort());
	    logger.debug("redis.password = {}", jedisConfigBean.getPassword());
	    logger.debug("redis.default.db = {}", jedisConfigBean.getDefaultDb());
	    
	    JedisPoolConfig config = new JedisPoolConfig();
	    config.setBlockWhenExhausted(true);
	    config.setMaxIdle(jedisConfigBean.getMaxActive());
	    config.setMaxIdle(jedisConfigBean.getMaxIdle());
	    config.setMaxWaitMillis(jedisConfigBean.getMaxWait());
	    config.setTestOnBorrow(jedisConfigBean.isTestOnBorrow());
	    config.setTestOnReturn(jedisConfigBean.isTestOnReturn());
	    config.setMaxTotal(jedisConfigBean.getMaxTotal());
        config.setMinIdle(jedisConfigBean.getMinIdle());
	    pool = new JedisPool(config, jedisConfigBean.getHost(), jedisConfigBean.getPort());
	}
	
	
	/**
	 * 
	 */
	public JedisHelperImpl(JedisConfigBean jedisConfigBean) {
		initJedisPool(jedisConfigBean);
	}
	
	private Integer getCurrentMode() {
		Integer mod = mode.get();
		if(null == mod)
		mode.set(MODE_PLAIN);
		return mode.get();
		
	}
	
	@Override
	public Jedis getNewJedis() {
		Jedis jedis = pool.getResource();
		if (null != jedisConfigBean.getPassword() && !"".equals(jedisConfigBean.getPassword())) {
            jedis.auth(jedisConfigBean.getPassword());
        }
		jedis.select(jedisConfigBean.getDefaultDb());
		return jedis;
	}
	/**
	 * 获得本地线程绑定的Jedis实例
	 * @return
	 */
	private Jedis getCurrentThreadLocalJedis() {
		return currentThreadLocalJedis.get();
	}
	/**
	 * 根据环境获取Jedis实例
	 * @return
	 */
	private Jedis getJedis() {
		logger.debug("当前Jedis资源池信息, 空闲连接数:{}, 活动连接数:{}", pool.getNumIdle(), pool.getNumActive());
		if(null != getCurrentThreadLocalJedis())
			return getCurrentThreadLocalJedis();
		else return getNewJedis();

	}
	private void closeIfNoCurrentJedis(Jedis jedis) {
		if(null == currentThreadLocalJedis.get()) {
			jedis.close();
		}
	}
	@Override
	public boolean boundJedis() {
		if(getCurrentMode() == MODE_THREAD_LOCAL)
			return true;
		if(getCurrentMode() != MODE_PLAIN)
			throw new IllegalStateException("Another mode is working:Mode[" + getCurrentMode() + "]");
		logger.debug("Bind a ThreadLocal Jedis instance.");
		clear();
		mode.set(MODE_THREAD_LOCAL);
		Jedis jedis = getJedis();
		currentThreadLocalJedis.set(jedis);
		return true;
	}
	@Override
	public void unboundJedis() {
		logger.debug("Unbind the ThreadLocal Jedis instance.");
		clear();
	}
	
	/**
	 * 清除本地线程事务、Jedis等
	 */
	public void clear() {
	    logger.debug("Clearing Jedis...");
	    mode.set(MODE_PLAIN);
		if(null != currentTransaction.get()) {
			logger.debug("Found transaction bound in this thread.");
			try {
				logger.debug("Discard current transaction.");
				currentTransaction.get().close();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				currentTransaction.set(null);
			}
		}
		if(null != currentPipeline.get()) {
			logger.debug("Found pipeline bound in this thread.");
			try {
				logger.debug("close current pipeline.");
				currentPipeline.get().close();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				currentPipeline.set(null);
			}
		}
		if(null != currentJedis.get()) {
			logger.debug("Found Jedis instance bound in this thread.");
			try {
				logger.debug("Returning Jedis instance");
				currentJedis.get().close();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				currentJedis.set(null);
			}
		}
		if(null != currentThreadLocalJedis.get()) {
			logger.debug("Found ThreadLocalJedis instance bound in this thread.");
			try {
				logger.debug("Returning Jedis instance");
				currentThreadLocalJedis.get().close();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				currentThreadLocalJedis.set(null);
			}
		}
	}
	/**
	 * 获取当前redis事务，如事务不存在返回null
	 */
	public final Transaction getCurrentTransaction() {
		return currentTransaction.get();
	}
	
	@Override
	public boolean startTransaction() {
		if(getCurrentMode() == MODE_TRANSACTION)
			return false;
		if(getCurrentMode() != MODE_PLAIN)
			throw new IllegalStateException("Another mode is working:Mode[" + getCurrentMode() + "]");
		logger.debug("Start a new transaction.");
		clear();
		mode.set(MODE_TRANSACTION);
		Jedis threadjedis = getNewJedis();
		Transaction transaction = threadjedis.multi();
		currentTransaction.set(transaction);
		currentJedis.set(threadjedis);
		return true;
	}
	
	@Override
	public boolean commit() {
		logger.debug("Committing a transaction.");
		if(null != currentTransaction.get()) {
			currentTransaction.get().exec();
			clear();
			return true;
		} else {
			throw new IllegalStateException("Jedis transaction is not bound in this thread!");
		}
	}
	
	@Override
	public boolean openPipeline() {
		if(getCurrentMode() == MODE_PIPELINE)
			return false;
		if(getCurrentMode() != MODE_PLAIN)
			throw new IllegalStateException("Another mode is working:Mode[" + getCurrentMode() + "]");
		logger.debug("Start a new transaction.");
		clear();
		mode.set(MODE_PIPELINE);
		Jedis threadjedis = getNewJedis();
		Pipeline pipeline = threadjedis.pipelined();
		currentPipeline.set(pipeline);
		currentJedis.set(threadjedis);
		return true;
	}
	
	@Override
	public void sync() {
		if(null != currentPipeline.get()) {
			currentPipeline.get().sync();
		} else {
			logger.error("No Pipeline bound in this thread!");
		}
	}
	
	@Override
	public boolean closePipeline() {
		if(null != currentPipeline.get()) {
			currentPipeline.get().sync();
			clear();
			return true;
		} else {
			logger.error("No Pipeline bound in this thread!");
			return false;
		}
	}
	
	/**
	 * 校验key或者Hash的field，如果key或field为空，则抛出异常
	 * @param keyorfield
	 */
	private void assertKey(byte[] keyorfield) {
		if(null == keyorfield) {
			throw new IllegalArgumentException("Key or field cannot be null!");
		}
	}
	/**
	 * 校验key或者Hash的field，如果key或field为空，则抛出异常
	 * @param keyorfield
	 */
	private void assertKey(String keyorfield) {
		if(null == keyorfield) {
			throw new IllegalArgumentException("Key or field cannot be null!");
		}
	}
	
	@Override
	public boolean discard() {
		logger.debug("About to discard a transaction");
		clear();
		return true;
	}
	
	
	///////////////////////////////////////////////////////////////////////////////////////////
	///////////////////////////////           功能区                ///////////////////////////////////////
	//////////////////////////////////////////////////////////////////////////////////////////
	
	
	@Override
	public String get(String key) {
		assertKey(key);
		Jedis jedis = getJedis();
		String ret = jedis.get(key);
		closeIfNoCurrentJedis(jedis);
		return ret;
	}
	@Override
	public void set(String key, String value) {
		assertKey(key);
		this.setEX(key, value, 0);
	}
	@Override
	public void setEX(String key, String value, int second) {
		assertKey(key);
		this.set(key.getBytes(), value == null ? null : value.getBytes(), second);
	}
	@Override
	public void setObjectEX(String key, Object value, int second) {
		byte[] bs = SerializeUtil.serialize(value);
		this.set(key.getBytes(), bs, second);
	}

	private void set(byte[] key, byte[] value, int second) {
		assertKey(key);
		if(null == value) {
			this.del(key);
			return;
		}
		if(null != currentTransaction.get()) {
			if(second <= 0)
			currentTransaction.get().set(key, value);
			else
			currentTransaction.get().setex(key, second, value);
		} else if(null != currentPipeline.get()) {
			Pipeline pl = currentPipeline.get();
			if(second <= 0)
			pl.set(key, value);
			else
			pl.setex(key, second, value);
		} else {
			Jedis jedis = getJedis();
			if(second <= 0)
			jedis.set(key, value);
			else
			jedis.setex(key, second, value);
			closeIfNoCurrentJedis(jedis);
		}
	}

	@Override
	public List<String> mget(String... keys) {
		if(null == keys || keys.length == 0)
			throw new IllegalArgumentException("Keys cannot be null or empty!");
		Jedis jedis = getJedis();
		List<String> ret = jedis.mget(keys);
		closeIfNoCurrentJedis(jedis);
		return ret;
	}

	///////////////////////////////////////////////////////////////////////////////===String类型的Hash结构开始
	@Override
	public String hget(String key, String field) {
		assertKey(key);
		Jedis jedis = getJedis();
		String ret = jedis.hget(key, field);
		closeIfNoCurrentJedis(jedis);
		return ret;
	}
	
	@Override
	public void hset(String key, String field, String value) {
		assertKey(key);
		assertKey(field);
		if(null == value) {
			this.hdel(key, field);
			return;
		}
		if(null != currentTransaction.get()) {
			currentTransaction.get().hset(key, field, value);
		} else {
			Jedis jedis = getJedis();
			jedis.hset(key, field, value);
			closeIfNoCurrentJedis(jedis);
		}
	}
	///////////////////////////////////////////////////////////////////////////////===String类型的Hash结构结束
	/*@Override
	public <T> T hget(String key, Object field, Class<T> recoverType) {
		byte[] fieldBytes = SerializeUtil.serialize(field);
		byte[] valbytes;
		if(null != currentTransaction.get()) {
			valbytes = currentTransaction.get().get(fieldBytes).get();
		} else {
			Jedis jedis = getJedis();
			valbytes = jedis.hget(key.getBytes(), fieldBytes);
			closeIfNoCurrentJedis(jedis);
		}
		return SerializeUtil.deserialize(valbytes, recoverType);
	}*/
	/**
	 * 私有方法，经过该方法缓存的field/value均需要封装进DataContainer
	 * @param key
	 * @param field
	 * @param value
	 */
	//@Override
	private void hset(String key, byte[] field, byte[] value) {
		assertKey(key);
		assertKey(field);
		if(null == value) {
			this.hdel(key, field);
			return;
		}
		if(null != currentTransaction.get()) {
			currentTransaction.get().hset(key.getBytes(), field, value);
		} else {
			Jedis jedis = getJedis();
			jedis.hset(key.getBytes(), field, value);
			closeIfNoCurrentJedis(jedis);
		}
	}
	///////////////////////////////////////////////////////////////////////////////===整个Map的存取开始
	/*@Override
	public Map<?, ?> getMap(String key) {
		Jedis jedis = getJedis();
		byte[] bs = jedis.get(key.getBytes());
		DataContainer dataContainer = new DataContainer();
		RuntimeSchema<DataContainer> schema = getSchema(DataContainer.class);
		if(null != bs)
			ProtostuffIOUtil.mergeFrom(bs, dataContainer, schema);
		return (Map<?, ?>) dataContainer.getData();
	}
	@Override
	public void setMap(String key, Map<?, ?> map) {
		byte[] bs = SerializeUtil.serialize(map);
		this.set(key.getBytes(), bs, 0);
	}*/
	///////////////////////////////////////////////////////////////////////////////===整个Map的存取结束

	
	@Override
	public void setMap(String key, Map<?, ?> map) {
		assertKey(key);
		if(null == map) {
			this.del(key);
			return;
		}
		for(Entry<?, ?> entry : map.entrySet()) {
			byte[] fieldBytes = SerializeUtil.serialize(entry.getKey());
			byte[] valueBytes = SerializeUtil.serialize(entry.getValue());
			this.hset(key, fieldBytes , valueBytes);
		}
	}
	
	@Override
	public <T> T getMapValue(String mapKey, Object fieldKey, Class<T> type) {
		assertKey(mapKey);
		byte[] fieldBytes = SerializeUtil.serialize(fieldKey);
		assertKey(fieldBytes);
		byte[] valbytes;
		Jedis jedis = getJedis();
		valbytes = jedis.hget(mapKey.getBytes(), fieldBytes);
		closeIfNoCurrentJedis(jedis);
		return SerializeUtil.deserialize(valbytes, type);
	}

	@Override
	public <T> List<T> getMapValues(String mapKey, Class<T> type) {
		assertKey(mapKey);
		Jedis jedis = getJedis();
		List<byte[]> ret = jedis.hvals(mapKey.getBytes());
		closeIfNoCurrentJedis(jedis);
		List<T> list = null;
		if(null != ret) {
			list = new ArrayList<T>();
			for(byte[] bs : ret) {
				T o = SerializeUtil.deserialize(bs, type);
				list.add(o);
			}
		}
		return list;
	}

	@Override
	public void setObject(String key, Object obj) {
		assertKey(key);
		if(null == obj) {
			this.del(key);
			return;
		}
		byte[] bs = SerializeUtil.serialize(obj);
		this.set(key.getBytes(), bs, 0);
	}

	@Override
	public <T> T getObject(String key, Class<T> type) {
		assertKey(key);
		byte[] bs;
		Jedis jedis = getJedis();
		bs = jedis.get(key.getBytes());
		closeIfNoCurrentJedis(jedis);
		return SerializeUtil.deserialize(bs, type);
	}

	@Override
	public void setObjectList(String key, List<?> list) {
		assertKey(key);
		if(null == list) {
			this.del(key);
			return;
		}
		if(existsKey(key)) {
			this.del(key);
		}
		byte[][] bls = new byte[list.size()][];
		for(int i = 0; i < list.size(); i++) {
			byte[] b = SerializeUtil.serialize(list.get(i));
			bls[i] = b;
		}
		if(null != currentTransaction.get()) {
			currentTransaction.get().rpush(key.getBytes(), bls);
		} else {
			Jedis jedis = getJedis();
			jedis.rpush(key.getBytes(), bls);
			closeIfNoCurrentJedis(jedis);
		}
	}

	@Override
	public <T> T getObjectListItem(String key, int index, Class<T> type) {
		assertKey(key);
		Jedis jedis = getJedis();
		byte[] resp = jedis.lindex(key.getBytes(), index);
		closeIfNoCurrentJedis(jedis);
		return SerializeUtil.deserialize(resp, type);
	}

	@Override
	public <T> List<T> getObjectListItems(String key, int start, int end, Class<T> type) {
		assertKey(key);
		List<byte[]> bss;
		Jedis jedis = getJedis();
		bss = jedis.lrange(key.getBytes(), start, end == -1 ? -1 : end - 1);
		closeIfNoCurrentJedis(jedis);
		if(null == bss || bss.isEmpty()) {
			return null;
		} else {
			List<T> ret = new ArrayList<T>();
			for(byte[] bs : bss) {
				ret.add(SerializeUtil.deserialize(bs, type));
			}
			return ret;
		}
	}
	
	@Override
	public void appendObjectListItem(String key, boolean tail, Object... items) {
		assertKey(key);
		byte[][] bls = new byte[items.length][];
		if(null != items && items.length > 0) {
			for(int i = 0; i < items.length; i++) {
				byte[] b = SerializeUtil.serialize(items[i]);
				bls[i] = b;
			}
		} else return;
		if(null != currentTransaction.get()) {
			if(tail)
			currentTransaction.get().rpush(key.getBytes(), bls);
			else
			currentTransaction.get().lpush(key.getBytes(), bls);
		} else {
			Jedis jedis = getJedis();
			if(tail)
			jedis.rpush(key.getBytes(), bls);
			else
			jedis.lpush(key.getBytes(), bls);
			closeIfNoCurrentJedis(jedis);
		}
	}

	@Override
	public void setStringList(String key, List<String> list) {
		assertKey(key);
		if(null == list) {
			this.del(key);
			return;
		}
		if(existsKey(key)) {
			this.del(key);
		}
		if(null != currentTransaction.get()) {
			currentTransaction.get().rpush(key, list.toArray(new String[0]));
		} else {
			Jedis jedis = getJedis();
			jedis.rpush(key, list.toArray(new String[0]));
			closeIfNoCurrentJedis(jedis);
		}
	}
	@Override
	public void appendStringListItem(String key, boolean tail, String... strings) {
		assertKey(key);
		if(null == strings || strings.length == 0)
			return;
		if(null != currentTransaction.get()) {
			if(tail)
			currentTransaction.get().rpush(key, strings);
			else
			currentTransaction.get().rpush(key, strings);
		} else {
			Jedis jedis = getJedis();
			if(tail)
			jedis.rpush(key, strings);
			else
			jedis.lpush(key, strings);
			closeIfNoCurrentJedis(jedis);
		}
	}
	@Override
	public String getStringListItem(String key, int index) {
		assertKey(key);
		String resp;
		if(null != currentPipeline.get()) {
			Pipeline pl = currentPipeline.get();
			resp = pl.lindex(key, index).get();
		} else {
			Jedis jedis = getJedis();
			resp = jedis.lindex(key, index);
			closeIfNoCurrentJedis(jedis);
		}
		return resp;
	}
	@Override
	public List<String> getStringListItems(String key, int start, int end) {
		assertKey(key);
		List<String> resp;
		Jedis jedis = getJedis();
		resp = jedis.lrange(key, start, end == -1 ? -1 : end - 1);
		closeIfNoCurrentJedis(jedis);
		return resp;
	}
	@Override
	public void trimList(String key, int start, int end) {
		assertKey(key);
		Jedis jedis = getJedis();
		jedis.ltrim(key, start, end == -1 ? -1 : end - 1);
		closeIfNoCurrentJedis(jedis);
	}

	@Override
	public void removeRepeatObjectListItem(String key, int count, Object value) {
		assertKey(key);
		byte[] b = SerializeUtil.serialize(value);
		Jedis jedis = getJedis();
		jedis.lrem(key.getBytes(), count, b);
		closeIfNoCurrentJedis(jedis);
	}
	@Override
	public void removeRepeatStringListItem(String key, int count, String value) {
		assertKey(key);
		Jedis jedis = getJedis();
		jedis.lrem(key, count, value);
		closeIfNoCurrentJedis(jedis);
	}
	@Override
	public long getListLength(String key) {
		assertKey(key);
		Jedis jedis = getJedis();
		Long len = jedis.llen(key);
		closeIfNoCurrentJedis(jedis);
		return len;
	}
	
	
	
	
	@Override
	public <T> void setObjectSet(String key, Set<T> sets) {
		assertKey(key);
		if(null == sets || sets.isEmpty()) {
			this.del(key);
			return;
		}
		if(existsKey(key)) {
			this.del(key);
		}
		byte[][] bls = new byte[sets.size()][];
		int index = 0;
		for(Iterator<?> iterator = sets.iterator(); iterator.hasNext();) {
			byte[] b = SerializeUtil.serialize(iterator.next());
			bls[index++] = b;
		}
		if(null != currentTransaction.get()) {
			currentTransaction.get().sadd(key.getBytes(), bls);
		} else {
			Jedis jedis = getJedis();
			jedis.sadd(key.getBytes(), bls);
			closeIfNoCurrentJedis(jedis);
		}
	}
	@Override
	public <T> Set<T> getObjectSetAll(String key, Class<T> type) {
		assertKey(key);
		Set<byte[]> returnBytes;
		Jedis jedis = getJedis();
		returnBytes = jedis.smembers(key.getBytes());
		closeIfNoCurrentJedis(jedis);
		if(null == returnBytes || returnBytes.isEmpty()) {
			return null;
		}
		Set<T> retSet = new HashSet<T>();
		for(Iterator<byte[]> iterator = returnBytes.iterator(); iterator.hasNext();) {
			T obj = SerializeUtil.deserialize(iterator.next(), type);
			retSet.add(obj);
		}
		return retSet;
	}
	@Override
	public <T> void appendObjectSetMember(String key, T... objects) {
		assertKey(key);
		if(null == objects || objects.length == 0)
			return;
		byte[][] bls = new byte[objects.length][];
		for(int i = 0; i < objects.length; i++) {
			byte[] b = SerializeUtil.serialize(objects[i]);
			bls[i] = b;
		}
		if(null != currentTransaction.get()) {
			currentTransaction.get().sadd(key.getBytes(), bls);
		} else {
			Jedis jedis = getJedis();
			jedis.sadd(key.getBytes(), bls);
			closeIfNoCurrentJedis(jedis);
		}
	}
	@Override
	public <T> void removeObjectSetMember(String key, T... objects) {
		assertKey(key);
		if(null == objects || objects.length == 0)
			return;
		byte[][] bls = new byte[objects.length][];
		for(int i = 0; i < objects.length; i++) {
			byte[] b = SerializeUtil.serialize(objects[i]);
			bls[i] = b;
		}
		Jedis jedis = getJedis();
		jedis.srem(key.getBytes(), bls);
		closeIfNoCurrentJedis(jedis);
	}
	@Override
	public <T> Set<T> getInterObjectSet(Class<T> type, byte[]... keys) {
		if(null == keys || keys.length == 0)
			return null;
		Set<T> ret = new HashSet<T>();
		Jedis jedis = getJedis();
		Set<byte[]> bsset = jedis.sinter(keys);
		closeIfNoCurrentJedis(jedis);
		for(Iterator<byte[]> iterator = bsset.iterator(); iterator.hasNext();) {
			T o = SerializeUtil.deserialize(iterator.next(), type);
			ret.add(o);
		}
		return ret;
	}
	
	
	
	
	@Override
	public void setStringSet(String key, Set<String> sets) {
		assertKey(key);
		if(null == sets || sets.isEmpty()) {
			this.del(key);
			return;
		}
		if(existsKey(key)) {
			this.del(key);
		}
		if(null != currentTransaction.get()) {
			currentTransaction.get().sadd(key, sets.toArray(new String[0]));
		} else {
			Jedis jedis = getJedis();
			jedis.sadd(key, sets.toArray(new String[0]));
			closeIfNoCurrentJedis(jedis);
		}
	}
	@Override
	public Set<String> getStringSetAll(String key) {
		assertKey(key);
		Set<String> returnStrings;
		Jedis jedis = getJedis();
		returnStrings = jedis.smembers(key);
		closeIfNoCurrentJedis(jedis);
		return returnStrings;
	}

	@Override
	public void appendStringSetMember(String key, String... strings) {
		assertKey(key);
		if(null == strings || strings.length == 0)
			return;
		if(null != currentTransaction.get()) {
			currentTransaction.get().sadd(key, strings);
		} else {
			Jedis jedis = getJedis();
			jedis.sadd(key, strings);
			closeIfNoCurrentJedis(jedis);
		}
	}

	@Override
	public void removeStringSetMember(String key, String... strings) {
		assertKey(key);
		if(null == strings || strings.length == 0)
			return;
		Jedis jedis = getJedis();
		jedis.srem(key, strings);
		closeIfNoCurrentJedis(jedis);
	}

	@Override
	public Set<String> getInterStringSet(String... keys) {
		if(null == keys || keys.length == 0)
			return null;
		Jedis jedis = getJedis();
		Set<String> bsset = jedis.sinter(keys);
		closeIfNoCurrentJedis(jedis);
		return bsset;
	}

	@Override
	public boolean existsKey(String key) {
		Jedis jedis = getJedis();
		boolean flag = jedis.exists(key);
		closeIfNoCurrentJedis(jedis);
		return flag;
	}
	@Override
	public void del(String key) {
		this.del(key.getBytes());
	}
	
	private void del(byte[] key) {
		assertKey(key);
		Jedis jedis = getJedis();
		jedis.del(key);
		closeIfNoCurrentJedis(jedis);
	}
	
	@Override
	public void hdel(String key, String field) {
		assertKey(key);
		assertKey(field);
		this.hdel(key, field.getBytes());
	}


	@Override
	public void hdel(String key, Object field) {
		assertKey(key);
		byte[] fieldBytes = SerializeUtil.serialize(field);
		assertKey(fieldBytes);
		this.hdel(key, fieldBytes);
	}
	
	private void hdel(String key, byte[] field) {
		assertKey(key);
		assertKey(field);
		Jedis jedis = getJedis();
		jedis.hdel(key.getBytes(), field);
		closeIfNoCurrentJedis(jedis);
	}

	@Override
	public long getSetSize(String key) {
		assertKey(key);
		Jedis jedis = getJedis();
		long size = jedis.scard(key);
		closeIfNoCurrentJedis(jedis);
		return size;
	}

	@Override
	public void appendMapItem(String mapKey, Object fieldKey, Object value) {
		assertKey(mapKey);
		byte[] fieldBytes = SerializeUtil.serialize(fieldKey);
		byte[] valueBytes = SerializeUtil.serialize(value);
		assertKey(mapKey);
		if(null == valueBytes) {
			this.hdel(mapKey, fieldBytes);
			return;
		}
		this.hset(mapKey, fieldBytes , valueBytes);
	}

	@Override
	public <T> void setSortedObjectSet(String key, Map<Long, T> set) {
		assertKey(key);
		if(null == set || set.isEmpty()) {
			this.del(key);
			return;
		}
		if(existsKey(key)) {
			this.del(key);
		}
		
		if(null != currentTransaction.get()) {
			for(Map.Entry<Long, T> entry : set.entrySet()) {
				Long score = entry.getKey();
				T o = entry.getValue();
				byte[] bs = SerializeUtil.serialize(o);
				currentTransaction.get().zadd(key.getBytes(), Double.valueOf(Long.toString(score)), bs);
			}
		} else {
			Jedis jedis = getJedis();
			for(Map.Entry<Long, T> entry : set.entrySet()) {
				Long score = entry.getKey();
				T o = entry.getValue();
				byte[] bs = SerializeUtil.serialize(o);
				jedis.zadd(key.getBytes(), Double.valueOf(Long.toString(score)), bs);
			}
			closeIfNoCurrentJedis(jedis);
		}
	}



	@Override
	public void setStringSortedObjectSet(String key, Map<Long, String> set) {
		assertKey(key);
		if(null == set || set.isEmpty()) {
			this.del(key);
			return;
		}
		if(existsKey(key)) {
			this.del(key);
		}
		
		if(null != currentTransaction.get()) {
			for(Map.Entry<Long, String> entry : set.entrySet()) {
				Long score = entry.getKey();
				String o = entry.getValue();
				currentTransaction.get().zadd(key, Double.valueOf(Long.toString(score)), o);
			}
		} else {
			Jedis jedis = getJedis();
			for(Map.Entry<Long, String> entry : set.entrySet()) {
				Long score = entry.getKey();
				String o = entry.getValue();
				jedis.zadd(key, Double.valueOf(Long.toString(score)), o);
			}
			closeIfNoCurrentJedis(jedis);
		}
	}

	
	@Override
	public <T> void appendSortedObjectSetMember(String key, Map<Long, T> set) {
		assertKey(key);
		if(null == set || set.isEmpty()) {
			return;
		}
		if(null != currentTransaction.get()) {
			for(Map.Entry<Long, T> entry : set.entrySet()) {
				Long score = entry.getKey();
				T o = entry.getValue();
				byte[] bs = SerializeUtil.serialize(o);
				currentTransaction.get().zadd(key.getBytes(), Double.valueOf(Long.toString(score)), bs);
			}
		} else {
			Jedis jedis = getJedis();
			for(Map.Entry<Long, T> entry : set.entrySet()) {
				Long score = entry.getKey();
				T o = entry.getValue();
				byte[] bs = SerializeUtil.serialize(o);
				jedis.zadd(key.getBytes(), Double.valueOf(Long.toString(score)), bs);
			}
			closeIfNoCurrentJedis(jedis);
		}
	}
	@Override
	public void appendStringSortedObjectSetMember(String key,
			Map<Long, String> set) {
		assertKey(key);
		if(null == set || set.isEmpty()) {
			return;
		}
		if(null != currentTransaction.get()) {
			for(Map.Entry<Long, String> entry : set.entrySet()) {
				Long score = entry.getKey();
				String o = entry.getValue();
				currentTransaction.get().zadd(key, Double.valueOf(Long.toString(score)), o);
			}
		} else {
			Jedis jedis = getJedis();
			for(Map.Entry<Long, String> entry : set.entrySet()) {
				Long score = entry.getKey();
				String o = entry.getValue();
				jedis.zadd(key, Double.valueOf(Long.toString(score)), o);
			}
			closeIfNoCurrentJedis(jedis);
		}
	}

	@Override
	public <T> void removeSortedObjectSetMember(String key, T... members) {
		assertKey(key);
		if(null == members || members.length == 0) {
			return;
		}
		byte[][] bls = new byte[members.length][];
		for(int i = 0; i < members.length; i++) {
			byte[] b = SerializeUtil.serialize(members[i]);
			bls[i] = b;
		}
		Jedis jedis = getJedis();
		jedis.zrem(key.getBytes(), bls);
		closeIfNoCurrentJedis(jedis);
	}

	@Override
	public void removeStringSortedObjectSetMember(String key, String... members) {
		assertKey(key);
		if(null == members || members.length == 0) {
			return;
		}
		Jedis jedis = getJedis();
		jedis.zrem(key, members);
		closeIfNoCurrentJedis(jedis);
	}

	@Override
	public <T> void removeSortedObjectSetMember(String key, long start, long end) {
		assertKey(key);
		Jedis jedis = getJedis();
		jedis.zremrangeByRank(key.getBytes(), start, end == -1 ? -1 : end - 1);
		closeIfNoCurrentJedis(jedis);
	}

	@Override
	public void removeStringSortedObjectSetMember(String key, long start,
			long end) {
		assertKey(key);
		Jedis jedis = getJedis();
		jedis.zremrangeByRank(key, start, end == -1 ? -1 : end - 1);
		closeIfNoCurrentJedis(jedis);
	}

	@Override
	public <T> Long increSortedObjectSetMemberScore(String key, T member,
			long incr) {
		if(null == key || null == member) return 0L;
		Jedis jedis = getJedis();
		byte[] b = SerializeUtil.serialize(member);
		Long ret = Long.valueOf((long) Math.floor(jedis.zincrby(key.getBytes(),  Double.valueOf(incr), b)));
		closeIfNoCurrentJedis(jedis);
		return ret;
	}
	@Override
	public Long increStringSortedObjectSetMemberScore(String key,
			String member, long incr) {
		if(null == key || null == member) return 0L;
		Jedis jedis = getJedis();
		Long ret = Long.valueOf((long) Math.floor(jedis.zincrby(key,  Double.valueOf(incr), member)));
		closeIfNoCurrentJedis(jedis);
		return ret;
	}
	
	@Override
	public <T> List<T> getSortedObjectSetMember(String key, long start, long end, Class<T> type) {
		assertKey(key);
		List<T> ss = null;
		Jedis jedis = getJedis();
		Set<byte[]> bset = jedis.zrange(key.getBytes(), start, end == -1 ? -1 : end - 1);
		closeIfNoCurrentJedis(jedis);
		if(null == bset || bset.isEmpty())
			return null;
		else {
			ss = new ArrayList<T>();
		}
		for(Iterator<byte[]> it = bset.iterator(); it.hasNext();) {
			byte[] bs = it.next();
			T o = SerializeUtil.deserialize(bs, type);
			ss.add(o);
		}
		return ss;
	}
	@Override
	public <T> List<T> getReverseSortedObjectSetMember(String key, long start,
			long end, Class<T> type) {
		assertKey(key);
		List<T> ss = null;
		Jedis jedis = getJedis();
		Set<byte[]> bset = jedis.zrevrange(key.getBytes(), start, end == -1 ? -1 : end - 1);
		closeIfNoCurrentJedis(jedis);
		if(null == bset || bset.isEmpty())
			return null;
		else {
			ss = new ArrayList<T>();
		}
		for(Iterator<byte[]> it = bset.iterator(); it.hasNext();) {
			byte[] bs = it.next();
			T o = SerializeUtil.deserialize(bs, type);
			ss.add(o);
		}
		return ss;
	}

	
	@Override
	public List<String> getStringSortedObjectSetMember(String key, long start,
			long end) {
		assertKey(key);
		List<String> ss = null;
		Jedis jedis = getJedis();
		Set<String> bset = jedis.zrange(key, start, end == -1 ? -1 : end - 1);
		closeIfNoCurrentJedis(jedis);
		if(null == bset || bset.isEmpty())
			return null;
		else {
			ss = new ArrayList<String>();
		}
		for(Iterator<String> it = bset.iterator(); it.hasNext();) {
			ss.add(it.next());
		}
		return ss;
	}

	@Override
	public List<String> getReverseStringSortedObjectSetMember(String key,
			long start, long end) {
		assertKey(key);
		List<String> ss = null;
		Jedis jedis = getJedis();
		Set<String> bset = jedis.zrevrange(key, start, end == -1 ? -1 : end - 1);
		closeIfNoCurrentJedis(jedis);
		if(null == bset || bset.isEmpty())
			return null;
		else {
			ss = new ArrayList<String>();
		}
		for(Iterator<String> it = bset.iterator(); it.hasNext();) {
			ss.add(it.next());
		}
		return ss;
	}
	
	@Override
	public long getSortedSetSize(String key) {
		assertKey(key);
		Jedis jedis = getJedis();
		long size = jedis.zcard(key);
		closeIfNoCurrentJedis(jedis);
		return size;
	}


	
	
	
	
	

	@Override
	public void info() {
		logger.info("当前Jedis资源池信息, 空闲连接数:{}, 活动连接数:{}", pool.getNumIdle(), pool.getNumActive());
	}


	@Override
	public void incr(String key, Long value) {
		assertKey(key);
		if(null != currentTransaction.get()) {
			currentTransaction.get().incrBy(key, value);
		} else {
			Jedis jedis = getJedis();
			jedis.incrBy(key, value);
			closeIfNoCurrentJedis(jedis);
		}
	}


	@Override
	public void hincr(String key, String field, Long value) {
		assertKey(key);
		assertKey(field);
		if(null != currentTransaction.get()) {
			currentTransaction.get().hincrBy(key, field, value);
		} else {
			Jedis jedis = getJedis();
			jedis.hincrBy(key, field, value);
			closeIfNoCurrentJedis(jedis);
		}
	}

	@Override
	public void exKey(String key, int sec) {
		assertKey(key);
		if(null != currentTransaction.get()) {
			currentTransaction.get().expire(key, sec);
		} else {
			Jedis jedis = getJedis();
			jedis.expire(key, sec);
			closeIfNoCurrentJedis(jedis);
		}
	}


	@Override
	public Set<String> keys(String pattern) {
		Jedis jedis = getJedis();
		Set<String> keys = jedis.keys(pattern);
		closeIfNoCurrentJedis(jedis);
		return null == keys ? new HashSet<String>() : keys;
	}
	
	
}
