package cn.huacheng.safebaiyun.compose

import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color as AndroidColor
import android.graphics.drawable.GradientDrawable
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.huacheng.safebaiyun.unlock.DataRepo
import cn.huacheng.safebaiyun.unlock.DoorConfig
import cn.huacheng.safebaiyun.util.showToast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditDialog(
    state: MutableState<Boolean>,
    initData: () -> DoorConfig,
    onSaved: () -> Unit,
    isNew: Boolean = false
) {
    val data = remember { initData() }
    val (name, setName) = remember { mutableStateOf(data.name) }
    val (mac, setMac) = remember { mutableStateOf(data.mac) }
    val (key, setKey) = remember { mutableStateOf(data.key) }
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = LocalFocusManager.current

    ModalBottomSheet(onDismissRequest = { state.value = false }) {
        Column(
            modifier = Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            NativeTextInput(name, setName, "Name", Modifier.fillMaxWidth())
            ClipboardInputRow(mac, setMac, "Mac", context)
            ClipboardInputRow(key, setKey, "Key", context)

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("SafeBaiyun configs", DataRepo.exportJson()))
                    showToast("配置 JSON 已复制到剪贴板")
                }, modifier = Modifier.weight(1f)) { Text("导出配置") }
                OutlinedButton(onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = clipboard.primaryClip
                    val raw = if (clip != null && clip.itemCount > 0) clip.getItemAt(0).coerceToText(context).toString() else ""
                    try {
                        val imported = DataRepo.importJson(raw)
                        DataRepo.saveConfigs(imported, 0)
                        onSaved()
                        state.value = false
                        showToast("配置导入成功")
                    } catch (e: Exception) {
                        showToast("导入失败：${e.message ?: "JSON 格式错误"}")
                    }
                }, modifier = Modifier.weight(1f)) { Text("从剪贴板导入") }
            }
            Button(
                modifier = Modifier.padding(8.dp),
                onClick = {
                    keyboard?.hide()
                    focus.clearFocus()
                    if (name.isBlank()) {
                        showToast("配置名称不能为空")
                    } else {
                        val edited = DoorConfig(name.trim(), mac.trim(), key.trim())
                        if (isNew) {
                            val all = DataRepo.readConfigs().toMutableList()
                            all.add(edited)
                            DataRepo.saveConfigs(all, all.lastIndex)
                        } else {
                            DataRepo.saveActiveConfig(edited)
                        }
                        onSaved()
                        state.value = false
                    }
                }
            ) { Text("保存") }
        }
    }
}

@Composable
private fun ClipboardInputRow(value: String, onValueChange: (String) -> Unit, hint: String, context: Context) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NativeTextInput(value, onValueChange, hint, Modifier.weight(1f))
        IconButton(onClick = {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = clipboard.primaryClip
            if (clip != null && clip.itemCount > 0) {
                onValueChange(clip.getItemAt(0).coerceToText(context).toString())
            } else showToast("剪贴板为空")
        }) {
            // Unicode clipboard/paste symbol; avoid unsupported icon glyph from font fallback.
            Text(text = "粘贴", fontSize = 14.sp)
        }
    }
}

@Composable
private fun NativeTextInput(value: String, onValueChange: (String) -> Unit, hint: String, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            EditText(ctx).apply {
                this.hint = hint
                setSingleLine(true)
                textSize = 16f
                setTextColor(AndroidColor.BLACK)
                setHintTextColor(AndroidColor.GRAY)
                setPadding(
                    (12 * resources.displayMetrics.density).toInt(),
                    (14 * resources.displayMetrics.density).toInt(),
                    (12 * resources.displayMetrics.density).toInt(),
                    (14 * resources.displayMetrics.density).toInt()
                )
                background = GradientDrawable().apply {
                    setColor(AndroidColor.TRANSPARENT)
                    setCornerRadius(4 * resources.displayMetrics.density)
                    setStroke((resources.displayMetrics.density).toInt().coerceAtLeast(1), AndroidColor.LTGRAY)
                }
                setText(value)
                addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                        onValueChange(s?.toString().orEmpty())
                    }
                    override fun afterTextChanged(s: Editable?) = Unit
                })
            }
        },
        update = { editText ->
            if (editText.text.toString() != value) {
                editText.setText(value)
                editText.setSelection(editText.text.length)
            }
            editText.hint = hint
        }
    )
}
