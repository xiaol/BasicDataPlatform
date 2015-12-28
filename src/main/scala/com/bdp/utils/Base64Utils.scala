package com.bdp.utils

/**
 * Created by zhange on 15/10/27.
 *
 */

trait Base64Utils {

  def encodeBase64(email: String): String =
    org.apache.commons.codec.binary.Base64.encodeBase64String(email.getBytes)

  def decodeBase64(encodedUusername: String): String =
    new String(org.apache.commons.codec.binary.Base64.decodeBase64(encodedUusername))
}

object Base64Utils extends Base64Utils

// MD5 with Timestamps
// import java.security.MessageDigest
// val imgMd5 = MessageDigest.getInstance("MD5").digest(s"${getCurrentTimestamps()}$imgUrl".getBytes)
