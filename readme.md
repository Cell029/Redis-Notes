# 一、 Redis 基础

## 1. 非关系型数据库（NoSQL）

1. 结构化与非结构化

   - 关系型数据库（SQL）：严格结构化，字段、数据类型、约束必须提前定义
   - 非关系型数据库（NoSQL）：结构松散，可以是键值、文档、列、图等形式

```sql
INSERT INTO user (id, name, age) VALUES (1, '张三', 18);
```

```js
db.users.insert({ name: "李四", email: "lisi@qq.com" });
```

MongoDB 中的插入即便没有age字段也能正常存储，灵活性高

2. 关联与非关联

   - SQL：依靠外键、联合查询维护表与表的关联，数据一致性强，关系清晰，但多表联查复杂、性能瓶颈
   - NoSQL：数据独立存储，若有关系，靠文档嵌套或程序逻辑维护，结构扁平、访问高效，可减少多表联查，但数据冗余明显，修改同步困难

```sql
CREATE TABLE user (id INT PRIMARY KEY, name VARCHAR(50));
/*使用 user 的主键作为 orders 的外键*/
CREATE TABLE orders (id INT PRIMARY KEY, user_id INT, product VARCHAR(50), FOREIGN KEY (user_id) REFERENCES user(id));
```

```js
{
  _id: ObjectId("..."),
  name: "张三",
  orders: [
    { id: 1, product: "手机" },
    { id: 2, product: "电脑" }
  ]
}
```

MongoDB 直接采用嵌套的方式

3. 查询方式

   - SQL：统一 SQL 标准，可跨平台通用，支持条件过滤、聚合统计、分组排序、多表联查，通用性较强
   - NoSQL：每种 NoSQL 都有独立语法，差异大，主要支持键值查找、文档匹配、简单聚合，复杂度有限

```js
db.users.find({ "orders.product": "手机" });
```

4. 事务对比

   - SQL：强事务，遵循ACID，保障数据可靠性
   - NoSQL：一般不支持强事务，强调最终一致性，部分提供弱事务，随没有 SQL 安全，但性能更高

5. 其它差异

存储介质：

- SQL：依赖磁盘存储，数据持久化
- NoSQL：大多依赖内存，追求高性能，当然部分也支持持久化

扩展模式：

- SQL：垂直扩展（升级服务器硬件）
- NoSQL：水平扩展（增加节点集群）

性能：

- SQL：单节点强，但高并发瓶颈明显
- NoSQL：分布式设计，天然支持大并发、大数据

6. 总结

关系型数据库：数据结构严谨，强一致性，适合规范化、支持事务保证线程安全；非关系型数据库：结构灵活、性能极致，适合海量数据、分布式、更适合高并发场景。

****
## 2. 认识 Redis

### 2.1 概念

Redis 是基于内存的高性能键值型非关系数据库（NoSQL），也可用作缓存、消息队列、排行榜等场景。

特征：

- 键值对存储：支持多种数据结构，功能远超简单的 Key-Value 存储
- 高性能：通过内存读写，延迟低
- 单线程架构：核心逻辑使用单线程，保证每个命令的原子性，避免锁竞争
- 多数据结构支持：	如：String、List、Hash、Set、ZSet、Bitmap、HyperLogLog、Geo、Stream
- 持久化机制：支持 RDB（快照）与 AOF（日志）的持久化，防止数据丢失
- 高可用性与分布式：支持主从复制、哨兵、Cluster 集群，适合大规模系统

Redis 常用的数据结构：

- String 字符串（最常用）：缓存单个值、计数器、简单对象
- List 集合：消息队列、任务队列、时间轴
- Hash 哈希：存储对象信息，如用户资料，类似于 Map 结构
- Set 集合：去重、标签系统、抽奖池，无序不可重复性
- ZSet 有序集合：排行榜、权重排序
- ...

Redis 的高性能来源：

使用内存存储，读写速度远超磁盘（主要原因），且使用 IO 多路复用技术以此支持高并发。

****
### 2.2 安装 Redis

本次安装使用的是 Windows 上的 Linux 子系统，通过使用 WSL2 进行模拟 Linux。

步骤：

使用 WSL2 的前提：

1. 开启 CPU 虚拟化（大部分电脑默认开启），打开任务管理器，切换到性能 CPU，查看下方的虚拟化是否显示“已开启”。
2. 打开“启用或关闭 Windows 功能”，勾选“适用于 Linux 的 Windows 子系统”和“虚拟机平台（Virtual Machine Platform）”，然后重启电脑

