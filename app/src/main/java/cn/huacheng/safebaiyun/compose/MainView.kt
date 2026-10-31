package cn.huacheng.safebaiyun.compose
import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun MainView(navController:NavHostController){
 val context=LocalContext.current
 val configs=remember{mutableStateOf(DataRepo.readConfigs())}
 val activeIndex=remember{mutableStateOf(DataRepo.activeIndex().coerceIn(0,configs.value.lastIndex))}
 val showEdit=remember{mutableStateOf(false)}
 val showConfigList=remember{mutableStateOf(false)}
 val creatingConfig=remember{mutableStateOf(false)}
 val pendingDelete=remember{mutableStateOf<Int?>(null)}
 fun refresh(){configs.value=DataRepo.readConfigs();activeIndex.value=DataRepo.activeIndex().coerceIn(0,configs.value.lastIndex)}
 fun exportAll(){val cb=context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager;cb.setPrimaryClip(android.content.ClipData.newPlainText("SafeBaiyun configs",DataRepo.exportJson()));showToast("全部配置 JSON 已复制到剪贴板")}
 fun importAll(){val cb=context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager;val clip=cb.primaryClip;val raw=if(clip!=null&&clip.itemCount>0)clip.getItemAt(0).coerceToText(context).toString() else "";try{val imported=DataRepo.importJson(raw);DataRepo.saveConfigs(imported,0);refresh();showToast("全部配置导入成功")}catch(e:Exception){showToast("导入失败：${e.message ?: "剪贴板内容不是有效配置 JSON"}")}}
 val enableLauncher=rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()){val a=(context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter;if(a?.isEnabled==true)unlock(context,configs.value[activeIndex.value]) else showToast("蓝牙未开启，无法解锁")}
 val permissionLauncher=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){ok->if(ok)requestEnable(context,enableLauncher){unlock(context,configs.value[activeIndex.value])}else showToast("需要蓝牙连接权限才能解锁")}
 Column{
  MainTopBar(onEditClick={showEdit.value=true},onHelperClick={navController.navigate("helper")},onConfigClick={refresh();showConfigList.value=true},onExportClick={exportAll()},onImportClick={importAll()})
  Box(Modifier.fillMaxSize().padding(8.dp),contentAlignment=Alignment.Center){Button(onClick={val ok=Build.VERSION.SDK_INT<Build.VERSION_CODES.S||ContextCompat.checkSelfPermission(context,Manifest.permission.BLUETOOTH_CONNECT)==PackageManager.PERMISSION_GRANTED;if(!ok)permissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT) else requestEnable(context,enableLauncher){unlock(context,configs.value[activeIndex.value])}},modifier=Modifier.size(144.dp,56.dp)){Text(stringResource(R.string.unlock_door),fontSize=18.sp)}}
  if(showEdit.value)EditDialog(state=showEdit,initData={if(creatingConfig.value)DoorConfig("","","") else DataRepo.activeConfig()},onSaved={refresh();creatingConfig.value=false},isNew=creatingConfig.value)
  if(showConfigList.value)AlertDialog(onDismissRequest={showConfigList.value=false},title={Text("切换配置（长按可删除）")},text={Column{
   TextButton(onClick={showConfigList.value=false;creatingConfig.value=true;showEdit.value=true}){Text("+ 新建配置",color=androidx.compose.ui.graphics.Color(0xFF1976D2))}
   configs.value.forEachIndexed{index,cfg->TextButton(onClick={DataRepo.selectConfig(index);refresh();showConfigList.value=false},modifier=Modifier.combinedClickable(onClick={DataRepo.selectConfig(index);refresh();showConfigList.value=false},onLongClick={pendingDelete.value=index})){Text((if(index==activeIndex.value)"✓  " else "　　")+cfg.name,color=if(index==activeIndex.value)androidx.compose.ui.graphics.Color(0xFF1976D2) else androidx.compose.ui.graphics.Color.Unspecified)}}
  }},confirmButton={TextButton(onClick={showConfigList.value=false}){Text("关闭")}})
  pendingDelete.value?.let{idx->configs.value.getOrNull(idx)?.let{cfg->AlertDialog(onDismissRequest={pendingDelete.value=null},title={Text("删除配置")},text={Text("确定删除“${cfg.name}”吗？此操作不可撤销。")},confirmButton={TextButton(onClick={val list=configs.value.toMutableList().also{it.removeAt(idx)};val old=activeIndex.value;val next=when{list.isEmpty()->0;idx<old->old-1;idx==old->idx.coerceAtMost(list.lastIndex);else->old};DataRepo.saveConfigs(list,next);pendingDelete.value=null;refresh()}){Text("删除",color=androidx.compose.ui.graphics.Color.Red)}},dismissButton={TextButton(onClick={pendingDelete.value=null}){Text("取消")}})}}
 }
}
private fun requestEnable(context:Context,launcher:androidx.activity.compose.ManagedActivityResultLauncher<Intent,androidx.activity.result.ActivityResult>,onEnabled:()->Unit){val a=(context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter;when{a==null->showToast("当前设备不支持蓝牙");a.isEnabled->onEnabled();else->launcher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))}}
private fun unlock(context:Context,cfg:DoorConfig){if(cfg.mac.isBlank()||cfg.key.isBlank()){showToast("当前配置未填写完整，请先编辑配置");return};showToast("开始解锁门禁");UnlockRepo.unlock(cfg.mac,cfg.key)}
