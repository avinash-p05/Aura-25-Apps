package com.techelites.attendacemarkingv1.ui.main.scanner

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.techelites.attendacemarkingv1.network.models.StudentDetails
import com.techelites.attendacemarkingv1.ui.theme.Blue500
import com.techelites.attendacemarkingv1.ui.theme.Green500
import com.techelites.attendacemarkingv1.ui.theme.Red500
import com.techelites.attendacemarkingv1.ui.theme.Orange500
import kotlinx.coroutines.flow.collectLatest
import org.json.JSONObject

private const val TAG = "ScannerScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    viewModel: ScannerViewModel,
    onStartScanning: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val state = viewModel.state

    LaunchedEffect(key1 = true) {
        Log.d(TAG, "ScannerScreen LaunchedEffect called")
        viewModel.navigationEvents.collectLatest { event ->
            Log.d(TAG, "Navigation event received in ScannerScreen: $event")
            when (event) {
                is ScannerNavigationEvent.RestartScanner -> {
                    Log.d(TAG, "Restarting scanner from ScannerScreen")
                    onStartScanning()
                }
                is ScannerNavigationEvent.NavigateBack -> {
                    Log.d(TAG, "Navigating back from ScannerScreen")
                    onNavigateBack()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val action = if (state.action == "entry") "Entry" else "Exit"
                    val checkpoint = if (state.checkpoint == "main-gate") "Main Gate" else "Concert Area"
                    Text("$checkpoint - $action")
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // QR Code scanner UI
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(64.dp)
                    )

                    // Scanner animation
                    ScannerAnimation()
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = if (state.action == "entry") "Scan for Entry" else "Scan for Exit",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Position the QR code within the frame",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Added a manual scan button
                Button(
                    onClick = {
                        Log.d(TAG, "Scan button clicked")
                        onStartScanning()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = null
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = "Start Scanning",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(16.dp)
                            .size(48.dp)
                    )

                    Text(
                        text = "Processing...",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }

                AnimatedVisibility(
                    visible = state.error != null,
                    enter = fadeIn(animationSpec = tween(300)),
                    exit = fadeOut(animationSpec = tween(300))
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(top = 16.dp)
                    ) {
                        // Parse the error message if it's in JSON format
                        val errorMessage = try {
                            val jsonError = JSONObject(state.error ?: "")
                            if (jsonError.has("message")) jsonError.getString("message") else state.error
                        } catch (e: Exception) {
                            state.error
                        } ?: "Unknown error"

                        ErrorDialog(
                            errorMessage = errorMessage,
                            onDismiss = {
                                viewModel.clearError()
                            },
                            onTryScan = {
                                viewModel.clearError()
                                onStartScanning()
                            }
                        )
                    }
                }
            }

            // Student details dialog
            if (state.showStudentDetailsDialog && state.studentDetails != null) {
                Log.d(TAG, "Showing student details dialog: ${state.studentDetails}")
                StudentDetailsDialog(
                    studentDetails = state.studentDetails,
                    action = state.action,
                    onDismiss = {
                        Log.d(TAG, "Student details dialog dismissed")
                        viewModel.dismissStudentDetailsDialog()
                    },
                    onScanAgain = {
                        Log.d(TAG, "Student details dialog dismissed, requesting to restart scanner")
                        viewModel.dismissStudentDetailsDialog()
                        onStartScanning()
                    }
                )
            }
        }
    }
}

@Composable
fun ErrorDialog(
    errorMessage: String,
    onDismiss: () -> Unit,
    onTryScan: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Status icon
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Red500.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        tint = Red500,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Error",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Two buttons: OK and Scan Again
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                    ) {
                        Text(text = "OK")
                    }

                    Button(
                        onClick = onTryScan,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                    ) {
                        Text(text = "Retry")
                    }
                }
            }
        }
    }
}

@Composable
fun ScannerAnimation() {
    val infiniteTransition = rememberInfiniteTransition()
    val animatedY = infiniteTransition.animateFloat(
        initialValue = -100f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .offset(y = animatedY.value.dp)
                .background(Blue500)
        )
    }
}

