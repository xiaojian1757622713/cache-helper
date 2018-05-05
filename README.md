# cache-helper for SpringBoot

在application.yml添加Redis配置信息：

```javascript
redis:
  host: redis.server.com
  port: 6379
  password: ''
  default.db: 0
  pool:
    maxTotal: 50
    maxActive: 1024
    maxIdle: 10
    minIdle: 2
    maxWait: 5000
    testOnBorrow: true
    testOnReturn: false
```

定义配置类:CustomJedisConfig.java
```javascript
@Configuration
public class CustomJedisConfig implements JedisConfigBean {

    @Value("${redis.pool.maxTotal}")
    private int maxTotal = 50;
    @Value("${redis.pool.maxActive}")
    private int maxActive = 1024;
    @Value("${redis.pool.maxIdle}")
    private int maxIdle = 10;
    @Value("${redis.pool.minIdle}")
    private int minIdle = 2;
    @Value("${redis.pool.maxWait}")
    private int maxWait = 5000;
    @Value("${redis.host}")
    private String host = "localhost";
    @Value("${redis.port}")
    private int port = 6379;
    @Value("${redis.default.db}")
    private int defaultDb = 0;
    @Value("${redis.password}")
    private String password = "";
    @Value("${redis.pool.testOnBorrow}")
    private boolean testOnBorrow = true;
    @Value("${redis.pool.testOnReturn}")
    private boolean testOnReturn = false;



    public int getMaxTotal() {
        return maxTotal;
    }

    public int getMaxActive() {
        return maxActive;
    }

    public int getMaxIdle() {
        return maxIdle;
    }

    public int getMinIdle() {
        return minIdle;
    }

    public int getMaxWait() {
        return maxWait;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public int getDefaultDb() {
        return defaultDb;
    }

    public String getPassword() {
        return null == password ? "" : password.trim();
    }

    public boolean isTestOnBorrow() {
        return testOnBorrow;
    }

    public boolean isTestOnReturn() {
        return testOnReturn;
    }
}
```

声明工具类实例：
```javascript
@SpringBootApplication
public class MyApplication {

    @Bean
    @Autowired
    public JedisCacheHelper jedisCacheHelper(CustomJedisConfig customJedisConfig) {
        return CacheHelperFactory.getJedisCacheHelper(customJedisConfig);
    }


    public static void main(String[] args) {
        SpringApplication.run(MyApplication.class, args);
    }
}
```

开始使用：
```javascript
...

@Autowired
private JedisCacheHelper jedisCacheHelper;

...

jedisCacheHelper.setObjectEX("Hello", "Redis", 3000);

```