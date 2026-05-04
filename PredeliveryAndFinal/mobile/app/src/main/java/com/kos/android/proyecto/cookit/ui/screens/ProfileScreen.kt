package com.kos.android.proyecto.cookit.ui.screens

import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.kos.android.proyecto.cookit.ui.components.CookitBottomNavigation
import com.kos.android.proyecto.cookit.ui.components.CookitScreen
import com.kos.android.proyecto.cookit.ui.viewmodel.ChefViewModel

@Composable
fun ProfileScreen(
    viewModel: ChefViewModel,
    onNavigateToHome: () -> Unit,
    onNavigateToExplore: () -> Unit,
    onNavigateToFavorites: () -> Unit,
    onNavigateToAddRecipe: () -> Unit,
    onLogout: () -> Unit
) {
    val userData by viewModel.userData.collectAsStateWithLifecycle()
    val favoriteRecipes by viewModel.favoriteRecipes.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val bitmap = if (Build.VERSION.SDK_INT < 28) {
                MediaStore.Images.Media.getBitmap(context.contentResolver, it)
            } else {
                val source = ImageDecoder.createSource(context.contentResolver, it)
                ImageDecoder.decodeBitmap(source)
            }
            viewModel.uploadProfilePicture(bitmap)
        }
    }

    val firstName = userData?.name?.split(" ")?.firstOrNull() ?: "Chef"

    Scaffold(
        containerColor = Color(0xFFFDF5E6),
        bottomBar = {
            CookitBottomNavigation(
                currentScreen = CookitScreen.PROFILE,
                onNavigateToHome = onNavigateToHome,
                onNavigateToExplore = onNavigateToExplore,
                onNavigateToFavorites = onNavigateToFavorites,
                onNavigateToProfile = { },
                onNavigateToAddRecipe = onNavigateToAddRecipe
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(30.dp))

            Box(contentAlignment = Alignment.BottomEnd) {
                Surface(
                    modifier = Modifier.size(140.dp),
                    shape = CircleShape,
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFFFCC80))
                ) {
                    if (userData?.photoURL.isNullOrBlank()) {
                        Box(modifier = Modifier.padding(10.dp), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.RestaurantMenu, null, modifier = Modifier.size(70.dp), tint = Color.Gray)
                        }
                    } else {
                        AsyncImage(
                            model = userData?.photoURL,
                            contentDescription = "Profile Picture",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    
                    // Overlay de carga si se está procesando
                    if (userData == null) {
                        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color.White)
                        }
                    }
                }

                Surface(
                    modifier = Modifier.size(36.dp),
                    shape = CircleShape,
                    color = Color(0xFFFFD180),
                    shadowElevation = 4.dp,
                    onClick = { galleryLauncher.launch("image/*") }
                ) {
                    Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.padding(6.dp))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Chef $firstName",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp
                ),
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "\"Cooking is love made visible\"",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp
                ),
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(40.dp))

            InfoCard(title = "My Stats") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(value = favoriteRecipes.size.toString(), label = "FAVORITES")
                    StatItem(value = "5", label = "FRIENDS")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            InfoCard(title = "Settings") {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SettingItem(label = "Notifications", value = "ON")
                    SettingItem(label = "Account Privacy", value = "Public")
                    SettingItem(label = "Language", value = "ES")
                }
            }

            Spacer(modifier = Modifier.height(50.dp))

            Button(
                onClick = onLogout,
                modifier = Modifier
                    .width(160.dp)
                    .height(45.dp),
                shape = RoundedCornerShape(22.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF9A9A))
            ) {
                Text(
                    "LOG OUT",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
private fun InfoCard(title: String, content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFFFFD180).copy(alpha = 0.5f),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Color.Black)
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp, horizontal = 24.dp), color = Color.Gray)
            content()
        }
    }
}

@Composable
private fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 26.sp, color = Color.Black)
        Text(label, fontSize = 10.sp, color = Color.Gray)
    }
}

@Composable
private fun SettingItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 14.sp, color = Color.Gray)
        Text(value, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color.Black)
    }
}
