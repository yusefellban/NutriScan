package iti.grad.nutriscan

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.gif.GifDecoder
import iti.grad.nutriscan.work.DailyTrackingSyncScheduler
import timber.log.Timber
import javax.inject.Inject

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
        DailyTrackingSyncScheduler.schedule(this)
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return ImageLoader.Builder(context)
            .components {
                add(OkHttpNetworkFetcherFactory())
                add(GifDecoder.Factory())
            }
            .build()
    }
}
