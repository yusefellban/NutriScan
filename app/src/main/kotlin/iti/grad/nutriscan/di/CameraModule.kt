package iti.grad.nutriscan.di

import android.content.Context
import androidx.camera.lifecycle.ProcessCameraProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.Executor
import javax.inject.Singleton
import androidx.core.content.ContextCompat
import com.google.common.util.concurrent.ListenableFuture

@Module
@InstallIn(SingletonComponent::class)
object CameraModule {

    @Provides
    @Singleton
    fun provideProcessCameraProviderFuture(
        @ApplicationContext context: Context,
    ): ListenableFuture<ProcessCameraProvider> =
        ProcessCameraProvider.getInstance(context)

    @Provides
    @Singleton
    fun provideCameraExecutor(
        @ApplicationContext context: Context,
    ): Executor = ContextCompat.getMainExecutor(context)
}
