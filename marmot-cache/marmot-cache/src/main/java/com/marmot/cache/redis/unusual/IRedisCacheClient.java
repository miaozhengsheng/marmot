package com.marmot.cache.redis.unusual;

import java.util.List;
import java.util.Map;
import java.util.Set;

import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;
import redis.clients.jedis.Tuple;

import com.marmot.cache.ICacheClientWithException;
import com.marmot.cache.enums.EnumExist;
import com.marmot.cache.enums.EnumTime;
import com.marmot.cache.exception.CacheException;
import com.marmot.cache.redis.pubsub.AbstractSubscriber;

public interface IRedisCacheClient  extends ICacheClientWithException {

    /** ==================== Key ==================== **/

    /**
     * 删除给定keys
     */
    public long keyDel(String... keys) throws CacheException;

    /**
     * 查找所有符合给定模式pattern的key
     */
    public Set<String> keyKeys(String pattern) throws CacheException;

    /**
     * 检查给定key是否存在。
     */
    public boolean keyExists(String key) throws CacheException;

    /**
     * 为给定 key 设置生存时间
     * 
     * @param key
     * @param seconds 单位：秒
     * @return
     */
    public boolean keyExpire(String key, int seconds) throws CacheException;

    /**
     * 为给定 key 设置生存时间
     * <p>
     * unixTime = System.currentTimeMillis()/1000;
     * 
     * @param key
     * @param unixTime 单位：UNIX 时间戳(unix timestamp)
     * @return
     */
    public boolean keyExpireAt(String key, long unixTime) throws CacheException;

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
    public long keyTtl(String key) throws CacheException;

    /** ==================== String ==================== **/

    /**
     * 将字符串值 value 关联到 key
     */
    public boolean stringSet(String key, String value) throws CacheException;

    /**
     * 将字符串值 value 关联到 key
     */
    public boolean stringSetex(String key, String value, int seconds) throws CacheException;

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
    public boolean stringSet(String key, String value, EnumExist nxxx, EnumTime expx, long time) throws CacheException;

    /**
     * 将字符串值 vluae 关联到不存在的key上
     * 
     * @param key
     * @param value
     */
    public boolean stringSetnx(String key, String value) throws CacheException;

    /**
     * 一次设置多个key的值
     */
    public boolean stringMset(Map<String, String> kvs) throws CacheException;

    /**
     * 获取key对应的string值
     */
    public String stringGet(String key) throws CacheException;

    /**
     * 一次获取多个key的值
     */
    public Map<String, String> stringMget(String... keys) throws CacheException;

    /**
     * 将给定 key 的值设为 value ，并返回 key 的旧值(old value)
     */
    public String stringGetset(String key, String value) throws CacheException;


    /** ==================== Hash ==================== **/

    /**
     * 将哈希表 key 中的域 field 的值设为 value
     */
    public boolean hashHset(String key, String field, String value) throws CacheException;

    /**
     * 将哈希表 key 中的域 field 的值设为 value,仅当field不存在的时候
     * 
     * @param key
     * @param field
     * @param value
     * @return
     */
    public Boolean hashHsetnx(String key, String field, String value) throws CacheException;

    /**
     * 同时将多个 field-value (域-值)对设置到哈希表 key 中
     */
    public boolean hashHmset(String key, Map<String, String> hash) throws CacheException;

    /**
     * 返回哈希表 key 中给定域 field 的值
     */
    public String hashHget(String key, String field) throws CacheException;

    /**
     * 返回哈希表 key 中，一个或多个给定域的值
     */
    public List<String> hashHmget(String key, String... fields) throws CacheException;

    /**
     * 返回哈希表 key 中，所有的域和值
     */
    public Map<String, String> hashHgetAll(String key) throws CacheException;

    /**
     * 删除哈希表 key 中的一个或多个指定域，不存在的域将被忽略
     */
    public long hashHdel(String key, String... fields) throws CacheException;

    /**
     * 返回哈希表 key 中域的数量
     */
    public long hashHlen(String key) throws CacheException;

    /**
     * 查看哈希表 key 中，给定域 field 是否存在
     */
    public boolean hashHexists(String key, String field) throws CacheException;

    /**
     * 为哈希表 key 中的域 field 的值加上增量 increment
     */
    public long hashHincrBy(String key, String field, long value) throws CacheException;

