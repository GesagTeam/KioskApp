// WebBrowser.kt
package com.example.firsttrykhs

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.net.http.SslError
import android.os.Handler
import android.util.Log
import android.webkit.SslErrorHandler
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import kotlinx.coroutines.delay
import androidx.compose.animation.core.tween
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.text.TextRange
import java.io.File

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebBrowser(modifier: Modifier = Modifier) {

    val context = LocalContext.current
    val sharedPreferences = remember { context.getSharedPreferences("WebBrowserPrefs", Context.MODE_PRIVATE) }

    fun collectMetrics(): Map<String, Any> {
        return mapOf(
            "webview_memory_kb" to Runtime.getRuntime().totalMemory() / 1024,
            "storage_bytes" to File(context.filesDir, "shared_prefs/WebBrowserPrefs.xml").length(),
            "failed_auth_attempts" to sharedPreferences.getInt("failed_attempts", 0)
        )
    }


    // Initialize PasswordManager with default password
    var adminPassword by remember { mutableStateOf(sharedPreferences.getString("ADMIN_PASSWORD", "12345") ?: "12345") }
    var showUrlInput by remember { mutableStateOf(false) }
    val passwordManager = remember { PasswordManager(context, adminPassword) }

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    var url by remember { mutableStateOf(loadSavedUrl(sharedPreferences)) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var textFieldValue by remember { mutableStateOf(TextFieldValue(url)) }
    var showPasswordDialog by remember { mutableStateOf(false) }

    var enteredPassword by remember { mutableStateOf("") }
    var isPasswordIncorrect by remember { mutableStateOf(false) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var shouldSelectAllText by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }

    // Track the scroll behavior
    var isTopBarVisible by remember { mutableStateOf(true) }
    val scrollState = rememberScrollState()

    // Hide top bar after some delay when no interaction
    val handler = remember { Handler() }
    var isUserInteracting by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            val metrics = collectMetrics()
            Log.d("AUTO_METRICS", metrics.toString())
            delay(1000) // Every minute
        }
    }

    // Monitor user interaction
    LaunchedEffect(isUserInteracting) {
        if (!isUserInteracting) {
            delay(2000)
            isTopBarVisible = false
        }
    }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                return super.onPreScroll(available, source)
            }
        }
    }

    val resetInteractionTimer = {
        isUserInteracting = true
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({
            isUserInteracting = false
        }, 2000)
    }

    Scaffold(
        topBar = {
            AnimatedVisibility(
                visible = isTopBarVisible,
                enter = fadeIn(animationSpec = tween(durationMillis = 500)),
                exit = fadeOut(animationSpec = tween(durationMillis = 500))
            ) {
                TopAppBar(
                    title = {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(80.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.khs_logo),
                                contentDescription = "App Logo",
                                modifier = Modifier.size(80.dp)
                            )
                        }
                    },
                    navigationIcon = {
                        Row {
                            IconButton(
                                onClick = { webView?.goBack() },
                                enabled = canGoBack
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Previous Page",
                                    tint = if (canGoBack) Color.Black else Color.Gray
                                )
                            }
                            IconButton(
                                onClick = { webView?.goForward() },
                                enabled = canGoForward
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Next Page",
                                    tint = if (canGoForward) Color.Black else Color.Gray
                                )
                            }
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = {
                                showPasswordDialog = true
                                resetInteractionTimer()
                            },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = Color.White,
                                containerColor = Color.Gray
                            )
                        ) {
                            Text("URL Eingeben")
                        }
                        TextButton(
                            modifier = Modifier.padding(5.dp),
                            onClick = {
                                showChangePasswordDialog = true
                                showUrlInput = false
                                resetInteractionTimer()
                            },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = Color.White,
                                containerColor = Color.Gray
                            )
                        ) {
                            Text("Passwort ändern")
                        }
                    }
                )
            }
        },
        content = { paddingValues ->
            Column(
                Modifier
                    .padding(paddingValues)
                    .nestedScroll(nestedScrollConnection)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures { _, dragAmount ->
                            if (dragAmount > 0) {
                                isTopBarVisible = true
                            }
                            else if (dragAmount < 0) {
                                isTopBarVisible = false
                            }
                            resetInteractionTimer()
                        }
                    }
            ) {
                if (showUrlInput) {
                    TextField(
                        value = textFieldValue,
                        onValueChange = { newValue ->
                            textFieldValue = newValue
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .focusRequester(focusRequester)
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused && shouldSelectAllText) {
                                    textFieldValue = textFieldValue.copy(
                                        selection = TextRange(0, textFieldValue.text.length)
                                    )
                                    shouldSelectAllText = false
                                }
                            },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                loadUrlFromTextField(textFieldValue.text) { formattedUrl ->
                                    url = formattedUrl
                                    webView?.loadUrl(url)
                                    saveUrl(sharedPreferences, url)
                                    showUrlInput = false
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                }
                            }
                        ),
                        placeholder = { Text("Geben Sie die URL ein") },
                        colors = TextFieldDefaults.textFieldColors(containerColor = Color.White)
                    )

                    LaunchedEffect(showUrlInput) {
                        if (showUrlInput) {
                            focusRequester.requestFocus()
                            shouldSelectAllText = true
                        }
                    }
                }

                AndroidView(
                    modifier = Modifier.fillMaxSize().padding(8.dp),
                    factory = { context ->
                        WebView(context).apply {
                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    canGoBack = view?.canGoBack() ?: false
                                    canGoForward = view?.canGoForward() ?: false
                                }

                                @SuppressLint("WebViewClientOnReceivedSslError")
                                override fun onReceivedSslError(
                                    view: WebView?,
                                    handler: SslErrorHandler,
                                    error: SslError
                                ) {
                                    handler.proceed()
                                }
                            }
                            settings.javaScriptEnabled = true
                            loadUrl(url)
                            webView = this
                        }
                    },
                    update = { webView?.loadUrl(url) }
                )
            }
        }
    )

    if (showPasswordDialog) {
        AlertDialog(
            onDismissRequest = {
                showPasswordDialog = false
                enteredPassword = ""
                isPasswordIncorrect = false
            },
            title = { Text("Bitte geben Sie das Passwort ein", fontSize = 18.sp) },
            text = {
                Column {
                    TextField(
                        value = enteredPassword,
                        onValueChange = {
                            enteredPassword = it
                            isPasswordIncorrect = false
                        },
                        visualTransformation = PasswordVisualTransformation(),
                        placeholder = { Text("Passwort") },
                        singleLine = true,
                        colors = TextFieldDefaults.textFieldColors(containerColor = Color.White)
                    )
                    if (isPasswordIncorrect) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Falsches Passwort! Bitte versuchen Sie es erneut.", color = Color.Red, fontSize = 14.sp)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (passwordManager.verifyPassword(enteredPassword)) {
                        showPasswordDialog = false
                        enteredPassword = ""
                        isPasswordIncorrect = false
                        textFieldValue = TextFieldValue(url)
                        shouldSelectAllText = true
                        showUrlInput = true
                    } else {
                        isPasswordIncorrect = true
                        enteredPassword = ""
                    }
                }) {
                    Text("Bestätigen")
                }
            },
            dismissButton = {
                Button(onClick = {
                    showPasswordDialog = false
                    enteredPassword = ""
                    isPasswordIncorrect = false
                }) {
                    Text("Abbrechen")
                }
            },
            containerColor = Color.LightGray
        )
    }

    passwordManager.ChangePasswordDialog(
        showDialog = showChangePasswordDialog,
        onDismiss = { showChangePasswordDialog = false },
        onPasswordChanged = { newPassword ->
            adminPassword = passwordManager.getPasswordHash()
        }
    )
}

fun saveUrl(sharedPreferences: SharedPreferences, url: String) {
    sharedPreferences.edit().putString("LAST_URL", url).apply()
}

fun loadSavedUrl(sharedPreferences: SharedPreferences): String {
    return sharedPreferences.getString("LAST_URL", "https://www.google.com") ?: "https://www.google.com"
}

fun loadUrlFromTextField(inputUrl: String, onUrlUpdated: (String) -> Unit) {
    val formattedUrl = if (!inputUrl.startsWith("http://") && !inputUrl.startsWith("https://")) {
        "https://$inputUrl"
    } else {
        inputUrl
    }
    onUrlUpdated(formattedUrl)
}