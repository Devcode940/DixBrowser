package com.example.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PasswordCredential
import com.example.data.PasswordSecurity

/**
 * Data holder for pending save password prompt.
 */
data class PendingSaveCredential(
    val siteTitle: String,
    val domain: String,
    val username: String,
    val rawPassword: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordManagerSheet(
    credentials: List<PasswordCredential>,
    currentDomain: String,
    onDismiss: () -> Unit,
    onSaveCredential: (siteTitle: String, domain: String, username: String, rawPassword: String, notes: String) -> Unit,
    onDeleteCredential: (Int) -> Unit,
    onDecryptPassword: (PasswordCredential) -> String,
    onAutoFillRequested: (username: String, rawPassword: String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Vault, 1: Generator, 2: Add Manual
    var editingCredential by remember { mutableStateOf<PasswordCredential?>(null) }
    var showDeleteConfirmId by remember { mutableStateOf<Int?>(null) }

    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    // Generator state
    var genLength by remember { mutableFloatStateOf(16f) }
    var genUppercase by remember { mutableStateOf(true) }
    var genNumbers by remember { mutableStateOf(true) }
    var genSymbols by remember { mutableStateOf(true) }
    var generatedPassword by remember {
        mutableStateOf(PasswordSecurity.generatePassword(16, true, true, true))
    }

    // Manual Add State
    var addTitle by remember { mutableStateOf(if (currentDomain.isNotBlank()) currentDomain else "") }
    var addDomain by remember { mutableStateOf(currentDomain) }
    var addUsername by remember { mutableStateOf("") }
    var addPassword by remember { mutableStateOf("") }
    var addNotes by remember { mutableStateOf("") }
    var addPasswordVisible by remember { mutableStateOf(false) }

    val filteredCredentials = remember(credentials, searchQuery) {
        if (searchQuery.isBlank()) credentials
        else credentials.filter {
            it.domain.contains(searchQuery, ignoreCase = true) ||
            it.siteTitle.contains(searchQuery, ignoreCase = true) ||
            it.username.contains(searchQuery, ignoreCase = true)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        scrimColor = Color.Black.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .fillMaxHeight(0.85f)
        ) {
            // Sheet Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF89B4FA).copy(alpha = 0.2f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.VpnKey,
                                contentDescription = "Password Vault",
                                tint = Color(0xFF89B4FA),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Password Manager",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Security,
                                contentDescription = null,
                                tint = Color(0xFFA6E3A1),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Local AES-256 Encrypted Vault",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFA6E3A1)
                            )
                        }
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            // Navigation Tabs
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Vault (${credentials.size})") },
                    icon = { Icon(Icons.Default.Lock, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Generator") },
                    icon = { Icon(Icons.Default.Casino, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Add New") },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (selectedTab) {
                0 -> {
                    // TAB 0: VAULT LIST
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        placeholder = { Text("Search logins by site, domain, username...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (filteredCredentials.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Outlined.Lock,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (searchQuery.isBlank()) "No passwords saved yet" else "No matching credentials found",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (searchQuery.isBlank()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(onClick = { selectedTab = 2 }) {
                                        Icon(Icons.Default.Add, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Add First Login")
                                    }
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredCredentials, key = { it.id }) { item ->
                                PasswordCredentialCard(
                                    credential = item,
                                    isCurrentDomainMatch = currentDomain.isNotBlank() && item.domain.contains(currentDomain, ignoreCase = true),
                                    onDecryptPassword = { onDecryptPassword(item) },
                                    onCopyUsername = {
                                        clipboard.setText(AnnotatedString(item.username))
                                        Toast.makeText(context, "Username copied to clipboard", Toast.LENGTH_SHORT).show()
                                    },
                                    onCopyPassword = {
                                        val pass = onDecryptPassword(item)
                                        clipboard.setText(AnnotatedString(pass))
                                        Toast.makeText(context, "Password copied to clipboard", Toast.LENGTH_SHORT).show()
                                    },
                                    onAutoFill = {
                                        val pass = onDecryptPassword(item)
                                        onAutoFillRequested(item.username, pass)
                                        onDismiss()
                                    },
                                    onDelete = { showDeleteConfirmId = item.id }
                                )
                            }
                        }
                    }
                }

                1 -> {
                    // TAB 1: GENERATOR
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = generatedPassword,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                Row {
                                    IconButton(onClick = {
                                        generatedPassword = PasswordSecurity.generatePassword(
                                            genLength.toInt(), genUppercase, genNumbers, genSymbols
                                        )
                                    }) {
                                        Icon(Icons.Default.Refresh, contentDescription = "Regenerate")
                                    }
                                    IconButton(onClick = {
                                        clipboard.setText(AnnotatedString(generatedPassword))
                                        Toast.makeText(context, "Generated password copied!", Toast.LENGTH_SHORT).show()
                                    }) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                                    }
                                }
                            }
                        }

                        // Strength indicator
                        val strength = remember(generatedPassword) {
                            when {
                                generatedPassword.length >= 16 && genUppercase && genNumbers && genSymbols -> "Very Strong"
                                generatedPassword.length >= 12 -> "Strong"
                                generatedPassword.length >= 8 -> "Good"
                                else -> "Weak"
                            }
                        }
                        val strengthColor = remember(strength) {
                            when (strength) {
                                "Very Strong" -> Color(0xFFA6E3A1)
                                "Strong" -> Color(0xFF89B4FA)
                                "Good" -> Color(0xFFF9E2AF)
                                else -> Color(0xFFF38BA8)
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Password Strength: ", style = MaterialTheme.typography.bodyMedium)
                            Text(strength, color = strengthColor, fontWeight = FontWeight.Bold)
                        }

                        // Length Slider
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Length: ${genLength.toInt()} characters")
                            }
                            Slider(
                                value = genLength,
                                onValueChange = {
                                    genLength = it
                                    generatedPassword = PasswordSecurity.generatePassword(
                                        genLength.toInt(), genUppercase, genNumbers, genSymbols
                                    )
                                },
                                valueRange = 8f..32f,
                                steps = 24
                            )
                        }

                        // Option Switches
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Include Uppercase (A-Z)")
                            Switch(
                                checked = genUppercase,
                                onCheckedChange = {
                                    genUppercase = it
                                    generatedPassword = PasswordSecurity.generatePassword(
                                        genLength.toInt(), genUppercase, genNumbers, genSymbols
                                    )
                                }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Include Numbers (0-9)")
                            Switch(
                                checked = genNumbers,
                                onCheckedChange = {
                                    genNumbers = it
                                    generatedPassword = PasswordSecurity.generatePassword(
                                        genLength.toInt(), genUppercase, genNumbers, genSymbols
                                    )
                                }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Include Symbols (!@#$)")
                            Switch(
                                checked = genSymbols,
                                onCheckedChange = {
                                    genSymbols = it
                                    generatedPassword = PasswordSecurity.generatePassword(
                                        genLength.toInt(), genUppercase, genNumbers, genSymbols
                                    )
                                }
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        Button(
                            onClick = {
                                clipboard.setText(AnnotatedString(generatedPassword))
                                Toast.makeText(context, "Password copied!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Copy Password")
                        }
                    }
                }

                2 -> {
                    // TAB 2: MANUAL ADD CREDENTIAL
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = addTitle,
                            onValueChange = { addTitle = it },
                            label = { Text("Site / Application Name") },
                            placeholder = { Text("e.g. GitHub, Google, Amazon") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = addDomain,
                            onValueChange = { addDomain = it },
                            label = { Text("Domain / URL") },
                            placeholder = { Text("e.g. github.com") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = addUsername,
                            onValueChange = { addUsername = it },
                            label = { Text("Username / Email") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = addPassword,
                            onValueChange = { addPassword = it },
                            label = { Text("Password") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            visualTransformation = if (addPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                Row {
                                    IconButton(onClick = {
                                        addPassword = PasswordSecurity.generatePassword(16, true, true, true)
                                        addPasswordVisible = true
                                    }) {
                                        Icon(Icons.Default.AutoAwesome, contentDescription = "Generate")
                                    }
                                    IconButton(onClick = { addPasswordVisible = !addPasswordVisible }) {
                                        Icon(
                                            if (addPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = "Toggle password visibility"
                                        )
                                    }
                                }
                            }
                        )

                        OutlinedTextField(
                            value = addNotes,
                            onValueChange = { addNotes = it },
                            label = { Text("Notes (Optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        Button(
                            onClick = {
                                if (addUsername.isBlank() || addPassword.isBlank()) {
                                    Toast.makeText(context, "Username and password cannot be empty", Toast.LENGTH_SHORT).show()
                                } else {
                                    onSaveCredential(
                                        addTitle.ifBlank { addDomain },
                                        addDomain,
                                        addUsername,
                                        addPassword,
                                        addNotes
                                    )
                                    Toast.makeText(context, "Saved to Password Vault!", Toast.LENGTH_SHORT).show()
                                    addUsername = ""
                                    addPassword = ""
                                    addNotes = ""
                                    selectedTab = 0
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save to Vault")
                        }
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    showDeleteConfirmId?.let { targetId ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirmId = null },
            title = { Text("Delete Credential?") },
            text = { Text("Are you sure you want to permanently delete this saved password?") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteCredential(targetId)
                    showDeleteConfirmId = null
                    Toast.makeText(context, "Credential deleted", Toast.LENGTH_SHORT).show()
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmId = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun PasswordCredentialCard(
    credential: PasswordCredential,
    isCurrentDomainMatch: Boolean,
    onDecryptPassword: () -> String,
    onCopyUsername: () -> Unit,
    onCopyPassword: () -> Unit,
    onAutoFill: () -> Unit,
    onDelete: () -> Unit
) {
    var passwordVisible by remember { mutableStateOf(false) }
    var decryptedPassword by remember { mutableStateOf("") }

    val cardBorderColor = if (isCurrentDomainMatch) Color(0xFF89B4FA) else Color.Transparent

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentDomainMatch) Color(0xFF89B4FA).copy(alpha = 0.12f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        border = if (isCurrentDomainMatch) androidx.compose.foundation.BorderStroke(1.5.dp, cardBorderColor) else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = credential.siteTitle.take(1).uppercase(),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = credential.siteTitle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = credential.domain,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row {
                    if (isCurrentDomainMatch) {
                        FilledTonalButton(
                            onClick = onAutoFill,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Auto-Fill", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Username Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = credential.username,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
                IconButton(onClick = onCopyUsername, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy username", modifier = Modifier.size(14.dp))
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Password Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Key,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (passwordVisible) decryptedPassword else "••••••••••••",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = if (passwordVisible) FontFamily.Monospace else FontFamily.Default,
                        fontWeight = FontWeight.Medium
                    )
                }
                Row {
                    IconButton(
                        onClick = {
                            if (!passwordVisible) {
                                decryptedPassword = onDecryptPassword()
                            }
                            passwordVisible = !passwordVisible
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Toggle password",
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(onClick = onCopyPassword, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy password", modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}

/**
 * In-browser banner floating prompt to save credentials after submitting a form.
 */
@Composable
fun SavePasswordPromptBanner(
    pendingSave: PendingSaveCredential,
    onSave: () -> Unit,
    onNever: () -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.inverseSurface,
        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF89B4FA),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.VpnKey,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Save password for ${pendingSave.domain}?",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = pendingSave.username,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.8f)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onNever) {
                    Text("Never", color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.7f))
                }
                Button(
                    onClick = onSave,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF89B4FA), contentColor = Color.Black)
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AutoFillPromptBanner(
    domain: String,
    credentials: List<PasswordCredential>,
    onAutoFill: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shadowElevation = 8.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.VpnKey, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Auto-fill for $domain",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(16.dp))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            LazyColumn(modifier = Modifier.heightIn(max = 150.dp)) {
                items(credentials) { cred ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onAutoFill(cred.username, PasswordSecurity.decrypt(cred.encryptedPassword))
                            }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = cred.username, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text(text = "Tap to fill password", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                        }
                        Icon(Icons.Default.Bolt, contentDescription = "Auto-fill", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}
