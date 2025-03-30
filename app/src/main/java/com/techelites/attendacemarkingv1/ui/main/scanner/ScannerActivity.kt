package com.techelites.attendacemarkingv1.ui.main.scanner

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.common.moduleinstall.ModuleInstall
import com.google.android.gms.common.moduleinstall.ModuleInstallRequest
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.techelites.attendacemarkingv1.ui.theme.AuraSecurityTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ScannerActivity : ComponentActivity() {
    private val TAG = "ScannerActivity"
    private val viewModel: ScannerViewModel by viewModels()
    private var hasCameraPermission by mutableStateOf(false)
    private lateinit var scanner: GmsBarcodeScanner
    private var isUserCanceled by mutableStateOf(false)
    private var isScannerActive by mutableStateOf(false)
    private var autoStartScanning by mutableStateOf(true) // Initially true for first launch

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        Log.d(TAG, "Camera permission granted: $isGranted")
        hasCameraPermission = isGranted
        if (isGranted) {
            setupScanner()
        } else {
            viewModel.showError("Camera permission is required for scanning")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get checkpoint and action from intent
        val checkpoint = intent.getStringExtra("checkpoint") ?: "main-gate"
        val action = intent.getStringExtra("action") ?: "entry"

        Log.d(TAG, "ScannerActivity started with checkpoint=$checkpoint, action=$action")

        // Set these values in ViewModel
        viewModel.setCheckpointAndAction(checkpoint, action)

        // Check camera permission before setting up scanner
        checkCameraPermission()

        // Set up navigation event collection
        lifecycleScope.launch {
            viewModel.navigationEvents.collectLatest { event ->
                Log.d(TAG, "Scanner navigation event received: $event")
                when (event) {
                    is ScannerNavigationEvent.RestartScanner -> {
                        Log.d(TAG, "Restart scanner requested, setting autoStartScanning=true")
                        autoStartScanning = true // Allow scanning on next appropriate opportunity
                        if (!isScannerActive) {
                            startScanning()
                        }
                    }
                    is ScannerNavigationEvent.NavigateBack -> {
                        finish()
                    }
                }
            }
        }

        setContent {
            AuraSecurityTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ScannerScreen(
                        viewModel = viewModel,
                        onStartScanning = {
                            Log.d(TAG, "onStartScanning called from ScannerScreen")
                            startScanning()
                        },
                        onNavigateBack = { finish() }
                    )
                }
            }
        }
    }

    private fun checkCameraPermission() {
        Log.d(TAG, "Checking camera permission")
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                Log.d(TAG, "Camera permission already granted")
                hasCameraPermission = true
                setupScanner()
            }
            else -> {
                Log.d(TAG, "Requesting camera permission")
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun setupScanner() {
        Log.d(TAG, "Setting up barcode scanner")
        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_QR_CODE,
                Barcode.FORMAT_AZTEC,
                Barcode.TYPE_TEXT
            )
            .enableAutoZoom() // Enable auto zoom for better scanning
            .build()

        scanner = GmsBarcodeScanning.getClient(this, options)

        // Ensure module is installed
        val moduleInstall = ModuleInstall.getClient(this)
        val moduleInstallRequest = ModuleInstallRequest.newBuilder()
            .addApi(scanner)
            .build()

        Log.d(TAG, "Installing scanner module")
        moduleInstall.installModules(moduleInstallRequest)
            .addOnSuccessListener {
                Log.d(TAG, "Scanner module installed successfully")
                // Ready to scan, but only start automatically if we should
                if (autoStartScanning) {
                    startScanning()
                }
            }
            .addOnFailureListener {
                Log.e(TAG, "Failed to initialize scanner module: ${it.message}", it)
                viewModel.showError("Failed to initialize scanner module: ${it.localizedMessage}")
            }
    }

    private fun startScanning() {
        if (!this::scanner.isInitialized) {
            Log.d(TAG, "Scanner not initialized, setting up")
            setupScanner()
            return
        }

        // Only start if we're not already scanning
        if (isScannerActive) {
            Log.d(TAG, "Scanner is already active, not starting again")
            return
        }

        isScannerActive = true
        Log.d(TAG, "Starting barcode scanner")

        scanner.startScan()
            .addOnSuccessListener { barcode ->
                isScannerActive = false
                autoStartScanning = false // Disable auto-scanning after successful scan
                Log.d(TAG, "Barcode scanned successfully: type=${barcode.valueType}")
                when (barcode.valueType) {
                    Barcode.TYPE_URL, Barcode.TYPE_TEXT -> {
                        val scannedData = barcode.displayValue ?: barcode.url?.url
                        scannedData?.let {
                            Log.d(TAG, "Processing scanned data: $it")
                            viewModel.processScannedData(it)
                        }
                    }
                    else -> {
                        Log.e(TAG, "Unsupported barcode format: ${barcode.valueType}")
                        viewModel.showError("Unsupported barcode format")
                    }
                }
            }
            .addOnCanceledListener {
                isScannerActive = false
                isUserCanceled = true
                Log.d(TAG, "Scanning canceled by user")
                // Don't automatically restart when user cancels
            }
            .addOnFailureListener {
                isScannerActive = false
                autoStartScanning = false // Disable auto-scanning after failure
                Log.e(TAG, "Scan failed: ${it.localizedMessage}", it)
                viewModel.showError("Scan failed: ${it.localizedMessage}")
            }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume called, hasCameraPermission=$hasCameraPermission, scanner initialized=${this::scanner.isInitialized}, isScannerActive=$isScannerActive, autoStartScanning=$autoStartScanning")

        // Only auto-start on resume if we have permission and scanner is initialized
        // AND auto-start flag is true AND scanner isn't already active
        if (hasCameraPermission && this::scanner.isInitialized && autoStartScanning && !isScannerActive) {
            startScanning()
        }
    }

    override fun onPause() {
        super.onPause()
        // Reset scanner active flag when activity is paused
        isScannerActive = false
    }
}