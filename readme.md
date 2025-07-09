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
因为它不可能第一时间全部完成操作，所以存在内存崩溃时数据仍未写进数据库的情况，造成数据的丢失，即异步操作时不具备原子性。

****
### 7. Redis 消息队列

#### 7.1 概念

消息队列就是存放消息的队列，由生产者将消息发送到队列，然后队列缓存消息，解耦生产与消费速度。最简单的消息队列模型包括3个角色：

* 消息队列：存储和管理消息，也被称为消息代理（Message Broker）
* 生产者：发送消息到消息队列
* 消费者：从消息队列获取消息并处理消息

在秒杀场景中就是：下单之后，利用 redis 去进行校验下单条件，再通过队列把消息发送出去，然后启动一个线程去消费这个消息，完成解耦，同时也加快响应速度。
而存储在消息队列中的数据独立于 JVM 内存之外，并且不受阻塞队列的大小限制，所以会更适合当前的情况。

1、基于 List 的消息队列实现

队列是入口和出口不在一边，而基于 Redis 的 List 结构可以通过 LPUSH + BRPOP 或者 RPUSH + LPOP 的组合来简单高效地模拟出消息队列。
不过要注意的是，当队列中没有消息时使用 RPOP 或 LPOP 操作会返回 null，并不像 JVM 的阻塞队列那样会阻塞并等待消息。
因此这里应该使用 BRPOP 或者 BLPOP 来实现阻塞效果。

优点：

* 利用Redis存储，不受限于JVM内存上限
* 基于Redis的持久化机制，数据安全性有保证
* 可以满足消息有序性

缺点：

* 无法避免消息丢失（宕机时数据可能还没来得及处理）
* 只支持单消费者（多消费者会打乱逻辑，导致 List 中元素混乱，影响业务正确性）

2、基于 PubSub 的消息队列

PubSub（发布订阅）是 Redis2.0 版本引入的消息传递模型，消费者可以订阅一个或多个 channel，生产者向对应 channel 发送消息后，订阅该 channel 的都能收到相关消息。

```redis
SUBSCRIBE channel [channel ...]   # 订阅一个或多个频道
PUBLISH channel message           # 向指定频道发布消息
PSUBSCRIBE pattern [pattern ...]  # 订阅匹配模式的所有频道（支持通配符）
```

例如：

```redis
publish order.queue msg1 # 在 order.queue 中发布信息 msg1
subscribe  order.queue # 订阅 order.queue 
psubscribe order.* # 订阅 order 下的所有
```

优点：

* 采用发布订阅模型，支持多生产、多消费

缺点：

* 不支持数据持久化
* 无法避免消息丢失
* 消息堆积有上限，超出时数据丢失

基于以上，实际使用中不如使用 List

3、基于 Stream 的消息队列

它是 Redis 5.0 版本引入的新型数据结构，支持多消费者、消费确认、消息持久化、消费回溯功能，是一个一个功能非常完善的消息队列。

```redis
XADD key [NOMKSTREAM] [MAXLEN|MINID [=|~] threshold [LIMIT count]] *|id field value [field value ...]
summary: Appends a new message to a stream. Creates the key if it doesn't exist.
```

- [NOMKSTREAM]:如果队列不存在，默认自动创建一个
- [MAXLEN|MINID [=|~] threshold [LIMIT count]]：设置消息队列的最大消息数
- *|id：消息的唯一 ID，* 代表由 Redis 自动生成，格式是 `<时间戳>-<序列号>`，例如 1656580000000-0
- field value [field value ...]：发送到消息队列的信息，称为 Entry，格式为多个 key-value 键值对

例如：

```redis
XADD mystream * orderId 123 userId 456
```

- 插入 orderId:123, userId:456
- 自动生成唯一消息 ID
- 数据持久化存储

```redis
XREAD [COUNT count] [BLOCK milliseconds] STREAMS key [key ...] id [id ...]
```

- [COUNT count]：每次读取信息的最大数量
- [BLOCK milliseconds]：当没有消息时是否阻塞，阻塞时间是多少
- STREAMS key [key ...]：要从哪个队列读取消息，key 就是队列名
- id [id ...]：起始 id，只返回大于该 id 的消息；0：代表从第一个消息开始；$：代表从最新的消息开始

例如：

```redis
XREADGROUP GROUP mygroup consumer1 COUNT 1 BLOCK 2000 STREAMS mystream >
```

- mygroup：消费组
- consumer1：消费者名称
- `>`：表示读取尚未消费的新消息
- BLOCK 2000：阻塞等待 2 秒，无新消息自动返回

注意：当指定起始 ID 为 $ 时，代表读取最新的消息，如果处理一条消息的过程中，又有超过 1 条以上的消息到达队列，则下次获取时也只能获取到最新的一条，会出现漏读消息的问题


****
#### 7.2 基于 Stream 的消息队列-消费者组

消费者组（Consumer Group）：将多个消费者划分到一个组中，监听同一个队列，具备下列特点：

1. 消息分流：

队列中的消息会分流给组内的不同消费者，而不是重复消费，从而加快消费处理的速度

2. 消息标示

消费者组会维护一个标示，记录最后一个被处理的消息，哪怕消费者宕机重启，也依然会从标示之后开始读取信息，确保每一个消息都会被消费

3. 消息确认

消费者获取消息后，消息处于 pending 状态并会存入一个 pending-list。当消息处理完成后需要通过 XACK 来确认消息，标记消息为已处理，这样才会从 pending-list 中移除。

创建消费者组：

```redis
XGROUP CREATE key groupName ID [MKSTREAM]
```

- key：队列名称（Stream 名称） 
- groupName：消费者组名称，多个消费者共享一个组
- ID：
  - $：从最后一条消息开始（只消费新消息，常用） 
  - 0：从第一条消息开始（消费所有历史消息）
- MKSTREAM：若 Stream 不存在则自动创建

例如：

```redis
XGROUP CREATE mystream mygroup $ MKSTREAM
```

- mystream：Stream 队列名称 
- mygroup：消费者组名称
- $：从最新消息开始消费 
- MKSTREAM：如果 mystream 不存在，自动创建

```text
127.0.0.1:6379> XGROUP CREATE mystream mygroup $ MKSTREAM
OK
127.0.0.1:6379> XADD mystream * orderId 123 userId 456
"1751290033062-0"
```

删除消费者组：

```redis
XGROUP DESTROY key groupName
```

例如：删除 mystream 队列下的 mygroup 消费者组，不影响队列中已有消息

```redis
XGROUP DESTROY mystream mygroup
```

手动创建消费者：这个命令通常不需要手动执行，XREADGROUP 时如果消费者不存在，系统会自动创建

```redis
XGROUP CREATECONSUMER key groupName consumerName
```

例如：

```redis
XGROUP CREATECONSUMER mystream mygroup consumer1
```

删除指定消费者：

```redis
XGROUP DELCONSUMER key groupName consumerName
```

例如：删除 mystream 队列下 mygroup 组的 consumer1 消费者信息，但仅删除消费者标记，未确认的 Pending 消息仍保留

```redis
XGROUP DELCONSUMER mystream mygroup consumer1
```

消费者组读取消息:

```redis
XREADGROUP GROUP groupName consumerName [COUNT count] [BLOCK ms] [NOACK] STREAMS key ID
```

* group：消费组名称
* consumer：消费者名称，如果消费者不存在，会自动创建一个消费者
* count：本次查询的最大数量
* BLOCK milliseconds：当没有消息时最长等待时间
* NOACK：无需手动ACK，获取到消息后自动确认
* STREAMS key：指定队列名称
* ID：获取消息的起始ID：
  * ">"：从下一个未消费的消息开始
  * 其它：根据指定id从pending-list中获取已消费但未确认的消息，例如0，是从pending-list中的第一个消息开始

例如：只消费 mygroup 队列中的未消费的新消息，最多读取 2 条消息并最多阻塞等待 5 秒

```redis
XREADGROUP GROUP mygroup consumer1 COUNT 2 BLOCK 5000 STREAMS mystream >
```

```text
127.0.0.1:6379> XREADGROUP GROUP mygroup consumer1 COUNT 2 BLOCK 5000 STREAMS mystream >
1) 1) "mystream"
   2) 1) 1) "1751290033062-0"
         2) 1) "orderId"
            2) "123"
            3) "userId"
            4) "456"
```

手动确认消息：

```redis
XACK mystream mygroup 1657000000000-0
```

- mystream：队列名 
- mygroup：消费者组 
- 1657000000000-0：消息 ID

确认消费完成后，消息从 Pending-List 移除

查看未确认消息：

```redis
XPENDING mystream mygroup
```

使用消费组的优点：

1、消息可回溯

Redis Stream 内部结构类似时间线，所有消息严格有序存储，并且消息不会因消费而立即删除，后续可以通过指定消息 ID，任意时刻读取历史消息，例如：

```redis
# 从 ID 为 0 开始读取，意味着回溯消费全部历史消息
XREADGROUP GROUP mygroup consumer STREAMS mystream 0
```

2、多消费者争抢消息，加快消费速度

Redis Stream 支持消费组模式，在同一个消费组下可以有多个消费者并发消费同一队列，而 Redis 会自动做消息分配，保证每条消息只被一个消费者处理：

```redis
# 同时获取最新的未消费消息，但获取的消息随机
XREADGROUP GROUP mygroup consumer1 STREAMS mystream >
XREADGROUP GROUP mygroup consumer2 STREAMS mystream >
```

3、可以阻塞读取，利用 BLOCK 自定义阻塞时间，模拟阻塞队列效果

4、没有消息漏读的风险

消息被消费者读取后，Redis 并不删除消息，未确认的消息保存在 Pending-List， 即使消费者异常、宕机，消息仍保留，可通过 XPENDING 查看未确认的消息

5、消息确认机制的存在保证了消息至少会被消费一次


****
#### 7.3 基于 Redis 的 Stream 结构作为消息队列实现异步秒杀下单

需求：

* 创建一个Stream类型的消息队列，名为stream.orders
* 修改之前的秒杀下单 Lua 脚本，在认定有抢购资格后，直接向 stream.orders 中添加消息，内容包含 voucherId、userId、orderId（避免直接使用 Java 代码向 redis 中发送数据）
* 项目启动时，开启一个线程任务，尝试获取 stream.orders 中的消息，完成下单

修改 lua 脚本，新增执行语句：

```lua
-- 发送消息到队列中， XADD stream.orders * k1 v1 k2 v2 ...
redis.call('xadd', 'stream.orders', '*', 'userId', userId, 'voucherId', voucherId, 'id', orderId)
```

