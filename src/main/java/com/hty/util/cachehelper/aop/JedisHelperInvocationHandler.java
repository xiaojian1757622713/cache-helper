
package com.hty.util.cachehelper.aop;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import com.hty.util.cachehelper.bean.JedisConfigBean;
import com.hty.util.cachehelper.cacher.impl.JedisHelperImpl;

/**
 * 用切面来执行JedisHelper的方法，如果方法执行过程中抛出异常，则回滚事务。
 * @author Hetianyi 2017/12/30
 * @version 1.0
 */
public class JedisHelperInvocationHandler implements InvocationHandler {

    private JedisHelperImpl targetObject;

    public JedisHelperInvocationHandler(JedisConfigBean jedisConfigBean) {
        if (null == targetObject) {
            targetObject = new JedisHelperImpl(jedisConfigBean);
        }
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args)
            throws Throwable {
        Object returnVal;
        try {
            returnVal = method.invoke(targetObject, args);
        } catch (Exception e) {
            Method m3 = JedisHelperImpl.class.getMethod("clear");
            m3.invoke(targetObject);//清除本地线程数据
            throw e;
        }
        return returnVal;
    }

}
