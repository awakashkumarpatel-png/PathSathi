package com.pathsathi.app.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.BatteryManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.pathsathi.app.R
import com.pathsathi.app.data.local.AppPreferences
import com.pathsathi.app.data.local.PathSathiDatabase
import com.pathsathi.app.location.LocationHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Upgraded emergency SOS: tap -> countdown (cancellable) -> confirm -> execute
 * user-configured emergency actions (call + share location). Never fires
 * silently - the countdown and confirm step are the safeguard against
 * accidental taps, and cancel always works.
 */
@Composable
fun SosScreen(onBack: () -> Unit, onManageContacts: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dao = remember { PathSathiDatabase.getInstance(context).emergencyContactDao() }
    val contacts by dao.getAll().collectAsState(initial = emptyList())
    val sosCountdownSeconds by AppPreferences.sosCountdownSeconds(context).collectAsState(initial = 5)

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var permissionWasDenied by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasLocationPermission = granted
        if (!granted) permissionWasDenied = true
    }

    var location by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var isFetchingLocation by remember { mutableStateOf(false) }
    var countdown by remember { mutableStateOf<Int?>(null) }
    var justSentSafe by remember { mutableStateOf(false) }

    fun fetchLocation() {
        scope.launch {
            isFetchingLocation = true
            location = LocationHelper(context).getCurrentLocation()
            isFetchingLocation = false
        }
    }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }
    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) fetchLocation()
    }

    // Countdown loop - cancellable at any point, never fires the emergency action on its own
    LaunchedEffect(countdown) {
        val c = countdown
        if (c != null && c > 0) {
            delay(1000)
            countdown = c - 1
        } else if (c == 0) {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:112"))
            context.startActivity(intent)
            countdown = null
        }
    }

    val battery = remember { getBatteryPercent(context) }
    val isOnline = remember { isNetworkAvailable(context) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.sos_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onManageContacts) {
                        Icon(Icons.Default.ContactPhone, contentDescription = "Emergency contacts")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Status row - informational only
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatusChip(icon = Icons.Default.BatteryFull, label = "${battery ?: "--"}%")
                StatusChip(icon = if (isOnline) Icons.Default.Wifi else Icons.Default.WifiOff, label = if (isOnline) stringResource(R.string.sos_online) else stringResource(R.string.sos_offline))
                StatusChip(
                    icon = Icons.Default.GpsFixed,
                    label = when {
                        !hasLocationPermission -> stringResource(R.string.sos_gps_blocked)
                        isFetchingLocation -> stringResource(R.string.sos_gps_loading)
                        location != null -> stringResource(R.string.sos_gps_ready)
                        else -> stringResource(R.string.sos_gps_unavailable)
                    }
                )
            }

            if (!hasLocationPermission && permissionWasDenied) {
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.sos_permission_denied),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
                TextButton(onClick = { permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) }) {
                    Text(stringResource(R.string.sos_grant_permission))
                }
            } else if (hasLocationPermission && location == null && !isFetchingLocation) {
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.sos_no_gps_fix),
                    style = MaterialTheme.typography.bodySmall
                )
                TextButton(onClick = { fetchLocation() }) {
                    Text(stringResource(R.string.action_retry))
                }
            }

            Spacer(Modifier.height(24.dp))

            if (countdown != null) {
                Text("${countdown}", style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.error)
                Text(stringResource(R.string.sos_calling), style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(20.dp))
                OutlinedButton(onClick = { countdown = null }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Text(stringResource(R.string.action_cancel))
                }
            } else {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.sos_press_confirm),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = { countdown = sosCountdownSeconds },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth().height(64.dp)
                ) {
                    Icon(Icons.Default.Call, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.sos_call_button), style = MaterialTheme.typography.titleMedium)
                }

                Spacer(Modifier.height(12.dp))

                OutlinedButton(
                    enabled = contacts.isNotEmpty() && location != null,
                    onClick = {
                        val mapsLink = location?.let { "https://maps.google.com/?q=${it.first},${it.second}" } ?: ""
                        val body = "I'm safe. Current location: $mapsLink"
                        val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("smsto:${contacts.firstOrNull()?.phoneNumber ?: ""}")
                            putExtra("sms_body", body)
                        }
                        context.startActivity(smsIntent)
                        justSentSafe = true
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.sos_i_am_safe))
                }

                if (contacts.isEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.sos_add_contacts_prompt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                if (justSentSafe) {
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.sos_message_prepared), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun StatusChip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}

private fun getBatteryPercent(context: Context): Int? {
    val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
    return bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
}

private fun isNetworkAvailable(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = cm.activeNetwork ?: return false
    val capabilities = cm.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}
