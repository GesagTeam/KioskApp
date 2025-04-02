package com.example.firsttrykhs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReLockDialog(onDismiss: () -> Unit, onReLock: () -> Unit) {
    var password by remember { mutableStateOf("") }
    var isPasswordIncorrect by remember { mutableStateOf(false) }
    val correctPassword = "1234" // Change this as needed

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Kiosk-Modus erneut sperren") },
        text = {
            Column {
                Text("Geben Sie das Passwort ein, um den Kiosk-Modus erneut zu sperren:", fontSize = 18.sp)
                Spacer(modifier = Modifier.height(8.dp))

                TextField(
                    value = password,
                    onValueChange = {
                        password = it
                        isPasswordIncorrect = false // Reset error when typing
                    },
                    singleLine = true,
                    isError = isPasswordIncorrect,
                    label = { Text("Passwort") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (password == correctPassword) {
                                onReLock()
                            } else {
                                isPasswordIncorrect = true
                            }
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            2.dp,
                            if (isPasswordIncorrect) Color.Red else Color.Gray,
                            RoundedCornerShape(8.dp)
                        )
                        .background(Color.White, shape = RoundedCornerShape(8.dp)),
                    colors = TextFieldDefaults.textFieldColors(
                        containerColor = Color.White,
                        focusedIndicatorColor = Color.Transparent, // Remove underline when focused
                        unfocusedIndicatorColor = Color.Transparent // Remove underline when not focused
                    )
                )

                // Display error message if password is incorrect
                if (isPasswordIncorrect) {
                    Text(
                        text = "Falsches Passwort! Bitte versuchen Sie es erneut.",
                        color = Color.Red,
                        fontSize = 14.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (password == correctPassword) {
                    onReLock()
                } else {
                    isPasswordIncorrect = true // Show error if password is wrong
                }
            }) {
                Text("Sperren")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
        ,containerColor = Color.LightGray
    )
}
