package com.bdp.utils

import java.net.InetAddress

import com.typesafe.config.ConfigFactory

/**
 * Created by zhange on 15/11/2.
 *
 */

trait Config {

  val config = ConfigFactory.load()

  val httpConfig = config.getConfig("http")
  val httpHost = httpConfig.getString("host")
  val httpPort = httpConfig.getInt("port")

  val mongoConfig = config.getConfig("mongo")
  val mongoServerList = mongoConfig.getStringList("serverList")

  val redisConfigBDP = config.getConfig("redis-BDP")
  val postgresConfigBDP = config.getConfig("postgres-BDP")

  val postgresUrlBDP = postgresConfigBDP.getString("url")
  val postgresUserBDP = postgresConfigBDP.getString("user")
  val postgresPasswordBDP = postgresConfigBDP.getString("password")

  val redisHostBDP = redisConfigBDP.getString("host")
  val redisPortBDP = redisConfigBDP.getInt("port")
  val tryPasswordBDP = redisConfigBDP.getString("password")
  val redisPasswordBDP: Option[String] = if (tryPasswordBDP.nonEmpty) Some(tryPasswordBDP) else None

  val redisConfigTEST = config.getConfig("redis-TEST")
  val postgresConfigTEST = config.getConfig("postgres-TEST")

  val postgresUrlTEST = postgresConfigTEST.getString("url")
  val postgresUserTEST = postgresConfigTEST.getString("user")
  val postgresPasswordTEST = postgresConfigTEST.getString("password")

  val redisHostTEST = redisConfigTEST.getString("host")
  val redisPortTEST = redisConfigTEST.getInt("port")
  val tryPasswordTEST = redisConfigTEST.getString("password")
  val redisPasswordTEST: Option[String] = if (tryPasswordTEST.nonEmpty) Some(tryPasswordTEST) else None

  val postgresUrl: String = postgresUrlBDP
  val postgresUser: String = postgresUserBDP
  val postgresPassword: String = postgresPasswordBDP

  val redisHost: String = redisHostBDP
  val redisPort: Int = redisPortBDP
  val redisPassword: Option[String] = redisPasswordBDP

  //  val localIPAddress = InetAddress.getLocalHost.getHostAddress
  //  val productionIPList = List("120.27.162.201", "120.27.163.41")
  //  if (productionIPList.contains(localIPAddress)) {
  //    println("Configuration Of Production")
  //    postgresUrl = postgresUrlBDP
  //    postgresUser = postgresUserBDP
  //    postgresPassword = postgresPasswordBDP
  //    redisHost = redisHostBDP
  //    redisPort = redisPortBDP
  //    redisPassword = redisPasswordBDP
  //  } else {
  //    println("Configuration Of Test")
  //    postgresUrl = postgresUrlTEST
  //    postgresUser = postgresUserTEST
  //    postgresPassword = postgresPasswordTEST
  //    redisHost = redisHostTEST
  //    redisPort = redisPortTEST
  //    redisPassword = redisPasswordTEST
  //  }
  //
  //  postgresUrl = postgresUrlBDP
  //  postgresUser = postgresUserBDP
  //  postgresPassword = postgresPasswordBDP
}
