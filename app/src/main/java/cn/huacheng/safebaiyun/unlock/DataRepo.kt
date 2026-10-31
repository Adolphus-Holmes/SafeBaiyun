package cn.huacheng.safebaiyun.unlock

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import cn.huacheng.safebaiyun.util.ContextHolder
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

@Serializable
data class DoorConfig(
    val name: String,
    val mac: String,
    val key: String
)

@Serializable
data class ConfigExport(
    val version: Int = 1,
    val configs: List<DoorConfig>
)

object DataRepo {
    private val preferences: SharedPreferences by lazy {
        ContextHolder.get().getSharedPreferences("data", Context.MODE_PRIVATE)
    }
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    fun readConfigs(): List<DoorConfig> {
        val raw = preferences.getString("configs_json", null)
        if (!raw.isNullOrBlank()) {
            return runCatching { json.decodeFromString<ConfigExport>(raw).configs }
                .getOrDefault(emptyList())
        }
        val oldMac = preferences.getString("mac", "").orEmpty()
        val oldKey = preferences.getString("key", "").orEmpty()
        return if (oldMac.isNotBlank() || oldKey.isNotBlank()) {
            listOf(DoorConfig("默认配置", oldMac, oldKey)).also { saveConfigs(it, 0) }
        } else {
            listOf(DoorConfig("默认配置", "", "")).also { saveConfigs(it, 0) }
        }
    }

    fun activeIndex(): Int = preferences.getInt("active_index", 0)

    fun saveConfigs(configs: List<DoorConfig>, active: Int = activeIndex()) {
        val safeConfigs = configs.ifEmpty { listOf(DoorConfig("默认配置", "", "")) }
        val safeIndex = active.coerceIn(0, safeConfigs.lastIndex)
        preferences.edit {
            putString("configs_json", json.encodeToString(ConfigExport(configs = safeConfigs)))
            putInt("active_index", safeIndex)
            putString("mac", safeConfigs[safeIndex].mac)
            putString("key", safeConfigs[safeIndex].key)
        }
    }

    fun activeConfig(): DoorConfig {
        val configs = readConfigs()
        return configs[activeIndex().coerceIn(0, configs.lastIndex)]
    }

    fun saveActiveConfig(config: DoorConfig) {
        val configs = readConfigs().toMutableList()
        val index = activeIndex().coerceIn(0, configs.lastIndex)
        configs[index] = config
        saveConfigs(configs, index)
    }

    fun selectConfig(index: Int) {
        saveConfigs(readConfigs(), index)
    }

    fun exportJson(): String =
        json.encodeToString(ConfigExport(configs = readConfigs()))

    fun importJson(raw: String): List<DoorConfig> {
        val parsed = json.decodeFromString<ConfigExport>(raw)
        require(parsed.version == 1) { "不支持的配置文件版本：${parsed.version}" }
        require(parsed.configs.isNotEmpty()) { "配置列表不能为空" }
        require(parsed.configs.all { it.name.isNotBlank() }) { "配置名称不能为空" }
        return parsed.configs
    }

    // Compatibility with the original single-profile API.
    fun readData(): Pair<String, String> = activeConfig().let { it.mac to it.key }
    fun save(mac: String, key: String) {
        val current = activeConfig()
        saveActiveConfig(current.copy(mac = mac, key = key))
    }
}