- 第一步：

以管理员身份打开命令提示符，输入 `wsl --install --web-download`，电脑会自动启用 WSL 相关功能并安装 WSL2 内核（设置默认版本为 WSL2），然后会自动下载并安装 Ubuntu 子系统（默认）

- 第二步：

根据提示给 Ubuntu 系统设置用户名和密码

- 第三步：

打开 PowerShell 终端，输入 `wsl --list -v`，查看当前系统安装的 Linux 子系统列表，同时能够看到它们的运行状态。
也可也通过 `wsl --set-default [子系统名]` 切换。

- 第四步：

通过 PowerShell 上方的下拉列表，选择 Ubuntu 启动，输入 `sudo service redis-server status` 查看 Redis 是否成功启动（`Active: active (running)`）。
然后输入 `redis-cli` 启动 Redis，此时输入界面变成 `127.0.0.1:6379>`，即默认绑定的 ip 和 端口号

- 第五步：
 
修改一些配置，让登录更加方便安全。输入 `sudo nano /etc/redis/redis.conf` 编辑 Redis 配置文件，找到 bind 127.0.0.1 这一行，修改为：

```shell
bind 0.0.0.0  # 允许所有 IP 连接
```

继续在配置文件中找到 `protected-mode yes`，修改为：

```shell
protected-mode no  # 关闭保护模式，允许外部连接
```

找到 `# requirepass foobared` ，去掉注释并修改：

```shell
requirepass 密码  # 设置 Redis 连接密码
```

- 第六步：

