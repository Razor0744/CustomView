package com.example.customview.presentation.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.customview.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

    }

    override fun onStart() {
        super.onStart()

        pieChart()
    }

    private fun pieChart() {
        binding.pieChart.setDataChart(
            listOf(
                Pair(4, "вроде должно работать"),
                Pair(5, "ahahahahha"),
                Pair(10, "notBad")
            )
        )
        binding.pieChart.startAnimation()
    }
}