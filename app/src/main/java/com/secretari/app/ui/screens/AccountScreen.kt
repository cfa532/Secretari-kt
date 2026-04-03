package com.secretari.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.secretari.app.R
import com.secretari.app.data.model.User
import com.secretari.app.util.UserManager
import kotlinx.serialization.InternalSerializationApi
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, InternalSerializationApi::class)
@Composable
fun AccountScreen(
    user: User?,
    loginStatus: UserManager.LoginStatus,
    showLoginFormForAnonymous: Boolean,
    showRegisterFormForAnonymous: Boolean,
    onLogin: (String, String) -> Unit,
    onRegister: (User) -> Unit,
    onBack: () -> Unit,
    onShowLoginForm: () -> Unit,
    onHideLoginForm: () -> Unit,
    onShowRegisterForm: () -> Unit,
    onHideRegisterForm: () -> Unit
) {
    val titleText = stringResource(when (loginStatus) {
        UserManager.LoginStatus.SIGNED_IN -> R.string.account
        UserManager.LoginStatus.SIGNED_OUT -> R.string.login
        UserManager.LoginStatus.UNREGISTERED -> R.string.register
    })

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(titleText) },
                navigationIcon = {
                    IconButton(onClick = {
                        when {
                            showLoginFormForAnonymous -> onHideLoginForm()
                            showRegisterFormForAnonymous -> onHideRegisterForm()
                            else -> onBack()
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (loginStatus) {
                UserManager.LoginStatus.SIGNED_IN -> {
                    if ((user?.username?.length ?: 0) > 20) {
                        when {
                            showLoginFormForAnonymous -> {
                                LoginForm(onLogin = onLogin, onBack = onHideLoginForm, onShowRegisterForm = onShowRegisterForm)
                            }
                            showRegisterFormForAnonymous -> {
                                RegisterForm(onRegister = onRegister, onBack = onHideRegisterForm, onShowLoginForm = onShowLoginForm)
                            }
                            else -> {
                                AnonymousUserProfile(user = user, onRegister = onRegister, onLogin = onLogin, onShowLoginForm = onShowLoginForm, onShowRegisterForm = onShowRegisterForm)
                            }
                        }
                    } else {
                        AccountDetails(user = user)
                    }
                }
                UserManager.LoginStatus.SIGNED_OUT -> {
                    LoginForm(onLogin = onLogin)
                }
                UserManager.LoginStatus.UNREGISTERED -> {
                    RegisterForm(onRegister = onRegister)
                }
            }
        }
    }
}

@OptIn(InternalSerializationApi::class)
@Composable
fun AccountDetails(user: User?) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.account_information),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            user?.let {
                val anonymousUserStr = stringResource(R.string.anonymous_user)
                val displayName = if (it.username.length > 20) anonymousUserStr else it.username
                InfoRow(stringResource(R.string.username), displayName)

                if (it.username.length > 20) {
                    InfoRow(stringResource(R.string.token_usage), it.tokenCount.toString())
                } else {
                    it.email?.let { email ->
                        InfoRow(stringResource(R.string.email), email)
                    }
                    InfoRow(stringResource(R.string.name), "${it.givenName ?: ""} ${it.familyName ?: ""}".trim())
                    val estimatedTokens = estimateTokens(it.dollarBalance)
                    InfoRow(stringResource(R.string.account_balance_usd), estimatedTokens.toString())
                }
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

private fun estimateTokens(dollarBalance: Double): Int {
    return (dollarBalance * 4 * 1000000 / 30).toInt()
}

@Composable
fun LoginForm(onLogin: (String, String) -> Unit, onBack: (() -> Unit)? = null, onShowRegisterForm: (() -> Unit)? = null) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        onBack?.let {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                TextButton(onClick = it) {
                    Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    Text(stringResource(R.string.back))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text(stringResource(R.string.username)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(stringResource(R.string.password)) },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                isLoading = true
                onLogin(username, password)
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = username.isNotBlank() && password.isNotBlank() && !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(stringResource(R.string.login))
            }
        }

        onShowRegisterForm?.let {
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(
                onClick = it,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.create_account_prompt))
            }
        }
    }
}

@OptIn(InternalSerializationApi::class)
@Composable
fun RegisterForm(onRegister: (User) -> Unit, onBack: (() -> Unit)? = null, onShowLoginForm: (() -> Unit)? = null) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var givenName by remember { mutableStateOf("") }
    var familyName by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val usernameValidationMsg = stringResource(R.string.username_validation_error)
    val passwordRequiredMsg = stringResource(R.string.password_required_error)
    val passwordsMismatchMsg = stringResource(R.string.passwords_do_not_match)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            onBack?.let {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    TextButton(onClick = it) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                        Text(stringResource(R.string.back))
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        item {
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text(stringResource(R.string.username)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(stringResource(R.string.password)) },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text(stringResource(R.string.confirm_password)) },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                isError = confirmPassword.isNotEmpty() && password != confirmPassword
            )

            if (confirmPassword.isNotEmpty() && password != confirmPassword) {
                Text(
                    text = passwordsMismatchMsg,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(stringResource(R.string.email)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = givenName,
                onValueChange = { givenName = it },
                label = { Text(stringResource(R.string.first_name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = familyName,
                onValueChange = { familyName = it },
                label = { Text(stringResource(R.string.last_name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    when {
                        username.isBlank() || username.length > 20 -> {
                            errorMessage = usernameValidationMsg
                            showError = true
                        }
                        password.isBlank() -> {
                            errorMessage = passwordRequiredMsg
                            showError = true
                        }
                        password != confirmPassword -> {
                            errorMessage = passwordsMismatchMsg
                            showError = true
                        }
                        else -> {
                            isLoading = true
                            showError = false
                            val user = User(
                                id = java.util.UUID.randomUUID().toString(),
                                username = username,
                                password = password,
                                email = email,
                                givenName = givenName,
                                familyName = familyName
                            )
                            onRegister(user)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = username.isNotBlank() && password.isNotBlank() && confirmPassword.isNotBlank() && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(stringResource(R.string.sign_up))
                }
            }

            if (showError) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            onShowLoginForm?.let {
                TextButton(
                    onClick = it,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.sign_in_prompt))
                }
            }
        }
    }
}

@OptIn(InternalSerializationApi::class)
@Composable
fun AnonymousUserProfile(
    user: User?,
    onRegister: (User) -> Unit,
    onLogin: (String, String) -> Unit,
    onShowLoginForm: () -> Unit,
    onShowRegisterForm: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            AccountDetails(user = user)
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
        item { Spacer(modifier = Modifier.height(24.dp)) }

        item {
            Button(
                onClick = { onShowRegisterForm() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = stringResource(R.string.create_account),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }

        item {
            OutlinedButton(
                onClick = { onShowLoginForm() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = stringResource(R.string.sign_in_existing),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.benefits_of_registration),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val benefits = listOf(
                        stringResource(R.string.benefit_save_recordings),
                        stringResource(R.string.benefit_premium_features),
                        stringResource(R.string.benefit_sync_devices),
                        stringResource(R.string.benefit_backup_data)
                    )

                    benefits.forEach { benefit ->
                        Row(
                            modifier = Modifier.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = benefit,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}