配置图形化桌面客户端，地址：[https://github.com/uglide/RedisDesktopManager](https://github.com/uglide/RedisDesktopManager)。下载安装好后，
打开软件，连接 Redis，输入地址（通过 Linux 子系统获取 `ip addr`）与密码，连接成功

通过 select + 数字，切换数据库（默认 0 - 15 号数据库）

```shell
127.0.0.1:6379> AUTH wyt131551
OK
127.0.0.1:6379> select 2
OK
127.0.0.1:6379[2]>
```

****
### 2.3 Redis 常见命令

Redis是典型的key-value数据库，key一般是字符串，而value包含很多不同的数据类型。不同的数据类型对于不同的操作命令，
在官网（ [https://redis.io/commands](https://redis.io/commands)）可以查看到不同的命令。不同类型的命令称为一个group，也可以通过help命令来查看各种不同group的命令：

```redis
127.0.0.1:6379> help
redis-cli 8.0.2
To get help about Redis commands type:
      "help @<group>" to get a list of commands in <group>
      "help <command>" for help on <command>
      "help <tab>" to get a list of possible help topics
      "quit" to exit
To set redis-cli preferences:
      ":set hints" enable online hints
      ":set nohints" disable online hints
Set your preferences in ~/.redisclirc
```

查看某个命令组：

```redis
help @string
help @list
help @set
help @hash
help @zset
help @stream
```

部分特殊组：

```redis
help @connection   # 连接相关命令
help @server       # 服务器管理命令
help @pubsub       # 发布订阅命令
help @cluster      # 集群管理命令
help @scripting    # Lua 脚本相关
```

#### 1. Redis 通用命令

通用指令是部分数据类型的，都可以使用的指令，常见的有：

- KEYS：查看符合模板的所有key
- DEL：删除一个指定的key
- EXISTS：判断key是否存在
- EXPIRE：给一个key设置有效期，有效期到期时该key会被自动删除
- TTL：查看一个KEY的剩余有效期

通过 `help [command]` 可以查看一个命令的具体用法，例如：

```shell
127.0.0.1:6379> help KEYS

  KEYS pattern
  summary: Returns all key names that match a pattern.
  since: 1.0.0
  group: generic
  
127.0.0.1:6379> help EXPIRE

  EXPIRE key seconds [NX|XX|GT|LT]
  summary: Sets the expiration time of a key in seconds.
  since: 1.0.0
  group: generic
  
127.0.0.1:6379> help ttl

  TTL key
  summary: Returns the expiration time in seconds of a key.
  since: 1.0.0
  group: generic
```

****
#### 2. String 类型

String 是 Redis 最基本、最常用的数据类型，一个 key 对应一个 value，value 是字符串（实际底层可存储二进制数据），单个 String 最大存储 512MB；
String 类型是 value 的一种分类 ，表示 value 以 “二进制安全的字符串 / 字节序列” 形式存储（可存文本、数字、二进制数据等 ）。所以根据字符串的格式不同，可以分为3类：

- string：普通字符串
- int：整数类型，可以做自增、自减操作
- float：浮点类型，可以做自增、自减操作

但不管是哪种格式，底层都是字节数组形式存储，只不过是编码方式不同。

```shell
# key 是字符串（"num"），value 是 int 编码（存整数 100）
SET num 100  

# key 是字符串（"code"），value 是 embstr 编码（存短字符串 "ABC"）
SET code "ABC"  

# key 是字符串（"big_data"），value 是 raw 编码（存长文本/二进制）
SET big_data "非常长的字符串..."  
```

##### String的常见命令：

基本存取操作：

- SET key value：设置 key 的值（存在则覆盖）
- GET key：获取 key 的值，即获取 value
- MSET key1 val1 key2 val2 ...：批量设置多个键值对，key1 为 key、val1 为 value
- MGET key1 key2 ...：批量获取多个 key 的值

数值操作（仅适用于数值类型）：

- INCR key：整数值自增 1，若不存在自动创建并赋值 0 后自增
- INCRBY key n：指定步长自增 n
- INCRBYFLOAT key f：指定步长自增浮点数

条件设置与有效期：

- SETNX key value：仅当 key 不存在时设置，常用于分布式锁，例如：SETNX lock "token" → 若 lock 不存在才设置
- SETEX key seconds value：设置键值对并指定有效期（单位秒），例如：SETEX code 60 "1234" → 60 秒后 code 失效

其他常用：

- APPEND key value：追加内容到字符串尾部
- STRLEN key：获取字符串长度
- GETSET key value：设置新值，并返回旧值

##### key 的前缀设计

Redis 由于没有类似 MySQL 的库表概念，所有数据都存储在一个扁平的 Key-Value 空间中，所以难免会出现相同 key 的情况，为了避免不同业务、不同数据类型 Key 冲突，
Redis 便引出了一种前缀设计，通过前缀名对 key 进行区分，层级间的分隔符统一用冒号（:）：

```text
[系统模块]:[业务类型]:[数据类别]:[唯一标识]
```

例如项目名称叫 demo ，有 user 和 product 两种不同类型的数据，则可以这样定义 key：

- user 相关的 key，demo:user:1
- product 相关的 key，demo:product:1

```shell
set demo:user:1 '{"id":1,  "name": "Jack", "age": 21}'
set demo:product:1 '{"id":1,  "name": "小米11", "price": 4999}'
```

并且，在 Redis 的桌面客户端中，还会以相同前缀作为层级结构，让数据看起来层次分明，关系清晰。

****
#### 3. Hash 类型

Redis 的 Hash 是典型的键值对集合结构，类似于 Java 中的 HashMap,每个 Hash 存储多个 field-value 对，适合用来存储对象、结构化数据，并i企鹅每个字段独立操作。

String vs Hash：

- String：对象整体需要存为 JSON 字符串，并且修改字段需整体更新，例如：`SET user:1 "{'id':1,'name':'Tom'}"`
- Hash：每个字段独立存储，支持单独增删改查，效率高，例如：`HSET user:1 id 1 name "Tom"`

Hash 常用名利：

- HSET key field value：添加或者修改hash类型key的field的值
- HGET key field：获取一个hash类型key的field的值
- HMSET：批量添加多个hash类型key的field的值
- HMGET：批量获取多个hash类型key的field的值
- HGETALL：获取一个hash类型的key中的所有的field和value
- HKEYS：获取一个hash类型的key中的所有的field
- HINCRBY:让一个hash类型key的字段值自增并指定步长
- HSETNX：添加一个hash类型的key的field值，前提是这个field不存在，否则不执行

****
#### 4. List 类型

Redis 中的 List 类型与 Java 中的 LinkedList 类似，可以看做是一个双向链表结构，既可以支持正向检索和也可以支持反向检索。特征也与LinkedList类似：
有序、元素可重复、插入与删除速度快，但查询速度较慢。

List的常见命令有：

- LPUSH key element ... ：向列表左侧插入一个或多个元素
- LPOP key：移除并返回列表左侧的第一个元素，没有则返回nil
- RPUSH key element ... ：向列表右侧插入一个或多个元素
- RPOP key：移除并返回列表右侧的第一个元素
- LRANGE key star end：返回一段角标范围内的所有元素
- BLPOP 和 BRPOP：当列表为空时，不立即返回 nil，而是等待一定时间，直到列表有新元素可弹出（或等待超时）

```shell
RPUSH task:queue task1 task2 task3
BLPOP task:queue 5
```

等待最多 5 秒，若有元素进入队列，立即返回；若 5 秒内队列仍为空，返回 nil

```shell
127.0.0.1:6379> BLPOP task:queue 5
1) "task:queue"
2) "task1"
```

```shell
127.0.0.1:6379> BLPOP task:queue 5
(nil)
(5.01s)
```

可以通过 List 结构模拟栈、队列和阻塞队列：

- 栈：入口和出口在一边，即只调用左进左出（右进右出）
- 队列：入口和出口反方向，即左近右出（右进左出）
- 阻塞队列：入口和出口反方向，且出队时调用 BLPOP 或 BRPOP

****
#### 5. Set 类型

Redis 的 Set 结构与 Java 中的 HashSet 类似，可以看做是一个 value 为 null 的 HashMap（Set 不需要存元素对应的具体值，可以理解为 value 固定为 null），因为也是一个 hash 表，因此与 HashSet 有类似的特征：无序、元素不可重复（Set 的 元素不可重复等价于 HashMap 的 key 不可重复）、查找快等。

例如：

- 当执行 `SADD myset "a" "b"` 时，Redis 会把 "a"、"b" 作为哈希表的 key，对应的 value 固定填一个空值（类似 null）。
- 此时哈希表的 key 唯一性就保证了 Set 的 元素不可重复。
- 哈希表的快速查找特性（O (1) 复杂度），也让 Set 的 SISMEMBER（判断元素是否存在）操作非常高效。


Set的常见命令有：

- SADD key member ... ：向 set 中添加一个或多个元素
- SREM key member ... : 移除 set 中的指定元素
- SCARD key： 返回 set 中元素的个数
- SISMEMBER key member：判断一个元素是否存在于 set 中
- SMEMBERS：获取 set 中的所有元素
- SINTER key1 key2 ... ：求 key1 与 key2 的交集

```shell
SADD user:1:tags "旅游" "摄影" "健身"
SADD user:2:tags "旅游" "美食"
```

****
#### 6. Sorted Set 类型

Redis 的 SortedSet 是一个可排序的 set 集合，与 Java 中的 TreeSet 有些类似，但底层数据结构却差别很大。
SortedSet 中的每一个元素都带有一个 score 属性，可以基于 score 属性对元素排序，底层的实现是一个跳表（SkipList）加 hash 表

SortedSet的常见命令有：

- ZADD key score member：添加一个或多个元素到 sorted set ，如果已经存在则更新其score值
- ZREM key member：删除sorted set中的一个指定元素
- ZSCORE key member : 获取sorted set中的指定元素的score值
- ZRANK key member：获取sorted set 中的指定元素的排名
- ZCARD key：获取sorted set中的元素个数
- ZCOUNT key min max：统计score值在给定范围内的所有元素的个数
- ZINCRBY key increment member：让sorted set中的指定元素自增，步长为指定的increment值
- ZRANGE key min max：按照score排序后，获取指定排名范围内的元素
- ZRANGEBYSCORE key min max：按照score排序后，获取指定score范围内的元素
- ZDIFF、ZINTER、ZUNION：求差集、交集、并集

注意：所有的排名默认都是升序，如果要降序则在命令的 Z 后面添加 REV 即可，例如：

- **升序**获取 sorted set 中的指定元素的排名：ZRANK key member 
- **降序**获取 sorted set 中的指定元素的排名：ZREVRANK key memeber

```shell
ZADD game:rank 500 "Tom" 800 "Jerry" 700 "Mike" 400 "Amy" 300 "Lucy"
ZREVRANGE game:rank 0 2 WITHSCORES  # 获取前三
```

```shell
127.0.0.1:6379> ZREVRANGE game:rank 0 2 WITHSCORES
1) "Jerry"
2) "800"
3) "Mike"
4) "700"
5) "Tom"
6) "500"
```

****
## 3. Redis 的 Java 客户端

### 3.1 Jedis 客户端

Jedis的官网地址： [https://github.com/redis/jedis](https://github.com/redis/jedis)

#### 1. 快速入门

- 第一步：引入依赖

```xml
<dependency>
   <groupId>redis.clients</groupId>
   <artifactId>jedis</artifactId>
   <version>6.0.0</version>
</dependency>
```

- 第二步：建立连接

在 linux 虚拟环境中输入 `ip addr` 获取 redis 的 ip 地址，即：172.23.14.3

```shell
cell@LAPTOP-SVEUFK1D:~$ ip addr
1: lo: <LOOPBACK,UP,LOWER_UP> mtu 65536 qdisc noqueue state UNKNOWN group default qlen 1000
    link/loopback 00:00:00:00:00:00 brd 00:00:00:00:00:00
    inet 127.0.0.1/8 scope host lo
       valid_lft forever preferred_lft forever
    inet 10.255.255.254/32 brd 10.255.255.254 scope global lo
       valid_lft forever preferred_lft forever
    inet6 ::1/128 scope host
       valid_lft forever preferred_lft forever
2: eth0: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1500 qdisc mq state UP group default qlen 1000
    link/ether 00:15:5d:85:21:07 brd ff:ff:ff:ff:ff:ff
    inet 172.23.14.3/20 brd 172.23.15.255 scope global eth0
       valid_lft forever preferred_lft forever
    inet6 fe80::215:5dff:fe85:2107/64 scope link
       valid_lft forever preferred_lft forever
```

```java
@BeforeEach
void setUp() {
  // 1.建立连接
  jedis = new Jedis("172.23.14.3", 6379);
  // 2.设置密码
  jedis.auth("wyt131551");
  // 3.选择库
  jedis.select(0);
}
```

- 第三步：存取数据

```java
@Test
void testString() {
    // 存入数据
    String result = jedis.set("name", "虎哥");
    System.out.println("result = " + result);
    // 获取数据
    String name = jedis.get("name");
    System.out.println("name = " + name);
}

@Test
void testHash() {
    // 插入hash数据
    jedis.hset("user:1", "name", "Jack");
    jedis.hset("user:1", "age", "21");

    // 获取
    Map<String, String> map = jedis.hgetAll("user:1");
    System.out.println(map);
}
```

- 第四步：释放资源

```java
@AfterEach
void tearDown() {
    if (jedis != null) {
        jedis.close();
    }
}
```

****
#### 2. Jedis 连接池

Jedis 本身是线程不安全的，并且频繁的创建和销毁连接会有性能损耗，因此使用 Jedis 连接池([JedisConnectionFactory](./Demo1-first/src/main/java/com/cell/jedis/util/JedisConnectionFactory.java))代替 Jedis 的直连方式更安全。

****
#### 3. SpringDataRedis 连接池

##### 1. 概念

Spring Data Redis 是 Spring 团队推出的数据访问模块，属于 Spring Data 项目的子模块之一，专门用于简化 Redis 的集成与操作。

核心功能：

- 多客户端支持：支持 Lettuce（默认，基于 Netty，异步线程安全） 和 Jedis（老牌，阻塞 IO，线程不安全）
- RedisTemplate API：提供统一的 RedisTemplate 高级封装，简化数据读写操作
- 发布/订阅模型：支持 Redis 的 Pub/Sub 机制，方便实现消息推送与订阅
- 哨兵模式支持：内置支持 Redis 高可用架构中的 Sentinel（哨兵）配置
- 响应式编程支持：基于 Lettuce，支持 Reactive 响应式数据操作
- ...

相关依赖：

```xml
<!--redis依赖-->
<dependency>
   <groupId>org.springframework.boot</groupId>
   <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<!--common-pool-->
<dependency>
   <groupId>org.apache.commons</groupId>
   <artifactId>commons-pool2</artifactId>
</dependency>
```

常用方法：

```java
// 存入字符串
redisTemplate.opsForValue().set("key", "value");
// 获取字符串
String value = (String) redisTemplate.opsForValue().get("key");
// 操作哈希
redisTemplate.opsForHash().put("user", "name", "Tom");
// 获取哈希所有字段
redisTemplate.opsForHash().entries("user");
// 操作列表
redisTemplate.opsForList().leftPush("list", "item");
// 发布消息
redisTemplate.convertAndSend("channel", "message");
```



常见序列化配置：

```yml
spring:
  data:
    redis:
      host: 172.23.14.3
      port: 6379
      password: wyt131551
      timeout: 10s
      lettuce:
        pool:
          enabled: true
          max-active: 8
          max-idle: 8
          min-idle: 0
          max-wait: 1s
```

****
##### 2. 自定义序列化

RedisTemplate 支持存储任意 Java 对象到 Redis（set 方法的第一个参数是 String，第二个是 Object），底层通过序列化机制将对象转为字节数组存储，所以实际写入 Redis 的内容是二进制字节数组，通过客户端工具查看时通常看到乱码，这种方式往往可读性较差

可以通过自定义 RedisTemplate 的序列化方式来提升可读性：

在原始的序列化方式中，传入的值会先进行判断是否为字节数组，然后再走 valueSerializer() 统一序列化流程

```java
byte[] rawValue(Object value) {
  if (this.valueSerializer() == null && value instanceof byte[] bytes) {
      return bytes;
  } else {
      return this.valueSerializer().serialize(value);
  }
}
```
valueSerializer() 是 RedisTemplate 配置的序列化器，比如默认 JdkSerializationRedisSerializer，通过 serializer.convert() ，实际上调用了序列化器内部的 serializeToByteArray()

```java
public byte[] serialize(@Nullable Object value) {
   if (value == null) {
      return SerializationUtils.EMPTY_ARRAY;
   } else {
      try {
         return (byte[])this.serializer.convert(value);
      } catch (Exception var3) {
         Exception ex = var3;
         throw new SerializationException("Cannot serialize", ex);
      }
   }
}
```

通过方法名就可以看出在这里调用字节流的方式，将数据转换成字节数组并返回

```java
default byte[] serializeToByteArray(T object) throws IOException {
  ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
  this.serialize(object, out); // 把对象序列化写入流
  return out.toByteArray();
}
```

而 RedisTemplate 中有这四个序列化器，

```java
@Nullable
private RedisSerializer keySerializer = null; // 操作 Key 的序列化
@Nullable
private RedisSerializer valueSerializer = null; // 操作 Value 的序列化
@Nullable
private RedisSerializer hashKeySerializer = null; // Hash 结构 Key 的序列化
@Nullable
private RedisSerializer hashValueSerializer = null; // Hash 结构 Value 的序列化
```

当初始化时会默认构建一个 Jdk 的序列化器

```java
public void afterPropertiesSet() {
    super.afterPropertiesSet();
    if (this.defaultSerializer == null) {
        this.defaultSerializer = new JdkSerializationRedisSerializer(this.classLoader != null ? this.classLoader : this.getClass().getClassLoader());
    }
}
```

所以重写方法时，让这四个序列化器构建成可以将对象转换成 Json 格式的序列化器

```java
@Bean
public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory){
  // 创建RedisTemplate对象
  RedisTemplate<String, Object> template = new RedisTemplate<>();
  // 设置连接工厂
  template.setConnectionFactory(connectionFactory);
  // 创建 JSON 序列化工具
  GenericJackson2JsonRedisSerializer jsonRedisSerializer =
          new GenericJackson2JsonRedisSerializer();
  // 设置 Key 的序列化，让 Key 存储为纯字符串，易于阅读、管理
  template.setKeySerializer(RedisSerializer.string());
  template.setHashKeySerializer(RedisSerializer.string());
  // 设置 Value 的序列化，让 Value 转为 JSON 格式存储，兼顾可读性、跨语言兼容性、性能优化
  template.setValueSerializer(jsonRedisSerializer);
  template.setHashValueSerializer(jsonRedisSerializer);
  // 返回
  return template;
}
```

****
##### 3. 使用 StringRedisTemplate 手动序列化

使用上面的自定义序列化的时候，会自动给对象存入一个路径的信息，就会导致占用更多的内存，为了节省内存就可以通过统一使用 String 序列化器，要求只能存储 String 类型的 key 和 value。
当需要存储 Java 对象时，手动完成对象的序列化和反序列化。因为存入和读取时的序列化及反序列化都是手动实现的，SpringDataRedis 就不会将 class 信息写入 Redis 了。

```json
{
  "@class": "com.cell.jedis.bean.User",
  "name": "hahawuwu",
  "age": 20
}
```

```java
@Autowired
private StringRedisTemplate stringRedisTemplate;
// JSON序列化工具
private static final ObjectMapper mapper = new ObjectMapper();
@Test
void testSaveUser() throws JsonProcessingException {
   // 创建对象
   User user = new User("王五", 21);
   // 手动序列化
   String json = mapper.writeValueAsString(user);
   // 写入数据
   stringRedisTemplate.opsForValue().set("user:200", json);
   // 获取数据
   String jsonUser = stringRedisTemplate.opsForValue().get("user:200");
   // 手动反序列化
   User user1 = mapper.readValue(jsonUser, User.class);
   System.out.println("user1 = " + user1);
}
```

```json
{
  "name": "王五",
  "age": 21
}
```

****






