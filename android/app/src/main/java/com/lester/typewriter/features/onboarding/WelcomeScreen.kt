package com.lester.typewriter.features.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Ink = Color(0xFFEFE6D2) // cream, like the key legends
private val Muted = Color(0xFFA9A296)
private val Night = Color(0xFF111113)
private val Card = Color(0xFF1C1C20)
private val Ribbon = Color(0xFFC0182F) // typewriter-ribbon red
private val Done = Color(0xFF7BC47F)

@Composable
fun WelcomeScreen(
    status: KeyboardStatus,
    version: String,
    onTurnOn: () -> Unit,
    onPick: () -> Unit,
) {
    val colors = darkColorScheme(
        primary = Ribbon,
        onPrimary = Ink,
        background = Night,
        onBackground = Ink,
        surface = Card,
        onSurface = Ink,
    )
    MaterialTheme(colorScheme = colors) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Night)
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "TYPEWRITER",
                color = Ink,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 34.sp,
                letterSpacing = 6.sp,
            )
            Text("A keyboard that clacks.", color = Muted, fontFamily = FontFamily.Serif, fontSize = 16.sp)
            Step(
                number = "1",
                title = "Turn it on",
                done = status.enabled,
                doneText = "Typewriter is switched on",
                hint = "In Settings, switch on Typewriter and accept Android's warning. " +
                    "Typewriter has no internet access, so nothing you type can leave your phone.",
                action = "Open keyboard settings",
                actionEnabled = true,
                onClick = onTurnOn,
            )
            Step(
                number = "2",
                title = "Pick it",
                done = status.selected,
                doneText = "Typewriter is your keyboard",
                hint = "Choose Typewriter from the list of keyboards.",
                action = "Choose keyboard",
                actionEnabled = status.enabled,
                onClick = onPick,
            )
            TryIt()
            Text("Version $version", color = Muted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun Step(
    number: String,
    title: String,
    done: Boolean,
    doneText: String,
    hint: String,
    action: String,
    actionEnabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(color = Card, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (done) "✓" else number,
                    color = if (done) Done else Ribbon,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    modifier = Modifier.width(28.dp),
                )
                Text(title, color = Ink, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            if (done) {
                Text(doneText, color = Done)
            } else {
                Text(hint, color = Muted, fontSize = 14.sp)
                Button(onClick = onClick, enabled = actionEnabled) { Text(action) }
            }
        }
    }
}

/** Three boxes that each ask the keyboard for something different. */
@Composable
private fun TryIt() {
    var note by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var search by remember { mutableStateOf("") }
    var searched by remember { mutableStateOf("") }
    Surface(color = Card, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Try it", color = Ink, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(
                "Enter moves to the next box. In the last box it searches.",
                color = Muted,
                fontSize = 14.sp,
            )
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Type here") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                label = { Text("Search") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { searched = search }),
                modifier = Modifier.fillMaxWidth(),
            )
            if (searched.isNotEmpty()) Text("Searched for: $searched", color = Ink)
        }
    }
}
