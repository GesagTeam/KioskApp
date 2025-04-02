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
fun AdminExitDialog(onDismiss: () -> Unit, onUnlock: () -> Unit) {
    var password by remember { mutableStateOf("") }
    val correctPassword = "1234" // Change this password as needed
    var isPasswordIncorrect by remember { mutableStateOf(false) } // Track if password is incorrect

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Admin-Zugang") },
        text = {
            Column {
                Text("Geben Sie das Admin-Passwort ein, um den Kiosk-Modus zu verlassen", fontSize = 20.sp)
                Spacer(modifier = Modifier.height(8.dp))

                // Conditionally apply red border and border radius if the password is incorrect
                TextField(
                    value = password,
                    onValueChange = {
                        password = it
                        isPasswordIncorrect = false // Reset error when typing
                    },
                    label = { Text("Passwort", modifier = Modifier.fillMaxWidth()) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions.Default.copy(
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (password == correctPassword) {
                                onUnlock()
                            } else {
                                isPasswordIncorrect = true // Mark password as incorrect if wrong
                            }
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (isPasswordIncorrect) {
                                Modifier
                                    .border(2.dp, Color.Red, RoundedCornerShape(8.dp)) // Apply red border with rounded corners
                            } else {
                                Modifier
                                    .border(2.dp, Color.Gray, RoundedCornerShape(8.dp)) // Default border color
                            }
                        ),
                    colors = TextFieldDefaults.textFieldColors(
                        containerColor = Color.White,
                        focusedIndicatorColor = Color.Transparent, // Remove underline when focused
                        unfocusedIndicatorColor = Color.Transparent // Remove underline when not focused
                    )
                )

                // Display error message if the password is incorrect
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
                    onUnlock()
                } else {
                    isPasswordIncorrect = true // Mark password as incorrect if wrong
                }
            }) {
                Text("Entsperren")
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
