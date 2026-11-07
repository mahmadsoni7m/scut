package tj.soni.editor

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

class SoniApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Default language is Tajik until the user picks another one (persisted by AppCompat).
        if (AppCompatDelegate.getApplicationLocales().isEmpty) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("tg"))
        }
    }
}
