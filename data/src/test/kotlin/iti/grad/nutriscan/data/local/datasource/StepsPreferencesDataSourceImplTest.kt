package iti.grad.nutriscan.data.local.datasource

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class StepsPreferencesDataSourceImplTest {

    @TempDir
    lateinit var tempDir: File

    private fun dataSource(scope: TestScope): StepsPreferencesDataSourceImpl {
        val store: DataStore<Preferences> = PreferenceDataStoreFactory.create(
            scope = TestScope(StandardTestDispatcher(scope.testScheduler)),
            produceFile = { File(tempDir, "steps_test.preferences_pb") },
        )
        return StepsPreferencesDataSourceImpl(store)
    }

    @Test
    fun `returns the saved total when the date matches`() = runTest {
        val dataSource = dataSource(this)

        dataSource.saveDailySteps(date = "2026-08-06", steps = 4200)

        assertEquals(4200, dataSource.getDailySteps("2026-08-06"))
    }

    @Test
    fun `returns zero when the saved total belongs to another day`() = runTest {
        val dataSource = dataSource(this)

        dataSource.saveDailySteps(date = "2026-08-05", steps = 4200)

        assertEquals(0, dataSource.getDailySteps("2026-08-06"))
    }

    @Test
    fun `returns zero when nothing has ever been saved`() = runTest {
        val dataSource = dataSource(this)

        assertEquals(0, dataSource.getDailySteps("2026-08-06"))
    }
}
