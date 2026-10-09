package id.smartpantry.app.presentation

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.*
import id.smartpantry.domain.*
import id.smartpantry.app.R
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun PantryApp(vm: PantryViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val featured by vm.featured.collectAsStateWithLifecycle()
    val nav=rememberNavController()
    val route=nav.currentBackStackEntryAsState().value?.destination?.route ?: "home"
    val context=LocalContext.current
    val gallery=rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if(uri!=null) { vm.processPhoto { PhotoLoader.load(context,uri) }; nav.navigate("result") { launchSingleTop=true } }
    }
    val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if(granted) nav.navigate("camera") else vm.error("Izinkan kamera, atau pilih foto dari galeri.")
    }
    val camera: ()->Unit = {
        if(ContextCompat.checkSelfPermission(context,Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED) nav.navigate("camera")
        else permission.launch(Manifest.permission.CAMERA)
    }
    Scaffold(topBar={
        TopAppBar(title={Text(when(route) { "result"->"Bahan kamu"; "recipes"->"Pilihan resep";
            "detail"->"Resep"; "camera"->"Foto bahan"; else->"SmartPantry" },style=MaterialTheme.typography.titleLarge)},
            colors=TopAppBarDefaults.topAppBarColors(containerColor=MaterialTheme.colorScheme.background),
            navigationIcon={ if(route!="home") IconButton(onClick={nav.popBackStack()}) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack,contentDescription="Kembali")
            } },actions={ if(route!="home") IconButton(onClick={vm.reset(); nav.navigate("home") { popUpTo("home") { inclusive=true } }}) {
                Icon(Icons.Default.Home,contentDescription="Kembali ke beranda")
            } })
    }) { padding ->
        NavHost(nav,startDestination="home",modifier=Modifier.padding(padding).fillMaxSize()) {
            composable("home") { HomeScreen(state,featured,camera,
                onGallery={gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))},
                onManual={vm.reset();nav.navigate("result")},
                onCategory={id->vm.reset();vm.toggleIngredient(id);vm.findRecipes();nav.navigate("recipes")},
                onRecipe={id->vm.reset();vm.openRecipe(id);nav.navigate("detail")}) }
            composable("camera") { CameraScreen(onPhoto={bitmap->vm.processPhoto { bitmap };nav.navigate("result") { popUpTo("camera") { inclusive=true } }},
                onError={vm.error(it);nav.popBackStack()}) }
            composable("result") { ResultScreen(state,vm::toggleIngredient,onSearch={vm.findRecipes();nav.navigate("recipes")}) }
            composable("recipes") { RecipeListScreen(state,onClick={vm.openRecipe(it);nav.navigate("detail")}) }
            composable("detail") { DetailScreen(state) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun HomeScreen(state: PantryState,featured: List<Recipe>,onCamera: ()->Unit,onGallery: ()->Unit,onManual: ()->Unit,onCategory: (Int)->Unit,onRecipe: (Long)->Unit) {
    val catalog=PhotoCatalog.get(LocalContext.current)
    var credits by rememberSaveable { mutableStateOf(false) }
    val orange=Color(0xFFED653B)
    LazyColumn(contentPadding=PaddingValues(horizontal=18.dp,vertical=4.dp),verticalArrangement=Arrangement.spacedBy(18.dp),modifier=Modifier.fillMaxSize()) {
        item { Text("Mau masak apa?",style=MaterialTheme.typography.headlineLarge) }
        item { Box(Modifier.fillMaxWidth().height(222.dp).clip(RoundedCornerShape(22.dp)).border(1.dp,MaterialTheme.colorScheme.outline,RoundedCornerShape(22.dp))) {
            catalog.photo("ayam_asam_manis")?.let { DishPhotoImage(it,Modifier.fillMaxSize(),960) }
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent,Color.Black.copy(alpha=.82f)))))
            Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Text("Ada bahan apa\nhari ini?",style=MaterialTheme.typography.headlineMedium,color=Color.White)
                Button(onClick=onCamera,shape=RoundedCornerShape(12.dp),colors=ButtonDefaults.buttonColors(containerColor=orange,contentColor=Color.White),
                    modifier=Modifier.heightIn(min=48.dp).testTag("cameraButton")) {
                    Icon(painterResource(R.drawable.ic_camera),null,Modifier.size(20.dp));Spacer(Modifier.width(8.dp));Text("Scan bahan");Spacer(Modifier.width(24.dp));Text("→")
                }
            }
        } }
        item { Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick=onManual,shape=RoundedCornerShape(12.dp),border=BorderStroke(1.dp,MaterialTheme.colorScheme.outline),
                modifier=Modifier.weight(1f).heightIn(min=50.dp).testTag("manualButton"),contentPadding=PaddingValues(10.dp)) {
                Icon(Icons.Default.Add,null,Modifier.size(20.dp));Spacer(Modifier.width(6.dp));Text("Pilih bahan")
            }
            OutlinedButton(onClick=onGallery,shape=RoundedCornerShape(12.dp),border=BorderStroke(1.dp,MaterialTheme.colorScheme.outline),modifier=Modifier.weight(1f).heightIn(min=50.dp),contentPadding=PaddingValues(10.dp)) {
                Icon(painterResource(R.drawable.ic_photo),null,Modifier.size(20.dp));Spacer(Modifier.width(6.dp));Text("Buka galeri")
            }
        } }
        item { Text("Mulai dari bahan",style=MaterialTheme.typography.titleLarge) }
        item { Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            listOf("Ayam" to 0,"Ikan" to 8,"Telur" to 5,"Tahu" to 6,"Tempe" to 7).forEach { (name,id)->
                Column(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).clickable { onCategory(id) }.padding(vertical=4.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(7.dp)) {
                    catalog.category(name)?.let { DishPhotoImage(it,Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(18.dp)).border(1.dp,MaterialTheme.colorScheme.outline,RoundedCornerShape(18.dp))) }
                    Text(name,style=MaterialTheme.typography.labelMedium)
                }
            }
        } }
        if(featured.isNotEmpty()) {
            item { Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                Text("Coba masak ini",style=MaterialTheme.typography.titleLarge)
                Surface(color=Color(0xFFFFE5D8),shape=RoundedCornerShape(8.dp)) { Text("MENU RUMAH",Modifier.padding(horizontal=8.dp,vertical=5.dp),style=MaterialTheme.typography.labelSmall,color=Color(0xFF9B391B)) }
            } }
            item { Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                featured.forEach { recipe -> Card(onClick={onRecipe(recipe.id)},modifier=Modifier.weight(1f),shape=RoundedCornerShape(18.dp),border=BorderStroke(1.dp,MaterialTheme.colorScheme.outline),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)) {
                    catalog.recipe(recipe.id)?.let { DishPhotoImage(it,Modifier.fillMaxWidth().aspectRatio(1.3f)) }
                    Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                        Text(recipe.category.uppercase(),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.secondary)
                        Text(recipe.title,style=MaterialTheme.typography.titleMedium,minLines=2,maxLines=2,overflow=TextOverflow.Ellipsis)
                        Text("Lihat resep →",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.primary)
                    }
                } }
            } }
        }
        item { TextButton(onClick={credits=true}) { Text("Kredit foto",style=MaterialTheme.typography.labelSmall) };ErrorMessage(state.error) }
    }
    if(credits) ModalBottomSheet(onDismissRequest={credits=false}) {
        LazyColumn(Modifier.fillMaxHeight(.85f),contentPadding=PaddingValues(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            item { Text("Kredit foto",style=MaterialTheme.typography.headlineMedium) }
            items(catalog.all(),key={it.key}) { PhotoCredit(it) }
        }
    }
}

