package com.pathsathi.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pathsathi.app.R
import com.pathsathi.app.data.local.AppLanguage
import com.pathsathi.app.data.local.AppPreferences
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Reads a raw text asset (terms_en/terms_hi/privacy_en/privacy_hi) matching the
 * user's current language setting and renders it as scrollable paragraphs.
 * Long legal text lives as plain-text raw resources rather than strings.xml
 * entries, since Android's string-resource XML collapses literal line breaks -
 * raw text files keep paragraph formatting reliable in both languages. See
 * res/raw/keep.xml for why these specific raw files are exempted from the
 * release resource shrinker (they're looked up dynamically below).
 */
@Composable
private fun LegalDocumentScreen(
    title: String,
    updatedLabel: String,
    baseFileName: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val language by AppPreferences.language(context).collectAsState(initial = AppLanguage.ENGLISH)

    val bodyText = remember(language) {
        val suffix = if (language == AppLanguage.HINDI) "hi" else "en"
        val resId = context.resources.getIdentifier("${baseFileName}_$suffix", "raw", context.packageName)
        try {
            context.resources.openRawResource(resId).use { stream ->
                BufferedReader(InputStreamReader(stream)).readText()
            }
        } catch (e: Exception) {
            "" // Graceful fallback: an empty body is better than a crash if a raw asset is ever missing.
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(updatedLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            Text(bodyText, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun TermsScreen(onBack: () -> Unit) {
    LegalDocumentScreen(
        title = stringResource(R.string.terms_title),
        updatedLabel = stringResource(R.string.terms_updated),
        baseFileName = "terms",
        onBack = onBack
    )
}

@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    LegalDocumentScreen(
        title = stringResource(R.string.privacy_title),
        updatedLabel = stringResource(R.string.privacy_updated),
        baseFileName = "privacy",
        onBack = onBack
    )
}
