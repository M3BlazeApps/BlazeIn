package mn.blazeapps.blazein.data

import android.content.Context
import android.content.SharedPreferences

class UserPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("telegram_prefs", Context.MODE_PRIVATE)

    var apiId: Int
        get() = prefs.getInt(KEY_API_ID, 0)
        set(value) = prefs.edit().putInt(KEY_API_ID, value).apply()

    var apiHash: String
        get() = prefs.getString(KEY_API_HASH, "") ?: ""
        set(value) = prefs.edit().putString(KEY_API_HASH, value).apply()

    var phoneNumber: String
        get() = prefs.getString(KEY_PHONE_NUMBER, "") ?: ""
        set(value) = prefs.edit().putString(KEY_PHONE_NUMBER, value).apply()

    fun hasApiCredentials(): Boolean = apiId != 0 && apiHash.isNotBlank()

    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_API_ID = "api_id"
        private const val KEY_API_HASH = "api_hash"
        private const val KEY_PHONE_NUMBER = "phone_number"
    }
}
