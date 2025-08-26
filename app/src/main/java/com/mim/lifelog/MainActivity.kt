package com.mim.lifelog

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState // Added for scrolling
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll // Added for scrolling
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Settings // Added for Settings icon
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog // Added for Dialog
import coil.compose.rememberAsyncImagePainter
import com.google.gson.Gson
import com.mim.lifelog.ImageUtil.ImageUtils.resizeImage
import com.mim.lifelog.ui.theme.LifeLogTheme
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.time.LocalDateTime


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Check if profile data is complete
            if (checkProfileCompletion(this)) {
                // Profile is complete; show the main app content
                LifeLogTheme {
                    LifeLogApp() // Main app content
                }
            } else {
                // Profile is incomplete; show the ProfileSetupScreen
                LifeLogTheme {
                    ProfileSetupScreen(
                        onProfileCompleted = { profileData ->
                            // Save profile data and switch to the main content
                            saveProfile(this, profileData)
                            setContent {
                                LifeLogApp() // Reload with main content after profile completion
                            }
                        }
                    )
                }
            }
        }
    }

    private var selectedLanguage by mutableStateOf("English")

    @Composable
    fun MainScreen() {
        val context = LocalContext.current
        val isProfileComplete = checkProfileCompletion(context)

        if (!isProfileComplete) {
            ProfileSetupScreen { saveProfile(context, it) }
        } else {
            LifeLogApp() // Your existing main content
        }
    }

    @Composable
    fun ProfileSetupScreen(onProfileCompleted: (ProfileData) -> Unit) {
        var name by remember { mutableStateOf("") }
        var age by remember { mutableStateOf("") }
        var sex by remember { mutableStateOf("") }
        var work by remember { mutableStateOf("") }
        var hobby by remember { mutableStateOf("") }
        var selectedLanguage by remember { mutableStateOf("English") }
        var notionToken by remember { mutableStateOf("") } // Or mutableStateOf<String?>(null)
        var notionDatabaseId by remember { mutableStateOf("") } // Or mutableStateOf<String?>(null)
        var chatGPTApiKey by remember { mutableStateOf("") } // Or mutableStateOf<String?>(null)
        var geminiApiKey by remember { mutableStateOf("") } // Or mutableStateOf<String?>(null)
        var expanded by remember { mutableStateOf(false) }
        val languages = listOf("English", "Russian", "Ukrainian", "Hungarian", "Italian", "Spanish")

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Set Up Your Profile",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Input Fields
            ProfileInputField("Name", name) { name = it }
            ProfileInputField("Age", age) { age = it }
            ProfileInputField("Sex", sex) { sex = it }
            ProfileInputField("Work", work) { work = it }
            ProfileInputField("Hobby", hobby) { hobby = it }
            ProfileInputField("Notion Token (Optional)", notionToken) { notionToken = it }
            ProfileInputField("Notion Database ID (Optional)", notionDatabaseId) { notionDatabaseId = it }
            ProfileInputField("ChatGPT API Key (Optional)", chatGPTApiKey) { chatGPTApiKey = it }
            ProfileInputField("Gemini API Key (Optional)", geminiApiKey) { geminiApiKey = it }

            // Language Dropdown
            OutlinedTextField(
                value = selectedLanguage,
                onValueChange = {},
                readOnly = true,
                label = { Text("Language") },
                trailingIcon = {
                    IconButton(onClick = { expanded = true }) {
                        Icon(Icons.Filled.ArrowDropDown, contentDescription = "Select Language")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth()
            ) {
                languages.forEach { language ->
                    DropdownMenuItem(
                        text = { Text(language) },
                        onClick = {
                            selectedLanguage = language
                            expanded = false
                        }
                    )
                }
            }

            // Save Button
            Button(
                onClick = {
                    val profileData = ProfileData(
                        name,
                        age,
                        sex,
                        work,
                        hobby,
                        selectedLanguage,
                        notionToken.ifBlank { null },
                        notionDatabaseId.ifBlank { null },
                        chatGPTApiKey.ifBlank { null },
                        geminiApiKey.ifBlank { null }
                    )
                    onProfileCompleted(profileData)
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.primary)
            ) {
                Text("Save Profile", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }

    @Composable
    fun ProfileInputField(label: String, value: String, onValueChange: (String) -> Unit) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label, style = MaterialTheme.typography.labelMedium) },
            modifier = Modifier.fillMaxWidth()
        )
    }


    @Composable
    fun ProfileEditScreen(onProfileSave: (ProfileData) -> Unit, onDismiss: () -> Unit) {
        val context = LocalContext.current
        val prefs = context.getSharedPreferences("profile_data", Context.MODE_PRIVATE)

        // Retrieve stored values or set defaults
        var name by remember { mutableStateOf(prefs.getString("name", "") ?: "") }
        var age by remember { mutableStateOf(prefs.getString("age", "") ?: "") }
        var sex by remember { mutableStateOf(prefs.getString("sex", "") ?: "") }
        var work by remember { mutableStateOf(prefs.getString("work", "") ?: "") }
        var hobby by remember { mutableStateOf(prefs.getString("hobby", "") ?: "") }
        var selectedLanguage by remember { mutableStateOf(prefs.getString("language", "English") ?: "English") }
        var notionToken by remember { mutableStateOf(prefs.getString("notionToken", "") ?: "") }
        var notionDatabaseId by remember { mutableStateOf(prefs.getString("notionDatabaseId", "") ?: "") }
        var chatGPTApiKey by remember { mutableStateOf(prefs.getString("chatGPTApiKey", "") ?: "") }
        var geminiApiKey by remember { mutableStateOf(prefs.getString("geminiApiKey", "") ?: "") }
        var expanded by remember { mutableStateOf(false) }
        val languages = listOf("English", "Russian", "Ukrainian", "Hungarian", "Italian", "Spanish")

        Column(
            modifier = Modifier
                // Using fillMaxWidth instead of fillMaxSize for dialog content
                // and relying on content to define height, or a specific height if needed.
                // However, for a scrollable list of fields, fillMaxSize might be okay if the Dialog itself has constraints.
                // For now, let's assume the Dialog provides reasonable constraints.
                // If the Dialog is full-screen, then fillMaxSize is fine.
                // If the Dialog is wrap_content, then fillMaxWidth and potentially a height modifier or relying on content is better.
                // Given it's a list of profile fields, making it scrollable implies content might exceed typical dialog height.
                // Changed to fillMaxWidth to better suit dialog behavior.
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()), // Added scrolling
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Fields for Name, Age, Sex, Work, Hobby
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
            OutlinedTextField(value = age, onValueChange = { age = it }, label = { Text("Age") })
            OutlinedTextField(value = sex, onValueChange = { sex = it }, label = { Text("Sex") })
            OutlinedTextField(value = work, onValueChange = { work = it }, label = { Text("Work") })
            OutlinedTextField(value = hobby, onValueChange = { hobby = it }, label = { Text("Hobby") })
            ProfileInputField("Notion Token (Optional)", notionToken) { notionToken = it }
            ProfileInputField("Notion Database ID (Optional)", notionDatabaseId) { notionDatabaseId = it }
            ProfileInputField("ChatGPT API Key (Optional)", chatGPTApiKey) { chatGPTApiKey = it }
            ProfileInputField("Gemini API Key (Optional)", geminiApiKey) { geminiApiKey = it }

            // Dropdown for Language
            OutlinedTextField(
                value = selectedLanguage,
                onValueChange = {},
                readOnly = true,
                label = { Text("Language") },
                trailingIcon = {
                    IconButton(onClick = { expanded = true }) {
                        Icon(Icons.Filled.ArrowDropDown, contentDescription = "Select Language")
                    }
                }
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth()
            ) {
                languages.forEach { language ->
                    DropdownMenuItem(
                        text = { Text(language) },
                        onClick = {
                            selectedLanguage = language
                            expanded = false
                        }
                    )
                }
            }

            Button(
                onClick = {
                    val profileData = ProfileData(
                        name,
                        age,
                        sex,
                        work,
                        hobby,
                        selectedLanguage,
                        notionToken.ifBlank { null },
                        notionDatabaseId.ifBlank { null },
                        chatGPTApiKey.ifBlank { null },
                        geminiApiKey.ifBlank { null }
                    )
                    onProfileSave(profileData) // Save updated profile data
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Save Changes", style = MaterialTheme.typography.bodyLarge)
            }

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.secondary)
            }
        }
    }

    // Function to check profile completion
    fun checkProfileCompletion(context: Context): Boolean {
        val prefs = context.getSharedPreferences("profile_data", Context.MODE_PRIVATE)
        val requiredKeys = listOf("name", "age", "sex", "work", "hobby", "language")
        Log.d("MyApp", "Profile has been found and loaded $prefs")
        // Check if any required key is missing
        return requiredKeys.all { prefs.contains(it) }
    }

    // Function to save profile data
    fun saveProfile(context: Context, profileData: ProfileData) {
        val prefs = context.getSharedPreferences("profile_data", Context.MODE_PRIVATE)
        val editor = prefs.edit()

        // Save each profile data field
        editor.putString("name", profileData.name)
        editor.putString("age", profileData.age)
        editor.putString("sex", profileData.sex)
        editor.putString("work", profileData.work)
        editor.putString("hobby", profileData.hobby)
        editor.putString("language", profileData.language)
        editor.putString("notionToken", profileData.notionToken)
        editor.putString("notionDatabaseId", profileData.notionDatabaseId)
        editor.putString("chatGPTApiKey", profileData.chatGPTApiKey)
        editor.putString("geminiApiKey", profileData.geminiApiKey)

        // Apply changes
        editor.apply()
    }

    data class ProfileData(
        val name: String,
        val age: String,
        val sex: String,
        val work: String,
        val hobby: String,
        val language: String,
        val notionToken: String?,
        val notionDatabaseId: String?,
        val chatGPTApiKey: String?,
        val geminiApiKey: String?
    )

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun LifeLogApp() {
        var comment by remember { mutableStateOf(TextFieldValue("")) }
        var imageUri by rememberSaveable { mutableStateOf<Uri?>(null) }
        var chatGPTResponse by rememberSaveable { mutableStateOf<String?>(null) }
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope() // Added CoroutineScope

        var expanded by remember { mutableStateOf(false) } // For language dropdown
        val languages = listOf("English", "Russian", "Ukrainian", "Hungarian", "Italian", "Spanish")

        // New state variables for LLM selection and Gemini
        var selectedLLM by rememberSaveable { mutableStateOf("ChatGPT") }
        val llmOptions = listOf("ChatGPT", "Gemini")
        var isModelSelectionExpanded by remember { mutableStateOf(false) }
        val geminiChatHistory = remember { mutableStateListOf<Pair<String, String>>() }
        var geminiResponse by rememberSaveable { mutableStateOf<String?>(null) }
        var showProfileEditScreen by rememberSaveable { mutableStateOf(false) }

        // Launcher for selecting a photo from gallery
        val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                imageUri = uri
            } else {
                Toast.makeText(context, "No image selected", Toast.LENGTH_SHORT).show()
            }
        }

        // Launcher for taking a photo with the camera
        var cameraImageUri by remember { mutableStateOf<Uri?>(null) }
        val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                imageUri = cameraImageUri
            } else {
                Toast.makeText(context, "Failed to capture image", Toast.LENGTH_SHORT).show()
            }
        }
        val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                cameraImageUri?.let { cameraLauncher.launch(it) }
            } else {
                Toast.makeText(context, "Camera permission denied", Toast.LENGTH_SHORT).show()
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("LifeLog App", style = MaterialTheme.typography.titleLarge) },
                    colors = TopAppBarDefaults.mediumTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    actions = {
                        IconButton(onClick = { showProfileEditScreen = true }) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = "Edit Profile",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                )
            },
            content = { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Grouping Selectors in a Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(2.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), Arrangement.spacedBy(10.dp)) {
                            // Language Selector Dropdown UI
                            OutlinedTextField(
                                value = selectedLanguage,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Select Language", style = MaterialTheme.typography.labelMedium) },
                                trailingIcon = {
                                    IconButton(onClick = { expanded = true }) {
                                        Icon(Icons.Filled.ArrowDropDown, contentDescription = "Dropdown")
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                            DropdownMenu(
                                expanded = expanded, // Language dropdown expanded state
                                onDismissRequest = { expanded = false },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                languages.forEach { language ->
                                    DropdownMenuItem(
                                        text = { Text(language, style = MaterialTheme.typography.bodyMedium) },
                                        onClick = {
                                            selectedLanguage = language
                                            expanded = false
                                            Toast.makeText(context, "Language selected: $language", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            }

                            // LLM Selector Dropdown UI
                            OutlinedTextField(
                                value = selectedLLM,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Select LLM", style = MaterialTheme.typography.labelMedium) },
                                trailingIcon = {
                                    IconButton(onClick = { isModelSelectionExpanded = true }) {
                                        Icon(Icons.Filled.ArrowDropDown, contentDescription = "Dropdown")
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                            DropdownMenu(
                                expanded = isModelSelectionExpanded,
                                onDismissRequest = { isModelSelectionExpanded = false },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                llmOptions.forEach { llm ->
                                    DropdownMenuItem(
                                        text = { Text(llm, style = MaterialTheme.typography.bodyMedium) },
                                        onClick = {
                                            selectedLLM = llm
                                            isModelSelectionExpanded = false
                                            Toast.makeText(context, "LLM selected: $llm", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Button to Capture Image using Camera
                    val interactionSourceCaptureImage = remember { MutableInteractionSource() }
                    val isPressedCaptureImage by interactionSourceCaptureImage.collectIsPressedAsState()
                    val scaleCaptureImage by animateFloatAsState(if (isPressedCaptureImage) 0.95f else 1f, label = "CaptureImageScale")

                    Button(
                        onClick = {
                            val photoFile = File.createTempFile("camera_", ".jpg", context.cacheDir)
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
                            cameraImageUri = uri
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                cameraLauncher.launch(uri)
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                scaleX = scaleCaptureImage
                                scaleY = scaleCaptureImage
                            },
                        interactionSource = interactionSourceCaptureImage
                    ) {
                        Text("Capture Image", style = MaterialTheme.typography.bodyLarge)
                    }

                    // Button to Select Image from Gallery
                    val interactionSourceSelectImage = remember { MutableInteractionSource() }
                    val isPressedSelectImage by interactionSourceSelectImage.collectIsPressedAsState()
                    val scaleSelectImage by animateFloatAsState(if (isPressedSelectImage) 0.95f else 1f, label = "SelectImageScale")

                    Button(
                        onClick = { galleryLauncher.launch("image/*") },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                scaleX = scaleSelectImage
                                scaleY = scaleSelectImage
                            },
                        interactionSource = interactionSourceSelectImage
                    ) {
                        Text("Select Image from Gallery", style = MaterialTheme.typography.bodyLarge)
                    }

                    // Display Selected Image
                    AnimatedVisibility(visible = imageUri != null) {
                        imageUri?.let {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                elevation = CardDefaults.cardElevation(4.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                            ) {
                                Image(
                                    painter = rememberAsyncImagePainter(it),
                                    contentDescription = "Selected Image",
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }

                    // Comment Input Field
                    OutlinedTextField(
                        value = comment,
                        onValueChange = { comment = it },
                        label = { Text("Add a comment...", style = MaterialTheme.typography.labelMedium) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Action Buttons
                    val interactionSourceGenerate = remember { MutableInteractionSource() }
                    val isPressedGenerate by interactionSourceGenerate.collectIsPressedAsState()
                    val scaleGenerate by animateFloatAsState(if (isPressedGenerate) 0.95f else 1f, label = "GenerateScale")

                    Button(
                        onClick = {
                            Toast.makeText(context, "Processing your entry...", Toast.LENGTH_SHORT).show()
                            if (selectedLLM == "ChatGPT") {
                                if (imageUri != null) {
                                    chatGPTResponse = sendToChatGPTAndNotion(context, imageUri!!, comment.text)
                                } else {
                                    Toast.makeText(context, "Please select an image for ChatGPT.", Toast.LENGTH_SHORT).show()
                                }
                            } else { // Gemini
                                coroutineScope.launch {
                                    val currentGeminiResponse = sendToGeminiAPI(context, imageUri, comment.text, geminiChatHistory)
                                    geminiResponse = currentGeminiResponse

                                    if (currentGeminiResponse != null) {
                                        try {
                                            val cleanJsonResponse = extractJson(currentGeminiResponse)
                                            val gson = Gson()
                                            val parsedResponse = gson.fromJson(cleanJsonResponse, ChatResponse::class.java)
                                            val notionSuccess = sendToNotionAPI(
                                                context,
                                                currentGeminiResponse, // This is the full JSON string from Gemini
                                                imageUrl = imageUri?.toString() ?: "no_image_provided",
                                                tags = parsedResponse.tags,
                                                mood = parsedResponse.mood.toString()
                                            )
                                            if (!notionSuccess) {
                                                withContext(Dispatchers.Main) {
                                                    Toast.makeText(context, "Failed to send Gemini data to Notion.", Toast.LENGTH_LONG).show()
                                                }
                                            }
                                        } catch (e: Exception) {
                                            Log.e("Lifelog", "Error parsing Gemini response or sending to Notion: ${e.message}")
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(context, "Error processing Gemini response.", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    } else {
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "Gemini call failed.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        enabled = imageUri != null || selectedLLM == "Gemini",
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                scaleX = scaleGenerate
                                scaleY = scaleGenerate
                            },
                        interactionSource = interactionSourceGenerate
                    ) {
                        Text("Generate Description", style = MaterialTheme.typography.bodyLarge)
                    }

                    // ChatGPT Response Dialog
                    AnimatedVisibility(
                        visible = chatGPTResponse != null,
                        enter = slideInVertically { initialOffsetY -> initialOffsetY / 2 } + fadeIn(),
                        exit = slideOutVertically { targetOffsetY -> targetOffsetY / 2 } + fadeOut()
                    ) {
                        chatGPTResponse?.let {
                            AlertDialog(
                                onDismissRequest = { chatGPTResponse = null },
                                confirmButton = {
                                    TextButton(onClick = { chatGPTResponse = null }) {
                                        Text("OK", style = MaterialTheme.typography.labelLarge)
                                    }
                                },
                                title = { Text("Generated Description (ChatGPT)", style = MaterialTheme.typography.titleMedium) },
                                text = { Text(it, style = MaterialTheme.typography.bodyMedium) }
                            )
                        }
                    }

                    // Gemini Response Dialog
                    AnimatedVisibility(
                        visible = geminiResponse != null,
                        enter = slideInVertically { initialOffsetY -> initialOffsetY / 2 } + fadeIn(),
                        exit = slideOutVertically { targetOffsetY -> targetOffsetY / 2 } + fadeOut()
                    ) {
                        geminiResponse?.let {
                            AlertDialog(
                                onDismissRequest = { geminiResponse = null },
                                confirmButton = {
                                    TextButton(onClick = { geminiResponse = null }) {
                                        Text("OK", style = MaterialTheme.typography.labelLarge)
                                    }
                                },
                                title = { Text("Generated Description (Gemini)", style = MaterialTheme.typography.titleMedium) },
                                text = { Text(it, style = MaterialTheme.typography.bodyMedium) }
                            )
                        }
                    }
                } // End of Main Column
            } // End of Scaffold content
        ) // End of Scaffold

        AnimatedVisibility(
            visible = showProfileEditScreen,
            enter = slideInVertically { initialOffsetY -> initialOffsetY / 2 } + fadeIn(),
            exit = slideOutVertically { targetOffsetY -> targetOffsetY / 2 } + fadeOut()
        ) {
            Dialog(onDismissRequest = { showProfileEditScreen = false }) {
                // Card to provide a background and shape for the dialog content
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.padding(16.dp) // Optional: if you want padding around the card itself
                ) {
                    ProfileEditScreen(
                        onProfileSave = { profileData ->
                            saveProfile(context, profileData)
                            showProfileEditScreen = false // Dismiss after saving
                        },
                        onDismiss = {
                            showProfileEditScreen = false // Dismiss from explicit cancel
                        }
                    )
                }
            }
        }
    }


    private val client = OkHttpClient()

    fun sendToChatGPTAndNotion(context: Context, imageUri: Uri, comment: String): String? {
        return runBlocking {
            withContext(Dispatchers.IO) {
                val prefs = context.getSharedPreferences("profile_data", Context.MODE_PRIVATE)
                var language = prefs.getString("language", "English") ?: "English"
                if (language.compareTo(selectedLanguage) != 0) {
                    language = selectedLanguage
                }

                try {
                    // Resize and encode the image
                    val resizedImageBytes = resizeImage(context, imageUri, 400, 400)
                    val base64Image = Base64.encodeToString(resizedImageBytes, Base64.NO_WRAP)
                    var myTags = mutableListOf("mood, day")
                    // Generate description with ChatGPT
                    val chatGPTResponse = sendToChatGPT(context, imageUri, comment )
                    Log.d("MyApp", "ChatGPT response: $chatGPTResponse")
                    chatGPTResponse?.let { description ->
                        // Send data to Notion
                        Log.d("Lifelog", "Preparing the request for Notion: $description")
                        val notionResponse = sendToNotionAPI(context, description, "base64Image", myTags, "2")
                        if (notionResponse) {
                            return@withContext description
                        } else {
                            throw Exception("Failed to send data to Notion")
                        }
                    }

                    return@withContext null

                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.e("Lifelog", e.message.toString())
                    null
                }
            }
        }
    }

    // ChatGPT integration part
    fun sendToChatGPT(context: Context, imageUri: Uri, comment: String): String? {
        return runBlocking {
            withContext(Dispatchers.IO) {
                val prefs = context.getSharedPreferences("profile_data", Context.MODE_PRIVATE)
                var language = prefs.getString("language", "English") ?: "English"
                if (language.compareTo(selectedLanguage) != 0){
                    language = selectedLanguage
                }

                val systemInstructions = "You are an application for creating a life log for a person. " +
                        "Based on the photo and comment you must create a rich description of what person has experienced or was doing, " +
                        "as if it is a diary page of the person. " +
                        "Every response you should start with time stamp, in following format: Year-Month-day Hour:minute. " +
                        "Based on this description the user should be able to understand what happened that moment. " +
                        "Try to describe as narrator, from first person. Try to identify small details in the photo and embed into a story." +
                        "Respond strictly in "+ language + " language. User profile is: Name - " + prefs.getString("name", "Unknown") +", " +
                        "sex - " + prefs.getString("sex", "Unknown") +", age" + prefs.getString("age", "Unknown") +", " +
                        "work - " + prefs.getString("work", "Unknown") +", hobby - " + prefs.getString("hobby", "Unknown") +"." +
                        "Try to evaluate a mood of the person based on the description and photo." +
                        "Send in a JSON format, with 'date' (following format '2024-11-30T13:59:00.000Z'), 'text', 'mood' (from 1 to 5), 'tags' (try to pick appropriate tags, as array of strings), " +
                        "'title' and create a short title for a diary record"

                try {
                    val chatGPTApiKey = prefs.getString("chatGPTApiKey", null)
                    if (chatGPTApiKey.isNullOrEmpty()) {
                        Log.e("Lifelog", "ChatGPT API Key not found in SharedPreferences or is empty.")
                        return@withContext null
                    }

                    // Encode image to base64
                    // val imageBytes = context.contentResolver.openInputStream(imageUri)?.readBytes() ?: return@withContext null
                    val resizedImageBytes = resizeImage(context, imageUri, 400, 400) // Adjust dimensions as needed
                    val base64Image = Base64.encodeToString(resizedImageBytes, Base64.NO_WRAP)

                    // Prepare the JSON body with separated text and image fields
                    val base64ImageAndText = "data:image/jpeg;base64,$base64Image" // Add your base64 encoded image here

                    // Create JSON request body in the new format
                    val requestBody = JSONObject().apply {
                        put("model", "gpt-4o-mini") // Ensure model name is valid for your API access
                        put("messages", JSONArray().apply {
                            put(JSONObject().apply {
                                put("role", "system")
                                put("content", systemInstructions)
                            })

                            put(JSONObject().apply {
                                put("role", "user")
                                put("content", JSONArray().apply {
                                    // First part of content: Text
                                    put(JSONObject().apply {
                                        put("type", "text")
                                        put("text", "Current time: "+ LocalDateTime.now() + ". What is in this image? Info from sender: " + comment)
                                    })
                                    // Second part of content: Image URL (using data URI scheme)
                                    put(JSONObject().apply {
                                        put("type", "image_url")
                                        put("image_url", JSONObject().apply {
                                            put("url", base64ImageAndText) // Embed the base64 data URI
                                        })
                                    })
                                })
                            })
                        })
                    }.toString()

                    val request = Request.Builder()
                        .url("https://api.openai.com/v1/chat/completions")
                        .post(RequestBody.create("application/json".toMediaTypeOrNull(), requestBody))
                        .addHeader("Authorization", "Bearer $chatGPTApiKey")
                        .build()
                    Log.d("Lifelog", "The request is prepared: $request")

                    // Execute request and parse response
                    val response = client.newCall(request).execute()
                    Log.d("MyApp", "ChatGPT response: $response")
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")

                    val jsonResponse = JSONObject(response.body?.string() ?: "")
                    val choicesArray = jsonResponse.getJSONArray("choices")
                    Log.d("Lifelog", "Choices for responses: $choicesArray")
                    if (choicesArray.length() > 0) {
                        val firstChoice = choicesArray.getJSONObject(0)
                        val messageObject = firstChoice.getJSONObject("message")
                        val content = messageObject.getString("content")
                        return@withContext content // Return the extracted content
                    } else {
                        return@withContext null // Return null if no choices found
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.e("Lifelog", e.message.toString())
                    null
                }
            }
        }
    }

    data class ChatResponse(
        val title: String,
        val date: String,
        val text: String,
        val mood: Int,
        val tags: List<String>
    )

    fun extractJson(rawString: String): String {
        // Regex pattern to match JSON blocks
        val regex = Regex("""\{.*\}""", RegexOption.DOT_MATCHES_ALL)

        // Find the first match for JSON
        val matchResult = regex.find(rawString)
        return matchResult?.value ?: throw IllegalArgumentException("No JSON block found in the input string")
    }

    suspend fun sendToNotionAPI(context: Context, description: String, imageUrl: String, tags: List<String>, mood: String): Boolean = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences("profile_data", Context.MODE_PRIVATE)
        val notionToken = prefs.getString("notionToken", null)
        val notionDatabaseId = prefs.getString("notionDatabaseId", null)

        if (notionToken.isNullOrEmpty() || notionDatabaseId.isNullOrEmpty()) {
            Log.e("Lifelog", "Notion Token or Database ID not found in SharedPreferences or is empty.")
            return@withContext false
        }

        Log.d("Lifelog", "Response before cleaning: $description")
        val cleanResponse = extractJson(description)
        Log.d("Lifelog", "Cleaned response: $cleanResponse")
        val gson = Gson()
        val responseData: ChatResponse = gson.fromJson(cleanResponse, ChatResponse::class.java) // Renamed to responseData to avoid conflict

        Log.d("Lifelog", "Entering the Notion update method")
        val notionBody = JSONObject().apply {
            put("parent", JSONObject().apply {
                put("database_id", notionDatabaseId)
            })

            put("properties", JSONObject().apply {
                put("Title", JSONObject().apply {
                    put("title", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", JSONObject().apply {
                                put("content", responseData.title)
                            })
                        })
                    })
                })

                put("Date", JSONObject().apply {
                    put("date", JSONObject().apply {
                        put("start", responseData.date)
                    })
                })

                put("Text", JSONObject().apply {
                    put("rich_text", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", JSONObject().apply {
                                put("content", responseData.text)
                            })
                        })
                    })
                })

                put("Tags", JSONObject().apply {
                    put("multi_select", JSONArray().apply {
                        responseData.tags.forEach {
                            put(JSONObject().apply {
                                put("name", it)
                            })
                        }
                    })
                })

                put("Mood", JSONObject().apply {
                    put("select", JSONObject().apply {
                        put("name", responseData.mood.toString())
                    })
                })
            })
        }

        Log.d("Lifelog", "Sending request to Notion API: $notionBody")

        val request = Request.Builder()
            .url("https://api.notion.com/v1/pages")
            .post(RequestBody.create("application/json".toMediaTypeOrNull(), notionBody.toString()))
            .addHeader("Authorization", "Bearer $notionToken")
            .addHeader("Notion-Version", "2022-06-28")
            .build()

        // client is already a class member, no need to redefine
        // val client = OkHttpClient()

        return@withContext try {
            val notionHttpResponse = client.newCall(request).execute() // Renamed to avoid conflict
            if (notionHttpResponse.isSuccessful) {
                true
            } else {
                val errorMessage = notionHttpResponse.body?.string() ?: "Unknown error occurred"
                Log.e("Lifelog", "Notion API error: $errorMessage")
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("Lifelog", e.message.toString())
            false
        }
    }

    fun sendToGeminiAPI(context: Context, imageUri: Uri?, comment: String, chatHistory: MutableList<Pair<String, String>>): String? {
        return runBlocking {
            withContext(Dispatchers.IO) {
                val prefs = context.getSharedPreferences("profile_data", Context.MODE_PRIVATE)
                val geminiApiKey = prefs.getString("geminiApiKey", null)

                if (geminiApiKey.isNullOrEmpty()) {
                    Log.e("Lifelog", "Gemini API Key not found in SharedPreferences or is empty.")
                    return@withContext null
                }

                var language = prefs.getString("language", "English") ?: "English"
                // Potentially use selectedLanguage if it's different and relevant for Gemini prompt
                // if (language.compareTo(selectedLanguage) != 0){
                //     language = selectedLanguage
                // }

                // System instructions/prompt for Gemini
                val basePrompt = "You are an application for creating a life log. " +
                        "Based on this description the user should be able to understand what happened that moment. " +
                        "Try to describe as narrator, from first person. Try to identify small details in the photo and embed into a story." +
                        "Based on the image (if provided) and comment, create a rich diary entry. " +
                        "Respond strictly in $language language. User profile: Name - ${prefs.getString("name", "Unknown")}, " +
                        "sex - ${prefs.getString("sex", "Unknown")}, age - ${prefs.getString("age", "Unknown")}, " +
                        "work - ${prefs.getString("work", "Unknown")}, hobby - ${prefs.getString("hobby", "Unknown")}. " +
                        "Evaluate mood (1-5). Create a short title. Suggest tags (array of strings). " +
                        "Return JSON: {'title': String, 'date': 'YYYY-MM-DDTHH:mm:ss.sssZ', 'text': String, 'mood': Int, 'tags': List<String>}."

                try {
                    val contentsArray = JSONArray()

                    // Add previous chat history
                    for (turn in chatHistory) {
                        contentsArray.put(JSONObject().apply {
                            put("role", turn.first) // "user" or "model"
                            put("parts", JSONArray().apply {
                                put(JSONObject().apply { put("text", turn.second) })
                            })
                        })
                    }

                    // Prepare current user message parts
                    val currentUserParts = JSONArray()
                    var currentMessageText = comment

                    if (chatHistory.isEmpty()) {
                        // If history is empty, this is the first turn. Prepend base prompt to user comment.
                        currentMessageText = "$basePrompt Current time: ${LocalDateTime.now()}. User comment: $comment"
                    } else {
                        currentMessageText = "Current time: ${LocalDateTime.now()}. User comment: $comment"
                    }
                    currentUserParts.put(JSONObject().apply { put("text", currentMessageText) })

                    var base64Image: String? = null
                    if (imageUri != null) {
                        val resizedImageBytes = resizeImage(context, imageUri, 400, 400) // Adjust dimensions
                        base64Image = Base64.encodeToString(resizedImageBytes, Base64.NO_WRAP)
                        currentUserParts.put(JSONObject().apply {
                            put("inline_data", JSONObject().apply {
                                put("mime_type", "image/jpeg")
                                put("data", base64Image)
                            })
                        })
                    }

                    // Add current user message to contents
                    contentsArray.put(JSONObject().apply {
                        put("role", "user")
                        put("parts", currentUserParts)
                    })

                    val requestJson = JSONObject().apply {
                        put("contents", contentsArray)
                        // Optional: Add generationConfig and safetySettings if needed
                        // put("generationConfig", JSONObject().apply { put("temperature", 0.7) })
                        // put("safetySettings", JSONArray().apply { ... })
                    }

                    val requestBodyString = requestJson.toString()
                    val request = Request.Builder()
                        .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=$geminiApiKey")
                        .post(RequestBody.create("application/json".toMediaTypeOrNull(), requestBodyString))
                        .addHeader("Content-Type", "application/json")
                        .build()

                    Log.d("Lifelog", "Gemini API Request: $requestBodyString")

                    val response = client.newCall(request).execute()
                    Log.d("Lifelog", "Gemini API Response: $response")

                    if (!response.isSuccessful) {
                        val errorBody = response.body?.string()
                        Log.e("Lifelog", "Gemini API error: ${response.code} - $errorBody")
                        // Add user message to history even if API call fails, for context in next retry? Or not?
                        // For now, only adding on success.
                        throw IOException("Unexpected code ${response.code} - $errorBody")
                    }

                    val responseBody = response.body?.string()
                    if (responseBody.isNullOrEmpty()) {
                        Log.e("Lifelog", "Gemini API response body is null or empty.")
                        return@withContext null
                    }

                    Log.d("Lifelog", "Gemini API Response Body: $responseBody")
                    val jsonResponse = JSONObject(responseBody)
                    // Add current user message to history
                    chatHistory.add(Pair("user", currentMessageText)) // Add the full text sent to Gemini

                    val candidates = jsonResponse.optJSONArray("candidates")
                    if (candidates != null && candidates.length() > 0) {
                        val firstCandidate = candidates.getJSONObject(0)
                        val content = firstCandidate.optJSONObject("content")
                        if (content != null) {
                            val parts = content.optJSONArray("parts")
                            if (parts != null && parts.length() > 0) {
                                val firstPart = parts.getJSONObject(0)
                                val generatedText = firstPart.optString("text", null)
                                if (generatedText != null) {
                                    chatHistory.add(Pair("model", generatedText))
                                    return@withContext generatedText
                                } else {
                                    Log.e("Lifelog", "Gemini API: 'text' field missing in the first part of the response.")
                                }
                            } else {
                                Log.e("Lifelog", "Gemini API: 'parts' array is missing or empty in the response.")
                            }
                        } else {
                            Log.e("Lifelog", "Gemini API: 'content' object is missing in the first candidate.")
                        }
                    } else {
                        // Check for promptFeedback if no candidates
                        val promptFeedback = jsonResponse.optJSONObject("promptFeedback")
                        if (promptFeedback != null) {
                            Log.e("Lifelog", "Gemini API: Call failed due to prompt feedback: $promptFeedback")
                        } else {
                            Log.e("Lifelog", "Gemini API: 'candidates' array is missing or empty and no promptFeedback.")
                        }
                    }
                    return@withContext null

                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.e("Lifelog", "Error in sendToGeminiAPI: ${e.message}")
                    // chatHistory.add(Pair("user", comment)) // Add user message even on exception?
                    null
                }
            }
        }
    }
}
