package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.GeminiVoiceService
import com.example.ai.TtsSpeaker
import com.example.data.ClientSettings
import com.example.voice.ClientStateHolder
import com.example.voice.ClientUiState
import com.example.voice.VoiceCommand
import kotlinx.coroutines.launch

private val GeminiPurple = Color(0xFFA855F7)
private val GeminiGradient = Brush.linearGradient(listOf(Color(0xFF8B5CF6), Color(0xFF06B6D4)))

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GeminiLiveVoiceAssistantSheet(
    state: ClientUiState,
    settings: ClientSettings,
    onDismiss: () -> Unit,
    onSendMusicCommand: (songQuery: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var textInput by remember { mutableStateOf("") }
    var selectedModel by remember { mutableStateOf(settings.geminiModelName) }
    var isTtsActive by remember { mutableStateOf(settings.geminiVoiceTtsEnabled) }
    var showApiKeyConfig by remember { mutableStateOf(!GeminiVoiceService.isConfigured(context)) }
    var customKeyInput by remember { mutableStateOf(settings.customGeminiApiKey) }
    var isTestingConnection by remember { mutableStateOf(false) }
    var connectionTestResult by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    val infiniteTransition = rememberInfiniteTransition(label = "gemini_glow")
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowScale"
    )

    LaunchedEffect(state.geminiChatMessages.size) {
        if (state.geminiChatMessages.isNotEmpty()) {
            listState.animateScrollToItem(state.geminiChatMessages.size - 1)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0D0E15),
        dragHandle = null,
        modifier = Modifier
            .fillMaxHeight(0.92f)
            .testTag("gemini_live_assistant_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .scale(glowScale)
                            .clip(CircleShape)
                            .background(GeminiGradient),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Gemini AI Live Voice",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0x33A855F7),
                                border = androidx.compose.foundation.BorderStroke(1.dp, GeminiPurple.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "3.5 FLASH",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GeminiPurple,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Conversación y control musical con IA",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = { showApiKeyConfig = !showApiKeyConfig },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Configurar API Key",
                            tint = if (GeminiVoiceService.isConfigured(context)) GeminiPurple else Color(0xFFEF4444),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            isTtsActive = !isTtsActive
                            settings.geminiVoiceTtsEnabled = isTtsActive
                            if (!isTtsActive) TtsSpeaker.stop()
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "TTS Voice",
                            tint = if (isTtsActive) GeminiPurple else Color(0xFF64748B),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Quick API Key Configuration / Diagnostics Banner
            AnimatedVisibility(visible = showApiKeyConfig) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x282E1065))
                        .border(1.dp, GeminiPurple.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Configuración de API Key Gemini",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        val isConfigured = GeminiVoiceService.isConfigured(context)
                        Text(
                            text = if (isConfigured) "● Activa" else "● Sin clave",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isConfigured) Color(0xFF4ADE80) else Color(0xFFF87171)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = customKeyInput,
                            onValueChange = {
                                customKeyInput = it
                                settings.customGeminiApiKey = it.trim()
                                connectionTestResult = null
                            },
                            placeholder = { Text("Pega tu API Key de AI Studio...", fontSize = 11.sp, color = Color(0xFF64748B)) },
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = Color.White),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GeminiPurple,
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedContainerColor = Color(0xFF13141F),
                                unfocusedContainerColor = Color(0xFF13141F)
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        Button(
                            onClick = {
                                isTestingConnection = true
                                connectionTestResult = null
                                scope.launch {
                                    val res = GeminiVoiceService.testConnection(
                                        explicitKey = customKeyInput,
                                        modelName = selectedModel,
                                        context = context
                                    )
                                    connectionTestResult = res
                                    isTestingConnection = false
                                }
                            },
                            enabled = !isTestingConnection,
                            colors = ButtonDefaults.buttonColors(containerColor = GeminiPurple),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            if (isTestingConnection) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Text("Probar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    connectionTestResult?.let { (success, msg) ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (success) Color(0x2222C55E) else Color(0x22EF4444),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (success) Color(0xFF22C55E) else Color(0xFFEF4444)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = msg,
                                fontSize = 10.sp,
                                color = if (success) Color(0xFF86EFAC) else Color(0xFFFCA5A5),
                                modifier = Modifier.padding(6.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Model Switcher Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    "gemini-3.5-flash" to "Flash 3.5",
                    "gemini-3.1-flash-lite-preview" to "Lite 3.1",
                    "gemini-3.1-pro-preview" to "Pro 3.1",
                    "gemini-flash-latest" to "Flash Latest"
                ).forEach { (modelKey, label) ->
                    val isSelected = selectedModel == modelKey
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) Color(0x33A855F7) else Color(0x221E1E2E),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) GeminiPurple else Color(0x33334155)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                selectedModel = modelKey
                                settings.geminiModelName = modelKey
                            }
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else Color(0xFF94A3B8),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(vertical = 5.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Chat & Suggestions Feed
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF13141F))
                    .border(1.dp, Color(0x22334155), RoundedCornerShape(16.dp))
                    .padding(12.dp)
            ) {
                if (state.geminiChatMessages.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(Color(0x22A855F7)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = GeminiPurple,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Habla o pregunta lo que quieras",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Recomendaciones, letras, artistas o pedidos directos",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Quick Prompt Suggestions
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                "🎵 Pon Queen - Bohemian Rhapsody",
                                "✨ Recomiéndame 3 temas de Synthwave",
                                "🎸 Pon rock en español de los 80s",
                                "🔉 Baja el volumen a la mitad",
                                "🎤 ¿Qué canciones famosas tiene Coldplay?"
                            ).forEach { prompt ->
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = Color(0x1EA855F7),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33A855F7)),
                                    modifier = Modifier.clickable {
                                        executeGeminiChat(
                                            userMsg = prompt,
                                            modelName = selectedModel,
                                            isTts = isTtsActive,
                                            scope = scope,
                                            context = context,
                                            onSendMusicCommand = onSendMusicCommand
                                        )
                                    }
                                ) {
                                    Text(
                                        text = prompt,
                                        fontSize = 11.sp,
                                        color = Color(0xFFE2E8F0),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(state.geminiChatMessages) { msg ->
                            val isUser = msg.role == "user"
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(
                                        topStart = 14.dp,
                                        topEnd = 14.dp,
                                        bottomStart = if (isUser) 14.dp else 2.dp,
                                        bottomEnd = if (isUser) 2.dp else 14.dp
                                    ),
                                    color = if (isUser) Color(0xFF3B82F6) else Color(0xFF1E1E2E),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isUser) Color(0x663B82F6) else Color(0x448B5CF6)
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = if (isUser) "Tú" else "Gemini AI",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isUser) Color(0xFFBFDBFE) else GeminiPurple
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = msg.text,
                                            fontSize = 13.sp,
                                            color = Color.White,
                                            lineHeight = 18.sp
                                        )
                                    }
                                }
                            }
                        }

                        if (state.isGeminiChatLoading) {
                            item {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(vertical = 4.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = GeminiPurple
                                    )
                                    Text(
                                        text = "Gemini está procesando en vivo...",
                                        fontSize = 11.sp,
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action & Input Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    placeholder = { Text("Escribe o pide una canción...", fontSize = 12.sp, color = Color(0xFF64748B)) },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("gemini_chat_input"),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF1A1B28),
                        unfocusedContainerColor = Color(0xFF141522),
                        focusedBorderColor = GeminiPurple,
                        unfocusedBorderColor = Color(0x33334155),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                IconButton(
                    onClick = {
                        val query = textInput.trim()
                        if (query.isNotBlank()) {
                            textInput = ""
                            executeGeminiChat(
                                userMsg = query,
                                modelName = selectedModel,
                                isTts = isTtsActive,
                                scope = scope,
                                context = context,
                                onSendMusicCommand = onSendMusicCommand
                            )
                        }
                    },
                    enabled = textInput.isNotBlank() && !state.isGeminiChatLoading,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (textInput.isNotBlank()) GeminiPurple else Color(0xFF1E293B))
                        .testTag("gemini_send_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Enviar",
                        tint = if (textInput.isNotBlank()) Color.White else Color(0xFF64748B),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

private fun executeGeminiChat(
    userMsg: String,
    modelName: String,
    isTts: Boolean,
    scope: kotlinx.coroutines.CoroutineScope,
    context: android.content.Context,
    onSendMusicCommand: (String) -> Unit
) {
    ClientStateHolder.addGeminiMessage("user", userMsg)
    ClientStateHolder.updateState { it.copy(isGeminiChatLoading = true) }

    scope.launch {
        val currentHistory = ClientStateHolder.state.value.geminiChatMessages
        val result = GeminiVoiceService.chatWithGemini(
            userMessage = userMsg,
            history = currentHistory,
            modelName = modelName,
            context = context
        )

        ClientStateHolder.updateState { it.copy(isGeminiChatLoading = false) }
        ClientStateHolder.addGeminiMessage("model", result.replyText)

        if (isTts && result.isSuccess && result.replyText.isNotBlank()) {
            TtsSpeaker.speak(result.replyText)
        }

        // If the chat recognized a specific song to play, dispatch it directly to Host!
        val suggestedCmd = result.suggestedCommand
        if (suggestedCmd is VoiceCommand.SearchAndPlay) {
            onSendMusicCommand(suggestedCmd.songQuery)
        }
    }
}
