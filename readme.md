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



