package com.example.secapp.data.local.security

import android.util.Base64
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.MGF1ParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PSource
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object CryptoHelper {
    private const val RSA_ALGORITHM = "RSA"
    private const val RSA_TRANSFORMATION = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding"
    private const val AES_TRANSFORMATION = "AES/GCM/NoPadding"
    private const val AES_KEY_ALGORITHM = "AES"

    fun generateRsaKeyPair(keySize: Int = 2048): KeyPair {
        val generator = KeyPairGenerator.getInstance(RSA_ALGORITHM)
        generator.initialize(keySize)
        return generator.generateKeyPair()
    }

    fun toBase64(bytes: ByteArray): String = Base64.encodeToString(bytes, Base64.NO_WRAP)

    fun fromBase64(value: String): ByteArray {
        // Remove any newlines/whitespace that Java Base64 encoder might have added
        val cleanValue = value.replace("\n", "").replace("\r", "").replace(" ", "")
        return Base64.decode(cleanValue, Base64.NO_WRAP)
    }

    fun rsaPublicKeyFromBase64(value: String): PublicKey {
        val encoded = fromBase64(value)
        return KeyFactory.getInstance(RSA_ALGORITHM).generatePublic(X509EncodedKeySpec(encoded))
    }

    fun rsaPrivateKeyFromBase64(value: String): PrivateKey {
        val encoded = fromBase64(value)
        return KeyFactory.getInstance(RSA_ALGORITHM).generatePrivate(PKCS8EncodedKeySpec(encoded))
    }

    fun decryptRsaOaep(cipherTextBase64: String, privateKey: PrivateKey): String {
        val cipherText = fromBase64(cipherTextBase64)
        val decrypted = runCatching {
            decryptRsaOaep(cipherText, privateKey, MGF1ParameterSpec.SHA1)
        }.getOrElse {
            decryptRsaOaep(cipherText, privateKey, MGF1ParameterSpec.SHA256)
        }
        return String(decrypted, Charsets.UTF_8)
    }

    private fun decryptRsaOaep(
        cipherText: ByteArray,
        privateKey: PrivateKey,
        mgfDigest: MGF1ParameterSpec
    ): ByteArray {
        val cipher = Cipher.getInstance(RSA_TRANSFORMATION)
        val spec = OAEPParameterSpec(
            "SHA-256",
            "MGF1",
            mgfDigest,
            PSource.PSpecified.DEFAULT
        )
        cipher.init(Cipher.DECRYPT_MODE, privateKey, spec)
        return cipher.doFinal(cipherText)
    }

    fun encryptAesGcm(plainText: ByteArray, secretKey: SecretKeySpec, iv: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(AES_TRANSFORMATION)
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec)
        return cipher.doFinal(plainText)
    }

    fun decryptAesGcm(cipherText: ByteArray, secretKey: SecretKeySpec, iv: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(AES_TRANSFORMATION)
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
        return cipher.doFinal(cipherText)
    }

    fun deriveAesKeyFromPin(pin: String, salt: ByteArray, iterations: Int, keyLength: Int): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(pin.toCharArray(), salt, iterations, keyLength)
        return SecretKeySpec(factory.generateSecret(spec).encoded, AES_KEY_ALGORITHM)
    }

    fun generateRandomBytes(length: Int): ByteArray = ByteArray(length).also { java.security.SecureRandom().nextBytes(it) }

    fun privateKeyToBase64(privateKey: PrivateKey): String = toBase64(privateKey.encoded)

    fun publicKeyToBase64(publicKey: PublicKey): String = toBase64(publicKey.encoded)
}
