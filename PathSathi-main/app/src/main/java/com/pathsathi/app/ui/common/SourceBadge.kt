package com.pathsathi.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pathsathi.app.R
import com.pathsathi.app.data.model.DataSource
import com.pathsathi.app.ui.theme.PsGreen
import com.pathsathi.app.ui.theme.PsSurfaceAlt
import com.pathsathi.app.ui.theme.PsTurquoise
import com.pathsathi.app.ui.theme.PsWarning

@Composable
fun SourceBadge(source: DataSource, modifier: Modifier = Modifier) {
    val (label, color) = when (source) {
        DataSource.OFFLINE_SAVED -> stringResource(R.string.label_offline_data) to PsTurquoise
        DataSource.DEMO -> stringResource(R.string.label_demo_data) to PsSurfaceAlt
        DataSource.LIVE -> stringResource(R.string.label_live_data) to PsGreen
        DataSource.ESTIMATED -> stringResource(R.string.label_estimated) to PsWarning
    }
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = modifier
            .background(PsSurfaceAlt, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}
