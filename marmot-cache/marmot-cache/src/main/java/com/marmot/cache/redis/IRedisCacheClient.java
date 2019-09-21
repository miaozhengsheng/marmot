package com.marmot.cache.redis;

import java.util.List;
import java.util.Map;
import java.util.Set;

import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;
import redis.clients.jedis.Tuple;

import com.marmot.cache.ICacheClient;
import com.marmot.cache.enums.EnumExist;
import com.marmot.cache.enums.EnumTime;
import com.marmot.cache.redis.pubsub.AbstractSubscriber;

public interface IRedisCacheClient extends ICacheClient{


    /** ==================== Key ==================== **/

    /**
     * 删除给定keys
     */
    public Long keyDel(String... keys);

    /**
     * 查找所有符合给定模式pattern的key
     */
    public Set<String> keyKeys(String pattern);

    /**
     * 检查给定key是否存在。
     */
    public Boolean keyExists(String key);

    /**
     * 为给定 key 设置生存时间
     * 
     * @param key
     * @param seconds 单位：秒
     * @return
     */
    public Boolean keyExpire(String key, int seconds);

    /**
     * 为给定 key 设置生存时间
     * <p>
     * unixTime = System.currentTimeMillis()/1000;
     * 
     * @param key
     * @param unixTime 单位：UNIX 时间戳(unix timestamp)
     * @return
     */
    public Boolean keyExpireAt(String key, long unixTime);

    /**
     * 返回给定 key 的剩余生存时间，单位秒
     * <p>
     * 当 key 不存在时，返回 -2 <br>
     * 当 key 存在但没有设置剩余生存时间时，返回 -1 <br>
     * 否则，以秒为单位，返回 key 的剩余生存时间<br>
     * 
     * @param key
     * @return
     */
    public Long keyTtl(String key);

    /** ==================== String ==================== **/

    /**
     * 将字符串值 value 关联到 key
     */
    public Boolean stringSet(String key, String value);

    /**
     * 将字符串值 value 关联到 key
     */
    public Boolean stringSetex(String key, String value, int seconds);

    /**
     * 将字符串值 value 关联到 key
     * 
     * @param key
     * @param value
     * @param nxxx NX ：只在键不存在时，才对键进行设置操作 | XX ：只在键已经存在时，才对键进行设置操作
     * @param expx EX ：设置键的过期时间为 second 秒 | PX ：设置键的过期时间为 millisecond 毫秒
     * @param time
     * @return
     */
    public Boolean stringSet(String key, String value, EnumExist nxxx, EnumTime expx, long time);

    /**
     * 将字符串值 vluae 关联到不存在的key上
     * 
     * @param key
     * @param value
     */
    public Boolean stringSetnx(String key, String value);

    /**
     * 一次设置多个key的值
     */
    public Boolean stringMset(Map<String, String> kvs);

    /**
     * 获取key对应的string值
     */
    public String stringGet(String key);

    /**
     * 一次获取多个key的值
     */
    public Map<String, String> stringMget(String... keys);

    /**
     * 将给定 key 的值设为 value ，并返回 key 的旧值(old value)
     */
    public String stringGetset(String key, String value);


    /** ==================== Hash ==================== **/

    /**
     * 将哈希表 key 中的域 field 的值设为 value
     */
    public Boolean hashHset(String key, String field, String value);

    /**
     * 将哈希表 key 中的域 field 的值设为 value,仅当field不存在的时候
     * 
     * @param key
     * @param field
     * @param value
     * @return
     */
    public Boolean hashHsetnx(String key, String field, String value);

    /**
     * 同时将多个 field-value (域-值)对设置到哈希表 key 中
     */
    public Boolean hashHmset(String key, Map<String, String> hash);

    /**
     * 返回哈希表 key 中给定域 field 的值
     */
    public String hashHget(String key, String field);

    /**
     * 返回哈希表 key 中，一个或多个给定域的值
     */
    public List<String> hashHmget(String key, String... fields);

    /**
     * 返回哈希表 key 中，所有的域和值
     */
    public Map<String, String> hashHgetAll(String key);

    /**
     * 删除哈希表 key 中的一个或多个指定域，不存在的域将被忽略
     */
    public Long hashHdel(String key, String... fields);

    /**
     * 返回哈希表 key 中域的数量
     */
    public Long hashHlen(String key);

    /**
     * 查看哈希表 key 中，给定域 field 是否存在
     */
    public Boolean hashHexists(String key, String field);

