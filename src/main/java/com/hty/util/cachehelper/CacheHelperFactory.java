/**
 * @author Hetianyi
 * @date 2018/5/5
 */
package com.hty.util.cachehelper;

import java.lang.reflect.Proxy;

import com.hty.util.cachehelper.aop.JedisHelperInvocationHandler;
import com.hty.util.cachehelper.bean.JedisConfigBean;
import com.hty.util.cachehelper.cacher.JedisCacheHelper;
import com.hty.util.cachehelper.cacher.LocalCacheHelper;
import com.hty.util.cachehelper.cacher.impl.LocalCacheHelperImpl;


public class CacheHelperFactory {

    private static JedisCacheHelper jedisHelper;

    private CacheHelperFactory() {
    }

    /**
     * 产生一个代理JedisHelper类
     */
    public static final JedisCacheHelper getJedisCacheHelper(JedisConfigBean jedisConfigBean) {
        if (null == jedisHelper) {
            JedisHelperInvocationHandler jedisHelperInvocationHandler =
                    new JedisHelperInvocationHandler(jedisConfigBean);
            jedisHelper = (JedisCacheHelper) Proxy.newProxyInstance(JedisCacheHelper.class.getClassLoader(),
                    new Class[]{JedisCacheHelper.class},
                    jedisHelperInvocationHandler);
        }
        return jedisHelper;
    }

    /**
     * 产生一个LocalCacheHelper
     */
    public static final LocalCacheHelper getLocalCacheHelper() {
        return LocalCacheHelperImpl.getInstance();
    }
}
