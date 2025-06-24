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








