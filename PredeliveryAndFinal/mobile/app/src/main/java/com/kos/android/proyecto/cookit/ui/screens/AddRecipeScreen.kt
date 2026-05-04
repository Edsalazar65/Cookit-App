package com.kos.android.proyecto.cookit.ui.screens

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.kos.android.proyecto.cookit.domain.model.UiState
import com.kos.android.proyecto.cookit.ui.viewmodel.ChefViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRecipeScreen(
    viewModel: ChefViewModel,
    onRecipeAdded: () -> Unit,
    onNavigateBack: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var ingredients by remember { mutableStateOf(emptyList<String>()) }
    var currentIngredient by remember { mutableStateOf("") }
    var steps by remember { mutableStateOf(emptyList<String>()) }
    var currentStep by remember { mutableStateOf("") }
    var difficulty by remember { mutableStateOf("Medium") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val context = LocalContext.current
    val addRecipeState by viewModel.addRecipeState.collectAsStateWithLifecycle()

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            selectedBitmap = if (Build.VERSION.SDK_INT < 28) {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, it)
            } else {
                val source = ImageDecoder.createSource(context.contentResolver, it)
                ImageDecoder.decodeBitmap(source)
            }
        }
    }

    LaunchedEffect(addRecipeState) {
        if (addRecipeState is UiState.Success) {
            viewModel.clearAddRecipeState()
            onRecipeAdded()
        }
    }

    Scaffold(
        containerColor = Color(0xFFFDF5E6),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Add New Recipe", fontWeight = FontWeight.Bold, color = Color(0xFF5D4037)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF5D4037))
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Image Selector
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.LightGray)
                    .clickable { galleryLauncher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (selectedImageUri == null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.AddPhotoAlternate, null, modifier = Modifier.size(48.dp), tint = Color.Gray)
                        Text("Add Recipe Photo", color = Color.Gray)
                    }
                } else {
                    AsyncImage(
                        model = selectedImageUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Recipe Name
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Recipe Name") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Difficulty
            Text("Difficulty", fontWeight = FontWeight.Bold, color = Color(0xFF5D4037), modifier = Modifier.align(Alignment.Start))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Low", "Medium", "High").forEach { level ->
                    FilterChip(
                        selected = difficulty == level,
                        onClick = { difficulty = level },
                        label = { Text(level) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Ingredients
            Text("Ingredients", fontWeight = FontWeight.Bold, color = Color(0xFF5D4037), modifier = Modifier.align(Alignment.Start))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = currentIngredient,
                    onValueChange = { currentIngredient = it },
                    placeholder = { Text("Add ingredient") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
                IconButton(onClick = {
                    if (currentIngredient.isNotBlank()) {
                        ingredients = ingredients + currentIngredient
                        currentIngredient = ""
                    }
                }) {
                    Icon(Icons.Default.AddCircle, null, tint = Color(0xFF388E3C))
                }
            }
            ingredients.forEach { ingredient ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Check, null, tint = Color(0xFF388E3C), modifier = Modifier.size(16.dp))
                    Text(ingredient, modifier = Modifier.weight(1f).padding(horizontal = 8.dp))
                    IconButton(onClick = { ingredients = ingredients - ingredient }) {
                        Icon(Icons.Default.Delete, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Steps
            Text("Steps", fontWeight = FontWeight.Bold, color = Color(0xFF5D4037), modifier = Modifier.align(Alignment.Start))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = currentStep,
                    onValueChange = { currentStep = it },
                    placeholder = { Text("Add step") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
                IconButton(onClick = {
                    if (currentStep.isNotBlank()) {
                        steps = steps + currentStep
                        currentStep = ""
                    }
                }) {
                    Icon(Icons.Default.AddCircle, null, tint = Color(0xFF388E3C))
                }
            }
            steps.forEachIndexed { index, step ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text("${index + 1}.", fontWeight = FontWeight.Bold)
                    Text(step, modifier = Modifier.weight(1f).padding(horizontal = 8.dp))
                    IconButton(onClick = { steps = steps - step }) {
                        Icon(Icons.Default.Delete, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    viewModel.saveManualRecipe(name, ingredients, steps, difficulty, selectedBitmap)
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C)),
                enabled = name.isNotBlank() && ingredients.isNotEmpty() && steps.isNotEmpty() && addRecipeState !is UiState.Loading
            ) {
                if (addRecipeState is UiState.Loading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("Save Recipe", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }
            
            if (addRecipeState is UiState.Error) {
                Text((addRecipeState as UiState.Error).message, color = Color.Red, modifier = Modifier.padding(top = 8.dp))
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
