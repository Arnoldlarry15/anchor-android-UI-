package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val viewModel: AnchorViewModel = viewModel()
      val themeColor by viewModel.themeColor.collectAsState()
      MyApplicationTheme(themeColor = themeColor) {
        AnchorAssistantScreen(viewModel)
      }
    }
  }
}

// Data class and enum are now removed as they are in Room Entity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnchorAssistantScreen(viewModel: AnchorViewModel = viewModel()) {
  var isOnline by remember { mutableStateOf(true) }
  var commandText by remember { mutableStateOf("") }
  var showSettings by remember { mutableStateOf(false) }
  var showFullHistory by remember { mutableStateOf(false) }
  
  val isThinking by viewModel.isThinking.collectAsState()
  val assistantMessage by viewModel.assistantMessage.collectAsState()

  val themeColor by viewModel.themeColor.collectAsState()
  val layoutDensity by viewModel.layoutDensity.collectAsState()
  val cardStyle by viewModel.cardStyle.collectAsState()
  val listSpacing = if (layoutDensity == "Compact") 8.dp else 16.dp

  val pendingTasks by viewModel.pendingTasks.collectAsState()
  val recentTasks by viewModel.recentTasks.collectAsState()
  val allTasks by viewModel.allTasks.collectAsState()

  val alphaAnim by animateFloatAsState(
    targetValue = if (isOnline) 1f else 0.4f,
    animationSpec = tween(500), label = "alpha"
  )

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    containerColor = MaterialTheme.colorScheme.background,
    bottomBar = {
      if (isOnline) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(24.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          TextField(
            value = commandText,
            onValueChange = { commandText = it },
            placeholder = { Text("Command anchor...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.6f)) },
            colors = TextFieldDefaults.colors(
              focusedContainerColor = Color.Transparent,
              unfocusedContainerColor = Color.Transparent,
              focusedIndicatorColor = Color.Transparent,
              unfocusedIndicatorColor = Color.Transparent,
              focusedTextColor = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier.weight(1f)
          )
          IconButton(
            onClick = {
              if (commandText.isNotBlank()) {
                viewModel.processCommand(commandText)
                commandText = ""
              }
            },
            enabled = !isThinking && commandText.isNotBlank()
          ) {
            Icon(Icons.Default.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.primary)
          }
        }
      }
    }
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(horizontal = 20.dp)
        .alpha(alphaAnim)
    ) {
      Spacer(modifier = Modifier.height(16.dp))

      // Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Row(verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.foundation.Image(
              painter = androidx.compose.ui.res.painterResource(id = R.drawable.anchor_logo),
              contentDescription = "anchor logo",
              modifier = Modifier.size(32.dp).clip(CircleShape)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
              text = "anchor",
              style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp,
                color = MaterialTheme.colorScheme.primary
              )
            )
          }
          Spacer(modifier = Modifier.height(4.dp))
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (isOnline) MaterialTheme.colorScheme.primary else Color.Red)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = if (isOnline) "ONLINE & LISTENING" else "OFFLINE",
              style = MaterialTheme.typography.labelSmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
              )
            )
          }
        }
        
        // Kill Switch Button
        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(
            onClick = { showSettings = true },
            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
          ) {
            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.primary)
          }
          Spacer(modifier = Modifier.width(8.dp))
          Button(
            onClick = { isOnline = !isOnline },
            colors = ButtonDefaults.buttonColors(
              containerColor = if (isOnline) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.surfaceVariant,
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.height(48.dp)
          ) {
            Icon(
              if (isOnline) Icons.Default.PowerSettingsNew else Icons.Default.PlayArrow,
              contentDescription = "Toggle anchor",
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (isOnline) "KILL" else "ACTIVATE", fontWeight = FontWeight.Bold)
          }
        }
      }

      Spacer(modifier = Modifier.height(32.dp))

      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp)
      ) {
        if (isThinking) {
          item {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), color = MaterialTheme.colorScheme.primary)
          }
        }
        
        if (assistantMessage != null) {
          item {
            Card(
              shape = RoundedCornerShape(16.dp),
              modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
              border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            ) {
              Text(
                text = assistantMessage!!,
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyLarge
              )
            }
          }
        }

        if (pendingTasks.isNotEmpty()) {
          item {
            Text(
              text = "AWAITING APPROVAL",
              style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
              )
            )
            Spacer(modifier = Modifier.height(16.dp))
          }
          
          items(pendingTasks) { task ->
            ApprovalCard(
              task = task,
              cardStyle = cardStyle,
              onApprove = { viewModel.approveTask(task.id) },
              onDecline = { viewModel.declineTask(task.id) }
            )
            Spacer(modifier = Modifier.height(listSpacing))
          }
          
          item { Spacer(modifier = Modifier.height(24.dp)) }
        }

        item {
          Text(
            text = "RECENT AUTOMATIONS",
            style = MaterialTheme.typography.labelMedium.copy(
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              letterSpacing = 1.sp
            )
          )
          Spacer(modifier = Modifier.height(16.dp))
        }

        items(recentTasks) { task ->
          LogCard(task, cardStyle)
          Spacer(modifier = Modifier.height(listSpacing))
        }

        item {
          TextButton(onClick = { showFullHistory = true }, modifier = Modifier.fillMaxWidth()) {
            Text("View All History Logs")
          }
        }
      }
    }
  }

  if (showSettings) {
    var aiModel by remember { mutableStateOf(viewModel.aiModel.value) }

    ModalBottomSheet(
      onDismissRequest = { showSettings = false },
      containerColor = MaterialTheme.colorScheme.surface
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 24.dp)
          .padding(bottom = 40.dp)
      ) {
        Text("DASHBOARD SETUP", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(24.dp))
        
        Text("AI Model", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
          val models = listOf("gemini-1.5-flash", "gemini-3.1-pro-preview")
          val labels = listOf("1.5 Flash (Fast)", "3.1 Pro (Advanced)")
          models.forEachIndexed { index, modelName ->
            OutlinedButton(
              onClick = { 
                viewModel.setAiModel(modelName)
                aiModel = modelName
              },
              colors = ButtonDefaults.outlinedButtonColors(
                containerColor = if (aiModel == modelName) MaterialTheme.colorScheme.primary.copy(alpha=0.1f) else Color.Transparent,
                contentColor = if (aiModel == modelName) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
              ),
              border = androidx.compose.foundation.BorderStroke(1.dp, if (aiModel == modelName) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.5f))
            ) {
              Text(labels[index])
            }
          }
        }
        
        Spacer(modifier = Modifier.height(32.dp))

        Text("Theme Color", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
          val colors = listOf("Cyan", "Violet", "Emerald", "Crimson", "Amber")
          colors.forEach { colorName ->
            val colorVal = when (colorName) {
              "Cyan" -> com.example.ui.theme.PrimaryCyan
              "Violet" -> com.example.ui.theme.PrimaryViolet
              "Emerald" -> com.example.ui.theme.PrimaryEmerald
              "Crimson" -> com.example.ui.theme.PrimaryCrimson
              "Amber" -> com.example.ui.theme.PrimaryAmber
              else -> com.example.ui.theme.PrimaryCyan
            }
            Box(
              modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(colorVal)
                .border(2.dp, if (themeColor == colorName) MaterialTheme.colorScheme.onSurface else Color.Transparent, CircleShape)
                .clickable { viewModel.setThemeColor(colorName) }
            )
          }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text("Layout Density", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
          val densities = listOf("Compact", "Spacious")
          densities.forEach { density ->
            OutlinedButton(
              onClick = { viewModel.setLayoutDensity(density) },
              colors = ButtonDefaults.outlinedButtonColors(
                containerColor = if (layoutDensity == density) MaterialTheme.colorScheme.primary.copy(alpha=0.1f) else Color.Transparent,
                contentColor = if (layoutDensity == density) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
              ),
              border = androidx.compose.foundation.BorderStroke(1.dp, if (layoutDensity == density) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.5f))
            ) {
              Text(density)
            }
          }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text("Card Style", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
          val styles = listOf("Filled", "Outlined")
          styles.forEach { style ->
            OutlinedButton(
              onClick = { viewModel.setCardStyle(style) },
              colors = ButtonDefaults.outlinedButtonColors(
                containerColor = if (cardStyle == style) MaterialTheme.colorScheme.primary.copy(alpha=0.1f) else Color.Transparent,
                contentColor = if (cardStyle == style) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
              ),
              border = androidx.compose.foundation.BorderStroke(1.dp, if (cardStyle == style) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.5f))
            ) {
              Text(style)
            }
          }
        }
      }
    }
  }

  if (showFullHistory) {
    ModalBottomSheet(
      onDismissRequest = { showFullHistory = false },
      containerColor = MaterialTheme.colorScheme.surface
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 24.dp)
          .padding(bottom = 24.dp)
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
          Spacer(modifier = Modifier.width(12.dp))
          Text("INFINITE TASK HISTORY", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(modifier = Modifier.height(24.dp))
        
        LazyColumn(
          modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
          contentPadding = PaddingValues(bottom = 24.dp)
        ) {
          items(allTasks) { task ->
            LogCard(task, cardStyle)
            Spacer(modifier = Modifier.height(listSpacing))
          }
          if (allTasks.isEmpty()) {
            item {
              Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                Text("No automation logs found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun ApprovalCard(task: TaskEntity, cardStyle: String, onApprove: () -> Unit, onDecline: () -> Unit) {
  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = if (cardStyle == "Filled") MaterialTheme.colorScheme.surface else Color.Transparent
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    modifier = Modifier
      .fillMaxWidth()
      .border(
        1.dp, 
        if (cardStyle == "Outlined") MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), 
        RoundedCornerShape(16.dp)
      )
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = task.category.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
              color = MaterialTheme.colorScheme.primary,
              letterSpacing = 1.sp
            )
          )
        }
      }
      
      Spacer(modifier = Modifier.height(12.dp))
      
      Text(
        text = task.title,
        style = MaterialTheme.typography.bodyLarge.copy(
          fontWeight = FontWeight.Medium,
          color = MaterialTheme.colorScheme.onSurface
        )
      )

      Spacer(modifier = Modifier.height(20.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        OutlinedButton(
          onClick = onDecline,
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
          border = null,
          modifier = Modifier
            .weight(1f)
            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
        ) {
          Text("Decline")
        }
        Button(
          onClick = onApprove,
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
          ),
          modifier = Modifier.weight(1f)
        ) {
          Text("Approve")
        }
      }
    }
  }
}

@Composable
fun LogCard(task: TaskEntity, cardStyle: String) {
  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = if (cardStyle == "Filled") MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f) else Color.Transparent
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    modifier = Modifier
      .fillMaxWidth()
      .border(
        if (cardStyle == "Outlined") 1.dp else 0.dp, 
        if (cardStyle == "Outlined") MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f) else Color.Transparent, 
        RoundedCornerShape(16.dp)
      )
  ) {
    Row(
      modifier = Modifier.padding(16.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(40.dp)
          .clip(CircleShape)
          .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          Icons.Default.CheckCircleOutline,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(20.dp)
        )
      }
      
      Spacer(modifier = Modifier.width(16.dp))
      
      Column {
        Text(
          text = task.category.uppercase(),
          style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = task.title,
          style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface)
        )
      }
    }
  }
}
