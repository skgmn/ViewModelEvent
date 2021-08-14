package com.github.skgmn.viewmodelevent.sample.test

import android.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import com.github.skgmn.viewmodelevent.answer
import com.github.skgmn.viewmodelevent.handle
import com.github.skgmn.viewmodelevent.sample.test.databinding.ActivityMainBinding
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.viewModel = viewModel

        handleEvents()
    }

    private fun handleEvents() = with(viewModel) {
        handle(feedback) {
            val yesNo = if (it) "Yes" else "No"
            Toast.makeText(
                this@MainActivity, "You said $yesNo", Toast.LENGTH_SHORT
            ).show()
        }
        answer(askUserToSelect) { message ->
            suspendCancellableCoroutine { cont ->
                AlertDialog.Builder(this@MainActivity)
                    .setMessage(message)
                    .setPositiveButton("Yes") { _, _ ->
                        cont.resume(true)
                    }
                    .setNegativeButton("No") { _, _ ->
                        cont.resume(false)
                    }
                    .setOnCancelListener {
                        cont.cancel()
                    }
                    .setCancelable(true)
                    .show()
            }
        }
    }
}