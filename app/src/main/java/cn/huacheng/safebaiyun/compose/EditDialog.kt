package cn.huacheng.safebaiyun.compose

import android.content.Context
import android.graphics.Color as AndroidColor
import android.graphics.drawable.GradientDrawable
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
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
import cn.huacheng.safebaiyun.unlock.DataRepo

/**
 *
 *@description:
 *@author: guangzhou
 *@create: 2024-05-10
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditDialog(state: MutableState<Boolean>, initData: () -> Pair<String, String>) {
    val data = remember { initData() }
    val (mac, setMac) = remember { mutableStateOf(data.first) }
    val (key, setKey) = remember { mutableStateOf(data.second) }
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    ModalBottomSheet(onDismissRequest = { state.value = false }) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            NativeTextInput(value = mac, onValueChange = setMac, hint = "Mac", context = context)
            NativeTextInput(value = key, onValueChange = setKey, hint = "Key", context = context)
            Button(
                modifier = Modifier.padding(8.dp),
                onClick = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    DataRepo.save(mac, key)
                    state.value = false
                }
            ) {
                Text(text = "保存")
            }
        }
    }
}

@Composable
private fun NativeTextInput(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    context: Context
) {
    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        factory = {
            EditText(context).apply {
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
                    setStroke(
                        (resources.displayMetrics.density).toInt().coerceAtLeast(1),
                        AndroidColor.LTGRAY
                    )
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
