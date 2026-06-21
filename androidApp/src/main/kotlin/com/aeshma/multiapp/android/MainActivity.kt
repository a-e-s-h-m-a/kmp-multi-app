package com.aeshma.multiapp.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.aeshma.multiapp.core.model.AppId
import com.aeshma.multiapp.ui.App

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            App(appId = AppId.fromExternalName(BuildConfig.APP_ID))
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App(appId = AppId.AppOne)
}
