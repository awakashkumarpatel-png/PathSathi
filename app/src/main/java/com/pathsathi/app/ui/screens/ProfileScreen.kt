package com.pathsathi.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.pathsathi.app.R
import com.pathsathi.app.data.local.AppPreferences
import com.pathsathi.app.ui.theme.TealPrimary
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onTermsClick: () -> Unit = {},
    onPrivacyClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var bloodGroup by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var saved by remember { mutableStateOf(false) }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val profile = AppPreferences.profile(context).first()
        name = profile.name
        phone = profile.phone
        bloodGroup = profile.bloodGroup
        note = profile.emergencyNote
        loaded = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.profile_title)) },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(TealPrimary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(44.dp))
                }
            }

            if (loaded) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; saved = false },
                    label = { Text(stringResource(R.string.profile_full_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it; saved = false },
                    label = { Text(stringResource(R.string.profile_phone)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                OutlinedTextField(
                    value = bloodGroup,
                    onValueChange = { bloodGroup = it; saved = false },
                    label = { Text(stringResource(R.string.profile_blood_group)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it; saved = false },
                    label = { Text(stringResource(R.string.profile_emergency_note)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 3
                )

                Button(
                    onClick = {
                        scope.launch {
                            AppPreferences.saveProfile(
                                context,
                                AppPreferences.ProfileData(name, phone, bloodGroup, note)
                            )
                            saved = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                ) {
                    Text(stringResource(R.string.action_save_profile))
                }
                if (saved) {
                    Text(stringResource(R.string.profile_saved), color = TealPrimary, style = MaterialTheme.typography.bodySmall)
                }
                Text(
                    stringResource(R.string.profile_privacy_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onTermsClick) {
                        Text(stringResource(R.string.legal_terms_link), style = MaterialTheme.typography.bodySmall)
                    }
                    Text("·", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = onPrivacyClick) {
                        Text(stringResource(R.string.legal_privacy_link), style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxWidth().padding(top = 32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}
