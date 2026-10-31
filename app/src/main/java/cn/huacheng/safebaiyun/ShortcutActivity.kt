package cn.huacheng.safebaiyun

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import cn.huacheng.safebaiyun.unlock.DataRepo
import cn.huacheng.safebaiyun.unlock.UnlockRepo

class ShortcutActivity : ComponentActivity() {
 companion object { private const val REQ_PERMISSION=7101; private const val REQ_ENABLE=7102 }
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);if(intent.action==Intent.ACTION_CREATE_SHORTCUT)createShortcut() else continueUnlock()}
 private fun createShortcut(){val result=Intent();val icon=Intent.ShortcutIconResource.fromContext(this,R.mipmap.ic_launcher);result.putExtra(Intent.EXTRA_SHORTCUT_NAME,getString(R.string.unlock_door));result.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,icon);result.putExtra(Intent.EXTRA_SHORTCUT_INTENT,Intent(this,ShortcutActivity::class.java));setResult(RESULT_OK,result);finish()}
 private fun continueUnlock(){
  val cfg=DataRepo.activeConfig()
  if(cfg.mac.isBlank()||cfg.key.isBlank()){toast("请先在应用中完善当前配置");openMain();return}
  if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.S&&checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED){requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_CONNECT),REQ_PERMISSION);return}
  ensureBluetooth()
 }
 private fun ensureBluetooth(){
  val adapter=(getSystemService(BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
  if(adapter==null){toast("当前设备不支持蓝牙");finish();return}
  if(!adapter.isEnabled){try{@Suppress("DEPRECATION") startActivityForResult(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),REQ_ENABLE)}catch(e:Exception){toast("无法请求开启蓝牙，请手动开启后重试");openMain()};return}
  doUnlock()
 }
 private fun doUnlock(){try{val cfg=DataRepo.activeConfig();UnlockRepo.unlock(cfg.mac,cfg.key)}catch(e:SecurityException){toast("蓝牙权限不足，请在应用中授权后重试");openMain();return}catch(e:Exception){toast("解锁失败：${e.message ?: "未知错误"}")};finish()}
 @Deprecated("Deprecated in Java") override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?){super.onActivityResult(requestCode,resultCode,data);if(requestCode==REQ_ENABLE){val a=(getSystemService(BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter;if(resultCode==RESULT_OK&&a?.isEnabled==true)doUnlock() else {toast("蓝牙未开启，无法解锁");finish()}}}
 @Deprecated("Deprecated in Java") override fun onRequestPermissionsResult(requestCode:Int,permissions:Array<String>,grantResults:IntArray){super.onRequestPermissionsResult(requestCode,permissions,grantResults);if(requestCode==REQ_PERMISSION){if(grantResults.firstOrNull()==PackageManager.PERMISSION_GRANTED)ensureBluetooth() else {toast("需要蓝牙连接权限才能解锁");openMain()}}}
 private fun toast(msg:String){Toast.makeText(this,msg,Toast.LENGTH_LONG).show()}
 private fun openMain(){startActivity(Intent(this,MainActivity::class.java));finish()}
}