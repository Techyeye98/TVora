package tv.cleanyour.core.flows

import kotlinx.serialization.json.*
import tv.cleanyour.core.model.*
import tv.cleanyour.core.spec.*
import tv.cleanyour.core.store.DocumentStore

// Separate from HomeChoice so cards, theme and dock edits cannot erase personal settings.
data class HomePersonalization(
    val hidden: List<PackageName>? = null,
    val inputs: Boolean? = null,
    val network: Boolean? = null,
    val bluetooth: Boolean? = null,
    val wallpaperEnabled: Boolean? = null,
    val photo: BlobRef? = null,
    val dim: Int = 35,
    val arrange: Boolean? = null,
) {
    init { require(dim in 0..90) }

    fun applyTo(doc: LauncherConfigDoc): LauncherConfigDoc {
        val extra = doc.unknown.toMutableMap()
        inputs?.let { extra["show_inputs_pill"] = JsonPrimitive(it) }
        network?.let { extra["show_network_pill"] = JsonPrimitive(it) }
        bluetooth?.let { extra["show_bluetooth_pill"] = JsonPrimitive(it) }
        return doc.copy(
            hiddenApps = hidden ?: doc.hiddenApps,
            dockApps = doc.dockApps?.filterNot { it in hidden.orEmpty() },
            wallpaper = wallpaperEnabled ?: doc.wallpaper,
            wallpaperDim = if (wallpaperEnabled == true) dim else doc.wallpaperDim,
            reorderButton = arrange != false,
            unknown = extra,
        )
    }
}

internal class PersonalHomePrefs(private val store: DocumentStore) {
    private fun path(id: TvId) = "tvs/${id.value}/personal-home.json"
    suspend fun load(id: TvId): HomePersonalization = store.read(path(id)) { bytes ->
        val obj = Json.parseToJsonElement(bytes.decodeToString()).jsonObject
        fun bool(key: String) = (obj[key] as? JsonPrimitive)?.booleanOrNull
        val photo = (obj["photo"] as? JsonObject)?.let {
            BlobRef(Sha256(it.getValue("sha256").jsonPrimitive.content), it.getValue("size").jsonPrimitive.long,
                it.getValue("path").jsonPrimitive.content)
        }
        HomePersonalization(
            hidden = (obj["hidden"] as? JsonArray)?.map { PackageName(it.jsonPrimitive.content) },
            inputs = bool("inputs"), network = bool("network"), bluetooth = bool("bluetooth"),
            wallpaperEnabled = bool("wallpaper"), photo = photo,
            dim = (obj["dim"] as? JsonPrimitive)?.intOrNull?.coerceIn(0, 90) ?: 35,
            arrange = bool("arrange"),
        )
    } ?: HomePersonalization()

    suspend fun save(id: TvId, value: HomePersonalization) {
        val obj = buildJsonObject {
            value.hidden?.let { put("hidden", JsonArray(it.map { p -> JsonPrimitive(p.value) })) }
            value.inputs?.let { put("inputs", it) }; value.network?.let { put("network", it) }
            value.bluetooth?.let { put("bluetooth", it) }; value.wallpaperEnabled?.let { put("wallpaper", it) }
            put("dim", value.dim)
            value.arrange?.let { put("arrange", it) }
            value.photo?.let { p -> put("photo", buildJsonObject {
                put("sha256", p.sha256.hex); put("size", p.size); put("path", p.path)
            }) }
        }
        store.write(path(id), obj.toString().encodeToByteArray())
    }
}
