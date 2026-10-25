package tv.cleanyour.app

import android.app.Application
import java.io.File
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import tv.cleanyour.android.work.OperationService
import tv.cleanyour.core.flows.OperationNotices
import tv.cleanyour.core.flows.planOrNull

class CytApplication : Application() {
    lateinit var graph: AppGraph
        private set

    override fun onCreate() {
        super.onCreate()
        CytLog.init(File(noBackupFilesDir, "logs"))
        graph = AppGraph(this)
        OperationService.Bus.channelName = getString(R.string.app_channel_setup_name)
        watchRuns()
        graph.sessionEnd.watch(graph.scope)
    }

    // D18: the foreground service follows the engine's runs. A plan that needs it (a transfer, an
    // install or more than 5 steps) starts it; the end of the run stops it within a second.
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun watchRuns() {
        val g = graph
        g.scope.launch {
            g.tvs.tvs.flatMapLatest { list ->
                if (list.isEmpty()) flowOf(emptyList())
                else combine(list.map { tv -> g.seams.engine.runState(tv.id) }) { states ->
                    list.zip(states.toList()).mapNotNull { (tv, s) ->
                        val plan = s.planOrNull() ?: return@mapNotNull null
                        if (!OperationNotices.needsForeground(plan)) null else OperationNotices.of(tv.id, tv.nickname, s, g.copy)
                    }
                }
            }.collect { notices -> OperationService.Bus.publish(this@CytApplication, notices.firstOrNull()) }
        }
    }
}
