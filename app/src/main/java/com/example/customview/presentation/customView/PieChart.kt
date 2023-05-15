package com.example.customview.presentation.customView

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF
import android.os.Parcelable
import android.util.AttributeSet
import android.view.View
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.example.customview.R
import com.example.customview.domain.extensions.dpToPx
import com.example.customview.domain.model.PieChartModel
import com.example.customview.domain.model.PieChartState
import com.example.customview.domain.repository.PieChartRepository

/**
 * Кольцевая диаграмма для отображения статистики объектов в процентном соотношении.
 *
 * PieChart адаптируется под любое значение высоты и ширины, которое передается
 * данной View от parent.
 * Проверено на всех возможных разрешениях экрана (ldpi, mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi).
 *
 * Для удобства использования и сборки в DI, класс имплементирует интерфейс взаимодействия PieChartInterface
 *
 *
 * AnalyticalPieChart обладает огромным количество настроек отображения.
 * Все расчеты производятся в пикселях, необходимо это учитывать при изменении кода.
 *
 * @property circleRect        объект отрисовки круговой диаграммы.
 * @property circleStrokeWidth значение толщины круговой диаграммы.
 * @property circleRadius      значение радиуса круговой диаграммы.
 * @property circlePadding     padding для всех сторон круговой диаграммы.
 * @property circlePaintRoundSize значение округления концов линий объектов круга.
 * @property circleSectionSpace   значение расстояние-процент между линиями круга.
 * @property circleCenterX        значение координаты X центра круговой диаграммы.
 * @property circleCenterY        значение координаты Y центра круговой диаграммы.
 * @property totalAmount          итоговый результат - сумма значений Int в [dataList].
 * @property pieChartColors       список цветов круговой диаграммы в виде текстового представления.
 * @property percentageCircleList список моделей для отрисовки.
 * @property dataList             исходный список данных.
 * @property animationSweepAngle  переменная для анимации.
 */

class PieChart @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), PieChartRepository {

    /**
     * Базовые значения для полей и самой PieChart
     */
    companion object {

        /** Процент ширины для отображения круговой диаграммы от общей ширины View */
        // 0.97 - 100%
        private const val CIRCLE_WIDTH_PERCENT = 0.97

        /** Базовые значения ширины и высоты View */
        const val DEFAULT_VIEW_SIZE_HEIGHT = 150
        const val DEFAULT_VIEW_SIZE_WIDTH = 250
    }

    private val circleRect = RectF()
    private var circleStrokeWidth: Float = context.dpToPx(0)
    private var circleRadius: Float = 0F
    private var circlePadding: Float = context.dpToPx(0)
    private var circlePaintRoundSize: Boolean = true
    private var circleSectionSpace: Float = 3F
    private var circleCenterX: Float = 0F
    private var circleCenterY: Float = 0F
    private var totalAmount: Int = 0
    private var pieChartColors: List<String> = listOf()
    private var percentageCircleList: List<PieChartModel> = listOf()
    private var dataList: List<Int> = listOf()
    private var animationSweepAngle: Int = 0

    /**
     * В INIT блоке инициализируются все необходимые поля и переменные.
     * Необходимые значения вытаскиваются из специальных Attr тегов
     * (<declare-styleable name="PieChart">).
     */
    init {

        // Инициализируем поля View, если Attr присутствуют
        if (attrs != null) {
            val typeArray = context.obtainStyledAttributes(attrs, R.styleable.PieChart)

            // Секция списка цветов
            val colorResId = typeArray.getResourceId(R.styleable.PieChart_pieChartColors, 0)
            pieChartColors = typeArray.resources.getStringArray(colorResId).toList()

            // Секция круговой диаграммы
            circleStrokeWidth = typeArray.getDimension(
                R.styleable.PieChart_pieChartCircleStrokeWidth,
                circleStrokeWidth
            )
            circlePadding =
                typeArray.getDimension(R.styleable.PieChart_pieChartCirclePadding, circlePadding)
            circlePaintRoundSize = typeArray.getBoolean(
                R.styleable.PieChart_pieChartCirclePaintRoundSize,
                circlePaintRoundSize
            )
            circleSectionSpace = typeArray.getFloat(
                R.styleable.PieChart_pieChartCircleSectionSpace,
                circleSectionSpace
            )

            typeArray.recycle()
        }

        circlePadding += circleStrokeWidth
    }

    /**
     * Метод жизненного цикла View.
     * Расчет необходимой ширины и высоты View.
     */
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {

        val initSizeWidth = resolveDefaultSize(widthMeasureSpec, DEFAULT_VIEW_SIZE_WIDTH)

        val initSizeHeight = calculateViewHeight(heightMeasureSpec)

        calculateCircleRadius(initSizeWidth, initSizeHeight)

        setMeasuredDimension(initSizeWidth, initSizeHeight)
    }

