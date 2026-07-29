package iti.grad.nutriscan

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.gif.GifDecoder
import coil3.request.crossfade
import iti.grad.nutriscan.notification.NotificationChannels
import iti.grad.nutriscan.notification.NotificationScheduler
import iti.grad.nutriscan.steps.StepsScheduler
import iti.grad.nutriscan.work.DailyTrackingSyncScheduler
import timber.log.Timber

@HiltAndroidApp
class NutriScanApplication : Application(), SingletonImageLoader.Factory, Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        NotificationChannels.createAll(this)
        NotificationScheduler.scheduleAll(this)
        StepsScheduler.schedule(this)
        DailyTrackingSyncScheduler.schedule(this)
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return ImageLoader.Builder(context)
            .components {
                add(OkHttpNetworkFetcherFactory())
                add(GifDecoder.Factory())
            }
            .memoryCache {
                // 25% of available app memory for decoded bitmaps — keeps avatars and
                // other frequently-revisited images (e.g. product photos) instant on
                // back-navigation without over-committing memory on low-end devices.
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.02)
                    .build()
            }
            .crossfade(true)
            .build()
    }
}
