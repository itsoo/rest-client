# RPC 客户端

#### 功能介绍

1. 通过注解启用 RPC 客户端，并可自定义扫描的包路径，默认为标注注解的类所在包（一般情况为 Application.java 包路径）
2. 支持通过配置文件配置服务列表，内置了软负载均衡（支持：轮询 RR、随机 R）
3. 支持 http 连接超时时间设置，请求失败最大重试次数等
4. 支持请求失败且超过最大重试次数后的 fallback 兜底数据响应的方法
5. RPC 客户端底层采用 http 通信方式，支持 Restful 形式的端口调用，并支持 MVC 的标准注解（如：@RequestMapping 等）


#### 使用说明

第一步：加入 maven 依赖（坐标如下）

```xml
<dependency>
  <groupId>com.cupshe</groupId>
  <artifactId>rest-client</artifactId>
  <version>0.0.2-RELEASE</version>
</dependency>
```


第二步：在 application.yml 文件配置远端服务列表

```yaml
# 路由的配置信息
rest-client:
  routers:
    - name: "demo"
      services:
        - "127.0.0.1:8080"
        - "127.0.0.1:8081"
        - "127.0.0.1:8082"
        - "127.0.0.1:8083"
    - name: "test"
      services:
        - "127.0.0.1:8090"
        - "127.0.0.1:8091"
```


第三步：在 SpringBoot 启动类标注 @EnableRestClient，并可通过 basePackages 自定义扫描包路径

```java
@SpringBootApplication
@EnableConfigurationProperties
@EnableRestClient
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class);
    }
}
```


第四步：声明一个接口并标注为 RPC 服务实例，注意注解的 value 属性值不可省略

```java
@RestClient(value = "demo", path = "/api/v1/demo", maxAutoRetries = 3)
public interface DemoProvider {
    @GetMapping("/{id}")
    String findOne(@PathVariable("id") Long id, @RequestParam("title") String title);
}
```


第五步：定义实现类并注入 RPC 服务实例，然后即可按本地方法一样调用了（示例如下）

```java
@Service
public class DemoServiceImpl implements DemoService {
    @Resource
    private DemoProvider demoProvider;

    public String findOne() {
        return demoProvider.findOne(2L, "demo");
    }

    /**
     * 自定义 fallback 兜底方法，如果需要单独处理的话（必须为无参数方法）
     *
     * @return T
     */
    public String fallback() {
        return "test fallback method.";
    }
}
```


#### 注意事项

- fallback 方法接收字符串类型的值，描述为 "@类的全限定名#方法名称" 例如："@com.examples.Demo#abc"
- fallback 的实现方法必须为无参数方法，返回值类型不做约束，也可以是 static 方法
- 接口的实现类是动态代理对象，使用 @Autowired 注解会报红色告警，建议使用 @Resource 注解来避免告警
- 接口的返回参数类型如果包含泛型的，必须指定正确的泛型类型，否则将会在反序列化过程中报错
- @RequestMapping 等注解中 headers 为请求携带的头信息，与标准注解不同
- @RequestMapping 等注解中 params 为请求携带的硬编码参数信息（不解析表达式），与标准注解不同
- @RequestMapping 等注解中 path 与 value 不能同时为空
- @RequestMapping 等注解中 path 与 value 同时设置时 path 权重高于 value
- @RestClient 注解中 name 与 value 不能同时为空
- @RestClient 注解中 name 与 value 同时设置时 name 权重高于 value


#### 测试场景

1. RPC 接口请求参数携带 @RequestBody，以及无注解参数的传值，另外需测试表单提交方式
2. @GetMapping，@PostMapping，@PutMapping，@DeleteMapping，@RequestMapping
3. 返回值类型为 ResponseVO<T>，List<T>，Map<K, V>，Object 其它对象类型
4. 返回值类型为基础数据类型（包装类及 String）
5. 无返回值类型的请求场景（接口方法返回值类型为 void）
6. 无请求参数的请求场景（接口方法无入参）
7. 测试本工程打包为 jar 文件后与各工程的整合，及联调是否可以正常工作
8. 测试标准 URI 路径的处理
