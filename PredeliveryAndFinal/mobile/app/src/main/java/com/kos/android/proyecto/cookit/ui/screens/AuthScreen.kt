package com.kos.android.proyecto.cookit.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kos.android.proyecto.cookit.R
import com.kos.android.proyecto.cookit.domain.model.UiState
import com.kos.android.proyecto.cookit.ui.viewmodel.ChefViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    viewModel: ChefViewModel,
    onAuthSuccess: () -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var isLoginMode by rememberSaveable { mutableStateOf(true) }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    val authUiState by viewModel.authUiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }


    LaunchedEffect(authUiState) {
        when (val state = authUiState) {
            is UiState.Success -> {
                viewModel.clearAuthUiState()
                onAuthSuccess()
            }
            is UiState.Error -> {
                snackbarHostState.showSnackbar(state.message)
                viewModel.clearAuthUiState()
            }
            else -> {}
        }
    }

    val isLoading = authUiState is UiState.Loading

    val textFieldColors = TextFieldDefaults.colors(
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
        disabledContainerColor = Color.Transparent,
        focusedIndicatorColor = Color.Black,
        unfocusedIndicatorColor = Color.Gray,
        focusedLabelColor = Color.Black,
        unfocusedLabelColor = Color.Gray
    )

    val context = androidx.compose.ui.platform.LocalContext.current

    val googleSignInLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
            account?.idToken?.let { token ->
                viewModel.signInWithGoogle(token)
            }
        } catch (e: com.google.android.gms.common.api.ApiException) {
            android.util.Log.e("GOOGLE_LOGIN", "Falló Google Auth. Código: ${e.statusCode}")
        } catch (e: Exception) {
            android.util.Log.e("GOOGLE_LOGIN", "Error desconocido: ${e.message}")
        }
    }


    fun startGoogleLogin() {
        val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        val client = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(context, gso)
        googleSignInLauncher.launch(client.signInIntent)
    }



    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFFDF5E6)) // Fondo crema suave de la imagen
        ) {
            Image(
                painter = painterResource(id = R.drawable.background_food),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = 0.7f
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (isLoginMode) "CookIt" else "Create\nAccount",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 42.sp,
                        lineHeight = 46.sp
                    ),
                    color = Color(0xFF000000),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(48.dp))

                if (!isLoginMode) {
                    TextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = textFieldColors
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                TextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColors
                )

                Spacer(modifier = Modifier.height(16.dp))

                TextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColors
                )

                Spacer(modifier = Modifier.height(40.dp))


                //Boton principal
                OutlinedButton(
                    onClick = {
                        if (isLoginMode) {
                            viewModel.signIn(email, password)
                        } else {
                            viewModel.signUp(name,email, password)
                        }
                    },
                    enabled = !isLoading && email.isNotBlank() && password.length >= 6,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color.DarkGray)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.Gray)
                    } else {
                        Text(
                            text = if (isLoginMode) "Log In" else "Sign Up",
                            color = Color.DarkGray,
                            fontSize = 18.sp
                        )
                    }
                }


                Spacer(modifier = Modifier.height(16.dp))

                // Botón de Google
                OutlinedButton(
                    onClick = { startGoogleLogin() }  ,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color.DarkGray)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_google_logo),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (isLoginMode) "Log in with Google" else "Sign up with Google",
                            color = Color.DarkGray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = if (isLoginMode) "Don't have an account?" else "Already have an account?", fontSize = 12.sp, color = Color.Black)
                    TextButton(onClick = { isLoginMode = !isLoginMode }) {
                        Text(
                            text = if (isLoginMode) "Sign In" else "Log In",
                            color = Color.Black,
                            fontSize = 12.sp
                        )
                    }

                }
            }
        }
    }
}