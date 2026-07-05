package com.aeshma.multiapp.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.aeshma.multiapp.core.model.FeatureId
import com.aeshma.multiapp.core.model.ProductId
import com.aeshma.multiapp.ui.ProductApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            ProductApp(
                productId = ProductId.fromExternalName(BuildConfig.PRODUCT_ID),
                buildFeatureBundle = BuildConfig.BUNDLED_FEATURES.toFeatureIds(),
                featureDefinitions = ProductFeatureBundle.featureDefinitions,
            )
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    ProductApp(productId = ProductId.AppOneStandalone)
}

private fun String.toFeatureIds(): Set<FeatureId> =
    split(",")
        .map(String::trim)
        .filter(String::isNotEmpty)
        .map(::FeatureId)
        .toSet()
