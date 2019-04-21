package com.dashlane.confstockagecipher

import android.util.Base64
import android.util.Log
import kotlinx.coroutines.yield
import okio.*
import java.io.File
import java.io.InputStream
import java.security.Key
import javax.crypto.Cipher

/**
 * Utilities function for all Cipher/Decipher operations
 * @author Jonathan Salamon
 */
object CipherUtils {

    fun cipherData(cipher: Cipher, data: String, key: Key): String {
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val bytes = cipher.doFinal(data.toByteArray())
        return Base64.encodeToString(bytes, Base64.DEFAULT)
    }

    fun decipherData(cipher: Cipher, data: String, key: Key): String {
        cipher.init(Cipher.DECRYPT_MODE, key)
        val cipheredData = Base64.decode(data, Base64.DEFAULT)
        val decipheredData = cipher.doFinal(cipheredData)
        return String(decipheredData)
    }

    suspend fun cipherFile(cipher: Cipher, inputStream: InputStream, outputFile: File, key: Key): File {
        cipher.init(Cipher.ENCRYPT_MODE, key)
        // Cipher
        writeBigFile(cipher, inputStream.source(), outputFile.sink(), 1024L)
        return outputFile
    }

    suspend fun decipherFile(cipher: Cipher, inputFile: File, outputFile: File, key: Key): File {
        cipher.init(Cipher.DECRYPT_MODE, key)
        // Decipher
        writeBigFile(cipher, inputFile.source(), outputFile.sink(), 1024L)
        return outputFile
    }

    private suspend fun writeBigFile(cipher: Cipher, input: Source, output: Sink, chunkSize: Long) {
        input.buffer().use { source ->
            output.buffer().use { out ->
                while (source.request(chunkSize)) {
                    Log.d("Chunk Cipher", "Handling chunk of $chunkSize bytes")
                    yield()
                    out.write(cipher.update(source.readByteArray(chunkSize)))
                }
                yield()
                val lastBytes = source.readByteArray()
                Log.d("Chunk Cipher", "Handling chunk of ${lastBytes.size} bytes")
                out.write(cipher.update(lastBytes))
                yield()
                val finalBytes = cipher.doFinal()
                Log.d("Chunk Cipher", "Finishing with ${finalBytes.size} bytes")
                out.write(finalBytes)
                Log.d("Chunk Cipher", "Done")
            }
        }
    }
}