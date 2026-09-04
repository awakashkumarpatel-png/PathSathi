package com.pathsathi.app.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.pathsathi.app.R
import com.pathsathi.app.data.local.OfflineMapEntity
import com.pathsathi.app.data.local.PathSathiDatabase
import com.pathsathi.app.data.map.OfflineMapManager
import com.pathsathi.app.data.model.Trek
import com.pathsathi.app.data.repository.TrekRepository
import com.pathsathi.app.ui.theme.TealPrimary
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.views.MapView

@Composable
fun OfflineMapsScreen(
    onBack: () -> Unit,
    onOpenMap: (String) -> Unit
) {
    val context = LocalContext.current
    val dao = remember { PathSathiDatabase.getInstance(context).offlineMapDao() }
    val scope = rememberCoroutineScope()

    val downloaded by dao.getAll().collectAsState(initial = null)
    val allTreks = remember { TrekRepository.treks }

    var downloadingTrekId by remember { mutableStateOf<String?>(null) }
    var downloadProgress by remember { mutableStateOf(0f) }
    var pendingDelete by remember { mutableStateOf<OfflineMapEntity?>(null) }

    // Headless MapView used only for CacheManager tile operations — never displayed
    val helperMapView = remember {
        Configuration.getInstance().load(
            context,
            context.getSharedPreferences("osmdroid_prefs", Context.MODE_PRIVATE)
        )
        Configuration.getInstance().osmdroidTileCache = context.getExternalFilesDir("osmdroid_tiles")
        MapView(context).apply { setTileSource(TileSourceFactory.MAPNIK) }
    }

    fun startDownload(trek: Trek) {
        downloadingTrekId = trek.id
        downloadProgress = 0f
        val bb = OfflineMapManager.boundingBoxForTrek(trek)
        var totalTiles = 0

        OfflineMapManager.downloadArea(
            context = context,
            mapView = helperMapView,
            bb = bb,
            onProgress = { progress, total ->
                if (total > 0) totalTiles = total
                if (totalTiles > 0) downloadProgress = (progress.toFloat() / totalTiles).coerceIn(0f, 1f)
            },
            onComplete = { success ->
                downloadingTrekId = null
                if (success) {
                    scope.launch {
                        dao.insert(
                            OfflineMapEntity(
                                trekId = trek.id,
                                trekName = trek.name,
                                north = bb.latNorth,
                                south = bb.latSouth,
                                east = bb.lonEast,
                                west = bb.lonWest,
                                minZoom = OfflineMapManager.MIN_ZOOM,
                                maxZoom = OfflineMapManager.MAX_ZOOM,
                                tileCount = totalTiles.toLong()
                            )
                        )
                    }
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.offline_maps_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        val downloadedIds = downloaded?.map { it.trekId }?.toSet() ?: emptySet()

        LazyColumn(
            modifier = Modifier.padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(stringResource(R.string.offline_maps_downloaded), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            when {
                downloaded == null -> item { CircularProgressIndicator() }
                downloaded!!.isEmpty() -> item {
                    Text(
                        stringResource(R.string.offline_maps_none),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                else -> items(downloaded!!, key = { it.id }) { map ->
                    ElevatedCard(shape = RoundedCornerShape(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(map.trekName, fontWeight = FontWeight.Bold)
                                Text(
                                    "${map.tileCount} tiles \u00b7 zoom ${map.minZoom}-${map.maxZoom}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { onOpenMap(map.trekId) }) {
                                Icon(Icons.Default.Map, contentDescription = "Open", tint = TealPrimary)
                            }
                            IconButton(onClick = { pendingDelete = map }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(8.dp)) }
            item {
                Text(stringResource(R.string.offline_maps_available), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            items(allTreks.filter { it.id !in downloadedIds }, key = { it.id }) { trek ->
                ElevatedCard(shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(14.dp)) {
                        Text(trek.name, fontWeight = FontWeight.Bold)
                        Text(
                            trek.location,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(10.dp))
                        if (downloadingTrekId == trek.id) {
                            LinearProgressIndicator(
                                progress = downloadProgress,
                                modifier = Modifier.fillMaxWidth(),
                                color = TealPrimary
                            )
                        } else {
                            OutlinedButton(
                                onClick = { startDownload(trek) },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = downloadingTrekId == null
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.offline_maps_download))
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(60.dp)) }
        }
    }

    pendingDelete?.let { map ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.offline_maps_delete_title)) },
            text = { Text(stringResource(R.string.offline_maps_delete_body, map.trekName)) },
            confirmButton = {
                TextButton(onClick = {
                    val bb = BoundingBox(map.north, map.east, map.south, map.west)
                    OfflineMapManager.deleteArea(context, helperMapView, bb)
                    scope.launch { dao.delete(map) }
                    pendingDelete = null
                }) { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }
}
