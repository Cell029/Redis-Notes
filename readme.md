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










