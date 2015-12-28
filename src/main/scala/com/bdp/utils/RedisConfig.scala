package com.bdp.utils

import com.typesafe.config.ConfigFactory
import redis.RedisClient

/**
 * Created by zhange on 15/12/15.
 *
 */

trait RedisConfig extends Config {
  implicit val systemRedisClient = akka.actor.ActorSystem("RedisClient", ConfigFactory.load("redis-akka"))
  val redisClient = RedisClient(redisHost, redisPort, redisPassword)
}
