// WebBrowser.kt
package com.example.firsttrykhs

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.net.http.SslError
import android.os.Handler
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

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebBrowser(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val sharedPreferences = remember { context.getSharedPreferences("WebBrowserPrefs", Context.MODE_PRIVATE) }

    // Create a MutableState for the correct password
    var correctPassword by remember { mutableStateOf(sharedPreferences.getString("ADMIN_PASSWORD", "12345") ?: "12345") }
    var showUrlInput by remember { mutableStateOf(false) }
    // Pass the correctPassword state to the PasswordManager
    val passwordManager = remember { PasswordManager(sharedPreferences, correctPassword, showUrlInput) }

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

    // Monitor user interaction
    LaunchedEffect(isUserInteracting) {
        if (!isUserInteracting) {
            delay(2000) // Add a 2-second delay to allow smooth hiding
            isTopBarVisible = false
        }
    }

    // Create a NestedScrollConnection for handling scrolling behavior
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // If we are scrolling vertically, detect swipe direction
                return super.onPreScroll(available, source)
            }
        }
    }

    // Reset the inactivity timer whenever user interacts
    val resetInteractionTimer = {
        isUserInteracting = true
        handler.removeCallbacksAndMessages(null) // Clear previous interactions
        handler.postDelayed({
            isUserInteracting = false // After 2 seconds of inactivity, hide the top bar
        }, 2000) // Reset timer to 2 seconds
    }

    Scaffold(
        topBar = {
            AnimatedVisibility(
                visible = isTopBarVisible,
                enter = fadeIn(animationSpec = tween(durationMillis = 500)), // Medium speed fade-in
                exit = fadeOut(animationSpec = tween(durationMillis = 500)) // Medium speed fade-out
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
                    .nestedScroll(nestedScrollConnection) // Apply nested scroll connection
                    .pointerInput(Unit) {
                        detectVerticalDragGestures { _, dragAmount ->
                            // If the user swipes down, show the top bar
                            if (dragAmount > 0) {
                                isTopBarVisible = true
                            }
                            // If the user swipes up, hide the top bar
                            else if (dragAmount < 0) {
                                isTopBarVisible = false
                            }
                            resetInteractionTimer() // Reset timer on scroll as well
                        }
                    }
            ) {
                if (showUrlInput) {
                    // Ensure the input field shows up even without internet
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
                                    // Select all text when the field gains focus
                                    textFieldValue = textFieldValue.copy(
                                        selection = TextRange(0, textFieldValue.text.length)
                                    )
                                    shouldSelectAllText = false // Reset the flag
                                }
                            },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                loadUrlFromTextField(textFieldValue.text) { formattedUrl ->
                                    url = formattedUrl
                                    webView?.loadUrl(url) // If there's no internet, this won't block UI
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
                            // Request focus after the TextField is composed
                            focusRequester.requestFocus()
                            shouldSelectAllText = true // Set the flag to select all text
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
                                    // Allow invalid certificates (for testing only)
                                    handler.proceed()
                                }
                            }
                            settings.javaScriptEnabled = true
                            loadUrl(url) // Handle URL even if there's no internet
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
                enteredPassword = "" // Clear password on dismissal
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
                    if (enteredPassword == correctPassword) {
                        showPasswordDialog = false
                        enteredPassword = "" // Clear password on success
                        isPasswordIncorrect = false
                        textFieldValue = TextFieldValue(url) // Reset TextFieldValue with the current URL
                        shouldSelectAllText = true // Set the flag to select all text
                        showUrlInput = true // Show the URL input field
                    } else {
                        isPasswordIncorrect = true
                        enteredPassword = "" // Clear password on incorrect attempt
                    }
                }) {
                    Text("Bestätigen")
                }
            },
            dismissButton = {
                Button(onClick = {
                    showPasswordDialog = false
                    enteredPassword = "" // Clear password on dismissal
                    isPasswordIncorrect = false
                }) {
                    Text("Abbrechen")
                }
            },
            containerColor = Color.LightGray
        )
    }

    // Use PasswordManager's ChangePasswordDialog
    passwordManager.ChangePasswordDialog(

        showDialog = showChangePasswordDialog,
        onDismiss = { showChangePasswordDialog = false },
        onPasswordChanged = { newPassword ->
            correctPassword = newPassword // Update the correct password
        }
    )

    // Use PasswordManager's ForgotPasswordDialog

}

// Save the URL to SharedPreferences
fun saveUrl(sharedPreferences: SharedPreferences, url: String) {
    sharedPreferences.edit().putString("LAST_URL", url).apply()
}

// Load the saved URL from SharedPreferences
fun loadSavedUrl(sharedPreferences: SharedPreferences): String {
    return sharedPreferences.getString("LAST_URL", "https://www.google.com") ?: "https://www.google.com"
}

// Format and load the URL from the input field
fun loadUrlFromTextField(inputUrl: String, onUrlUpdated: (String) -> Unit) {
    val formattedUrl = if (!inputUrl.startsWith("http://") && !inputUrl.startsWith("https://")) {
        "https://$inputUrl"
    } else {
        inputUrl
    }
    onUrlUpdated(formattedUrl)
}