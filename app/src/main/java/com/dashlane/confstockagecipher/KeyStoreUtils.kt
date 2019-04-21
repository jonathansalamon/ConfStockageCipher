package com.dashlane.confstockagecipher

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey

/**
 * Utilities functions for KeyStore
 * @author Jonathan Salamon
 */
object KeyStoreUtils {
    fun createKey(alias: String): KeyPair {
        val generator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, "AndroidKeyStore")
        val builder = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_ECB)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1)
        generator.initialize(builder.build())
        return generator.generateKeyPair()
    }

    fun getSavedKey(alias: String): KeyPair {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        val privateKey = keyStore.getKey(alias, null) as PrivateKey
        val publicKey = keyStore.getCertificate(alias).publicKey
        return KeyPair(publicKey, privateKey)
    }
}