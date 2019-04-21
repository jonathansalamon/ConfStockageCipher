package com.dashlane.confstockagecipher

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.security.*
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

/**
 * Demo of principles explained in
 * https://docs.google.com/presentation/d/17Hk6ktZ9iL0XzXeqtzsmyakaEW6PPI5yRNgjxKUaDLg/edit?usp=sharing
 *
 * @author Jonathan Salamon
 */
class MainActivity : AppCompatActivity() {

    /**
     * Directory where we will store files, and where files can be resolved by our FileProvider
     */
    private val filesDirectory by lazy { File(cacheDir, "file_provider").apply { mkdirs() } }

    /**
     * Simple extension function to hash a String (SHA-256, SHA-128, etc.)
     */
    private fun String.hash(type: String) = MessageDigest.getInstance(type).digest(this.toByteArray())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Simplified to not use IV, ECB should not be used in production
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        val masterPasswordHash = "MyMasterPassword".hash("SHA-256")
        val key = SecretKeySpec(masterPasswordHash, "AES")
        // Alias used to create and restore key from KeyStore
        val alias = "DemoKeyAlias"

        // Cipher/Decipher a simple text
        decipher_text_button.isEnabled = false
        cipher_text_button.setOnClickListener {
            text_input.text?.toString()?.let {
                val ciphered = if(keyStore_checkbox.isChecked) {
                   cipherWithKeyStore(alias, it)
                } else {
                    CipherUtils.cipherData(cipher, it, key)
                }
                Log.d("Cipher", ciphered)
                textView.text = ciphered
                decipher_text_button.isEnabled = true
            }
        }

        decipher_text_button.setOnClickListener {
            val ciphered = textView.text.toString()
            val deciphered = if(keyStore_checkbox.isChecked) {
                decipherWithKeyStore(alias, ciphered)
            } else {
                CipherUtils.decipherData(cipher, ciphered, key)
            }
            Log.d("Cipher", deciphered)
            textView.text = deciphered
            decipher_text_button.isEnabled = false
        }

        // File Ciphering/Deciphering
        file_cipher_decipher_button.setOnClickListener {
            it.isEnabled = false
            // You should use a proper scope with cancellation, it has been simplified here
            GlobalScope.launch(Dispatchers.Main) {
                val inputStream = assets.open("rome.jpg")
                val outputFile = File(filesDirectory, "rome.dash")
                // Cipher a file, chunk by chunk
                val cipheredFile = CipherUtils.cipherFile(cipher, inputStream, outputFile, key)
                // Decipher a file and open it using a FileProvider
                openFile(cipheredFile, File(filesDirectory, "rome.jpg"), cipher, key)
                it.isEnabled = true
            }
        }
    }

    private fun cipherWithKeyStore(alias: String, data: String): String {
        // KeyStore demo
        KeyStoreUtils.createKey(alias)
        val savedKey = KeyStoreUtils.getSavedKey(alias)
        Log.d("KeyStore", "Algorithm of Public Key found is ${savedKey.public.algorithm}")

        // Cipher a key with another stored key
        val cipherKeyStore = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        return CipherUtils.cipherData(cipherKeyStore, data, savedKey.public)
    }

    private fun decipherWithKeyStore(alias: String, data: String): String {
        // KeyStore demo
        val savedKey = KeyStoreUtils.getSavedKey(alias)
        val cipherKeyStore = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        return CipherUtils.decipherData(cipherKeyStore, data, savedKey.private)
    }

    private suspend fun openFile(cipheredFile: File, outputFile: File, cipher: Cipher, key: Key) {
        val file = CipherUtils.decipherFile(cipher, cipheredFile, outputFile, key)
        val uri = FileProvider.getUriForFile(this@MainActivity, "$packageName.fileprovider", file)
        val openIntent = Intent(Intent.ACTION_VIEW).apply {
            type = contentResolver.getType(uri)
            data = uri
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        if (openIntent.resolveActivity(packageManager) != null) {
            startActivity(openIntent)
        }
    }
}
