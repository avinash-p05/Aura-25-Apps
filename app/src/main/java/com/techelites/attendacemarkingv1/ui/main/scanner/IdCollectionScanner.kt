package com.techelites.attendacemarkingv1.ui.main.scanner

import android.Manifest
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.common.moduleinstall.ModuleInstall
import com.google.android.gms.common.moduleinstall.ModuleInstallRequest
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.techelites.attendacemarkingv1.ui.main.MainViewModel
import com.techelites.attendacemarkingv1.ui.theme.Blue500
import com.techelites.attendacemarkingv1.ui.theme.Green500
import com.techelites.attendacemarkingv1.ui.theme.Orange500
import com.techelites.attendacemarkingv1.ui.theme.Red500
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun IdCollectionScanner(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()

    // Permission handling
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    var showPermissionRequest by remember { mutableStateOf(false) }

    // Scanner setup
    val options = GmsBarcodeScannerOptions.Builder()
        .setBarcodeFormats(
            Barcode.FORMAT_QR_CODE,
            Barcode.FORMAT_AZTEC,
            Barcode.TYPE_TEXT
        )
        .build()
    val scanner = remember { GmsBarcodeScanning.getClient(context, options) }

    // Check and request camera permission
    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }

        // Install scanner module
        val moduleInstall = ModuleInstall.getClient(context)
        val moduleInstallRequest = ModuleInstallRequest.newBuilder()
            .addApi(scanner)
            .build()

        moduleInstall.installModules(moduleInstallRequest)
    }

    // Student details dialog state
    var showStudentDialog by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.showStudentDetails) {
        if (uiState.showStudentDetails && uiState.studentInfo != null) {
            showStudentDialog = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ID Collection") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Orange500,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            // Main content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Scanner visual
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Orange500.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Badge,
                        contentDescription = null,
                        tint = Orange500,
                        modifier = Modifier.size(80.dp)
                    )

                    // Animated scanner line (using a different color from attendance scanner)
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        val infiniteTransition = rememberInfiniteTransition(label = "scanner_animation")
                        val animatedY = infiniteTransition.animateFloat(
                            initialValue = -100f,
                            targetValue = 100f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(2000, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "scanner_line"
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .offset(y = animatedY.value.dp)
                                .background(Orange500)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "Scan Student ID",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Position the QR code within the frame to collect ID",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Status message
                AnimatedVisibility(
                    visible = uiState.errorMessage.isNotBlank() || uiState.successMessage.isNotBlank(),
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (uiState.errorMessage.isNotBlank())
                                Red500.copy(alpha = 0.1f) else Green500.copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (uiState.errorMessage.isNotBlank())
                                    Icons.Default.Error else Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (uiState.errorMessage.isNotBlank()) Red500 else Green500
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = if (uiState.errorMessage.isNotBlank())
                                    uiState.errorMessage else uiState.successMessage,
                                color = if (uiState.errorMessage.isNotBlank()) Red500 else Green500,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = {
                            scope.launch {
                                viewModel.setEndpointType("id-collection")
                                viewModel.setAction("collect")
                                startScanningForId(scanner, viewModel)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Orange500
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Collect ID")
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                viewModel.setEndpointType("id-collection")
                                viewModel.setAction("return")
                                startScanningForId(scanner, viewModel)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Blue500
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.AssignmentReturn,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Return ID")
                    }
                }
            }

            // Loading indicator
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = Orange500)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Processing...",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }
        }
    }

    // Student details dialog - reuse from AttendanceScanner but with slight UI differences
    if (showStudentDialog && uiState.studentInfo != null) {
        Dialog(onDismissRequest = {
            showStudentDialog = false
            viewModel.dismissStudentDetails()
            // Start a new scan after dismissing
            scope.launch {
                delay(500) // Short delay before starting next scan
                startScanningForId(scanner, viewModel)
            }
        }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Status indicator - use different color for ID collection
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Orange500.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (uiState.currentAction == "collect")
                                Icons.Default.Badge else Icons.Default.AssignmentReturned,
                            contentDescription = null,
                            tint = Orange500,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = if (uiState.currentAction == "collect") "ID Collected" else "ID Returned",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = uiState.successMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Student details
                    Text(
                        text = uiState.studentInfo!!.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "${uiState.studentInfo!!.college} - ${uiState.studentInfo!!.department}",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "UID: ${uiState.studentInfo!!.uid}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 8.dp)
                        )

                        Text(
                            text = "USN: ${uiState.studentInfo!!.usn}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            showStudentDialog = false
                            viewModel.dismissStudentDetails()
                            scope.launch {
                                delay(500)
                                startScanningForId(scanner, viewModel)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Orange500
                        )
                    ) {
                        Text("Continue Scanning")
                    }
                }
            }
        }
    }

    // Permission request dialog
    if (showPermissionRequest) {
        AlertDialog(
            onDismissRequest = { showPermissionRequest = false },
            title = { Text("Camera Permission Required") },
            text = {
                Text("The camera permission is required to scan QR codes. Please grant the permission to continue.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPermissionRequest = false
                        cameraPermissionState.launchPermissionRequest()
                    }
                ) {
                    Text("Request Permission")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showPermissionRequest = false
                        onNavigateBack()
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

private suspend fun startScanningForId(scanner: GmsBarcodeScanner, viewModel: MainViewModel) {
    try {
        scanner.startScan()
            .addOnSuccessListener { barcode ->
                val scannedData = when (barcode.valueType) {
                    Barcode.TYPE_URL -> barcode.url?.url
                    Barcode.TYPE_TEXT -> barcode.displayValue
                    else -> barcode.displayValue
                }

                scannedData?.let {
                    viewModel.processScannedData(it)
                }
            }
    } catch (e: Exception) {
        // Handle scanning errors
    }
}