package com.github.skgmn.viewmodelevent.sample.test

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import com.github.skgmn.viewmodelevent.handle
import com.github.skgmn.viewmodelevent.sample.test.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.viewModel = viewModel

        handle(viewModel.navigateToChild) {
            startActivity(Intent(this, ChildActivity::class.java))
        }
    }
}