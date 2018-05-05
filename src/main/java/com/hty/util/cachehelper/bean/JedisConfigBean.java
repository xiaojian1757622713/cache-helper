package com.hty.util.cachehelper.bean;

public interface JedisConfigBean {
    //#最大连接数
    //protected int maxTotal = 50;
    //#控制一个pool可分配多少个jedis实例，通过pool.getResource()来获取；如果赋值为-1，则表示不限制
    //protected int maxActive = 1024;
    //#最大空闲连接数
    //protected int maxIdle = 10;
    //#最小空闲连接数
    //protected int minIdle = 2;
    //#获取连接时的最大等待毫秒数(如果设置为阻塞时BlockWhenExhausted),如果超时就抛异常, 小于零:阻塞不确定的时间,  默认-1
    //protected int maxWait = 5000;
    //服务器地址
    //protected String host = "localhost";
    //#Redis服务端口
    //protected int port = 6379;
    //#默认数据库
    //protected int defaultDb = 0;
    ////#数据库密码
    //protected String password = "";
    //#在获取连接的时候检查有效性
    //protected boolean testOnBorrow = true;
    //#在返回连接的时候检查有效性
    //protected boolean testOnReturn = false;

    int getMaxTotal();

    int getMaxActive();

    int getMaxIdle();

    int getMinIdle();

    int getMaxWait();

    String getHost();

    int getPort();

    int getDefaultDb();

    String getPassword();

    boolean isTestOnBorrow();

    boolean isTestOnReturn();
}