    /**
     * 为哈希表 key 中的域 field 的值加上增量 increment
     */
    public Long hashHincrBy(String key, String field, long value);

    /**
     * 返回哈希表 key 中的所有域
     */
    public Set<String> hashHkeys(String key);

    /**
     * 返回哈希表 key 中所有域的值
     */
    public List<String> hashHvals(String key);


    /** ==================== List ==================== **/

    /**
     * 将一个或多个值 value 插入到列表 key 的表头
     * 
     * @param key
     * @param strings
     * @return 列表长度
     */
    public Long listLpush(String key, String... strings);

    /**
     * 将一个或多个值 value 插入到列表 key 的表尾(最右边)
     */
    public Long listRpush(String key, String... strings);

    /**
     * 移除并返回列表 key 的头元素
     */
    public String listLpop(String key);

    /**
     * 阻塞移除并返回列表 key 的头元素
     * 
     * @param key
     * @param timeout 单位秒
     * @return
     */
    public String listBlpop(String key, int timeout);

    /**
     * 阻塞移除并返回列表 key 的尾元素
     * 
     * @param key
     * @param timeout 单位秒
     * @return
     */
    public String listBrpop(String key, int timeout);

    /**
     * 移除并返回列表 key 的尾元素
     */
    public String listRpop(String key);

    /**
     * 返回列表 key 的长度
     */
    public Long listLlen(String key);

    /**
     * 返回列表 key 中指定区间内的元素，区间以偏移量 start 和 stop 指定 下标都从0开始，负值表示从后面计算，-1表示倒数第一个元素
     */
    public List<String> listLrange(String key, long start, long end);

    /**
     * 根据参数 count 的值，移除列表中所有与参数 value 相等的元素。
     */
    public Long listLrem(String key, String value);

    /**
     * 
     * 对一个列表进行修剪(trim)，就是说，让列表只保留指定区间内的元素，不在指定区间之内的元素都将被删除
     */
    public Boolean listLtrim(String key, long start, long end);

    /**
     * 将列表 key 下标为 index 的元素的值设置为 value 。
     * <p>
     * 成功返回true<br>
     * 失败返回false<br>
     * 
     * @param key
     * @param index
     * @param value
     * @return
     */
    public Boolean listLset(String key, long index, String value);


    /** ==================== Set ==================== **/

    /**
     * 将一个或多个 member 元素加入到集合 key 当中，已经存在于集合的 member 元素将被忽略
     */
    public Long setSadd(String key, String... members);

    /**
     * 移除集合 key 中的一个或多个 member 元素，不存在的 member 元素会被忽略
     */
    public Long setSrem(String key, String... members);

    /**
     * 返回集合 key 中的所有成员
     */
    public Set<String> setSmembers(String key);

    /**
     * 判断 member 元素是否集合 key 的成员
     */
    public Boolean setSismember(String key, String member);

    /**
     * 返回集合 key 的基数(集合中元素的数量)
     */
    public Long setScard(String key);

    /**
     * 移除并返回集合中的一个随机元素
     * 
     * @param key
     * @return
     */
    public String setSpop(String key);

    /**
     * 将 member 元素从 source 集合移动到 destination 集合
     * 
     * @param srckey
     * @param dstkey
     * @param member
     * @return
     */
    public Boolean setSmove(String srckey, String dstkey, String member);

    /**
     * 增量地迭代一集元素
     * 
     * @param key
     * @param cursor 游标: 当游标参数被设置为0时，服务器将开始一次新的迭代，而当服务器向用户返回值为0的游标时，表示迭代已结束。
     * @return
     */
    public ScanResult<String> setScan(String key, String cursor);

    /**
     * 增量地迭代一集元素
     * 
     * @param key
     * @param cursor 游标: 当游标参数被设置为0时，服务器将开始一次新的迭代，而当服务器向用户返回值为0的游标时，表示迭代已结束。
     * @param params 扫描模式参数（匹配参数、返回个数）
     * @return
     */
    public ScanResult<String> setScan(String key, String cursor, ScanParams params);

    /** ==================== Sorted set ==================== **/

    /**
     * 将一个或多个 member 元素及其 score 值加入到有序集 key 当中
     */
    public Long sortSetZadd(String key, String member, double score);

    /**
     * 将一个或多个 member 元素及其 score 值加入到有序集 key 当中
     * 
     * @deprecated starting from version 1.3.5 this method will not be exposed.
     * @see {@link com.liepin.cache.redis.IRedisCacheClient#sortSetZadd2()}
     */
    public Long sortSetZadd(String key, Map<Double, String> scoreMembers);

