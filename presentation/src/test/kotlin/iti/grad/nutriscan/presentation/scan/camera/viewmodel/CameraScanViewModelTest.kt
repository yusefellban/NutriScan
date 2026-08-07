package iti.grad.nutriscan.presentation.scan.camera.viewmodel

import app.cash.turbine.test
import io.mockk.every
import io.mockk.mockk
import iti.grad.nutriscan.domain.scan.usecase.DeleteSavedScanUseCase
import iti.grad.nutriscan.domain.scan.usecase.GetSavedScansUseCase
import iti.grad.nutriscan.domain.scan.usecase.GetScanResultUseCase
import iti.grad.nutriscan.domain.scan.usecase.SaveScanUseCase
import iti.grad.nutriscan.domain.scan.usecase.SubmitScanImageUseCase
import iti.grad.nutriscan.presentation.scan.camera.state.CameraScanEffect
import iti.grad.nutriscan.presentation.scan.camera.state.CameraScanEvent
import iti.grad.nutriscan.presentation.scan.camera.state.ScanInputMode
import iti.grad.presentation.R
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CameraScanViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var submitScanImageUseCase: SubmitScanImageUseCase
    private lateinit var getScanResultUseCase: GetScanResultUseCase
    private lateinit var saveScanUseCase: SaveScanUseCase
    private lateinit var deleteSavedScanUseCase: DeleteSavedScanUseCase
    private lateinit var getSavedScansUseCase: GetSavedScansUseCase

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        submitScanImageUseCase = mockk(relaxed = true)
        getScanResultUseCase = mockk(relaxed = true)
        saveScanUseCase = mockk(relaxed = true)
        deleteSavedScanUseCase = mockk(relaxed = true)
        getSavedScansUseCase = mockk()

        every { getSavedScansUseCase.invoke() } returns flowOf(emptyList())
    }

    @AfterEach
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state defaults to photo mode`() {
        val viewModel = buildViewModel()

        assertEquals(ScanInputMode.PHOTO, viewModel.state.value.selectedMode)
        assertTrue(viewModel.state.value.isScanning)
        assertFalse(viewModel.state.value.permissionDenied)
    }

    @Test
    fun `mode selected updates state`() {
        val viewModel = buildViewModel()

        viewModel.onEvent(CameraScanEvent.ModeSelected(ScanInputMode.GALLERY))

        assertEquals(ScanInputMode.GALLERY, viewModel.state.value.selectedMode)
    }

    @Test
    fun `center action emits take picture in photo mode`() = runTest {
        val viewModel = buildViewModel()

        viewModel.effect.test(timeout = 5.seconds) {
            assertTrue(awaitItem() is CameraScanEffect.RequestCameraPermission)

            viewModel.onEvent(CameraScanEvent.CenterActionClicked)

            assertTrue(awaitItem() is CameraScanEffect.TakePicture)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `center action emits take picture in qr mode`() = runTest {
        val viewModel = buildViewModel()
        viewModel.onEvent(CameraScanEvent.ModeSelected(ScanInputMode.QR))

        viewModel.effect.test(timeout = 5.seconds) {
            assertTrue(awaitItem() is CameraScanEffect.RequestCameraPermission)

            viewModel.onEvent(CameraScanEvent.CenterActionClicked)

            assertTrue(awaitItem() is CameraScanEffect.TakePicture)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `center action emits open gallery picker in gallery mode`() = runTest {
        val viewModel = buildViewModel()
        viewModel.onEvent(CameraScanEvent.ModeSelected(ScanInputMode.GALLERY))

        viewModel.effect.test(timeout = 5.seconds) {
            assertTrue(awaitItem() is CameraScanEffect.RequestCameraPermission)

            viewModel.onEvent(CameraScanEvent.CenterActionClicked)

            assertTrue(awaitItem() is CameraScanEffect.OpenGalleryPicker)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `gallery cancel emits cancellation snackbar`() = runTest {
        val viewModel = buildViewModel()

        viewModel.effect.test(timeout = 5.seconds) {
            assertTrue(awaitItem() is CameraScanEffect.RequestCameraPermission)

            viewModel.onEvent(CameraScanEvent.GalleryPickCancelled)

            val effect = awaitItem()
            assertTrue(effect is CameraScanEffect.ShowSnackBarRes)
            assertEquals(R.string.scan_gallery_pick_cancelled, (effect as CameraScanEffect.ShowSnackBarRes).messageResId)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `permission false sets denied state`() {
        val viewModel = buildViewModel()

        viewModel.onEvent(CameraScanEvent.PermissionResult(false))

        assertTrue(viewModel.state.value.permissionDenied)
        assertFalse(viewModel.state.value.hasCameraPermission)
    }

    @Test
    fun `permission true clears denied state`() {
        val viewModel = buildViewModel()

        viewModel.onEvent(CameraScanEvent.PermissionResult(true))

        assertFalse(viewModel.state.value.permissionDenied)
        assertTrue(viewModel.state.value.hasCameraPermission)
    }

    private fun buildViewModel(): CameraScanViewModel = CameraScanViewModel(
        submitScanImageUseCase = submitScanImageUseCase,
        getScanResultUseCase = getScanResultUseCase,
        saveScanUseCase = saveScanUseCase,
        deleteSavedScanUseCase = deleteSavedScanUseCase,
        getSavedScansUseCase = getSavedScansUseCase,
    )
}