    /**
     * Метод жизненного цикла View.
     * Отрисовка всех необходимых компонентов на Canvas.
     */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        drawCircle(canvas)
    }

    /**
     * Восстановление данных из PieChartState
     */
    override fun onRestoreInstanceState(state: Parcelable?) {
        val analyticalPieChartState = state as? PieChartState
        super.onRestoreInstanceState(analyticalPieChartState?.superState ?: state)

        dataList = analyticalPieChartState?.dataList ?: listOf()
    }

    /**
     * Сохранение dataList в собственный PieChartState
     */
    override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        return PieChartState(superState, dataList)
    }

    /**
     * Имплиментируемый метод интерфейса взаимодействия PieChartInterface.
     * Добавление данных в View.
     */
    override fun setDataChart(list: List<Int>) {
        dataList = list
        calculatePercentageOfData()
    }

    /**
     * Имплиментируемый метод интерфейса взаимодействия PieChartInterface.
     * Запуск анимации отрисовки View.
     */
    override fun startAnimation() {
        // Проход значений от 0 до 360 (целый круг), с длительностью - 1.5 секунды
        val animator = ValueAnimator.ofInt(0, 360).apply {
            duration = 1500
            interpolator = FastOutSlowInInterpolator()
            addUpdateListener { valueAnimator ->
                animationSweepAngle = valueAnimator.animatedValue as Int
                invalidate()
            }
        }
        animator.start()
    }

    /**
     * Метод отрисовки круговой диаграммы на Canvas.
     */
    private fun drawCircle(canvas: Canvas) {
        for (percent in percentageCircleList) {
            if (animationSweepAngle > percent.percentToStartAt + percent.percentOfCircle) {
                canvas.drawArc(
                    circleRect,
                    percent.percentToStartAt,
                    percent.percentOfCircle,
                    false,
                    percent.paint
                )
            } else if (animationSweepAngle > percent.percentToStartAt) {
                canvas.drawArc(
                    circleRect,
                    percent.percentToStartAt,
                    animationSweepAngle - percent.percentToStartAt,
                    false,
                    percent.paint
                )
            }
        }
    }

    /**
     * Метод получения размера View по переданному Mode.
     */
    private fun resolveDefaultSize(spec: Int, defValue: Int): Int {
        return when (MeasureSpec.getMode(spec)) {
            MeasureSpec.UNSPECIFIED -> context.dpToPx(defValue).toInt()
            else -> MeasureSpec.getSize(spec)
        }
    }

    /**
     * Метод расчёта высоты всего View, включая отступы.
     */
    private fun calculateViewHeight(heightMeasureSpec: Int): Int {
        val initSizeHeight = resolveDefaultSize(heightMeasureSpec, DEFAULT_VIEW_SIZE_HEIGHT)

        val textHeightWithPadding = paddingTop + paddingBottom
        return if (textHeightWithPadding > initSizeHeight) textHeightWithPadding else initSizeHeight
    }

    /**
     * Метод расчёта радиуса круговой диаграммы, установка координат для отрисовки.
     */
    private fun calculateCircleRadius(width: Int, height: Int) {
        val circleViewWidth = (width * CIRCLE_WIDTH_PERCENT)
        circleRadius = if (circleViewWidth > height) {
            (height.toFloat() - circlePadding) / 2
        } else {
            circleViewWidth.toFloat() / 2
        }

        with(circleRect) {
            left = circlePadding
            top = height / 2 - circleRadius
            right = circleRadius * 2 + circlePadding
            bottom = height / 2 + circleRadius
        }

        circleCenterX = (circleRadius * 2 + circlePadding + circlePadding) / 2
        circleCenterY = (height / 2 + circleRadius + (height / 2 - circleRadius)) / 2
    }

    /**
     * Метод заполнения поля [percentageCircleList]
     */
    private fun calculatePercentageOfData() {
        totalAmount = dataList.fold(0) { res, value -> res + value }

        var startAt = circleSectionSpace
        percentageCircleList = dataList.mapIndexed { index, pair ->
            var percent = pair * 100 / totalAmount.toFloat() - circleSectionSpace
            percent = if (percent < 0F) 0F else percent

            val resultModel = PieChartModel(
                percentOfCircle = percent,
                percentToStartAt = startAt,
                colorOfLine = Color.parseColor(pieChartColors[index % pieChartColors.size]),
                stroke = circleStrokeWidth,
                paintRound = circlePaintRoundSize
            )
            startAt += percent + circleSectionSpace
            resultModel
        }
    }

}