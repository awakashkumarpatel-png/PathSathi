package com.pathsathi.app.ads

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pathsathi.app.ui.theme.PsSurfaceAlt
import com.pathsathi.app.ui.theme.PsTextSecondary

/**
 * Renders nothing at all unless [orchestrator] actually returns real ad
 * content — no placeholder box, no "ad loading" flicker, no fake content.
 * Only call this from screens listed in [AdSurface]; that enum deliberately
 * has no entry for Safety, Sathi, Map, or Live Trip, so it's not possible to
 * accidentally wire an ad slot into those screens through this component.
 */
@Composable
fun AdSlot(surface: AdSurface, orchestrator: AdsOrchestrator, modifier: Modifier = Modifier) {
    var ad by remember { mutableStateOf<AdContent?>(null) }

    LaunchedEffect(surface) {
        ad = orchestrator.loadAd(surface)
    }

    ad?.let { content ->
        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = PsSurfaceAlt)
        ) {
            Column(Modifier.padding(12.dp)) {
                Text("Sponsored", style = MaterialTheme.typography.labelSmall, color = PsTextSecondary)
                Text(content.headline, style = MaterialTheme.typography.titleMedium)
                Text(content.body, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
