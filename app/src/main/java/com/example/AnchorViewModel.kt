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
import android.speech.tts.TextToSpeech
import java.util.Locale

class AnchorViewModel(application: Application) : AndroidViewModel(application), TextToSpeech.OnInitListener {

    private val repository: TaskRepository
    
    private val sharedPrefs = application.getSharedPreferences("anchor_prefs", android.content.Context.MODE_PRIVATE)

    private val _themeColor = MutableStateFlow(sharedPrefs.getString("theme_color", "Cyan") ?: "Cyan")
    val themeColor: StateFlow<String> = _themeColor

    private val _layoutDensity = MutableStateFlow(sharedPrefs.getString("layout_density", "Spacious") ?: "Spacious")
    val layoutDensity: StateFlow<String> = _layoutDensity

    private val _cardStyle = MutableStateFlow(sharedPrefs.getString("card_style", "Filled") ?: "Filled")
    val cardStyle: StateFlow<String> = _cardStyle

    private val _aiModel: MutableStateFlow<String> = run {
        var model = sharedPrefs.getString("ai_model", "gemini-3.5-flash") ?: "gemini-3.5-flash"
        if (model.contains("1.5") || model == "gemini-1.5-flash" || model == "gemini-1.5-pro") {
            model = "gemini-3.5-flash"
            sharedPrefs.edit().putString("ai_model", model).apply()
        }
        MutableStateFlow(model)
    }
    val aiModel: StateFlow<String> = _aiModel

    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false

    fun setThemeColor(color: String) {
        sharedPrefs.edit().putString("theme_color", color).apply()
        _themeColor.value = color
    }

    fun setLayoutDensity(density: String) {
        sharedPrefs.edit().putString("layout_density", density).apply()
        _layoutDensity.value = density
    }

    fun setCardStyle(style: String) {
        sharedPrefs.edit().putString("card_style", style).apply()
        _cardStyle.value = style
    }

    fun setAiModel(model: String) {
        sharedPrefs.edit().putString("ai_model", model).apply()
        _aiModel.value = model
    }

    init {
        val taskDao = AppDatabase.getDatabase(application).taskDao()
        repository = TaskRepository(taskDao)
        tts = TextToSpeech(application, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
            isTtsInitialized = true
        }
    }

    override fun onCleared() {
        tts?.stop()
        tts?.shutdown()
        super.onCleared()
    }

    private fun speak(text: String) {
        if (isTtsInitialized) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    val pendingTasks: StateFlow<List<TaskEntity>> = repository.pendingTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentTasks: StateFlow<List<TaskEntity>> = repository.recentCompletedTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTasks: StateFlow<List<TaskEntity>> = repository.allCompletedTasks
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
                    val result = GeminiRetrofitClient.service.generateContent(_aiModel.value, apiKey, request)
                    result.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "No response from core."
                }
            } catch (e: retrofit2.HttpException) {
                val errorCode = e.code()
                val errorBody = try {
                    e.response()?.errorBody()?.string()
                } catch (ex: Exception) {
                    null
                }
                val serverMsg = if (!errorBody.isNullOrBlank()) {
                    "\nServer details: $errorBody"
                } else {
                    ""
                }
                
                if (errorCode == 429) {
                    "Error communicating with anchor core - HTTP 429 (Rate Limit Exceeded). Your Gemini API key has exceeded its rate limit or free quota. Please wait a minute, use a paid Gemini tier, or try switching back to Gemini 3.5 Flash inside Settings.$serverMsg"
                } else if (errorCode == 404) {
                    "Error communicating with anchor core - HTTP 404 (Model Not Found / Path Not Found). The selected model (${_aiModel.value}) might be unsupported under your API key's tier, or the API endpoint is unavailable. Please ensure you are using Gemini 3.5 Flash or Gemini 3.1 Pro.$serverMsg"
                } else if (errorCode == 400) {
                    "Error communicating with anchor core - HTTP 400 (Bad Request). Check if the selected model (${_aiModel.value}) is supported by your API key, or switch to Gemini 3.5 Flash in Settings.$serverMsg"
                } else {
                    "Error communicating with anchor core: HTTP $errorCode (${e.message()}).$serverMsg"
                }
            } catch (e: Exception) {
                "Error communicating with anchor core: ${e.localizedMessage ?: "Unknown connection error"}"
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
            
            _assistantMessage.value?.let { speak(it) }
            _isThinking.value = false
        }
    }

    fun approveTask(taskId: Int) {
        viewModelScope.launch {
            repository.approveTask(taskId)
            _assistantMessage.value = "Task approved and moved to automation log."
            speak("Task approved.")
        }
    }

    fun declineTask(taskId: Int) {
        viewModelScope.launch {
            repository.deleteTask(taskId)
            _assistantMessage.value = "Task declined and removed."
            speak("Task declined.")
        }
    }
}
