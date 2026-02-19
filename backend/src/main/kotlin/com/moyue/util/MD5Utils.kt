package com.moyue.util

import java.security.MessageDigest

object MD5Utils {
    
    fun md5Encode(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
    
    fun md5Encode(input: ByteArray): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(input)
        return digest.joinToString("") { "%02x".format(it) }
    }
    
    fun md5EncodeWithSalt(input: String, salt: String): String {
        return md5Encode(input + salt)
    }
}
