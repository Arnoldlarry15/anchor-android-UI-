package com.example

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.util.Log

class AnchorViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TaskRepository

    init {
        val taskDao = AppDatabase.getDatabase(application).taskDao()
        repository = TaskRepository(taskDao)
        
        // Add some initial data if empty
        viewModelScope.launch {
            // In a real app we might check if empty first, but let's just let it be empty until the user adds something.
        }
    }

    val pendingTasks: StateFlow<List<TaskEntity>> = repository.pendingTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentTasks: StateFlow<List<TaskEntity>> = repository.completedTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isThinking = MutableStateFlow(false)
    val isThinking: StateFlow<Boolean> = _isThinking

    private val _assistantMessage = MutableStateFlow<String?>(null)
    val assistantMessage: StateFlow<String?> = _assistantMessage

    fun processCommand(command: String) {
        viewModelScope.launch {
            _isThinking.value = true
            _assistantMessage.value = null
            
            val response = try {
                withContext(Dispatchers.IO) {
                    val apiKey = BuildConfig.GEMINI_API_KEY
                    val request = GenerateContentRequest(
                        contents = listOf(
                            Content(
                                parts = listOf(Part(text = "The user says: \"$command\"\nAnalyze the request. If the user is asking you to perform a task or automation, extract the task details and output a strictly formatted JSON response like this: {\n\"action\": \"CREATE_TASK\",\n\"title\": \"<short task description>\",\n\"category\": \"<category name, e.g., Financial, Inbox, Operations>\"\n}\nIf it is just a question or conversation, reply normally with strings."))
                            )
                        ),
                        systemInstruction = Content(
                            parts = listOf(Part(text = "You are anchor, an advanced, highly intelligent, and sleek personal assistant. Keep your answers extremely concise, confident, and professional. Act as if you can execute any automated task."))
                        )
                    )
                    val result = GeminiRetrofitClient.service.generateContent(apiKey, request)
                    result.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "No response from core."
                }
            } catch (e: Exception) {
                "Error communicating with anchor core: ${e.localizedMessage}"
            }
            
            // Try to parse JSON from the response to see if we should create a task
            try {
                if (response.contains("\"action\":") && response.contains("\"CREATE_TASK\"")) {
                    val titleMatch = "\"title\":\\s*\"(.*?)\"".toRegex().find(response)
                    val categoryMatch = "\"category\":\\s*\"(.*?)\"".toRegex().find(response)
                    if (titleMatch != null && categoryMatch != null) {
                        val title = titleMatch.groupValues[1]
                        val category = categoryMatch.groupValues[1]
                        repository.insert(TaskEntity(title = title, category = category, isPendingApproval = true))
                        _assistantMessage.value = "Task queued for your approval."
                    } else {
                        _assistantMessage.value = response
                    }
                } else {
                    _assistantMessage.value = response
                }
            } catch (e: Exception) {
                _assistantMessage.value = response
            }
            
            _isThinking.value = false
        }
    }

    fun approveTask(taskId: Int) {
        viewModelScope.launch {
            repository.approveTask(taskId)
            _assistantMessage.value = "Task approved and moved to automation log."
        }
    }

    fun declineTask(taskId: Int) {
        viewModelScope.launch {
            repository.deleteTask(taskId)
            _assistantMessage.value = "Task declined and removed."
        }
    }
}
