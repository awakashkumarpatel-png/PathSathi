package com.pathsathi.app.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

private val imageHttpClient by lazy { OkHttpClient() }

/**
 * Loads and displays a bitmap from [url] with a loading spinner and a
 * graceful broken-image fallback - never crashes on a bad URL, timeout, or
 * decode failure. No third-party image library dependency; uses OkHttp
 * (already in the project) and Android's built-in BitmapFactory.
 */
@Composable
fun NetworkImage(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    val state = produceState<Result<ImageBitmap>?>(initialValue = null, url) {
        value = if (url.isNullOrBlank()) {
            Result.failure(Exception("No image URL"))
        } else {
            withContext(Dispatchers.IO) {
                try {
                    val request = Request.Builder().url(url).build()
                    imageHttpClient.newCall(request).execute().use { response ->
                        val bytes = response.body?.bytes()
                        val bitmap = bytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
                        if (bitmap != null) Result.success(bitmap.asImageBitmap())
                        else Result.failure(Exception("Could not decode image"))
                    }
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }
        }
    }

    Box(modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
        val result = state.value
        when {
            result == null -> CircularProgressIndicator()
            result.isSuccess -> Image(
                bitmap = result.getOrThrow(),
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            else -> Icon(
                Icons.Default.BrokenImage,
                contentDescription = "Image unavailable",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
