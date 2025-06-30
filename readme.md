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

---

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

---

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

---

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

---

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

---

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

---

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

---

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

---

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

---

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

---

#### 2. Jedis 连接池

Jedis 本身是线程不安全的，并且频繁的创建和销毁连接会有性能损耗，因此使用 Jedis 连接池([JedisConnectionFactory](./Demo1-first/src/main/java/com/cell/jedis/util/JedisConnectionFactory.java))代替 Jedis 的直连方式更安全。

---

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

---

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

---

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

---

## 二. 黑马点评项目

### 1. 短信登录

#### 1.1 基于 Session 实现登录流程

发送验证码： [UserServiceImpl#sendCode](./hm-dianping/src/main/java/com/hmdp/service/impl/UserServiceImpl.java)

- 用户在提交手机号后，会校验手机号是否合法，如果不合法，则要求用户重新输入手机号；
- 如果手机号合法，后台就生成对应的验证码，同时将验证码进行保存，然后再通过短信的方式将验证码发送给用户（目前通过打印到日志模拟发送验证码）

短信验证码注册、登录：[UserServiceImpl#login](./hm-dianping/src/main/java/com/hmdp/service/impl/UserServiceImpl.java)

- 用户输入收到的验证码和手机号，后台从 session 中拿到当前验证码，然后和用户输入的验证码进行校验，如果不一致，则无法通过校验；
- 如果一致，则后台根据手机号查询用户；
- 当查询不到该手机号的用户时，就为该手机号创建用户账号信息并保存保存到数据库
- 最后将用户信息保存到 session 中，方便后续从 session 中获取当前登录用户的信息

校验登陆状态：[LoginInterceptor](./hm-dianping/src/main/java/com/hmdp/utils/LoginInterceptor.java)

- 用户发送请求时，后台会创建 session，服务端返回响应时则会把 JSessionId 保存到服务端，所以服务端可以从 JSessionId 中获取到用户信息，
- 如果没有获取到对应的信息，则进行拦截，
- 如果获取到了，就将用户信息存到 ThreadLocal 中，而通过 Tomcat 的线程池复用技术，可以较为方便的获取到 ThreadLocal 中的数据，通常一种 ThreadLocal 存一种数据

****
#### 1.2 Session 共享问题

每个 tomcat 中都有一份属于自己的 session，假设用户第一次访问第一台 tomcat，并且把自己的信息存放到第一台服务器的 session 中，但是第二次这个用户访问到了第二台 tomcat，
那么在第二台服务器上，肯定没有第一台服务器存放的 session，所以此时整个登录拦截功能就会出现问题（即使账号密码正确也无法登录成功），早期的方案是通过 session 拷贝来解决，
就是说虽然每个 tomcat 上都有不同的 session，但是每当任意一台服务器的 session 修改时，都会同步给其他的 tomcat 服务器的 session，
这样的话，就可以实现 session 的共享了

但是这种方案具有两个大问题：

1. 每台服务器中都有完整的一份 session 数据，这会导致服务器压力过大。 
2. session 拷贝数据时，可能会出现延迟，当用户短时间内切换节点时，新节点可能还没接收到最新的 session 数据。

****
#### 1.3 用 Redis 代替 Session 

使用 Redis 存储 Session，相当于把所有用户状态放在一个公共内存池中，多台 Tomcat 共享这份数据，实现真正意义上的 Session 共享，避免拷贝的副作用。而本项目存入的数据较为简单，
key 的数据结构可以考虑直接使用 String 类型，或者使用 Hash 结构，例如：

```text
// String
Key：session:{token}
Value：序列化后的 JSON 字符串
// Hash
Key：session:{token}
Field-Value结构：
    userId => 1001
    username => "Tom"
    role => "admin"
    lastLoginTime => "2024-06-27"
```

- 使用 String 结构操作简单，通过 SET、GET 即可完成基本操作，但整个 Session 作为字符串整体存储，占用空间稍大，且 Value 作为整体不容易从中拆分数据单独操作
- 使用 Hash 结构则可以支持字段级别读写，操作更灵活，并且减少了内存，但结构较为复杂，不适合存储结构复杂的数据

设计 key 时应该注意：

- key 要具有唯一性
- key 要方便携带

虽然用户的手机号满足以上两点，但是它作为敏感数据存储到 redis 中并从页面传递不太安全，容易泄露信息，所以可以通过后台生成随机 token 的方式，让 token 作为 key

****
#### 1.4 基于 Redis 实现短信登录

当注册完成后，用户登录会去校验用户提交的手机号和验证码是否一致，如果一致则根据手机号查询该用户信息，若该用户不存在则新建，最后将用户数据保存到 redis 并生成 token 作为 redis 的 key，
然后用户会携带着 token 进行访问，拦截则是根据 token 从 redis 中取出 token 对应的 value 来判断是否存在这个数据（手机号和验证码一致），
如果没有则拦截，存在则将其保存到 threadLocal 中并且放行，后续直接通过 ThreadLocal 快速获取用户信息。

发送验证码：[UserServiceImpl#sendCode](./hm-dianping/src/main/java/com/hmdp/service/impl/UserServiceImpl.java)

大体逻辑与前面类似，将 `session.setAttribute(DetailConstants.PHONE_CODE,code);` 替换成 `stringRedisTemplate.opsForValue().set(RedisConstants.LOGIN_CODE_KEY + phone, code, RedisConstants.LOGIN_CODE_TTL, TimeUnit.MINUTES);`，
使用 login:code:phone 作为 redis 的 key，并设置了一个有效时间，当长时间未操作时 token 会自动清除

短信验证码注册、登录：[UserServiceImpl#login](./hm-dianping/src/main/java/com/hmdp/service/impl/UserServiceImpl.java)

成功验证后将 User 对象转换成 UserDTO 对象，然后以 HashMap 的结构传入 redis 中（`stringRedisTemplate.opsForHash().putAll()` 可以将一个 Map 结构的数据存入 redis，但 Map 中的所有数据都要是 String 类型），
然后给 UserDTO 信息设置一个 token，同样的，长时间未操作就清除 token

校验登陆状态：[LoginInterceptor](./hm-dianping/src/main/java/com/hmdp/utils/LoginInterceptor.java)

因为用户发送请求时会随身携带 token（此 token 为用户登陆时系统随机生成的作为 UserDTO 的 key） 在请求头中，从请求头中获取到 token 后从 redis 中获取 UserDTO 用户信息存到 localhost 并刷新 token 有效期；
如果没获取到证明没注册成功，即拦截。

需要注意的是：拦截器不推荐纳入 IoC 容器管理，因为拦截器本身是 SpringMVC 的一部分，为了和 Spring 保持职责分离，不希望它和 Spring 容器强行绑定，
让 MVC 的组件手动实例化（new 一个），与此同理的还有过滤器、监听器等组件

****
#### 1.5 登录刷新 token 有效期问题

初始代码中只使用了一个 LoginInterceptor 拦截器，但是它只有在某些特定的请求（路径）时才会拦截并刷新 token 有效期，所以发送其他请求时就无法刷新，
因此可以再创建一个新的 [RefreshTokenInterceptor](./hm-dianping/src/main/java/com/hmdp/utils/RefreshTokenInterceptor.java) 拦截器，它作为外层拦截器负责拦截所有的请求，
当从请求头中没有获取到 token 时（可能没有登录或者已完成登录），当用户发送某些特定请求时就放行给 LoginInterceptor 判断是否登录（从 localhost 中查找是否有数据）；获取到 token 时证明刚完成登录或注册，然后同理。
这种方式保证了登录的用户每次发送请求时都会刷新一次 token 有效期，确保一直保持登录状态。

****
### 2. 商户查询缓存

#### 2.1 缓存

缓存就是把常用、热点、重复访问的数据，临时存放在更快的存储介质中，以提高系统性能、降低访问延迟，一般从数据库中获取，存储于本地代码。例如:

```java
1:Static
final ConcurrentHashMap<K, V> map = new ConcurrentHashMap<>(); // 本地用于高并发，线程安全的本地缓存

2:
static final Cache<K, V> USER_CACHE = CacheBuilder.newBuilder().build(); // 用于 redis 等缓存

3:Static
final Map<K, V> map = new HashMap(); // 本地缓存
```

由于其被 Static 修饰，所以随着类的加载而被加载到内存之中作为本地缓存，由于其又被 final 修饰，所以其引用(例3:map)和对象(例3:new HashMap())之间的关系是固定的不能改变，因此不用担心赋值(=)导致缓存失效，
但内部数据依旧可以动态增删（缓存更新）。

优点：

缓存数据存储于代码中，而代码运行在内存中，内存的读写性能远高于磁盘，缓存可以大大降低用户访问并发量带来的服务器读写压力。
实际开发中企业系统数据量庞大，如果频繁、直接操作数据库会导致系统吞吐量急剧下降、高并发下数据库易崩溃、响应缓慢，而缓存的使用正好能环节这些问题。

缺点：

- 缓存与数据库数据可能不一致，出现脏数据
- 需要合理设计缓存结构、key 方案、过期策略
- 多了一套缓存逻辑，修改功能时要考虑缓存更新与清理
- 缓存占用内存空间，分布式缓存（如 Redis）需要额外部署维护

缓存的使用：

实际开发中，缓存并不仅限于单一层面，整个系统各个环节都有缓存机制参与：

1. 浏览器缓存
   - 用户浏览器内存、磁盘，可避免重复请求服务器，提升前端页面加载速度
2. 应用层缓存
   - 本地缓存：存放在JVM内存中，速度极快，适合频繁访问、变化不大的数据，例如：ConcurrentHashMap
   - 分布式缓存：独立部署，在多个应用中共享，例如：Redis
3. 数据库缓存
   - 数据库内部缓存机制，MySQL 的 InnoDB 引擎有 Buffer Pool，可以将增删改查的数据放入里面
4. CPU 缓存
   - 计算机硬件层的多级缓存，L1 Cache、L2 Cache、L3 Cache，以此缓解内存访问瓶颈，提升整体运算速度

****
#### 2.2 添加缓存

添加商户缓存：[ShopServiceImpl#queryById](./hm-dianping/src/main/java/com/hmdp/service/impl/ShopServiceImpl.java)

在查询商户信息时是直接从数据库中去查询的，大致逻辑是这样:

```java
@GetMapping("/{id}")
public Result queryShopById(@PathVariable("id") Long id) {
    //这里是直接查询数据库
    return shopService.queryById(id);
}
```

直接查询数据库肯定慢，所以需要增加缓存，标准的操作方式就是查询数据库之前先查询缓存，如果缓存数据存在，则直接从缓存中返回；如果缓存数据不存在，再查询数据库，然后将数据存入 redis

添加商户类型列表缓存：[ShopTypeServiceImpl#queryShopTypeList](./hm-dianping/src/main/java/com/hmdp/service/impl/ShopTypeServiceImpl.java)

这个需要注意的是从 redis 中获取的 json 数据需要转换成 List<ShopType> 类型，将从数据库中找到的 List<ShopType> 在存入 redis 时要把它转换成 json

****
#### 2.3 缓存更新

缓存虽能加速数据读取，但面临数据不一致问题，必须设计合理的更新策略以平衡数据的一致性与系统性能之间的关系。常见的缓存更新策略如下：

1、被动淘汰

redis 自动进行，当 redis 内存达到设定的 max-memery 的时候，会自动触发淘汰机制，淘汰掉一些不重要的数据(可以自己设置策略方式)，但数据的一致性较差，因为无法主动控制失效时间

2、主动更新

可以手动编写逻辑调用方法把缓存删掉，下一次查询会重新加载新数据，通常用于解决缓存和数据库不一致问题，数据的一致性较高，但存在缓存击穿风险，当缓存失效时可能会有多个请求同时查询数据库

3、超时剔除

当给 redis 设置了过期时间 ttl 之后，redis 会将超时的数据进行删除，到下次访问时再更新缓存。

****

由于缓存的数据源来自于数据库，而数据库的数据是会发生变化的，如果当数据库中数据发生变化而缓存却没有同步，此时就会有一致性问题存在，其后果是:用户使用缓存中的过时数据,就会产生类似多线程数据安全问题,从而影响业务,产品口碑等。常用解决方法如下：

- Cache Aside Pattern 人工编码方式：缓存调用者在更新完数据库后再去更新缓存，也称之为双写方案

这种方式简单易用，是主流的方案，缓存与数据库松耦合，易扩展，但高并发下有缓存不一致风险（需加锁优化），且数据短暂时间内存在脏读风险

- Read/Write Through Pattern : 由系统本身完成，数据库与缓存的问题交由系统本身去处理

业务开发者无需关心缓存逻辑，系统自动保障缓存与数据库一致性，但实现复杂，需强大中间件支持（如某些分布式缓存框架），且较难维护

- Write Behind Caching Pattern ：调用者只操作缓存，其他线程去异步处理数据库，实现最终一致

写入性能极高，极大缓解数据库压力，但最终的数据是否一致并非强制性的，因为可能由于系统的崩溃或网络异常而导致数据丢失等问题，并且实现复杂，维护困难

综上，使用最多的是 Cache Aside Pattern 人工编码方式

****

数据库和缓存不一致时采用什么方案处理呢？操作缓存和数据库时有三个问题需要考虑：

如果采用第一个方案，那么假设每次操作数据库后都操作缓存，但是中间如果没有人查询，那么这个更新动作实际上只有最后一次生效，中间的更新动作意义并不大，
那么就可以把缓存删除，等待再次查询时，将缓存中的数据加载出来

* 但是先删除缓存还是更新缓存？
   * 更新缓存：每次更新数据库都更新缓存，无效写操作较多
   * 删除缓存：更新数据库时让缓存失效，查询时再更新缓存

先更新数据库，再删除缓存。避免脏数据回写，防止数据错乱。

* 如何保证缓存与数据库的操作的同时成功或失败？
   * 采用单体系统，将缓存与数据库操作放在一个事务
   * 分布式系统，利用 TCC 等分布式事务方案

* 先操作缓存还是先操作数据库？
   * 先删除缓存，再操作数据库
   * 先操作数据库，再删除缓存

应当是先操作数据库，再删除缓存，原因在于，如果选择第一种方案，在两个线程并发来访问时，假设线程1先来，它先把缓存删了，此时线程2过来，它查询缓存数据并不存在，此时它写入缓存，写完后线程1再执行更新动作时，实际上写入的就是旧的数据，新的数据被旧数据覆盖了。

****
#### 2.4 缓存穿透问题的解决思路

缓存穿透是指客户端请求的数据在缓存中和数据库中都不存在，这样缓存永远不会生效，这些请求都会打到数据库，导致数据库压力陡增。常见的解决方案有两种：

1、缓存空对象

当客户端访问不存在的数据时，先请求 redis，但是此时 redis 中没有数据，就会访问到数据库，但是数据库中也没有数据，这个数据穿透了缓存，
直击数据库，但数据库能够承载的并发不如redis这么高，如果大量的请求同时过来访问这种不存在的数据，这些请求就都会访问到数据库，
简单的解决方案就是哪怕这个数据在数据库中也不存在，我们也把这个数据存入到 redis 中去，下次用户过来访问这个不存在的数据，
那么在 redis 中也能找到这个数据就不会进入到数据库中了，虽然会消耗额外的内存，但是可以设置一个较短的生命周期来解决，
让后面的无效请求直接命中缓存，但数据短时间内有可能刚好插入，造成临时不一致。

解决缓存穿透：[ShopServiceImpl#queryById](./hm-dianping/src/main/java/com/hmdp/service/impl/ShopServiceImpl.java)

在原代码的基础上进行了一些添加，当数据库没有查询到信息时，就将一个空值存入 redis 中，key 就用的当前查询的条件（shop#id）：

```java
if (shop != null) {
    if (shopJson.isEmpty()) {
    // 命中空缓存，直接返回
    System.out.println("命中空值缓存！");
    return null;
    }

```
            
```java
// 5. 如果不存在就返回错误信息
if (shop == null) {
    // 将空值写入 redis
    stringRedisTemplate.opsForValue().set(RedisConstants.CACHE_SHOP_KEY + id, "",
            RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);
    return Result.fail("店铺信息不存在！");
}
```

然后发送请求时，通过了 `shopJson` 不为空，则证明命中了缓存，此时就要判断命中的是不是空缓存，因为如果命中的是空缓存就不能再让它走下去了，应该直接返回错误信息。

```java
// 设置了空值，所以要判断请求是否命中空值还是还是不存在缓存中的数据
// 第二层缓存是否是空字符串（缓存空值）
if (shopJson != null && shopJson.isEmpty()) { // 因为设置了空缓存，所以一定能获取到缓存信息，只不过这个信息是 "" 空字符串
    // 命中的是缓存的空值，直接返回不存在
    System.out.println("命中空值！");
    return Result.fail("店铺信息不存在！");
}
```

2、布隆过滤器（Bloom Filter）

布隆过滤器是一个概率型的数据结构，系统启动时，把所有数据库中真实存在的 id 存入布隆过滤器，每次查询前，先经过布隆过滤器，
布隆过滤器其实采用的是哈希思想来解决这个问题，通过一个庞大的二进制数组，走哈希思想去判断当前这个要查询的这个数据是否存在，将数据映射到数组的不同位置，若所有对应位都为1，判断数据可能存在，若有任意位为0，数据必定不存在。
如果布隆过滤器判断存在，则放行，这个请求会去访问 redis，哪怕此时 redis 中的数据过期了，但是数据库中一定存在这个数据，在数据库中查询出来这个数据后，再将其放入到 redis 中，
假设布隆过滤器判断这个数据不存在，则直接返回，这种方式优点在于节约内存空间，存在误判，误判原因在于：布隆过滤器走的是哈希思想，只要哈希思想，就可能存在哈希冲突

还可以通过

* 增强id的复杂度，避免被猜测id规律
* 做好数据的基础格式校验
* 加强用户权限校验
* 做好热点参数的限流

等方式来解决缓存穿透问题。

****
#### 2.5 缓存雪崩

缓存雪崩是指在同一时段大量的缓存 key 同时失效或者 Redis 服务宕机，导致大量请求到达数据库，带来巨大压力。常见解决方案：

- 随机 TTL，错峰失效。在缓存设置时额外加上随机时间避免所有 Key 同时到期，降低集中失效概率。

```java
stringRedisTemplate.opsForValue().set(key, value, baseTime + RandomUtil.randomInt(10, 30), TimeUnit.SECONDS);
```

* 利用Redis集群提高服务的可用性
* 给缓存业务添加降级限流策略
* 给业务添加多级缓存

****
#### 2.6 缓存击穿

缓存击穿问题也叫热点 Key 问题，就是一个被高并发访问并且缓存重建业务较复杂的 key 突然失效了，无数的请求访问会在瞬间给数据库带来巨大的冲击，导致数据库压力激增，可能宕机。

例如：假设线程1在查询缓存之后，本来应该去查询数据库，然后把这个数据重新加载到缓存的，此时只要线程1走完这个逻辑，其他线程就都能从缓存中加载这些数据了，
但是如果在线程1没有走完的时候，后续的线程2、线程3、线程4同时过来访问当前这个方法，他们就会同一时刻来访问查询缓存，但是都没查到，
接着同一时间去访问数据库，同时的去执行数据库代码，就会对数据库访问压力过大

常见的解决方案有两种：

* 互斥锁

因为锁能实现互斥性，只能一个一个的来访问数据库，从而避免对于数据库访问压力过大，但这也会影响查询的性能，因为此时会让查询的性能从并行变成了串行，
可以采用 tryLock 方法 + double check 来解决这样的问题。

假设现在线程1过来访问，他查询缓存没有命中，但是此时他获得到了锁的资源，那么线程1就会一个人去执行逻辑，此时现在线程2过来，它在执行过程中并没有获得到锁，
那么线程2就可以进行到休眠，直到线程1把锁释放后获得锁，然后再来执行逻辑，此时就能够从缓存中拿到数据了。

* 逻辑过期

之所以会出现这个缓存击穿问题，主要原因是在于对 key 设置了过期时间，假设不设置过期时间，其实就不会有缓存击穿的问题，但是不设置过期时间，这样数据就会一直占用内存，
逻辑过期就是把过期时间设置在 redis 的 value 中（注意：这个过期时间并不会直接作用于 redis，而是后续通过逻辑去处理）。

假设线程1去查询缓存，然后从 value 中判断出来当前的数据已经过期了，此时线程1去获得互斥锁，那么其他线程会进行阻塞，它会自己或开启另一个线程去重建缓存，
直到完成这个逻辑后才释放锁并直接进行返回，假设现在线程2过来访问，由于锁被占有，所以线程2无法获得锁，线程2也直接返回数据，只有等到重建缓存完成并释放锁后，其他线程才能走返回新的数据。
这种方案巧妙在于异步的构建缓存，但缺点是在构建完缓存之前，返回的都是脏数据。

- **互斥锁方案：** 由于保证了互斥性，所以数据一致，且实现简单，因为仅仅只需要加一把锁而已，也没其他的事情需要操心，所以没有额外的内存消耗，缺点在于有锁就有死锁问题的发生，且只能串行执行性能肯定受到影响
- **逻辑过期方案：** 线程读取过程中不需要等待，性能好，有一个额外的线程持有锁去进行重构数据，但是在重构数据完成前，其他的线程只能返回之前的数据，且实现起来麻烦

****
#### 2.7 利用互斥锁解决缓存击穿问题

相较于原来从缓存中查询不到数据后直接查询数据库而言，现在的方案是:进行查询之后，如果从缓存没有查询到数据，则进行互斥锁的获取，获取互斥锁后，判断是否获得到了锁，
如果没有获得到，则休眠，过一会再进行尝试，直到获取到锁为止，才能进行查询；
如果获取到了锁的线程，再去进行查询，查询后将数据写入 redis 再释放锁，然后返回数据，这样利用互斥锁就能保证只有一个线程去执行操作数据库的逻辑，防止缓存击穿

解决缓存击穿：[ShopServiceImpl#queryWithMutex](./hm-dianping/src/main/java/com/hmdp/service/impl/ShopServiceImpl.java)

基于以上，可以利用 redis 的 setnx 方法来表示获取锁，该方法含义是 redis 中如果没有这个 key，则插入成功，返回1，在 stringRedisTemplate 中返回 true；
如果有这个 key 则插入失败，则返回0，在 stringRedisTemplate 返回 false。可以通过 true 或者是 false 来表示是否有线程成功插入 key，
成功插入的 key 的线程则认为就是获得到锁的线程。


```java
private boolean tryLock(String key) {
    Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 20, TimeUnit.SECONDS);
    return BooleanUtil.isTrue(flag);
}
// 通过删除 key 来释放锁
private void unlock(String key) {
    stringRedisTemplate.delete(key);
}
```

****
#### 2.8 基于逻辑过期解决缓存击穿问题

解决缓存击穿：[ShopServiceImpl#queryWithLogicalExpire](./hm-dianping/src/main/java/com/hmdp/service/impl/ShopServiceImpl.java)

当用户开始查询时，会判断是否命中缓存，如果命中则判断是否命中的为空缓存，如果不是则将 value 取出，判断 value 中的过期时间是否满足，
如果没有过期，则直接返回 redis 中的数据，如果过期，则在开启独立线程后直接返回之前的数据，独立线程去重构数据缓存，重构完成后释放互斥锁。

****
#### 2.9 封装 Redis 工具

* 方法1：将任意 Java 对象序列化为 json 并存储在 string 类型的 key 中，并且可以设置 TTL 过期时间

[CacheClient#set](./hm-dianping/src/main/java/com/hmdp/utils/CacheClient.java)

* 方法2：将任意 Java 对象序列化为 json 并存储在 string 类型的 key 中，并且可以设置逻辑过期时间，用于处理缓

[CacheClient#setWithLogicalExpire](./hm-dianping/src/main/java/com/hmdp/utils/CacheClient.java)。

存击穿问题：

* 方法3：根据指定的 key 查询缓存，并反序列化为指定类型，利用缓存空值的方式解决缓存穿透问题

[CacheClient#queryWithPassThrough](./hm-dianping/src/main/java/com/hmdp/utils/CacheClient.java)。因为该方法最终会返回一个对象，所以要根据调用者具体要使用的类型来判断返回类型，
这就需要用到泛型，根据调用者传来的具体类型，然后使用 JSONUtil.toBean 转换成对应的具体对象，然后返回。

```java
public <R,ID> R queryWithPassThrough(String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFallback, Long time, TimeUnit unit) {
}
```

```java
return JSONUtil.toBean(json, type);
```

因为解决缓存穿透问题是先判断有无缓存，然后查询数据库再赋空值给缓存，所以这里涉及数据库的操作，但是工具类没有纳入 MyBatisPlus 的管理，所以不能直接查询到数据，
只能通过调用者传入一个函数来查询。

```java
cacheClient.queryWithPassThrough(RedisConstants.CACHE_SHOP_KEY, id, Shop.class, shopId -> getById(shopId), RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);

R r = dbFallback.apply(id);
```

* 方法4：根据指定的 key 查询缓存，并反序列化为指定类型，需要利用逻辑过期解决缓存击穿问题

[CacheClient#queryWithLogicalExpire](./hm-dianping/src/main/java/com/hmdp/utils/CacheClient.java)。该方法需要的参数与上面一致，不过因为代码逻辑的问题，
需要注意多个泛型类型的返回值。

****
### 3. 优惠卷秒杀

#### 3.1 全局唯一 ID

每个店铺都可以发布优惠券，当用户抢购时，就会生成订单并保存到 tb_voucher_order 表中，而订单表如果使用数据库自增 ID 就存在一些问题：

* id的规律性太明显
* 受单表数据量的限制

如果 id 具有太明显的规则，用户或者说商业对手很容易猜测出来一些敏感信息，比如商城在一天时间内，卖出了多少单，这明显不合适。
并且随着商城规模越来越大，数据量过大之后，就需要进行拆库拆表，但拆分表了之后，它们从逻辑上讲又是同一张表，所以他们的 id 是不能一样的，所以就需要保证 id 的唯一性。

**全局 ID 生成器**，是一种在分布式系统下用来生成全局唯一 ID 的工具，一般要满足下列特性：

- 唯一性
- 高性能
- 安全性
- 递增行
- 高可用

而 Redis 全局 ID 设计思路则是采用拼接的方式：

- 成部分：符号位：1 bit，永远为0 
- 时间戳：31 bit，秒级时间戳，以秒为单位，可以使用 69 年 
- 序列号：32 bit，秒内的计数器，支持每秒产生 2^32 个不同 ID

实现唯一 ID：[RedisIdWorker#nextId](./hm-dianping/src/main/java/com/hmdp/utils/RedisIdWorker.java)

测试：

关于 CountDownLatch，CountDownLatch 名为信号枪（倒计时锁）：主要的作用是同步协调在多线程的等待与唤醒问题，如果没有 CountDownLatch，那么由于程序是异步的，当异步程序没有执行完时，
主线程就已经执行完了，当希望分线程先运行完后再让主线程运行，就需要使用到 CountDownLatch

CountDownLatch 中有两个最重要的方法：

1. countDown 
2. await

await 是阻塞方法，使用 await 可以让 main 线程阻塞，当 CountDownLatch 内部维护的变量变为 0 时，就不再阻塞并直接放行；而每调用一次 countDown，
内部变量就减少 1，所以可以让分线程和变量绑定，执行完一个分线程就减少一个变量，当分线程全部走完，CountDownLatch 维护的变量就是 0，此时 await 就不再阻塞，
统计出来的时间也就是所有分线程执行完后的时间。

```java
@Test
void testIdWorker() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(300);
    Runnable task = () -> {
        for (int i = 0; i < 100; i++) {
            long id = redisIdWorker.nextId("order");
            System.out.println("id = " + id);
        }
        latch.countDown();
    };
    long begin = System.currentTimeMillis();
    for (int i = 0; i < 300; i++) {
        es.submit(task);
    }
    latch.await();
    long end = System.currentTimeMillis();
    System.out.println("time = " + (end - begin));
}
```

****
#### 3.2 添加优惠卷

每个店铺都可以发布优惠券，分为平价券和特价券。平价券可以任意购买，而特价券需要秒杀抢购，由于优惠力度大，所以像第二种卷，就得限制数量，
特价卷除了具有优惠卷的基本信息以外，还具有库存，抢购时间，结束时间等等字段

- tb_voucher：优惠券的基本信息，优惠金额、使用规则等
- tb_seckill_voucher：优惠券的库存、开始抢购时间，结束抢购时间。特价优惠券才需要填写这些信息 

下单：[VoucherOrderServiceImpl#seckillVoucher](./hm-dianping/src/main/java/com/hmdp/service/impl/VoucherOrderServiceImpl.java)

****
#### 3.3 库存超卖问题

在原代码中是这么写的：

```java
if (voucher.getStock() < 1) {
    // 库存不足
    return Result.fail("库存不足！");
}
//5，扣减库存
boolean success = seckillVoucherService.update()
        .setSql("stock= stock -1")
        .eq("voucher_id", voucherId).update();
if (!success) {
    //扣减库存
    return Result.fail("库存不足！");
}
```

假设线程1过来查询库存，判断出来库存大于1，正准备去扣减库存，但是还没有来得及去扣减，此时线程2过来，线程2也去查询库存，发现这个数量一定也大于1，
那么这两个线程都会去扣减库存，最终多个线程相当于一起去扣减库存，此时就会出现库存的超卖问题。超卖问题是典型的多线程安全问题，针对这一问题的常见解决方案就是加锁，
而对于加锁通常有两种解决方案：

- 悲观锁

悲观锁可以实现对于数据的串行化执行，同时，悲观锁中又可以再细分为公平锁，非公平锁，可重入锁，等等

- 乐观锁

乐观锁则会有一个版本号，每次操作数据会对版本号+1，再提交回数据时，会去校验是否比之前的版本大1，如果大1，则进行操作成功，这套机制的核心逻辑在于，
如果在操作过程中，版本号只比原来大1 ，那么就意味着操作过程中没有人对它进行过修改，它的操作就是安全的，如果不大1，则数据被修改过，
当然乐观锁还有一些变种的处理方式比如 cas，不用版本号进行判断，而是使用要操作的数据本身作为判断，用它作为修改的前置条件，如果操作前后该数据一致，则认为它是安全的

利用乐观锁解决超卖问题：

```java
boolean success = seckillVoucherService.update()
                .setSql("stock= stock -1") // set stock = stock -1
                .eq("voucher_id", voucherId).gt("stock", 0) // where id = ？ and stock > 0
                .update();
```

以上逻辑的核心含义是：只要扣减库存时的库存和之前查询到的库存是一样的，就意味着没有人在中间修改过库存，那么此时就是安全的，但是以上这种方式可能导致购买失败的情况，失败的原因在于：
假设100个线程同时都拿到了100的库存，在使用乐观锁过程中这些线程一起去进行扣减，但是100个之中只有1个能扣减成功，其他的在处理时，就会发现库存已经被修改过了，所以此时其他线程都会失败，
但如果把前置条件降低一点标准，将必须前后相等改为只要库存不为 0 即可：

```java
boolean success = seckillVoucherService.update()
            .setSql("stock= stock -1")
            .eq("voucher_id", voucherId).update().gt("stock",0); //where id = ? and stock > 0
```

****
#### 3.4 优惠券秒杀，一人一单

初步考虑时，是直接在创建订单前进行一个简单的判断，通过商品的 id 作为条件去查询用户信息，如果查到了说明该用户是购买过的，那么就不能再继续往下走。
但是这个方法有个问题，就是当大量的线程同时进行查询判断时，总会有些线程同时进行购买操作，而这种情况则更适合用悲观锁解决，那就要使用到 synchronized。

```java
// 5.一人一单逻辑
// 5.1.用户id
Long userId = UserHolder.getUser().getId();
int count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();
// 5.2.判断是否存在
if (count > 0) {
    // 用户已经购买过了
    return Result.fail("用户已经购买过一次！");
}
```

可以直接使用 synchronized 把整个方法纳入锁中，但是这样就会导致效率低下，因为该方法是对象独有的，如果将方法纳入锁中，那同一时刻只能有一个用户方法该方法，
这就会造成堵塞，锁的粒度太粗了，而在使用锁过程中，控制锁粒度是一个非常重要的事情，所以可以考虑只锁住关键部分的代码，让该方法共享出来。

```java
synchronized(userId.toString().intern()){
         // 5.1.查询订单
        int count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();
        // 5.2.判断是否存在
        if (count > 0) {
            // 用户已经购买过了
            return Result.fail("用户已经购买过一次！");
        }
        。。。
}
```

但是这样还是存在问题，因为当前方法是被 spring 的事务控制的，如果在方法内部加锁，可能会导致当前方法事务还没有提交，但是锁已经释放，这也会导致问题，例如：

线程A 获得锁，进入代码块并查数据库，发现用户未下单，就开始执行 save() 写入订单（此时事务未提交），线程A 走出 synchronized，释放锁；
线程B 获得锁，进入代码块并查数据库，发现订单未提交，查不到新数据，于是线程B 也下单，形成重复下单问题，最后两个订单都提交，导致数据不一致

所以应该选择将当前方法整体包裹起来，但不是直接锁住方法，以确保事务不会出现问题，那么就可以考虑在调用该方法的地方上锁，既保证事务的特性，同时也控制了锁的粒度。

```java
synchronized (userId.toString().intern()) {
    return createVoucherOrder(voucherId);
}
```

至于为什么用 userId.toString().intern() 作为锁对象，这是因为必须确保锁对象是同一个，才能保证多线程竞争生效，userId 是 Long 类型的，它在 `-128 ~ 127` 范围中时是走内置的常量池的，
但是超出范围的都是新的对象，就会导致即使它们数值相等，但是被判断为不同的对象，所以就要把它转换成字符串然后放入常量池中，保证每一次相同的值都是同一个对象。

事务的提交是由 Spring 的代理对象进行操作的，只有走代理对象的方法调用，事务才能生效，而当前只给 createVoucherOrder 方法加上了 @Transactional 注解，也就是说，只有它被 Spring 的代理对象所管理，
而在别的方法中调用它，其实是通过 this. 的方式调用的，就会让事务的特性丧失，所以就要使用一个代理对象来让外部调用的方法也纳入 Spring 事务的管理

```java
Long userId = UserHolder.getUser().getId();
synchronized (userId.toString().intern()) {
// 获取代理对象，事务提交后才会释放锁
IVoucherService proxy = (IVoucherService) AopContext.currentProxy();
    return proxy.createVoucherOrder(voucherId);
}
```

****
#### 3.5 集群环境下的并发问题

通过加锁可以解决在单机情况下的一人一单安全问题，但是在集群模式下就不行了。将服务启动两份，端口分别为8081和8082，一个服务对应一个tomcat。
由于现在部署了多个tomcat，每个tomcat都有一个属于自己的jvm，那么假设在服务器A的tomcat内部有两个线程，这两个线程由于使用的是同一份代码，
那么它们的锁对象是同一个，是可以实现互斥的，但是如果现在是服务器B的tomcat内部，又有两个线程，但是他们的锁对象写的虽然和服务器A一样，但是锁对象却不是同一个，
所以线程3和线程4可以实现互斥，但是却无法和线程1和线程2实现互斥，这就是集群环境下，synchronized 锁失效的原因，在这种情况下就需要使用分布式锁来解决这个问题。

****
### 4. 分布式锁

#### 4.1 基本原理和实现方式对比

分布式锁的核心思想就是让大家都使用同一把锁，只要大家使用的是同一把锁，那么就能锁住线程，不让线程进行，让程序串行执行，跨多个节点、多个进程之间的同步机制，这就是分布式锁的核心思路

一个好的分布式锁应该具有以下特性：

1. 互斥性：同一时间只能有一个客户端持有锁 
2. 避免死锁：客户端宕机后，锁能自动释放 
3. 高可用性：锁服务高可用，避免单点故障 
4. 可重入性（可选）：同一客户端可多次获取同一锁 
5. 锁超时机制：防止意外长时间持有锁

**基于数据库的分布式锁**

MySQL 本身具备锁机制，开发中可以通过以下两种常见方式模拟分布式锁，它的实现简单，基于已有数据库，但存在死锁风险，事务控制复杂，不适合高并发、大规模的系统

- 唯一约束法：通过向一张表中插入唯一键（如 lock_name 字段），若插入成功，表示加锁成功，若失败表示锁已存在。
- 悲观锁法 (SELECT ... FOR UPDATE)： 对某行数据加排他锁，其他线程/节点必须等待锁释放才能操作。

**基于 Redis 的分布式锁**

Redis 提供的 SETNX（Set if Not eXists） 命令配合 EX 过期时间即可实现简单分布式锁，如果插入key成功，则表示获得到了锁，如果有线程插入成功，
表示其它的插入将失败（表示无法获得到锁），可以利用这套逻辑来实现分布式锁

实现分布式锁时需要实现的两个基本方法：

1. 获取锁

必须保证互斥性，即同一时间，只有一个线程/进程/节点能获取锁；也必须保证非阻塞性：尝试获取锁一次，成功返回 true，失败返回 false，调用方自己决定是否重试

2. 释放锁

应该支持手动释放，确保业务完成后及时释放锁；也应该防止死锁，设置超时时间，避免异常情况下锁长时间占用

核心思路：

利用 SETNX 命令，当有多个线程进入时，就利用该方法，第一个线程进入时，redis 中就有这个 key 了，返回了1，如果结果是1，则表示它抢到了锁，
那么它去执行业务，然后再删除锁，退出锁逻辑，没有抢到锁的就等待一定时间后重试即可

```shell
SET key value NX PX 30000
```

- key：锁的名称 
- value：唯一标识（如UUID，防止误删别人的锁） 
- NX：仅当 key 不存在时才设置成功，实现互斥 
- PX 30000：设置超时时间，单位毫秒，防止死锁

**基于 ZooKeeper 的分布式锁**

ZooKeeper 可以通过临时有序节点机制天然适合分布式锁，但性能一般，只适合中低并发场景

****
#### 4.2 实现分布式锁(版本一)

利用 setnx 方法进行加锁，用线程 id 作为锁的 value，避免解锁时误删其他线程；同时增加过期时间，防止死锁，此方法可以保证加锁和增加过期时间具有原子性

```java
@Override
public boolean tryLock(long timeoutSec) {
    // 获取线程标识
    Long threadId = Thread.currentThread().getId();
    // 获取锁
    Boolean success = stringRedisTemplate.opsForValue()
            .setIfAbsent(KEY_PREFIX + name, threadId + "", timeoutSec, TimeUnit.SECONDS);
    // 防止拆箱时出现 null 值导致无法返回
    return Boolean.TRUE.equals(success);
}   
```

基于以上，只需要在下单前进行以此查缓存，检查是否能获取到对应的锁（以用户 ID 为 key，可以保证一人一单），如果获取不到证明该用户正在进行操作，防止其他端口的同一个用户进行相同的操作：

```java
// 创建锁对象
SimpleRedisLock simpleRedisLock = new SimpleRedisLock(stringRedisTemplate, "order:" + userId);
// 获取锁
boolean isLock = simpleRedisLock.tryLock(1200);
// 判断是否获取锁成功
if (!isLock) {
    // 获取锁失败，返回错误信息
    return Result.fail("一个用户只能下一单！");
}
try {
    // 获取锁成功
    // 获取代理对象，事务提交后才会释放锁
    IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
    return proxy.createVoucherOrder(voucherId);
} finally {
    simpleRedisLock.unlock();
}
```

#### 4.3 Redis 分布式锁误删情况

持有锁的线程在锁的内部出现了阻塞，就可能导致的锁过期时间到了，自动释放，此时线程2来尝试获得锁，就拿到了这把锁，然后线程2在持有锁执行过程中，线程1反应过来继续执行，并走到了删除锁逻辑，
此时就会把本应该属于线程2的锁进行删除，当锁被删除后，线程3又可以获取锁了，但是线程2可能还在执行，就又会造成并行问题。

所以，应该在每个线程释放锁的时候都去判断一下当前这把锁是否属于自己，如果不属于自己，则不进行锁的删除，假设还是上边的情况，线程1卡顿，锁自动释放，
线程2进入到锁的内部执行逻辑，此时线程1反应过来，然后删除锁，但是线程1，一看当前这把锁不是属于自己，于是不进行删除锁逻辑，
当线程2走到删除锁逻辑时，如果锁还没自动释放，则判断当前这把锁是否属于自己的，是否可以删除。

```java
@Override
public void unlock() {
    // 获取线程标识(UUID 拼接线程 ID)
    String threadId =  ID_PREFIX + Thread.currentThread().getId();
    // 获取锁中的标识（即 value）
    String id = stringRedisTemplate.opsForValue().get(KEY_PREFIX + name);
    // 判断标识是否一致
    if (threadId.equals(id)) {
        stringRedisTemplate.delete(KEY_PREFIX + name);
    }
}
```

但以上代码仍然存在一些问题，例如：线程1现在持有锁之后，在执行业务逻辑过程中，正准备删除锁，而且已经走到了条件判断的过程中，
比如它已经拿到了当前这把锁确实是属于它自己的，正准备删除锁，但是此时它的线程阻塞了，那么此时线程2进来，但是线程1他会接着往后执行，
当线程1卡顿结束后，就会直接执行删除锁那行代码，相当于条件判断并没有起到作用，这就是删锁时的原子性问题，之所以有这个问题，
是因为线程1的拿锁，比锁，删锁，实际上并不是原子性的，所以需要防止这种的情况发生

****
#### 4.4 Lua 脚本

Redis 提供了 Lua 脚本功能，在一个脚本中编写多条 Redis 命令，确保多条命令执行时的原子性，网站：[https://www.runoob.com/lua/lua-tutorial.html](https://www.runoob.com/lua/lua-tutorial.html)。
Redis 在执行 Lua 脚本时，整个脚本是作为一个“单个命令”提交和执行的，脚本内的所有 redis.call() 命令必须全部执行完成，才可能返回结果。

```lua
redis.call('命令名称', 'key', '其它参数', ...)
```

例如：执行 set name jack，则脚本是这样：

```lua
redis.call('set', 'name', 'jack')
```

查询 key：

```lua
local name = redis.call('get', 'name')
# 返回
return name
```

以上内容是写在脚本中的，写好后需要用 Redis 命令来调用脚本：

```shell
127.0.0.1:6379> help @scripting

  EVAL script numkeys [key [key ...]] [arg [arg ...]]
  summary: Executes a server-side Lua script.
  since: 2.6.0
```

例如：固定写死脚本

```redis
EVAL "return redis.call('set', 'name', 'jack')" 0
```

- 没有 KEYS 参数，numkeys 填 0 
- 直接在 Lua 脚本里操作具体的键和值

使用参数，key 类型参数会放入 KEYS 数组，其它参数会放入 ARGV 数组，在脚本中可以从 KEYS 和 ARGV 数组获取这些参数：

```redis
EVAL "return redis.call('set', KEYS[1], ARGV[1])" 1 name jack
```

- 脚本：redis.call('set', KEYS[1], ARGV[1]
- numkeys = 1，即后面 1 个参数属于 KEYS 
- name -> KEYS[1]
- jack -> ARGV[1]

释放锁的业务流程是这样的：

1. 获取锁中的线程标示
2. 判断是否与指定的标示（当前线程标示）一致
3. 如果一致则释放锁（删除）
4. 如果不一致则什么都不做

Lua 脚本中就是：

```lua
-- 这里的 KEYS[1] 就是锁的key，这里的ARGV[1] 就是当前线程标示
-- 获取锁中的标示，判断是否与当前线程标示一致
if (redis.call('GET', KEYS[1]) == ARGV[1]) then
  -- 一致，则删除锁
  return redis.call('DEL', KEYS[1])
end
-- 不一致，则直接返回
return 0
```

****
#### 4.5 利用 Java 代码调用 Lua 脚本改造分布式锁

可以利用 RedisTemplate 中的 execute 方法去执行 lua 脚本，参数对应关系如下：

```java
public <T> T execute(RedisScript<T> script, List<K> keys, Object... args) {
    return this.scriptExecutor.execute(script, keys, args);
}
```

```redis
EVAL script numkeys [key [key ...]] [arg [arg ...]]
```

```java
private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;
    static {
        UNLOCK_SCRIPT = new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setLocation(new ClassPathResource("unlock.lua"));
        UNLOCK_SCRIPT.setResultType(Long.class);
    }

public void unlock() {
    // 调用lua脚本
    stringRedisTemplate.execute(
            UNLOCK_SCRIPT,
            Collections.singletonList(KEY_PREFIX + name),
            ID_PREFIX + Thread.currentThread().getId());
}
```

****
### 5. 分布式锁 Redisson

#### 5.1 概念

基于 setnx 实现的分布式锁存在下面的问题：

- **重入问题**：重入问题是指获得锁的线程可以再次进入到相同的锁的代码块中，比如 HashTable 中的方法都是使用 synchronized 修饰的，假如它在一个方法内调用另一个方法，如果此时是不可重入的就会造成死锁。所以可重入锁的主要意义是防止死锁，synchronized 是可重入的，但是 Redis 并不感知是同一线程（即方法中调用别的方法会多次加锁，导致第二次加锁失败，抛出异常），即不可重入。
- **不可重试**：是指目前的分布式只能尝试一次，因为普通 setnx 方案在获取锁失败时直接返回，但当线程在获得锁失败后，应该要让它能再次尝试获得锁。
- **超时释放**：在加锁时增加了过期时间，这样就可以防止死锁，但是如果卡顿的时间过长，就算采用了 lua 表达式防止删锁的时候误删别人的锁，但是仍然会超时释放。
- **主从一致性**：如果 Redis 提供了主从集群，向集群写数据时，主机需要异步的将数据同步给从机，万一在同步过去之前，主机宕机了，就会出现死锁问题，数据也无法同步。

于是引入了 Redisson，Redisson 是一个在 Redis 的基础上实现的 Java 驻内存数据网格（In-Memory Data Grid）。它不仅提供了一系列的分布式的 Java 常用对象，还提供了许多分布式服务，其中就包含了各种分布式锁的实现。

配置 Redisson 客户端：

```java
@Configuration
public class RedissonConfig {
    @Bean
    public RedissonClient redissonClient(){
        // 配置
        Config config = new Config();
        config.useSingleServer().setAddress("redis://192.168.150.101:6379")
            .setPassword("123321");
        // 创建RedissonClient对象
        return Redisson.create(config);
    }
}
```

如果引入了官方 Starter 的依赖，就可以自动装配，无序写上述手动配置，在 application 文件中配置即可：

```pom
<dependency>
    <groupId>org.redisson</groupId>
    <artifactId>redisson-spring-boot-starter</artifactId>
    <version>3.28.0</version>
</dependency>
```


通过 Spring 注入 RedissonClient，调用它的方法即可：

```java
@Resource
private RedissonClient redissonClient;
@Test
void testRedisson() throws Exception {
    //获取锁(可重入)，指定锁的名称
    RLock lock = redissonClient.getLock("anyLock");
    //尝试获取锁，参数分别是：获取锁的最大等待时间(期间会重试)，锁自动释放时间，时间单位
    boolean isLock = lock.tryLock(1, 10, TimeUnit.SECONDS);
    //判断获取锁成功
    if (isLock) {
        try {
            System.out.println("执行业务");
        } finally {
            //释放锁
            lock.unlock();
        }
    }
}
```

****
#### 5.2 Redisson 可重入锁原理

验证 Redisson 的可重入机制：

```java
RLock lock = redissonClient.getLock("lock");
void method1() {
    boolean isLock = lock.tryLock();
    if (!isLock) {
        System.out.println("获取锁失败, 1");
        return;
    }
    try {
        System.out.println("获取锁成功, 1");
        method2();
    } finally {
        System.out.println("释放锁, 1");
        lock.unlock();
    }
}
void method2() {
    boolean isLock = lock.tryLock();
    if (!isLock) {
        System.out.println("获取锁失败, 2");
        return;
    }
    try {
        System.out.println("获取锁成功, 2");
    } finally {
        System.out.println("释放锁, 2");
        lock.unlock();
    }
}
```

输出：

```text
获取锁成功, 1
获取锁成功, 2
释放锁, 2
释放锁, 1
```

根据输出结果可以看到 Redisson 确实是支持可重入锁机制的。

实现该方法的核心原理：该锁在 Redis 中的结构不再是像之前使用的 setnx 的那样的 String 类型，而是 Hash 结构，因为它不仅需要一个 ID 表示当前锁的持有者，还需要一个变量（state/count）来跟踪锁的重入次数，
初始为 0，表示锁是空闲的，没有线程持有，当线程第一次获取锁时，state 变为 1，如果同一个线程再次请求锁（重入），state 会 +1，释放锁时，state -1，只有 state 变回 0，锁才真正释放，用这个变量来限制后续使用锁时不能提前释放

```text
Key: lock:{lockName}
Field (hash key): threadId （比如 "uuid:threadId"）
Value (hash value): 重入计数（int 类型）
```

源码跟踪：

通过进入 tryLock() 方法，可以看到它是有默认值的，表示没有等待时间与过期时间，

```java
public RFuture<Boolean> tryLockAsync(long threadId) {
    return this.getServiceManager().execute(() -> {
        return this.tryAcquireOnceAsync(-1L, -1L, (TimeUnit)null, threadId);
    });
}
```

然后这里进行判断是否手动给了过期时间，如果没有那就使用默认的过期时间（30000L 毫秒）

```java
private RFuture<Boolean> tryAcquireOnceAsync(long waitTime, long leaseTime, TimeUnit unit, long threadId) {
    RFuture acquiredFuture;
    if (leaseTime > 0L) {
        acquiredFuture = this.tryLockInnerAsync(waitTime, leaseTime, unit, threadId, RedisCommands.EVAL_NULL_BOOLEAN);
    } else {
        acquiredFuture = this.tryLockInnerAsync(waitTime, this.internalLockLeaseTime, TimeUnit.MILLISECONDS, threadId, RedisCommands.EVAL_NULL_BOOLEAN);
    }
}
```

接着就是执行 lua 脚本，直接由 lua 脚本来判断是否加锁成功：

- redis.call('exists', KEYS[1]) == 0：判断锁是否不存在（未被任何线程持有）
- redis.call('hexists', KEYS[1], ARGV[2])：判断当前线程是否已持有锁（重入场景）
- redis.call('hincrby', KEYS[1], ARGV[2], 1)：将当前线程的 value 值加 1，即代表有一个线程在操作
- pexpire(KEYS[1], ARGV[1])：重置锁的过期时间，避免死锁
- return redis.call('pttl', KEYS[1])：返回锁剩余的过期时间（毫秒），代表锁被占用

```java
<T> RFuture<T> tryLockInnerAsync(long waitTime, long leaseTime, TimeUnit unit, long threadId, RedisStrictCommand<T> command) {
    // this.getRawName():KEYS[1]，锁的 Redis Key，hash 类型
    // LongCodec.INSTANCE:Redis 编解码器，处理 long 类型数据
    // command:Redis 执行命令（EVAL_BOOLEAN、EVAL_NULL_BOOLEAN 等）
    return this.evalWriteSyncedAsync(this.getRawName(), LongCodec.INSTANCE, command, 
            "if ((redis.call('exists', KEYS[1]) == 0) or (redis.call('hexists', KEYS[1], ARGV[2]) == 1)) " +
                    "then redis.call('hincrby', KEYS[1], ARGV[2], 1); " +
                    "redis.call('pexpire', KEYS[1], ARGV[1]); " +
                    "return nil; " + // 如果成功获取到锁就返回 nil，会被 Java 识别为 null，以此表示获取锁成功，避免使用数字，与下面返回 TTL 剩余时间产生冲突
                    "end; " +
                    "return redis.call('pttl', KEYS[1]);",
                          // KEYS 数组（这里只有一个 key）      ARGV 数组
            Collections.singletonList(this.getRawName()), new Object[]{unit.toMillis(leaseTime), this.getLockName(threadId)});
            // unit.toMillis(leaseTime): ARGV[1]: 过期时间（毫秒）
            // this.getLockName(threadId): ARGV[2]: 当前线程在锁 Hash 中的字段名（clientId:threadId）            
}
```

解锁则是通过判断当前锁状态来决定是否需要进行释放锁，检查 KEYS[3]（0 未释放，1 已释放），如果锁存在未被释放（val ~= false），则表示本次解锁操作已被执行（重复操作），直接返回该值（转换成数字），避免重复执行解锁逻辑；
然后判断当前锁的 key（KEYS[1]） 是否存在当前线程的标识（ARGV[3]），若不存在，说明锁不属于当前线程（可能已过期或被其他线程获取），返回 nil 触发异常；
然后处理重入锁计数递减，返回递减后的计数 counter；
然后判断重入锁计数是否大于 0，
如果大于 0，证明锁仍然被持有，就不能释放，就需要重置过期时间，然后设置标识 KEYS[3] 为 0 并返回 0 表示锁仍被持有；
如果小于等于 0，就证明现在这个锁的最后使用者就是本线程，就进行释放锁，然后标记 KEYS[3] 为 1，并返回 1 告知别人该锁已释放；

```java
protected RFuture<Boolean> unlockInnerAsync(long threadId, String requestId, int timeout) {
    // this.getRawName(): KEYS[1]，锁的 Redis Key（hash 类型）
    // LongCodec.INSTANCE: Redis 编解码器，处理 long 类型数据
    // RedisCommands.EVAL_BOOLEAN: Redis 命令，执行 Lua 脚本后返回 Boolean 类型
    return this.evalWriteSyncedAsync(this.getRawName(), LongCodec.INSTANCE, RedisCommands.EVAL_BOOLEAN, 
            "local val = redis.call('get', KEYS[3]); " +
                    "if val ~= false " +
                    "then return tonumber(val);" +
                    "end; " +
                    "if (redis.call('hexists', KEYS[1], ARGV[3]) == 0) " +
                    "then return nil;" +
                    "end; " +
                    "local counter = redis.call('hincrby', KEYS[1], ARGV[3], -1); " +
                    "if (counter > 0) " +
                    "then redis.call('pexpire', KEYS[1], ARGV[2]); " +
                    "redis.call('set', KEYS[3], 0, 'px', ARGV[5]); " +
                    "return 0; " +
                    "else redis.call('del', KEYS[1]); " +
                    "redis.call(ARGV[4], KEYS[2], ARGV[1]); " +
                    "redis.call('set', KEYS[3], 1, 'px', ARGV[5]); " +
                    "return 1; " +
                    "end; ", 
                        // KEYS 数组                                                                    ARGV 数组
            Arrays.asList(this.getRawName(), this.getChannelName(), this.getUnlockLatchName(requestId)), new Object[]{LockPubSub.UNLOCK_MESSAGE, this.internalLockLeaseTime, this.getLockName(threadId), this.getSubscribeService().getPublishCommand(), timeout});
            // LockPubSub.UNLOCK_MESSAGE: ARGV[1]，解锁通知消息内容（字符串常量，通常是 "unlock"）
            // this.internalLockLeaseTime: ARGV[2]，锁的过期时间，单位是毫秒
            // this.getLockName(threadId): ARGV[3]，当前线程对应的锁哈希字段名（线程唯一标识）
            // this.getSubscribeService().getPublishCommand(): ARGV[4]，发布消息的 Redis 命令（比如 "publish"）
            // timeout: ARGV[5]，用作 set 命令中键的过期时间（毫秒）
}
```

****
#### 5.3 Redisson 锁重试与看门狗机制

当调用无参数的 tryLock 方法时会进入 tryLockAsync 方法，它会调用 tryAcquireOnceAsync 开始加锁尝试，

```java
// 当使用的是无参数的 tryLock 时会经过该方法
public RFuture<Boolean> tryLockAsync(long threadId) {
    return this.getServiceManager().execute(() -> {
        return this.tryAcquireOnceAsync(-1L, -1L, (TimeUnit)null, threadId);
    });
}
```

调用有参数的 tryLock 方法时会进入 tryAcquire 方法，内部调用 tryAcquireAsync0 开启异步加锁流程

```java
private Long tryAcquire(long waitTime, long leaseTime, TimeUnit unit, long threadId) {
    return (Long)this.get(this.tryAcquireAsync0(waitTime, leaseTime, unit, threadId));
}
private RFuture<Long> tryAcquireAsync0(long waitTime, long leaseTime, TimeUnit unit, long threadId) {
    return this.getServiceManager().execute(() -> {
        return this.tryAcquireAsync(waitTime, leaseTime, unit, threadId);
    });
}
```

tryAcquireOnceAsync 方法负责进行异步加锁，根据加锁是否成功（acquired 标记）设置 internalLockLeaseTime 的值，
成功了就判断是否传入了过期时间，没传入就开启看门狗自动续期机制

```java
private RFuture<Boolean> tryAcquireOnceAsync(long waitTime, long leaseTime, TimeUnit unit, long threadId) {
    RFuture acquiredFuture;
    if (leaseTime > 0L) {
        // 传入调用者手动设置的过期时间
        acquiredFuture = this.tryLockInnerAsync(waitTime, leaseTime, unit, threadId, RedisCommands.EVAL_NULL_BOOLEAN);
    } else {
        // 如果锁不存在，或是本线程重入，则加锁成功；否则，返回剩余 TTL，表示锁被别人占用
        acquiredFuture = this.tryLockInnerAsync(waitTime, this.internalLockLeaseTime, TimeUnit.MILLISECONDS, threadId, RedisCommands.EVAL_NULL_BOOLEAN);
    }
    // 统一封装“未获取到锁”时的清理逻辑，比如自动释放、重试控制
    CompletionStage<Boolean> acquiredFuture = this.handleNoSync(threadId, acquiredFuture);
    CompletionStage<Boolean> f = acquiredFuture.thenApply((acquired) -> {
        if (acquired) {
            if (leaseTime > 0L) {
                // 如果手动设置了过期时间，就更新 internalLockLeaseTime
                this.internalLockLeaseTime = unit.toMillis(leaseTime);
            } else {
                // 如果没设置过期时间，则开启看门狗续期机制，每隔一段时间自动延长锁的过期时间，避免意外超时释放
                this.scheduleExpirationRenewal(threadId);
            }
        }
        return acquired;
    });
    return new CompletableFutureWrapper(f);
}
```

该方法是锁重试的核心机制，当使用者传入了 waitTime 后就会进入，它整体是一个 “循环尝试 + 订阅锁释放通知” 的流程，先调用 tryAcquire 尝试加锁，拿到锁的剩余 TTL（ttl ）。若 ttl == null，说明加锁成功，直接返回 true；
后续就通过 subscribe 方法订阅锁释放的消息，让当前线程等待锁释放的通知，这就是第一次的避免无效尝试加锁。
然后就开启循环尝试，但也是使用订阅锁释放消息的方式，不能让它一直无限的尝试。

```java
// 该方法是在等待时间（waitTime）中不断获取锁
public boolean tryLock(long waitTime, long leaseTime, TimeUnit unit) throws InterruptedException {
    long time = unit.toMillis(waitTime);
    long current = System.currentTimeMillis();
    long threadId = Thread.currentThread().getId();
    // 获取到剩余 TTL
    Long ttl = this.tryAcquire(waitTime, leaseTime, unit, threadId);
    // 如果加锁成功就结束方法
    if (ttl == null) {
        return true;
    } else {
        // 更新设置的 waitTime 的剩余的等待时间
        time -= System.currentTimeMillis() - current;
        if (time <= 0L) {
            this.acquireFailed(waitTime, unit, threadId);
            return false;
        } else {
            current = System.currentTimeMillis();
            // 监听消息，监听该线程是否释放锁，类似于让该线程等待一会，因为上一个线程获得锁一般不会太快释放，避免多次无效尝试
            CompletableFuture<RedissonLockEntry> subscribeFuture = this.subscribe(threadId);
            try {
                // 阻塞等待锁释放通知
                subscribeFuture.get(time, TimeUnit.MILLISECONDS);
            } catch (TimeoutException var21) { // 超时处理
                if (!subscribeFuture.completeExceptionally(new RedisTimeoutException("Unable to acquire subscription lock after " + time + "ms. Try to increase 'subscriptionsPerConnection' and/or 'subscriptionConnectionPoolSize' parameters."))) {
                    subscribeFuture.whenComplete((res, ex) -> {
                        if (ex == null) {
                            this.unsubscribe(res, threadId);
                        }
                    });
                }
                this.acquireFailed(waitTime, unit, threadId);
                return false;
            } catch (ExecutionException var22) { // 异常处理
                ExecutionException e = var22;
                LOGGER.error(e.getMessage(), e);
                this.acquireFailed(waitTime, unit, threadId);
                return false;
            }
            try {
                time -= System.currentTimeMillis() - current;
                if (time <= 0L) {
                    this.acquireFailed(waitTime, unit, threadId);
                    boolean var25 = false;
                    return var25;
                } else {
                    // 通过监听事件后依然剩余一些 watiTime，则可以进行尝试获取锁
                    boolean var16;
                    do {
                        // 自旋，尝试获取锁
                        long currentTime = System.currentTimeMillis();
                        // 尝试获取锁
                        ttl = this.tryAcquire(waitTime, leaseTime, unit, threadId);
                        if (ttl == null) {
                            // 获取成功，退出循环
                            var16 = true;
                            return var16;
                        }

                        time -= System.currentTimeMillis() - currentTime;
                        if (time <= 0L) { // 失败处理
                            this.acquireFailed(waitTime, unit, threadId);
                            var16 = false;
                            return var16;
                        }

                        currentTime = System.currentTimeMillis();
                        if (ttl >= 0L && ttl < time) { // 当 TTL 小于剩余 waitTime，证明很快就能获得锁，直接等待 TTL 的时间即可
                            // 与上面的监听效果类似，都是等待锁的释放通知，避免多次无效尝试
                            ((RedissonLockEntry)this.commandExecutor.getNow(subscribeFuture)).getLatch().tryAcquire(ttl, TimeUnit.MILLISECONDS);
                        } else { // 否则证明 TTL 大于 剩余 waitTime
                            // 尝试等待通知，使用全部时间
                            ((RedissonLockEntry)this.commandExecutor.getNow(subscribeFuture)).getLatch().tryAcquire(time, TimeUnit.MILLISECONDS);
                        }
                        time -= System.currentTimeMillis() - currentTime;
                    } while(time > 0L);
                    // waitTime 耗尽，退出方法
                    this.acquireFailed(waitTime, unit, threadId);
                    var16 = false;
                    return var16;
                }
            } finally {
                this.unsubscribe((RedissonLockEntry)this.commandExecutor.getNow(subscribeFuture), threadId);
            }
        }
    }
}
```

- 所以使用 tryLock() 时会尝试立即获取锁，获取失败立即返回 false，不发生阻塞
- 使用 tryLock(long waitTime, TimeUnit unit) 则会出发锁重试机制，并且默认使用看门狗机制，不设置固定的 leaseTime，锁成功后，后台自动续期，防止锁提前释放

```java
// 传递 leaseTime = -1 作为特殊标识，让它走无参数走的那个源码，也就是设置默认过期时间为 30 s
public boolean tryLock(long waitTime, TimeUnit unit) throws InterruptedException {
    return this.tryLock(waitTime, -1, unit);
}
```

- 使用 tryLock(long waitTime, long leaseTime, TimeUnit unit) 则表示主动指定了锁的 leaseTime，并且不会再自动开启看门狗续期机制
- 所以更推荐使用第二种方法，让锁依赖看门狗机制，增高程序的可靠性，避免因执行时间过长导致锁失效

当调用 tryLock() 且未指定 leaseTime 时，就会触发 scheduleExpirationRenewal() 方法，它负责注册和启动锁的看门狗续期任务，确保锁不会因为超时而自动释放

```java
protected void scheduleExpirationRenewal(long threadId) {
    // 用来记录锁的续期信息
    ExpirationEntry entry = new ExpirationEntry();
    // 尝试将当前获取到的锁的 key 的名字放入全局管理，如果全局当中有相同名字的就不放入，则复用旧的词条
    ExpirationEntry oldEntry = (ExpirationEntry)EXPIRATION_RENEWAL_MAP.putIfAbsent(this.getEntryName(), entry);
    if (oldEntry != null) {
        // 复用旧词条
        oldEntry.addThreadId(threadId);
    } else {
        entry.addThreadId(threadId);
        try {
            // 给锁续期
            this.renewExpiration();
        } finally {
            if (Thread.currentThread().isInterrupted()) {
                this.cancelExpirationRenewal(threadId, (Boolean)null);
            }
        }
    }
}
```

启动该方法后，它会设置一个延迟时间，10 s 后才会开始执行，如果不设置延迟时间可能导致连续的续期情况，没有让 Redis 休息的机会，续期成功后则会递归的调用本身，

```java
private void renewExpiration() {
    ExpirationEntry ee = (ExpirationEntry)EXPIRATION_RENEWAL_MAP.get(this.getEntryName());
    if (ee != null) { // 如果 ee == null 证明锁已被释放
        Timeout task = this.getServiceManager().newTimeout(new TimerTask() {
            public void run(Timeout timeout) throws Exception {
                ExpirationEntry ent = (ExpirationEntry)RedissonBaseLock.EXPIRATION_RENEWAL_MAP.get(RedissonBaseLock.this.getEntryName());
                if (ent != null) {
                    Long threadId = ent.getFirstThreadId();
                    if (threadId != null) {
                        // 执行续期，通过 lua 脚本判断该线程的锁是否还存在并完成重置过期时间的操作
                        CompletionStage<Boolean> future = RedissonBaseLock.this.renewExpirationAsync(threadId);
                        future.whenComplete((res, e) -> {
                            if (e != null) {
                                RedissonBaseLock.log.error("Can't update lock {} expiration", RedissonBaseLock.this.getRawName(), e);
                                RedissonBaseLock.EXPIRATION_RENEWAL_MAP.remove(RedissonBaseLock.this.getEntryName());
                            } else {
                                if (res) {
                                    // 续期成功，递归再次调用 renewExpiration()
                                    RedissonBaseLock.this.renewExpiration();
                                } else {
                                    // 锁不存在，取消看门狗续期工作
                                    RedissonBaseLock.this.cancelExpirationRenewal((Long)null, (Boolean)null);
                                }

                            }
                        });
                    }
                }
            }
            // 设置延迟时间 = internalLockLeaseTime / 3（默认 10 秒）
        }, this.internalLockLeaseTime / 3L, TimeUnit.MILLISECONDS);
        ee.setTimeout(task);
    }
}
```

在执行续期操作前需要判断锁是否仍然属于当前线程，如果存在就重置过期时间并返回 1，所以重置的具体操作其实是在这里完成的

```java
protected CompletionStage<Boolean> renewExpirationAsync(long threadId) {
    return this.evalWriteSyncedAsync(this.getRawName(), LongCodec.INSTANCE, RedisCommands.EVAL_BOOLEAN, 
            "if (redis.call('hexists', KEYS[1], ARGV[2]) == 1) " +
                    "then redis.call('pexpire', KEYS[1], ARGV[1]); " +
                    "return 1; " +
                    "end; " +
                    "return 0;",
            Collections.singletonList(this.getRawName()), this.internalLockLeaseTime, this.getLockName(threadId));
}
```

****
#### 5.4 Redisson 的 MutiLock 原理

Redis 是一种多节点的技术，如果只给主节点添加锁，那么在分布式环境无法保障数据一致性与可靠性，所以需要在所有节点加上同一个锁，保证所有节点的数据一致性，防止并发冲突。

可以看到 redissonClient.getMultiLock(....) 方法会进入以下方法，这个方法里面会把所有的锁添加进集合中，那很明显，后续要完成主从的一致，就需要从集合中取出所有的锁

```java
final List<RLock> locks = new ArrayList();

public RedissonMultiLock(RLock... locks) {
    if (locks.length == 0) {
        throw new IllegalArgumentException("Lock objects are not defined");
    } else {
        this.locks.addAll(Arrays.asList(locks));
    }
}
```

```java
public boolean tryLock() {
    try {
        return this.tryLock(-1L, -1L, (TimeUnit)null);
    } catch (InterruptedException var2) {
        Thread.currentThread().interrupt();
        return false;
    }
}
```

这个 tryLock 源码就和上面的单个锁的不一样了，通过 tryLock 方法尝试依次获取多实例的锁，核心逻辑是所有节点必须全部加锁成功才算整体锁成功，若部分失败则释放已加锁节点并根据等待时间重试，
同时加锁成功后会统一重置锁的有效期，如果方法执行成功，证明全局锁加锁成功

```java
public boolean tryLock(long waitTime, long leaseTime, TimeUnit unit) throws InterruptedException {
    // 判断有没有传递手动设置的过期时间
    long newLeaseTime = -1L;
    // 如果手动设置了过期时间
    if (leaseTime > 0L) {
        // 如果手动设置了等待时间
        if (waitTime > 0L) {
            // 将过期时间设置为等待时间的两倍，防止还在重试等待中就自动释放了
            newLeaseTime = unit.toMillis(waitTime) * 2L;
        } else {
            // 没设置等待时间，那就用手动设置的过期时间
            newLeaseTime = unit.toMillis(leaseTime);
        }
    }
    long time = System.currentTimeMillis();
    long remainTime = -1L;
    if (waitTime > 0L) {
        remainTime = unit.toMillis(waitTime);
    }
    // 计算锁的等待时间，这个方法就是直接把剩余时间传回
    long lockWaitTime = this.calcLockWaitTime(remainTime);
    // 设置失败锁的限制，也就是 0 
    int failedLocksLimit = this.failedLocksLimit();
    // 遍历所有的锁
    List<RLock> acquiredLocks = new ArrayList(this.locks.size());
    ListIterator<RLock> iterator = this.locks.listIterator();
    // 依次获取每一把锁
    while(iterator.hasNext()) {
        RLock lock = (RLock)iterator.next();
        
        boolean lockAcquired;
        try {
            if (waitTime <= 0L && leaseTime <= 0L) {
                // 这个方法就是不传递参数的获取锁的方法，只进行获取一次
                lockAcquired = lock.tryLock();
            } else {
                long awaitTime = Math.min(lockWaitTime, remainTime);
                // 这个方法同理
                lockAcquired = lock.tryLock(awaitTime, newLeaseTime, TimeUnit.MILLISECONDS);
            }
        } catch (RedisResponseTimeoutException var21) {
            this.unlockInner(Arrays.asList(lock));
            lockAcquired = false;
        } catch (Exception var22) {
            lockAcquired = false;
        }
        // 判断是否拿到锁
        if (lockAcquired) {
            // 成功就放到锁的集合中
            acquiredLocks.add(lock);
        } else {
            // 获取锁失败，用锁的总数量（初始设置的）- 已经获取的锁的数量是否等于失败锁的限制（0）
            if (this.locks.size() - acquiredLocks.size() == this.failedLocksLimit()) {
                // 也就是说想跳出循环就必须锁的数量和已获取锁的数量一致
                break;
            }
            if (failedLocksLimit == 0) {
                this.unlockInner(acquiredLocks);
                if (waitTime <= 0L) {
                    // 如果没有设置等待时间，证明不想重试获取锁，那么一次获取失败就结束程序
                    return false;
                }
                failedLocksLimit = this.failedLocksLimit();
                acquiredLocks.clear();
                while(iterator.hasPrevious()) {
                    // 指针前置
                    iterator.previous();
                }
            } else {
                --failedLocksLimit;
            }
        }
        // 如果还有剩余时间存在
        if (remainTime > 0L) {
            remainTime -= System.currentTimeMillis() - time;
            time = System.currentTimeMillis();
            // 如果以上操作把等待时间耗尽了，就证明没有机会获取下一把锁了
            if (remainTime <= 0L) {
                // 释放锁
                this.unlockInner(acquiredLocks);
                return false;
            }
        }
    }
    // 判断是否手动设置了过期时间，如果手动设置了，那就重置锁的有效期
    if (leaseTime > 0L) {
        acquiredLocks.stream().map((l) -> (RedissonBaseLock)l).map((l) -> l.expireAsync(unit.toMillis(leaseTime), TimeUnit.MILLISECONDS)).forEach((f) -> f.toCompletableFuture().join());
    }
    return true;
}
```

****
### 6. 秒杀优化

当前程序的下单流程：当用户发起请求，此时会请求 nginx，nginx 会访问到 tomcat，而 tomcat 中的程序，会进行串行操作，分成如下几个步骤：

1. 查询优惠卷 
2. 判断秒杀库存是否足够 
3. 查询订单 
4. 校验是否是一人一单 
5. 扣减库存 
6. 创建订单

在这六步操作中，又有很多操作是要去操作数据库的，而且还是一个线程串行执行， 这样就会导致程序执行的很慢，所以需要异步程序执行，开启多个线程，一个线程执行查询优惠卷，
一个执行判断扣减库存，一个去创建订单等等，然后在做统一的返回，虽然这样做看上去可以并行的执行操作，但操作之间存在数据强依赖，没有库存订单根本不能创建，所以即使多线程拆开，
最终还是得汇总各步骤结果。

于是可以将耗时比较短的逻辑判断放入到 redis 中，比如是否库存足够，比如是否一人一单，只要这种逻辑可以完成，就意味着一定可以下单完成，然后只需要进行快速的逻辑判断，
根本就不用等下单逻辑走完，就可以直接给用户返回成功信息，然后再在后台开一个线程，让后台线程慢慢的去执行。因为存在一人一单这种限制，所以将用户信息存入 redis 时可以考虑使用 set 结构，
确保信息不能重复，即保证一人一单。

当用户下单之后，判断库存是否充足只需要到 redis 中根据 key 找对应的 value 是否大于 0 即可，如果小于 0，证明库存不足，直接结束，
如果大于 0，继续在 redis 中判断用户是否可以下单，如果 set 集合中没有这条数据，说明他可以下单，并将 userId 和优惠卷存入到 redis 中，整个过程需要保证是原子性的，可以使用 lua 来操作。

需求：

* 新增秒杀优惠券的同时，将优惠券信息保存到Redis中
* 基于Lua脚本，判断秒杀库存、一人一单，决定用户是否抢购成功

使用 [lua](./hm-dianping/src/main/resources/seckill.lua) 脚本即可完成上述两个需求，根据 lua 脚本返回的值来判断是否具有购买资格（0 即可以购买）

* 如果抢购成功，将优惠券id和用户id封装后存入阻塞队列
* 开启线程任务，不断从阻塞队列中获取信息，实现异步下单功能

剩下两个需求则需要用到阻塞队列来完成，因为当没有值存在队列中时，阻塞队列就不会不会运行，避免长时间占用资源。

当用户点击优惠秒删卷后，进入该方法，通过 lua 脚本判断是否有资格购买秒杀卷（即第一次购买或尚有库存），判断为有资格后将订单信息放入阻塞队列中，让它异步进行后续对数据库的操作，
此时则可以直接将购买成功的信息返回给前端。

```java
@Override
public Result seckillVoucher(Long voucherId) {
    Long userId = UserHolder.getUser().getId();
    // 获取订单 ID
    long orderId = redisIdWorker.nextId("order");
    // 1. 执行lua脚本
    Long result = stringRedisTemplate.execute(
            SECKILL_SCRIPT,
            Collections.emptyList(), // 因为没有 KEYS，所以传一个空集合
            voucherId.toString(), userId.toString(), String.valueOf(orderId) // 其他类型参数，即 ARGS
    );
    int r = result.intValue();
    // 2. 判断结果是否为0
    if (r != 0) {
        // 2. 不为0 ，代表没有购买资格
        return Result.fail(r == 1 ? "库存不足" : "不能重复下单");
    }
    // 有购买资格，把下单信息保存到阻塞队列
    VoucherOrder voucherOrder = new VoucherOrder();
    voucherOrder.setId(orderId);
    voucherOrder.setUserId(userId);
    voucherOrder.setVoucherId(voucherId);
    // 放入阻塞队列
    orderTasks.add(voucherOrder);
    // 获取代理对象
    proxy = (IVoucherOrderService) AopContext.currentProxy();
    // 3. 返回订单id
    return Result.ok(orderId);
}
```

阻塞队列本质只是数据结构，它是是一个线程安全的队列，所以它本身是不主动消费数据的，必须要有消费线程数据才会被处理，当队列里没有数据时，orderTasks.take() 方法就会原地等待，直到数据出现。

```java
// 定义了一个阻塞队列，用于存放 VoucherOrder 对象，队列容量是 1024 * 1024 = 1048576，即大约存放这么多个元素
private BlockingQueue<VoucherOrder> orderTasks = new ArrayBlockingQueue<>(1024 * 1024);
// 异步处理线程池
private static final ExecutorService SECKILL_ORDER_EXECUTOR = Executors.newSingleThreadExecutor();
// 当前类初始化完成后开始执行
@PostConstruct
private void init() {
    SECKILL_ORDER_EXECUTOR.submit(new VoucherOrderHandler());
}

private class VoucherOrderHandler implements Runnable {
    @Override
    public void run() {
        while (true){
            try {
                // 1. 获取队列中的订单信息（获取队列头）
                VoucherOrder voucherOrder = orderTasks.take();
                // 2. 创建订单
                handleVoucherOrder(voucherOrder);
            } catch (Exception e) {
                log.error("处理订单异常", e);
            }
        }
    }
}

// 线程内部处理订单流程
private void handleVoucherOrder(VoucherOrder voucherOrder) {
    // 1. 获取用户
    Long userId = voucherOrder.getUserId();
    // 2. 创建锁对象
    RLock lock = redissonClient.getLock("lock:order:" + userId);
    // 3. 尝试获取锁
    boolean isLock = lock.tryLock();
    // 4. 判断是否获得锁成功
    if (!isLock) {
        // 获取锁失败，直接返回失败或者重试
        log.error("不允许重复下单！");
        return;
    }
    try {
        // 注意：由于是 spring 的事务是放在 threadLocal 中，而此时的是多线程，事务会失效
        proxy.createVoucherOrder(voucherOrder);
    } finally {
        // 释放锁
        lock.unlock();
    }
}
```

需要注意的是：创建订单信息时不能和之前一样直接通过 UserHolder 获取用户信息，因为用户信息是存储在线程 ThreadLocal 中的，而创建订单的方法是在子线程中执行的，
所以直接通过它会获取到错误的信息，所以传递参数时不能再传递一个 orderId，而是直接把整个 VoucherOrder 传递过来，从阻塞队列中已经包装好的对象里获取

```java
@Transactional
public void createVoucherOrder(VoucherOrder voucherOrder) {
    // Long userId = UserHolder.getUser().getId();
    Long userId = voucherOrder.getUserId();
    // 查询订单
    Long count = query().eq("user_id", userId).eq("voucher_id", voucherOrder.getVoucherId()).count();
    // 判断是否存在
    if (count > 0) {
        // 用户已经购买过了
        log.error("用户已经购买过一次！");
        return;
    }
    // 6. 扣减库存
    boolean success = seckillVoucherService.update()
            .setSql("stock = stock - 1") // set stock = stock - 1
            .eq("voucher_id", voucherOrder.getVoucherId()).gt("stock", 0) // where id = ? and stock > 0
            .update();
    if (!success) {
        // 扣减失败
        log.error("库存不足！");
        return;
    }
    save(voucherOrder);
}
```

以上通过 lua 前端管理抢购资格与阻塞队列和异步线程的搭配空值数据库数据更新的组合确实能在一定程度上提高运行效率，但是仍然存在一些问题，该案例中的阻塞队列使用的 JDK 内置的，
也就是说使用的是 JVM 的内存来存储信息，在高并发的情况下可能导致 JVM 内存崩溃，所以提前设置了阻塞队列的大小，但是也可能存在不够用的情况；其次，这些存储在阻塞队列（内存）的信息并不一定安全，
因为它不可能第一时间全部完成操作，所以存在内存崩溃时数据仍未写进数据库的情况，造成数据的丢失。

****
#### 












