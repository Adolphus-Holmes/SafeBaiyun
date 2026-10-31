package cn.huacheng.safebaiyun.compose

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import cn.huacheng.safebaiyun.R
import cn.huacheng.safebaiyun.unlock.DataRepo
import cn.huacheng.safebaiyun.unlock.DoorConfig
import cn.huacheng.safebaiyun.unlock.UnlockRepo
import cn.huacheng.safebaiyun.util.showToast

@Composable
fun MainView(navController: NavHostController) {
    val context = LocalContext.current
    val configs = remember { mutableStateOf(DataRepo.readConfigs()) }
    val activeIndex = remember { mutableStateOf(DataRepo.activeIndex().coerceIn(0, configs.value.lastIndex)) }
    val showEdit = remember { mutableStateOf(false) }
    val showConfigList = remember { mutableStateOf(false) }
    val creatingConfig = remember { mutableStateOf(false) }

    fun refreshConfigs() {
        configs.value = DataRepo.readConfigs()
        activeIndex.value = DataRepo.activeIndex().coerceIn(0, configs.value.lastIndex)
    }

    val enableBluetoothLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        val adapter = (context.getSystemService(android.content.Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
        if (adapter?.isEnabled == true) {
            startUnlock(context, configs.value[activeIndex.value])
        } else {
            showToast("蓝牙未开启，无法解锁")
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            requestBluetoothEnable(context, enableBluetoothLauncher) {
                startUnlock(context, configs.value[activeIndex.value])
            }
        } else {
            showToast("需要蓝牙连接权限才能解锁")
        }
    }

    Column {
        MainTopBar(
            onEditClick = { showEdit.value = true },
            onHelperClick = { navController.navigate("helper") },
            onConfigClick = { refreshConfigs(); showConfigList.value = true }
        )
        Box(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Button(
                onClick = {
                    val permissionGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
                    if (!permissionGranted) {
                        permissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                    } else {
                        requestBluetoothEnable(context, enableBluetoothLauncher) {
                            startUnlock(context, configs.value[activeIndex.value])
                        }
                    }
                },
                modifier = Modifier.size(144.dp, 56.dp)
            ) {
                Text(text = stringResource(id = R.string.unlock_door), fontSize = 18.sp)
            }
        }
        if (showEdit.value) {
            EditDialog(
                state = showEdit,
                initData = { if (creatingConfig.value) DoorConfig("", "", "") else DataRepo.activeConfig() },
                onSaved = { refreshConfigs(); creatingConfig.value = false },
                isNew = creatingConfig.value
            )
        }
        if (showConfigList.value) {
            AlertDialog(
                onDismissRequest = { showConfigList.value = false },
                title = { Text("切换配置") },
                text = {
                    Column {
                        TextButton(onClick = {
                            showConfigList.value = false
                            creatingConfig.value = true
                            showEdit.value = true
                        }) { Text("+ 新建配置", color = androidx.compose.ui.graphics.Color(0xFF1976D2)) }
                        configs.value.forEachIndexed { index, config ->
                            TextButton(onClick = {
                                DataRepo.selectConfig(index)
                                refreshConfigs()
                                showConfigList.value = false
                            }) {
                                Text(
                                    text = (if (index == activeIndex.value) "✓  " else "　　") + config.name,
                                    color = if (index == activeIndex.value) androidx.compose.ui.graphics.Color(0xFF1976D2) else androidx.compose.ui.graphics.Color.Unspecified
                                )
                            }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { showConfigList.value = false }) { Text("关闭") } }
            )
        }
    }
}

private fun requestBluetoothEnable(
    context: android.content.Context,
    launcher: androidx.activity.compose.ManagedActivityResultLauncher<Intent, androidx.activity.result.ActivityResult>,
    onEnabled: () -> Unit
) {
    val adapter = (context.getSystemService(android.content.Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
    when {
        adapter == null -> showToast("当前设备不支持蓝牙")
        adapter.isEnabled -> onEnabled()
        else -> launcher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
    }
}

private fun startUnlock(context: android.content.Context, config: DoorConfig) {
    if (config.mac.isBlank() || config.key.isBlank()) {
        showToast("当前配置未填写完整，请先编辑配置")
        return
    }
    showToast("开始解锁门禁")
    UnlockRepo.unlock(config.mac, config.key)
}
