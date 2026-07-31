package iti.grad.nutriscan.data.repository

import android.util.Log
import io.mockk.every
import io.mockk.mockkStatic

/**
 * `android.util.Log` is an un-implemented stub on the JVM — any call throws
 * "Method d in android.util.Log not mocked". Repositories that log on their happy path
 * (see FoodLogRepositoryImpl / DailyTrackingRepositoryImpl) therefore need it stubbed
 * before the code under test runs.
 *
 * Deliberately scoped to the test files that need it rather than flipping the module-wide
 * `testOptions.unitTests.isReturnDefaultValues` flag: that flag also silences
 * `android.graphics`, which `UserRepositoryImplTest.uploadAvatar returns failure when the
 * image cannot be processed` relies on throwing.
 */
fun stubAndroidLog() {
    mockkStatic(Log::class)
    every { Log.v(any(), any()) } returns 0
    every { Log.d(any(), any()) } returns 0
    every { Log.i(any(), any()) } returns 0
    every { Log.w(any(), any<String>()) } returns 0
    every { Log.e(any(), any()) } returns 0
    every { Log.e(any(), any(), any()) } returns 0
}
