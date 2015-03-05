/*
 * Copyright 2012-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.baidu.rigel.biplatform.cache.config;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.redis.RedisProperties.Sentinel;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisNode;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnection;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import redis.clients.jedis.JedisPoolConfig;

import com.baidu.rigel.biplatform.cache.redis.config.RedisPoolProperties;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Data's Redis support.
 * 
 * @author Dave Syer
 * @author Andy Wilkinson
 * @author Christian Dupuis
 */
@Configuration
@ConditionalOnClass({ JedisConnection.class})
@EnableConfigurationProperties
public class BiplatformRedisConfiguration {


    @Bean
    @ConditionalOnMissingBean
    RedisPoolProperties redisPoolProperties() {
        return new RedisPoolProperties();
    }

    /**
     * Base class for Redis configurations.
     */
    protected static abstract class AbstractRedisConfiguration {

        @Autowired
        protected RedisPoolProperties properties;

        @Autowired(required = false)
        private RedisSentinelConfiguration sentinelConfiguration;

        protected final JedisConnectionFactory applyProperties(
                JedisConnectionFactory factory) {
            factory.setHostName(this.properties.getHost());
            factory.setPort(this.properties.getPort());
            if (this.properties.getPassword() != null) {
                factory.setPassword(this.properties.getPassword());
            }
            factory.setDatabase(this.properties.getDatabase());
            return factory;
        }

        protected final RedisSentinelConfiguration getSentinelConfig() {
            if (this.sentinelConfiguration != null) {
                return this.sentinelConfiguration;
            }
            Sentinel sentinelProperties = this.properties.getSentinel();
            if (sentinelProperties != null) {
                RedisSentinelConfiguration config = new RedisSentinelConfiguration();
                config.master(sentinelProperties.getMaster());
                config.setSentinels(createSentinels(sentinelProperties));
                
                
                return config;
            }
            return null;
        }

        private List<RedisNode> createSentinels(Sentinel sentinel) {
            List<RedisNode> sentinels = new ArrayList<RedisNode>();
            String nodes = sentinel.getNodes();
            for (String node : StringUtils.commaDelimitedListToStringArray(nodes)) {
                try {
                    String[] parts = StringUtils.split(node, ":");
                    Assert.state(parts.length == 2, "Must be defined as 'host:port'");
                    sentinels.add(new RedisNode(parts[0], Integer.valueOf(parts[1])));
                }
                catch (RuntimeException ex) {
                    throw new IllegalStateException("Invalid redis sentinel "
                            + "property '" + node + "'", ex);
                }
            }
            return sentinels;
        }

    }

    /**
     * Redis pooled connection configuration.
     */
    @Configuration
    protected static class RedisPooledConnectionConfiguration extends
            AbstractRedisConfiguration {

        @Bean
        @ConditionalOnProperty(prefix = "config.redis", name = "active", havingValue = "true")
        public RedisConnectionFactory redisConnectionFactory()
                throws UnknownHostException {
            return applyProperties(createJedisConnectionFactory());
        }
        
        @Bean
        @ConditionalOnBean(RedisConnectionFactory.class)
        public RedisOperations<Object, Object> redisTemplate(
                RedisConnectionFactory redisConnectionFactory)
                throws UnknownHostException {
            RedisTemplate<Object, Object> template = new RedisTemplate<Object, Object>();
            template.setConnectionFactory(redisConnectionFactory);
            return template;
        }

        
        @Bean(name="redisCacheManager")
        @ConditionalOnBean(RedisOperations.class)
        public CacheManager redisCacheManager(RedisTemplate<Object, Object> template) {
            return new RedisCacheManager(template);
        }

        private JedisConnectionFactory createJedisConnectionFactory() {
            JedisConnectionFactory factory = null;
            if (this.properties.getPoolConfig() != null) {
                factory = new JedisConnectionFactory(getSentinelConfig(), jedisPoolConfig());
            }
            factory = new JedisConnectionFactory(getSentinelConfig());
            factory.setUsePool(this.properties.isUsePool());
            factory.setPassword(this.properties.getPassword());
            return factory;
        }

        private JedisPoolConfig jedisPoolConfig() {
            JedisPoolConfig config = new JedisPoolConfig();
            RedisPoolProperties.Pool props = this.properties.getPoolConfig();
            config.setMaxTotal(props.getMaxActive());
            config.setMaxIdle(props.getMaxIdle());
            config.setMinIdle(props.getMinIdle());
            config.setMaxWaitMillis(props.getMaxWait());
            config.setTestOnBorrow(props.isTestOnBorrow());
            config.setTestOnCreate(props.isTestOnCreate());
            config.setTestOnReturn(props.isTestOnReturn());
            config.setTestWhileIdle(props.isTestWhileIdle());
            config.setTimeBetweenEvictionRunsMillis(props.getTimeBetweenEvictionRunsMillis());
            config.setMinEvictableIdleTimeMillis(props.getMinEvictableIdleTimeMillis());
            config.setNumTestsPerEvictionRun(props.getNumTestsPerEvictionRun());
            config.setSoftMinEvictableIdleTimeMillis(props.getSoftMinEvictableIdleTimeMillis());
            config.setLifo(props.isLifo());
            return config;
        }

    }



}
