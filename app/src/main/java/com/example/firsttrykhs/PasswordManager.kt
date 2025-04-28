package com.example.firsttrykhs

import android.content.SharedPreferences
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import at.favre.lib.crypto.bcrypt.BCrypt

class PasswordManager(
    private val sharedPreferences: SharedPreferences,
    private var correctPassword: String,
    private var showUrlInput: Boolean
) {

    // Save the admin password to SharedPreferences (hashed)
    fun savePassword(newPassword: String) {
        val bcryptHashString = BCrypt.withDefaults().hashToString(12, newPassword.toCharArray())
        sharedPreferences.edit().putString("ADMIN_PASSWORD", bcryptHashString).apply()
        correctPassword = bcryptHashString // Update stored hashed password
        Log.d("PasswordManager", "Admin password saved (hashed): $bcryptHashString")
    }

    // Retrieve the admin password hash from SharedPreferences
    fun getPasswordHash(): String {
        return sharedPreferences.getString("ADMIN_PASSWORD", "") ?: ""
    }

    fun debugPrintStoredPassword() {
        val stored = sharedPreferences.getString("ADMIN_PASSWORD", null)
        if (stored == null) {
            Log.d("DEBUG", "No password stored (null)")
        } else {
            Log.d("DEBUG", "Stored password: $stored")
        }
    }

    // Re-enable password verification logic
    fun verifyPassword(inputPassword: String): Boolean {
        val storedPassword = getPasswordHash()

        if (storedPassword.isBlank()) {
            // No password saved yet, fallback to default password (12345)
            return inputPassword == "12345"
        }

        return if (storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$")) {
            // Stored password is a bcrypt hash
            val result = BCrypt.verifyer().verify(inputPassword.toCharArray(), storedPassword)
            result.verified
        } else {
            // Stored password is plain text (legacy)
            inputPassword == storedPassword
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ChangePasswordDialog(
        showDialog: Boolean,
        onDismiss: () -> Unit,
        onPasswordChanged: (String) -> Unit
    ) {
        var adminPassword by remember { mutableStateOf("") }
        var newPassword by remember { mutableStateOf("") }
        var confirmNewPassword by remember { mutableStateOf("") }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        // Color for the border when there is an error
        val errorBorderColor = Color.Red
        val normalBorderColor = Color.Gray

        if (showDialog) {
            AlertDialog(
                modifier = Modifier.padding(16.dp),
                onDismissRequest = onDismiss,
                title = { Text("Passwort ändern", fontSize = 18.sp) },
                text = {
                    Column {
                        // Current Password TextField
                        TextField(
                            value = adminPassword,
                            onValueChange = { adminPassword = it },
                            visualTransformation = PasswordVisualTransformation(),
                            placeholder = { Text("Aktuelles Passwort") },
                            singleLine = true,
                            isError = errorMessage != null && adminPassword.isNotEmpty(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            colors = TextFieldDefaults.textFieldColors(
                                containerColor = Color.White,
                                focusedIndicatorColor = if (errorMessage != null && adminPassword.isNotEmpty()) errorBorderColor else Color.Blue,
                                unfocusedIndicatorColor = normalBorderColor
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // New Password TextField
                        TextField(
                            value = newPassword,
                            onValueChange = { newPassword = it },
                            visualTransformation = PasswordVisualTransformation(),
                            placeholder = { Text("Neues Passwort") },
                            singleLine = true,
                            isError = errorMessage != null && newPassword.isNotEmpty(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            colors = TextFieldDefaults.textFieldColors(
                                containerColor = Color.White,
                                focusedIndicatorColor = if (errorMessage != null && newPassword.isNotEmpty()) errorBorderColor else Color.Blue,
                                unfocusedIndicatorColor = normalBorderColor
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Confirm New Password TextField
                        TextField(
                            value = confirmNewPassword,
                            onValueChange = { confirmNewPassword = it },
                            visualTransformation = PasswordVisualTransformation(),
                            placeholder = { Text("Neues Passwort bestätigen") },
                            singleLine = true,
                            isError = errorMessage != null && confirmNewPassword.isNotEmpty(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            colors = TextFieldDefaults.textFieldColors(
                                containerColor = Color.White,
                                focusedIndicatorColor = if (errorMessage != null && confirmNewPassword.isNotEmpty()) errorBorderColor else Color.Blue,
                                unfocusedIndicatorColor = normalBorderColor
                            )
                        )

                        // Displaying error messages
                        errorMessage?.let {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(it, color = Color.Red, fontSize = 14.sp)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            when {
                                adminPassword.isEmpty() || newPassword.isEmpty() || confirmNewPassword.isEmpty() -> {
                                    errorMessage = "Bitte alle Felder ausfüllen."
                                }
                                !verifyPassword(adminPassword) -> {
                                    errorMessage = "Aktuelles Passwort ist falsch."
                                    adminPassword = "" // Reset the field if incorrect password
                                }
                                newPassword != confirmNewPassword -> {
                                    errorMessage = "Neues Passwort und Bestätigung stimmen nicht überein."
                                    // Clear both new password and confirmation password if they don't match
                                    newPassword = ""
                                    confirmNewPassword = ""
                                }
                                else -> {
                                    savePassword(newPassword) // Save the new password
                                    onPasswordChanged(newPassword) // Update the UI or notify the password change
                                    onDismiss() // Close the dialog
                                    // Clear fields after successful change
                                    adminPassword = ""
                                    newPassword = ""
                                    confirmNewPassword = ""
                                    errorMessage = ""
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text("Bestätigen", style = MaterialTheme.typography.bodyMedium)
                    }
                },
                dismissButton = {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text("Abbrechen", style = MaterialTheme.typography.bodyMedium)
                    }
                },
                containerColor = Color.LightGray
            )
        }
    }

}
