package com.infomericainc.infotranscribe

import android.app.Application
import com.infomericainc.infotranscribe.api.InfoTranscribe
import com.infomericainc.infotranscribe.util.Constants

class InfoTranscribeApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        //Creates an instance of InfoTranscribe
        InfoTranscribe.initialize(
            this,
            Constants.AZURE_API_KEY,
            Constants.AZURE_REGION
        )
    }
}