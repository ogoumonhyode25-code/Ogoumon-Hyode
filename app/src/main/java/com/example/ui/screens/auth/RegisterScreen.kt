package com.example.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.GoogleSignInButton
import com.example.ui.theme.VideoCashGoldAccent
import com.example.ui.theme.VideoCashPurplePrimary
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

@Composable
fun RegisterScreen(
    onNavigateToLogin: () -> Unit,
    onRegisterSuccess: () -> Unit,
    viewModel: AuthViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val form by viewModel.registerForm.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isPasswordVisible by remember { mutableStateOf(false) }
    var isConfirmPasswordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Success) {
            onRegisterSuccess()
            viewModel.resetUiState()
        }
    }

    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090614))
            .imePadding()
            .testTag("register_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Titre & Sous-titre
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Vidéo",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                )
                Text(
                    text = "Cash",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Black,
                        color = VideoCashGoldAccent
                    )
                )
            }

            Text(
                text = "Créer un nouveau compte",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.padding(top = 6.dp)
            )
            Text(
                text = "Rejoignez la communauté et gagnez des récompenses en FCFA",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFFAFA7C9)
                ),
                modifier = Modifier.padding(top = 2.dp, bottom = 24.dp)
            )

            // Bannière d'erreur générale
            if (uiState is AuthUiState.Error) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF3F1322),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .border(1.dp, Color(0xFFE11D48), RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = "Erreur",
                            tint = Color(0xFFFDA4AF),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = (uiState as AuthUiState.Error).message,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFFFFF1F2),
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            }

            // 1. Prénom
            AuthTextField(
                value = form.firstName,
                onValueChange = { viewModel.updateFirstName(it) },
                label = "Prénom",
                placeholder = "Ex: Jean",
                leadingIcon = Icons.Default.Person,
                errorMessage = form.firstNameError,
                testTag = "input_register_firstname",
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Nom
            AuthTextField(
                value = form.lastName,
                onValueChange = { viewModel.updateLastName(it) },
                label = "Nom",
                placeholder = "Ex: Dupont",
                leadingIcon = Icons.Default.Person,
                errorMessage = form.lastNameError,
                testTag = "input_register_lastname",
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Nom d'utilisateur
            AuthTextField(
                value = form.username,
                onValueChange = { viewModel.updateUsername(it) },
                label = "Nom d'utilisateur",
                placeholder = "Ex: jean_229",
                leadingIcon = Icons.Default.AlternateEmail,
                errorMessage = form.usernameError,
                testTag = "input_register_username",
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 4. Adresse e-mail
            AuthTextField(
                value = form.email,
                onValueChange = { viewModel.updateEmail(it) },
                label = "Adresse e-mail",
                placeholder = "jean.dupont@gmail.com",
                leadingIcon = Icons.Default.Email,
                keyboardType = KeyboardType.Email,
                errorMessage = form.emailError,
                testTag = "input_register_email",
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 5. Numéro de téléphone
            AuthTextField(
                value = form.phone,
                onValueChange = { viewModel.updatePhone(it) },
                label = "Numéro de téléphone",
                placeholder = "+229 97 00 00 00",
                leadingIcon = Icons.Default.Phone,
                keyboardType = KeyboardType.Phone,
                errorMessage = form.phoneError,
                testTag = "input_register_phone",
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 6. Mot de passe
            AuthTextField(
                value = form.password,
                onValueChange = { viewModel.updatePassword(it) },
                label = "Mot de passe (min. 6 caractères)",
                placeholder = "••••••••",
                leadingIcon = Icons.Default.Lock,
                keyboardType = KeyboardType.Password,
                isPassword = true,
                isPasswordVisible = isPasswordVisible,
                onTogglePassword = { isPasswordVisible = !isPasswordVisible },
                errorMessage = form.passwordError,
                testTag = "input_register_password",
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 7. Confirmation mot de passe
            AuthTextField(
                value = form.confirmPassword,
                onValueChange = { viewModel.updateConfirmPassword(it) },
                label = "Confirmer le mot de passe",
                placeholder = "••••••••",
                leadingIcon = Icons.Default.Lock,
                keyboardType = KeyboardType.Password,
                isPassword = true,
                isPasswordVisible = isConfirmPasswordVisible,
                onTogglePassword = { isConfirmPasswordVisible = !isConfirmPasswordVisible },
                errorMessage = form.confirmPasswordError,
                imeAction = ImeAction.Done,
                testTag = "input_register_confirm_password",
                onDone = {
                    focusManager.clearFocus()
                    viewModel.register()
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Case d'acceptation des conditions
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.toggleTerms(!form.acceptTerms) }
                    .padding(vertical = 4.dp)
            ) {
                Checkbox(
                    checked = form.acceptTerms,
                    onCheckedChange = { viewModel.toggleTerms(it) },
                    colors = CheckboxDefaults.colors(
                        checkedColor = VideoCashPurplePrimary,
                        uncheckedColor = Color(0xFF887EA5),
                        checkmarkColor = Color.White
                    ),
                    modifier = Modifier.testTag("checkbox_accept_terms")
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "J'accepte les conditions générales et la politique de confidentialité de VidéoCash.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = if (form.termsError != null) Color(0xFFFDA4AF) else Color(0xFFD4CDE6)
                    )
                )
            }

            if (form.termsError != null) {
                Text(
                    text = form.termsError ?: "",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFFFDA4AF),
                        fontSize = 11.sp
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, top = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Bouton de soumission
            val isLoading = uiState is AuthUiState.Loading
            Button(
                onClick = {
                    focusManager.clearFocus()
                    viewModel.register()
                },
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = VideoCashPurplePrimary,
                    disabledContainerColor = Color(0xFF382361)
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("btn_submit_register")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = VideoCashGoldAccent,
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Text(
                        text = "Créer mon compte",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Séparateur ou
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(Color(0xFF281F47))
                )
                Text(
                    text = "OU",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color(0xFF8679A8),
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(horizontal = 14.dp)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(Color(0xFF281F47))
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Inscription / Connexion avec Google
            GoogleSignInButton(
                text = "S'inscrire avec Google",
                isLoading = isLoading,
                onClick = {
                    focusManager.clearFocus()
                    coroutineScope.launch {
                        GoogleAuthHelper.requestGoogleIdToken(
                            context = context,
                            onTokenReceived = { idToken ->
                                viewModel.signInWithGoogle(idToken)
                            },
                            onError = { errorMsg ->
                                viewModel.setError(errorMsg)
                            }
                        )
                    }
                },
                testTag = "btn_register_google"
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Lien retour à la connexion
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Text(
                    text = "Vous avez déjà un compte ? ",
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF9E95B8))
                )
                Text(
                    text = "Se connecter",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = VideoCashGoldAccent,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier
                        .clickable { onNavigateToLogin() }
                        .padding(4.dp)
                        .testTag("btn_goto_login")
                )
            }
        }
    }
}

@Composable
fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    errorMessage: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    isPasswordVisible: Boolean = false,
    onTogglePassword: (() -> Unit)? = null,
    imeAction: ImeAction = ImeAction.Next,
    testTag: String,
    onNext: (() -> Unit)? = null,
    onDone: (() -> Unit)? = null
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            isError = errorMessage != null,
            leadingIcon = {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = label,
                    tint = if (errorMessage != null) Color(0xFFF43F5E) else VideoCashGoldAccent
                )
            },
            trailingIcon = if (isPassword && onTogglePassword != null) {
                {
                    IconButton(onClick = onTogglePassword) {
                        Icon(
                            imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (isPasswordVisible) "Masquer" else "Afficher",
                            tint = Color(0xFFA89FBF)
                        )
                    }
                }
            } else null,
            singleLine = true,
            visualTransformation = if (isPassword && !isPasswordVisible) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = imeAction
            ),
            keyboardActions = KeyboardActions(
                onNext = { onNext?.invoke() },
                onDone = { onDone?.invoke() }
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = VideoCashPurplePrimary,
                unfocusedBorderColor = Color(0xFF281F47),
                errorBorderColor = Color(0xFFE11D48),
                focusedLabelColor = VideoCashGoldAccent,
                unfocusedLabelColor = Color(0xFF9086AA),
                errorLabelColor = Color(0xFFFDA4AF),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = Color(0xFF140D29),
                unfocusedContainerColor = Color(0xFF140D29),
                errorContainerColor = Color(0xFF1B0E28)
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag)
        )

        if (errorMessage != null) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFFFDA4AF),
                    fontSize = 11.sp
                ),
                modifier = Modifier.padding(start = 12.dp, top = 2.dp)
            )
        }
    }
}
