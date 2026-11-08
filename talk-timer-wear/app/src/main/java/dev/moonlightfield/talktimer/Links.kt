package dev.moonlightfield.talktimer

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.wear.remote.interactions.RemoteActivityHelper

object Links {
    /** Must be a public, non-PDF page before release (Google Play User Data policy). */
    const val PRIVACY_POLICY_URL = "https://moonlightfield.dev/apps/talktimer/privacy"

    /** Watches have no full browser, so open the page on the paired phone. */
    fun openOnPhone(context: Context, url: String) {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
            .addCategory(Intent.CATEGORY_BROWSABLE)
        val future = RemoteActivityHelper(context).startRemoteActivity(intent)
        future.addListener({
            val ok = runCatching { future.get() }.isSuccess
            Toast.makeText(
                context,
                if (ok) R.string.opened_on_phone else R.string.phone_unavailable,
                Toast.LENGTH_SHORT,
            ).show()
        }, ContextCompat.getMainExecutor(context))
    }
}
