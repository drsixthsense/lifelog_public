package com.mim.lifelog

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class MainActivityLogicTest {

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockSharedPreferences: SharedPreferences

    @Mock
    private lateinit var mockEditor: SharedPreferences.Editor

    @Mock
    private lateinit var mockImageUri: Uri

    private lateinit var mainActivityInstance: MainActivity

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        mainActivityInstance = MainActivity()

        whenever(mockContext.getSharedPreferences(any(), any())).thenReturn(mockSharedPreferences)
        whenever(mockSharedPreferences.edit()).thenReturn(mockEditor)
        whenever(mockEditor.putString(any(), any())).thenReturn(mockEditor)
        whenever(mockEditor.apply()).then {} // For void methods
    }

    @Test
    fun testSaveProfile_savesApiKeysToSharedPreferences() {
        val profileData = ProfileData(
            name = "Test User",
            age = "30",
            sex = "Other",
            work = "Developer",
            hobby = "Testing",
            language = "Klingon",
            notionToken = "testNotionToken123",
            notionDatabaseId = "testNotionDbId456",
            chatGPTApiKey = "testChatGPTKey789",
            geminiApiKey = "testGeminiKeyABC"
        )

        mainActivityInstance.saveProfile(mockContext, profileData)

        verify(mockEditor).putString("name", "Test User")
        verify(mockEditor).putString("age", "30")
        verify(mockEditor).putString("sex", "Other")
        verify(mockEditor).putString("work", "Developer")
        verify(mockEditor).putString("hobby", "Testing")
        verify(mockEditor).putString("language", "Klingon")
        verify(mockEditor).putString("notionToken", "testNotionToken123")
        verify(mockEditor).putString("notionDatabaseId", "testNotionDbId456")
        verify(mockEditor).putString("chatGPTApiKey", "testChatGPTKey789")
        verify(mockEditor).putString("geminiApiKey", "testGeminiKeyABC")
        verify(mockEditor).apply()
    }

    @Test
    fun testSendToChatGPT_missingKey_returnsNull() {
        whenever(mockSharedPreferences.getString(eq("chatGPTApiKey"), any())).thenReturn(null)
        // Mocking resizeImage to avoid actual image processing
        // This part is tricky as resizeImage is a top-level extension function in ImageUtil.kt
        // For this test, we assume sendToChatGPT will short-circuit before calling it.
        // If it were a direct dependency, we'd mock it.

        val result = mainActivityInstance.sendToChatGPT(mockContext, mockImageUri, "test comment")
        assertNull(result)
        // Verify no network call is attempted (not shown here, but implied by null return)
    }

    @Test
    fun testSendToNotionAPI_missingToken_returnsFalse() {
        whenever(mockSharedPreferences.getString(eq("notionToken"), any())).thenReturn(null)
        whenever(mockSharedPreferences.getString(eq("notionDatabaseId"), any())).thenReturn("testDbId")

        val result = mainActivityInstance.sendToNotionAPI(mockContext, "description", "imageUrl", emptyList(), "1")
        assertFalse(result)
    }

    @Test
    fun testSendToNotionAPI_missingDatabaseId_returnsFalse() {
        whenever(mockSharedPreferences.getString(eq("notionToken"), any())).thenReturn("testToken")
        whenever(mockSharedPreferences.getString(eq("notionDatabaseId"), any())).thenReturn(null)

        val result = mainActivityInstance.sendToNotionAPI(mockContext, "description", "imageUrl", emptyList(), "1")
        assertFalse(result)
    }

    @Test
    fun testSendToNotionAPI_bothKeysMissing_returnsFalse() {
        whenever(mockSharedPreferences.getString(eq("notionToken"), any())).thenReturn(null)
        whenever(mockSharedPreferences.getString(eq("notionDatabaseId"), any())).thenReturn(null)

        val result = mainActivityInstance.sendToNotionAPI(mockContext, "description", "imageUrl", emptyList(), "1")
        assertFalse(result)
    }

    @Test
    fun testSendToGeminiAPI_missingKey_returnsNull() {
        whenever(mockSharedPreferences.getString(eq("geminiApiKey"), any())).thenReturn(null)
        // Similar to ChatGPT, assuming short-circuit before image processing for this specific test case

        val result = mainActivityInstance.sendToGeminiAPI(mockContext, null, "test comment", mutableListOf())
        assertNull(result)
    }
}