@Composable
fun StudentDetailsDialog(
    studentDetails: StudentDetails,
    action: String,
    onDismiss: () -> Unit,
    onScanAgain: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Determine if this is a warning or success based on the message content
                val isWarning = studentDetails.message.contains("already")
                        || studentDetails.message.contains("before")
                        || studentDetails.message.contains("No re-entry allowed")

                val isSuccess = !isWarning

                val iconColor = when {
                    isWarning -> Orange500
                    else -> Green500
                }

                val iconBackgroundColor = when {
                    isWarning -> Orange500.copy(alpha = 0.1f)
                    else -> Green500.copy(alpha = 0.1f)
                }

                val titleText = when {
                    isWarning -> "Notice"
                    action == "entry" -> "Entry Successful"
                    else -> "Exit Successful"
                }

                // Status icon
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(iconBackgroundColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when {
                            isWarning -> Icons.Default.Warning
                            action == "entry" -> Icons.Default.Login
                            else -> Icons.Default.Logout
                        },
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = titleText,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = studentDetails.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Show user type badge
                if (studentDetails.userType.isNotEmpty()) {
                    val userTypeColor = when (studentDetails.userType) {
                        "FACULTY" -> Color(0xFF4CAF50) // Green for faculty
                        "STUDENT_EXTERNAL" -> Color(0xFF2196F3) // Blue for external students
                        "GIT_STUDENT" -> Color(0xFF9C27B0) // Purple for GIT students
                        else -> MaterialTheme.colorScheme.primary
                    }

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = userTypeColor.copy(alpha = 0.1f)
                        ),
                        border = BorderStroke(1.dp, userTypeColor),
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        Text(
                            text = when (studentDetails.userType) {
                                "FACULTY" -> "Faculty"
                                "STUDENT_EXTERNAL" -> "External Student"
                                "GIT_STUDENT" -> "GIT Student"
                                else -> studentDetails.userType.replace("_", " ").lowercase()
                                    .split(" ")
                                    .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
                            },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            color = userTypeColor,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Faculty specific information
                if (studentDetails.userType == "FACULTY") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Faculty",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Entry count info
                        if (studentDetails.additionalInfo.containsKey("entryCount")) {
                            InfoRow(
                                label = "Entry Count",
                                value = studentDetails.additionalInfo["entryCount"] ?: "0"
                            )
                        }

                        // Remaining entries info
                        if (studentDetails.additionalInfo.containsKey("remainingEntries")) {
                            InfoRow(
                                label = "Remaining Entries",
                                value = studentDetails.additionalInfo["remainingEntries"] ?: "0"
                            )
                        }

                        // Status info
                        if (studentDetails.additionalInfo.containsKey("status")) {
                            InfoRow(
                                label = "Status",
                                value = studentDetails.additionalInfo["status"]?.replace("_", " ")
                                    ?.lowercase()
                                    ?.split(" ")
                                    ?.joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
                                    ?: ""
                            )
                        }

                        // Location info
                        if (studentDetails.additionalInfo.containsKey("location")) {
                            InfoRow(
                                label = "Location",
                                value = studentDetails.additionalInfo["location"]?.replace("_", " ")
                                    ?.lowercase()
                                    ?.split(" ")
                                    ?.joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
                                    ?: ""
                            )
                        }
                    }
                }
                // Student information - only show if name is not empty
                else if (studentDetails.name.isNotEmpty()) {
                    // Student photo (if available)
                    if (studentDetails.photoUrl.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                                .background(Color.LightGray),
                            contentAlignment = Alignment.Center
                        ) {
                            val securePhotoUrl = if (!studentDetails.photoUrl.startsWith("https://")) {
                                studentDetails.photoUrl.replace("http://", "https://")
                            } else {
                                studentDetails.photoUrl
                            }

                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(securePhotoUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Student Photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    } else {
                        // Show placeholder for missing photo
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(60.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Student information - name and college are available in the log you shared
                    Text(
                        text = studentDetails.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    // Only show college if available
                    if (studentDetails.college.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = studentDetails.college,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Only show department and other details if available
                    if (studentDetails.department.isNotEmpty() || studentDetails.year > 0 || studentDetails.section.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))

                        if (studentDetails.department.isNotEmpty()) {
                            Text(
                                text = studentDetails.department,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                        }

                        if (studentDetails.year > 0 || studentDetails.section.isNotEmpty()) {
                            val yearSection = buildString {
                                if (studentDetails.year > 0) append("Year: ${studentDetails.year}")
                                if (studentDetails.year > 0 && studentDetails.section.isNotEmpty()) append(", ")
                                if (studentDetails.section.isNotEmpty()) append("Section: ${studentDetails.section}")
                            }

                            if (yearSection.isNotEmpty()) {
                                Text(
                                    text = yearSection,
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    // Only show UID and USN if available
                    if (studentDetails.uid?.isNotEmpty() == true || studentDetails.usn?.isNotEmpty() == true) {
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (studentDetails.uid?.isNotEmpty() == true) {
                                Text(
                                    text = "UID: ${studentDetails.uid}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(end = if (studentDetails.usn?.isNotEmpty() == true) 8.dp else 0.dp)
                                )
                            }

                            if (studentDetails.usn?.isNotEmpty() == true) {
                                Text(
                                    text = "USN: ${studentDetails.usn}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Two buttons: OK and Scan Again
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                    ) {
                        Text(text = "OK")
                    }

                    Button(
                        onClick = onScanAgain,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                    ) {
                        Text(text = "Continue")
                    }
                }
            }
        }
    }
}

@Composable
fun InfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 6.dp)
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}