package cn.huacheng.safebaiyun.compose
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.navigation.NavController
import androidx.compose.ui.res.stringResource
import cn.huacheng.safebaiyun.R
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTopBar(onEditClick:()->Unit,onHelperClick:()->Unit,onConfigClick:()->Unit,onExportClick:()->Unit,onImportClick:()->Unit){
 val showMenu=remember{mutableStateOf(false)}
 TopAppBar(colors=TopAppBarDefaults.topAppBarColors(containerColor=MaterialTheme.colorScheme.primary,titleContentColor=MaterialTheme.colorScheme.onPrimary),title={Text(stringResource(R.string.app_name))},actions={
  IconButton(onClick=onEditClick){Icon(Icons.Default.Edit,"编辑配置",tint=MaterialTheme.colorScheme.onPrimary)}
  IconButton(onClick={showMenu.value=true}){Icon(Icons.Default.MoreVert,"更多选项",tint=MaterialTheme.colorScheme.onPrimary)}
  DropdownMenu(expanded=showMenu.value,onDismissRequest={showMenu.value=false}){
   DropdownMenuItem(text={Text("切换配置")},onClick={showMenu.value=false;onConfigClick()})
   DropdownMenuItem(text={Text("导出全部配置")},onClick={showMenu.value=false;onExportClick()})
   DropdownMenuItem(text={Text("导入全部配置")},onClick={showMenu.value=false;onImportClick()})
   DropdownMenuItem(text={Text("使用说明")},onClick={showMenu.value=false;onHelperClick()})
  }
 })
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelperTopBar(navController:NavController){TopAppBar(colors=TopAppBarDefaults.topAppBarColors(containerColor=MaterialTheme.colorScheme.primary,titleContentColor=MaterialTheme.colorScheme.onPrimary),title={Text("使用说明")},navigationIcon={IconButton(onClick={navController.popBackStack()}){Icon(Icons.AutoMirrored.Filled.ArrowBack,null,tint=MaterialTheme.colorScheme.onPrimary)}})}
