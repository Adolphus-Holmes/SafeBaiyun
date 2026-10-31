package cn.huacheng.safebaiyun.unlock

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import cn.huacheng.safebaiyun.util.ContextHolder
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class DoorConfig(
    val id: String,
    val name: String,
    val mac: String,
    val key: String
)

@Serializable
data class ConfigFile(
    val version: Int = 1,
    val configs: List<DoorConfig>,
    val activeConfigId: String? = null
)

object DataRepo {
    private val preferences: SharedPreferences by lazy {
        ContextHolder.get().getSharedPreferences("data", Context.MODE_PRIVATE)
    }
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    private fun loadFile(): ConfigFile {
        val raw = preferences.getString("configs_json", null)
        if (!raw.isNullOrBlank()) {
            try {
                val parsed = json.decodeFromString<ConfigFile>(raw)
                if (parsed.configs.isNotEmpty()) return parsed
            } catch (_: Exception) { }
        }

        // Migrate the original single Mac/Key pair without discarding existing credentials.
        val oldMac = preferences.getString("mac", "") ?: ""
        val oldKey = preferences.getString("key", "") ?: ""
        val migrated = if (oldMac.isNotBlank() || oldKey.isNotBlank()) {
            listOf(DoorConfig("legacy", "默认配置", oldMac, oldKey))
        } else {
            listOf(DoorConfig("default", "默认配置", "", ""))
        }
        return ConfigFile(configs = migrated, activeConfigId = migrated.first().id).also { saveFile(it) }
    }

    private fun saveFile(file: ConfigFile) {
        val active = file.configs.firstOrNull { it.id == file.activeConfigId } ?: file.configs.first()
        val normalized = file.copy(activeConfigId = active.id)
        preferences.edit {
            putString("configs_json", json.encodeToString(normalized))
            // Keep legacy keys synchronized for older app components.
            putString("mac", active.mac)
            putString("key", active.key)
        }
    }

    fun readConfigs(): ConfigFile = loadFile()

    fun activeConfig(): DoorConfig {
        val file = loadFile()
        return file.configs.firstOrNull { it.id == file.activeConfigId } ?: file.configs.first()
    }

    fun readData(): Pair<String, String> = activeConfig().let { it.mac to it.key }

    fun selectConfig(id: String) {
        val file = loadFile()
        if (file.configs.any { it.id == id }) saveFile(file.copy(activeConfigId = id))
    }

    fun saveConfigs(configs: List<DoorConfig>, activeId: String?) {
        require(configs.isNotEmpty()) { "至少保留一份配置" }
        require(configs.map { it.id }.distinct().size == configs.size) { "配置 ID 重复" }
        saveFile(ConfigFile(configs = configs, activeConfigId = activeId ?: configs.first().id))
    }

    fun exportJson(): String = json.encodeToString(loadFile())

    fun importJson(content: String) {
        val imported = json.decodeFromString<ConfigFile>(content)
        require(imported.configs.isNotEmpty()) { "JSON 中没有配置" }
        require(imported.configs.map { it.id }.distinct().size == imported.configs.size) { "配置 ID 重复" }
        require(imported.configs.all { it.name.isNotBlank() }) { "配置名称不能为空" }
        val activeId = imported.configs.firstOrNull { it.id == imported.activeConfigId }?.id
            ?: imported.configs.first().id
        saveFile(imported.copy(activeConfigId = activeId))
    }
}