@Composable private fun PhotoCredit(photo: DishPhoto) {
    val links=LocalUriHandler.current
    Column(verticalArrangement=Arrangement.spacedBy(4.dp)) {
        Text("Foto hidangan: ${photo.dishName}",style=MaterialTheme.typography.labelLarge)
        Text("${photo.author} · ${photo.license}",style=MaterialTheme.typography.bodySmall)
        Text(photo.changes,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        Row { TextButton(onClick={links.openUri(photo.sourceUrl)}) { Text("Sumber foto") }
            if(photo.licenseUrl.isNotBlank()) TextButton(onClick={links.openUri(photo.licenseUrl)}) { Text("Lisensi") } }
    }
}

@Composable private fun ErrorMessage(error: String?) {
    if(error!=null) Text(error,color=MaterialTheme.colorScheme.error,modifier=Modifier.testTag("errorMessage"))
}

@OptIn(ExperimentalLayoutApi::class,ExperimentalMaterial3Api::class)
@Composable private fun ResultScreen(state: PantryState,onToggle: (Int)->Unit,onSearch: ()->Unit) {
    var showAdd by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            state.photo?.let { bitmap ->
                Box(Modifier.fillMaxWidth().aspectRatio(bitmap.width.toFloat()/bitmap.height).clip(RoundedCornerShape(24.dp))) {
                    Image(bitmap.asImageBitmap(),"Foto bahan dan hasil deteksi",modifier=Modifier.fillMaxSize())
                    Canvas(Modifier.fillMaxSize()) {
                        val sx=size.width/bitmap.width; val sy=size.height/bitmap.height
                        state.detections.forEach { d ->
                            val b=d.box
                            drawRect(Color(0xFF00C853),Offset(b.left*sx,b.top*sy),Size((b.right-b.left)*sx,(b.bottom-b.top)*sy),
                                style=androidx.compose.ui.graphics.drawscope.Stroke(3.dp.toPx()))
                            val label="${Ingredients.names[d.classId]} ${(d.confidence*100).roundToInt()}%"
                            val paint=android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { color=android.graphics.Color.WHITE; textSize=12.sp.toPx() }
                            val x=(b.left*sx).coerceAtMost((size.width-paint.measureText(label)-8.dp.toPx()).coerceAtLeast(0f))
                            val y=(b.top*sy).coerceIn(0f,(size.height-20.dp.toPx()).coerceAtLeast(0f))
                            drawRect(Color(0xFF21633F),Offset(x,y),Size(paint.measureText(label)+8.dp.toPx(),20.dp.toPx()))
                            drawContext.canvas.nativeCanvas.drawText(label,x+4.dp.toPx(),y+15.dp.toPx(),paint)
                        }
                    }
                }
            }
            if(state.busy) { LinearProgressIndicator(Modifier.fillMaxWidth()); Text("Mengenali bahan…") }
            ErrorMessage(state.error)
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
                Text("Bahan pilihan",style=MaterialTheme.typography.titleLarge)
                Text("${state.selected.size} bahan",style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary)
            }
            if(!state.busy && state.photo!=null && state.error==null) Text("Scan selesai",modifier=Modifier.testTag("scanComplete"),style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.primary)
            if(state.selected.isEmpty()) {
                Icon(painterResource(R.drawable.ic_camera),null,Modifier.size(40.dp),tint=MaterialTheme.colorScheme.primary)
                Text(if(state.photo!=null) "Belum menemukan bahan. Tambahkan dari daftar." else "Pilih bahan yang ada di dapurmu.",color=MaterialTheme.colorScheme.onSurfaceVariant)
            } else Text("Ketuk bahan untuk menghapusnya.",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                state.selected.sorted().forEach { id ->
                    InputChip(selected=true,onClick={onToggle(id)},label={Text(Ingredients.names[id])},
                        trailingIcon={Icon(Icons.Default.Close,contentDescription="Hapus ${Ingredients.names[id]}")})
                }
            }
            OutlinedButton(onClick={showAdd=true},enabled=!state.busy,modifier=Modifier.testTag("addIngredients").heightIn(min=48.dp)) {
                Icon(Icons.Default.Add,null);Spacer(Modifier.width(8.dp));Text("Tambahkan bahan")
            }
        }
        Button(onClick=onSearch,enabled=state.selected.isNotEmpty() && !state.busy,modifier=Modifier.padding(horizontal=20.dp,vertical=14.dp).fillMaxWidth().heightIn(min=56.dp).testTag("searchRecipes")) { Text("Cari resep") }
    }
    if(showAdd) ModalBottomSheet(onDismissRequest={showAdd=false},sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true)) {
        var query by rememberSaveable { mutableStateOf("") }
        Column(Modifier.fillMaxHeight(0.85f).padding(horizontal=20.dp)) {
            Text("Pilih bahan",style=MaterialTheme.typography.headlineMedium)
            OutlinedTextField(value=query,onValueChange={query=it},placeholder={Text("Cari bahan")},singleLine=true,
                leadingIcon={Icon(Icons.Default.Search,null)},modifier=Modifier.fillMaxWidth().padding(vertical=16.dp),shape=RoundedCornerShape(18.dp))
            LazyColumn(Modifier.weight(1f)) { items(Ingredients.names.indices.filter { Ingredients.names[it].contains(query,ignoreCase=true) }) { id ->
                Row(Modifier.fillMaxWidth().clickable { onToggle(id) }.heightIn(min=56.dp),verticalAlignment=Alignment.CenterVertically) {
                    Checkbox(checked=id in state.selected,onCheckedChange={onToggle(id)},modifier=Modifier.testTag("ingredient-$id"))
                    Text(Ingredients.names[id],style=MaterialTheme.typography.bodyLarge)
                }
            } }
            Button(onClick={showAdd=false},modifier=Modifier.fillMaxWidth().padding(vertical=16.dp).heightIn(min=52.dp)) { Text("Selesai") }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class,ExperimentalMaterial3Api::class)
@Composable private fun RecipeListScreen(state: PantryState,onClick: (Long)->Unit) {
    val photos=PhotoCatalog.get(LocalContext.current)
    var category by rememberSaveable { mutableStateOf("Semua") }
    var query by rememberSaveable { mutableStateOf("") }
    var onlyComplete by rememberSaveable { mutableStateOf(false) }
    var showFilters by rememberSaveable { mutableStateOf(false) }
    // Keep the recommendation order; filters only narrow the existing results.
    val filtered=remember(state.matches,category,query,onlyComplete,photos) {
        state.matches.filter { (category=="Semua" || it.recipe.category.equals(category,ignoreCase=true)) &&
            it.recipe.title.contains(query,ignoreCase=true) && (!onlyComplete || it.missingIds.isEmpty()) }
            .sortedWith(compareByDescending<RecipeMatch> { it.coverage }.thenByDescending { photos.recipe(it.recipe.id)!=null })
    }
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        item { Column(verticalArrangement=Arrangement.spacedBy(14.dp)) {
            Text("Temukan menu yang cocok",style=MaterialTheme.typography.titleLarge)
            OutlinedTextField(value=query,onValueChange={query=it},placeholder={Text("Cari resep")},singleLine=true,
                leadingIcon={Icon(Icons.Default.Search,null)},trailingIcon={IconButton(onClick={showFilters=true},modifier=Modifier.testTag("recipeFilters")) { Icon(Icons.Default.Settings,contentDescription="Filter resep") }},
                modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(20.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                listOf("Semua","Ayam","Ikan","Telur","Tahu","Tempe").forEach { name ->
                    FilterChip(selected=category==name,onClick={category=name},label={Text(name)},modifier=Modifier.testTag("category-$name"))
                }
            }
            FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp)) { state.selected.sorted().forEach { id ->
                Surface(shape=RoundedCornerShape(12.dp),color=MaterialTheme.colorScheme.primaryContainer) {
                    Text(Ingredients.names[id],Modifier.padding(horizontal=12.dp,vertical=8.dp),style=MaterialTheme.typography.labelLarge)
                }
            } }
            Text("${filtered.size} resep",style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.onSurfaceVariant)
            if(state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            ErrorMessage(state.error)
        } }
        if(!state.busy && filtered.isEmpty() && state.error==null) item {
            Icon(Icons.Default.Search,null,Modifier.size(40.dp),tint=MaterialTheme.colorScheme.primary)
            Text("Belum ada resep yang cocok. Coba ubah filter atau bahan pilihan.",Modifier.padding(top=12.dp))
        }
        items(filtered.chunked(2),key={it.first().recipe.id}) { row ->
            Row(horizontalArrangement=Arrangement.spacedBy(14.dp),modifier=Modifier.fillMaxWidth()) {
                row.forEach { match -> RecipeCard(match,onClick,Modifier.weight(1f)) }
                if(row.size==1) Spacer(Modifier.weight(1f))
            }
        }
    }
    if(showFilters) ModalBottomSheet(onDismissRequest={showFilters=false}) {
        Column(Modifier.padding(24.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
            Text("Filter resep",style=MaterialTheme.typography.headlineMedium)
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text("Semua bahan utama tersedia",style=MaterialTheme.typography.titleMedium);Text("Bumbu tetap perlu diperiksa.",style=MaterialTheme.typography.bodyMedium) }
                Switch(checked=onlyComplete,onCheckedChange={onlyComplete=it},modifier=Modifier.testTag("completeFilter"))
            }
            Button(onClick={showFilters=false},modifier=Modifier.fillMaxWidth().heightIn(min=52.dp)) { Text("Tampilkan resep") }
            TextButton(onClick={category="Semua";query="";onlyComplete=false;showFilters=false},modifier=Modifier.align(Alignment.CenterHorizontally)) { Text("Hapus filter") }
        }
    }
}

