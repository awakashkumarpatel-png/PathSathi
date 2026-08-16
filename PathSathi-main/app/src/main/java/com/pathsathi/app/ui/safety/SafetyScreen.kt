package com.pathsathi.app.ui.safety

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pathsathi.app.R
import com.pathsathi.app.data.repository.DemoDataProvider
import com.pathsathi.app.ui.common.SourceBadge
import com.pathsathi.app.ui.theme.PsDanger

@Composable
fun SafetyScreen(vm: SafetyViewModel = viewModel()) {
    val context = LocalContext.current
    val contacts by vm.contacts.collectAsState()
    val emergencyInfo = remember { DemoDataProvider.emergencyInfo("your area") }

    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var relation by remember { mutableStateOf("") }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Text(stringResource(R.string.safety_title), style = MaterialTheme.typography.headlineMedium) }

        item {
            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:112"))
                    context.startActivity(intent)
                },
                colors = ButtonDefaults.buttonColors(containerColor = PsDanger),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) { Text(stringResource(R.string.safety_sos) + " — Call 112") }
        }

        item { Text("Emergency information", style = MaterialTheme.typography.titleMedium) }
        items(emergencyInfo) { info ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(info.name, style = MaterialTheme.typography.titleMedium)
                    Text("${info.category.replaceFirstChar { it.uppercase() }} · ${info.phone}", style = MaterialTheme.typography.bodyMedium)
                    Text(info.notes, style = MaterialTheme.typography.labelSmall)
                    SourceBadge(info.source)
                }
            }
        }

        item { Text(stringResource(R.string.emergency_contacts), style = MaterialTheme.typography.titleMedium) }
        items(contacts) { c ->
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors()) {
                Row(Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(c.name, style = MaterialTheme.typography.titleMedium)
                        Text("${c.relation} · ${c.phone}", style = MaterialTheme.typography.bodyMedium)
                    }
                    Button(onClick = {
                        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${c.phone}")))
                    }) { Text("Call") }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Add a trusted contact", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = relation, onValueChange = { relation = it }, label = { Text("Relation") }, modifier = Modifier.fillMaxWidth())
                Button(onClick = {
                    vm.addTrustedContact(name, phone, relation)
                    name = ""; phone = ""; relation = ""
                }, modifier = Modifier.fillMaxWidth()) { Text("Save Contact") }
            }
        }
    }
}
