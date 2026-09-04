package com.pathsathi.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import com.pathsathi.app.R
import com.pathsathi.app.data.local.AppPreferences
import com.pathsathi.app.ui.theme.GreenAccent
import com.pathsathi.app.ui.theme.TealPrimary
import kotlinx.coroutines.launch

private data class OnboardPage(
    val icon: ImageVector,
    val titleRes: Int,
    val descriptionRes: Int
)

private val pages = listOf(
    OnboardPage(Icons.Default.Landscape, R.string.onboard_title_1, R.string.onboard_desc_1),
    OnboardPage(Icons.Default.MyLocation, R.string.onboard_title_2, R.string.onboard_desc_2),
    OnboardPage(Icons.Default.Emergency, R.string.onboard_title_3, R.string.onboard_desc_3),
    OnboardPage(Icons.Default.WbCloudy, R.string.onboard_title_4, R.string.onboard_desc_4)
)

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    onOpenTerms: () -> Unit = {},
    onOpenPrivacy: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { pages.size + 1 }) // +1 for the permissions page

    var legalAccepted by remember { mutableStateOf(false) }

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasLocationPermission = granted }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasNotificationPermission = granted }

    fun completeOnboarding() {
        scope.launch {
            AppPreferences.setOnboardingComplete(context, true)
            AppPreferences.setLegalAccepted(context, true)
            onFinished()
        }
    }

    Scaffold { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                if (page < pages.size) {
                    OnboardPageContent(pages[page])
                } else {
                    PermissionsPageContent(
                        hasLocationPermission = hasLocationPermission,
                        hasNotificationPermission = hasNotificationPermission,
                        onRequestLocation = { locationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) },
                        onRequestNotification = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        },
                        legalAccepted = legalAccepted,
                        onLegalAcceptedChange = { legalAccepted = it },
                        onOpenTerms = onOpenTerms,
                        onOpenPrivacy = onOpenPrivacy
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(pages.size + 1) { index ->
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .size(if (pagerState.currentPage == index) 10.dp else 8.dp)
                            .clip(CircleShape)
                            .background(if (pagerState.currentPage == index) TealPrimary else Color.LightGray)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (pagerState.currentPage > 0) {
                    TextButton(onClick = {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                    }) { Text(stringResource(R.string.action_back)) }
                } else {
                    TextButton(onClick = { completeOnboarding() }) { Text(stringResource(R.string.action_skip)) }
                }

                Button(
                    onClick = {
                        if (pagerState.currentPage < pages.size) {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        } else {
                            completeOnboarding()
                        }
                    },
                    enabled = pagerState.currentPage < pages.size || legalAccepted,
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                ) {
                    Text(stringResource(if (pagerState.currentPage < pages.size) R.string.action_next else R.string.action_get_started))
                }
            }
        }
    }
}

@Composable
private fun OnboardPageContent(page: OnboardPage) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(TealPrimary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(page.icon, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(56.dp))
        }
        Spacer(Modifier.height(28.dp))
        Text(
            stringResource(page.titleRes),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(page.descriptionRes),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PermissionsPageContent(
    hasLocationPermission: Boolean,
    hasNotificationPermission: Boolean,
    onRequestLocation: () -> Unit,
    onRequestNotification: () -> Unit,
    legalAccepted: Boolean,
    onLegalAcceptedChange: (Boolean) -> Unit,
    onOpenTerms: () -> Unit,
    onOpenPrivacy: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Shield, contentDescription = null, tint = GreenAccent, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(20.dp))
        Text(
            stringResource(R.string.onboard_setup_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.onboard_setup_desc),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(28.dp))

        PermissionRow(
            icon = Icons.Default.LocationOn,
            title = stringResource(R.string.permission_location_title),
            description = stringResource(R.string.permission_location_desc),
            granted = hasLocationPermission,
            onGrant = onRequestLocation
        )
        Spacer(Modifier.height(12.dp))
        PermissionRow(
            icon = Icons.Default.Notifications,
            title = stringResource(R.string.permission_notifications_title),
            description = stringResource(R.string.permission_notifications_desc),
            granted = hasNotificationPermission,
            onGrant = onRequestNotification
        )

        Spacer(Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = legalAccepted,
                onCheckedChange = onLegalAcceptedChange,
                colors = CheckboxDefaults.colors(checkedColor = TealPrimary)
            )
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.legal_accept_prefix), style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        stringResource(R.string.legal_terms_link),
                        style = MaterialTheme.typography.bodySmall,
                        color = TealPrimary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onOpenTerms() }
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.legal_and), style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        stringResource(R.string.legal_privacy_link),
                        style = MaterialTheme.typography.bodySmall,
                        color = TealPrimary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onOpenPrivacy() }
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionRow(
    icon: ImageVector,
    title: String,
    description: String,
    granted: Boolean,
    onGrant: () -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = TealPrimary)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(description, style = MaterialTheme.typography.bodySmall)
            }
            if (granted) {
                Icon(Icons.Default.CheckCircle, contentDescription = "Granted", tint = TealPrimary)
            } else {
                TextButton(onClick = onGrant) { Text(stringResource(R.string.action_allow)) }
            }
        }
    }
}
