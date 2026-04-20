package com.kos.android.proyecto.cookit.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit,
    onNavigateToFavorites: () ->Unit
    // Pasar datos del usuario
) {
    Scaffold(
        containerColor = Color(0xFFFDF5E6), // Fondo crema suave de la imagen web
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, null) },
                    selected = false,
                    onClick = onNavigateBack
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Favorite, null) },
                    selected = false,
                    onClick = onNavigateToFavorites
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Person, null) },
                    selected = true,
                    onClick = { },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFF388E3C))
                )
            }
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
                    border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFFFCC80)) // Borde naranja suave
                ) {

                    Box(modifier = Modifier.padding(10.dp), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.RestaurantMenu, null, modifier = Modifier.size(70.dp), tint = Color.Gray)
                    }
                }


                Surface(
                    modifier = Modifier.size(36.dp),
                    shape = CircleShape,
                    color = Color(0xFFFFD180), // Fondo naranja de la web
                    shadowElevation = 4.dp,
                    onClick = { /* Acción editar imagen */ }
                ) {
                    Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.padding(6.dp))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Chef Gustavo",
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
                    StatItem(value = "12", label = "FAVORITES")
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
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF9A9A)) // Rojo suave de la imagen
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
            Divider(modifier = Modifier.padding(vertical = 12.dp, horizontal = 24.dp), color = Color.Gray)
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