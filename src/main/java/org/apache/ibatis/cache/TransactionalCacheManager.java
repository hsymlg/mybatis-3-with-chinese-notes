/*
 *    Copyright 2009-2021 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.apache.ibatis.cache;

import java.util.HashMap;
import java.util.Map;

import org.apache.ibatis.cache.decorators.TransactionalCache;
import org.apache.ibatis.util.MapUtil;

/**
 * @author Clinton Begin
 * 如果对应的 statement 的二级缓存配置有效，则会先通过缓存事务管理器 TransactionalCacheManager 查询二级缓存，
 * 如果没有命中则查询一级缓存，仍没有命中才会执行数据库查询。
 * 缓存执行器对二级缓存的维护是基于缓存事务管理器 TransactionalCacheManager 的，
 * 其内部维护了一个 Map 容器，用于保存 namespace 缓存配置与事务缓存对象的映射关系。
 */
public class TransactionalCacheManager {

  /**
   * 缓存配置 - 缓存事务对象
   */
  private final Map<Cache, TransactionalCache> transactionalCaches = new HashMap<>();

  /**
   * 清除缓存
   */
  public void clear(Cache cache) {
    getTransactionalCache(cache).clear();
  }

  /**
   * 获取缓存
   */
  public Object getObject(Cache cache, CacheKey key) {
    return getTransactionalCache(cache).getObject(key);
  }

  /**
   * 写缓存
   */
  public void putObject(Cache cache, CacheKey key, Object value) {
    getTransactionalCache(cache).putObject(key, value);
  }

  /**
   * 缓存提交
   */
  public void commit() {
    for (TransactionalCache txCache : transactionalCaches.values()) {
      txCache.commit();
    }
  }

  /**
   * 缓存回滚
   */
  public void rollback() {
    for (TransactionalCache txCache : transactionalCaches.values()) {
      txCache.rollback();
    }
  }

  /**
   * getTransactionalCache 会从维护容器中查找对应的事务缓存对象，
   * 如果找不到就创建一个事务缓存对象，即通过事务缓存对象装饰当前缓存配置。
   */
  private TransactionalCache getTransactionalCache(Cache cache) {
    return MapUtil.computeIfAbsent(transactionalCaches, cache, TransactionalCache::new);
  }

}
