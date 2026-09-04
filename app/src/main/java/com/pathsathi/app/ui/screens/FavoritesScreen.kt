package com.pathsathi.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.ui.res.stringResource
import com.pathsathi.app.R
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pathsathi.app.data.local.PathSathiDatabase
import com.pathsathi.app.data.repository.TrekRepository

@Composable
fun FavoritesScreen(onBack: () -> Unit, onTrekClick: (String) -> Unit) {
    val context = LocalContext.current
    val dao = remember { PathSathiDatabase.getInstance(context).favoriteDao() }
    val favoriteIds by dao.getAll().collectAsState(initial = emptyList())
    val favoriteTreks = remember(favoriteIds) { TrekRepository.treks.filter { it.id in favoriteIds } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.favorites_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (favoriteTreks.isEmpty()) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.FavoriteBorder, contentDescription = null, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.favorites_empty))
                    Text(stringResource(R.string.favorites_empty_desc), style = MaterialTheme.typography.bodySmall)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(favoriteTreks) { trek ->
                    ElevatedCard(onClick = { onTrekClick(trek.id) }) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(trek.name, fontWeight = FontWeight.Bold)
                                Text(trek.location, style = MaterialTheme.typography.bodySmall)
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null)
                        }
                    }
                }
            }
        }
    }
}
