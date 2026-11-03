package io.github.lucafregona.unscroll.ui

import android.app.Activity
import android.os.Bundle
import android.widget.TextView
import io.github.lucafregona.unscroll.R

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(TextView(this).apply { setText(R.string.app_name) })
    }
}
