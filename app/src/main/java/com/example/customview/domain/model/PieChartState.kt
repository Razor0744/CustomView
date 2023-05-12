package com.example.customview.domain.model

import android.os.Parcelable
import android.view.View

class PieChartState(
    private val superSavedState: Parcelable?,
    val dataList: List<Pair<Int, String>>
) : View.BaseSavedState(superSavedState), Parcelable {
}