@Composable private fun RecipeCard(match: RecipeMatch,onClick: (Long)->Unit,modifier: Modifier) {
    Card(onClick={onClick(match.recipe.id)},modifier=modifier.testTag("recipe-${match.recipe.id}"),shape=RoundedCornerShape(22.dp),
        border=BorderStroke(1.dp,MaterialTheme.colorScheme.outline),
        colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface),elevation=CardDefaults.cardElevation(defaultElevation=2.dp)) {
        PhotoCatalog.get(LocalContext.current).recipe(match.recipe.id)?.let {
            DishPhotoImage(it,Modifier.fillMaxWidth().aspectRatio(1.18f))
        }
        Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text(match.recipe.category.uppercase(),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.secondary,letterSpacing=1.sp)
            Text(match.recipe.title,style=MaterialTheme.typography.titleMedium.copy(fontSize=14.sp,lineHeight=20.sp),minLines=3,maxLines=3,overflow=TextOverflow.Ellipsis)
            Text("${match.matchedIds.size} dari ${match.recipe.ingredientIds.size} bahan utama",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.primary)
            LinearProgressIndicator(progress={match.coverage.toFloat()},color=Color(0xFFED653B),trackColor=MaterialTheme.colorScheme.primaryContainer,modifier=Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(8.dp)))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable private fun DetailScreen(state: PantryState) {
    LazyColumn(Modifier.fillMaxSize().testTag("detailList"),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
        item { if(state.busy) LinearProgressIndicator(Modifier.fillMaxWidth()); ErrorMessage(state.error) }
        val recipe=state.detail
        if(recipe!=null) {
            item { Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
                PhotoCatalog.get(LocalContext.current).recipe(recipe.id)?.let { photo ->
                    DishPhotoImage(photo,Modifier.fillMaxWidth().height(250.dp).clip(RoundedCornerShape(24.dp)).border(1.dp,MaterialTheme.colorScheme.outline,RoundedCornerShape(24.dp)),960)
                    var expanded by rememberSaveable(recipe.id) { mutableStateOf(false) }
                    TextButton(onClick={expanded=!expanded},contentPadding=PaddingValues(0.dp)) { Text("Tentang foto",style=MaterialTheme.typography.labelMedium) }
                    if(expanded) { Text("Foto contoh hidangan; penyajian resep dapat berbeda.",style=MaterialTheme.typography.bodySmall);PhotoCredit(photo) }
                }
                Text(recipe.category.uppercase(),style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.secondary,letterSpacing=2.sp)
                Text(recipe.title,style=MaterialTheme.typography.headlineMedium,modifier=Modifier.testTag("detailReady"))
            } }
            item { Text("Bahan utama",style=MaterialTheme.typography.titleLarge)
                FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) { recipe.ingredientIds.sorted().forEach { id ->
                    Surface(shape=RoundedCornerShape(12.dp),color=MaterialTheme.colorScheme.primaryContainer) {
                        Row(Modifier.padding(horizontal=12.dp,vertical=10.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                            Icon(if(id in state.selected) Icons.Default.Check else Icons.Default.Add,contentDescription=if(id in state.selected) "Tersedia" else "Belum tersedia",modifier=Modifier.size(18.dp))
                            Text(Ingredients.names[id],style=MaterialTheme.typography.labelLarge)
                        }
                    }
                } }
            }
            item { Text("Bahan lengkap",style=MaterialTheme.typography.titleLarge) }
            items(recipe.ingredientLines) { line -> Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                Text("•",color=MaterialTheme.colorScheme.secondary);Text(line,style=MaterialTheme.typography.bodyLarge)
            } }
            item { Text("Periksa juga bumbu dan takarannya sebelum memasak.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant) }
            item { Text("Cara memasak",style=MaterialTheme.typography.titleLarge) }
            items(recipe.steps.indices.toList()) { index ->
                Row(horizontalArrangement=Arrangement.spacedBy(14.dp)) {
                    Surface(shape=RoundedCornerShape(12.dp),color=MaterialTheme.colorScheme.primaryContainer,modifier=Modifier.size(34.dp)) {
                        Box(contentAlignment=Alignment.Center) { Text("${index+1}",fontWeight=FontWeight.Bold) }
                    }
                    Text(recipe.steps[index],Modifier.weight(1f),style=MaterialTheme.typography.bodyLarge)
                }
            }
            item { var expanded by rememberSaveable { mutableStateOf(false) }
                TextButton(onClick={expanded=!expanded}) { Text("Sumber resep") }
                if(expanded) SelectionContainer { Text(recipe.sourceUrl,style=MaterialTheme.typography.bodySmall) }
            }
        } else if(!state.busy && state.error==null) item { Text("Pilih resep dari daftar untuk melihat detailnya.") }
    }
}