    /**
     * 返回哈希表 key 中的所有域
     */
    public Set<String> hashHkeys(String key) throws CacheException;

    /**
     * 返回哈希表 key 中所有域的值
     */
    public List<String> hashHvals(String key) throws CacheException;


    /** ==================== List ==================== **/

    /**
     * 将一个或多个值 value 插入到列表 key 的表头
     */
    public long listLpush(String key, String... strings) throws CacheException;

    /**
     * 将一个或多个值 value 插入到列表 key 的表尾(最右边)
     */
    public long listRpush(String key, String... strings) throws CacheException;

    /**
     * 移除并返回列表 key 的头元素
     */
    public String listLpop(String key) throws CacheException;

    /**
     * 阻塞移除并返回列表 key 的头元素
     */
    public String listBlpop(String key, int timeout) throws CacheException;

    /**
     * 阻塞移除并返回列表 key 的尾元素
     * 
     * @param key
     * @param timeout 单位秒
     * @return
     */
    public String listBrpop(String key, int timeout) throws CacheException;

    /**
     * 移除并返回列表 key 的尾元素
     */
    public String listRpop(String key) throws CacheException;

    /**
     * 返回列表 key 的长度
     */
    public long listLlen(String key) throws CacheException;

    /**
     * 返回列表 key 中指定区间内的元素，区间以偏移量 start 和 stop 指定 下标都从0开始，负值表示从后面计算，-1表示倒数第一个元素
     */
    public List<String> listLrange(String key, long start, long end) throws CacheException;

    /**
     * 根据参数 count 的值，移除列表中所有与参数 value 相等的元素。
     */
    public long listLrem(String key, String value) throws CacheException;

    /**
     * 
     * 对一个列表进行修剪(trim)，就是说，让列表只保留指定区间内的元素，不在指定区间之内的元素都将被删除
     */
    public boolean listLtrim(String key, long start, long end) throws CacheException;

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
     * @throws CacheException
     */
    public Boolean listLset(String key, long index, String value) throws CacheException;


    /** ==================== Set ==================== **/

    /**
     * 将一个或多个 member 元素加入到集合 key 当中，已经存在于集合的 member 元素将被忽略
     */
    public long setSadd(String key, String... members) throws CacheException;

    /**
     * 移除集合 key 中的一个或多个 member 元素，不存在的 member 元素会被忽略
     */
    public long setSrem(String key, String... members) throws CacheException;

    /**
     * 返回集合 key 中的所有成员
     */
    public Set<String> setSmembers(String key) throws CacheException;

    /**
     * 判断 member 元素是否集合 key 的成员
     */
    public boolean setSismember(String key, String member) throws CacheException;

    /**
     * 返回集合 key 的基数(集合中元素的数量)
     */
    public long setScard(String key) throws CacheException;

    /**
     * 移除并返回集合中的一个随机元素
     * 
     * @param key
     * @return
     */
    public String setSpop(String key) throws CacheException;

    /**
     * 将 member 元素从 source 集合移动到 destination 集合
     * 
     * @param srckey
     * @param dstkey
     * @param member
     * @return
     */
    public boolean setSmove(String srckey, String dstkey, String member) throws CacheException;

    /**
     * 增量地迭代一集元素
     * 
     * @param key
     * @param cursor 游标
     * @return
     */
    public ScanResult<String> setScan(String key, String cursor) throws CacheException;

    /**
     * 增量地迭代一集元素
     * 
     * @param key
     * @param cursor 游标
     * @param params 扫描模式参数（匹配参数、返回个数）
     * @return
     */
    public ScanResult<String> setScan(String key, String cursor, ScanParams params) throws CacheException;

    /** ==================== Sorted set ==================== **/

    /**
     * 将一个或多个 member 元素及其 score 值加入到有序集 key 当中
     */
    public long sortSetZadd(String key, String member, double score) throws CacheException;

    /**
     * 将一个或多个 member 元素及其 score 值加入到有序集 key 当中
     * 
     * @deprecated starting from version 1.3.5 this method will not be exposed.
     * @see {@link che.redis.IRedisCacheClient#sortSetZadd2()}
     */
    public long sortSetZadd(String key, Map<Double, String> scoreMembers) throws CacheException;

