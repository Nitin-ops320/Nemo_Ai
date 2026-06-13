package com.nemo.ai

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NemoBrain(private val context: Context) {

    private val gemini = GeminiService()
    private val scope = CoroutineScope(Dispatchers.Main)

    fun execute(userCommand: String, onResult: (String) -> Unit) {
        scope.launch {
            // Step 1 — Read screen
            val screenText = NemoAccessibilityService.instance?.readScreen()
                ?: "Screen not readable"

            // Step 2 — Ask Gemini what to do
            val response = gemini.think(userCommand, screenText)

            // Step 3 — Do the action
            when (response.action) {
                "tap" -> {
                    response.target?.let {
                        NemoAccessibilityService.instance?.tapByText(it)
                    }
                    onResult("✅ ${response.message}")
                }
                "scroll" -> {
                    NemoAccessibilityService.instance?.scrollDown()
                    onResult("✅ ${response.message}")
                }
                "open_app" -> {
                    response.target?.let { appName ->
                        openApp(appName)
                    }
                    onResult("✅ ${response.message}")
                }
                "read" -> {
                    onResult("👁️ ${response.message}")
                }
                else -> {
                    onResult("💬 ${response.message}")
                }
            }
        }
    }

    private fun openApp(appName: String) {
        val pm = context.packageManager
        val packages = pm.getInstalledApplications(0)
        val match = packages.firstOrNull {
            pm.getApplicationLabel(it).toString()
                .contains(appName, ignoreCase = true)
        }
        match?.let {
            val intent = pm.getLaunchIntentForPackage(it.packageName)
            intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
}
