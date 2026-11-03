package sevynidd.bloomseason.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private const val KEY_ALIAS = "bloomseason_db_key"
private const val PREFS = "db_key"
private const val PREF_VALUE = "wrapped"
private const val PASSPHRASE_BYTES = 32
private const val IV_BYTES = 12

/**
 * Das Passwort der Datenbank ist eine Zufallszahl. Sie liegt nur verpackt mit einem Schlüssel aus dem
 * Android Keystore auf dem Gerät, dieser Schlüssel verlässt den Keystore nie.
 */
object DatabaseKey {
    fun passphrase(context: Context): ByteArray {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.getString(PREF_VALUE, null)?.let { return unwrap(Base64.decode(it, Base64.NO_WRAP)) }
        val fresh = ByteArray(PASSPHRASE_BYTES).also(SecureRandom()::nextBytes)
        prefs.edit().putString(PREF_VALUE, Base64.encodeToString(wrap(fresh), Base64.NO_WRAP)).commit()
        return fresh
    }

    internal fun wrap(plain: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, keystoreKey())
        return cipher.iv + cipher.doFinal(plain)
    }

    internal fun unwrap(bytes: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, keystoreKey(), GCMParameterSpec(128, bytes, 0, IV_BYTES))
        return cipher.doFinal(bytes, IV_BYTES, bytes.size - IV_BYTES)
    }

    private fun keystoreKey(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }
}
