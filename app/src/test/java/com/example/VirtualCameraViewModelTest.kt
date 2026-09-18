package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.model.AspectRatioMode
import com.example.model.CameraLens
import com.example.model.CameraSourceMode
import com.example.viewmodel.VirtualCameraViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class VirtualCameraViewModelTest {

    private lateinit var viewModel: VirtualCameraViewModel

    @Before
    fun setup() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        viewModel = VirtualCameraViewModel(application)
    }

    @Test
    fun initialState_hasSensibleDefaults() {
        val state = viewModel.uiState.value
        assertEquals(CameraSourceMode.VIRTUAL_FEED, state.sourceMode)
        assertEquals(CameraLens.BACK, state.selectedLens)
        assertFalse(state.isPlaying)
        assertTrue(state.isLooping)
        assertFalse(state.isMuted)
        assertEquals(1.0f, state.playbackSpeed, 0.001f)
        assertEquals(AspectRatioMode.FIT, state.aspectRatioMode)
        assertNull(state.errorMessage)
    }

    @Test
    fun toggleSourceMode_switchesBetweenPhysicalAndVirtual() {
        assertEquals(CameraSourceMode.VIRTUAL_FEED, viewModel.uiState.value.sourceMode)
        viewModel.toggleSourceMode()
        assertEquals(CameraSourceMode.PHYSICAL_CAMERA, viewModel.uiState.value.sourceMode)
        viewModel.toggleSourceMode()
        assertEquals(CameraSourceMode.VIRTUAL_FEED, viewModel.uiState.value.sourceMode)
    }

    @Test
    fun toggleLens_switchesBetweenBackAndFront() {
        assertEquals(CameraLens.BACK, viewModel.uiState.value.selectedLens)
        viewModel.toggleLens()
        assertEquals(CameraLens.FRONT, viewModel.uiState.value.selectedLens)
        viewModel.toggleLens()
        assertEquals(CameraLens.BACK, viewModel.uiState.value.selectedLens)
    }

    @Test
    fun toggleLoop_togglesLoopingState() {
        assertTrue(viewModel.uiState.value.isLooping)
        viewModel.toggleLoop()
        assertFalse(viewModel.uiState.value.isLooping)
        viewModel.toggleLoop()
        assertTrue(viewModel.uiState.value.isLooping)
    }

    @Test
    fun toggleMute_togglesMutedState() {
        assertFalse(viewModel.uiState.value.isMuted)
        viewModel.toggleMute()
        assertTrue(viewModel.uiState.value.isMuted)
        viewModel.toggleMute()
        assertFalse(viewModel.uiState.value.isMuted)
    }

    @Test
    fun setPlaybackSpeed_updatesSpeed() {
        viewModel.setPlaybackSpeed(1.5f)
        assertEquals(1.5f, viewModel.uiState.value.playbackSpeed, 0.001f)
        viewModel.setPlaybackSpeed(2.0f)
        assertEquals(2.0f, viewModel.uiState.value.playbackSpeed, 0.001f)
    }

    @Test
    fun toggleAspectRatioMode_switchesFitAndFillCrop() {
        assertEquals(AspectRatioMode.FIT, viewModel.uiState.value.aspectRatioMode)
        viewModel.toggleAspectRatioMode()
        assertEquals(AspectRatioMode.FILL_CROP, viewModel.uiState.value.aspectRatioMode)
        viewModel.toggleAspectRatioMode()
        assertEquals(AspectRatioMode.FIT, viewModel.uiState.value.aspectRatioMode)
    }

    @Test
    fun seekTo_clampsWithinBounds() {
        viewModel.updatePlaybackProgress(0L, 10000L)
        viewModel.seekTo(5000L)
        assertEquals(5000L, viewModel.uiState.value.currentPositionMs)

        // Beyond duration -> clamped
        viewModel.seekTo(20000L)
        assertEquals(10000L, viewModel.uiState.value.currentPositionMs)

        // Below 0 -> clamped
        viewModel.seekTo(-500L)
        assertEquals(0L, viewModel.uiState.value.currentPositionMs)
    }

    @Test
    fun stop_resetsPositionAndStopsPlaying() {
        viewModel.updatePlaybackProgress(4000L, 10000L)
        viewModel.stop()
        assertFalse(viewModel.uiState.value.isPlaying)
        assertEquals(0L, viewModel.uiState.value.currentPositionMs)
    }

    @Test
    fun architectureDialog_togglesProperly() {
        assertFalse(viewModel.uiState.value.showArchitectureDialog)
        viewModel.setShowArchitectureDialog(true)
        assertTrue(viewModel.uiState.value.showArchitectureDialog)
        viewModel.setShowArchitectureDialog(false)
        assertFalse(viewModel.uiState.value.showArchitectureDialog)
    }
}
