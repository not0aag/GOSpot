package week11.st695922.finalproject.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import week11.st695922.finalproject.ui.components.ErrorBanner
import week11.st695922.finalproject.ui.components.LabeledTextField
import week11.st695922.finalproject.ui.components.PrimaryButton
import week11.st695922.finalproject.ui.theme.GoGreen

@Composable
fun SignInScreen(
    formError: String?,
    isSubmitting: Boolean,
    passwordResetSent: Boolean,
    onSignIn: (email: String, password: String) -> Unit,
    onForgotPassword: (email: String) -> Unit,
    onNavigateToCreateAccount: () -> Unit,
    modifier: Modifier = Modifier
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showForgotPasswordField by remember { mutableStateOf(false) }
    var resetEmail by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        Box(
            modifier = Modifier
                .padding(top = 24.dp, bottom = 16.dp)
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(GoGreen),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.LocationOn, contentDescription = null, tint = Color.White)
        }

        Text(
            text = "Welcome back",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Sign in to see live lot occupancy and enable automatic check-ins.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
        )

        LabeledTextField(
            label = "Email",
            value = email,
            onValueChange = { email = it },
            placeholder = "you@example.com"
        )
        Spacer(Modifier.height(16.dp))
        LabeledTextField(
            label = "Password",
            value = password,
            onValueChange = { password = it },
            isPassword = true
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            ClickableText(
                text = AnnotatedString("Forgot password?"),
                style = MaterialTheme.typography.bodyMedium.copy(color = GoGreen, fontWeight = FontWeight.Medium),
                onClick = {
                    resetEmail = email
                    showForgotPasswordField = true
                }
            )
        }

        if (showForgotPasswordField) {
            Column(modifier = Modifier.padding(bottom = 12.dp)) {
                if (passwordResetSent) {
                    Text(
                        text = "Reset email sent to $resetEmail. Check your inbox.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = GoGreen,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                } else {
                    LabeledTextField(
                        label = "Email for password reset",
                        value = resetEmail,
                        onValueChange = { resetEmail = it },
                        placeholder = "you@example.com"
                    )
                    Spacer(Modifier.height(8.dp))
                    PrimaryButton(
                        text = "Send reset email",
                        onClick = { onForgotPassword(resetEmail) },
                        isLoading = isSubmitting,
                        enabled = resetEmail.isNotBlank()
                    )
                }
            }
        }

        formError?.let {
            ErrorBanner(message = it, modifier = Modifier.padding(bottom = 12.dp))
        }

        PrimaryButton(
            text = "Sign in",
            onClick = { onSignIn(email, password) },
            isLoading = isSubmitting,
            enabled = email.isNotBlank() && password.isNotBlank()
        )

        Spacer(Modifier.height(120.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Text("New to GOSpot? ", style = MaterialTheme.typography.bodyMedium)
            ClickableText(
                text = AnnotatedString("Create an account"),
                style = MaterialTheme.typography.bodyMedium.copy(color = GoGreen, fontWeight = FontWeight.Bold),
                onClick = { onNavigateToCreateAccount() }
            )
        }
        Text(
            text = "Secured by Firebase Authentication",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )
    }
}