    /**
     * 将一个或多个 member 元素及其 score 值加入到有序集 key 当中
     * 
     */
    public Long sortSetZadd2(String key, Map<String, Double> scoreMembers);

    /**
     * 移除有序集 key 中的一个或多个成员，不存在的成员将被忽略
     */
    public Long sortSetZrem(String key, String... members);

    /**
     * 返回有序集 key 的基数
     */
    public Long sortSetZcard(String key);

    /**
     * 返回有序集 key 中， score 值在 min 和 max 之间(默认包括 score 值等于 min 或 max )的成员的数量
     */
    public Long sortSetZcount(String key, double min, double max);

    /**
     * 返回有序集 key 中，成员 member 的 score 值
     */
    public Double sortSetZscore(String key, String member);

    /**
     * 为有序集 key 的成员 member 的 score 值加上增量 increment
     */
    public Double sortSetZincrby(String key, String member, double score);

    /**
     * 返回有序集 key 中，指定区间内的成员 其中成员的位置按 score 值递增(从小到大)来排序。 下标参数 start 和 stop 都以 0
     * 为底
     */
    public Set<String> sortSetZrange(String key, long start, long end);

    public Map<String, Double> sortSetZrangeWithScores(String key, long start, long end);

    /**
     * 从大到小
     */
    public Set<String> sortSetZrevrange(String key, long start, long end);

    public Map<String, Double> sortSetZrevrangeWithScores(String key, long start, long end);

    /**
     * 返回有序集 key 中成员 member 的排名。其中有序集成员按 score 值递增(从小到大)顺序排列
     */
    public Long sortSetZrank(String key, String member);

    /**
     * 返回有序集 key 中成员 member 的排名。其中有序集成员按 score 值递减(从大到小)排序
     */
    public Long sortSetZrevrank(String key, String member);

    /**
     * 返回有序集 key 中，所有 score 值介于 min 和 max 之间(包括等于 min 或 max )的成员。有序集成员按 score
     * 值递增(从小到大)次序排列。
     */
    public Set<String> sortSetZrangeByScore(String key, double min, double max);

    /**
     * 返回有序集 key 中，所有 score 值介于 min 和 max 之间(包括等于 min 或 max )的成员。有序集成员按 score
     * 值递增(从小到大)次序排列。
     */
    public Set<String> sortSetZrangeByScore(String key, double min, double max, int offset, int count);

    /**
     * 返回有序集 key 中，score 值介于 max 和 min 之间(默认包括等于 max 或 min )的所有的成员。有序集成员按 score
     * 值递减(从大到小)的次序排列。
     */
    public Set<String> sortSetZrevrangeByScore(String key, double max, double min);

    /**
     * 返回有序集 key 中，score 值介于 max 和 min 之间(默认包括等于 max 或 min )的所有的成员。有序集成员按 score
     * 值递减(从大到小)的次序排列。
     */
    public Set<String> sortSetZrevrangeByScore(String key, double max, double min, int offset, int count);

    /**
     * 移除有序集 key 中，指定排名(rank)区间内的所有成员
     */
    public Long sortSetZremrangeByRank(String key, long start, long end);

    /**
     * 增量地迭代一集元素
     * 
     * @param key
     * @param cursor 游标: 当游标参数被设置为0时，服务器将开始一次新的迭代，而当服务器向用户返回值为0的游标时，表示迭代已结束。
     * @return
     */
    public ScanResult<Tuple> sortSetZscan(String key, String cursor);

    /**
     * 增量地迭代一集元素
     * 
     * @param key
     * @param cursor 游标: 当游标参数被设置为0时，服务器将开始一次新的迭代，而当服务器向用户返回值为0的游标时，表示迭代已结束。
     * @param params 扫描模式参数（匹配参数、返回个数）
     * @return
     */
    public ScanResult<Tuple> sortSetZscan(String key, String cursor, ScanParams params);

    /**
     * 向channel发布消息message
     */
    public Boolean publish(String channel, String message);

    /**
     * 向多个channel订阅消息
     * 
     * @param subscriber
     * @param channels
     */
    public void subscribe(AbstractSubscriber subscriber, String... channels);

    /**
     * 订阅一个或多个符合给定模式的channel
     * <p>
     * 注意：性能
     * <p>
     * 时间复杂度：O(N)， N 是订阅的模式的数量
     * 
     * @param subscriber
     * @param channels
     */
    public void psubscribe(AbstractSubscriber subscriber, String... patterns);


}