    /**
     * 将一个或多个 member 元素及其 score 值加入到有序集 key 当中
     * 
     * @param key
     * @param scoreMembers
     * @return
     * @throws CacheException
     */
    public long sortSetZadd2(String key, Map<String, Double> scoreMembers) throws CacheException;

    /**
     * 移除有序集 key 中的一个或多个成员，不存在的成员将被忽略
     */
    public long sortSetZrem(String key, String... members) throws CacheException;

    /**
     * 返回有序集 key 的基数
     */
    public long sortSetZcard(String key) throws CacheException;

    /**
     * 返回有序集 key 中， score 值在 min 和 max 之间(默认包括 score 值等于 min 或 max )的成员的数量
     */
    public long sortSetZcount(String key, double min, double max) throws CacheException;

    /**
     * 返回有序集 key 中，成员 member 的 score 值
     */
    public Double sortSetZscore(String key, String member) throws CacheException;

    /**
     * 为有序集 key 的成员 member 的 score 值加上增量 increment
     */
    public double sortSetZincrby(String key, String member, double score) throws CacheException;

    /**
     * 返回有序集 key 中，指定区间内的成员 其中成员的位置按 score 值递增(从小到大)来排序。 下标参数 start 和 stop 都以 0
     * 为底
     */
    public Set<String> sortSetZrange(String key, long start, long end) throws CacheException;

    public Map<String, Double> sortSetZrangeWithScores(String key, long start, long end) throws CacheException;

    /**
     * 从大到小
     */
    public Set<String> sortSetZrevrange(String key, long start, long end) throws CacheException;

    public Map<String, Double> sortSetZrevrangeWithScores(String key, long start, long end) throws CacheException;

    /**
     * 返回有序集 key 中成员 member 的排名。其中有序集成员按 score 值递增(从小到大)顺序排列
     */
    public Long sortSetZrank(String key, String member) throws CacheException;

    /**
     * 返回有序集 key 中成员 member 的排名。其中有序集成员按 score 值递减(从大到小)排序
     */
    public Long sortSetZrevrank(String key, String member) throws CacheException;

    /**
     * 返回有序集 key 中，所有 score 值介于 min 和 max 之间(包括等于 min 或 max )的成员。有序集成员按 score
     * 值递增(从小到大)次序排列。
     */
    public Set<String> sortSetZrangeByScore(String key, double min, double max) throws CacheException;

    /**
     * 返回有序集 key 中，所有 score 值介于 min 和 max 之间(包括等于 min 或 max )的成员。有序集成员按 score
     * 值递增(从小到大)次序排列。
     */
    public Set<String> sortSetZrangeByScore(String key, double min, double max, int offset, int count)
            throws CacheException;

    /**
     * 返回有序集 key 中，score 值介于 max 和 min 之间(默认包括等于 max 或 min )的所有的成员。有序集成员按 score
     * 值递减(从大到小)的次序排列。
     */
    public Set<String> sortSetZrevrangeByScore(String key, double max, double min) throws CacheException;

    /**
     * 返回有序集 key 中，score 值介于 max 和 min 之间(默认包括等于 max 或 min )的所有的成员。有序集成员按 score
     * 值递减(从大到小)的次序排列。
     */
    public Set<String> sortSetZrevrangeByScore(String key, double max, double min, int offset, int count)
            throws CacheException;

    /**
     * 移除有序集 key 中，指定排名(rank)区间内的所有成员
     */
    public long sortSetZremrangeByRank(String key, long start, long end) throws CacheException;

    /**
     * 增量地迭代一集元素
     * 
     * @param key
     * @param cursor 游标
     * @return
     */
    public ScanResult<Tuple> sortSetZscan(String key, String cursor) throws CacheException;

    /**
     * 增量地迭代一集元素
     * 
     * @param key
     * @param cursor 游标
     * @param params 扫描模式参数（匹配参数、返回个数）
     * @return
     */
    public ScanResult<Tuple> sortSetZscan(String key, String cursor, ScanParams params) throws CacheException;

    /**
     * 向channel发布消息message
     */
    public boolean publish(String channel, String message) throws CacheException;

    /**
     * 向多个channel订阅消息
     * 
     * @param subscriber
     * @param channels
     */
    public void subscribe(AbstractSubscriber subscriber, String... channels) throws CacheException;

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
    public void psubscribe(AbstractSubscriber subscriber, String... patterns) throws CacheException;

}