因为现在使用的是 Redis 的 Stream 结构作为消息队列，所以可以不再使用阻塞队列来存储数据，但依然需要异步线程来提高执行效率。异步线程中执行的内容发生了较大的变化，
但后续流程仍需要 lua 脚本中返回的值（0，1，2）来判断用户当前订单情况，以提前返回正确的信息。如 [VoucherOrderServiceImpl#seckillVoucher](./hm-dianping/src/main/java/com/hmdp/service/impl/VoucherOrderServiceImpl.java)方法，
在以前的版本需要创建一个 VoucherOrder 对象，然后封装 orderId、userId、voucherId，现在就不用了，因为 lua 脚本中将 'userId', userId, 'voucherId', voucherId, 'id', orderId 信息放在了队列中，
所以它就需要通过 read() 方法读取信息，该方法有三个参数，第一个包装消息组的信息（消费组名和消费者名），第二个包装读取时的操作（读几条、阻塞多久），第三个包装消息队列的信息（队列名、从第几条开始读）。

而该方法的主要逻辑就是通过消费组的特性模拟阻塞队列，然后解析获取到的信息（类似 Map 集合的操作），最重要的是它要进行确认，因为消费组从队列中获取到消息后不会立即删除，
消息会进入 pending-list（待确认消息列表），如果不确认，该消息可能被 Redis 误认为未完成消息，该消息就可能会被其他消费者获取到，造成数据的紊乱。

```java
private class VoucherOrderHandler implements Runnable {
String queueName = "stream.orders";

@Override
public void run() {
    while (true) {
        try {
            // 1. 获取消息队列中的订单信息：XREADGROUP GROUP g1 c1 COUNT 1 BLOCK 2000 STREAMS streams.order >
            List<MapRecord<String, Object, Object>> list = stringRedisTemplate.opsForStream().read(
                    Consumer.from("g1", "c1"),
                    StreamReadOptions.empty().count(1).block(Duration.ofSeconds(2)),
                    StreamOffset.create(queueName, ReadOffset.lastConsumed())
            );
            // 2. 判断消息获取是否成功
            if (list == null || list.isEmpty()) {
                continue;
            }
            // 3. 解析消息中的信息
            MapRecord<String, Object, Object> entries = list.get(0);
            Map<Object, Object> values = entries.getValue();
            VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(values, new VoucherOrder(), true);
            // 4. 从消息队列中成功获取消息，开始创建订单
            handleVoucherOrder(voucherOrder);
            // 5. 进行 ACK 确认:XACK stream.order g1 id
            stringRedisTemplate.opsForStream().acknowledge(queueName, "g1", entries.getId());
        } catch (Exception e) {
            log.error("处理订单异常", e);
            // 当发生异常证明消息没有进行 ACK 确认，就要进 pending-list 中进行确认
            handlePendingList();
        }
    }
}
```

所以如果发生了异常导致消息没法主动确认时，就需要在异常处理的时候进行二次确认，避免上述情况发生，当然这里不用一直重复尝试获取 pending-list 中的信息，
因为第一次获取失败证明发生异常时上面的程序还没来得及读取信息，所以直接结束循环跳出方法，通过这种机制可以很大程度上保证 lua 脚本返回成功信息与同步数据库的原子性。

```java
private void handlePendingList() {
    while (true) {
        try {
            // 1. 获取 pending-list 中的订单信息：XREADGROUP GROUP g1 c1 COUNT 1 STREAMS streams.order 0
            List<MapRecord<String, Object, Object>> list = stringRedisTemplate.opsForStream().read(
                    Consumer.from("g1", "c1"),
                    StreamReadOptions.empty().count(1),
                    StreamOffset.create(queueName, ReadOffset.from("0"))
            );
            // 2. 判断消息获取是否成功
            if (list == null || list.isEmpty()) {
                // 获取失败，证明 pending-list 中没有异常消息，结束循环
                break;
            }
            // 3. 解析消息中的信息
            MapRecord<String, Object, Object> entries = list.get(0);
            Map<Object, Object> values = entries.getValue();
            VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(values, new VoucherOrder(), true);
            // 4. 从消息队列中成功获取消息，开始创建订单
            handleVoucherOrder(voucherOrder);
            // 5. 进行 ACK 确认:XACK stream.order g1 id
            stringRedisTemplate.opsForStream().acknowledge(queueName, "g1", entries.getId());
        } catch (Exception e) {
            log.error("处理 pending-list 订单异常", e);
        }
    }
}
```

****
### 8. 达人探店

#### 8.1 查看探店笔记与点赞功能

查看探店笔记，通过数据库获取到 Blog 的信息，即返回一个封装好的 Blog 对象给前端

```java
@Override
public Result queryById(Long id) {
    // 1. 查询 blog
    Blog blog = getById(id);
    if (blog == null) {
        return Result.fail("笔记不存在！");
    }
    // 2. 查询 blog 相关用户
    queryBlogUser(blog);
    // 3. 查询 blog 是否已点赞
    isBlogLiked(blog);
    return Result.ok(blog);
}

private void queryBlogUser(Blog blog) {
    Long userId = blog.getUserId();
    User user = userService.getById(userId);
    blog.setName(user.getNickName());
    blog.setIcon(user.getIcon());
}
```

初始的点赞功能只是简单的给 Blog 设置了一个字段，只要有用户点赞了，这个字段就会 +1，但是这种方式会导致一个用户可以给一片笔记点无限个赞，这是需要避免的，
所以需要给 Blog 添加一个新的字段 isLike 用来标示当前用户是否已点赞（true 则已点赞），可以利用 Redis 的 set 集合判断是否点赞过，把已点赞的用户的信息放到 set 集合中，
然后每次查询笔记时（即加载页面时）会去判断 set 集合中是否存在当前用户的信息，如果存在证明点赞了，就高亮点赞标识。

```java
// 每次加载首页时会进入此方法
@Override
public Result queryHotBlog(Integer current) {
    // 根据用户查询
    Page<Blog> page = query()
            .orderByDesc("liked")
            .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
    // 获取当前页数据
    List<Blog> records = page.getRecords();
    // 查询用户
    records.forEach(blog ->{
        queryBlogUser(blog);
        isBlogLiked(blog);
    });
    return Result.ok(records);
}
```

```java
private void isBlogLiked(Blog blog) {
    // 1. 获取登录用户
    Long userId = UserHolder.getUser().getId();
    // 2. 判断当前登录用户是否已经点赞
    String key = RedisConstants.BLOG_LIKED_KEY + blog.getId();
    Boolean isMember = stringRedisTemplate.opsForSet().isMember(key, userId.toString());
    // 为 true 证明已经点过赞了
    blog.setIsLike(BooleanUtil.isTrue(isMember));
}
```

用户的点赞逻辑则是：先判断谋篇笔记是否已点赞，没点赞的就可以点，修改数据库的信息，然后将该用户的信息存到 Redis 的 set 集合中，
如果对已点赞的笔记再点以此就是取消点赞，那么不仅要修改数据库，还要从 Redis 中找到 set 集合中对应的本用户的信息，然后删除。

```java
 @Override
public Result likeBlog(Long id) {
    // 1. 获取登录用户
    Long userId = UserHolder.getUser().getId();
    // 2. 判断当前登录用户是否已经点赞
    String key = RedisConstants.BLOG_LIKED_KEY + id;
    Boolean isMember = stringRedisTemplate.opsForSet().isMember(key, userId.toString());
    if (BooleanUtil.isFalse(isMember)) {
        // 3. 如果未点赞，则该用户可以点赞
        // 数据库点赞数+1
        boolean isSuccess = update().setSql("liked = liked + 1").eq("id", id).update();
        // 保存用户信息到 Redis 的 set 集合，以此作为已点赞
        if (isSuccess) {
            stringRedisTemplate.opsForSet().add(key, userId.toString());
        }
    } else {
        // 4. 如果已点赞，取消点赞
        // 数据库点赞数-1
        boolean isSuccess = update().setSql("liked = liked - 1").eq("id", id).update();
        // 把用户从 Redis 的 set 集合移除
        if (isSuccess) {
            stringRedisTemplate.opsForSet().remove(key, userId.toString());
        }
    }
    return Result.ok();
}
```

****
#### 8.2 点赞排行榜

因为要设置点赞的排行榜，让先点赞的人显示在前面，所以就不能再使用上面的那种 set 集合的方式，因为 set 集合虽然保证数据唯一，但无法排序，所以可以使用 sortedset 集合，
它既保证了数据的唯一，也能根据 score 的值进行排序，所以只需要把点赞的时间作为 score，即可完成排序。

在原有的基础上把 set 集合判断是否点赞的代码换成 sortedset 的，后续根据 score 是否为 null 判断有无点赞

```java
// Boolean isMember = stringRedisTemplate.opsForSet().isMember(key, userId.toString());
Double score = stringRedisTemplate.opsForZSet().score(key, user.getId().toString());
```

查询点赞排序列表返回给前端，需要注意的是 mybatisplus 使用的查询是 in 的方式，需要在后面拼接 order by 来手动设置查询结果的先后顺序（依照 sortedset 中的数据的先后）

```java
@Override
public Result queryBlogLikes(Long id) {
    String key = RedisConstants.BLOG_LIKED_KEY + id;
    // 1. 查询 top5 的点赞用户 zrange key 0 4
    Set<String> top5 = stringRedisTemplate.opsForZSet().range(key, 0, 4);
    if (top5 == null || top5.isEmpty()) {
        return Result.ok(Collections.emptyList());
    }
    // 2. 解析出其中的用户 id
    List<Long> ids = top5.stream().map(Long::valueOf).collect(Collectors.toList());
    String idStr = StrUtil.join(",", ids); // 在 id 后面拼接 ','
    // 因为查询语句用的是 in，所以最终查出的数据可能与真实的插入顺序无关，所以需要手动设置最后的排序，让 sorted 前面的数据做头
    // 3. 根据用户 id 查询用户 WHERE id IN ( 5 , 1 ) ORDER BY FIELD(id, 5, 1)
    List<UserDTO> userDTOS = userService.query()
            .in("id", ids)
            .last("ORDER BY FIELD(id," + idStr + ")").list() // 在后面拼接，手动设置排序
            .stream()
            .map(user -> BeanUtil.copyProperties(user, UserDTO.class))
            .collect(Collectors.toList());
    // 4.返回
    return Result.ok(userDTOS);
}
```

****
### 9. 好友关注

#### 9.1 关注与取消关注

基于 tb_follow 表数据结构，关联当前用户与关注者的 ID，当某用户点击关注时，关联操作者的 ID 与该篇笔记的作者的 ID 到这张表中，取消关注则根据两人的 ID 作为条件查询数据库中是否存在该条记录，
存在就直接删掉这条记录。

```java
// 关注
public Result follow(Long followUserId, Boolean isFollow) {
    // 1. 获取登录用户
    Long userId = UserHolder.getUser().getId();
    // 2. 判断到底是关注还是取关
    if (isFollow) {
        // 关注，新增数据
        Follow follow = new Follow();
        follow.setUserId(userId);
        follow.setFollowUserId(followUserId);
        save(follow);
    } else {
        // 3. 取关，删除 delete from tb_follow where user_id = ? and follow_user_id = ?
        Map<String, Object> condition = new HashMap<>();
        condition.put("user_id", userId);
        condition.put("follow_user_id", followUserId);
        removeByMap(condition);
    }
    return Result.ok();
}

public Result isFollow(Long followUserId) {
    // 1. 获取登录用户
    Long userId = UserHolder.getUser().getId();
    // 2. 查询是否关注 select count(*) from tb_follow where user_id = ? and follow_user_id = ?
    Long count = query().eq("user_id", userId).eq("follow_user_id", followUserId).count();
    return Result.ok(count > 0);
}
```

****
#### 9.2 共同关注列表

可以利用 Redis 中的 set 数据结构实现共同关注功能，它有个求交集的命令，可以筛选出两个 set 集合中共有的元素，所以在某位用户关注了某个作者时，需要将作者的 id 放进 set 集合，
用当前用户的 id 作为 set 集合的 key，后面查看某个作者的页面时则通过 SINTER 判断这两个人的 set 集合是否有交集，有就把交集取出，封装成详细对象

```java
public Result follow(Long followUserId, Boolean isFollow) {
    // 关注成功，把被关注用户的 id 放入 redis 的 set 集合中 sadd userId followerUserId
    stringRedisTemplate.opsForSet().add(key, followUserId.toString());
    // 取关， 把关注用户的 id 从 Redis 集合中移除
    stringRedisTemplate.opsForSet().remove(key, followUserId.toString());
}
```

前端访问用户共同关注列表时会访问该方法：

```java
public Result followCommons(Long id) {
    // 1. 获取当前用户
    Long userId = UserHolder.getUser().getId();
    String key = "follows:" + userId;
    // 2. 求当前用户与被关注者的 redis 的 set 集合中的交集
    String key2 = "follows:" + id; // 传进来的 id 是被关注者的 id
    Set<String> intersect = stringRedisTemplate.opsForSet().intersect(key, key2);
    if (intersect == null || intersect.isEmpty()) {
        // 无交集
        return Result.ok(Collections.emptyList());
    }
    // 3. 解析 id 集合
    List<Long> ids = intersect.stream().map(Long::valueOf).collect(Collectors.toList());
    // 4.查询用户
    List<UserDTO> users = userService.listByIds(ids)
            .stream()
            .map(user -> BeanUtil.copyProperties(user, UserDTO.class))
            .collect(Collectors.toList());
    return Result.ok(users);
}
```
#### 9.3 Feed 流

当我们关注了用户后，这个用户发了动态，那么我们应该把这些数据推送给用户，这个需求又被做 Feed 流，关注推送也叫做 Feed 流，直译为投喂。
为用户持续的提供沉浸式的体验，通过无限下拉刷新获取新的信息。对于传统的模式的内容解锁，是需要用户去通过搜索引擎或者是其他的方式去解锁想要看的内容，
对于新型的 Feed 流的的效果，则不需要用户再去推送信息，而是系统分析用户到底想要什么，然后直接把内容推送给用户，从而使用户能够更加的节约时间，不用主动去寻找。

Feed 流的实现有两种模式：

Timeline 模式，不做内容筛选，简单的按照内容发布时间排序，常用于好友或关注，例如朋友圈

* 优点：信息全面，不会有缺失。并且实现也相对简单
* 缺点：信息噪音较多，用户不一定感兴趣，内容获取效率低

智能排序模式，利用智能算法屏蔽掉违规的、用户不感兴趣的内容，推送用户感兴趣信息来吸引用户

* 优点：投喂用户感兴趣信息，用户粘度很高，容易沉迷
* 缺点：如果算法不精准，可能起到反作用

因为目前要实现的是基于关注的好友来实现 Feed 流，所以使用的是 Timeline 模式，该模式的实现方式如下：

* 拉模式

拉模式也叫做读扩散，当用户请求时，系统动态查询数据生成 Feed 流，也就是说发送者只需要发送一条数据到发件箱并附带时间，而有没有人接收取决于用户想不想拉取这条信息，
所以该方法比较节约空间，但是可能存在延迟现象，因为当用户读取数据时才去关注的人里边拉取数据，如果该用户关注了大量的用户，那么此时就会拉取海量的内容，对服务器压力巨大。

* 推模式

该模式也叫做写扩散，发送者会给每个关注自己的人都发送一份信息，不需要用户手动拉取，虽然时效较快，但当关注者较多时就需要写很多分信息，导致内存压力增大

* 推拉结合

该模式也叫做读写混合，兼具推和拉两种模式的优点，它是一个折中的方案，如果发件人是个普通的人，那么就可以采用写扩散的方式，直接把数据写入到他的粉丝中去，因为普通的人他的粉丝关注量比较小，所以这样做没有压力，
如果是大V，那么他是直接将数据先写入到一份到发件箱里边去（非活跃用户则需要主动拉取），然后再写一份到活跃粉丝收件箱里边去，
对于用户来说，如果是活跃粉丝，那么大V和普通的人发的都会直接写入到自己收件箱里边来，而如果是普通的粉丝，由于上线不是很频繁，所以等他们上线时，需要再从发件箱里边去拉信息。


****
#### 9.4 推送消息到粉丝收件箱

需求：

* 修改新增探店笔记的业务，在保存 blog 到数据库的同时，推送到粉丝的收件箱
* 收件箱满足可以根据时间戳排序，可以用 Redis 的 sortedset 结构实现
* 查询收件箱数据时，可以实现分页查询

在实现程序前需要知道的是：Feed 流的本质就是数据实时变化快，整体排序需要动态调整，所以在作者发布笔记时需要将笔记更新到 Redis 中用 sortedset 结构存储，以此达到最新消息在第一位的效果。
查询信息的一种基础写法就是使用分页查询，根据发送的当前页数与提前设定的每页条数，决定当前页显示的内容，而接下来要进行的推送操作就是将最新的笔记作为第一条，所以原先的数据都需要后移，
这就会导致本该第一页获取的内容结果移到了第二页，这就造成分页不稳定，所以应该采取新的分页方式。

按照上面的流程，当作者成功新增笔记后，应该把当前笔记发送到每个粉丝的邮箱中，所以需要先通过数据库查询有哪些人关注了作者，然后用粉丝的 ID 作为 sortedset 集合的 key 存储在 redis 中充当邮箱，后续通过 redis 中的邮箱获取。

```java
public Result saveBlog(Blog blog) {
    // 1. 获取登录用户
    UserDTO user = UserHolder.getUser();
    blog.setUserId(user.getId());
    // 2. 保存探店笔记
    boolean isSuccess = save(blog);
    if(!isSuccess){
        return Result.fail("新增笔记失败!");
    }
    // 3. 查询笔记作者的所有粉丝 select * from tb_follow where follow_user_id = ?
    List<Follow> follows = followService.query().eq("follow_user_id", user.getId()).list();
    // 4. 推送笔记 id 给所有粉丝
    for (Follow follow : follows) {
        // 获取粉丝 id
        Long userId = follow.getUserId();
        // 推送
        String key = RedisConstants.FEED_KEY + userId;
        stringRedisTemplate.opsForZSet().add(key, blog.getId().toString(), System.currentTimeMillis());
    }
    // 5.返回id
    return Result.ok(blog.getId());
}
```

传统的 sortedset 结构的分页查询通常依赖于元素下标，直接使用 ZRANGE 0 9 这类方式会导致无法处理动态更新的数据集，当内容更新时需要频繁的去寻找该条数据处于具体的哪个范围的下标，

```redis
ZRANGE key start stop [WITHSCORES]
```

而滚动分页不依赖固定页码，而是基于上一次查询的结果进行滚动式查询，因为是接收消息，所以可以使用 score（使用时间戳）来完成，通过上一次查询的最大与最小的 score 来选择分页，
因为 sortedset 是天然带顺序的，所以处理起来比较方便，例如第一次查询时可以使用当前的时间作为最大的范围，然后以 0 作为最小范围，然后选择合适的条数获取数据，
下一次查询就以上一次查询的最小值作为本次查询的最大值，最小值依然使用 0，然后选择合适的条数获取数据，以此类推...

但需要注意的是，ZRANGE 命令有些需要注意的地方：

```redis
ZREMRANGEBYSCORE z1 1000 0 WITHSCORES LIMIT 0 3
```

这条命令的 0 和 3 代表从最大值的下 0 个开始获取，也就是从 1000 开始（包含）获取 3 挑，如果后面是 1 和 3，那么就是从最大值的下 1 个开始，也就是 999；
其次如果上一次查询到的数据中有多个相同的 score 作为最小值的话，就需要从下 n 个（从开始获取到最后有几个相同的最小值就跳几个），例如：

```redis
1) "m8"
2) "8"
3) "m7"
4) "6"
5) "m6"
6) "6"
```

上一次获取到的三个数中，最小值是 6，所以下一次的查询就需要从 6 开始然后 limit 2 ，因为 redis 是从头遍历找到第一个符合条件的返回，所以如果给的查询条件是 6，
那么 redis 就会认为你要的 6 是第一个 6，所以下一次查询就要排除掉这两个已经被查过的 6，所以 limit 后面跟的是 2。

具体流程如下，用户访问该页面后则会进入此方法，通过用户的 ID 从 sortedset 集合中获取所有的信息，先从当前时间开始获取，然后依次将最小时间传递给下一次查询请求，
也就是向下翻找内容时，每一次新的请求都会使用上一次请求使用的最小时间作为本次请求的最大时间，同理，如果向上刷新的话，那就是一次全新的请求，从当前时间开始查找。



```java
public Result queryBlogOfFollow(Long max, Integer offset) {
    // 1. 获取当前用户
    Long userId = UserHolder.getUser().getId();
    // 2. 查询收件箱 ZREVRANGEBYSCORE key Max Min LIMIT offset count
    String key = RedisConstants.FEED_KEY + userId;
    Set<ZSetOperations.TypedTuple<String>> typedTuples =
            stringRedisTemplate.opsForZSet().reverseRangeByScoreWithScores(key, 0, max, offset, 2);
    // 3. 非空判断
    if (typedTuples == null || typedTuples.isEmpty()) {
        return Result.ok();
    }
    // 4. 解析数据：blogId、minTime（时间戳）、offset
    List<Long> ids = new ArrayList<>(typedTuples.size());
    long minTime = 0;
    int os = 1; // 计算本次获取的数据中有几个相同的最小值
    for (ZSetOperations.TypedTuple<String> tuple : typedTuples) {
        // 获取 id 并保存在 List 集合中，因为取出的东西是自带顺序的，所以添加进 List 的最后一个元素就是本次查询的最小值
        ids.add(Long.valueOf(tuple.getValue()));
        // 获取 score(时间戳）
        long time = tuple.getScore().longValue();
        // 判断当前遍历到的 score（时间戳）是否与最小 score 一致
        if(time == minTime){
            // 一致
            os++;
        }else{
            // 不一致，跟新最小 score（时间戳）
            minTime = time;
            // 防止最小值前面有非最小的相同 score，所以出现不一致时重置偏移量为 1
            os = 1;
        }
    }
    if (offset > 1 && minTime == max) {
        os = os + offset;
    }
    // 5. 根据 id 查询 blog
    String idStr = StrUtil.join(",", ids); // 与之前的内容同理，mybatisplus 的查询用到了 in，所以要在后面拼接 order by 手动设置查询顺序
    List<Blog> blogs = query().in("id", ids).last("ORDER BY FIELD(id," + idStr + ")").list();
    // 接收到推送后应该更新它的点赞与
    for (Blog blog : blogs) {
        // 查询 blog 有关的用户信息
        queryBlogUser(blog);
        // 查询 blog 是否被点赞
        isBlogLiked(blog);
    }
    // 6. 封装并返回
    ScrollResult r = new ScrollResult();
    r.setList(blogs);
    r.setOffset(os);
    r.setMinTime(minTime);
    return Result.ok(r);
}
```    

本流程需要注意的是每一次偏移量的计算，因为存在多种情况：

1. 正常情况，没有出现重复的 score，那么就走正常的流程，下一次的偏移量一定就是 1

```java
else{ // 进行重置 os
    // 不一致，跟新最小 score（时间戳）
    minTime = time;
    // 防止最小值前面有非最小的相同 score，所以出现不一致时重置偏移量为 1
    os = 1;
}
```

2. 出现几个重复的最小值，那么就需要计算新的偏移量，偏移量设置为重复值的个数

```java
// 有相同的最小值，累加偏移量
if(time == minTime){
    // 一致
    os++;
}
```

3. 当本次查询的最大与最小都是相同的值时，并且上一次的重复的最小值不止一个，那么就需要本次计算的偏移量加上上次的偏移量作为下一次的偏移量

```java
// 当上一次的偏移量大于 1 时，证明上一次的查询中有不止一个相同的最小值，
// 而同时本次查询的最大与最小 score 都相同时，证明这是好几个相同大小的值放在一块了
// 所以再下一次的查找仍然是从这几个连续的相同的值开始
// 所以设置的偏移量应该是本次使用的偏移量加上本次计算出来的最小相同值的个数
if (offset > 1 && minTime == max) {
    os = os + offset;
}
```

****
### 10. 附近商铺

#### 10.1 GEO 基本用法

GEO 就是 Geolocation 的简写形式，代表地理坐标。Redis 在 3.2 版本中加入了对 GEO 的支持，允许存储地理坐标信息，可以根据经纬度来检索数据。常见的命令有：

* GEOADD：添加一个地理空间信息，包含：经度（longitude）、纬度（latitude）、值（member）

```redis
# 添加北京、上海、广州的坐标
GEOADD cities:china 116.4074 39.9042 beijing 121.4737 31.2304 shanghai 113.2644 23.1261 guangzhou
```

* GEODIST：计算指定的两个点之间的距离并返回，默认单位是米，也可手动指定

```redis
# 计算北京到上海的距离（单位：千米）
GEODIST cities:china beijing shanghai km
# "1067.6112" km
```

* GEOHASH：将指定member的坐标转为hash字符串形式并返回

```redis
# 获取北京的GeoHash值
GEOHASH cities:china beijing
# "wx4g0bm6c40"
```

* GEOPOS：返回指定member的坐标，以二位数组的形式返回：[longitude, latitude]

```redis
# 获取上海的坐标
GEOPOS cities:china shanghai
# 1) 1) "121.47369772195816"
#   2) "31.23039952570101"
```

* GEORADIUS：指定圆心、半径，找到该圆内包含的所有member，并按照与圆心之间的距离排序后返回。6.以后已废弃

```redis
# 查询北京周边100千米内的城市，按距离升序排列
GEORADIUS cities:china 116.4074 39.9042 100 km WITHCOORD WITHDIST ASC
# 只能查到本身（没添加周边城市）
```

* GEOSEARCH：在指定范围内搜索member，并按照与指定点之间的距离排序后返回。范围可以是圆形或矩形。6.2.新功能

```redis
# 圆形查询：上海周边200千米内的城市（默认排序是从距离近到远，也可也添加 SORT ASC|DESC 参数显式指定）
GEOSEARCH cities:china BYRADIUS 121.4737 31.2304 200 KM WITHDIST

# 127.0.0.1:6379> GEOSEARCH cities:china FROMMEMBER beijing BYRADIUS 200 km WITHDIST
# 1) 1) "beijing" 匹配到的成员名
# 2) "0.0000" 距离

# 矩形查询：查询北京所在矩形区域内的城市
GEOSEARCH cities:china FROMMEMBER beijing BYBOX 300 300 km WITHDIST
```

* GEOSEARCHSTORE：与 GEOSEARCH 功能一致，不过可以把结果存储到一个指定的 key， 6.2 新功能

```redis
# 查询广州周边150千米内的城市并存储结果
GEOSEARCHSTORE nearby:guangzhou cities:china FROMLONLAT 113.2644 23.1261 BYRADIUS 150 km
```

****
#### 10.2 添加并查询附近商户

当点击美食之后，会出现一系列的商家，商家中可以按照多种排序方式，如果此时关注的是距离，那就需要使用到 GEO 向后台传入当前 app 收集的地址(此处是写死的) ，
以当前坐标作为圆心，同时绑定相同的店家类型 type 以及分页信息，把这几个条件传入后台，后台查询出对应的数据再返回。

所以需要将数据库表中的数据导入到 redis 中的 GEO，GEO 在 redis 中就一个 member 和一个经纬度，把 x 和 y 轴（经纬度）传入到 redis 的经纬度位置，
但是不能把所有的数据都放入到 member 中去，毕竟作为 redis 是一个内存级数据库，如果存海量数据，redis 容易宕机，所以在这个地方存储店铺 id 即可。
所以在 GEO 中，key 是店铺类型 id，value 是 店铺 id，score 是经纬度。

```java
void loadShopData() {
    // 1. 查询店铺信息
    List<Shop> list = shopService.list();
    // 2. 把店铺分组，按照 typeId 分组，typeId 一致的放到一个集合
    Map<Long, List<Shop>> map = list.stream().collect(Collectors.groupingBy(Shop::getTypeId));
    // 3. 分批写入 Redis
    for (Map.Entry<Long, List<Shop>> entry : map.entrySet()) {
        // 获取类型 id
        Long typeId = entry.getKey();
        String key = SHOP_GEO_KEY + typeId;
        // 获取同类型的店铺的集合
        List<Shop> value = entry.getValue();
        // GeoLocation<T> 是 Redis 提供的地理位置对象，用来封装经纬度和业务标识（member，这里是店铺 id）
        List<RedisGeoCommands.GeoLocation<String>> locations = new ArrayList<>(value.size());
        // 写入 redis: GEOADD key 经度 纬度 member
        for (Shop shop : value) {
            // stringRedisTemplate.opsForGeo().add(key, new Point(shop.getX(), shop.getY()), shop.getId().toString());
            locations.add(new RedisGeoCommands.GeoLocation<>(
                    shop.getId().toString(),
                    new Point(shop.getX(), shop.getY())
            ));
        }
        // 写入到 redis 的 GEO 中，店铺类型 id 作为 key
        stringRedisTemplate.opsForGeo().add(key, locations);
    }
}
```

成功把数据库中的店铺信息存储到 redis 中后，则可以从 redis 中获取了，需要注意的是 GEO 是基于 Redis 的有序集合（Sorted Set）实现，本身并不直接提供传统数据库那样的 “页码 + 每页条数” 的分页能力，
但可以通过 LIMIT 来模拟分页效果，不过因为店铺的更新不会像发邮件那样频繁，所以可以不用滚动式分页。

```java
public Result queryShopByType(Integer typeId, Integer current, Double x, Double y) {
    // 1. 判断是否需要根据坐标查询
    if (x == null || y == null) {
        // 不需要坐标查询，按数据库查询
        Page<Shop> page = query()
                .eq("type_id", typeId)
                .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));
        // 返回数据
        return Result.ok(page.getRecords());
    }
    // 2. 计算分页参数
    // 获取前一页的最后一个数据的下标
    int from = (current - 1) * SystemConstants.DEFAULT_PAGE_SIZE;
    // 根据当前页数乘默认每页条数获取当前页的最后一条数据下标
    int end = current * SystemConstants.DEFAULT_PAGE_SIZE;
    // 3. 查询redis、按照距离排序、分页。结果：shopId、distance
    String key = RedisConstants.SHOP_GEO_KEY + typeId;
    // GEOSEARCH key BYLONLAT x y BYRADIUS 10 WITHDISTANCE
    GeoResults<RedisGeoCommands.GeoLocation<String>> results = stringRedisTemplate.opsForGeo()
            .search(
                    key,
                    // 传入经纬度
                    GeoReference.fromCoordinate(x, y),
                    // 设置距离
                    new Distance(5000),
                    RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs()
                            .includeDistance() // 返回距离信息
                            .limit(end) // 最多返回 end 条信息
            );
    // 4. 解析出 GEO id
    if (results == null) {
        return Result.ok(Collections.emptyList());
    }
    List<GeoResult<RedisGeoCommands.GeoLocation<String>>> list = results.getContent();
    if (list.size() <= from) {
        // 如果获取的数据比上一页的最后一条数据的下标还小，证明没有下一页了，结束
        return Result.ok(Collections.emptyList());
    }
    // 处理分页数据，截取 from ~ end的部分
    List<Long> ids = new ArrayList<>(list.size());
    Map<String, Distance> distanceMap = new HashMap<>(list.size());
    list.stream().skip(from) // 跳过前 from 条，拿到当前页数据
            .forEach(result -> { // 遍历分页中的数据
        // 获取 GEO 的 member，这里存的是店铺 ID
        String shopIdStr = result.getContent().getName();
        ids.add(Long.valueOf(shopIdStr));
        // 临时存储店铺距离，后续填充到返回数据
        Distance distance = result.getDistance();
        distanceMap.put(shopIdStr, distance);
    });
    // 5. 根据 id 查询数据库的 Shop 详细信息
    String idStr = StrUtil.join(",", ids);
    // 与上面内容一样，需要手动拼接 order by 设置查询顺序，保持与 GEO 的存储顺序一致
    List<Shop> shops = query().in("id", ids).last("ORDER BY FIELD(id," + idStr + ")").list();
    for (Shop shop : shops) {
        shop.setDistance(distanceMap.get(shop.getId().toString()).getValue());
    }
    return Result.ok(shops);
}
```

**** 
### 11. 用户签到

#### 11.1 BitMap

针对签到功能，其实可以依靠数据库来完成，用户每签到一次就向签到表中插入一条数据，但是这种方式太浪费存储空间了，如果使用的人数一多，就会导致数据库中的数据超多，查询统计困难，效率低下。
所以引出了一种新的机制，模拟现实中的签到卡片，一张卡片上记录一个月的签到情况，签到了就在对应的那天打上勾就行，而 Redis 中的 BitMap 就能实现类似的功能，把每月的每一天用一个 bit 标示，
用 0 和 1 标记签到情况，所以一个月最多使用 31 位 bit，redis 中是利用 String 类型结构实现 BitMap 的，本质为一串二进制序列，
所以最大上限为 512 M（Redis 单个 String 最大 512 MB），对应 2^32 个 bit 位。

常用命令：

* SETBIT：向指定位置（offset）存入一个 0 或 1，key 不存在时则自动创建

```redis
SETBIT key offset value

SETBIT sign:1001:2025-07 0 1   # 设置第0位为1，表示7月1号已签到
SETBIT sign:1001:2025-07 1 1   # 设置第1位为1，表示7月2号已签到
``` 

* GETBIT ：获取指定位置（offset）的 bit 值，返回 1 表示该位置 bit 为 1；返回 0 表示该位置 bit 为 0（不存在的 key 默认返回 0）

```redis
GETBIT key offset

GETBIT sign:1001:2025-07 0   # 返回 1
GETBIT sign:1001:2025-07 5   # 返回 0
```

* BITCOUNT ：统计 BitMap 中值为 1 的 bit 位的数量，start、end：表示字节范围，单位是字节（byte），不是 bit 位

```redis
BITCOUNT key [start] [end]

BITCOUNT sign:1001:2025-07  # 统计整个月的签到天数
# 范围统计，统计前 1 个字节（即前 8 天）的签到
BITCOUNT sign:1001:2025-07 0 0
```

* BITFIELD ：操作（查询、修改、自增）BitMap 中 bit 数组中的指定位置（offset）的值（把一段连续的 bit 位组合成一个整数）

常见子命令：GET（获取指定位置的整数）、SET（设置指定位置的整数）、INCRBY（指定位置整数自增指定值）
数据类型格式：u{bits} 表示无符号整数（如 u8 表示8位）；i{bits} 表示有符号整数

```redis
BITFIELD key [subcommand] [arguments]...

# 设置第0位开始的8位无符号整数值为100
BITFIELD mybitmap SET u8 0 100
# 获取第0位开始的8位无符号整数
BITFIELD mybitmap GET u8 0
# 指定位置自增 1
BITFIELD mybitmap INCRBY u8 0 1
```

* BITFIELD_RO ：获取 BitMap 中 bit 数组，并以十进制形式返回，与 BITFIELD 类似，但不能修改数据

```redis
BITFIELD_RO mybitmap GET u8 0  # 获取第0位起8位的值
```

* BITOP ：将多个 BitMap 的结果做位运算（与 AND 、或 OR、异或 XOR）

```redis
BITOP operation destkey key [key...]

# 统计7月1日，用户A、B是否都签到（AND）
BITOP AND both:2025-07-01 sign:1001:2025-07 sign:1002:2025-07

# 统计7月1日，至少有一人签到（OR）
BITOP OR one:2025-07-01 sign:1001:2025-07 sign:1002:2025-07
```

* BITPOS ：查找 bit 数组中指定范围内第一个 0 或 1 出现的位置

```redis
BITPOS key bit [start] [end]

# 查找第一个未签到（0）的位置
BITPOS sign:1001:2025-07 0

# 查找第一个已签到（1）的位置
BITPOS sign:1001:2025-07 1
```

****
#### 11.2 实现签到

可以把年和月（通过代码获取当前时间）作为 bitMap 的 key 然后保存到一个 bitMap 中，每次签到就到对应的位上把数字从 0 变成 1，只要对应是 1，就表明说明这一天已经签到了，反之则没有签到。

```java
public Result sign() {
    // 1. 获取当前登录用户
    Long userId = UserHolder.getUser().getId();
    // 2. 获取日期
    LocalDateTime now = LocalDateTime.now();
    // 3. 给 key 的后缀拼接上现在的年月
    String keySuffix = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
    String key = RedisConstants.USER_SIGN_KEY + userId + keySuffix;
    // 4. 获取今天是本月的第几天
    int dayOfMonth = now.getDayOfMonth();
    // 5. 写入 Redis: SETBIT key offset 1，true 代表 1
    stringRedisTemplate.opsForValue().setBit(key, dayOfMonth - 1, true);
    return Result.ok();
}
```

****
#### 11.3 统计当前连续签到天数

从最后一次签到开始向前统计，直到遇到第一次未签到为止，计算总的签到次数，就是当前连续签到的天数。因为获取的内容是 0 1 组合的 bit 位，所以可以通过由后向前的方式，
对每个 bit 位进行与 1 的与运算，只有遇到 1 才返回 1，就可以让计数器 +1，遇到 0 就结束，统计当前计数器。

```java
public Result signCount() {
    ...
    // 5. 获取本月截止今天为止的所有的签到记录，返回的是一个十进制的数字 BITFIELD sign:userId:202507 GET u2 0
    List<Long> result = stringRedisTemplate.opsForValue().bitField(
            key,
            BitFieldSubCommands.create()
                    .get(BitFieldSubCommands.BitFieldType.unsigned(dayOfMonth)).valueAt(0)
    );
    if (result == null || result.isEmpty()) {
        // 没有任何签到结果
        return Result.ok(0);
    }
    // 因为只返回了一条数据，所以获取第一个即可
    Long num = result.getFirst();
    if (num == null || num == 0) {
        return Result.ok(0);
    }
    // 6.循环遍历
    int count = 0;
    while (true) {
        // 让这个数字与 1 做与运算，得到数字的最后一个bit位
        // 判断这个bit位是否为0
        if ((num & 1) == 0) {
            // 如果为0，说明未签到，直接结束
            break;
        }else {
            // 如果不为0，说明已签到，计数器+1
            count++;
        }
        // 把数字右移一位，抛弃最后一个 bit 位，继续下一个 bit 位
        num >>>= 1;
    }
    return Result.ok(count);
}
```

****
### 12. UV 统计

#### 12.1 HyperLogLog

* UV：全称 Unique Visitor，也叫独立访客量，是指通过互联网访问、浏览这个网页的自然人，当 1 天内同一个用户多次访问该网站，只记录 1 次。
* PV：全称 Page View，也叫页面访问量或点击量，用户每访问网站的一个页面就记录 1 次 PV，用户多次打开页面，则记录多次 PV，通常用来衡量网站的流量。

传统的 UV 统计在服务端做会比较麻烦，因为要判断该用户是否已经统计过了，需要将统计过的用户信息保存（如用户ID、IP、Cookie）。
但是如果每个访问的用户都保存到 Redis 中，数据量会非常恐怖，为了解决这种难题，HyperLogLog 由此而生。

Hyperloglog(HLL) 是从 Loglog 算法派生的概率算法，用于确定非常大的集合的基数，而不需要存储其所有值，是一种概率性数据结构。
Redis 中的 HLL 是基于 string 结构实现的，单个 HLL 的内存永远小于16 kb，内存占用极低，但作为代价，其测量结果是概率性的，有小于0.81％的误差。
不过对于 UV 统计来说完全可以忽略。

常用命令：

- 添加元素，新增元素到 HyperLogLog，重复元素不会影响结果（天生适合做统计次数）

```redis
PFADD key element [element ...]

PFADD uv_2025-07-02 user1
PFADD uv_2025-07-02 user2
PFADD uv_2025-07-02 user3
PFADD uv_2025-07-02 user1  # 重复添加，无影响
```

- 统计基数（UV 数量）

```redis
PFCOUNT key [key ...]

# 返回 3，表示有 3 个不同用户
PFCOUNT uv_2025-07-02
```

- 合并多个 HyperLogLog，统计总的不同元素数，常用于跨天、跨设备、跨系统 UV 汇总

```redis
PFMERGE destkey sourcekey [sourcekey ...]

PFMERGE uv_total uv_2025-07-01 uv_2025-07-02 uv_2025-07-03
PFCOUNT uv_total
```

测试：

使用前：used_memory:1304896 字节，使用后：1329600 字节，最终返回的数据虽然不是 1000，但是只占用了 24 kb，内存占用较小。

```java
@Test
void testHyperLoglog() {
    String[] users = new String[1000];
    int index = 0;
    for (int i = 0; i < 1000000; i++) {
        users[index++] = "user_" + i;
        // 每一千条发送一次
        if (i % 1000 == 0) {
            index = 0;
            stringRedisTemplate.opsForHyperLogLog().add("hll:", Arrays.toString(users));
        }
    }
    // 统计数量
    Long hllSize = stringRedisTemplate.opsForHyperLogLog().size("hll:");
    System.out.println("hllSize = " + hllSize); // 990
}
```

****
## 3. 分布式缓存

单机的 Redis 存在四大问题：

1、容量瓶颈

Redis 是内存级数据库，数据全部存放在内存中，单机内存受限，数据量增长到一定规模就无法单机承载，这就容易出现内存溢出，造成数据丢失。

2、并发瓶颈

Redis 单线程处理命令（核心线程单独负责网络读写和命令执行），尽管单线程高效，但高并发下，命令队列容易堆积，这就会导致响应时间变长，出现性能瓶颈。

3、单点故障风险

单机 Redis 一旦宕机，缓存数据就会全部丢失（如果未持久化），依赖于 Redis 的系统功能也将无法使用。

4、数据持久化风险

单机 Redis 支持 RDB、AOF 两种持久化，但 RDB 持久化存在快照间隔，数据可能丢失；AOF 追加日志若未设置 always 策略，也可能丢数据

### 1. Redis 持久化

#### 1.1 RDB 持久化

RDB 全称 Redis Database Backup file（Redis数据备份文件），也被叫做 Redis 数据快照，简单来说就是将某一时刻 Redis 内存中的所有数据，生成二进制文件（.rdb 文件）保存在磁盘。
当 Redis 宕机重启后，可以从磁盘读取快照文件并恢复数据。快照文件称为 RDB 文件，默认是保存在当前运行目录。

RDB持久化在以下情况会执行：

- 执行 save 命令，Redis 立即开始生成 dump.rdb 文件

会同步阻塞式生成 RDB 快照，并且由 Redis 主线程直接执行，但期间无法响应任何请求。

- 执行 bgsave 命令

异步后台生成 RDB 快照，Redis 会使用 fork() 创建子进程，父进程继续正常处理请求，而子进程负责内存数据复制，生成 RDB 文件。

- Redis 停机时

使用 SHUTDOWN SAVE 命令或直接关闭 Redis 服务时，如果 Redis 开启了持久化（RDB 或 AOF），在停机前就会生成 RDB 快照。

```redis
SHUTDOWN SAVE   # 停机前强制保存 RDB
SHUTDOWN NOSAVE # 停机不保存数据
```

- 触发 RDB 条件时

Redis 内部会自动检测，达到指定时间+写操作次数条件时会自动执行 BGSAVE 命令，也可以通过 CONFIG SET save "" 关闭自动 RDB

例如：

```redis
save 900 1      # 900秒内至少1次写操作，触发RDB
save 300 10     # 300秒内至少10次写操作，触发RDB
save 60 10000   # 60秒内至少10000次写操作，触发RDB
```

```redis
# 是否压缩 ,建议不开启，压缩也会消耗cpu，磁盘的话不值钱
rdbcompression yes

# RDB文件名称
dbfilename dump.rdb  

# 文件保存的路径目录
dir ./ 
```

****
#### 1.2 RDB 的 BGSAVE 执行原理

BGSAVE 命令执行过程：

1. 父进程创建子进程：

当客户端发送 BGSAVE 命令给 Redis 服务器时，Redis 的父进程会调用操作系统的 fork 函数创建一个子进程，然后操作系统会为子进程分配一套新的页表（虚拟内存映射表），
父子进程的页表指向同一块物理内存，此时物理内存没有立即整体复制，只有在父进程有写操作时，才触发 Copy-On-Write，针对被修改的内存页单独复制

2. 子进程进行数据快照：

子进程会先拷贝父进程的内存页表（COW共享），然后负责将内存中的数据写入磁盘，生成 RDB 文件。在这个过程中，子进程会按照 Redis 的 RDB 文件格式，
依次将 Redis 数据库中的键值对、过期时间等信息序列化后写入文件。而父进程在子进程执行数据写入操作期间，仍然可以继续处理客户端的请求，
不过它不会对共享内存数据进行修改（除了一些特殊情况，如过期键的删除等），因为系统会触发 COW 为修改的内存页创建物理副本，让父线程对副本进行修改，
子进程不受影响，仍基于 fork 时刻的内存视图生成 RDB，当再次调用 BGSAVE 时才会把刚刚修改的副本作为新的内存页写入 RDB。

3. 完成持久化并清理：

当子进程成功将所有数据写入磁盘，生成完整的 RDB 文件后，会向父进程发送一个信号（通常是 SIGCHLD 信号），告知父进程持久化操作已经完成。
父进程收到信号后，会进行一些必要的清理工作，比如更新相关的持久化状态信息等。

BGSAVE 与 Copy-On-Write 的优缺点：

优点：

- 快速恢复：RDB 文件是数据的快照，在 Redis 重启时，通过加载 RDB 文件可以快速恢复数据（RDB 文件结构是二进制压缩格式，加载效率极高），相比其他持久化方式（如 AOF），恢复速度更快。
- 节省内存和 IO 开销：Copy-On-Write 技术减少了数据复制的内存占用，并且在子进程进行持久化期间，父进程可以继续处理请求，提高了系统的并发处理能力，同时也降低了磁盘 IO 的压力。
缺点：

- 数据丢失风险：如果两次 RDB 持久化之间的数据变化没有其他持久化方式补充，那么在 Redis 意外宕机时数据会丢失。例如，在执行BGSAVE后，10 分钟内发生宕机，这 10 分钟的数据修改就会丢失。
- 对系统性能有一定影响：执行 BGSAVE 时，fork 子进程会消耗一定的系统资源，在内存数据量非常大时，fork 操作可能会导致短时间内系统性能下降。同时，写时复制过程中，如果频繁有写操作（多次使用 COW），也会增加内存管理和数据复制的开销。

****
#### 1.3 AOF 持久化

AOF（Append Only File）是通过日志追加的方式，将 Redis 每次执行的写命令记录到磁盘文件中（.aof 文件），当 Redis 宕机重启时，会按照 AOF 文件中记录的命令顺序重新执行一遍，从而将数据恢复到最新状态。
Redis 接收到客户端的写命令（如 SET、DEL、HSET 等）后，会先将该命令追加到 AOF 文件的末尾，文件追加结束后，根据配置决定是否立即更新到磁盘，该操作由主进程完成。

例如：

```redis
SET name "Redis"
HSET user age 18

*3
$3
SET
$4
name
$5
Redis
*4
$4
HSET
$4
user
$3
age
$2
18
```

- always：每个写命令执行完，立马将 AOF 缓冲区中的内容写入并同步到 AOF 文件。这种策略数据安全性最高，因为一旦发生宕机，最多只会丢失一个命令的数据，但频繁的磁盘 IO 操作会对 Redis 的性能产生较大影响。
- everysec（默认）：每秒将 AOF 缓冲区中的内容写入并同步到 AOF 文件。这种策略在数据安全性和性能之间做了一个平衡，即使发生宕机也最多只丢失 1 秒内的数据，但由于每秒只进行一次磁盘同步操作，对性能的影响相对较小。
- no：由操作系统决定何时将 AOF 缓冲区中的内容写入和同步到磁盘。这种策略的性能最好，因为减少了主动的磁盘同步操作，但数据安全性最低，一旦系统崩溃，可能会造成数据丢失。

```redis
# 是否开启AOF功能，默认是no
appendonly yes
# AOF文件的名称
appendfilename "appendonly.aof"

# 表示每执行一次写命令，立即记录到AOF文件
appendfsync always 
# 写命令执行完先放入AOF缓冲区，然后表示每隔1秒将缓冲区数据写到AOF文件，是默认方案
appendfsync everysec 
# 写命令执行完先放入AOF缓冲区，由操作系统决定何时将缓冲区内容写回磁盘
appendfsync no
```


****
#### 1.4 AOF 重写（rewrite）机制

随着 Redis 不断执行写命令，AOF 文件会越来越大，这不仅会占用大量磁盘空间，还会导致 Redis 在重启时恢复数据的时间变长，而 AOF 重写会根据内存中的数据，
重新生成一份精简的 AOF 文件。它会去除掉 AOF 文件中冗余的命令，比如对同一个键多次执行 SET 命令，那么在重写后的 AOF 文件中只会保留最后一次设置的结果，触发方式如下：

- 手动触发：

可以通过执行 BGREWRITEAOF 命令手动触发 AOF 重写，执行该命令时 Redis 会 fork 出一个子进程，由子进程负责根据内存中的数据生成新的 AOF 文件，
而父进程继续处理客户端请求。子进程生成完新的 AOF 文件后会通知父进程，父进程将新的 AOF 文件替换旧的 AOF 文件。

- 自动触发：

可以在 Redis 配置文件中设置 auto-aof-rewrite-percentage 和 auto-aof-rewrite-min-size 两个参数来实现自动触发 AOF 重写。
auto-aof-rewrite-percentage 表示当前 AOF 文件大小相较于上次重写后 AOF 文件大小的增长率，
auto-aof-rewrite-min-size 表示 AOF 文件的最小大小，当 AOF 文件大小超过设置的大小并且增长率超过 auto-aof-rewrite-percentage 时，就会自动触发 AOF 重写。
默认情况下，auto-aof-rewrite-percentage 为 100，auto-aof-rewrite-min-size为 64MB。

```redis
# AOF文件比上次文件 增长超过多少百分比则触发重写
auto-aof-rewrite-percentage 100
# AOF文件体积最小多大以上才触发重写 
auto-aof-rewrite-min-size 64mb 
```

****
#### 1.5 RDB 与 AOF对比

1、持久化原理

RDB：

RDB 是对 Redis 某一时刻的内存数据进行快照，将其以二进制文件（.rdb）的形式保存到磁盘。

AOF：

AOF 则是将 Redis 执行的每一个写命令追加到 AOF 文件中，文件以文本形式存储，当 Redis 重启时，通过重新执行 AOF 文件中的命令来恢复数据。

2、数据安全性

RDB：

因为 RDB 是定期或手动生成快照，两次快照之间如果发生宕机，这段时间内的数据修改将会丢失，所以数据的安全性相对较低。

AOF：

如果采用 always 同步策略，那么几乎可以保证不丢失数据；采用 everysec 策略，最多丢失 1 秒内的数据；采用 no 策略，数据丢失量取决于操作系统的写入时机，可能丢失较多数据，但总体上比 RDB 在默认配置下丢失数据的风险要低。

3、对性能的影响

RDB：

执行 BGSAVE 时，fork 子进程会消耗一定的系统资源（主要是内存和 CPU），并且在数据量较大与 COW 触发频繁时，可能会导致 Redis 短时间内响应变慢。
不过子进程生成 RDB 文件过程中，父进程仍可处理请求，正常情况下对 Redis 的性能影响相对较小。

AOF：

虽然写入 .aof 文件到缓冲区的操作是由主进程执行的，但速度极快几乎不影响，主要还是由处理策略影响：always 策略由于每次写操作都要进行磁盘同步，会严重影响 Redis 的写入性能；everysec 策略每秒同步一次，对性能影响相对较小；no 策略对性能影响最小，但数据安全性也最低。
不过 AOF 重写时也会 fork 子进程，但一般情况下重写操作不会像 RDB 快照那样频繁进行。

4、文件大小

RDB：

RDB 文件是二进制格式，并且可以开启压缩机制，所以文件体积通常较小。

AOF：

AOF 文件记录的是命令，以文本形式存储，即使经过 AOF 重写去除冗余命令，文件体积通常也会比 RDB 文件大，
因为 AOF 文件会记录对同一键的多次修改操作，而 RDB 文件在生成快照时只会保留最新的数据状态。

****
### 2. Redis 主从

#### 2.1 Redis 主从集群的搭建

这里我会在同一台虚拟机中开启3个 redis 实例，模拟主从集群，信息如下：

| IP              | PORT | 角色   |
|-----------------|------|--------|
| 172.23.14.3     | 7001 | master |
| 172.23.14.3     | 7002 | slave  |
| 172.23.14.3     | 7003 | slave  |

要在同一台虚拟机开启3个实例，必须准备三份不同的配置文件和目录，配置文件所在目录也就是工作目录：

1、创建目录

创建三个文件夹，名字分别叫 7001、7002、7003

```shell
# 进入/tmp目录
cd /tmp
# 创建目录
mkdir 7001 7002 7003
```

2、拷贝配置文件到每个实例目录

```shell
# 因为此时还没给普通用户设置权限，必须通过 sudo 命令临时获取 root 的权限，
cell@LAPTOP-SVEUFK1D:~$ sudo cp /etc/redis/redis.conf /tmp/7001/
cell@LAPTOP-SVEUFK1D:~$ sudo cp /etc/redis/redis.conf /tmp/7002/
cell@LAPTOP-SVEUFK1D:~$ sudo cp /etc/redis/redis.conf /tmp/7003/
```

```shell
# 给指定目录赋予 root 权限
sudo chown -R $USER:$USER /tmp/7001
sudo chown -R $USER:$USER /tmp/7002
sudo chown -R $USER:$USER /tmp/7003
```

3、修改每个实例的端口、工作目录

```shell
sed -i -e 's/port 6379/port 7001/g' -e 's#^dir .*#dir /tmp/7001#g' /tmp/7001/redis.conf
sed -i -e 's/port 6379/port 7002/g' -e 's#^dir .*#dir /tmp/7002#g' /tmp/7002/redis.conf
sed -i -e 's/port 6379/port 7003/g' -e 's#^dir .*#dir /tmp/7003#g' /tmp/7003/redis.conf
```

4、修改每个实例的声明 IP

```shell
sudo sed -i '1a replica-announce-ip 172.23.14.3' /tmp/7001/redis.conf
sudo sed -i '1a replica-announce-ip 172.23.14.3' /tmp/7002/redis.conf
sudo sed -i '1a replica-announce-ip 172.23.14.3' /tmp/7003/redis.conf
```

5、启动

为了方便查看日志，打开 3 个 ssh 窗口，分别启动 3 个 redis 实例，启动命令：

```shell
redis-server /tmp/7001/redis.conf
redis-server /tmp/7002/redis.conf
redis-server /tmp/7003/redis.conf
```

6、开启主从关系

现在三个实例还没有任何关系，要配置主从可以使用 replicaof 或者 slaveof（5.0以前）命令。

有临时和永久两种模式：

- 修改配置文件（永久生效）
    - 在redis.conf中添加一行配置：`slaveof <masterip> <masterport>`
- 使用 redis-cli 客户端连接到 redis 服务，执行 slaveof 命令（重启后失效）

```shell
redis-cli -p 7002
auth 123 # 输入密码，不然使用不了命令
# 因为 redis 设置了密码，所以连接 master 时需要设置密码，否则主从握手过程中认证失败，master_link_status 会一直是 down，无法同步数据
CONFIG SET masterauth 123 
# 设置连接，master 是 7001 端口，slave 是 7002 端口，其余同理
slaveof 172.23.14.3 7001
# 查看状态
info replication
```

```shell
127.0.0.1:7001> info replication
# Replication
role:master
connected_slaves:2
slave0:ip=172.23.14.3,port=7002,state=online,offset=224,lag=3
slave1:ip=172.23.14.3,port=7003,state=online,offset=224,lag=0
master_failover_state:no-failover
master_replid:ed6cddc9b8cfbeb0333b4b3a69a3d4cc23a989d4
master_replid2:0000000000000000000000000000000000000000
master_repl_offset:224
second_repl_offset:-1
repl_backlog_active:1
repl_backlog_size:1048576
repl_backlog_first_byte_offset:1
repl_backlog_histlen:224
```

测试：

- 利用redis-cli连接7001，执行`set num 123`
- 利用redis-cli连接7002，执行`get num`，再执行`set num 666`
- 利用redis-cli连接7003，执行get num`，再执行`set num 888`

```shell
127.0.0.1:7003> set num 666
(error) READONLY You can't write against a read only replica.
```

可以发现，只有在7001这个master节点上可以执行写操作，7002 和 7003 这两个 slave 节点只能执行读操作。

****
#### 2.2 主从数据同步原理

##### 1. 全量同步

Redis 主从复制过程中，全量同步是当从节点第一次连接主节点，或断开连接后与主节点复制偏移量差距过大无法进行增量复制时，必须做的一次完整数据同步过程。

同步过程：

1、建立连接与信息交互

从节点开始尝试与主节点建立网络连接，一旦连接成功，从节点就可以向主节点发送同步请求，主节点接收到从节点的数据同步请求后，会检查该从节点是否是第一次与自己进行同步。
判断依据通常是从节点是否保存有与主节点相关的同步状态信息等。如果判断出这是从节点第一次与主节点进行同步，主节点会向从节点返回自身的数据版本信息，
比如当前数据的快照版本号等。这一步是为了让从节点记录下主节点当前的数据状态。从节点接收到主节点返回的数据版本信息后，会将其保存下来，用于后续判断数据的一致性和完整性。

2、生成并传输 RDB 文件

主节点确认需要进行全量同步后，会执行 bgsave 命令，fork 出一个子进程来生成 RDB 文件，这个过程中，主节点会对当前内存中的数据进行快照并写入 RDB 文件。
在主节点执行 bgsave 生成 RDB 文件的过程中，主节点会把这段时间内接收到的所有写命令记录到 repl_baklog（复制积压缓冲区）中。
这是因为在生成 RDB 文件期间，主节点的数据可能会发生变化，这些变化需要后续同步到从节点。从节点接收到主节点发送的 RDB 文件后，
会先清空本地当前存储的数据（如果有的话），然后加载 RDB 文件。

3、同步 RDB 生成期间的写命令

主节点发送 RDB 文件后会把 repl_baklog 中的写命令发送给从节点，这些命令记录了从开始生成 RDB 文件到 RDB 文件生成完成这段时间内主节点数据的变化。
从节点接收到主节点发送过来的写命令后，会依次执行这些命令，然后从节点可以将自己的数据更新到与主节点当前完全一致的状态，从而完成整个全量同步过程。

主节点通常是通过从节点发送来的 Replication ID（数据集标识） 和 Offset（复制偏移量）识别是否为第一次连接，然后再决定是否需要全量同步：

- **Replication Id**：简称 replid，是数据集的标记，id 一致则说明是同一数据集，每一个主节点都有唯一的 replid，从节点则会继承主节点的 replid
- **offset**：偏移量，随着记录在 repl_baklog 中的数据增多而逐渐增大，从节点完成同步时也会记录当前同步的 offset，如果从节点的 offset 小于主节点的 offset，说明从节点数据落后于主节点，则需要更新。

因为从节点原本也是一个主节点，有自己的 replid 和 offset，当第一次变成从节点与主节点建立连接时，发送的 replid 和 offset 是自己的，
主节点判断发现从节点发送来的 replid 与自己的不一致，说明这是一个全新的从节点，就知道要做全量同步了，
然后主节点会将自己的 replid 和 offset 都发送给这个从节点，从节点则会保存这些信息，以后从节点的 replid 就与主节点一致了。


****
##### 2. 增量同步

全量同步需要先做 RDB，然后将 RDB 文件通过网络传输给从节点，但这样的成本太高了，因此除了第一次做全量同步，其它大多数时候主从节点之间都是做增量同步。
增量同步是指主从复制过程中，只同步主节点与从节点之间数据差异的部分，通常用于短时间内网络波动、从节点短暂掉线、或正常复制延续场景。

增量同步的前提必须依赖复制偏移量（offset）和复制积压缓冲区（backlog）：

- 复制偏移量

主节点和每个从节点都有自己的偏移量（replication offset）来表示同步的数据字节位置，正常情况下主从偏移量保持一致。

- 复制积压缓冲区

主节点分配一块内存区域（repl_backlog_size，默认1MB），它是环形结构，保存最近写命令的数据。

流程：

1、正常主从复制

主节点执行写命令，然后发送给从节点同时写入积压缓冲区，从节点接收并应用写命令，更新本地数据

2、从节点短暂掉线

从节点由于网络波动或宕机，连接中断，但主节点仍保持积压缓冲区，所以从节点断开期间的写操作都保存在缓冲区

3、从节点重连主节点

从节点重新连接后会发送 replid 和 offset，主节点会判断是否满足增量同步条件，即判断 replid 是否一致并且偏移量差距在积压缓冲区范围内

4、主节点发送缺失数据

主节点从缓冲区中找到从节点缺失的部分数据并发送（若从节点长时间掉线，会导致挤压缓冲区的数据堆积导致大小不足，数据被覆盖），从节点接收到数据后进行更新，并将偏移量与主节点同步

积压缓冲区原理：

repl_backlog 是 Redis 实现增量同步的关键组件，本质是一个固定大小的环形字节数组，也就是说当角标到达数组末尾后，会再次从 0 开始读写，这样数组头部的数据就会被覆盖了。
repl_baklog 中会记录 Redis 处理过的命令日志及 offset，包括主节点当前的 offset，和从节点的 offset（从节点定期发送 offset），而主从之间的 offset 的差异就是从节点需要增量拷贝的数据。
如果主节点持续写入数据，那么 offset 就会不断增长，最终导致环形缓冲区开始覆盖旧数据，若从节点的 offset 已被覆盖，主节点就无法提供增量数据，此时触发全量同步。

主从同步优化：

Redis 主从复制默认情况下虽然简单，但高并发、大数据量下存在全量同步代价高、大量从节点直接挂靠主节点，主节点压力大等问题，可以通过以下方法进行优化：

- 在 master 中配置 repl-diskless-sync yes 启用无磁盘复制，避免全量同步时的磁盘 IO
- Redis 单节点上的内存占用不要太大，减少 RDB 导致的过多磁盘 IO
- 适当提高 repl_baklog 的大小，发现从节点宕机时尽快修复故障，尽可能避免全量同步
- 限制一个主节点上的从节点数量，如果实在是太多从节点，则可以采用主-从-从链式结构（Master → Slave1 → Slave2 → Slave3），减少主节点压力

****
### 3. Redis 哨兵

#### 3.1 哨兵原理

Redis 主从架构虽然能实现读写分离和数据冗余，但无法自动故障切换，当主节点宕机时会导致数据无法恢复，需要人为进行操作，恢复数据，而哨兵模式为此提供了解决方案，
当主节点故障时会被哨兵监控到，然后让其中一个从节点成为新的主节点，保证整个系统的数据不会丢失。

主要功能：

- **监控**：Sentinel 会不断检查主从节点是否按预期工作
- **自动故障恢复**：如果主节点故障，Sentinel 会将一个从节点提升为主节点，当故障实例恢复后也以新的主节点为主
- **通知**：Sentinel 充当 Redis 客户端的服务发现来源，当集群发生故障转移时，会将最新信息推送给 Redis 的客户端

集群监控原理：

哨兵监控 Redis 集群的实时健康状态以及时发现主节点和从节点的网络、服务是否异常；判断故障是否真实，防止误判；一旦确认故障，执行主从切换。

1、心跳机制

Sentinel 每隔 1 秒就会向所有主节点和从节点对象发送 PING 命令，如果它们没有在规定时间内响应 PING 返回 PONG，该 Sentinel 就会主观的认为它们发生故障下线。

2、客观下线

因为整个系统的监听不可能只使用一个哨兵，系统会配置多个 Sentinel 一起监听，当多个 Sentinel（一般是一半）都判断某个或某几个主从节点下线时，则标记为客观下线，执行切换操作。

一旦发现主节点故障，Sentinel 需要在从节点中选择一个作为新的主节点，选择依据是这样的：

- 首先会判断从节点与主节点断开时间的长短，如果超过指定值（down-after-milliseconds * 10）则会排除该从节点，一次避免使用较为老旧的节点
- 然后判断从节点的 slave-priority 值，越小则优先级越高，如果是 0 则永不参与选举（即备胎节点）
- 如果 slave-prority 一样，则判断从节点的 offset 值（offset 反映主从节点的数据同步进度），越大说明数据越新，则优先级越高
- 若上述条件均相同（极少出现），则是判断从节点的运行 id 大小，越小优先级越高。

主从节点的切换：

- sentinel 给备选的 slave1 节点发送 slaveof no one 命令，让该节点成为 master
- sentinel 给所有其它 slave 发送 slaveof ip slave1 命令，让这些 slave 成为新 master 的从节点，开始从新的 master 上同步数据。
- 最后，sentinel 将故障节点标记为 slave，当故障节点恢复后会自动成为新的 master 的 slave 节点

****
#### 3.2 搭建哨兵集群

三个 sentinel 实例信息如下：

| 节点 | IP   | PORT  |
|----|------|-------|
| s1 | 172.23.14.3 | 27001 |
| s2 | 172.23.14.3 | 27002 |
| s3 | 172.23.14.3 | 27003 |

要在同一台虚拟机开启 3 个实例，必须准备三份不同的配置文件和目录，配置文件所在目录也就是工作目录，创建三个文件夹，名字分别叫s1、s2、s3：

```shell
# 进入/tmp目录
cd /tmp
# 创建目录
mkdir s1 s2 s3
```

然后在 s1 目录创建一个 sentinel.conf 文件：

```shell
vi s1/sentinel.conf
```

添加下面的内容：

```shell
port 27001
sentinel announce-ip 172.23.14.3
sentinel monitor mymaster 172.23.14.3 7001 2
sentinel down-after-milliseconds mymaster 5000
sentinel failover-timeout mymaster 60000
sentinel auth-pass mymaster 123
dir "/tmp/s1"
```

- `port 27001`：是当前 sentinel 实例的端口
- `sentinel monitor mymaster 172.23.14.3 7001 2`：指定主节点信息
    - `mymaster`：主节点名称，自定义，任意写
    - `172.23.14.3 7001`：主节点的 ip 和端口
    - `2`：选举 master 时的 quorum 值

然后将 s1/sentinel.conf 文件拷贝到 s2、s3 两个目录中（在/tmp目录执行下列命令）：

```shell
cp s1/sentinel.conf s2
cp s1/sentinel.conf s3
```

修改 s2、s3 两个文件夹内的配置文件，将端口分别修改为 27002、27003：

```shell
sed -i -e 's/27001/27002/g' -e 's/s1/s2/g' s2/sentinel.conf
sed -i -e 's/27001/27003/g' -e 's/s1/s3/g' s3/sentinel.conf
```

启动命令：

```shell
 redis-server /tmp/s1/sentinel.conf --sentinel
 redis-server /tmp/s2/sentinel.conf --sentinel
 redis-server /tmp/s3/sentinel.conf --sentinel
```

测试：

开启 7001、7002、7003 端口，并设置好主从关系，7001 为 master，7002 和 7003 为 slave，开启 s1、s2、s3 哨兵，开始监听节点，
当我关闭 7001 master 节点时，哨兵会开始发现 master 故障:

```shell
+failover-state-select-slave master mymaster 172.23.14.3 7001
```

然后判断是否为客观下线：

```shell
# 这里成功判断为客观下线
+odown master mymaster 172.23.14.3 7001 #quorum 2/2
```

然后根据判断选取另一个从节点作为新的主节点：

```shell
+switch-master mymaster 172.23.14.3 7001 172.23.14.3 7003
```

然后哨兵通知其他节点以后从 7002 那里复制数据：

```shell
+slave slave 172.23.14.3:7002 172.23.14.3 7002 @ mymaster 172.23.14.3 7003
```

验证一下：

```shell
cell@LAPTOP-SVEUFK1D:~$ redis-cli -a 123 -p 7003 info replication
# Replication
role:master # 成为主节点
connected_slaves:1 # 有一个新的从节点
```

```shell
cell@LAPTOP-SVEUFK1D:~$ redis-cli -p 27001 info sentinel
# Sentinel
sentinel_masters:1 # 监听一个主节点
master0:name=mymaster,status=ok,address=172.23.14.3:7003,slaves=2,sentinels=3 # 这个主节点是 7003 端口
```

****
#### 3.2 Redis Template

在 Sentinel 集群监管下的 Redis 主从集群，其节点会因为自动故障转移而发生变化，Redis 的客户端必须感知这种变化，及时更新连接信息。
Spring 的 RedisTemplate 底层利用 lettuce 实现了节点的感知和自动切换。

引入 redis 相关依赖：

```pom
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

配置 redis 的 sentinel 相关信息

```yaml
spring:
  data:
    redis:
      password: 123
      sentinel:
        master: mymaster
        nodes:
          - 172.23.14.3:27001
          - 172.23.14.3:27002
          - 172.23.14.3:27003
        password: 123
```

在项目的启动类中，添加一个新的 bean：

```java
@Bean
public LettuceClientConfigurationBuilderCustomizer clientConfigurationBuilderCustomizer(){
    return clientConfigurationBuilder -> clientConfigurationBuilder.readFrom(ReadFrom.REPLICA_PREFERRED);
}
```

这个bean中配置的就是读写策略，包括四种：

- MASTER：从主节点读取
- MASTER_PREFERRED：优先从 master 节点读取，master 不可用才读取 replica
- REPLICA：从 slave（replica）节点读取
- REPLICA _PREFERRED：优先从 slave（replica）节点读取，所有的 slave 都不可用才读取 zmaster

启动时可以看到连接到了 redis 集群，里面又三个节点，

```text
Trying to get a Redis Sentinel connection for one of: [redis://***@172.23.14.3:27001, redis://***@172.23.14.3:27002, redis://***@172.23.14.3:27003]
```

检测到 7001 下线后就会更新节点：

```text
[channel=0xbf690b21, /172.23.0.1:55372 -> /172.23.14.3:27001, epid=0x15, chid=0x1d] channelInactive()
```

```text
RedisMasterReplicaNode [redisURI=redis://***@172.23.14.3:7003, role=REPLICA], 
RedisMasterReplicaNode [redisURI=redis://***@172.23.14.3:7002, role=UPSTREAM], 
RedisMasterReplicaNode [redisURI=redis://***@172.23.14.3:7001, role=REPLICA]
```

当 7001 重启后会收到来自 Sentinel 的变更通知：

```text
Received topology changed signal from Redis Sentinel (+role-change)
```

****
### 4. Redis 分片集群

#### 4.1 搭建分片集群

主从和哨兵可以解决高可用、高并发读的问题，但是依然有两个问题没有解决：海量数据存储问题以及高并发写的问题，不过使用分片集群可以解决这些问题

分片集群特征：

- 集群中有多个 master，每个 master 保存不同数据
- 每个 master 都可以有多个 slave 节点
- master 之间通过 ping 监测彼此健康状态
- 客户端请求可以访问集群任意节点，最终都会被转发到正确节点

搭建流程：

分片集群需要的节点数量较多，这里我搭建一个最小的分片集群，包含 3 个 master 节点，每个 master 包含一个 slave 节点：

|       IP       | PORT |  角色  |
|:--------------:| :--: | :----: |
|  172.23.14.3   | 7001 | master |
|  172.23.14.3   | 7002 | master |
|  172.23.14.3   | 7003 | master |
|  172.23.14.3   | 8001 | slave  |
|  172.23.14.3   | 8002 | slave  |
|  172.23.14.3   | 8003 | slave  |


删除之前的 7001、7002、7003 这几个目录，重新创建出 7001、7002、7003、8001、8002、8003 目录：

```shell
# 进入/tmp目录
cd /tmp
# 删除旧的，避免配置干扰
rm -rf 7001 7002 7003
# 创建目录
mkdir 7001 7002 7003 8001 8002 8003
```

如果之前开启了 7001、7002、7003 端口，最好提前结束一下，不然可能导致后面无法添加新的 redis.conf 文件：

```shell
# 查看当前运行进程
ps -ef | grep redis
```

```text
redis        176       1  0 14:14 ?        00:00:10 /usr/bin/redis-server 0.0.0.0:6379
cell        1321       1 10 14:40 ?        00:03:53 redis-server 0.0.0.0:7001
cell        1493       1  0 14:46 ?        00:00:05 redis-server 0.0.0.0:7002
cell        1502       1  0 14:47 ?        00:00:05 redis-server 0.0.0.0:7003
cell        1574     300  0 15:09 ?        00:00:01 redis-server 0.0.0.0:8001 [cluster]
cell        1576     300  0 15:09 ?        00:00:01 redis-server 0.0.0.0:8002 [cluster]
cell        1588     300  0 15:09 ?        00:00:01 redis-server 0.0.0.0:8003 [cluster]
cell        1629     301  0 15:17 pts/0    00:00:00 grep --color=auto redisv
```

```shell
sudo kill -9 1321
sudo kill -9 1493
sudo kill -9 1502
```

在 /tmp 下准备一个新的 redis.conf 文件，内容如下：

```redis
port 6379
# 开启集群功能
cluster-enabled yes
# 集群的配置文件名称，不需要我们创建，由redis自己维护
cluster-config-file /tmp/6379/nodes.conf
# 节点心跳失败的超时时间
cluster-node-timeout 5000
# 持久化文件存放目录
dir /tmp/6379
# 绑定地址
bind 0.0.0.0
# 让redis后台运行
daemonize yes
# 注册的实例ip
replica-announce-ip 172.23.14.3
# 保护模式
protected-mode no
# 数据库数量
databases 1
# 日志
logfile /tmp/6379/run.log
```

将这个文件拷贝到每个目录下：

```shell
# 在 /tmp 目录下执行拷贝
echo 7001 7002 7003 8001 8002 8003 | xargs -t -n 1 cp redis.conf
```

修改每个目录下的 redis.conf，将其中的 6379 修改为与所在目录一致：

```shell
# 修改配置文件
printf '%s\n' 7001 7002 7003 8001 8002 8003 | xargs -I{} -t sed -i 's/6379/{}/g' {}/redis.conf
```

因为已经配置了后台启动模式，所以可以直接启动服务：

```shell
# 一键启动所有服务
printf '%s\n' 7001 7002 7003 8001 8002 8003 | xargs -I{} -t redis-server {}/redis.conf
```

查看当前登录状态：

```shell
ps -ef | grep redis

redis        176       1  0 14:14 ?        00:00:11 /usr/bin/redis-server 0.0.0.0:6379
cell        1574     300  0 15:10 ?        00:00:02 redis-server 0.0.0.0:8001 [cluster]
cell        1576     300  0 15:10 ?        00:00:02 redis-server 0.0.0.0:8002 [cluster]
cell        1588     300  0 15:10 ?        00:00:02 redis-server 0.0.0.0:8003 [cluster]
cell        1668     300  0 15:23 ?        00:00:00 redis-server 0.0.0.0:7001 [cluster]
cell        1670     300  0 15:23 ?        00:00:00 redis-server 0.0.0.0:7002 [cluster]
cell        1682     300  0 15:23 ?        00:00:00 redis-server 0.0.0.0:7003 [cluster]
cell        1689     301  0 15:23 pts/0    00:00:00 grep --color=auto redis
```

如果要关闭所有进程，可以执行命令：

```shell
ps -ef | grep redis | awk '{print $2}' | xargs kill
# 或者（推荐这种方式）：
printf '%s\n' 7001 7002 7003 8001 8002 8003 | xargs -I{} -t redis-cli -p {} shutdown
```

虽然服务启动了，但是目前每个服务之间都是独立的，没有任何关联，需要执行命令来创建集群，在 Redis 5.0 之前创建集群比较麻烦，5.0 之后集群管理命令都集成到了 redis-cli 中：

1）Redis 5.0 之前

Redis 5.0 之前集群命令都是用 redis 安装包下的 src/redis-trib.rb 来实现的。因为 redis-trib.rb 是有 ruby 语言编写的所以需要安装 ruby 环境。

```shell
# 安装依赖
yum -y install zlib ruby rubygems
gem install redis
```

然后通过命令来管理集群：

```shell
# 进入redis的src目录
cd /tmp/redis-6.2.4/src
# 创建集群
./redis-trib.rb create --replicas 1 172.23.14.3:7001 172.23.14.3:7002 172.23.14.3:7003 172.23.14.3:8001 172.23.14.3:8002 172.23.14.3:8003
```

2）Redis 5.0 以后

集群管理以及集成到了 redis-cli 中，格式如下：

```shell
redis-cli --cluster create --cluster-replicas 1 172.23.14.3:7001 172.23.14.3:7002 172.23.14.3:7003 172.23.14.3:8001 172.23.14.3:8002 172.23.14.3:8003
```

- `redis-cli --cluster` 或者 `./redis-trib.rb`：代表集群操作命令
- `create`：代表是创建集群
- `--replicas 1` 或者 `--cluster-replicas 1` ：指定集群中每个 master 的副本个数为 1，此时 `节点总数 ÷ (replicas + 1)` 得到的就是 master 的数量。因此节点列表中的前 n 个就是 master，其它节点都是 slave 节点，随机分配到不同 master

启动结果：

```shell
cell@LAPTOP-SVEUFK1D:/tmp$ redis-cli --cluster create --cluster-replicas 1 172.23.14.3:7001 172.23.14.3:7002 172.23.14.3:7003 172.23.14.3:8001 172.23.14.3:8002 172.23.14.3:8003
>>> Performing hash slots allocation on 6 nodes...
Master[0] -> Slots 0 - 5460
Master[1] -> Slots 5461 - 10922
Master[2] -> Slots 10923 - 16383
Adding replica 172.23.14.3:8002 to 172.23.14.3:7001
Adding replica 172.23.14.3:8003 to 172.23.14.3:7002
Adding replica 172.23.14.3:8001 to 172.23.14.3:7003
>>> Trying to optimize slaves allocation for anti-affinity
[WARNING] Some slaves are in the same host as their master
M: ada56c41f9836d8b0a635c2b915d504e792301c9 172.23.14.3:7001
   slots:[0-5460] (5461 slots) master
M: d442b5b8de755ff6f97545d4b90d4e08aaa7b1d7 172.23.14.3:7002
   slots:[5461-10922] (5462 slots) master
M: 44714da596fa56671dbd885b7c8d53581705f8ff 172.23.14.3:7003
   slots:[10923-16383] (5461 slots) master
S: 3b949b48263380b95b34e0dfff4745b60777c97e 172.23.14.3:8001
   replicates ada56c41f9836d8b0a635c2b915d504e792301c9
S: a6a83eb40f681133cbd96e509899dae6b14d1f54 172.23.14.3:8002
   replicates d442b5b8de755ff6f97545d4b90d4e08aaa7b1d7
S: d524be6619a813f82c3d5963616174bf9e604da5 172.23.14.3:8003
   replicates 44714da596fa56671dbd885b7c8d53581705f8ff
Can I set the above configuration? (type 'yes' to accept): yes
>>> Nodes configuration updated
>>> Assign a different config epoch to each node
>>> Sending CLUSTER MEET messages to join the cluster
Waiting for the cluster to join
.
>>> Performing Cluster Check (using node 172.23.14.3:7001)
M: ada56c41f9836d8b0a635c2b915d504e792301c9 172.23.14.3:7001
   slots:[0-5460] (5461 slots) master
   1 additional replica(s)
S: d524be6619a813f82c3d5963616174bf9e604da5 172.23.14.3:8003
   slots: (0 slots) slave
   replicates 44714da596fa56671dbd885b7c8d53581705f8ff
S: a6a83eb40f681133cbd96e509899dae6b14d1f54 172.23.14.3:8002
   slots: (0 slots) slave
   replicates d442b5b8de755ff6f97545d4b90d4e08aaa7b1d7
M: d442b5b8de755ff6f97545d4b90d4e08aaa7b1d7 172.23.14.3:7002
   slots:[5461-10922] (5462 slots) master
   1 additional replica(s)
M: 44714da596fa56671dbd885b7c8d53581705f8ff 172.23.14.3:7003
   slots:[10923-16383] (5461 slots) master
   1 additional replica(s)
S: 3b949b48263380b95b34e0dfff4745b60777c97e 172.23.14.3:8001
   slots: (0 slots) slave
   replicates ada56c41f9836d8b0a635c2b915d504e792301c9
[OK] All nodes agree about slots configuration.
>>> Check for open slots...
>>> Check slots coverage...
[OK] All 16384 slots covered.
```

查看集群状态：

```shell
redis-cli -p 7001 cluster nodes

d524be6619a813f82c3d5963616174bf9e604da5 172.23.14.3:8003@18003 slave 44714da596fa56671dbd885b7c8d53581705f8ff 0 1751527506322 3 connected
ada56c41f9836d8b0a635c2b915d504e792301c9 172.23.14.3:7001@17001 myself,master - 0 0 1 connected 0-5460
a6a83eb40f681133cbd96e509899dae6b14d1f54 172.23.14.3:8002@18002 slave d442b5b8de755ff6f97545d4b90d4e08aaa7b1d7 0 1751527505920 2 connected
d442b5b8de755ff6f97545d4b90d4e08aaa7b1d7 172.23.14.3:7002@17002 master - 0 1751527505920 2 connected 5461-10922
44714da596fa56671dbd885b7c8d53581705f8ff 172.23.14.3:7003@17003 master - 0 1751527505920 3 connected 10923-16383
3b949b48263380b95b34e0dfff4745b60777c97e 172.23.14.3:8001@18001 slave ada56c41f9836d8b0a635c2b915d504e792301c9 0 1751527505920 1 connected
```

测试：

```shell
# 连接
redis-cli -p 7001
# 存储数据
set num 123
# 读取数据
get num # 123
```

再次尝试：

```shell
# 再次存储
set a 1

# 报错
(error) MOVED 15495 172.23.14.3:7003
```

这个报错是 Redis 集群中的重定向（MOVED）错误，它说明当前执行的 SET a 1 这个 key a 应该存储在槽（slot）编号为 15495 的节点上，但是当前连接的 127.0.0.1:7001 不是该槽对应的主节点。
因为 Redis 集群会根据 key 的哈希值，把所有 key 映射到 0~16383 这些槽上，不同的槽由不同的主节点负责管理。如果要解决这种情况，就要使用带 -c 参数的 redis-cli 启用集群模式：

```shell
# 带 -c 后，redis-cli 会自动识别 MOVED 重定向，自动跳转到正确的节点执行命令
redis-cli -c -h 172.23.14.3 -p 7001

172.23.14.3:7001> set a 1
-> Redirected to slot [15495] located at 172.23.14.3:7003
OK

172.23.14.3:7003> get a
"1"
```

****
#### 4.2 散列插槽

Redis Cluster 采用 哈希槽（Hash Slot） 机制来管理数据分布，每个 master 节点负责一部分插槽（共 16384 个），key 通过计算插槽值决定存储在哪个节点上。可以通过 CLUSTER SLOTS 命令查看插槽分布：

```shell
127.0.0.1:7003> CLUSTER SLOTS
1) 1) (integer) 0 # 起始插槽
   2) (integer) 5460 # 结束插槽
   3) 1) "172.23.14.3"
      2) (integer) 7001
      3) "ada56c41f9836d8b0a635c2b915d504e792301c9"
      4) (empty array)
   4) 1) "172.23.14.3"
      2) (integer) 8001 # 从节点端口
      3) "3b949b48263380b95b34e0dfff4745b60777c97e"
      4) (empty array)
2) 1) (integer) 5461
   2) (integer) 10922
   3) 1) "172.23.14.3"
      2) (integer) 7002
      3) "d442b5b8de755ff6f97545d4b90d4e08aaa7b1d7"
      4) (empty array)
   4) 1) "172.23.14.3"
      2) (integer) 8002
      3) "a6a83eb40f681133cbd96e509899dae6b14d1f54"
      4) (empty array)
3) 1) (integer) 10923
   2) (integer) 16383
   3) 1) "172.23.14.3"
      2) (integer) 7003
      3) "44714da596fa56671dbd885b7c8d53581705f8ff"
      4) (empty array)
   4) 1) "172.23.14.3"
      2) (integer) 8003
      3) "d524be6619a813f82c3d5963616174bf9e604da5"
      4) (empty array)
```

Redis 使用 CRC16 算法来计算 key 的哈希值，然后对 16384 取模，得到插槽号：

```shell
slot = CRC16(key) % 16384
```

而 key 的有效部分分为两种：

- 如果 key 包含 {}，则 {} 内的部分作为有效部分计算插槽。

例如：{user:1000}:name，计算的是 user:1000 的插槽，对应的 value 则一起放在这个插槽中。

- 如果 key 不包含 {}，则整个 key 作为有效部分计算插槽。

例如：user:1000:name，计算的是 user:1000:name 的插槽，对应的 value 则一起放在这个插槽中。

所以客户端访问 key 时也就是先对 key 计算它的插槽，然后判断该插槽是否属于当前节点，如果不属于就重定向到对应的插槽所在节点

****
#### 4.3 集群伸缩

Redis 集群伸缩是指集群具有扩容与缩容的特点，可以新增与移除节点，因为 Redis 集群采用槽位迁移机制，可以确保数据有序转移，所以支持在线动态调整节点数量（本质是对每个节点槽位的重新分配）。
例如：向集群中添加一个新的 master 节点，并向其中存储 num = 10

- 启动一个新的 redis 实例，端口为 7004
- 添加 7004 到之前的集群，并作为一个 master 节点
- 给 7004 节点分配插槽

配置操作参看上面搭建分片集群的步骤来创建一个新的 redis 实例，创建好后添加该节点到 redis：

```shell
redis-cli --cluster add-node 172.23.14.3:7004 172.23.14.3:7001
```

- 172.23.14.3:7004 新节点地址
- 172.23.14.3:7001 已在集群的任一节点

```shell
cell@LAPTOP-SVEUFK1D:/tmp$ redis-cli -p 7001 cluster nodes
...
# 新增的节点默认是 master
97c49a23ab32e6f1ca89e9dbbd58c2583e444e04 172.23.14.3:7004@17004 master - 0 1751532051526 8 connected

# node-id：97c49a23ab32e6f1ca89e9dbbd58c2583e444e04，是节点的唯一标识符
```

可以看到 7004 节点的插槽数量为 0，因此没有任何数据可以存储到 7004 上，所以要进行一次重新分配槽位：

```shell
# 与集群建立连接
redis-cli --cluster reshard 172.23.14.3:7001
# 节点信息
...
[OK] All nodes agree about slots configuration.
>>> Check for open slots...
>>> Check slots coverage...
[OK] All 16384 slots covered.
How many slots do you want to move (from 1 to 16384)? 
```

选择需要多少个槽位，然后指定接收这些槽位的节点（用节点 id 指定）：

```shell
How many slots do you want to move (from 1 to 16384)? 4000
What is the receiving node ID? 97c49a23ab32e6f1ca89e9dbbd58c2583e444e04
Please enter all the source node IDs.
  Type 'all' to use all the nodes as source nodes for the hash slots.
  Type 'done' once you entered all the source nodes IDs.
Source node #1: 
```

这里询问，插槽需要从哪里分配过来：

- all：代表全部，也就是三个节点各转移一部分
- 具体的节点 id：目标节点的 id，填写谁的就是从谁的那里拿槽位过来
- done：没有了


```shell
redis-cli -p 7001 cluster nodes
...
97c49a23ab32e6f1ca89e9dbbd58c2583e444e04 172.23.14.3:7004@17004 master - 0 1751532051526 8 connected 0-1332 5461-6794 10923-12255
```

现在后面多了一串数字，这串数字就是该节点负责的槽位（因为是每个槽位都给一部分，所以是这些数字并没有连续）：

- 0-1332（1333 个插槽） 
- 5461-6794（1334 个插槽） 
- 10923-12255（1333 个插槽）

如果是移除一个主节点呢，步骤如下：

```shell
# 登录一个节点，查看当前节点的信息，拿到要删除的节点的 id
redis-cli -c -h 172.23.14.3 -p 7001
cluster nodes
# 97c49a23ab32e6f1ca89e9dbbd58c2583e444e04
```

因为刚刚给这个节点分配了槽位，所以需要把它的槽位重新分配回去，也就是再执行一遍分配节点的流程， 然后选择要分配槽位的节点，让这个节点接收要移除的节点的所有槽位：

```shell
redis-cli --cluster reshard 172.23.14.3:7001

How many slots do you want to move (from 1 to 16384)? 4000
What is the receiving node ID? ada56c41f9836d8b0a635c2b915d504e792301c9 # 用 7001 接收
Please enter all the source node IDs.
  Type 'all' to use all the nodes as source nodes for the hash slots.
  Type 'done' once you entered all the source nodes IDs.
Source node #1: 97c49a23ab32e6f1ca89e9dbbd58c2583e444e04 # 从 7004 拿
Source node #2: done
```

然后移除空的 7004 节点：

```shell
# 获取连接（从随意一个集群节点获取连接）后删除节点
redis-cli --cluster del-node 172.23.14.3:7001 97c49a23ab32e6f1ca89e9dbbd58c2583e444e04
```

如果 7004 有从节点，那么可以选择删除（同 del-node 方法）或者重新分配个其他的主节点：

```shell
redis-cli -p 8004 CLUSTER REPLICATE <new-master-id>
```

****
#### 4.4 故障转移

Redis 集群自动故障转移的前提是每个主节点至少有 1 个从节点，系统会周期性的通过 PING/PONG 探测节点状态。如果我现在手动关闭 7002 端口模拟短路：

```shell
redis-cli -p 7002 shutdown
# 监听的信息就会显示 7002 断开连接
d442b5b8de755ff6f97545d4b90d4e08aaa7b1d7 172.23.14.3:7002@17002 master,fail - 1751534606339 1751534604327 2 disconnected
# 接着选取从节点作为新的主节点，原本 7002 的槽位由 8002 接管
a6a83eb40f681133cbd96e509899dae6b14d1f54 172.23.14.3:8002@18002 master - 0 1751534668000 9 connected 6795-10922
```

当重启 7002 后，7002 则会作为一个主节点的从节点：

```shell
d442b5b8de755ff6f97545d4b90d4e08aaa7b1d7 172.23.14.3:7002@17002 slave a6a83eb40f681133cbd96e509899dae6b14d1f54 0 1751534
```

除了系统的自动选取从节点作为新的主节点，也可以通过手动的方式进行故障转移，把某个从节点主动提升为主节点（要求 master 在线配合）

```shell
cluster failover

d442b5b8de755ff6f97545d4b90d4e08aaa7b1d7 172.23.14.3:7002@17002 master - 0 1751535263579 10 connected 6795-10922
```

流程：

从节点告诉主节点我要进行转换了，请你停止接收客户端的请求，然后主节点就会开始返回当前的数据给从节点（包含 offset），等从节点的 offset 和主节点的 offset 一致时开始进行故障转移，
然后标记从节点为新的主节点，然后作为新主节点开始处理客户端请求。

当然，还有别的模式可以跳过一些步骤：

- cluster failover force：省略了对 offset 的一致性校验
- cluster failover takeover：直接忽略数据一致性、master 状态和其它 master 的意见

****
#### 4.5 RedisTemplate 访问分片集群

RedisTemplate底层同样基于lettuce实现了分片集群的支持，而使用的步骤与哨兵模式基本一致：

1. 引入 redis 的 starter 依赖
2. 配置分片集群地址
3. 配置读写分离

与哨兵模式相比，其中只有分片集群的配置方式略有差异，如下：

```yaml
spring:
  data:
    redis:
      cluster:
        nodes:
          - 172.23.14.3:7001
          - 172.23.14.3:7002
          - 172.23.14.3:7003
          - 172.23.14.3:8001
          - 172.23.14.3:8002
          - 172.23.14.3:8003
```

****
## 4. 多级缓存

在传统单级缓存（如 Redis）中，可能面临缓存击穿/雪崩、网络延迟、资源浪费（热点数据可能被重复计算或传输）等问题，而多级缓存通过分层存储来解决这些问题，
优先从速度最快的缓存层获取数据，并逐层回源，减少数据库压力，当某一层失效时仍能从其他层获取数据。

典型的多级缓存架构（三级缓存模型）:

- 浏览器访问静态资源时，优先读取浏览器本地缓存
- 访问非静态资源（ajax 查询数据）时，访问服务端
- 请求到达 Nginx 后，优先读取 Nginx 本地缓存
- 如果 Nginx 本地缓存未命中，则去直接查询 Redis（不经过Tomcat）
- 如果 Redis 查询未命中，则查询 Tomcat
- 请求进入 Tomcat 后，优先查询 JVM 进程缓存
- 如果 JVM 进程缓存未命中，则查询数据库

```text
┌───────────┐
│  客户端    │
└────┬──────┘
     │ 1. 请求静态资源
     ▼
┌────┴────────┐          ┌───────────────┐
│   CDN缓存    │◀─────────┤ 命中则直接返回  │
└────┬────────┘          └───────────────┘
     │ 未命中，回源
     ▼
┌────┴────────┐          ┌───────────────┐
│  Nginx缓存   │◀─────────┤   缓存+返回    │
└────┬────────┘          └───────────────┘
     │ 2. 请求动态数据
     ▼
┌────┴──────────────┐      ┌───────────────┐
│   应用本地缓存      │◀─────┤ 本地缓存+返回   │
└────┬──────────────┘      └───────────────┘
     │ 3. 查分布式缓存
     ▼
┌────┴────────┐          ┌───────────────┐
│  Redis集群   │◀─────────┤   返回数据     │
└────┬────────┘          └───────────────┘
     │ 4. 未命中则查库
     ▼
┌────┴────────┐          ┌───────────────┐
│   数据库     │◀─────────┤   回填缓存     │
└────┬────────┘          └───────────────┘
     │ 最终响应回传
     ▼
┌────┴────────┐
│  客户端      │
└─────────────┘
```

### 1. JVM 进程缓存

#### 1.1 安装 Docker

下载前需要查看 WSL 配置文件，通过 cat /etc/wsl.conf 确保在引导选项中 systemd 被设置为 true。若尚未设置，需在配置中添加：

```shell
sudo nano /etc/wsl.conf

[boot]
systemd=true

# 然后重启 WSL
wsl --shutdown
# 重新进入
wsl
```

首先是安装 Docker 到 Ubuntu 中：

```shell
# 首先通过 curl 下载脚本
curl -fsSL https://get.docker.com -o get-docker.sh  
```

执行脚本安装 Docker：

```shell
sudo sh get-docker.sh
```

通过 docker version 查看 Docker 引擎的客户端版本、服务器版本，以及引擎、containerd、runc 和 Docker init 的具体版本信息

```shell
docker version

Client:
 Version:           27.5.1
 API version:       1.47
 Go version:        go1.22.2
 Git commit:        27.5.1-0ubuntu3~24.04.2
 Built:             Mon Jun  2 11:51:53 2025
 OS/Arch:           linux/amd64
 Context:           default

Server:
 Engine:
  Version:          27.5.1
  API version:      1.47 (minimum version 1.24)
  Go version:       go1.22.2
  Git commit:       27.5.1-0ubuntu3~24.04.2
  Built:            Mon Jun  2 11:51:53 2025
  OS/Arch:          linux/amd64
  Experimental:     false
 containerd:
  Version:          1.7.27
  GitCommit:
 runc:
  Version:          1.2.5-0ubuntu1~24.04.1
  GitCommit:
 docker-init:
  Version:          0.19.0
  GitCommit:
```

安装好后尝试运行：

```shell
docker run hello-world

# 提示权限被拒
docker: Got permission denied while trying to connect to the Docker daemon socket at unix:///var/run/docker.sock: 
dial unix /var/run/docker.sock: connect: permission denied.
See 'docker run --help'.
```

所以需要将当前用户添加到 Docker 组中，让他具有使用命令的权限：

```shell
sudo usermod -aG docker $USER
# 刷新用户组权限
newgrp docker
```

查看是否添加进组成功：

```shell
id
uid=1000(cell) gid=1000(cell) groups=1000(cell),4(adm),24(cdrom),27(sudo),30(dip),46(plugdev),100(users),109(docker)
```

再次尝试 docker run hello-world：

```shell
cell@LAPTOP-SVEUFK1D:/tmp/mysql$ docker run hello-world
# 成功启动 Docker
Hello from Docker!
...
```

如果返回的是以下内容：

```shell
Unable to find image 'hello-world:latest' locally
docker: Error response from daemon: Get "https://registry-1.docker.io/v2/": net/http: request canceled while waiting for connection (Client.Timeout exceeded while awaiting headers).
See 'docker run --help'.
```

证明这是网络的问题，需要给 Docker 配置网络代理：

```shell
# 代理设置
sudo mkdir -p /etc/docker

# 设置加速器（可用加速器经常变化，网络搜索最新的服务器）
sudo tee /etc/docker/daemon.json <<EOF
{
    "registry-mirrors": [
        "https://docker.xuanyuan.me",
        "https://docker.m.daocloud.io",
        "https://docker.1ms.run"
    ]
}
EOF
```

重启docker服务，使其生效：

```shell
sudo systemctl daemon-reload && sudo systemctl restart docker
```

如果使用 docker run hello-world 输出了 Hello from Docker!，证明配置成功，解决了网络问题，然后配置 Docker 在 WSL 启动时自动运行：

```shell
# 启用 Docker 服务自启动
sudo systemctl enable docker.service
# 启用 containerd 服务自启动
sudo systemctl enable containerd.service
```

验证状态：

```shell
systemctl status docker.service
systemctl status containerd.service

# 有输出 enable 证明设置成功
Loaded: loaded (/usr/lib/systemd/system/docker.service; enabled; preset: enabled)
                                                        ^^^^^^^ 
```

安装 Docker Compose：

```shell
# 24.0.2 版本之前可用
sudo apt-get update  
sudo apt-get install docker-compose-plugin  
```

如果是最新测试版的 Ubuntu（Noble）使用以上命令是无法成功安装的，因为这个还是测试版，它的插件包可能还没有发布，所以没法直接从 Ubuntu 的软件包仓库中下载，
不过可以直接添加官方源，直接获取最新的 Docker 插件：

安装依赖工具：

```shell
sudo apt update
sudo apt install -y ca-certificates curl gnupg
```

添加 Docker 官方 GPG 密钥（校验软件包的来源和完整性）:

```shell
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
sudo chmod a+r /etc/apt/keyrings/docker.gpg
```

```shell
cell@LAPTOP-SVEUFK1D:~$ ls -l /etc/apt/keyrings/docker.gpg
# 导入成功
-rw-r--r-- 1 root root 2760 Jul  3 21:49 /etc/apt/keyrings/docker.gpg
```

添加 Docker 官方软件源：

```shell
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | \
  sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
```

更新软件包索引并安装:

```shell
sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
```

验证安装：

```shell
# 检查 Docker 版本
docker --version
# 输出版本号
Docker version 28.3.1, build 38b7060

# 检查 Docker Compose 插件版本
docker compose version
# 输出版本号
Docker Compose version v2.38.1

# 检查 Docker 服务状态
sudo systemctl status docker
# 正常运行
Active: active (running) since Thu 2025-07-03 22:05:59 CST; 2min 7s ago
```

****
#### 1.2 利用 Docker 运行 MySQL 

准备两个目录，用于挂载容器的数据和配置文件目录：

```shell
# 进入/tmp目录
cd /tmp
# 创建文件夹
mkdir mysql
# 进入mysql目录
cd mysql
```

进入 mysql 目录后，执行下面的 Docker 命令：

```shell
docker run \
 -p 3306:3306 \
 --name mysql \
 -v $PWD/conf:/etc/mysql/conf.d \
 -v $PWD/logs:/logs \
 -v $PWD/data:/var/lib/mysql \
 -e MYSQL_ROOT_PASSWORD=123 \
 --privileged \
 -d \
 mysql:8.0.33
```

查看容器是否运行成功：

```shell
docker ps
# 成功运行
CONTAINER ID   IMAGE          COMMAND                  CREATED             STATUS          PORTS                                                    NAMES
da2d20b23750   mysql:8.0.33   "docker-entrypoint.s…"   About an hour ago   Up 16 seconds   0.0.0.0:3306->3306/tcp, [::]:3306->3306/tcp, 33060/tcp   mysql

ll
# 文件创建成功
drwxr-xr-x  2 cell    cell 4096 Jul  3 20:54 conf/
drwxr-xr-x  8 dnsmasq root 4096 Jul  3 22:11 data/
drwxr-xr-x  2 root    root 4096 Jul  3 20:50 logs/
```

在 mysql 目录下通过 vi conf/my.cnf 创建文件，然后将以下内容粘贴进该文件，然后使用 docker restart mysql 重启容器：

```shell
[mysqld]
skip-name-resolve
character_set_server=utf8
datadir=/var/lib/mysql
server-id=1000
```

然后在 MySQL 中创建新的数据库，用 ip 作为连接名（172.23.14.3）

****
#### 1.3 导入一个 Demo 工程

导入后需要注意更新 pom 依赖：

```xml
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
    <version>3.5.9</version>
</dependency>
<!--于 v3.5.9 起，PaginationInnerInterceptor 已分离出来，如需使用，则需单独引入 mybatis-plus-jsqlparser 依赖-->
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-jsqlparser</artifactId>
    <version>3.5.9</version>
</dependency>
```

****
#### 1.4 运行 nginx 服务

上面的程序导入成功后，访问 http://localhost/item.html?id=10001 即可看到对应的数据库中的信息(写死的数据，不是动态的)，需要向服务器发送 ajax 请求，查询商品数据

```text
Request URL
http://localhost/api/item/10001
Referrer Policy
strict-origin-when-cross-origin
```

查看 nginx 的 conf 目录下的 nginx.conf 文件：

```text
#user  nobody;
worker_processes  1;

events {
    worker_connections  1024;
}

http {
    include       mime.types;
    default_type  application/octet-stream;

    sendfile        on;
    #tcp_nopush     on;
    keepalive_timeout  65;
    # nginx的业务集群，做nginx本地缓存、redis缓存、tomcat缓存
    upstream nginx-cluster{
        # 虚拟机的 ip 地址，也就是 nginx 业务集群要部署的地方
        server 172.23.14.3:8081;
    }
    server {
        listen       80;
        server_name  localhost;
    # 监听 /api 路径，反向代理到 nginx-cluster 集群
	location /api {
            proxy_pass http://nginx-cluster;
        }

        location / {
            root   html;
            index  index.html index.htm;
        }

        error_page   500 502 503 504  /50x.html;
        location = /50x.html {
            root   html;
        }
    }
}
```

Nginx 监听 80 端口，此时访问的 /item.html 资源来源于 Nginx 的 html 目录，然后 /api 请求会被反向代理到后端 172.23.14.3:8081，即 SpringBoot 项目：

```text
浏览器 -> Nginx :80 -> 后端服务(SpringBoot) -> 前端页面
浏览器访问：http://localhost/api/item/10001 ，Nginx 把请求转发：http://172.23.14.3:8081/api/item/10001 ，SpringBoot 接收到请求，内部连数据库
浏览器 Ajax -> Nginx :80 -> /api/item/10001 -> 后端服务(SpringBoot)
```

****
#### 1.5 初识 Caffeine

Caffeine 是 Java 平台下的本地高性能缓存库，它广泛用于对性能要求极高的系统，到需要高频率、低延迟的需求时可以考虑使用。

基本用法：

```java
// 创建缓存对象
Cache<String, String> cache = Caffeine.newBuilder().build();
// 存数据
cache.put("name", "张三");
// 取数据，不存在则返回null
String name = cache.getIfPresent("name");
System.out.println("name = " + name);
// 取数据，不存在则去数据库查询（也被叫做备选方案）
String defaultName = cache.get("defaultName", key -> {
    // 这里可以去数据库根据 key 查询 value
    // key 对应的其实就是 defaultName，只不过换个名字提高可读性
    return "李四";
});
System.out.println("defaultName = " + defaultName);
```

Caffeine 提供了三种缓存驱逐策略：

- **基于容量**：设置缓存的数量上限

```java
// 创建缓存对象
Cache<String, String> cache = Caffeine.newBuilder()
        // 设置缓存大小上限为 1，只能存放一条数据
        .maximumSize(1)
        .build();
// 存数据
cache.put("name1", "张三");
cache.put("name2", "李四");
cache.put("name3", "王五");
// 延迟 10 ms，给清理线程一点时间
Thread.sleep(10L);
// 获取数据，最终只会输出一个 name 的值
System.out.println("name1" + cache.getIfPresent("name1"));
System.out.println("name2: " + cache.getIfPresent("name2"));
System.out.println("name3: " + cache.getIfPresent("name3"));
```

- **基于时间**：设置缓存的有效时间

```java
// 创建缓存对象
Cache<String, String> cache = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofSeconds(1)) // 设置缓存有效期为 10 秒
        .build();
// 存数据
cache.put("name", "赵六");
// 获取数据
System.out.println("name: " + cache.getIfPresent("name")); // 赵六
// 休眠一会儿
Thread.sleep(1200L);
System.out.println("name: " + cache.getIfPresent("name")); // null
```

还有一些常用的参数：

- maximumSize(long size)：限制缓存最大条目数
- maximumWeight(long weight)：按权重限制总缓存容量（需配合 weigher()）
- expireAfterWrite(time)：写入后多长时间数据过期
- expireAfterAccess(time)：最后一次访问后多长时间数据过期
- refreshAfterWrite(time)：写入后多长时间异步刷新（不影响读取，后台更新）
- recordStats()：开启统计功能，获取命中率、加载次数
- weakKeys()、weakValues()：弱引用key或value，避免内存泄漏，适合短生命周期数据
- removalListener()：监听缓存移除事件

****
#### 1.6 实现 JVM 进程缓存

创建一个缓存配置文件，专门用来创建 Caffeine 缓存，并且让它们纳入 IoC 管理，避免以后多次手动创建缓存：

```java
// 创建商品缓存
@Bean
public Cache<Long, Item> itemCache(){
    return Caffeine.newBuilder()
            .initialCapacity(100)
            .maximumSize(10_000)
            .build();
}
// 创建库存缓存
@Bean
public Cache<Long, ItemStock> stockCache(){
    return Caffeine.newBuilder()
            .initialCapacity(100) // 设置缓存初始容量
            .maximumSize(10_000) // 最大缓存上限
            .build();
}
```

浏览器输入对应的 URL 后，第一次控制台会输出对应的查询数据库的 SQL 日志：

```sql
SELECT id,name,title,price,image,category,brand,spec,status,create_time,update_time FROM tb_item WHERE (status <> ? AND id = ?)
```

再次查询后就不再输出了，证明走的是缓存

```java
// 查商品
return itemCache.get(id, key -> itemService.query()
                .ne("status", 3).eq("id", key)
                .one()
        );

// 查库存
return stockCache.get(id, key -> stockService.getById(key));
```

****
### 2. Lua 语法入门

Lua 是一种轻量小巧的脚本语言，用标准 C 语言编写并以源代码形式开放，其设计目的是为了嵌入应用程序中，从而为应用程序提供灵活的扩展和定制功能。而 Nginx 本身也是 C 语言开发的，因此也允许基于 Lua 做拓展。

#### 2.1 变量与数据类型

Lua 是动态类型语言，变量无需声明类型，而是用 local 来声明变量为局部变量，作用于当前文件(不使用 local 那就是全局变量)：

```lua
local a = 10        -- 整数
local b = 3.14      -- 浮点数
local c = "hello" .. 'world'  -- 声明字符串，可以用单引号或双引号，用 .. 拼接
local d = true      -- 布尔值
local e = nil       -- 空值
```

Lua 中的 table 类型既可以作为数组，又可以作为 Java 中的 map 来使用，数组就是特殊的 table，key 是数组角标而已：

```lua
-- 声明数组 ，key 为角标的 table
local arr = {'java', 'python', 'lua'}
-- 声明 table，类似 java 的 map
local map =  {name='Jack', age=21}
```

Lua 中的数组角标是从 1 开始，访问的时候与 Java 中类似：

```lua
-- 访问数组，lua数组的角标从1开始
do -- 因为使用的是局部变量，如果不在同一行可能就访问不到，所以用 do ... end 包裹起来
    local arr = {'java', 'python', 'lua'}
    print(arr[1])  -- 正确输出 "java"
end
```

Lua 中的 table 可以用 key 来访问：

```lua
print(map['name']) -- Jack
print(map.name) -- Jack
print(map)
table: 0x604f55ac8390
```

数据类型，可以利用 type 函数测试给定的变量或值的类型：

- nil：表示无值、空
- boolean：布尔值 true 或 false
- number：数值类型（整数、浮点数）
- string：字符串
- table：表（数组、字典、对象统一结构）
- function：函数
- userdata：自定义 C 数据类型
- thread：协程

例如：

```shell
> print(type(true))
boolean # true 是布尔类型
> print(type(print))
function # print 是函数类型
```


通过 lua 命令进入 lua 的控制台，测试 lua 语法：

```shell
cell@LAPTOP-SVEUFK1D:~$ lua
Lua 5.3.6  Copyright (C) 1994-2020 Lua.org, PUC-Rio
```

****
#### 2.2 循环

对于 table 可以利用 for 循环来遍历，不过数组和普通 table 遍历略有差异：

遍历数组：

```lua
-- 声明数组 key为索引的 table
local arr = {'java', 'python', 'lua'}
-- 遍历数组
for index,value in ipairs(arr) do -- do 代表大括号的开始
    print(index, value) 
end -- end 代表大括号的结束
```

```lua
1       java
2       python
3       lua
```

遍历普通 table：

```lua
-- 声明map，也就是table
local map = {name='Jack', age=21}
-- 遍历 table
for key,value in pairs(map) do
   print(key, value) 
end
```

```lua
age     21
name    Jack
```

- ipairs：是一个用于顺序遍历数组（数字索引表）的迭代器函数。它会从索引 1 开始，依次遍历表中的连续整数键（1, 2, 3, ...），直到遇到第一个 nil 值为止
- pairs：是一个用于遍历表中所有键值对的迭代器函数，它会遍历表中的所有键（无论类型是数字、字符串还是其他），并返回对应的键和值。但 pairs 不保证遍历顺序，但能确保遍历所有元素。

****
#### 2.3 条件控制、函数

定义函数的语法：

```lua
function 函数名( argument1, argument2..., argumentn)
    -- 函数体
    return 返回值
end
```

例如，定义一个函数，用来打印数组：

```lua
function printArr(arr)
    for index, value in ipairs(arr) do
        print(value)
    end
end
```

类似 Java 的条件控制，例如 if、else 语法：

```lua
if(布尔表达式)
then
   --[ 布尔表达式为 true 时执行该语句块 --]
else
   --[ 布尔表达式为 false 时执行该语句块 --]
end
```

与 java 不同，布尔表达式中的逻辑运算是基于英文单词：

- and：逻辑与操作符
- or：逻辑或操作符
- not：逻辑非操作符

例如：自定义一个函数，可以打印 table，当参数为 nil 时，打印错误信息

```lua
function printArr(arr)
    if not arr then
        print('数组不能为空！')
        return -- 结束运行
    end
    for index, value in ipairs(arr) do
        print(value)
    end
end
```

****
### 3. 实现多级缓存

#### 3.1 安装 OpenResty

1、更新系统并安装依赖

```shell
sudo apt update
sudo apt upgrade -y
sudo apt install -y wget gnupg software-properties-common
```

2、添加 OpenResty 官方 APT 仓库

```shell
wget -O - https://openresty.org/package/pubkey.gpg | sudo apt-key add -
sudo add-apt-repository -y "deb http://openresty.org/package/ubuntu $(lsb_release -sc) main"
sudo apt update
```

3、安装 OpenResty

```shell
sudo apt install -y openresty
```

4、验证安装

```shell
openresty -v
# 输出版本号
nginx version: openresty/1.27.1.2
```

5、启动 OpenResty 服务

```shell
sudo systemctl start openresty
sudo systemctl enable openresty  # 设置开机自启
```

检查服务状态：

```shell
sudo systemctl status openresty
# 正常运行
Loaded: loaded (/usr/lib/systemd/system/openresty.service; enabled; preset: enabled)
Active: active (running) 
```

6、配置 nginx 的环境变量

```shell
sudo vi /etc/profile
```

然后在最下面加入两行：

```shell
export NGINX_HOME=/usr/local/openresty/nginx
export PATH=${NGINX_HOME}/sbin:$PATH
```

然后让配置生效：

```shell
source /etc/profile
```

nginx 的默认配置文件注释太多，影响后续的编辑，这里将 nginx.conf 中的注释部分删除，保留有效部分，修改 /usr/local/openresty/nginx/conf/nginx.conf 文件，内容如下：

```shell
#user  nobody;
worker_processes  1;
error_log  logs/error.log;

events {
    worker_connections  1024;
}

http {
    include       mime.types;
    default_type  application/octet-stream;
    sendfile        on;
    keepalive_timeout  65;

    server {
        listen       8081;
        server_name  localhost;
        location / {
            root   html;
            index  index.html index.htm;
        }
        error_page   500 502 503 504  /50x.html;
        location = /50x.html {
            root   html;
        }
    }
}
```

启动命令：

```shell
# 启动 nginx（绝对路径启动）
sudo /usr/local/openresty/nginx/sbin/nginx
# 配置软连接后直接使用即可（sudo nginx、sudo nginx -s reload、sudo nginx -s stop）
sudo ln -s /usr/local/openresty/nginx/sbin/nginx /usr/local/bin/nginx
# 重新加载配置
sudo /usr/local/openresty/nginx/sbin/nginx -s reload
# 停止
sudo /usr/local/openresty/nginx/sbin/nginx -s stop
```

查看日志命令：

```shell
tail -f /usr/local/openresty/nginx/logs/error.log
```

在浏览器输入 http://172.23.14.3:8081，看到 Welcome to OpenResty! 证明安装成功。

****
#### 3.2 反向代理流程

windows 上的 nginx 用来做反向代理服务，将前端的查询商品的 ajax 请求代理到 OpenResty 集群，然后 OpenResty 集群用来编写多级缓存业务，因为 nginx 会监听浏览器发送的请求，
然后讲请求代理给集群处理，

```shell
# 修改 /usr/local/openresty/nginx/conf/nginx.conf 文件（或已经写好的别人的），在其中的 http 下面，添加下面代码
# 主要作用是让 OpenResty 能够正确加载手动编写的 Lua 脚本或第三方 Lua/C 模块
#lua 模块
lua_package_path "/usr/local/openresty/lualib/?.lua;;";
#c模块     
lua_package_cpath "/usr/local/openresty/lualib/?.so;;";  
```

```shell
server {
    listen 8081;
    server_name localhost;
    # 修改 /usr/local/openresty/nginx/conf/nginx.conf 文件，在 nginx.conf 的 server 下面，添加对 /api/item 这个路径的监听
    location /api/item {
        default_type application/json; # 格式为 json
        content_by_lua_file lua/item.lua;
    }
    location / {
        root html;
        index index.html index.htm;
    }
}
```

例如现在监听的是 /api/item 和 / 路径，发送这些 URL 后就会跳转到对应的处理，发送 / 就直接进入 nginx 的欢迎页面，发送 /api/item 就是进入手动添加的一个 lua 文件，
这个监听，就类似于 SpringMVC 中的 @GetMapping("/api/item") 做路径映射，而 content_by_lua_file lua/item.lua 则相当于调用 item.lua 这个文件，
执行其中的业务，把结果返回给用户，也就是相当于 java 中调用 service。

****
#### 3.3 编写 item.lua

在 /usr/local/openresty/nginx 目录创建文件夹 lua 并进入该目录，然后创建 item.lua 文件:

```shell
cd /usr/local/openresty/nginx
sudo mkdir lua
cd lua
sudo vi item.lua
```

在 item.lua 中利用 ngx.say() 函数将数据返回到 response 中：

```shell
ngx.say('{"id":10001,"name":"SALSA AIR",...)
```

然后重新加载配置：

```shell
sudo -s reload
```

****
#### 3.3 请求参数处理

OpenResty 中提供了一些 API 用来获取不同类型的前端请求参数：

1、获取 URL 路径参数（Path Parameters）

当 URL 中包含动态路径部分时（如 /user/123），可以使用 location 块的正则匹配捕获参数：

```nginx
-- '~' 表示要使用正则表达式，(\d+) 表示对正则表达式分组，\d 代表所有的数字，+ 代表至少一个字符
location ~ ^/user/(\d+)$ {
    # 使用 $1 访问第一个捕获组（即用户 ID）
    content_by_lua_block {
        -- 匹配到的参数会存入 ngx.var 数组中，可以通过角标获取
        local user_id = ngx.var[1]  -- 获取第一个正则捕获组，若路径为 /user/123，则 ngx.var[1] 的值为 123
        -- 可以直接调用函数，也可以交给一个 item.lua 文件处理
        ngx.say("User ID: ", user_id)
        -- content_by_lua_file lua/item.lua;
    }
}
```

例如：

在 nginx.conf 文件中这样配置 location：

```nginx
 location ~ /api/item/(\d+) {
    # 默认的响应类型
    default_type application/json;
    # 响应结果由lua/item.lua文件来决定
    content_by_lua_file lua/item.lua;
}
```

在 item.lua 中这样配置：

```nginx
-- 获取商品id
local id = ngx.var[1]
-- 拼接并返回
ngx.say('{"id":' .. id .. ',"name":"SALSA AIR",...')
```

然后重启服务，在浏览器中带上 id 重新输入 URL：http://localhost:8081/api/item/10002 ，显示的内容 id 会变成 1002，

2、获取 URL 查询参数（GET 参数）

对于 URL 中的查询字符串（如 ?name=john&age=25），使用 ngx.req.get_uri_args() 解析：

```nginx
location /search {
    content_by_lua_block {
        local args = ngx.req.get_uri_args()  -- 返回一个包含所有参数的 Lua 表（键值对，table 类型）
        -- 例如：获取单个参数（带默认值）；对于重复参数（如 ?id=1&id=2），返回数组：args.id = {1, 2}
        local name = args.name or "guest"
        local age = tonumber(args.age) or 0 
        ngx.say("Name: ", name)
        ngx.say("Age: ", age)
    }
}
```

3、获取请求头参数（Headers）

使用 ngx.req.get_headers() 获取请求头信息：

```nginx
location /headers {
    content_by_lua_block {
        local headers = ngx.req.get_headers()  -- 获取所有请求头
        -- 获取特定请求头（忽略大小写）
        local user_agent = headers["User-Agent"]
        local token = headers["Authorization"] or ""
        ngx.say("User-Agent: ", user_agent)
        ngx.say("Authorization: ", token)
    }
}
```

4、获取 POST 表单参数

```nginx
location /login {
    content_by_lua_block {
        ngx.req.read_body()  -- 必须先读取请求体
        local args = ngx.req.get_post_args()  -- 获取 POST 参数
        local username = args.username or ""
        local password = args.password or ""
        ngx.say("Username: ", username)
        ngx.say("Password: ", password)
    }
}
```

5、获取 JSON 请求体

对于 application/json 格式的请求体，同样需要先读取请求体，再解析 JSON：

```nginx
location /api {
    content_by_lua_block {
        ngx.req.read_body()  -- 先读取请求体
        local data = ngx.req.get_body_data()  -- 获取原始请求体内容
        if data then
            local cjson = require("cjson")  -- 引入 JSON 解析库
            local ok, json_data = pcall(cjson.decode, data)
            if ok then
                -- 解析成功，访问 JSON 字段
                local name = json_data.name or ""
                local age = json_data.age or 0
                ngx.say("Name: ", name)
                ngx.say("Age: ", age)
            else
                ngx.status = 400
                ngx.say("Invalid JSON")
            end
        else
            ngx.status = 400
            ngx.say("No request body")
        end
    }
}
```

****
#### 3.4 查询 Tomcat

拿到商品 ID 后，本应去缓存中查询商品信息，不过目前还未建立 nginx、redis 缓存。因此，这里先根据商品 id 去 tomcat 查询商品信息。
需要注意的是，OpenResty 是在虚拟机，Tomcat 是在 Windows 电脑上，两者 IP 一定不要搞错了，如果 ip 填写错误就会导致访问不到 Windows 本地的 Tomcat 服务，
在 WSL2 中使用 ip route 查看主机 IP 地址：

```shell
ip route
# 主机 ip 为 172.23.0.1
default via 172.23.0.1 dev eth0 proto kernel
```

在 nginx 中，它提供了内部 API 用以发送 http 请求并处理响应的 Lua 模块，专门设计用于 OpenResty (Nginx + Lua) 环境：

```nginx
-- 这里的 path 是路径，并不包含 IP 和端口。这个请求会被 nginx 内部的 server 监听并处理。
local resp = ngx.location.capture("/path",{
    method = ngx.HTTP_GET,   -- 请求方式
    args = {a=1,b=2},  -- get方式传参数
})
```

返回的响应内容包括：

- resp.status：响应状态码
- resp.header：响应头，是一个table
- resp.body：响应体，就是响应数据

如果希望这个请求发送到 Tomcat 服务器，那么就还需要编写一个 server 来对这个路径做反向代理：

```nginx
location /item {
 # 这里是 windows 电脑的 ip 和 Java 服务端口，需要确保 windows 防火墙处于关闭状态
 proxy_pass http://172.23.0.1:8081; 
}
```

也就是说当 ngx.location.capture 发起请求后，会被反向代理到 windows 上的 Java 服务的 IP 和端口，
所以最终的请求就是：GET http://172.23.0.1:8081/path?a=1&b=2

****
#### 3.5 封装 http 工具

要基于 ngx.location.capture 来实现查询 tomcat，就得手动配置反向代理，设置好具体的 IP 与端口，并自定义 nginx 的内置 API。

1、添加反向代理到 Windows 的 Java 服务

因为 item-service 项目中的接口都是 /item 开头，所以需要监听的是 /item 路径，然后这个路径代理到 Windows 上的 tomcat 服务上，
修改 /usr/local/openresty/nginx/conf/nginx.conf 文件，添加一个 location：

```shell
sudo vi /usr/local/openresty/nginx/conf/nginx.conf
# 在 nginx.conf 中添加以下内容
location /item {
    proxy_pass http://172.23.0.1:8081;
}
```

当调用 ngx.location.capture("/item") 时，就能成功发送请求到 Windows 的 tomcat 上。

2、自定义 nginx 的内置 API

前面内容提到过，OpenResty 启动时会加载以下两个目录中的工具文件：

```shell
#lua 模块
lua_package_path "/usr/local/openresty/lualib/?.lua;;";
#c模块     
lua_package_cpath "/usr/local/openresty/lualib/?.so;;";  
```

所以自定义的 http 工具就可以放到 lualib 目录下，在 /usr/local/openresty/lualib 目录下，新建一个 common.lua 文件：

```shell
sudo vi /usr/local/openresty/lualib/common.lua
# 将以下内容写入 common.lua 文件中
# 封装函数，发送 http 请求，并解析响应
# 接收两个参数，一个路径，一个 GET 请求的参数表
local function read_http(path, params)
    local resp = ngx.location.capture(path,{
        method = ngx.HTTP_GET,
        args = params,
    })
    if not resp then
        -- 记录错误信息，返回404
        ngx.log(ngx.ERR, "http请求查询失败, path: ", path , ", args: ", args)
        ngx.exit(404)
    end
    return resp.body
end
-- 将方法导出
local _M = {  
    read_http = read_http
}  
return _M
```

这个工具将 read_http 函数封装到 _M 这个 table 类型的变量中，然后返回这个 table 表实现类似导出的效果。

3、实现商品查询

最后，修改 /usr/local/openresty/lua/item.lua 文件，利用刚刚封装的函数库实现对 tomcat 的查询：

```nginx
-- 引入自定义common工具模块，返回值是common中返回的 _M
local common = require("common")
-- 从 common中获取read_http这个函数
local read_http = common.read_http
-- 获取路径参数
local id = ngx.var[1]
-- 根据id查询商品
local itemJSON = read_http("/item/".. id, nil)
-- 根据id查询商品库存
local itemStockJSON = read_http("/item/stock/".. id, nil)
-- 测试：返回数据
ngx.say(itemJSON)
```

当通过访问 /api/item 路径时，就会去访问 item.lua 文件，然后就可以调用 require("common")，此时会查找名为 common.lua 的文件并执行该文件的内容，最后返回该文件最后返回的值（即 _M 表）

```nginx
location /api/item {
    default_type application/json; # 格式为 json
    content_by_lua_file lua/item.lua;
}
```

最终这里查询到的结果是 json 字符串，并且包含商品、库存两个 json 字符串（商品和库存信息），但页面最终需要的是把两个 json 拼接为一个 json，
所以需要先把 JSON 变为 lua 的 table 类型，在 item.lua 文件中完成数据整合后，再转为 JSON。

OpenResty 提供了一个 cjson 的模块用来处理 JSON 的序列化和反序列化：

1. 引入 cjson 模块

```nginx
local cjson = require "cjson"
```

2. 序列化

```nginx
local obj = {
    name = 'jack',
    age = 21
}
-- 把 table 序列化为 json
local json = cjson.encode(obj)
```

3. 反序列化

```nginx
local json = '{"name": "jack", "age": 21}'
-- 反序列化 json 为 table
local obj = cjson.decode(json);
print(obj.name)
```

在 item.lua 文件中添加 json 处理的功能：

```nginx
local common = require('common')
...
-- JSON转化为lua的table
local item = cjson.decode(itemJSON)
local stock = cjson.decode(itemStockJSON)

-- 组合数据
item.stock = stock.stock
item.sold = stock.sold

-- 把 item 序列化为 json 返回结果
ngx.say(cjson.encode(item))
```

****
#### 3.6 基于 ID 负载均衡

上面的代码中的 tomcat 是单机部署，而单机无法承载高并发，所以实际开发中 tomcat 一定是集群模式，不过 tomcat 集群本身不自带分发能力，所以需要负载均衡器配合使用，
而默认的负载均衡模式是轮询模式。在 Tomcat 集群中，每台 Tomcat 内部都有自己的 JVM 本地缓存（例如 Caffeine），但 JVM 缓存无法在多台 Tomcat 之间共享，
所以轮询负载均衡就会带来问题：

- 第一次请求：轮询到 8081，JVM 缓存建立
- 第二次请求：轮询到 8082，缓存未命中，重新查库
- 第三次请求：轮询到 8081，命中缓存

缓存生效依赖于是否轮询回同一个 tomcat，这就会造成命中率低，性能浪费。对于上面的代码，可以让同一商品请求固定路由到同一 tomcat，基于商品 ID 做负载均衡，
避免同一商品轮询不同的 tomcat 导致每次都重新查库建立缓存。

而 nginx 提供了基于请求路径做负载均衡的算法：

```nginx
hash $request_uri;
```

根据请求路径（如 /item/10001）做哈希，让哈希值对 Tomcat 数量取余，然后再决定落在哪台机器，只要 item 后面跟的 id 不变，那就可以保证同个商品一定一直访问同一个 tomcat，
确保 JVM 缓存生效。

首先，定义 tomcat 集群，并设置基于路径做负载均衡：

```nginx
upstream tomcat-cluster {
    hash $request_uri;
    server 172.23.0.1:8081;
    server 172.23.0.1:8082;
}
```

修改 /usr/local/openresty/nginx/conf/nginx.conf 文件，实现基于 ID 做负载均衡：

```nginx
location /item {
    # proxy_pass http://172.23.0.1:8081;
    proxy_pass http://tomcat-cluster;
}
```

****
#### 3.7 Redis 缓存预热

缓存预热指的是在系统上线或重启后，提前将热点数据加载到 Redis 缓存中，避免系统冷启动时缓存为空，导致大量请求直接查询数据库，从而造成数据库压力骤增。

使用步骤：

1、利用 Docker 安装 Redis

```shell
docker run --name redis -p 6379:6379 -d redis redis-server --appendonly yes
```

启动前需要查看本地 Redis 是否占用了 6379 端口：

```shell
sudo lsof -i :6379
# 显示 6379 被本地 Redis 占用
COMMAND   PID  USER   FD   TYPE DEVICE SIZE/OFF NODE NAME
redis-ser 195 redis   20u  IPv4   5491      0t0  TCP *:redis (LISTEN)
```

如果只打算使用 Docker 的 Redis 就关掉本地的 Redis：

```shell
sudo systemctl stop redis
```

然后再次启动 Docker 的 Redis：

```shell
docker start redis
```

2、在项目中添加 Redis 依赖

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

3、配置 Redis 地址

```yaml
spring:
  data:
    redis:
      host: 172.23.14.3
```

4、编写初始化类

缓存预热需要在项目启动时完成，并且必须是拿到 RedisTemplate 之后。这里利用 InitializingBean 接口来实现，因为 InitializingBean 可以在对象被 Spring 创建并且成员变量全部注入后开始执行。

```java
@Override
public void afterPropertiesSet() throws Exception {
    // 初始化缓存
    // 1. 查询商品信息
    List<Item> itemList = itemService.list();
    // 2. 放入缓存
    for (Item item : itemList) {
        // item序列化为JSON
        String json = MAPPER.writeValueAsString(item);
        // 存入redis
        redisTemplate.opsForValue().set("item:id:" + item.getId(), json);
    }
    // 3. 查询商品库存信息
    List<ItemStock> stockList = stockService.list();
    // 4. 放入缓存
    for (ItemStock stock : stockList) {
        // item序列化为JSON
        String json = MAPPER.writeValueAsString(stock);
        // 存入redis
        redisTemplate.opsForValue().set("item:stock:id:" + stock.getId(), json);
    }
}
```

因为测试中的商品用例较少，所以直接把所有数据都放进缓存中，将来再次访问时可以直接通过 Redis 获取。当然每次启动 SpringBoot 都会导致重新向 Redis 中写一份数据，
这就造成启动时压力较大，可以通过添加判断来决定哪些写入哪些不写入：

```java
// 只有在该 key 不存在时才写入
Boolean result = redisTemplate.opsForValue().setIfAbsent("item:id:", json);
```

****
#### 3.8 查询 Redis 缓存

因为之前设置的是 Java 内置的缓存 Caffeine，所以 OpenResty 查询的缓存是没用到 Redis 的，现在就可以让 OpenResty 优先查询 Redis 缓存，如果未命中再查询 Tomcat。
而 OpenResty 提供了操作 Redis 的模块，只要引入该模块就能直接使用，将 Redis 操作封装到之前的 common.lua 工具库中就可以快速使用。

1、修改 /usr/local/openresty/lualib/common.lua 文件，引入 Redis 模块，并初始化 Redis 对象：

```nginx
-- 导入redis
local redis = require('resty.redis')
-- 初始化redis
local red = redis:new()
red:set_timeouts(1000, 1000, 1000) -- 连接、发送、接收超时时间，单位毫秒
```

2、封装函数，用来释放 Redis 连接，其实是放入连接池：

```nginx
-- 关闭redis连接的工具方法，其实是放入连接池
local function close_redis(red)
    local pool_max_idle_time = 10000 -- 连接的空闲时间，单位是毫秒
    local pool_size = 100 --连接池大小
    local ok, err = red:set_keepalive(pool_max_idle_time, pool_size)
    if not ok then
        ngx.log(ngx.ERR, "放入redis连接池失败: ", err)
    end
end
```

3、封装函数，根据 key 查询 Redis 数据：

```nginx
-- 查询redis的方法 ip和port是redis地址，key是查询的key
local function read_redis(ip, port, key)
    -- 获取一个连接
    local ok, err = red:connect(ip, port)
    if not ok then
        ngx.log(ngx.ERR, "连接redis失败 : ", err)
        return nil
    end
    -- 查询redis
    local resp, err = red:get(key)
    -- 查询失败处理
    if not resp then
        ngx.log(ngx.ERR, "查询Redis失败: ", err, ", key = " , key)
    end
    --得到的数据为空处理
    if resp == ngx.null then
        resp = nil
        ngx.log(ngx.ERR, "查询Redis数据为空, key = ", key)
    end
    close_redis(red)
    return resp
end
```

4、导出

```nginx
-- 将方法导出
local _M = {  
    read_http = read_http,
    -- 添加新的函数
    read_redis = read_redis
}  
return _M
```

5、修改 /usr/local/openresty/nginx/item.lua 文件，添加一个查询函数：

```nginx
local read_redis = common.read_redis
-- 封装查询函数
function read_data(key, path, params)
    -- 查询本地缓存
    local val = read_redis("172.23.14.3", 6379, key)
    -- 判断查询结果
    if not val then
        ngx.log(ngx.ERR, "redis查询失败，尝试查询http， key: ", key)
        -- redis查询失败，去查询http
        val = read_http(path, params)
    end
    -- 返回数据
    return val
end
```

6、修改商品查询、库存查询的业务，使用新封装好的查询函数：

```nginx
-- 查询商品信息
local itemJSON = read_data("item:id:" .. id,  "/item/" .. id, nil)
-- 查询库存信息
local stockJSON = read_data("item:stock:id:" .. id, "/item/stock/" .. id, nil)
```

将上述文件全部更改后，测试项目，先让所有数据存入 Redis，然后关闭 SpringBoot 再刷新页面，看能否查到数据，如果能则配置成功。

****
#### 3.9 Nginx 本地缓存

在前面的配置中，已经完成了 JVM 进程缓存的配置（Caffeine）和 Redis 缓存的配置，而整个缓存的查询流程是先查询 OpenResty 本地缓存，再查 Redis，最后是进程缓存，
而 OpenResty 为 Nginx 提供了 shard dict 的功能，可以在 nginx 的多个 worker 之间共享数据，实现缓存功能。

1、开启共享字典，在 /usr/local/openresty/nginx/conf/nginx.conf 的 http 下添加配置：

```nginx
# 共享字典，也就是本地缓存，名称叫做：item_cache，大小150m
lua_shared_dict item_cache 150m; 
 ```

2、修改 /usr/local/openresty/lua/item.lua 文件，修改 read_data 查询函数，添加本地缓存逻辑：

```nginx
-- 导入共享词典，本地缓存
local item_cache = ngx.shared.item_cache

-- 封装查询函数
function read_data(key, expire, path, params)
    -- 查询本地缓存
    local val = item_cache:get(key)
    if not val then
        ngx.log(ngx.ERR, "本地缓存查询失败，尝试查询Redis， key: ", key)
        -- 查询redis
        val = read_redis("127.0.0.1", 6379, key)
        -- 判断查询结果
        if not val then
            ngx.log(ngx.ERR, "redis查询失败，尝试查询http， key: ", key)
            -- redis查询失败，去查询http
            val = read_http(path, params)
        end
    end
    -- 查询成功，把数据写入本地缓存
    item_cache:set(key, val, expire)
    -- 返回数据
    return val
end
```

3、修改 item.lua 中查询商品和库存的业务，实现最新的 read_data 函数：

```nginx
-- 查询商品信息
local itemJSON = read_data("item:id:" .. id, 1800,  "/item/" .. id, nil) -- 设置缓存时间，1800 s 后过期，自动删除
-- 查询库存信息
local stockJSON = read_data("item:stock:id:" .. id, 60, "/item/stock/" .. id, nil)
```

测试：

输入 tail -f /usr/local/openresty/nginx/logs/error.log 打开日志，打开 SpringBoot 输入 URL，第一次查询某个商品时日志会打印：

```text
本地缓存查询失败，尝试查询Redis， key: item:id:10004, client: 127.0.0.1, server: localhost, request: "GET /api/item/10004 HTTP/1.1", host: "localhost"
```

第二次访问该 URL 时，则不会打印错误信息，证明配置成功。

****
### 4. 缓存同步

缓存数据类似于数据库数据的副本，它可以加速读操作，但写操作仍需保证数据库是最终数据源。所以缓存与数据库写入时序不同步，
可能导致缓存数据和数据库数据不一致，不一致会造成读脏数据、数据丢失、业务逻辑错误，严重时会带来数据安全风险。而大多数情况下，浏览器查询到的都是缓存数据，
如果缓存数据与数据库数据存在较大差异，可能会产生比较严重的后果。而常见的数据同步策略有三种：

1、设置有效期：给缓存设置有效期，到期后自动删除，再次查询时再更新

- 优势：简单、方便
- 缺点：时效性差，缓存过期之前可能不一致
- 场景：更新频率较低，时效性要求低的业务

2、同步双写：在修改数据库的同时，直接修改缓存

- 优势：时效性强，缓存与数据库强一致
- 缺点：有代码侵入，耦合度高；
- 场景：对一致性、时效性要求较高的缓存数据

3、异步通知：修改数据库时发送事件通知，相关服务监听到通知后修改缓存数据

- 优势：低耦合，可以同时通知多个缓存服务
- 缺点：时效性一般，可能存在中间不一致状态
- 场景：时效性要求一般，有多个服务需要同步

而异步实现又可以基于 MQ 或者 Canal 来实现：

1、基于 MQ 的异步通知：

- 数据修改阶段：商品服务(ItemService)接收到商品数据修改请求，先将数据写入MySQL数据库，然后再向消息队列(MQ)发送一条变更通知消息（包含商品ID等关键信息）
- 缓存更新阶段：缓存服务作为 MQ 的消费者，它需要监听并接收到商品变更消息，然后根据消息内容查询最新数据并更新 Redis 缓存

虽然这种方式可以让商品服务无需关心缓存实现，只需在意发布事件，以此达到解耦合的效果，但依然可能存在数据不一致的问题（因为各种问题可能造成更新延迟）。

2、基于 Canal 的通知：

- 数据修改阶段：商品服务正常执行数据库操作
- Canal 监控到 MySQL 发生变化，立即通知缓存服务；缓存服务接收到 Canal 通知，更新缓存

#### 4.1 Canal

MySQL 主从同步机制：

- 1）MySQL master 将数据变更写入二进制日志（binary log），其中记录的数据叫做 binary log events
- 2）MySQL slave 将 master 的 binary log events 拷贝到它的中继日志(relay log)
- 3）MySQL slave 重放 relay log 中事件，将数据变更反映它自己的数据

而 Canal 就是把自己伪装成 MySQL 的一个 slave 节点，从而监听 master 的 binary log 变化，然后再把得到的变化信息通知给 Canal 的客户端，进而完成对其它数据库的同步。

Canal 的安装：

Canal 是基于 MySQL 的主从同步功能，因此必须先开启 MySQL 的主从功能才可以：

1、开启 binlog

打开 mysql 容器挂载的日志文件，在 /tmp/mysql/conf 目录:

```shell
sudo vi /tmp/mysql/conf/my.cnf

[mysqld]
skip-name-resolve
character_set_server=utf8
datadir=/var/lib/mysql
server-id=1000
log-bin=/var/lib/mysql/mysql-bin
binlog-do-db=redis
```

2、设置用户权限

添加一个仅用于数据同步的账户，出于安全考虑，这里仅提供对 redis 这个库的操作权限

```sql
create user canal@'%' IDENTIFIED by 'canal';
GRANT SELECT, REPLICATION SLAVE, REPLICATION CLIENT,SUPER ON *.* TO 'canal'@'%';
FLUSH PRIVILEGES;
      
ALTER USER 'canal'@'%' IDENTIFIED BY 'canal_password';
GRANT SELECT, REPLICATION SLAVE, REPLICATION CLIENT ON *.* TO 'canal'@'%';
FLUSH PRIVILEGES;
```

3、重启 MySQL 并查看设置是否成功

```shell
docker restart mysql
```

```sql
show master status;
```

文件名就是刚刚设置的 log-bin=/var/lib/mysql/mysql-bin，Position 就类似于 Redis 的 offset 偏移量，用来记录从文件与主文件的数据差值

| File            | Position | Binlog_Do_DB | Binlog_Ignore_DB | Executed_Gtid_Set |
|-----------------|----------|--------------|------------------|-------------------|
| mysql - bin.000001 | 476      | redis        |                  |                   |


安装 Canal：

1、创建网络，将 MySQL、Canal、MQ 放到同一个 Docker 网络中

```shell
docker network create cell

3e59daf994303a4776178bc382f91c7d280ce9af21e812d360023b15e78b092c
```

让 mysql 加入这个网络：

```shell
docker network connect cell mysql
```

2、拉取 canal 镜像

```shell
# 拉取最新稳定版
docker pull canal/canal-server:latest
```

3、运行命令创建 Canal 容器

```shell
docker run -p 11111:11111 --name canal \
-e canal.destinations=cell \
-e canal.instance.master.address=mysql:3306  \
-e canal.instance.dbUsername=canal  \
-e canal.instance.dbPassword=canal  \
-e canal.instance.connectionCharset=UTF-8 \
-e canal.instance.tsdb.enable=true \
-e canal.instance.gtidon=false  \
-e canal.instance.filter.regex=redis\\..* \
--network cell \
canal/canal-server:latest
```

- docker run -p 11111:11111 --name canal \：Canal 默认监听端口是11111，客户端要通过这个端口连接 Canal，给容器命名为 canal
- -e canal.destinations=cell \：定义 Canal 监听的实例名，叫 cell
- -e canal.instance.master.address=mysql:3306  \：指定 Canal 监听的 MySQL 主库地址，这里 mysql 是 MySQL 容器的名称或主机名，端口是 3306。果不知道 mysql 容器地址，可以通过 docker inspect 容器id 来查看
- -e canal.instance.dbUsername=canal：数据库用户名
- -e canal.instance.dbPassword=canal：数据库密码
- -e canal.instance.filter.regex=：要监听的表名称
- --network cell \：指定容器加入名为 cell 的 Docker 网络

```shell
docker logs -f canal
# 查看是否安装成功
DOCKER_DEPLOY_TYPE=VM
==> INIT /alidata/init/02init-sshd.sh
==> EXIT CODE: 0
==> INIT /alidata/init/fix-hosts.py
==> EXIT CODE: 0
==> INIT DEFAULT
==> INIT DONE
==> RUN /home/admin/app.sh
==> START ...
start canal ...
start canal successful
==> START SUCCESSFUL ...
```

4、查看日志

```shell
tail -f /home/admin/canal-server/logs/canal/canal.log
```

表名称监听支持的语法：

```text
mysql 数据解析关注的表，Perl 正则表达式.
多个正则之间以逗号(,)分隔，转义符需要双斜杠(\\) 
常见例子：
1.  所有表：.*   or  .*\\..*
2.  canal schema下所有表： canal\\..*
3.  canal下的以canal打头的表：canal\\.canal.*
4.  canal schema下的一张表：canal.test1
5.  多个规则组合使用然后以逗号隔开：canal\\..*,mysql.test1,mysql.test2 
```

****
#### 4.2 监听 Canal

1、引入依赖

```xml
<dependency>
    <groupId>top.javatool</groupId>
    <artifactId>canal-spring-boot-starter</artifactId>
    <version>1.2.1-RELEASE</version>
</dependency>
```

2、编写配置

```yaml
canal:
  destination: cell # canal的集群名字，要与安装canal时设置的名称一致，即与 destinations 一直
  server: 172.23.14.3:11111 # canal服务地址
```

3、编写监听器

通过实现 EntryHandler<T> 接口编写监听器，监听 Canal 消息，但需要注意两点：

- 实现类通过 @CanalTable("tb_item") 指定监听的表信息
- EntryHandler 的泛型是与表对应的实体类

当 MySQL 中的数据发生改变时（增删改），就会触发以下三个方法，然后把新的数据存入redis与JVM缓存中

```java
@CanalTable("tb_item")
@Component
public class ItemHandler implements EntryHandler<Item> {

    @Autowired
    private RedisHandler redisHandler;
    @Autowired
    private Cache<Long, Item> itemCache;

    @Override
    public void insert(Item item) {
        System.out.println("Insert event received: " + item);
        // 写数据到JVM进程缓存
        itemCache.put(item.getId(), item);
        // 写数据到redis
        redisHandler.saveItem(item);
    }

    @Override
    public void update(Item before, Item after) {
        System.out.println("Update event received: before=" + before + ", after=" + after);
        // 写数据到JVM进程缓存
        itemCache.put(after.getId(), after);
        // 写数据到redis
        redisHandler.saveItem(after);
    }

    @Override
    public void delete(Item item) {
        System.out.println("Delete event received: " + item);
        // 删除数据到JVM进程缓存
        itemCache.invalidate(item.getId());
        // 删除数据到redis
        redisHandler.deleteItemById(item.getId());
    }
}
```

****
## 四. Redis 键值设计

Redis 的 Key 虽然可以自定义，但最好遵循下面的几个最佳实践约定：

- 遵循基本格式：[业务名称]:[数据名]:[id]
- 长度不超过 44 字节
- 不包含特殊字符

例如登录业务，需要保存用户信息，其 key 可以设计成：login:user:1，这样设计的好处是可读性强，因为 redis 中没有 table 的概念，所以这样可以避免 key 冲突，
并且更节省内存，key 是 string 类型，底层编码包含 int、embstr 和 raw三 种，embstr 在小于44 字节时使用，采用连续内存空间，内存占用更小。
当字节数大于 44 字节时，会转为 raw 模式存储，在 raw 模式下，内存空间不是连续的，而是采用一个指针指向了另外一段内存空间，在这段空间里存储 SDS 内容，
这样空间不连续，访问的时候性能也就会收到影响，还有可能产生内存碎片。

```shell
set num 123
type num
string
object encoding num
"int"

set name Jack
object encoding name
"embstr"

set name aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
OK
object encoding name
"raw"
```

### 1. BigKey

BigKey 通常以 Key 的大小和 Key 中成员的数量来综合判定，例如：

- String 类型：Value 体积超大，如存储大文件内容
- Hash 类型：成员数量虽少，但单个 Value 数据超大
- ZSet / List / Set：成员数量巨大，单次操作时 Redis 需处理大量数据

BigKey 的危害:

网络阻塞：
- Redis 是单线程，虽然性能高，但一次性传输较高容量的数据，容易导致带宽使用率被占满

数据倾斜：
- BigKey 所在的 Redis 实例内存使用率远超其他实例，就会导致无法使数据分片的内存资源达到均衡

Redis阻塞：
- 对元素较多的 hash、list、zset 等做运算会耗时较旧，使主线程被阻塞

CPU压力：
- 对 BigKey 的数据序列化和反序列化会导致 CPU 的使用率飙升，影响 Redis 实例和本机其它应用

如何排查 BigKey：

1、利用 redis-cli 提供的 --bigkeys 参数，可以遍历分析所有 key，并返回 Key 的整体统计信息与每个数据的 Top1 的 big key

```shell
redis-cli --bigkeys

100.00% ||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||
Keys sampled: 15

-------- summary -------

Total key length in bytes is 193 (avg len 12.87)

Biggest string found "item:id:10004" has 467 bytes

0 lists with 0 items (00.00% of keys, avg size 0.00)
0 hashs with 0 fields (00.00% of keys, avg size 0.00)
0 streams with 0 entries (00.00% of keys, avg size 0.00)
15 strings with 2962 bytes (100.00% of keys, avg size 197.47)
0 sets with 0 members (00.00% of keys, avg size 0.00)
0 zsets with 0 members (00.00% of keys, avg size 0.00)
```

2、scan 扫描

手动编程，利用 scan 扫描 Redis 中的所有 key，利用 strlen、hlen 等命令判断 key 的长度（不建议使用MEMORY USAGE），
scan 命令调用完后每次会返回 2 个元素，第一个是下一次迭代的光标，第一次光标会设置为 0，当最后一次 scan 返回的光标等于 0 时，
表示整个 scan 遍历结束了，第二个返回的是 List，一个匹配的 key 的数组

```java
private Jedis jedis;
@BeforeEach
void setUp() {
    // 1. 建立连接
    jedis = new Jedis("172.23.14.3", 6379);
    // 2. 设置密码
    jedis.auth("123");
    // 3. 选择库
    jedis.select(0);
}
// 定义 BigKey 的阈值
final static int STR_MAX_LEN = 10 * 1024;
// 其他结构超过 500 个元素视为 BigKey
final static int HASH_MAX_LEN = 500;
@Test
void testScan() {
    int maxLen = 0;
    long len = 0;
    String cursor = "0";
    do {
        // 扫描并获取一部分key
        ScanResult<String> result = jedis.scan(cursor);
        // 记录cursor
        cursor = result.getCursor();
        List<String> list = result.getResult();
        if (list == null || list.isEmpty()) {
            break;
        }
        // 遍历
        for (String key : list) {
            // 判断key的类型
            String type = jedis.type(key);
            switch (type) {
                case "string":
                    len = jedis.strlen(key);
                    maxLen = STR_MAX_LEN;
                    break;
                case "hash":
                    len = jedis.hlen(key);
                    maxLen = HASH_MAX_LEN;
                    break;
                case "list":
                    len = jedis.llen(key);
                    maxLen = HASH_MAX_LEN;
                    break;
                case "set":
                    len = jedis.scard(key);
                    maxLen = HASH_MAX_LEN;
                    break;
                case "zset":
                    len = jedis.zcard(key);
                    maxLen = HASH_MAX_LEN;
                    break;
                default:
                    break;
            }
            if (len >= maxLen) {
                System.out.printf("Found big key : %s, type: %s, length or size: %d %n", key, type, len);
            }
        }
    } while (!cursor.equals("0"));
}
@AfterEach
void tearDown() {
    if (jedis != null) {
        jedis.close();
    }
}
```

在 Redis 中插入一条长度大于 10240 的数据，然后通过这个代码获取：

```text
Found big key : big:string, type: string, length or size: 11000 
```

BigKey 的删除：

因为 BigKey 内存占用较多，删除这样的 key 需要耗费很长时间，若直接释放大块内存或大量子元素，这就会导致 Redis 主线程阻塞，引发一系列问题。在 Redis 3.0 及以下版本，
如果是集合类型，则遍历 BigKey 的元素，先逐个删除子元素，最后删除 BigKey：

```shell
# 如果是集合类型，就逐个释放每个子元素
DEL bigkey
```

Redis 4.0 以后，使用异步删除 UNLINK 命令，它会立即将 key 从主字典中删除，但实际的内存释放是在后台由异步线程完成的：

```shell
# 返回 1 表示已标记删除，真正内存释放稍后在后台完成
UNLINK bigkey
```

****
### 2. 恰当的数据类型

存储一个 User 对象，通常有三种方法：

1、Json 字符串

|   key    |       value                  |
|:--------:|:----------------------------:|
| user:1   | {"name": "Jack", "age": 21}  |

- 优点：实现简单粗暴，可以同时获取全部信息
- 缺点：数据耦合，不够灵活，修改数据时较为麻烦

2、每个字段拆成独立的 key-value

|  key        | value |
|:-----------:|:-----:|
| user:1:name | Jack  |
| user:1:age  |  21   |

- 优点：可以灵活访问对象任意字段 
- 缺点：占用空间大、没办法做统一控制

Redis 每个 Key 都有自己的元数据（内部结构体等），这部分内存开销是固定的开销，段打散后，user:1:name 和 user:1:age 等多个 Key，
它们每个 Key 都需要存储完整的前缀（如 user:1:），这就会导致导致重复存储，且每个 Key 的元信息都占用空间，这就导致元信息大量无效重复

3、Hash 结构

|  key   | field | value |
|:------:|------:|:-----:|
| user:1 |  name | jack  |
|        |   age |  21   |

- 优点：底层使用 ziplist，空间占用小，可以灵活访问对象的任意字段 
- 缺点：代码相对复杂

Redis 中的 Hash 类似于 Java 的 Map<String, String> 结构，当数据较小时，底层使用 ziplist（压缩列表，内存连续分配，类似数组）优化存储，以此减少内存碎片，比单独拆散成多个 Key 节省空间，还可以通过配置 redis.conf 参数控制结构：

```redis
hash-max-ziplist-entries 512  # 最大字段数
hash-max-ziplist-value 64 # 最大 value 字节数
```

超过这个范围，底层结构自动切换为 hashtable 存储（每个元素占用独立内存块，通过哈希函数定位数据位置）。

假如有 hash 类型的 key，其中有 100 万对 field 和 value，field 是自增 id，这个 key 存在什么问题？如何优化？

存在的问题：当 hash 的 entry 数量超过 500 时，会使用哈希表而不是 ZipList，这就会导致内存占用较多，然后变成一个 bigkey。所以可以把这一整个 hash 结构拆分为多个 hash 结构，
例如将 id / 100 作为 key， 将 id % 100 作为 field，这样每 100个 元素为一个 hash，

|   key    | field |    value    |
|:--------:|------:|:-----------:|
|  key:0   | id:00 |   value0    |
|          | ..... |    .....    |
|          | id:99 |   value99   |
|  key:1   | id:00 |  value100   |
|          | ..... |    .....    |
|          | id:99 |  value199   |
|   ...    |   ... |     ...     |
| key:9999 | id:00 | value999900 |
|          |   ... |     ...     |
|          | id:99 | value999999 |

将大 hash 变成多个小 hash，保证每个 hash 底层使用的都是 ziplist 结构。

****
### 3. 批处理优化

Redis 提供了很多 Mxxx 这样的命令，可以实现批量插入数据，例如：

- MSET:批量设置多个字符串键值，让多个 key-value 一次写入，该操作具有原子性
- MGET：批量获取多个字符串键值
- HMSET：批量设置 Hash 的多个字段，但现在推荐使用 HSET
- HSET：支持批量设置 Hash 字段，新版 HSET 支持多字段一次性插入

客户端与 Redis 服务端的一次命令响应时间 = 1 次往返的网络传输耗时 + 1 次 Redis 执行命令耗时，而这里面主要耗时的就是网络传输，Redis 处理指令很快，
所以一次只传输一条指令的话就很浪费时间，如果一次传输多条数据就可以减少网络传输的次数。

```java
@Test
void testMxx() {
    String[] arr = new String[2000];
    int j;
    long b = System.currentTimeMillis();
    for (int i = 1; i <= 100000; i++) {
        // 每 1000 次循环执行一次批量操作
        j = (i % 1000) << 1; // (i % 1000) * 2，得到 0 - 999 然后获取偶数
        arr[j] = "test:key_" + i;
        arr[j + 1] = "value_" + i;
        if (j == 0) {
            jedis.mset(arr);
        }
    }
    long e = System.currentTimeMillis();
    System.out.println("time: " + (e - b));
}
```

虽然以上操作可以减少耗时，但仍然存在局限性，因为它们本质上是针对同一种数据结构的一次性多数据操作，并不能跨数据类型混用，例如 MSET 只操作字符串，
HSET 只操作 Hash，SADD 只操作 Set。因此，如果有对复杂数据类型的批处理需要，建议使用 Pipeline。

Pipeline 是 Redis 提供的一种客户端批量发送命令的机制，但它发送的每条命令独立，不具备原子性

```java
Pipeline pipeline = jedis.pipelined();

pipeline.set("key1", "value1");
pipeline.hset("user:1", "name", "jack");
pipeline.sadd("set1", "a", "b", "c");
pipeline.zadd("zset1", 1, "score1");
pipeline.zadd("zset1", 2, "score2");

pipeline.sync();
jedis.close();
```

集群下的批处理：

如 MSET 或 Pipeline 这样的批处理需要在一次请求中携带多条命令，而如果此时 Redis 是一个集群，那批处理命令的多个 key 就必须落在同一个插槽中，如果这些 Key 映射到了不同槽位，Redis 集群就无法确保所有 Key 在同一节点，就会导致执行失败。

解决方案：

1、串行执行，依次执行每个命令，虽然耗时效率低，但不会导致上述问题。

2、使用串行 slot，简单来说，就是执行前让客户端先计算一下对应的 key 的 slot，一样 slot 的 key 就放到一个组里边，然后对每个组执行 pipeline 的批处理，
它就能串行执行各个组的命令，这种做法比第一种方法耗时要少，但是相对来说执行较为复杂一点

3、使用并行 slot，相较于第二种方案，在分组完成后串行执行，该方法就变成了并行执行各个命令，所以他的耗时就非常短，但是实现更加复杂。

4、使用 hash_tag，redis 计算 key 的 slot 的时候，其实是根据 key 的有效部分来计算的，通过这种方式就能一次处理所有的 key，这种方式耗时最短，实现也简单，
但是如果通过操作 key 的有效部分，那么就会导致所有的 key 可能都落在一个节点上，产生数据倾斜的问题

例如串行 slot 方案：

```java
void testMSet2() {
    Map<String, String> map = new HashMap<>(3);
    map.put("name", "Jack");
    map.put("age", "21");
    map.put("sex", "Male");
    // 对 Map 数据进行分组。根据相同的slot放在一个分组
    // key 就是 slot，value 就是一个组
    Map<Integer, List<Map.Entry<String, String>>> result = map.entrySet()
            .stream()
            .collect(Collectors.groupingBy(
                    // 将 map 中的所有 entry 计算出它们的 key 对应的 slot，然后根据这个 slot 作为新的 key
                    entry -> ClusterSlotHashUtil.calculateSlot(entry.getKey()))
            );
    // 串行的去执行 mset 的逻辑， value 是 List<Map.Entry<String, String>>
    for (List<Map.Entry<String, String>> list : result.values()) {
        String[] arr = new String[list.size() * 2];
        for (int i = 0; i < list.size(); i++) {
            Map.Entry<String, String> e = list.get(i);
            arr[i * 2] = e.getKey();
            arr[i * 2 + 1] = e.getValue();
        }
        jedisCluster.mset(arr);
    }
}
```

Spring 集群环境下批处理代码就不会产生出现在不同 slot 的情况，

```java
void testMSetInCluster() {
    Map<String, String> map = new HashMap<>(3);
    map.put("name", "Rose");
    map.put("age", "21");
    map.put("sex", "Female");
    stringRedisTemplate.opsForValue().multiSet(map);

    List<String> strings = stringRedisTemplate.opsForValue().multiGet(Arrays.asList("name", "age", "sex"));
    if (strings != null) {
        strings.forEach(System.out::println);
    }
}
```

multiSet 底层调用 Lettuce（异步Redis客户端），通过 Lettuce 的异步命令接口调用 mset，然后同步获取结（这里依然是单节点执行的命令）

```java
public Boolean mSet(Map<byte[], byte[]> tuples) {
    Assert.notNull(tuples, "Tuples must not be null");
    return (Boolean)this.connection.invoke().from(RedisStringAsyncCommands::mset, tuples).get(Converters.stringToBooleanConverter());
}
```

这是集群环境下重写的 mset 方法，调用 SlotHash.partition，把所有 key 按照 Redis 集群的 slot 拆分成多个组，然后对每个分组执行父类的单节点的 mset，

```java
public RedisFuture<String> mset(Map<K, V> map) {
    Map<Integer, List<K>> partitioned = SlotHash.partition(this.codec, map.keySet());
    // 如果所有 key 都落在同一 slot，直接调用单节点的 mset 方法
    if (partitioned.size() < 2) {
        return super.mset(map);
    } else {
        Map<Integer, RedisFuture<String>> executions = new HashMap();
        for(Map.Entry<Integer, List<K>> entry : partitioned.entrySet()) {
            Map<K, V> op = new HashMap();
            ((List)entry.getValue()).forEach((k) -> op.put(k, map.get(k)));
            RedisFuture<String> mset = super.mset(op);
            executions.put(entry.getKey(), mset);
        }
        return MultiNodeExecution.firstOfAsync(executions);
    }
}
```

****
### 4. 服务器端优化

#### 4.1 持久化配置

Redis 的持久化虽然可以保证数据安全，但也会带来很多额外的开销，例如：需要大量内存配合 fork 子进程，rewrite（日志重写）期间有额外CPU、磁盘压力等，因此持久化请遵循下列建议：

* 用来做缓存的 Redis 实例尽量不要开启持久化功能

如果是纯粹做缓存（热点数据、短期临时数据）这种本身要求高性能、低延迟的操作，并且这些数据通常可以丢弃（可以通过数据库重建），如果对这些数据开启持久化，
就会消耗 CPU、内存和磁盘资源，与缓存追求的高性能目标冲突。

* 建议关闭 RDB 持久化功能，使用 AOF 持久化

RDB 快照在生成过程中需要 fork 子进程，虽然恢复速度较快，但整体内存消耗较高，而 AOF 通过牺牲磁盘的空间来解决占用较多内存的问题，并且数据丢失的风险更低。

* 利用脚本定期在 slave 节点做 RDB，实现数据备份（redis-cli bgsave）

如果主节点压力较大，并且数据较为重要，就应该避免使用 RDB 造成堵塞，让数据备份操作由 slave 进行，master 专注于读写操作。

* 设置合理的 rewrite 阈值，避免频繁的 bgrewrite
* 配置 no-appendfsync-on-rewrite = yes，禁止在 rewrite 期间做 aof，避免因 AOF 竞争导致的主线程阻塞

部署有关建议：
- Redis 实例的物理机要预留足够内存，应对 fork 和 rewrite
- 单个 Redis 实例内存上限不要太大，例如 4G 或 8G。避免大内存实例的 fork 操作，以加快 fork 的速度、减少主从同步、数据迁移压力
- 因为 Redis 是单线程模型，CPU 利用率直接影响请求处理能力，所以不要与 CPU 密集型应用部署在一起
- 数据库的随机 IO 与 Redis 的顺序 IO 模式冲突，而消息队列的批量写入会抢占磁盘带宽，所以尽量不要与它们一起部署

****
#### 4.2 慢查询优化

凡是在 Redis 内部执行时间超过设定阈值的命令，都视为慢查询，即便只是查一个很小的 key，如果 Redis 内部逻辑复杂、数据结构膨胀，也可能触发慢查询。
由于 Redis 是单线程的，所以当客户端发出指令后，它们都会进入到同一个队列中，如果此时有一些慢查询的数据，就会导致大量请求阻塞，从而引起报错。

常见慢查询：

- 大键查询（get bigkey）
- 复杂聚合操作，如执行 SORT
- 正则匹配查询，可能会全库扫描键空间，阻塞主线程
- 滥用全盘扫描
- AOF 刷盘阻塞导致命令处理延迟

慢查询会被放入慢查询日志中，日志的长度有上限，可以通过配置指定：

```redis
# 默认值
slowlog-log-slower-than:10000 # 阈值(微秒)
slowlog-max-len:128 # 日志最大长度
slowlog-entries:5 # 当前日志条目数
```

通过 get 获取，通过 set 修改：

```redis
config get slowlog-log-slower-than
1) "slowlog-log-slower-than"
2) "10000"

config get slowlog-max-len
1) "slowlog-max-len"
2) "128"
```

```redis
config set slowlog-log-slower-than 1000
OK
config get slowlog-log-slower-than
1) "slowlog-log-slower-than"
2) "1000"
```

查看慢查询：

* slowlog len：查询慢查询日志长度
* slowlog get [n]：读取n条慢查询日志
* slowlog reset：清空慢查询列表

****
#### 4.3 敏感命令及安全配置

默认情况下 Redis 服务监听 0.0.0.0:6379，即所有公网 IP 可直接访问，如果 redis.conf 没有开启 requirepass 配置（设置密码），那么任意客户端无需身份认证即可操作 Redis。

为了避免这样的漏洞，这里给出一些建议：

* Redis 一定要设置密码
* 禁止线上使用下面命令：keys、flushall、flushdb、config set 等命令，可以利用 rename-command 禁用。
* bind：限制网卡，禁止外网网卡访问
* 开启防火墙
* 不要使用 Root 账户启动 Redis
* 尽量不使用默认的端口

****
#### 4.4 内存配置

当 Redis 内存不足时，可能导致 Key 频繁被删除、响应时间变长、QPS 不稳定等问题。当内存使用率达到 90% 以上时就需要警惕，并快速定位到内存占用的原因。

Redis 内存三大核心组成：

- 数据内存：存储所有 Key、Value 数据，是 Redis 内存占用的大头，重点关注 BigKey、碎片
- 进程内存：Redis 运行本身的占用，包括代码区、常量区、堆栈，通常几 MB，可忽略不计
- 缓冲区内存：Redis 读写过程中为保障性能设计的内存区域，波动大，需重点监控

内存碎片：

内存碎片指的是由于内存分配策略，导致系统分配的实际内存大于程序真正使用的内存，多出来的空间无法被有效利用，这部分浪费空间被称为内存碎片。
Redis 默认使用 jemalloc 分配内存，jemalloc 并不会精准按需要的内存大小来分配，而是按照内存块粒度进行分配，例如申请 5 字节，实际分配 8 字节。

```redis
info memory
# Memory
used_memory:19068056
used_memory_rss:37302272
used_memory_rss_human:35.57M
total_system_memory:8212672512
maxmemory_policy:noeviction
allocator_frag_ratio:1.02
allocator_rss_ratio:1.21
rss_overhead_ratio:1.53
mem_fragmentation_ratio:1.96

used_memory:         实际使用内存
used_memory_rss:     操作系统分配的物理内存
mem_fragmentation_ratio: 碎片率
```

当 ratio 超过 2 时证明此时内存严重浪费，需要进行调整。

缓冲区内存：

1、客户端缓冲区

每个客户端连接 Redis 时，Redis 都会为该客户端分配输入、输出缓冲区，输入缓冲区负责存放客户端发送的命令请求；输出缓冲区负责存放 Redis 执行完命令后返回的结果。

输出缓冲区的风险与原因：

当客户端发送请求 GET big:string，因为 Redis 是单线程执行很快，5MB 数据会立即进入输出缓冲区，若客户端网络慢、消费慢，就会导致输出缓冲区无法及时释放，
如果多个客户端同时执行类似操作，输出缓冲区总体暴涨，就会导致 Redis 宕机，数据丢失。

默认情况下输出缓冲区是没设置大小的，这就会导致它无限增长：

```redis
CONFIG GET client-output-buffer-limit
client-output-buffer-limit normal 0 0 0
```

可以设置它的软硬上限让它及时断开连接，避免 Redis 断连

```redis
client-output-buffer-limit <类别> <硬上限> <软上限> <软限制生效时间（秒）>
client-output-buffer-limit normal 64mb 16mb 60 # 普通客户端
client-output-buffer-limit pubsub 32mb 8mb 60 # 订阅/发布的客户端
client-output-buffer-limit replica 256mb 64mb 60 # 主从同步客户端，从节点连接主节点
```

2、AOF 缓冲区

AOF 持久化开启时，执行的写操作会先写入内存缓冲区，然后再批量进行刷盘操作，但无法设置大小，相关配置如下：

```redis
appendfsync always # 每次写操作立刻持久化，开销大
appendfsync everysec # 每秒同步一次，常用，兼顾性能与数据安全
appendfsync no # 由操作系统决定何时刷盘，效率高但数据不安全
```

3、复制缓冲区

Redis 主从同步时，主机会维护一片复制缓冲区用来存放全量或增量数据，然后将这片缓冲区的内容发送给从节点，可以通过 replbacklog-size 来设置，默认 1mb。

****
#### 4.5 集群最佳实践

集群虽然具备高可用特性，能实现自动故障恢复，但是如果使用不当，也会存在一些问题：

1、在 Redis 的默认配置中，如果发现任意一个插槽不可用，则整个集群都会停止对外服务

Redis 未保障数据完整性，防止访问不一致的数据问题，默认是开启该功能的，假如某节点宕机，导致部分 Slot 丢失，那么该集群整体都罢工， 这对于业务功能来讲是不合适的，
它会严重导致效率低下，所以在一般不需要强一致性的情况下可以选择关闭，这样即使有部分插槽不能使用，但整体集群还是在工作中的。

```redis
cluster-require-full-coverage yes
```

2、集群带宽问题

集群节点之间会不断的互相 Ping 来确定集群中其它节点的状态，而每次 Ping 携带的信息至少包括插槽信息和集群状态信息，当集群中节点越多，集群状态信息数据量也就越大，
10 个节点的相关信息可能达到 1kb，当打击群频繁的通讯，此时每次集群互通需要的带宽会非常高，这样会导致集群中大量的带宽都会被占用，极端情况西可能导致 Redis 响应变慢。

所以在使用时：

- 避免超大型集群，官方建议集群节点总数不宜超过 1000，如果有超大规模业务，应该拆分成多个独立 Redis 集群，避免单集群过重
- 不建议单机部署太多 Redis 实例，当多个 Redis 共用带宽时，必定会占用较多网络资源
- 配置 cluster-node-timeout，当节点在指定时间内未接收到消息，则认为其处于宕机状态

3、命令的集群兼容性问题

当使用批处理的命令时，redis 要求处理中的所有 key 必须落在相同的 slot 上，当有大量的 key 同时操作时可能是无法完成的，所以客户端必须要对这样的数据进行处理。

4、lua 和事务的问题

Redis 是串行执行命令的，当一次性将多条命令发送到 Redis 时，Redis 只会保证命令的顺序性，不会保证原子性，即使部分命令失败，也不会回滚，这就是 Redis 的事务操作；
而 lua 脚本是由 Redis 服务器执行的命令，它的存在让 Redis 的事务真正具备了原子性（Redis 执行脚本时不会执行其他命令，脚本要么完全执行成功，要么完全不执行）。
对于集群来说，它是无法发挥事务的特性的，因为事务内只能操作同槽 key，否则 Redis 无法分发到正确节点统一处理，lua 脚本同理。

大部分情况下单体 Redis（主从 Redis）已经能达到万级别的 QPS（1秒时间内能够成功处理的请求数量），并且也具备很强的高可用特性。
如果在主从能满足业务需求的情况下，尽量不搭建 Redis 集群。

****
## 五. Redis 数据结构

### 1. 动态字符串

Redis 中保存的 Key 是字符串，value 往往也是字符串或者字符串的集合，不过 Redis 没有直接使用 C 语言中的字符串，因为 C 语言字符串存在很多问题，
例如获取字符串长度的需要通过运算，且是非二进制安全的，声明的字符串（底层为数组）不可修改等，所以 Redis 构建了一种新的字符串结构，称为简单动态字符串（Simple Dynamic String），
简称 SDS。例如执行 set name Jack，Redis 在底层会创建两个 SDS，一个包含 name，另一个包含 Jack。

Redis 的 SDS 结构有五种，分别是 sdshdr5、sdshdr8、sdshdr16、sdshdr32、sdshdr64，它们分别对应使用的字节大小，

```c
#define SDS_TYPE_5  0 // 对应 sdshdr5，表示 3 位，即 31 字节以内
#define SDS_TYPE_8  1 // 对应 sdshdr8，表示 1 - 255 字节
#define SDS_TYPE_16 2 // 对应 sdshdr16，表示 2 - 65535 字节（64KB）
#define SDS_TYPE_32 3 // 对应 sdshdr32，表示 4 字节 - 4GB 
#define SDS_TYPE_64 4 // 对应 sdshdr64，表示 8 字节 - 18EB
```

SDS 结：

```c
struct __attribute__ ((__packed__)) sdshdr8 {
    uint8_t len; // 已保存的字符串字节数，不包含结束标识
    uint8_t alloc; // 申请的总的字节数，不包含结束标识
    unsigned char flags; // 不同 SDS 类型，用来控制 SDS 的头大小，即 SDS_TYPE_8
    char buf[]; // 存放实际字符串内容
};
```

sdshdr5 与其他类型不同，没有单独的 len 和 alloc 字段，长度直接存储在 flags 的高 5 位（最多 31 位），只适用于非常短的字符串优化

```c
struct __attribute__ ((__packed__)) sdshdr5 {
    unsigned char flags; // 低3位存类型，高5位存长度
    char buf[];
};
```

例如一个存储 "Hello" 的 sdshdr8：

```text
+-----+------+-------+-------------------+
| len | alloc| flags |      buf          |
+-----+------+-------+-------------------+
|  5  |  10  |   1   |'H''e''l''l''o''\0'|
+-----+------+-------+-------------------+
```

len 代表该字符串的长度（英文、数字、标点通常只占一字节，中文占三字节    ），alloc 代表已分配的物理总空间，flags 代表使用的使用的是哪种类型，末尾的 0 是自动添加的空终止符，但实际上 Redis 根据 len 的长度来判断是否读取结束，即读取前五个元素即可。

SDS 之所以叫做动态字符串，是因为它具备动态扩容的能力，例如一个内容为 “hi” 的 SDS：

```text
+-----+------+-------+---------------+
| len | alloc| flags |      buf      |
+-----+------+-------+---------------+
|  3  |  3   |   1   | 'h''i'',''\0' | 
+-----+------+-------+---------------+
```

Redis 根据新字符串总长度，决定如何扩容，当新字符串总长度小于 1 MB，则新空间为扩展后字符串长度的两倍 +1（加一个终止符，但不会计入 len 中）；如果新字符串大于 1MB，则新空间为扩展后字符串长度 +1MB +1

```text
+-----+------+-------+-----------------------+
| len | alloc| flags |        buf            |
+-----+------+-------+-----------------------+
|  6  |  12  |   1   |'h''i'',''A''m''y''\0' |
+-----+------+-------+-----------------------+
```

因为这种结构的特性，Redis 可以在 O(1) 时间复杂度内获取长度（读取 len），并且拼接前会进行长度判断，以及申请的本地空间可以有效减少空间分配的次数。

****
### 2. intset

IntSet 是 Redis 内部用来实现 Set 集合的一种底层数据结构，基于整数数组来实现，并且具备长度可变、有序等特征，适用于集合中的所有元素都是整数且个数较少的情况。

```c
typedef struct intset {
    uint32_t encoding; // 编码方式，支持存放 16 位、32 位、64 位整数
    uint32_t length; // 元素个数
    int8_t contents[]; // 实际存储整数元素的数组
} intset; 
```

其中的 encoding 包含三种模式，表示存储的整数大小不同：

```c
#define INTSET_ENC_INT16 (sizeof(int16_t)) // 2 字节整数，类似于 Java 的 short
#define INTSET_ENC_INT32 (sizeof(int32_t)) // 4 字节整数，类似于 Java 的 int
#define INTSET_ENC_INT64 (sizeof(int64_t)) // 8 字节整数，类似于 Java 的 long
```

为了方便查找，Redis 会将 intset 中所有的整数按照升序依次保存在 contents 数组中：

```text
+------------------+--------+-------+-------+-------+
|     encoding     | length |   1   |   3   |   5   |
+------------------+--------+-------+-------+-------+
| INTSET_ENC_INT16 |   3    |   1   |   3   |   5   |
+------------------+--------+-------+-------+-------+
```

数组中每个数字都在 int16_t 的范围内，因此采用的编码方式是 INTSET_ENC_INT16，每部分占用的字节大小为：

- encoding：4 字节
- length：4 字节
- contents：2 * 3 = 6 字节

contents 数组中存放的每个整数的大小都是一样的，因为这样就保证了可以基于下标快速查找，而且元素是从 0 开始计算的，所以可以得到一个数学公式：address = start + (sizeof(int_16) * index)，

但当向其中添加一个数字 50000，这个数字就超过了 int16_t 的范围，此时 intset 就会自动升级编码方式到合适的大小：

- 升级编码为 INTSET_ENC_INT32, 每个整数占4字节，并按照新的编码方式及元素个数扩容数组
- 倒序依次将数组中的元素拷贝到扩容后的正确位置，如果从头开始移动元素的话，每次都要移动整个集合，而倒序就不需要了
- 将待添加的元素放入数组末尾
- 最后，将 inset 的 encoding 属性改为 INTSET_ENC_INT32，将 length 属性改为 4

插入方法：

```c
intset *intsetAdd(intset *is, int64_t value, uint8_t *success) {
    // 获取新元素适用的编码
    uint8_t valenc = _intsetValueEncoding(value);
    // 声明要插入的位置
    uint32_t pos;
    if (success) *success = 1;
    // 判断编码是否超过了当前 intset 的编码
    if (valenc > intrev32ifbe(is->encoding)) {
        // 超出则进行升级
        return intsetUpgradeAndAdd(is,value);
    } else {
        // 在当前 intset 中查找值与 value 一致的元素的角标
        if (intsetSearch(is,value,&pos)) {
            // 如果找到了则无需插入，返回 0 代表插入失败
            if (success) *success = 0;
            return is;
        }
        // 数组扩容操作，只是增加 contents 容量，原数据不动
        is = intsetResize(is,intrev32ifbe(is->length)+1);
        // 若插入位置不是数组末尾，则移动数组中 pos 之后的元素到 pos + 1，给新元素腾出空间
        if (pos < intrev32ifbe(is->length)) intsetMoveTail(is,pos,pos+1);
    }
    // 插入新元素
    _intsetSet(is,pos,value);
    // 重置元素长度
    is->length = intrev32ifbe(intrev32ifbe(is->length)+1);
    return is;
}
```

编码升级：

```c
static intset *intsetUpgradeAndAdd(intset *is, int64_t value) {
    // 获取当前 intset 的编码
    uint8_t curenc = intrev32ifbe(is->encoding);
    // 获取新编码
    uint8_t newenc = _intsetValueEncoding(value);
    // 获取元素个数
    int length = intrev32ifbe(is->length);
    // 判断新元素整数负数，负数设置为 1，正数设置为 0
    int prepend = value < 0 ? 1 : 0;
    // 重置编码
    is->encoding = intrev32ifbe(newenc);
    // 重置数组大小
    is = intsetResize(is,intrev32ifbe(is->length)+1);
    // 倒序遍历，逐个搬运元素到新的位置，_intsetGetEncoded 方法则是按照就编码的方式查找旧元素
    while(length--)
        _intsetSet(is,length+prepend,_intsetGetEncoded(is,length,curenc));
    // 插入新元素，1 就插数组首位
    if (prepend)
        _intsetSet(is,0,value);
    // 0 就插数组尾部
    else
        _intsetSet(is,intrev32ifbe(is->length),value);
    // 修改数组长度
    is->length = intrev32ifbe(intrev32ifbe(is->length)+1);
    return is;
}
```

****
### 3. Dict

Redis 是一个键值型（Key-Value）的数据库，可以根据 key 实现快速的增删改查，而键与值的映射关系正是通过 Dict 结构来实现的。Dict 由三部分组成，
分别是：哈希表（DictHashTable）、哈希节点（DictEntry）、字典（Dict）。Redis 的 Set 类型会根据集合中的元素是否为整数来判断是否使用 Dict 结构，
如果元素数量超过 set-max-intset-entries 配置（默认 512），也会自动转换。

```c
// 哈希表结构，实际的存储空间
typedef struct dictht {
    dictEntry **table; // 指向 dictEntry 指针数组，数组每个槽位是链表头指针
    unsigned long size; // 哈希表的大小
    unsigned long sizemask; // 哈希表大小的掩码，总等于 size - 1，用来快速计算
    unsigned long used; // 实际存储的键值对数量
} dictht;
```

union 是 C 语言的一种特殊结构，它可以内部多个成员互斥使用，同一时间只能使用其中一种类型，但可根据需要切换，因为 Redis 支持多样化数据结构，所以 value 的存储需求多变，
存储字符串时就让 void* val 指向字符串，存放整数，就用 int64_t s64 直接存数字...具体存储什么类型，需配合 dict 结构中的 dictType 的操作函数决定。

```c
// 哈希节点，存储每个键值对
typedef struct dictEntry {
    void *key; // 键指针
    union {
        void *val; // 值指针
        uint64_t u64; // 64位无符号整数
        int64_t s64; // 64位有符号整数
        double d; // 双精度浮点数
    } v;
    struct dictEntry *next; // 指向下一个节点，形成链表
} dictEntry;
```

向 Dict 添加键值对时，Redis 首先根据 key 计算出 hash 值（h），然后利用 h & sizemask 来计算元素应该存储到数组中的哪个索引位置。而 dictht 中的 size 大小始终为 2^n，
与 Java 的 Hash 结构一样，利用该特性可以做到与 % 运算一样的结果，让 sizemask = size - 1 就是为了让 size 的低位为 1，这样 key 与 sizemask 做与运算时只需要考虑低位即可，
以此达到 key = 2^n + q 的效果。

```text
哈希表数组(dictEntry*[4]):
+-----+-----+-----+-----+
| [0] | [1] | [2] | [3] |
+-----+-----+-----+-----+
|  ↘  |  ↘  | NULL| NULL|
+-----+-----+-----+-----+
     |     |
     |     +------------------+
     |                        |
     v                        v
+------------+            +------------+
|  dictEntry |            |  dictEntry |
+------------+            +------------+
|   key: k1  |            |   key: k3  |
|   val: v1  |            |   val: v3  |
| *next:NULL |            | *next:     |
+------------+            +------------+
                          |
                          v
                     +------------+
                     |  dictEntry |
                     +------------+
                     |   key: k2  |
                     |   val: v2  |
                     | *next:NULL |
                     +------------+
```

插入 k1 -> v1 时，先计算 k1 的哈希值，假设计算结果 index = 0，然后就查找槽位 [0]，发现为空，直接新建节点，插入 k2 -> v2 同理；如果此时插入 k3 -> v3 与 k2 发生哈希冲突，
此时就会更新结构，使用头插法插入 [1] 号槽位的第一个位置，让 k2 链接在 k3 后面，以此提高插入效率，避免使用尾插法遍历整个链表。而 Redis 是单线程模型，所有操作都在一个线程中完成，
所以天然避免了并发冲突，也就不会像 Java 的 Hash 结构那样。

```c
// 字典本体，管理哈希表、扩容状态等
typedef struct dict {
    dictType *type; // dict 类型，内置不同的 hash 函数
    void *privdata; // 私有数据，在做特殊 rehash 运算时用
    dictht ht[2]; // 一个 Dict 包含两个哈希表，其中一个是存放当前数据，另一个一般是空的，rehash 时再使用
    long rehashidx; // rehash 的进度，-1 表示未进行
    int16_t pauserehash; // 用来判断 rehash 是否暂停，1 则暂停，0 则继续
} dict;
```

Dict 的扩容：

Dict 中的哈希表就是数组结合单向链表的实现，当集合中元素较多时，必然导致哈希冲突增多，链表过长也会导致查询效率大大降低。所以在每次执行插入、更新等操作前，
都会调用下面的 _dictExpandIfNeeded 方法，用来动态判断是否需要扩容。判断条件则是负载因子（LoadFactor = used/size）的大小，
负载因子 ≥ 1，即数据数量 ≥ 槽位数量，这是基础扩容的触发条件，此时系统允许扩容，但通常需要避免与 RDB、AOF 子进程冲突；当负载因子 > 强制扩容阈值（通常是 5），
此时进行强制扩容，避免链表过长性能恶化。

```c
static int _dictExpandIfNeeded(dict *d) {
    // 如果正在 rehash 就退出
    if (dictIsRehashing(d)) return DICT_OK;

    // 如果哈希表为空，则初始化哈希表为默认大小 4
    if (d->ht[0].size == 0) return dictExpand(d, DICT_HT_INITIAL_SIZE);

    // 当负载因子（used/size）达到 1 以上，并且当前没有进行 bgsave、rewrite 等子进程操作
    // 或者当负载因子超过 5，则进行 dictExpand，也就是扩容操作
    if (d->ht[0].used >= d->ht[0].size &&
        (dict_can_resize ||
         d->ht[0].used/d->ht[0].size > dict_force_resize_ratio) &&
        dictTypeExpandAllowed(d)) {
        // 扩容大小为 used + 1，底层会对扩容大小进行判断，实际上是找第一个大于等于 used + 1 的 2^n
        return dictExpand(d, d->ht[0].used + 1);
    }
    return DICT_OK;
}
```

目标扩容容量为 used + 1，即现有元素数再多分配 1 个槽位，实际上，_dictExpand 内部会对传入的 size 参数处理，总是扩容到大于等于 used + 1 的最小 2 的幂次方

```c
unsigned long _dictNextPower(unsigned long size) {
    unsigned long i = DICT_HT_INITIAL_SIZE;
    while (1) {
        if (i >= size) return i;
        i *= 2;
    }
}
```

```c
int dictExpand(dict *d, unsigned long size) {
    return _dictExpand(d, size, NULL);
}
```

传入 d：目标字典、size：希望扩容的槽位数量、malloc_failed：标记是否分配内存失败（初始为 null）。

```c
int _dictExpand(dict *d, unsigned long size, int* malloc_failed) {
    if (malloc_failed) *malloc_failed = 0;
    // 如果当前 entry 数量超过了要申请的 size 大小，或者正在 rehash，就直接报错
    if (dictIsRehashing(d) || d->ht[0].used > size)
        return DICT_ERR;
    dictht n; // 声明新的哈希表
    unsigned long realsize = _dictNextPower(size); // 计算实际数组大小，找到第一个大于等于 size 的 2 的幂次方
    // 计算出的目标数组大小小于期望，逻辑出错
    if (realsize < size || realsize * sizeof(dictEntry*) < realsize)
        return DICT_ERR;

    // 新的 size 与旧的 size 一致也报错
    if (realsize == d->ht[0].size) return DICT_ERR;

    // 重置新的 hash table 的大小和掩码
    n.size = realsize;
    n.sizemask = realsize-1;
    if (malloc_failed) {
        n.table = ztrycalloc(realsize*sizeof(dictEntry*));
        *malloc_failed = n.table == NULL;
        if (*malloc_failed)
            return DICT_ERR;
    } else
        n.table = zcalloc(realsize*sizeof(dictEntry*)); // 分配内存

    n.used = 0;

    // 如果是第一次，直接把 n 赋值给 ht[0] 即可
    if (d->ht[0].table == NULL) {
        d->ht[0] = n;
        return DICT_OK;
    }
    // 上面的不等于 null，证明有初始数据，即现在正在扩容或者收缩，那么就把 rehashidx 置为 0，在每次增删改查时都出发 rehash
    d->ht[1] = n;
    d->rehashidx = 0;
    return DICT_OK;
}
```

Dict 的 rehash：

不管是扩容还是收缩，必定会创建新的哈希表，导致哈希表的 size 和 sizemask 变化，而 key 的查询与 sizemask 有关。因此必须对哈希表中的每一 key 重新计算索引，
插入新的哈希表的过程则称为 rehash，而每次执行增删改查，触发一小步搬迁：

```c
int dictRehash(dict *d, int n) {
    int empty_visits = n*10; // 限制空桶访问最大次数，避免极端情况下陷入死循环
    if (!dictIsRehashing(d)) return 0; // 判断是否在 rehash，如果在则继续进行搬迁

    while(n-- && d->ht[0].used != 0) { // 判断条件为需要搬迁的槽位和 ht[0] 表中的数据
        dictEntry *de, *nextde;

        // rehashidx 用来记录搬迁进度，不能超出旧表数组边界
        assert(d->ht[0].size > (unsigned long)d->rehashidx);
        while(d->ht[0].table[d->rehashidx] == NULL) {
            d->rehashidx++;
            if (--empty_visits == 0) return 1;
        }
        de = d->ht[0].table[d->rehashidx];
        // 搬迁链表上的所有节点
        while(de) {
            uint64_t h;

            nextde = de->next; // 重新计算新表索引
            h = dictHashKey(d, de->key) & d->ht[1].sizemask; // 头插法插入新表
            de->next = d->ht[1].table[h];
            d->ht[1].table[h] = de;
            d->ht[0].used--;
            d->ht[1].used++;
            de = nextde;
        }
        d->ht[0].table[d->rehashidx] = NULL;
        d->rehashidx++;
    }

    // 检查 rehash 是否结束
    if (d->ht[0].used == 0) {
        zfree(d->ht[0].table); // 释放旧表内存
        d->ht[0] = d->ht[1]; // 新表变为主表
        _dictReset(&d->ht[1]); // 重置 ht[1]，准备下次扩容
        d->rehashidx = -1; // 标记 rehash 结束
        return 0;
    }

    // 搬迁未完成，返回 1 作为标记
    return 1;
}
```

计算新哈希表的 realSize，值取决于当前要做的是扩容还是收缩，但本质上是一致的，扩容是找到 used + 1 的最小 2^n，收缩则是找到最接近 used 的最小 2^n。
不管怎么样，最终都会进行 rehash，然后把新哈希表的 size 传进去，rehash 操作并不会一次性完成，而是分多次完成，然后下次继续从 ht[0] 中转移元素，
而查询、修改、删除操作则是依次对这两个表进行查找，不过新增操作则是直接写进 ht[1] 中，这样就可以避免再从 ht[0] 中移动一遍，也就是说，rehash 时 ht[0] 只减不增，
直到 ht[0] 为空，然后再重置 ht[1]。

****
### 4. ZipList

ZipList（压缩列表） 是 Redis 中一种为节省内存而设计的特殊数据结构，它的底层由一块连续的内存区域组成，类似数组，但它并不使用传统链表那样的“前后指针”来串联元素。
ZipList 是通过一套编码规则和内部字段来自行推算每个元素的准确位置。ZipList 中所有存储长度的数值均采用小端字节序，即低位字节在前，高位字节在后。
例如：数值 0x12|34，采用小端字节序后实际存储值为：0x34|12

结构：

- zlbytes：4 字节，用于记录整个 ZipList 占用的内存总字节数（包含所有字段）     
- zltail：4 字节，用于记录最后一个 Entry 节点到 ZipList 起始位置的偏移量（字节单位）    
- zllen：2 字节，用于记录 Entry 节点数量（最大 65535，超过则需遍历统计）
- Entry 列表：可变长度，实际存储数据的节点集合
- zlend：1 字节，用于固定结束标记（0xFF），0xFF                       

每个 Entry 包含 revlen（前驱节点长度）、encoding（数据编码）、data（实际数据）：

- previous_entry_length

前一节点的长度，占 1 个或 5 个字节，如果前一节点的长度小于 254 字节，则采用 1 个字节来保存这个长度值；如果前一节点的长度大于 254 字节，则采用 5 个字节来保存这个长度值。
第 1 字节存 0x00，后 4 字节存真正的前一节点长度，使用小端字节序

- encoding

表示当前节点数据类型和长度，支持字符串类型和整数类型

- contents

负责保存节点的数据，可以是字符串或整数。

字符串类型：

编码信息中低两位或高两位区分类型：

- 00xxxxxx：字符串，长度小于 64 字节 
- 01xxxxxx xxxxxxxx：字符串，长度小于 16384 字节 
- 10000000 后跟 32 位长度：超大字符串

例如保存字符串 "ab" 和 "bc"：

第一个节点 "ab"：

```text
+-----------------------+-----------------+------------------+
| previous_entry_length | encoding (类型) | contents (数据)   |
+-----------------------+-----------------+------------------+
|         0x00          | 0b00000010      | 'a' 'b'          |
+-----------------------+-----------------+------------------+
```

由于 "ab" 是第一个节点，前面没有节点，所以只占 1 字节，存放 0x00，值为 0，存放的 "ab" 长度为 2 字节（只用 1 字节保存长度），所以编码为 0b00000010（最高两位 00 表示字符串，后 6 位存长度），
contents 部分只有两个字符，所以占 2 字节，整体就是 4 字节。

第二个节点 "bc"：

```text
+-----------------------+-----------------+------------------+
| previous_entry_length | encoding (类型) | contents (数据)   |
+-----------------------+-----------------+------------------+
|         0x04          | 0b00000010      | 'b' 'c'          |
+-----------------------+-----------------+------------------+
```

前一个节点总长度 = 1（prevlen）+ 1（encoding）+ 2（数据） = 4，因此存储 0x04（1字节）;字符串类型，长度为 2（用 1 字节存储），contents 占用 2 字节，所以整体为 4 字节

```text
+-------------+-------------+----------+-----------------------+-----------------+------------------+-----------------------+-----------------+------------------+-------+
|   zlbytes   |   zltail    |  zllen   | previous_entry_length | encoding (类型) | contents ('ab')  | previous_entry_length | encoding (类型)  | contents ('bc')  | zlend |
+-------------+-------------+----------+-----------------------+-----------------+------------------+-----------------------+-----------------+------------------+-------+
| 0x14|000000 | 0x0E|000000 | 0x01|00  |         0x00          |    0b00000010   |  'a' 'b'         |         0x04          |   0b00000010    |    'b' 'c'       | 0xFF  |
+-------------+-------------+----------+-----------------------+-----------------+------------------+-----------------------+-----------------+------------------+-------+
```

头部总长度 = zlbytes(4) + zltail(4) + zllen(2) = 10 字节；节点总长度 = 节点 1 (4) + 节点 2 (4) = 8 字节；zlend 长度 = 1 字节；
总长度 = 10 + 8 + 1 = 19 字节，但 Redis 内部通常会按 4 字节对齐，19 字节需向上取整为 20 字节

整数类型：

如果 encoding 是以 “11” 开始，则证明 content 是整数，且 encoding 固定只占用 1 个字节来表示长度：

| 编码标识         | 占用字节  | 含义                                    |
| ------------ | ----- |---------------------------------------|
| `0b11000000` | 1 字节  | 8 位有符号整数（-128 ~ 127）                  |
| `0b11010000` | 2 字节  | 16 位有符号整数（-32,768 ~ 32,767）           |
| `0b11100000` | 3 字节  | 24 位有符号整数                             |
| `0b11110000` | 4 字节  | 32 位有符号整数                             |
| `0b11111110` | 8 字节  | 64 位有符号整数                             |
| `0b1111xxxx` | 无额外字节 | 直接存储，范围从0001~1101，减1后结果为实际值 0 ~ 12 的小整数 |

例如存整数 1 和 99：

```text
+-----------------------+-----------------+
| previous_entry_length | encoding (类型) |
+-----------------------+-----------------+
|        0x00           | 0b11110001      |
+-----------------------+-----------------+
```

因为是小整数，所以可以省略 contents，整体为 1 + 1 = 2 字节

```text
+-------------+-------------+----------+-----------------------+-----------------+-------+
|   zlbytes   |    zltail   |   zllen  | previous_entry_length | encoding (类型) | zlend |
+-------------+-------------+----------+-----------------------+-----------------+-------+
| 0x10|000000 | 0x0A|000000 | 0x01|00  |         0x00          |    0b11110001   | 0xFF  |
+-------------+-------------+----------+-----------------------+-----------------+-------+
```

头部总长度 = zlbytes(4) + zltail(4) + zllen(2) = 10 字节；节点总长度 = 2 字节；zlend 长度 = 1 字节；
总长度 = 10 + 2 + 1 = 13 字节，但 Redis 内部通常会按 4 字节对齐，13 字节需向上取整为 16 字节。

现在存入 99：

```text
+-----------------------+-----------------+------------------+
| previous_entry_length | encoding (类型) | contents (数据)   |
+-----------------------+-----------------+------------------+
|       0x02            |    0b11000000   |      99          |
+-----------------------+-----------------+------------------+
```

前一个节点长度为 2 字节，所以 previous_entry_length = 2（0x02）；99 不属于小整数范围，所以需要用 8 位整数存储，即 0b11000000；
contents 存放 99，占 1 字节，总共 1 + 1 + 1 = 3 字节。

```text
+-------------+-------------+---------+-----------------------+-----------------+-----------------------+-----------------+------------------+-------+
|   zlbytes   |    zltail   |  zllen  | previous_entry_length | encoding (类型) | previous_entry_length | encoding (类型)  | contents (数据)   | zlend |
+-------------+-------------+---------+-----------------------+-----------------+-----------------------+-----------------+------------------+-------+
| 0x10|000000 | 0x0C|000000 | 0x02|00 |         0x00          |    0b11110001   |         0x02          |    0b11000000   |       99         | 0xFF  |
+-------------+-------------+---------+-----------------------+-----------------+-----------------------+-----------------+------------------+-------+
```

头部总长度 = zlbytes(4) + zltail(4) + zllen(2) = 10 字节；节点总长度 = 节点 1 (2) + 节点 2 (3) = 5 字节；zlend 长度 = 1 字节；
总长度 = 10 + 5 + 1 = 16 字节。

由以上内容可以得知：ZipList 的每个 Entry 都包含 previous_entry_length 来记录上一个节点的大小，长度是 1 个或 5 个字节，如果前一节点的长度小于 254 字节，则采用 1 个字节来保存这个长度值；
如果前一节点的长度大于等于 254 字节，则采用 5 个字节来保存这个长度值，然后第一个字节就成为 0xFE（用于标识前一个节点的真实长度大于等于 254 字节），
后四个字节才是真实长度数据。现在，假设有 N 个连续的、长度为 250~253 字节之间的 entry，因此 entry 的 previous_entry_length 属性用 1 个字节即可表示，
但是当插入或删除某个数据后，让某个节点的长度大于 254 字节了，这就导致下一个节点的 previous_entry_length 要变成 5 字节，
也就是新增 4 字节，然后该节点的长度也大于 254 字节了，以此类推，就会产生连续多次的空间扩展操作，称为连锁更新。

****
### 5. QuickList

ZipList 虽然节省内存，但申请的内存必须是连续空间，如果内存占用较多，那么申请内存的效率就很低，所以必须限制 ZipList 的长度和 entry 大小，所以可以考虑创建多个 ZipList 来分片存储数据，
但由于 ZipList 是不带指针的，所以分散后不便于管理，所以 Redis 引入了 QuickList 结构，它是一个双端链表，只不过链表中的每个节点都是一个 ZipList，并且中间节点可以压缩，进一步节省了内存

为了避免 QuickList 中的每个 ZipList 中 entry 过多，Redis 提供了一个配置项 list-max-ziplist-size 来限制，如果值为正数 N，则代表 ZipList 的允许的 entry 个数的最大值为 N，
如果值为负，则代表 ZipList 的最大内存大小，分 5 种情况（也就是 fill 的值）：

* -1：每个 ZipList 的内存占用不能超过 4kb
* -2：每个 ZipList 的内存占用不能超过 8kb（默认值）
* -3：每个 ZipList 的内存占用不能超过 16kb
* -4：每个 ZipList 的内存占用不能超过 32kb
* -5：每个 ZipList 的内存占用不能超过 64kb

```c
typedef struct quicklist {
    quicklistNode *head; // 头节点指针
    quicklistNode *tail; // 尾节点指针
    unsigned long count; // 所有 ZipList 中 Entry 总数  
    unsigned long len; // QuickListNode 节点数量
    int fill : QL_FILL_BITS; // 节点容量控制参数（正值: Entry 数；负值: 内存 KB）
    unsigned int compress : QL_COMP_BITS; // 两端不压缩节点数量
    unsigned int bookmark_count: QL_BM_BITS; // 书签数量（用于快速定位）
    quicklistBookmark bookmarks[]; // 书签数组
} quicklist;
```

```c
typedef struct quicklistNode {
    struct quicklistNode *prev; // 前驱节点
    struct quicklistNode *next; // 后继节点
    unsigned char *zl; // 指向 ZipList 的内存
    unsigned int sz; // ZipList 占用字节数
    unsigned int count : 16; // ZipList 中 Entry 数量
    unsigned int encoding : 2; // 编码类型：RAW==1 或 LZF==2（压缩标志）
    unsigned int container : 2; // 数据容器类型：NONE==1 或 ZIPLIST==2
    unsigned int recompress : 1; // 节点是否处于解压态，操作完成后需重新压缩
    unsigned int attempted_compress : 1; // 压缩尝试失败标志，数据过小不压缩
    unsigned int extra : 10; // 预留扩展位
} quicklistNode;
```

****
### 6. SkipList

跳表（SkipList）是一种有序数据结构，它通过多级索引加速搜索、插入和删除。从最高层级开始向下查找，沿前向指针搜索，遇到大于目标的节点就移动到下移一层，
重复直到第 1 层，然后逐个比较找到目标；而当元素插入后，由 Redis 随机决定新节点的层数。

```c
typedef struct zskiplist {
    struct zskiplistNode *header, *tail; // 头尾节点
    unsigned long length; // 跳表长度
    int level; // 当前最大层数
} zskiplist;
```

```c
typedef struct zskiplistNode {
    sds ele; // 元素值
    double score; // 排序分数
    struct zskiplistNode *backward; // 后退指针，支持反向遍历
    struct zskiplistLevel {
        struct zskiplistNode *forward; // 每层前向指针
        unsigned int span; // 跨度，用于排名计算
    } level[]; // 多层索引数组
} zskiplistNode;
```

每个节点随机拥有不同层数索引，越高层数的节点越稀疏，查找过程自顶向下，快速缩小范围，最多可以拥有 32 级索引

```text
Level 3:  A ------------> D
Level 2:  A ----> B ----> D ----> E
Level 1:  A -> B -> C -> D -> E -> F
```

SkipList 的特点：

* 跳跃表是一个双向链表，每个节点都包含 score 和 ele 值
* 节点按照 score 值排序，score 值一样则按照 ele 字典排序
* 每个节点都可以包含多层指针，层数是 1 到 32 之间的随机数
* 不同层指针到下一个节点的跨度不同，层级越高，跨度越大
* 增删改查效率与红黑树基本一致，实现却更简单

****
### 7. RedisObject

Redis 中的任意数据类型的键和值都会被封装为一个 RedisObject，也叫做 Redis 对象。
对于 Redis 使用者而言，操作的核心是 database（数据库），它是一个逻辑上的键值对容器，非集群模式下默认有 16 个 database（编号 0~15），可通过 SELECT id 切换。
而集群模式下仅支持 1 个 database（编号 0），这是因为集群需要将数据分片到不同节点，多个数据库会增加分片复杂度。
key 固定为字符串类型（如 user:100、counter），且在单个数据库内唯一；但 value 支持多种数据类型（string、list、hash、set、sorted set 等），通过 SET、LPUSH、HSET 等命令操作不同类型的 value。
每个数据库就像一个 “大字典”，通过 Dict（字典）作为容器，sds（动态字符串）存储 key，robj（redisObject）封装 value 这三大核心结构维护 key-value 的映射关系。

Dict 就是存放映射关系的容器，每个数据库内部通过一个 Dict 结构体管理所有键值对，相当于哈希表，实现 key 到 value 的快速映射，而 Dict 的 key 以 sds 类型存储，
因为 value 支持多种类型，所以想要在 Dict 中统一存储就需要进行封装，可以用 robj（redisObject）结构体封装所有类型的 value。

```c
typedef struct redisObject {
    unsigned type : 4; // 数据类型
    unsigned encoding : 4; // 编码方式
    unsigned lru : 24; // LRU 时间戳或 LFU 计数
    int refcount; // 引用计数
    void *ptr; // 指向真实数据的指针
} robj;
```

type：

```c
#define OBJ_STRING  0
#define OBJ_LIST    1
#define OBJ_SET     2
#define OBJ_ZSET    3
#define OBJ_HASH    4
#define OBJ_MODULE  5
#define OBJ_STREAM  6
```

这些是 RedisObject 支持的主要逻辑类型，初始化时通过指定数字来选择需要封装的类型。

encoding：

| **编号** | **编码方式**            | **说明**               |
| -------- | ----------------------- | ---------------------- |
| 0        | OBJ_ENCODING_RAW        | raw编码动态字符串      |
| 1        | OBJ_ENCODING_INT        | long类型的整数的字符串 |
| 2        | OBJ_ENCODING_HT         | hash表（字典dict）     |
| 3        | OBJ_ENCODING_ZIPMAP     | 已废弃                 |
| 4        | OBJ_ENCODING_LINKEDLIST | 双端链表               |
| 5        | OBJ_ENCODING_ZIPLIST    | 压缩列表               |
| 6        | OBJ_ENCODING_INTSET     | 整数集合               |
| 7        | OBJ_ENCODING_SKIPLIST   | 跳表                   |
| 8        | OBJ_ENCODING_EMBSTR     | embstr的动态字符串     |
| 9        | OBJ_ENCODING_QUICKLIST  | 快速列表               |
| 10       | OBJ_ENCODING_STREAM     | Stream流               |


lru：

一种内存淘汰机制，默认是 LRU（最近最少使用）策略，记录访问时间戳；也可以选择 LFU（最不常用）策略，记录访问频率；占 24 位。

ptr，根据类型与编码方式不同，指向不同结构：

| type        | encoding                 | ptr 真实指向     |
| ----------- | ------------------------ | ------------ |
| OBJ\_STRING | OBJ\_ENCODING\_RAW       | sds 动态字符串    |
| OBJ\_STRING | OBJ\_ENCODING\_INT       | 存储整数值（指针强转）  |
| OBJ\_LIST   | OBJ\_ENCODING\_QUICKLIST | QuickList 结构 |
| OBJ\_SET    | OBJ\_ENCODING\_HT        | 字典结构         |
| OBJ\_SET    | OBJ\_ENCODING\_INTSET    | 整数集合         |
| OBJ\_ZSET   | OBJ\_ENCODING\_SKIPLIST  | 跳表结构         |
| OBJ\_ZSET   | OBJ\_ENCODING\_ZIPLIST   | 压缩列表         |
| OBJ\_HASH   | OBJ\_ENCODING\_HT        | 字典结构         |
| OBJ\_HASH   | OBJ\_ENCODING\_ZIPLIST   | 压缩列表         |

****
### 8. String 结构

String 是 Redis 中最常见的数据存储类型，它有三种编码方式，分别为 RAW、EMBSTR、INT。

RAW：

RAW 是基于简单动态字符串实现的，存储长字符串（长度 > 44 字节）或非整数的字符串（即使长度较短，但无法用 INT 编码时），存储上限为 512 MB，SDS 是一个独立的内存空间，由 ptr 指针指向。
但是如果⼀个 String 类型的 value 的值是数字，那么 Redis 内部会把它转成 long 类型来存储，从⽽减少内存的使用。如果该数字是整数，
且大小在 long 类型所能表示的最大整数范围内，则会直接将数据保存在 RedisObject 的 ptr 指针位置（刚好8字节），不再需要 SDS 了，但实际的操作仍然是针对 String 类型。

EMBSTR：

存储短字符串（长度 ≤ 44 字节），它和 RedisObject 一起存放在一片连续的内存空间中，以此减少内存碎片，并且 EMBSTR 编码的字符串是只读的，若执行修改操作（如 APPEND），会先转换为 RAW 编码。
因为该特性，分配或释放内存时，EMBSTR 只需 1 次系统调用，RAW 需要 2 次（分别为 redisObject 和 sds 分配内存）。

INT：

存储 64 位有符号整数（范围：-2^63 ~ 2^63-1），直接将整数值存储在 redisObject 的 ptr 字段中（无需额外分配内存存储字符串），节省空间，且无需进行字符串与整数的转换。

所以，String 在 Redis 中是⽤⼀个 robj 来表示的。用来表示 String 的 robj 可能编码成 3 种内部表⽰：OBJ_ENCODING_RAW，OBJ_ENCODING_EMBSTR，OBJ_ENCODING_INT。
其中前两种编码使⽤的是 sds 来存储，最后⼀种 OBJ_ENCODING_INT 编码直接把 string 存成了 long 型。

```redis
SET key "32"
OK
OBJECT ENCODING key
"int"
```

初始 "32" 可编码为 INT，SETBIT 修改 ASCII 码（针对字符串的二进制位操作），结果字符串变 "22"，并且编码切换为 RAW。所以在使用 SETBIT 的时候，Redis 将 "32" 先转换回了 String 类型，
然后基于 sds 字符串执行 SETBIT 操作

```redis
# 将 "32" 的第 0 字节的第 7 位比特位修改为相反的
SETBIT key 7 0
(integer) 1
GET key
"22"
OBJECT ENCODING key
"raw"
```

****
### 9. List

Redis 的 List 类型可以从首、尾操作列表中的元素

```redis
LPUSH list
RPUSH list
```

在 3.2 版本以前，Redis 采用 ZipList 和 LinkedList 来实现 List，当元素数量小于 512 并且元素大小小于 64 字节时采用 ZipList 编码，超过则采用 LinkedList 编码。
3.2 之后则统一采用 QuickList 来实现。

* LinkedList ：普通链表，可以从双端访问，内存占用较高，内存碎片较多
* ZipList ：压缩列表，可以从双端访问，内存占用低，存储上限低
* QuickList：LinkedList + ZipList，可以从双端访问，内存占用较低，包含多个 ZipList，存储上限高

list 中由 pushGenericCommand 统一封装两端插入逻辑，通过传入要操作的具体位置来实现头尾的双端操作：

```c
// 所有的操作最终会封装到 *c 中，然后从这个结构中获取对象
void pushGenericCommand(client *c, int where, int xx) {
    int j;
    // 判断元素大小，不能超过 LIST_MAX_ITEM_SIZELIST_MAX_ITEM_SIZE
    // 例如 LPUSH key v1 v2，LPUSH 就是 argv[0]
    for (j = 2; j < c->argc; j++) {
        // 从第二个位置开始获取 value 值
        if (sdslen(c->argv[j]->ptr) > LIST_MAX_ITEM_SIZE) {
            addReplyError(c, "Element too large");
            return;
        }
    }
    // 尝试找到 key 对应的 list
    robj *lobj = lookupKeyWrite(c->db, c->argv[1]); // 传入客户端要访问的数据库和 key
    if (checkType(c,lobj,OBJ_LIST)) return;
    // 判断 list 是否为空
    if (!lobj) {
        if (xx) { // xx 默认传入 false，所以一般会默认创建新的 QuickList
            addReply(c, shared.czero);
            return;
        }
        // 创建 QuickList，在这个方法里会把 list 的编码方式设置为 OBJ_ENCODING_QUICKLIST
        lobj = createQuicklistObject();
        quicklistSetOptions(lobj->ptr, server.list_max_ziplist_size,
                            server.list_compress_depth);
        dbAdd(c->db,c->argv[1],lobj);
    }
    // 插入数据
    for (j = 2; j < c->argc; j++) {
        listTypePush(lobj,c->argv[j],where);
        server.dirty++;
    }

    addReplyLongLong(c, listTypeLength(lobj));

    char *event = (where == LIST_HEAD) ? "lpush" : "rpush";
    signalModifiedKey(c,c->db,c->argv[1]);
    notifyKeyspaceEvent(NOTIFY_LIST,event,c->argv[1],c->db->id);
}
```

```redis
robj *createQuicklistObject(void) {
    quicklist *l = quicklistCreate(); // 创建 QuickList
    robj *o = createObject(OBJ_LIST,l); // 创建 RedisObject，type 为 OBJ_LIST，ptr 指向 QuickList
    o->encoding = OBJ_ENCODING_QUICKLIST; // 设置编码为 QuickList
    return o;
}
```

结构图：

```text
+------------------+------------------+------------------+------------------+------------------+
| *head            | *tail            | count            | fill factor      | compress         |
| (指向首节点)       | (指向尾节点)       | (总元素数量)      | (ZipList大小限制) | (首尾不压缩深度)    |
+------------------+------------------+------------------+------------------+------------------+
        |                   |
        v                   v
```

```text
+------------------+       +------------------+       +------------------+
| QuickListNode #1 | <---> | QuickListNode #2 | <---> | QuickListNode #3 |
+------------------+       +------------------+       +------------------+
| *prev            |       | *prev            |       | *prev            |
| *next            |       | *next            |       | *next            |
| *zl              |       | *zl              |       | *zl              |
| sz=16            |       | sz=24            |       | sz=8             |
| encoding=RAW     |       | encoding=RAW     |       | encoding=RAW     |
| container=ZIPLIST|       | container=ZIPLIST|       | container=ZIPLIST|
+------------------+       +------------------+       +------------------+
        |                         |                         |
        v                         v                         v
   +---------+               +---------+               +---------+
   | ZipList |               | ZipList |               | ZipList |
   +---------+               +---------+               +---------+
   | zlbytes |               | zlbytes |               | zlbytes |
   | zltail  |               | zltail  |               | zltail  |
   | zllen=3 |               | zllen=5 |               | zllen=2 |
   | Entry1  |               | Entry1  |               | Entry1  |
   | Entry2  |               | Entry2  |               | Entry2  |
   | Entry3  |               | ...     |               | zlend   |
   | zlend   |               | zlend   |               +---------+
   +---------+               +---------+
```

****
### 10. Set

Set 是 Redis 中的单列集合，满足下列特点：

* 不保证有序性
* 保证元素唯一
* 求交集、并集、差集

Set 内部提供了很多方法，它们底层都是要快速找到某个元素，所以 Set 对元素的查询效率要求很高，而 Redis 提供了两套方案：

- 使用哈希编码，用 Dict 的 key 用来存储元素，value 设置为 null，虽然保证了高效与不可重复，但内部具有较多指针，会占用较多内存
- 当使用的所有数据都是整数类型，并且元素数量不超过 set-max-intset-entries 时，Set 会采用 IntSet 编码，以节省内存

```c
robj *setTypeCreate(sds value) {
    // 判断 value 是否为数值类型
    if (isSdsRepresentableAsLongLong(value,NULL) == C_OK)
        // 如果是则采用 intset 编码方式
        return createIntsetObject();
    // 否则采用默认编码，也就是哈希编码
    return createSetObject();
}
```

```c
robj *createIntsetObject(void) {
    intset *is = intsetNew(); // 初始化 intset 并分配内存
    robj *o = createObject(OBJ_SET,is); // 创建 RedisObject 对象
    o->encoding = OBJ_ENCODING_INTSET; // 指定编码
    return o;
}
```

在每一次插入新元素时，底层会检查插入的元素是否为数值类型，只要不是就立即将 intset 编码转换成默认编码

```c
// subject 就是设置编码时返回的 RedisObject 对象
int setTypeAdd(robj *subject, sds value) { 
    long long llval;
    if (subject->encoding == OBJ_ENCODING_HT) { // 判断是否为默认编码，如果是则直接插入元素
        dict *ht = subject->ptr;
        dictEntry *de = dictAddRaw(ht,value,NULL);
        if (de) {
            dictSetKey(ht,de,sdsdup(value));
            dictSetVal(ht,de,NULL);
            return 1;
        }
    } else if (subject->encoding == OBJ_ENCODING_INTSET) { // 如果是 intset 编码
        if (isSdsRepresentableAsLongLong(value,&llval) == C_OK) { // 判断是否是整数类型
            uint8_t success = 0;
            subject->ptr = intsetAdd(subject->ptr,llval,&success);
            if (success) {
                // 当 intset 元素超出设置的 set_max_intset_entries 就转为默认编码
                size_t max_entries = server.set_max_intset_entries; 
                if (max_entries >= 1<<30) max_entries = 1<<30;
                if (intsetLen(subject->ptr) > max_entries)
                    setTypeConvert(subject,OBJ_ENCODING_HT);
                return 1;
            }
        } else {
            // 不是整数类型则设置为默认编码
            setTypeConvert(subject,OBJ_ENCODING_HT);
            // 通过哈希表的插入操作将元素插入 set 集合
            serverAssert(dictAdd(subject->ptr,sdsdup(value),NULL) == DICT_OK);
            return 1;
        }
    } else {
        serverPanic("Unknown set encoding");
    }
    return 0;
}
```

****
### 11. ZSet

ZSet 也就是 SortedSet，其中每一个元素都需要指定一个 score 值和 member 值：

* score 可重复，可以根据 score 来排序
* member 必须唯一
* 可以根据 member 查询 score

```redis
# m 是 member，数字是 score
ZADD z1 10 m1 20 m2 30 m3
```

因此，zset 底层数据结构必须满足键值存储、键必须唯一、可排序这几个需求。

* SkipList：按照升序排序，并且可以同时存储 score 和 ele 值（member）
* HT（Dict）：可以键值存储并保证 key 的唯一（代替 member），可以根据 key 找 value

所以 ZSet 的底层就是使用这两种结构，dict 提供快速查找（根据 member 查 score）；skiplist 提供有序访问（根据 score 查范围）

```c
typedef struct zset {
    dict *dict; // Dict 指针
    zskiplist *zsl; // SkipList 指针
}zset;
```

```c
robj *createZsetObject(void) {
    zset *zs = zmalloc(sizeof(*zs));
    robj *o;
    // 创建 Dict
    zs->dict = dictCreate(&zsetDictType,NULL);
    // 创建 SkipList
    zs->zsl = zslCreate();
    o = createObject(OBJ_ZSET,zs);
    o->encoding = OBJ_ENCODING_SKIPLIST;
    return o;
}
```

由此可知，ZSet 这种结构十分占用内存，因为它内部维护了两份数据，但当元素数量不多时，HT 和 SkipList 的优势不明显，却占用较多内存，所以 Redis 规定 ZSet 也可以采用 ZipList 结构来节省内存，不过需要同时满足两个条件：

* 元素数量小于 zset_max_ziplist_entries，默认值 128
* 每个元素都小于 zset_max_ziplist_value字节，默认值 64

ZipList 本身没有排序功能，而且没有键值对的概念，但可以通过模拟键值对的方式，让 element 在前，score 在后，只要获取到 element，它的下一个节点就是 score：

* ZipList 是连续内存，因此 score 和 element 是紧挨在一起的两个 entry， element 在前，score 在后
* score 越小越接近队首，score 越大越接近队尾，按照 score 值升序排列



```c
// zadd 添加元素时，先根据 key 找到 zset，不存在则创建新的 zset
zobj = lookupKeyWrite(c->db,key);
if (checkType(c,zobj,OBJ_ZSET)) goto cleanup;
// 判断是否存在
if (zobj == NULL) { // zset 不存在
    if (server.zset_max_ziplist_entries == 0 || server.zset_max_ziplist_value < sdslen(c->argv[scoreidx+1]->ptr)) { 
        // zset_max_ziplist_entries 设置为 0 就是禁用了 ZipList
        // 或者 value 大小超过了 zset_max_ziplist_value，则采用 HT + SkipList
        zobj = createZsetObject();
    } else {
        // 否则正常使用 ZipList
        zobj = createZsetZiplistObject();
    }
    dbAdd(c->db,key,zobj);
}
```

使用 ZipList 结构时，它的底层就不存在 ZSet 结构了，直接使用 ZipList 代替

```c
robj *createZsetZiplistObject(void) {
    // 创建 ZipList
    unsigned char *zl = ziplistNew();
    robj *o = createObject(OBJ_ZSET,zl);
    o->encoding = OBJ_ENCODING_ZIPLIST;
    return o;
}
```

```c
int zsetAdd(robj *zobj, double score, sds ele, int in_flags, int *out_flags, double *newscore) {
    if (zobj->encoding == OBJ_ENCODING_ZIPLIST) { // 判断编码方式
        unsigned char *eptr;
        // 在 ziplist 中 顺序遍历查找目标 ele，并返回它的指针位置，若存在，还会通过 score 返回当前分数
        if ((eptr = zzlFind(zobj->ptr,ele,&curscore)) != NULL) { // 是 ZipList 编码，证明可能有需要转换的风险
           ...
           return 1;
        } else if (!xx) {
            // 元素不存在，需要新增，然后根据 ZipList 长度有没有超、元素大小有没有超来判断是否需要转换编码
            if (zzlLength(zobj->ptr)+1 > server.zset_max_ziplist_entries ||
                sdslen(ele) > server.zset_max_ziplist_value ||
                !ziplistSafeToAdd(zobj->ptr, sdslen(ele))) {
                // 超出则转换成 SkipList 编码
                zsetConvert(zobj,OBJ_ENCODING_SKIPLIST);
            } else {
                zobj->ptr = zzlInsert(zobj->ptr,ele,score);
                if (newscore) *newscore = score;
                *out_flags |= ZADD_OUT_ADDED;
                return 1;
            }
        } else {
            *out_flags |= ZADD_OUT_NOP;
            return 1;
        }
    }
    // 本身就是 SkipList 编码则无需转化
    if (zobj->encoding == OBJ_ENCODING_SKIPLIST) {
        ...
    }
    ...
    return 0;
}
```

****
### 12. Hash

Hash 结构与 Redis 中的 ZSet 非常类似，都是键值存储、都需求根据键获取值、键必须唯一。但 ZSet 的键是 member，值是 score；而 hash 的键和值都是任意值，
并且 ZSet 需要根据 score 排序，hash 则不需要排序。所以 hash 的底层结构就不需要像 ZSet 那样使用到 SkipList，直接使用 ZipList 或 Dict 即可，若满足以下条件则使用 ZipList 实现。

* 元素数量小于 zset_max_ziplist_entries，默认值 512
* 每个元素都小于 zset_max_ziplist_value 字节，默认值 64

因为 ZipList 是一块连续的内存，每一次新增元素都可能触发扩容（连续内存不够），就需要申请一片新的内存空间，当 ZipList 的内容越多，拷贝的成本就越高，并且 Redis 是单线程的，
极端情况下可能造成阻塞。

```c
// 封装 Redis 命令的方法， 例如 hset user1 name jack age 22
void hsetCommand(client *c) {
    int i, created = 0;
    robj *o;
    // 判断 hash 的 key 是否存在，不存在则新建，默认采用 ZipList 结构
    if ((o = hashTypeLookupWriteOrCreate(c,c->argv[1])) == NULL) return;
    // 判断是否需要把 ZipList 转换成 Dict
    hashTypeTryConversion(o,c->argv,2,c->argc-1);
    // 循环遍历每一个 field 和 value 并执行 hset 命令
    for (i = 2; i < c->argc; i += 2)
        created += !hashTypeSet(o,c->argv[i]->ptr,c->argv[i+1]->ptr,HASH_SET_COPY);
    ...
}
```

```c
robj *hashTypeLookupWriteOrCreate(client *c, robj *key) {
    // 通过 key 查找对应的 RedisObject
    robj *o = lookupKeyWrite(c->db,key);
    // 判断是否为 hash 类型
    if (checkType(c,o,OBJ_HASH)) return NULL;
    // 判断 RedisObjct 是否为空，为空证明 hash 结构不存在，则创建 hash 结构
    if (o == NULL) {
        o = createHashObject();
        dbAdd(c->db,key,o);
    }
    return o;
}
```

```c
robj *createHashObject(void) {
    // 默认采用 ZipList 编码，所以要申请一个 ZipList 内存空间
    unsigned char *zl = ziplistNew();
    robj *o = createObject(OBJ_HASH, zl);
    // 设置 ZipList 编码
    o->encoding = OBJ_ENCODING_ZIPLIST;
    return o;
}
```

```c
// 接收 RedisBbject、命令参数数组，start 为 第一个 field 的下标，end 为最后一个 value 的下标
void hashTypeTryConversion(robj *o, robj **argv, int start, int end) {
    int i;
    size_t sum = 0;
    // 因为本方法是尝试把默认编码 ZipList 转换成 HT，所以如果本身就是 ZipList 编码，就无需进行
    if (o->encoding != OBJ_ENCODING_ZIPLIST) return;
    // 遍历 field 和 value
    for (i = start; i <= end; i++) {
        if (!sdsEncodedObject(argv[i]))
            continue;
        size_t len = sdslen(argv[i]->ptr);
        判断 field 或 value 的下标是否超过 hash_max_ziplist_value 字节数
        if (len > server.hash_max_ziplist_value) {
            hashTypeConvert(o, OBJ_ENCODING_HT);
            return;
        }
        sum += len;
    }
    // 判断该 hash 结构总的大小是否超出标准
    if (!ziplistSafeToAdd(o->ptr, sum))
        hashTypeConvert(o, OBJ_ENCODING_HT);
}
```

以下就是 hash 的主要插入逻辑：

```c
int hashTypeSet(robj *o, sds field, sds value, int flags) {
    int update = 0;
    // 判断是否为 ZipList 编码
    if (o->encoding == OBJ_ENCODING_ZIPLIST) {
        unsigned char *zl, *fptr, *vptr;
        zl = o->ptr;
        // 查询 ZipList 的第一个指针的位置
        fptr = ziplistIndex(zl, ZIPLIST_HEAD);
        if (fptr != NULL) { // head 不为空，说明 ZipList 不为空，那么就开始查找 key
            fptr = ziplistFind(zl, fptr, (unsigned char*)field, sdslen(field), 1);
            if (fptr != NULL) { // 判断是否存在，存在则更新
                update = 1;
                zl = ziplistReplace(zl, vptr, (unsigned char*)value,
                        sdslen(value));
            }
        }
        // 不存在，直接 push
        if (!update) { // 依次 push 新的 field 和 value 到 ZipList 尾部
            zl = ziplistPush(zl, (unsigned char*)field, sdslen(field),
                    ZIPLIST_TAIL);
            zl = ziplistPush(zl, (unsigned char*)value, sdslen(value),
                    ZIPLIST_TAIL);
        }
        o->ptr = zl;
        // 插入新元素后判断是否符合标砖，否则转成 HT 编码
        if (hashTypeLength(o) > server.hash_max_ziplist_entries)
            hashTypeConvert(o, OBJ_ENCODING_HT);
    } else if (o->encoding == OBJ_ENCODING_HT) {
        // 如果是 HT 编码，则直接进行插入或覆盖操作
        ...
    } else {
        serverPanic("Unknown hash encoding");
    }
    ...
    return update;
}
```

****
## 六. Redis 网络模型

### 1. 用户空间和内核态空间

常见的 Ubuntu、CentOS 等操作系统，其实都是 Linux 发行版，发行版 = Linux 内核 + 常用工具集（Shell、包管理器、图形界面等），无论是 Ubuntu 还是 CentOS，
它们都属于 Linux 内核。而用户的应用，比如 redis，mysql 等其实是没有办法去直接访问操作系统的硬件的，所以可以通过发行版的这个壳子去访问内核，再通过内核去访问计算机硬件。
计算机硬件包括 cpu，内存，网卡等等，内核（通过寻址空间）可以操作硬件的，但是内核需要不同设备的驱动，有了这些驱动之后，内核就可以去对计算机硬件去进行内存管理，文件系统的管理，进程的管理等等。

如果想要通过用户应用来访问内核，计算机就必须要通过对外暴露的一些接口才能访问到，从而简便的实现对内核的操控，但是内核本身上来说也是一个应用，
所以他本身也需要一些内存、cpu 等设备资源，而用户应用本身也在消耗这些资源，如果不加任何限制，用户随意的去操作这些资源，就有可能导致一些冲突，甚至有可能导致系统出现无法运行的问题，
因此我们需要把用户和内核隔离开。

寻址空间是指一个进程或系统可以访问的虚拟地址范围，它是操作系统为程序划分的一块连续的内存地址空间，进程的寻址空间划分成两部分：内核空间、用户空间。
不管是应用程序，还是内核空间，都是没有办法直接访问物理内存的，只能通过分配一些虚拟内存映射到物理内存中，所以内核和应用程序去访问虚拟内存的时候，
就需要这个虚拟地址，这个地址是一个无符号的整数，比如一个 32 位的操作系统，它的带宽就是 32，它的虚拟地址就是 2^32，也就是说他寻址的范围就是 0 ~ 2^32，
这片寻址空间对应的就是 2^32 个字节，就是 4GB，其中会有 3GB 分给用户空间，1GB 给内核系统

在 linux 中，它们权限分成两个等级，0 和 3，用户空间只能执行受限的命令（Ring 3），而且不能直接调用系统资源，必须通过内核提供的接口来访问；
内核空间可以执行特权命令（Ring 0），调用一切系统资源，所以一般情况下，用户的操作是运行在用户空间，而内核运行的数据是在内核空间的，
但有些情况需要一个应用程序去调用一些特权资源，去调用一些内核空间的操作，所以此时它们需要在用户态和内核态之间进行切换。

比如 Linux 系统为了提高 IO 效率，会在用户空间和内核空间都加入缓冲区，写数据时，要把用户缓冲数据拷贝到内核缓冲区，然后写入设备；
读数据时，要从设备读取数据到内核缓冲区，然后再拷贝到用户缓冲区。
针对这个操作，用户在写读数据时，会去向内核态申请读取内核的数据，而内核数据要去等待驱动程序从硬件上读取数据，当从磁盘上加载到数据之后，
内核会将数据写入到内核的缓冲区中，然后再将数据拷贝到用户态的缓冲区中，然后再返回给应用程序，整体而言，速度较慢。
但实际情况下，通常希望 read/wait 等操作尽可能不阻塞，或者等待时间尽可能短。

```text
┌────────────────────────────┐
│       应用程序（用户态）       │
│  调用 read()                │
└────────────┬───────────────┘
             │ 系统调用切换
             ▼
┌────────────────────────────┐
│         Linux 内核（内核态）  │
│  检查文件描述符               │
│  如果无数据，阻塞等待 I/O      │
│  有数据 → 写入内核缓冲区       │
└────────────┬───────────────┘
             │ 拷贝数据
             ▼
┌────────────────────────────┐
│ 用户缓冲区 ← 内核缓冲区        │
│ 返回 read() 调用结果         │
└────────────────────────────┘
```

****
### 2. 阻塞 IO

应用程序想要去读取数据，它是无法直接去读取磁盘数据的，它需要先到内核里边去等待内核操作硬件拿到数据，等到内核从磁盘上把数据加载出来之后，
再把这个数据写给用户的缓存区，而用户读取数据时，会去先发起 recvform 命令，尝试从内核上加载数据，如果内核没有数据，那么用户就会等待，此时内核会去从硬件上读取数据，
内核读取数据之后，会把数据拷贝到用户态，并且返回 ok，整个过程，都是阻塞等待的，这就是阻塞 IO

阶段一：

- 用户进程尝试读取数据（比如网卡数据）
- 此时数据尚未到达，内核需要等待数据
- 此时用户进程也处于阻塞状态

阶段二：

* 数据到达并拷贝到内核缓冲区，代表已就绪
* 将内核数据拷贝到用户缓冲区
* 拷贝过程中，用户进程依然阻塞等待
* 拷贝完成，用户进程解除阻塞，处理数据

用户进程在以上两个阶段都是处于阻塞状态。

****
### 3. 非阻塞 IO

顾名思义，非阻塞 IO 的 recvfrom 操作会立即返回结果而不是阻塞用户进程。

阶段一：

* 用户进程尝试读取数据（比如网卡数据）
* 此时数据尚未到达，内核需要等待数据
* 返回异常给用户进程
* 用户进程拿到 error 后，再次尝试读取
* 循环往复，直到数据就绪

阶段二：

* 将内核数据拷贝到用户缓冲区
* 拷贝过程中，用户进程依然阻塞等待
* 拷贝完成，用户进程解除阻塞，处理数据

非阻塞 IO 模型中，用户进程在第一个阶段是非阻塞，第二个阶段是阻塞状态。虽然是非阻塞 IO，但性能并没有得到提高，而且忙等机制会导致 CPU 空转，CPU 使用率暴增。

****
### 4. IO 多路复用

#### 4.1 定义

无论是阻塞 IO 还是非阻塞 IO，用户应用在一阶段都需要调用 recvfrom 来获取数据，差别在于无数据时的处理方案：

- 如果调用 recvfrom 时，恰好没有数据，阻塞 IO 会使 CPU 阻塞，非阻塞 IO 使 CPU 空转，都不能充分发挥 CPU 的作用。
- 如果调用 recvfrom 时，恰好有数据，则用户进程可以直接进入第二阶段，读取并处理数据

而在单线程情况下，只能依次处理 IO 事件，如果正在处理的 IO 事件恰好未就绪（数据不可读或不可写），线程就会被阻塞，所有 IO 事件都必须等待，性能自然会很差。
就比如服务员给顾客点餐，分两步：1、顾客思考要吃什么（等待数据就绪）2、顾客想好了，开始点餐（读取数据）。而提高效率的方法就是增加服务员（多线程）、
不排队，谁想好了吃什么（数据就绪了），服务员就给谁点餐（用户应用就去读取数据）。而 IO 多路复用就是基于第二种情况，当数据准备好后才开始运行。

文件描述符（File Descriptor）：简称 FD，是一个从 0 开始的无符号整数，用来关联 Linux 中的一个文件。在 Linux 中，一切皆文件，
例如常规文件、视频、硬件设备等，当然也包括网络套接字（Socket）。通过 FD，网络模型可以利用一个线程监听多个 FD，并在某个 FD 可读、可写时得到通知，
从而避免无效的等待，充分利用 CPU 资源。

当用户去读取数据的时候，不再去直接调用 recvfrom 了，而是调用 select 的函数，select 函数会将需要监听的数据交给内核，由内核去检查这些数据是否就绪了，
如果说这个数据就绪了，就会通知应用程序该数据就绪，然后来读取数据，再从内核中把数据拷贝给用户态，完成数据处理，如果 N 多个 FD 一个都没处理完，此时就进行等待。
用 IO 多路复用模式，可以确保去读数据的时候，数据是一定存在的，它的效率比原来的阻塞 IO 和非阻塞 IO 性能都要高。

阶段一：

* 用户进程调用 select，指定要监听的 FD 集合
* 核监听 FD 对应的多个 socket
* 任意一个或多个 socket 数据就绪则返回 readable
* 此过程中用户进程阻塞

阶段二：

* 用户进程找到就绪的 socket
* 依次调用 recvfrom 读取数据
* 内核将数据拷贝到用户空间
* 用户进程处理数据

IO 多路复用是利用单个线程来同时监听多个 FD，并在某个 FD 可读、可写时得到通知。不过监听 FD 的方式、通知的方式又有多种实现，常见的有：

- select
- poll
- epoll

其中 select 和 pool 相当于是当被监听的数据准备好之后，它会把监听的 FD 整个数据都发给用户端，就需要到整个 FD 中去找，
哪些是处理好了的，需要通过遍历的方式，所以性能也并不是那么好；而 epoll 则相当于内核准备好了之后，它会把准备好的数据，直接发给用户端，可以省去遍历查询的动作。

****
####  4.2 select 方式

select 是 Linux 最早使用的 IO 多路复用技术。把需要处理的数据封装成 FD，然后在用户态时创建一个 fd 的集合（这个集合的大小是要监听的那个 FD 的最大值 +1），
这个集合的长度大小是有限制的，同时在这个集合中，标明出来需要控制哪些数据，比如要监听的数据是 1、2、5 三个数据，此时执行 select 函数，然后将整个 fd 发给内核态，
内核态会去遍历用户态传递过来的数据，如果发现这里边都数据都没有就绪，就休眠，直到有数据准备好时，就会被唤醒，唤醒之后，再次遍历一遍，看看谁准备好了，
然后再处理掉没有准备好的数据，最后再将这个 FD 集合写回到用户态中去，此时用户态就知道了有数据已经准备好了，但是对于用户态而言，并不知道谁处理好了，
所以用户态也需要去进行遍历，然后找到对应准备好数据的节点，再去发起读请求。这种模式虽然比阻塞 IO 和非阻塞 IO 好，但是依然效率不高，比如说频繁的传递 fd 集合，
频繁的去遍历 FD 等，并且限制了监听的 fd 数量，1024 在目前来看是不够用的。

```c
// 定义类型别名 __fd_mask，本质是 long int 类型
typedef long int __fd_mask;
```

fd_set 结构内部是一个 fds_bits 数组，这个数组指定了大小，__FD_SETSIZE 为 1024，__NFDBITS 为 32，所以数组大小为 32，而该数组又是一个 __fd_mask 类型，
实际上就是一个 long 类型，占 4 字节（32 bit），所以 fds_bits 数组的元素个数为 32 bit，而每一个元素的长度又为 32 bit，所以这个数组整体长度为 1024 bit，
而保存 fd 时是使用比特位来保存的，所以将来可以保存 1024 个 fd，0 代表未就绪，1 代表就绪，初始全部为 0。

```c
// fd_set 记录要监听的 fd 集合以及其对应的状态
typedef struct {
    // fds_bits 是 long 类型数组，长度为 1024 / 32 = 32
    // 共 1024 个 bit 位，每个 bit 位代表一个 fd，0 代表未就绪，1 代表就绪
    __fd_mask fds_bits[__FD_SETSIZE / __NFDBITS];
    ...
} fd_set;
```

```c
// select 函数，用于监听多个 fd 集合
int select (
    int nfds, // 要监听的 fd_set 集合的最大 fd + 1
    fd_set *readfds, // 要监听的读事件的 fd 集合
    fd_set *writefds, // 要监听的写事件的 fd 集合
    fd_set *execeptfds, // 要监听的异常事件的 fd 集合
    struct timeval *timeout // 超时时间，null 为永不超时；0 为不阻塞等待；大于 0 则为固定等待时间（秒）
);
```

```text
用户空间：

1.1 创建 fd_set rfds 集合，fds_bits 为 00000000
1.2 监听 1，2，5，此时 fds_bits 数组变为 00010011，从右往左数，1 代表第一位，2 代表第二位。
1.3 执行 select(5 + 1, rfds, null, null, 3)

3.1 遍历 fds_bits，找到就绪的 fd，读取数据 
```

```text
内核空间：当用户空间执行 select 的那一刻，就把 fds_bits 传递过来：00010011

2.1 遍历 fds_bits
2.2 没有发现已就绪的 fd，休眠（假设没有）
2.3 等待数据就绪被唤醒或超时
2.4 此时 fd = 1 数据就绪，遍历 fds_bits，找到被标记监听的 bit 位和 fd = 1 作比较，已就绪的就保留，未就绪的就删除，此时 fds_bits 为 00000001
2.5 将新 fds_bits 拷贝回用户空间，覆盖旧的 fds_bits
```

****
#### 4.3 poll 方式

poll 方式对 select 方式做了简单改进，但性能提升不明显。

```c
// pollfd 中的事件类型
#define POLLIN // 可读事件
#define POLLOUT // 可写事件
#define POLLERR // 错误事件
#define POLLNVAL // fd 未打开

// pollfd 结构
struct pollfd {
    int fd; // 监听的 fd
    short int events; // 监听的事件类型
    short int revents; // 实际发生的事件类型
}

//poll 函数
int poll (
    struct polled *fds, // pollfd 数组，可自定义大小
    nfds_t nfds, // 数组元素个数
    int timeout // 超时时间
);
```

IO流程：

* 创建 pollfd 数组，向其中添加关注的 fd 信息，数组大小自定义，理论上无上限
* 调用 poll 函数，将 pollfd 数组拷贝到内核空间，转链表存储，无上限
* 内核遍历fd，判断是否就绪
* 数据就绪或超时后，拷贝 pollfd 数组到用户空间，返回就绪 fd 具体数量 n
* 用户进程判断n是否大于 0，大于 0 则遍历 pollfd 数组，找到就绪的 fd

与 select 对比：

* select 模式中的 fd_set 大小固定为 1024，而 pollfd 在内核中采用链表，理论上无上限
* 监听 FD 越多，每次遍历消耗时间也越久，性能反而会下降

****
#### 4.4 epoll 方式

epoll 是对 select 和 poll 的改进，它提供了三个函数：

```c
struct eventpoll {
    // ...
    struct rb_root rbr; // 一个红黑树，记录要监听的 fd
    struct list_head rdlist; // 一个链表，记录就绪的 fd
    // ...
}

// 1. 会在内核创建 eventpoll 结构体，返回对应的句柄 epfd，这个可以看作是 eventpoll 的唯一标识，每个 epfd 指向唯一的一个 eventpoll
int epoll_create(int size);

// 2. 将一个 fd 添加到 epoll 的红黑树中，并设置 ep_poll_callback
// callback 触发时，就把对应的 fd 加入到 relist 就绪链表中
int epoll_ctl(
    int epfd, // epoll 实例的句柄，唯一标识
    int op, // 要执行的操作，包括 ADD、MOD、DEL
    int fd, // 要监听的 fd
    struct epoll_event *event // 要监听的事件类型：读、写、异常等
);  

// 3. 检查 rdlist 列表是否为空，不为空则返回就绪的 fd 数量
int epoll_wait (
    int epfd, 
    struct epoll_event *events, // 空 event 数组，用于接收就绪的 fd
    int maxevents, // events 数组的最大长度
    itn timeout // 超时时间，-1 不超时、0 不阻塞、大于 0 为阻塞时间
);
```

```text
用户空间：

1. epoll_create(1) 创建 epoll 实例
2. epoll_ctl() 添加要监听的 fd，关联 callback 函数，一旦 fd 准备就绪旧触发 callback，将就绪的 fd 放到 rdlist 就绪链表中
3. epoll_wait() 等待 fd 就绪，当初发 callback 时返回 fd 的就绪个数，并将 rdlist 中的数据拷贝到 event 数组中
4. 接收到就绪个数后，从 event 数组中获取 fd
```

select 模式存在的三个问题：

* 能监听的 FD 最大不超过 1024
* 每次 select 都需要把所有要监听的 FD 都拷贝到内核空间
* 每次都要遍历所有 FD 来判断就绪状态

poll 模式的问题：

* poll 利用链表解决了 select 中监听 FD 上限的问题，但依然要遍历所有 FD，如果监听较多，性能会下降

epoll 模式中如何解决这些问题的？

* 基于 epoll 实例中的红黑树保存要监听的 FD，理论上无上限，而且增删改查效率都非常高
* 每个 FD 只需要执行一次 epoll_ctl 添加到红黑树，以后每次 epol_wait 无需传递任何参数，无需重复拷贝 FD 到内核空间
* 利用 ep_poll_callback 机制来监听 FD 状态，无需遍历所有 FD，因此性能不会随监听的 FD 数量增多而下降

****
#### 4.5 epoll 中的 ET 和 LT 

当 FD 有数据可读时，调用 epoll_wait（或者 select、poll）可以得到通知，但是事件通知的模式有两种：

* LevelTriggered：简称 LT，也叫做水平触发。只要某个 FD 中有数据可读，每次调用 epoll_wait 都会得到通知。

```text
客户端发送 2KB 数据 →
服务端 epoll_wait() → 返回 FD 可读
→ 读取 1KB 数据
→ 再次 epoll_wait()，FD 仍然返回，因为还有 1KB 没读完
```

* EdgeTriggered：简称 ET，也叫做边沿触发。只有在某个 FD 有状态变化时，调用 epoll_wait 才会被通知。

```text
客户端发送 2KB 数据 →
服务端 epoll_wait() → 返回 FD 可读（状态变化）
→ 读取 1KB 数据（未读完）
→ 再次 epoll_wait()，FD 不再返回（因为没有状态变化）
→ 剩下的 1KB 数据“看不见了”
```

虽然 ET 模式只在数据第一次到达时通知一次，但是可以通过循环读取数据的方式获取所有的 FD，但需要注意的是，不能使用阻塞 IO 的方式，因为阻塞 IO 的特性是读不到数据了就阻塞等待，
如果在循环中这样等待，就会造成死循环。

* 假设一个客户端 socket 对应的 FD 已经注册到了 epoll 实例中
* 客户端 socket 发送了 2kb 的数据
* 服务端循环调用 epoll_wait，得到通知说 FD 就绪
* 服务端从 FD 读取了 1kb 数据回到步骤3（再次调用 epoll_wait，形成循环）
* 处理完后再返回读取完毕的信息

整体流程：

```text
用户空间：

1.1 服务端调用 epoll_create 创建实例
1.2 创建 serverSocket 得到 FD，记作 ssfd
1.3 调用 epoll_ctl 监听 FD
1.5 调用 epoll_wait 等待 FD 就绪
1.7 有 FD 就绪后，判断事件类型
1.9 接收客户端发送的 socke，得到对应的 FD
```

```text
内核空间：

1.4 通过 epoll_ctl 监听到的 FD 会放入创建实例时创建的一个红黑树结构中 rb_root
1.6 当发现 FD 就绪时，就会把 rb_root 中就绪的 FD 放进就绪链表 rd_list 中
1.8 如果判断出的事件类型为可读类型，就把 rd_list 中的数据拷贝到 event 数组中
```

****
### 5. 信号驱动 IO

当内核检测到某个文件描述符（FD）上发生指定事件时，它会主动给用户进程发送一个 SIGIO 信号来通知，而不是让用户程序主动去调用 select、poll、epoll 等阻塞等待。

阶段一：

* 用户进程调用 sigaction，注册信号处理函数
* 内核返回成功，开始监听 FD
* 用户进程不阻塞等待，可以执行其它业务
* 当内核数据就绪后，回调用户进程的 SIGIO 处理函数

阶段二：

* 收到 SIGIO 回调信号
* 调用 recvfrom，开始阻塞读取
* 内核将数据拷贝到用户空间
* 用户进程处理数据

当有大量 IO 操作时，信号较多，SIGIO 处理函数不能及时处理可能导致信号队列溢出，而且内核空间与用户空间的频繁信号交互性能也较低。

****
### 6. 异步 IO

这种方式不仅仅是用户态在试图读取数据后不阻塞，而且当内核的数据准备完成后，也不会阻塞，它会由内核将所有数据处理完成后，由内核将数据写入到用户态中，
然后才算完成，所以性能极高，不会有任何阻塞，全部都由内核完成。异步 IO 模型中，用户进程在两个阶段中都是非阻塞状态。

****
### 7. Redis 单线程

#### 7.1 Redis 是单线程还是多线程

如果仅仅聊 Redis 的核心业务部分（命令处理），那就是单线程；如果是聊整个 Redis，那么就是多线程。在 Redis 版本迭代过程中，在两个重要的时间节点上引入了多线程的支持：

* Redis v4.0：引入多线程异步处理一些耗时较旧的任务，例如异步删除命令 unlink，将删除对象从主线程中剥离，用后台线程异步释放内存；引入 AOF 重写子进程后台完成，但主线程仍处理命令
* Redis v6.0：在核心网络模型中引入 IO 多线程，进一步提高对于多核 CPU 的利用率

因此，对于 Redis 的核心网络模型，在 Redis 6.0 之前确实都是单线程。是利用 epoll（Linux 系统）这样的 IO 多路复用技术在事件循环中不断处理客户端情况。

为什么 Redis 要选择单线程？

* 抛开持久化不谈，Redis 是纯内存操作，执行速度非常快，它的性能瓶颈是网络延迟而不是执行速度，因此多线程并不会带来巨大的性能提升
* 多线程会导致过多的上下文切换，带来不必要的开销
* 引入多线程会面临线程安全问题，必然要引入线程锁这样的安全手段，实现复杂度增高，而且性能也会大打折扣

****
#### 7.2 Redis 单线程和多线程网络模型变更

Redis 启动后开始初始化网络部分，

```c
int main (
    int argc,
    char **argv
) {
    // ... 
    // 初始化服务
    initServer();
    // ...
    // 开始监听事件循环
    aeMain(server.el);
    // ...
}
```

通过 aeCreateEventLoop 注册创建事件循环体结构，初始化底层的 IO 多路复用技术，内部会创建 epoll 对象，然后创建服务器 socket 并绑定监听端口，
并注册 accept 事件处理器，监听 socket，通过 createSocketAcceptHandler 处理服务端的 serverSocket 上发生的事件。所以这一步是在进行准备工作，还没进行相关的处理。

```c
void initServer(void) {
    // ...
    // 内部会调用 aeApiCreate(eventLoop)，类似 epoll_create
    server.el = aeCreateEventLoop(server.maxclients+CONFIG_FDSET_INCR);
    // ...
    // 监听 TCP 端口，创建 ServerSocket 并得到 FD，传入端口和 IP 地址，默认为本地 IP 127.0.0.1
    listenToPort(server.port, &server.ipfd);
    // ...
    // 注册连接处理器，内部会调用 aeApiAddEvent(&server.ipfd) 监听 FD（将就绪链表中的 FD 添加到 event 数组中）
    // acceptTcpHandler 用来处理 serverSocket 中的读事件 
    createSocketAcceptHandler(&server.ipfd, acceptTcpHandler)
    // 注册 ae_api_poll 前的处理器，在调用 epoll_wait 之前做一些准备工作
    aeSetBeforeSleepProc(server.el, beforeSleep);
}
```

相关服务完成初始化后，再开始执行循环监听事件，也就是等待 FD 就绪。

```c
void aeMain(aeEventLoop * eventLoop) {
    eventLoop -> stop = 0;
    // 循环监听事件
    while (!eventLoop -> stop) {
        aeProcessEvents (
            eventtLoop,
            AE_ALLEVENTS |
                AE_CALL_BEFORE_SLEEP | 
                AE_CALL_AFTER_SLEEP
        );
    }
} 
```

aeMain 内部循环调用 aeProcessEvents 来处理事件，该方法里面又调用了 aeApiPoll（类似于 epoll_wait），也就是在这体现了等待 FD 的过程

```c
int aeProcessEvents (aeEventLoop *eventLoop, int flags) {
    // ... 调用前置处理器 beforeSleep
    eventLoop -> beforesleep(eventLoop);
    // 等待 FD 就绪，类似 epoll_wait
    numevents = aeApiPoll (eventLoop, tvp);
    for (j = 0; j < numevents; j++) {
        // 遍历处理就绪的 FD，调用对应事件的处理器
    }
}
```

在初始化的过程中会封装一个数据处理器监听客户端的 socket，而这个里面又会封装一个专门读事件的处理器，读取客户端的 socket 发送来的请求并处理。
所以当客户端连接到服务端时，就会不断触发 serversocket 的读事件，然后就会调用 acceptTcpHandler 处理器，然后再通过这个处理器里面封装的 readQueryFromClient 处理器接收客户端的请求并处理，
然后把客户端的 FD 注册到 aeCreateEventLoop 中进行 IO 多路复用

```c
// 数据处理器
void acceptTcpHandler(...) {
    // ...
    // 接收客户端 socket 连接，获取 FD
    fd = accept(s, sa, len);
    // ...
    // 创建 connection，关联 FD
    connection *conn = connCreateSocket();
    conn.fd = fd;
    // ...
    // 内部调用 aeApiAddEvent(fd, READABLE)
    // 监听 socket 的 FD 读事件，并绑定读处理器 readQueryFromClient，处理客户端的读事件
    connSetReadHandler(conn, readQueryFromClient);
}
```

readQueryFromClient 处理器负责将客服端发送的请求放到客户端的缓冲区，然后通过 processCommand 将解析这些请求为 Redis 命令，并获取它们的返回值，
最后把返回结果写进客户端缓冲区，根据 buf 中是否够用选择是否使用 reply 链表存储。接着就是把这些存储了命令信息的客户端信息放进一个 clients_pending_wirte 队列，
所以此过程并没有写出响应。

```c
void readQueryFromClient(connection *conn) {
    // 获取当前客户端，客户端中有缓存区用来读和写
    clietn *c = connGetPrivateData(conn);
    // 获取 c -> querybuf 缓冲区大小
    long int qblen = sdslen(c -> querybuf);
    // 读取请求数据到 c -> querybuf 缓冲区
    connRead(c -> conn, c -> querybuf+qblen, readlen);
    // ...
    // 解析缓冲区字符串，转为 Redis 命令参数存入 c -> argv 数组
    processInputBuffer(c);
    // ...
    // 处理 c -> argv 中的命令
    processCommand(c);
}

int processCommand(clietn *c) {
    // ...
    // 根据命名名称寻找命令对应的 command，例如 setCommand
    c -> cmd = c -> lastcmd = lookupCommand(c -> argv[0] -> ptr);
    // ...
    // 执行 command，得到响应结果，例如 ping 命令，对应 pingCommand
    c -> cmd -> proc(c);
    // 把执行结果写出，例如 ping 命令，就返回 pong 给 client；SET 命令就返回 0 或 1
    // shared.pong 是字符串 “pong” 的 SDS 对象
    addReply(c, shared.pong);
}

void addReply(clietn *c, robj *robj) {
    // 尝试把结果写到 c -> buf 客户端缓冲区
    if (_addReplyToBuffer(c, obj -> ptr, sdslen(obj -> prtr)) != C_OK)
        // 如果 c -> buf 写不下，则写到 c -> reply，这是一个链表，容量无上限
        _addReplyProtoToList(c, obj -> ptr, sdslen(obj -> ptr));
    // 将客户端添加到 server.clients_pending_write 队列，等待被写出
    listAddNodeHead(server.clients_pending_wirte,c);
}
```

在每次事件循环前会调用 beforeSleep 为需要写出响应的客户端注册写事件处理器，只有当 socket 可写时，即 clients_pending_wirte 队列中有数据时，
才会使用 sendReplyToClient 写处理器帮助写出响应信息，返回给客户端

```c
void beforeSleep(struct aeEventLoop *eventLoop) {
    // ...
    // 定义迭代器，指向 server.clients_pending_write -> head
    listIter li;
    li -> next = server.clients_pending_write -> head;
    li -> direction = AL_START_HEAD;
    // 循环遍历待写出的 client
    while ((len = listNext(&li))) {
        // 内部调用 aeApiAddEvent(fd, WRITEABLE) 监听 socket 写事件
        // 并且绑定写处理器 sendReplyToClient，可以把响应写到客户端 socket
        connSetWriteHandlerWithBarrier(c -> conn, sendReplyToClient, ae_barrier);
    }
}
```

在老版本的 Redis 单线程模型中，客户端与服务端的连接是单线程的，也就是说，Redis 的效率是收网络带宽影响的，再引入多线程后，
给读与写操作开启了多线程模式，让多个线程并行的解析请求终的数据，但真正执行命令依然是单线程模型，而写出响应时也是让多个线程并行响应，减少因为网络 IO 造成的影响。

****
### 8. Redis 通信协议

#### 8.1 RESP 格式

RESP 是 Redis 自定义的序列化协议，用于客户端与服务端之间的数据交换。它最早引入于 Redis 1.2 版本，从 Redis 2.0 开始成为默认协议（RESP2），
Redis 6.0 引入 RESP3，但默认仍使用 RESP2。而 RESP 的设计核心是：每种数据类型使用不同的首字节进行标识（数据以 \r\n (CRLF) 作为结束标志）。

1、单行字符串

格式：+<string>\r\n。首字节使用 '+'，后面跟上单行字符串，并以 CRLF（ "\r\n" ）结尾。例如：+OK\r\n 表示字符串 "OK"；+hello world\r\n 表示 "hello world"。

2、错误信息

格式：-<error message>\r\n。首字节使用 '-'，与单行字符串格式一样，只是字符串是异常信息。例如：-WRONGTYPE Operation against a key holding the wrong kind of value\r\n。

3、整数

格式：:<number>\r\n。首字节使用 ':'，后面跟上数字格式的字符串，并以 CRLF（ "\r\n" ）结尾。例如：:1000\r\n 表示整数 1000。

4、多行字符串

格式：$<length>\r\n<bytes>\r\n。首字节使用 '$'，表示二进制安全的字符串，最大支持 512MB，使用数字表示有多少字节的字符串，例如：

- $-1\r\n 表示 NULL 值
- $0\r\n\r\n 表示空字符串
- $11\r\nhello\r\nworld\r\n 表示 "hello\r\nworld"
- $4\r\nball\r\n 表示 "ball"

5、数组

格式：*<number of elements>\r\n<element 1>...<element N>。首字节使用 '*'，后面跟上数组元素个数，再跟上元素，元素数据类型不限:

- *-1\r\n 表示 NULL 数组
- *0\r\n 表示空数组
- *3\r\n:1\r\n:2\r\n:3\r\n 表示数组 [1, 2, 3]
- *5\r\n:1\r\n:2\r\n:3\r\n:4\r\n$6\r\nfoobar\r\n 表示混合类型数组，[1, 2, 3, 4, " foobar"]

```redis
SET mykey "Hello World"
*3\r\n$3\r\nSET\r\n$5\r\nmykey\r\n$11\r\nHello World\r\n

# 成功响应
+OK\r\n
# 错误响应
-ERR value is not an integer or out of range\r\n
# 获取字符串
$11\r\nHello World\r\n
# 获取数组
*2\r\n$5\r\nhello\r\n$5\r\nworld\r\n
```

****
#### 8.2 基于 Socket 自定义 Redis 的客户端 

Redis 支持 TCP 通信，因此可以使用 Socket 来模拟客户端，与 Redis 服务端建立连接。

```java
public class Main {
    static Socket s;
    static PrintWriter writer;
    static BufferedReader reader;

    public static void main(String[] args) {
        try {
            // 1. 建立连接
            String host = "172.23.14.3";
            int port = 6379;
            s = new Socket(host, port);
            // 2. 从 socket 中获取输出流、输入流
            writer = new PrintWriter(new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8));
            reader = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));

            // 3. 发出请求
            // 获取授权 auth 123
            sendRequest("auth", "123");
            Object obj = handleResponse();
            System.out.println("obj = " + obj);

            // set name 张三
            sendRequest("set", "name", "张三");
            // 4.解析响应
            obj = handleResponse();
            System.out.println("obj = " + obj);

            // get name
            sendRequest("get", "name");
            obj = handleResponse();
            System.out.println("obj = " + obj);

            sendRequest("mset", "name", "李四", "age", "20", "address", "江西");
            obj = handleResponse();
            System.out.println("obj = " + obj);
            
            sendRequest("mget", "name", "age", "address");
            obj = handleResponse();
            System.out.println("obj = " + obj);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // 5.释放连接
            try {
                if (reader != null) reader.close();
                if (writer != null) writer.close();
                if (s != null) s.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // 从 socket 中读取 Redis 返回的数据
    private static Object handleResponse() throws IOException {
        // 读取首字节
        int prefix = reader.read();
        // 判断数据类型标示
        switch (prefix) {
            case '+': // 单行字符串，每个字符串后都会衔接换行符，所以直接读一行
                return reader.readLine();
            case '-': // 异常，也读一行
                throw new RuntimeException(reader.readLine());
            case ':': // 数字
                return Long.parseLong(reader.readLine());
            case '$': // 多行字符串
                // 先读长度
                int len = Integer.parseInt(reader.readLine());
                if (len == -1) {
                    return null;
                }
                if (len == 0) {
                    return "";
                }
                // 再读数据，读 len 个字节。假设没有特殊字符，所以读一行（简化）
                return reader.readLine();
            case '*':
                return readBulkString();
            default:
                throw new RuntimeException("错误的数据格式！");
        }
    }

    // 读取 Redis 返回的数组信息
    private static Object readBulkString() throws IOException {
        // 获取数组大小（行数）
        int len = Integer.parseInt(reader.readLine());
        if (len <= 0) {
            return null;
        }
        // 定义集合，接收多个元素
        List<Object> list = new ArrayList<>(len);
        // 遍历，依次读取每个元素
        for (int i = 0; i < len; i++) {
            // 调用 handleResponse 读取对应的数据，然后封装进集合
            list.add(handleResponse());
        }
        return list;
    }

    // 将 Redis 命令转换为 RESP 协议格式：SET name 张三 -> *3\r\n$3\r\nSET\r\n$4\r\nname\r\n$6\r\n张三\r\n
    // 接收多个参数，循环将这些参数进行格式转换，然后写进 socket
    private static void sendRequest(String ... args) {
        writer.println("*" + args.length);
        for (String arg : args) {
            writer.println("$" + arg.getBytes(StandardCharsets.UTF_8).length);
            writer.println(arg);
        }
        writer.flush();
    }
}
```

****
### 9. Redis 内存策略

#### 9.1 过期 key 处理

Redis 之所以性能强，最主要的原因就是基于内存存储，然而单节点的 Redis 其内存大小不宜过大，否则会影响持久化或主从同步性能。可以通过修改配置文件来设置 Redis 的最大内存：

```redis
# 设置最大内存为 1gb
maxmemory 1gb
```

当内存使用达到上限时，就无法存储更多数据了，所以 Redis 提供了一些策略实现内存回收：

Redis 中可以通过 expire 命令给 key 设置 TTL（存活时间）：set name jack expire 5（存活时间 5 s），当 key 的 TTL 到期以后，再次访问 name 返回的是 nil，
说明这个 key 已经不存在了，对应的内存也得到释放，从而起到内存回收的目的。而 Redis 本身是一个典型的 key-value 内存存储数据库，因此所有的 key、value 都保存在 Dict 结构中。
不过在其 database 结构体中，有两个 Dict：一个用来记录 key-value；另一个用来记录 key-TTL。

```c
typedef struct redisDb {
    dict *dict; // 存放所有 key 和 calue 的地方， 也被称为 keyspace
    dict *expires; // 存放每个 key 及其对应 TTL，只包含设置了 TTL 的 key
    dict *blocking_keys; // 阻塞等待某个 key 的 client 集合
    dict *ready_keys; // 当 key 被 push 后，已准备就绪可唤醒的 client 集合
    dict *watched_keys; /* WATCHED keys for MULTI/EXEC CAS */
    int id; // 数据库 id
    long long avg_ttl; // 记录平均 TTL 时间
    unsigned long expires_cursor; // expire 检查时在 dict 中抽样的索引位置
    list *defrag_later; // 等待碎片整理的 key 列表
} redisDb;
```

虽然 Redis 用了一个 Dict 来存储设置了 TTL 的 key，但并不是 TTL 一到期就立即删除，而是进行惰性删除，也就是在访问某个 key 的时候，检查该 key 的存活时间，如果已经过期才执行删除。

```c
// 查找 key 执行写操作
robj *lookupKeyWriteWithFlags(redisDb *db, robj *key, int flags) {
    expireIfNeeded(db,key);
    return lookupKey(db,key,flags);
}
```

```c
// 查找 key 执行读操作
robj *lookupkeyReadWithFlags(redisDb *db, robj *key, int flags) {
    robj *val;
    // 检查 key 是否过期
    if (expireIfNeeded(db, key) == 1) {
        ...
    }
    return NULL;
}
```

```c
int expireIfNeeded(redisDb *db, robj ?*key) {
    // 判断是否过期，如果未过期直接结束并返回 0
    if (!keyIsExpired(db, key)) return 0;
    ...
    // 删除过期 key
    deleteExpiredKeyAndPropagate(db, key);
    return 1;
}
```

但惰性删除存在一个问题，就是如果一直不访问的话，那么这个 key 就会一直存在，占用内存，无法达到内存回收的目的，所以 Redis 又引入了周期删除技术，
通过一个定时任务，周期性的抽样部分过期的 key，然后执行删除。执行周期有两种：

- Redis 服务初始化函数 initServer() 中设置定时任务，按照 server.hz 的频率来执行过期 key 清理，模式为 SLOW

Redis 启动时进行初始化，还会创建一个定时器

```c
void initServer(void){
    ...
    // 创建定时器，关联回调函数 serverCron，处理周期取决于 server.hz，默认为 10
    aeCreatTimeEvent(server.el, 1, serverCron, NULL, NULL)
}
```

然后调用 aeMain 方法循环中，会调用创建好的定时器，检查 key 的 TTL 是否到了，如果到了则执行 serverCron 方法，然后回返回一个默认时间 100ms，

```c
int serverCron(struct aeEventLoop *eventLoop, long long id, void *clientData) {
    // 更新 lruclock 到当前时间，为后期的 LRU 和 LFU 做准备
    unsigned int lruclock = getLRUClock();
    atomicSet(server.lruclock, lruclock);
    // 执行 database 的数据清理，例如过期的 key 处理
    databaseCron();
    return 1000/server.hz;
}
```

```c
void databaseCron(void) {
    // 尝试清理部分过期 key，清理模式默认为 SLOW
    activeExpireCycle(ACTIVE_EXPIRE_CYCLE_SLOW);
}
```

- Redis 的每个事件循环前会调用 beforeSleep() 函数，执行过期 key 清理，模式为 FAST

当完成初始化后 Redis 会调用 aeMain 方法开启事件监听，其中就会调用 beforeSleep 方法，不仅创建写处理器，还会尝试清理过期的 key，每经历一次循环就嗲用一次 FAST 模式

```c
void beforeSleep(struct aeEventLoop *eventloop) {
    ...
    // 尝试清理部分过期 key，清理模式默认为 FAST
    activeExpireCycle(ACTIVE_EXPIRE_CYCLE_FAST);
}
```

SLOW模式规则：

* 执行频率受server.hz影响，默认为10，即每秒执行10次，每个执行周期100ms。
* 执行清理耗时不超过一次执行周期的25%.默认slow模式耗时不超过25ms
* 逐个遍历db，逐个遍历db中的bucket，抽取20个key判断是否过期
* 如果没达到时间上限（25ms）并且过期key比例大于10%，再进行一次抽样，否则结束

FAST模式规则（过期key比例小于10%不执行 ）：

* 执行频率受beforeSleep()调用频率影响，但两次FAST模式间隔不低于2ms
* 执行清理耗时不超过1ms
* 逐个遍历db，逐个遍历db中的bucket，抽取20个key判断是否过期 
* 如果没达到时间上限（1ms）并且过期key比例大于10%，再进行一次抽样，否则结束

****
#### 9.2 内存淘汰策略

内存淘汰就是当 Redis 内存使用达到设置的上限时，主动挑选部分 key 删除以释放更多内存的流程。Redis 会在处理客户端命令的方法 processCommand() 中尝试做内存淘汰：

Redis 解析命令的时候会调用下面的方法，但是在真正执行前，会对内存上线进行检查，如果设置了上限，就会尝试进行清理，

```c
int processCommand(client *c) {
    // 如果服务器设置了 saerver.maxmemory 属性，并且没有执行 lua 脚本
    if (saerver.maxmemory && !server.lua_timedout) {
        // 尝试进行内存淘汰 performEvictions
        int out_of_memory = (performEvictions() == EVICT_FAIL);
        ...
        if (out_of_memory && reject_cmd_on_oom) {
            rejectCommand(c, shared.oomerr);
            return C_OK;
        }
    }
}
```

Redis支持8种不同策略来选择要删除的key：

* noeviction：不淘汰任何key，但是内存满时不允许写入新数据，默认就是这种策略
* volatile-ttl：对设置了TTL的key，比较key的剩余TTL值，TTL越小越先被淘汰
* allkeys-random：对全体key ，随机进行淘汰。也就是直接从db->dict中随机挑选
* volatile-random：对设置了TTL的key ，随机进行淘汰。也就是从db->expires中随机挑选
* allkeys-lru：对全体key，基于LRU算法进行淘汰
* volatile-lru：对设置了TTL的key，基于LRU算法进行淘汰
* allkeys-lfu：对全体key，基于LFU算法进行淘汰
* volatile-lfu：对设置了TTL的key，基于LFI算法进行淘汰
  比较容易混淆的有两个：
    * LRU（Least Recently Used），最少最近使用。用当前时间减去最后一次访问时间，这个值越大则淘汰优先级越高。
    * LFU（Least Frequently Used），最少频率使用。会统计每个 key 的访问频率，值越小淘汰优先级越高。

Redis 的数据都会被封装为 RedisObject 结构：

```c
typedef struct redisObject {
    unsigned type:4;
    unsigned encoding:4;
    unsigned lru:LRU_BITS; // LRU:以秒为单位记录最近一次访问时间，长度为 24 bit；LFU：高 16 位以分钟为单位记录最近一次访问时间，低 8 位记录逻辑访问次数
    int refcount;
    void *ptr;
} robj;
```

LFU 的访问次数之所以叫做逻辑访问次数，是因为并不是每次 key 被访问都计数，而是通过运算：

* 生成 0~1 之间的随机数 R
* 计算 (旧次数 * lfu_log_factor + 1)，记录为 P，lfu_log_factor 默认 10
* 如果 R < P ，则计数器 + 1，且最大不超过 255
* 访问次数会随时间衰减，距离上一次访问时间每隔 lfu_decay_time 分钟（默认 1），计数器 -1

整体流程：

首先会判断内存是否充足，若充足则结束；不充足则会先判断使用的内存策略是否为 noeviction，如果是那就不进行内存淘汰，直接结束；然后判断使用的是 allkeys 还是 volatile，
allkeys 策略就从 db -> dict 中淘汰，volatile 就从 db -> entries 淘汰；然后判断是否为随机淘汰策略，如果是就直接遍历 db 随机选一个 key 淘汰，然后循环判断内存是否满足需要；
如果不是就准备一个 evication_pool 淘汰池，因为将来 Redis 中可能存在数以万计的 key，如果还是依次遍历它们的 LRU/LFU/TTL 的值的话效率就太低了，
所以 Redis 选择从 db 中挑选一些作为样本，然后让它们作比较看谁应该先淘汰（默认挑选 maxmemory_samples 5个），然后从这些样本中进行筛选可以淘汰的 key 放到 evication_pool 池中，
因为不同的淘汰策略的淘汰标准不一致，所以需要统一放入 evication_pool 池中的规则，所以 Redis 选择让它们按照对应值的升序进行排序放入池中，值大的优先淘汰；例如：
TTL：用 maxTTL - TTL 作为 idleTime（用设定的存活时间 - 当前剩余存活时间，差值越大证明所剩时间不多了，可以考虑优先淘汰）；
LRU：用 now - LRU 作为 idleTime（当前时间 - 最近使用时间，这个差值越大证明这个 key 距离上次使用的时间越久，可以考虑优先淘汰）；
LFU：用 255 - LFU 作为 idleTime（用最大上限 - LFU 当前值，差值越大证明这个 key 使用的频率越低，可以优先考虑淘汰）。
判断完后依次升序放入 evication_pool 池中，然后倒序从里面获取 key 进行淘汰，接着循环判断是否满足内存需求。

****






