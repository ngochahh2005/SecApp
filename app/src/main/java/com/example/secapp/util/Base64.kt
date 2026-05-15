package com.example.secapp.util

import android.util.Base64

fun bytesToBase64(data: ByteArray): String {
    return Base64.encodeToString(data, Base64.DEFAULT)
}

fun base64ToBytes(base64String: String): ByteArray {
    return Base64.decode(base64String, Base64.DEFAULT)
}