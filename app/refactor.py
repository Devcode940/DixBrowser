with open('src/main/java/com/example/BrowserScreen.kt', 'r') as f:
    content = f.read()

# Let's find where SettingsItem starts and replace from line 1386 to the end with a clean helper composable and correct calls
target_block = """@Composable
fun SettingsItem(icon: ImageVector, title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = title, tint = Color.Black, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(title, color = Color.Black, fontSize = 16.sp)
    }"""

replacement_block = """@Composable
fun SettingsItem(icon: ImageVector, title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = title, tint = Color.Black, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(title, color = Color.Black, fontSize = 16.sp)
    }
}

@Composable
fun BrowserDialogs(
    activeDialog: String?,
    onDismissDialog: () -> Unit,
    onConfirmDialog: () -> Unit,
    showFindInPage: Boolean,
    findQuery: String,
    onFindQueryChange: (String) -> Unit,
    onFindNext: () -> Unit,
    onCloseFind: () -> Unit,
    showPageSource: Boolean,
    pageSourceText: String,
    onClosePageSource: () -> Unit,
    showMemo: Boolean,
    memoText: String,
    onMemoChange: (String) -> Unit,
    onSaveMemo: () -> Unit,
    onCloseMemo: () -> Unit,
    showImageGallery: Boolean,
    onCloseImageGallery: () -> Unit,
    surfaceColor: Color,
    textColor: Color,
    activeTab: BrowserTab,
    context: android.content.Context
) {
    if (activeDialog != null) {
        AlertDialog(
            onDismissRequest = onDismissDialog,
            title = { Text(activeDialog ?: "", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    when (activeDialog) {
                        "Rate app" -> {
                            var rating by remember { mutableStateOf(5) }
                            Text("Please rate your experience with Dix Browser:")
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                                for (i in 1..5) {
                                    IconButton(onClick = { rating = i }) {
                                        Icon(
                                            imageVector = if (i <= rating) Icons.Default.Star else Icons.Default.StarBorder,
                                            contentDescription = "$i stars",
                                            tint = Color(0xFFFFC107)
                                        )
                                    }
                                }
                            }
                        }
                        "Send feedback" -> {
                            var feedbackText by remember { mutableStateOf("") }
                            OutlinedTextField(
                                value = feedbackText,
                                onValueChange = { feedbackText = it },
                                label = { Text("Your feedback") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        "Information" -> {
                            Text("Dix Browser v2.5.0\\nFast, secure, and feature-rich browsing experience.\\n© 2026 Dix Inc.")
                        }
                        "Password Manager" -> {
                            Text("No saved passwords. Saved login credentials will appear here securely encrypted.")
                        }
                        else -> {
                            Text("Configure settings for $activeDialog.")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onConfirmDialog) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissDialog) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showFindInPage) {
        Surface(
            modifier = Modifier.fillMaxWidth().zIndex(20f),
            color = surfaceColor,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = findQuery,
                    onValueChange = onFindQueryChange,
                    placeholder = { Text("Find in page...") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                IconButton(onClick = onFindNext) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Next")
                }
                IconButton(onClick = onCloseFind) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        }
    }

    if (showPageSource) {
        AlertDialog(
            onDismissRequest = onClosePageSource,
            title = { Text("Page Source") },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth().height(300.dp)) {
                    item {
                        Text(pageSourceText, fontSize = 12.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onClosePageSource) { Text("Close") }
            }
        )
    }

    if (showMemo) {
        AlertDialog(
            onDismissRequest = onCloseMemo,
            title = { Text("Quick Memo") },
            text = {
                OutlinedTextField(
                    value = memoText,
                    onValueChange = onMemoChange,
                    label = { Text("Type notes here...") },
                    modifier = Modifier.fillMaxWidth().height(200.dp)
                )
            },
            confirmButton = {
                TextButton(onClick = onSaveMemo) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = onCloseMemo) { Text("Cancel") }
            }
        )
    }

    if (showImageGallery) {
        AlertDialog(
            onDismissRequest = onCloseImageGallery,
            title = { Text("Image Viewer") },
            text = {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    Text("No extracted images found on this page.", color = textColor.copy(alpha = 0.6f))
                }
            },
            confirmButton = {
                TextButton(onClick = onCloseImageGallery) { Text("Close") }
            }
        )
    }
}
"""

if target_block in content:
    # Remove everything from target_block to the end of file, then append replacement_block
    base_content = content[:content.find(target_block)]
    new_content = base_content + replacement_block
    
    with open('src/main/java/com/example/BrowserScreen.kt', 'w') as f:
        f.write(new_content)
    print("Refactored BrowserScreen dialogs into BrowserDialogs composable")
else:
    print("Target block not found")
