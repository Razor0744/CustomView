package com.example.customview.domain.model

import android.os.Parcelable
import android.view.View

class PieChartState(
    superSavedState: Parcelable?,
    val dataList: List<Int>
) : View.BaseSavedState(superSavedState), Parcelable {
}