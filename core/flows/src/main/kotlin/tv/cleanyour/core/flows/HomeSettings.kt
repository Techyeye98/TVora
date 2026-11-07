package tv.cleanyour.core.flows

import kotlinx.coroutines.sync.Mutex
import kotlinx.serialization.json.*
import tv.cleanyour.core.model.*
import tv.cleanyour.core.spec.*
import tv.cleanyour.core.store.DocumentStore
import java.util.concurrent.ConcurrentHashMap

// One last-successful document per TV, used until a newer TV snapshot is available.
internal class HomeSettings(private val documents: DocumentStore) {
    private val locks = ConcurrentHashMap<TvId, Mutex>()
    fun lock(id: TvId): Mutex = locks.getOrPut(id) { Mutex() }
    private fun path(id: TvId) = "tvs/${id.value}/launcher-settings.json"

    suspend fun current(id: TvId, snapshot: Snapshot?): LauncherConfigDoc? {
        val saved = documents.read(path(id)) { Json.parseToJsonElement(it.decodeToString()).jsonObject }
        val at = saved?.get("atMs")?.jsonPrimitive?.long ?: Long.MIN_VALUE
        val observed = saved?.get("snapshot")?.jsonPrimitive?.contentOrNull
        // A different snapshot is a later observation; wall-clock changes cannot make our own write look older.
        val newerRead = snapshot != null && if (observed != null) snapshot.contentHash.hex != observed else snapshot.takenAtMs > at
        val bytes = if (newerRead) snapshot?.launcherOverlay
            else saved?.get("config")?.toString()?.encodeToByteArray() ?: snapshot?.launcherOverlay
        return bytes?.let(::decode)
    }

    suspend fun remember(id: TvId, doc: LauncherConfigDoc, atMs: Long, snapshot: Snapshot? = null) {
        val value = buildJsonObject {
            put("atMs", atMs)
            snapshot?.let { put("snapshot", it.contentHash.hex) }
            put("config", Json.parseToJsonElement(LauncherConfigCodec.encode(doc).decodeToString()))
        }
        documents.write(path(id), value.toString().encodeToByteArray())
    }

    suspend fun clear(id: TvId) = documents.delete(path(id))

    companion object {
        fun decode(bytes: ByteArray): LauncherConfigDoc = when (val decoded = LauncherConfigCodec.decode(bytes)) {
            is DecodeResult.Ok -> decoded.doc
            is DecodeResult.Rejected -> error("The current launcher settings cannot be safely edited")
        }
    }
}

internal enum class HomeEdit { LOOK, DOCK, PERSONAL, CARDS }

internal object HomeSettingsPatch {
    // A look owns appearance and layout; remote, network, app, clock and motion settings are separate.
    private val lookKeys = setOf("background_color", "wallpaper", "wallpaper_dim", "columns", "rows_below_dock",
        "sky_intensity", "cinematic_home", "ambient_glow", "glow_mode", "dock_opacity", "dock_glass", "tile_style", "focus_style")

    fun merge(current: LauncherConfigDoc, built: LauncherConfigDoc, edit: HomeEdit,
              before: HomePersonalization, after: HomePersonalization, dock: List<PackageName>): LauncherConfigDoc {
        val source = Json.parseToJsonElement(LauncherConfigCodec.encode(built).decodeToString()).jsonObject
        val kept = Json.parseToJsonElement(LauncherConfigCodec.encode(current).decodeToString()).jsonObject.toMutableMap()
        val keys = when (edit) {
            HomeEdit.LOOK -> lookKeys
            HomeEdit.DOCK -> setOf("dock_apps")
            HomeEdit.CARDS -> setOf("module_cards")
            HomeEdit.PERSONAL -> buildSet {
                if (before.hidden != after.hidden) add("hidden_apps")
                if (before.inputs != after.inputs) add("show_inputs_pill")
                if (before.network != after.network) add("show_network_pill")
                if (before.bluetooth != after.bluetooth) add("show_bluetooth_pill")
                if (before.arrange != after.arrange) add("reorder_button")
                if (before.wallpaperEnabled != after.wallpaperEnabled || before.photo != after.photo) add("wallpaper")
                if (before.dim != after.dim || before.photo != after.photo) add("wallpaper_dim")
            }
        }
        for (key in keys) { kept.remove(key); source[key]?.let { kept[key] = it } }
        if (edit == HomeEdit.PERSONAL && (before.dim != after.dim || before.photo != after.photo))
            kept["wallpaper_dim"] = JsonPrimitive(after.dim)
        val merged = HomeSettings.decode(JsonObject(kept).toString().encodeToByteArray())
        return if (edit == HomeEdit.DOCK || (edit == HomeEdit.PERSONAL && before.hidden != after.hidden))
            DockLayers.applyTo(merged, dock.filterNot { it in merged.hiddenApps.orEmpty() }) else merged
    }

    fun personal(doc: LauncherConfigDoc?, saved: HomePersonalization): HomePersonalization {
        if (doc == null) return saved
        fun bool(key: String) = (doc.unknown[key] as? JsonPrimitive)?.booleanOrNull
        return saved.copy(hidden = doc.hiddenApps ?: saved.hidden, inputs = bool("show_inputs_pill") ?: saved.inputs,
            network = bool("show_network_pill") ?: saved.network, bluetooth = bool("show_bluetooth_pill") ?: saved.bluetooth,
            arrange = doc.reorderButton ?: saved.arrange, wallpaperEnabled = doc.wallpaper ?: saved.wallpaperEnabled,
            dim = doc.wallpaperDim?.coerceIn(0, 90) ?: saved.dim)
    }
}
