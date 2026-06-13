package com.nemo.ai

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val statusText = findViewById<TextView>(R.id.statusText)
        val enableBtn = findViewById<Button>(R.id.enableAccessibilityBtn)

        // Check if accessibility is enabled
        updateStatus(statusText)

        // Button opens accessibility settings
        enableBtn.setOnClickListener {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        val statusText = findViewById<TextView>(R.id.statusText)
        updateStatus(statusText)
    }

    private fun updateStatus(statusText: TextView) {
        val enabled = isAccessibilityEnabled()
        if (enabled) {
            statusText.text = "✅ Nemo is Active and Ready!"
        } else {
            statusText.text = "⚠️ Please enable Nemo in Accessibility Settings"
        }
    }

    private fun isAccessibilityEnabled(): Boolean {
        val service = "${packageName}/${NemoAccessibilityService::class.java.canonicalName}"
        val enabled = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        return enabled?.contains(service) == true
    }
}
