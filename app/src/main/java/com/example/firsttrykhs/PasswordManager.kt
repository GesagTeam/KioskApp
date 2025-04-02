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

class PasswordManager(
    private val sharedPreferences: SharedPreferences,
    private var correctPassword: String,
    private var showUrlInput: Boolean
) {



    // Save the admin password to SharedPreferences
    fun savePassword(newPassword: String) {
        sharedPreferences.edit().putString("ADMIN_PASSWORD", newPassword).apply()
        correctPassword = newPassword // Update the correctPassword state
        Log.d("PasswordManager", "Admin password saved: $newPassword")
    }

    // Retrieve the admin password from SharedPreferences
    fun getPassword(): String {
        return sharedPreferences.getString("ADMIN_PASSWORD", "12345") ?: "12345"
    }

    // Show a dialog to change or assign a new password
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
        var isPasswordIncorrect by remember { mutableStateOf(false) }
        var isPasswordIncorrectTwo by remember { mutableStateOf(false) }
        var isPasswordIncorrectThree by remember { mutableStateOf(false) }
        var isPasswordIncorrectFour by remember { mutableStateOf(false) }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("Passwort ändern", fontSize = 18.sp) },
                text = {
                    Column {
                        // Step 1: Ask for the current admin password
                        TextField(
                            value = adminPassword,
                            onValueChange = { adminPassword = it },
                            visualTransformation = PasswordVisualTransformation(),
                            placeholder = { Text("Aktuelles Passwort") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.textFieldColors(containerColor = Color.White)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Step 2: Ask for the new password and confirmation
                        TextField(
                            value = newPassword,
                            onValueChange = { newPassword = it },
                            visualTransformation = PasswordVisualTransformation(),
                            placeholder = { Text("Neues Passwort") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.textFieldColors(containerColor = Color.White)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        TextField(
                            value = confirmNewPassword,
                            onValueChange = { confirmNewPassword = it },
                            visualTransformation = PasswordVisualTransformation(),
                            placeholder = { Text("Neues Passwort bestätigen") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.textFieldColors(containerColor = Color.White)
                        )

                        if (isPasswordIncorrect) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Aktuelles Passwort ist Falsch! Bitte versuchen Sie es erneut.",
                                color = Color.Red,
                                fontSize = 14.sp
                            )
                        }
                        if (isPasswordIncorrectTwo) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Aktuelles Passwort ist Falsch! Bitte versuchen Sie es erneut.",
                                color = Color.Red,
                                fontSize = 14.sp
                            )
                        }


                        if (isPasswordIncorrectThree) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Bitte füllen sie die restlichen Felder aus.",
                                color = Color.Red,
                                fontSize = 14.sp
                            )
                        }

                        if (isPasswordIncorrectFour) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Neues Passwort und passwortbestätigung stimmen nicht überein.",
                                color = Color.Red,
                                fontSize = 14.sp
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        if (adminPassword != correctPassword && newPassword == "" && confirmNewPassword =="") {
                            // Validate the current password
                            isPasswordIncorrect = true
                            adminPassword = ""
                        } else if (newPassword != confirmNewPassword) {
                            // Validate new password and confirmation
                            isPasswordIncorrectFour = true
                            newPassword = ""
                            confirmNewPassword = ""
                        }
                        else if (newPassword == confirmNewPassword && newPassword == "" && confirmNewPassword =="") {
                            // Validate new password and confirmation
                            isPasswordIncorrectThree = true
                        }

                        else if (newPassword == confirmNewPassword && adminPassword != correctPassword) {
                            // Validate new password and confirmation
                            isPasswordIncorrect = true
                            adminPassword = ""
                        }
                             else {
                            // Save the new password and notify the parent
                            savePassword(newPassword)
                            onPasswordChanged(newPassword)
                            onDismiss()
                        }
                    }) {
                        Text("Bestätigen")
                    }
                },
                dismissButton = {
                    Button(onClick = onDismiss) {
                        Text("Abbrechen")

                        isPasswordIncorrect = false
                        isPasswordIncorrectTwo = false
                        isPasswordIncorrectThree = false
                        isPasswordIncorrectFour = false
                        adminPassword = ""
                        newPassword = ""
                        confirmNewPassword = ""
                    }
                },
                containerColor = Color.LightGray
            )

        }
    } }

