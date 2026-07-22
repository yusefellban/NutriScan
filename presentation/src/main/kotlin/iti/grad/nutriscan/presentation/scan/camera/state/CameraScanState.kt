package iti.grad.nutriscan.presentation.scan.camera.state

import iti.grad.nutriscan.presentation.common.model.BottomNavTab

data class CameraScanState(
    val hasCameraPermission: Boolean = false,
    val isScanning: Boolean = true,
    val activeScan: ActiveScanUiModel? = null,
    val selectedTab: BottomNavTab = BottomNavTab.SCAN,
    val permissionDenied: Boolean = false,
)
