package week11.st695922.finalproject.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import week11.st695922.finalproject.ui.components.ErrorBanner
import week11.st695922.finalproject.ui.components.LabeledTextField
import week11.st695922.finalproject.ui.components.PrimaryButton
import week11.st695922.finalproject.ui.theme.GoGreen

@Composable
fun CreateAccountScreen(
    formError: String?,
    isSubmitting: Boolean,
    onCreateAccount: (fullName: String, email: String, password: String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        IconButton(onClick = onNavigateBack, modifier = Modifier.padding(top = 16.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }

        Text(
            text = "Create your account",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            text = "Verified accounts keep the crowdsourced parking counts accurate.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
        )

        LabeledTextField(
            label = "Full name",
            value = fullName,
            onValueChange = { fullName = it },
            placeholder = "Ashish Garg"
        )
        Spacer(Modifier.height(16.dp))
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
            placeholder = "At least 8 characters",
            isPassword = true
        )
        Text(
            text = "Used only for Firebase Auth sign-in.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp, bottom = 16.dp)
        )

        formError?.let {
            ErrorBanner(message = it, modifier = Modifier.padding(bottom = 12.dp))
        }

        // No client-side length/format validation here on purpose: the taught
        // pattern (Week 5) is to call Firebase directly and surface whatever
        // error it returns (e.g. "weak password") through formError below.
        PrimaryButton(
            text = "Create account",
            onClick = { onCreateAccount(fullName, email, password) },
            isLoading = isSubmitting,
            enabled = fullName.isNotBlank() && email.isNotBlank() && password.isNotBlank()
        )

        Spacer(Modifier.height(120.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Text("Already registered? ", style = MaterialTheme.typography.bodyMedium)
            ClickableText(
                text = AnnotatedString("Sign in"),
                style = MaterialTheme.typography.bodyMedium.copy(color = GoGreen, fontWeight = FontWeight.Bold),
                onClick = { onNavigateBack() }
            )
        }
    }
}
