package sevynidd.bloomseason.security

import android.content.Context
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.CancellationSignal

/** App-Sperre über die Bildschirmsperre des Geräts: Fingerabdruck oder Gesicht, sonst PIN, Muster oder Passwort. */
object AppLock {
    private const val AUTHENTICATORS =
        BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL

    fun isAvailable(context: Context): Boolean =
        context.getSystemService(BiometricManager::class.java)
            ?.canAuthenticate(AUTHENTICATORS) == BiometricManager.BIOMETRIC_SUCCESS

    /** Ruft [onResult] genau einmal auf: true bei Erfolg, false bei Abbruch oder Fehler. */
    fun authenticate(context: Context, title: String, onResult: (Boolean) -> Unit) {
        val prompt = BiometricPrompt.Builder(context)
            .setTitle(title)
            .setAllowedAuthenticators(AUTHENTICATORS)
            .build()
        var finished = false
        fun finish(success: Boolean) {
            if (finished) return
            finished = true
            onResult(success)
        }
        prompt.authenticate(
            CancellationSignal(),
            context.mainExecutor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = finish(true)

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) = finish(false)
            },
        )
    }
}
