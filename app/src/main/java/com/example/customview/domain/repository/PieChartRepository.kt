package com.example.customview.domain.repository

interface PieChartRepository {

    fun setDataChart(list: List<Int>)

    fun startAnimation()
}