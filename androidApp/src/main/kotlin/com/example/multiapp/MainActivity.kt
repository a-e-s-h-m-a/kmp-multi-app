package com.example.multiapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.multiapp.domain.AppId

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            App(appId = BuildConfig.APP_ID.toAppId())
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App(appId = AppId.AppOne)
}

private fun String.toAppId(): AppId =
    AppId.fromExternalName(this)
