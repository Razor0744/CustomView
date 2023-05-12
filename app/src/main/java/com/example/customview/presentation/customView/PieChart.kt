package com.example.customview.presentation.customView

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Parcelable
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristic
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.example.customview.R
import com.example.customview.domain.extensions.dpToPx
import com.example.customview.domain.extensions.spToPx
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
 * @property marginTextFirst   значение отступа между числом и его описанием.
 * @property marginTextSecond  значение отступа между объектами, где объект - число и описание.
 * @property marginTextThird   значение отступа между числом и его описанием общего результата.
 * @property marginSmallCircle значение отступа между числом и маленьким кругом.
 * @property marginText        значение суммы отступов [marginTextFirst] и [marginTextSecond].
 * @property circleRect        объект отрисовки круговой диаграммы.
 * @property circleStrokeWidth значение толщины круговой диаграммы.
 * @property circleRadius      значение радиуса круговой диаграммы.
 * @property circlePadding     padding для всех сторон круговой диаграммы.
 * @property circlePaintRoundSize значение округления концов линий объектов круга.
 * @property circleSectionSpace   значение расстояние-процент между линиями круга.
 * @property circleCenterX        значение координаты X центра круговой диаграммы.
 * @property circleCenterY        значение координаты Y центра круговой диаграммы.
 * @property numberTextPaint      объект кисти отрисовки текста чисел.
 * @property descriptionTextPain  объект кисти отрисовки текста описания.
 * @property amountTextPaint      объект кисти отрисовки текста результата.
 * @property textStartX           значение координаты X, откуда отрисовывается текст.
 * @property textStartY           значение координаты Y, откуда отрисовывается текст.
 * @property textHeight           значение высоты текста.
 * @property textCircleRadius     значение радиуса малого круга около текста числа.
 * @property textAmountStr        строка результата.
 * @property textAmountY          значение координаты Y, откуда отрисовывается результирующий текст.
 * @property textAmountXNumber    значение координаты X, откуда отрисовывается результирующий текст числа.
 * @property textAmountXDescription     значение координаты X, откуда отрисовывается описание результата.
 * @property textAmountYDescription     значение координаты Y, откуда отрисовывается описание результата.
 * @property totalAmount          итоговый результат - сумма значений Int в [dataList].
 * @property pieChartColors       список цветов круговой диаграммы в виде текстового представления.
 * @property percentageCircleList список моделей для отрисовки.
 * @property textRowList          список строк, которые необходимо отобразить.
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
        private const val DEFAULT_MARGIN_TEXT_1 = 2
        private const val DEFAULT_MARGIN_TEXT_2 = 10
        private const val DEFAULT_MARGIN_TEXT_3 = 2
        private const val DEFAULT_MARGIN_SMALL_CIRCLE = 12

        /** Процент ширины для отображения текста от общей ширины View */
        private const val TEXT_WIDTH_PERCENT = 0.40

        /** Процент ширины для отображения круговой диаграммы от общей ширины View */
        private const val CIRCLE_WIDTH_PERCENT = 0.50

        /** Базовые значения ширины и высоты View */
        const val DEFAULT_VIEW_SIZE_HEIGHT = 150
        const val DEFAULT_VIEW_SIZE_WIDTH = 250
    }

    private var marginTextFirst: Float = context.dpToPx(DEFAULT_MARGIN_TEXT_1)
    private var marginTextSecond: Float = context.dpToPx(DEFAULT_MARGIN_TEXT_2)
    private var marginTextThird: Float = context.dpToPx(DEFAULT_MARGIN_TEXT_3)
    private var marginSmallCircle: Float = context.dpToPx(DEFAULT_MARGIN_SMALL_CIRCLE)
    private val marginText: Float = marginTextFirst + marginTextSecond
    private val circleRect = RectF()
    private var circleStrokeWidth: Float = context.dpToPx(6)
    private var circleRadius: Float = 0F
    private var circlePadding: Float = context.dpToPx(8)
    private var circlePaintRoundSize: Boolean = true
    private var circleSectionSpace: Float = 3F
    private var circleCenterX: Float = 0F
    private var circleCenterY: Float = 0F
    private var numberTextPaint: TextPaint = TextPaint()
    private var descriptionTextPain: TextPaint = TextPaint()
    private var amountTextPaint: TextPaint = TextPaint()
    private var textStartX: Float = 0F
    private var textStartY: Float = 0F
    private var textHeight: Int = 0
    private var textCircleRadius: Float = context.dpToPx(4)
    private var textAmountStr: String = ""
    private var textAmountY: Float = 0F
    private var textAmountXNumber: Float = 0F
    private var textAmountXDescription: Float = 0F
    private var textAmountYDescription: Float = 0F
    private var totalAmount: Int = 0
    private var pieChartColors: List<String> = listOf()
    private var percentageCircleList: List<PieChartModel> = listOf()
    private var dataList: List<Pair<Int, String>> = listOf()
    private var animationSweepAngle: Int = 0

    /**
     * В INIT блоке инициализируются все необходимые поля и переменные.
     * Необходимые значения вытаскиваются из специальных Attr тегов
     * (<declare-styleable name="PieChart">).
     */
    init {
        // Задаем базовые значения и конвертируем в px
        var textAmountSize: Float = context.spToPx(22)
        var textNumberSize: Float = context.spToPx(20)
        var textDescriptionSize: Float = context.spToPx(14)
        var textAmountColor: Int = Color.WHITE
        var textNumberColor: Int = Color.WHITE
        var textDescriptionColor: Int = Color.GRAY

        // Инициализируем поля View, если Attr присутствуют
        if (attrs != null) {
            val typeArray = context.obtainStyledAttributes(attrs, R.styleable.PieChart)

            // Секция списка цветов
            val colorResId = typeArray.getResourceId(R.styleable.PieChart_pieChartColors, 0)
            pieChartColors = typeArray.resources.getStringArray(colorResId).toList()

            // Секция отступов
            marginTextFirst = typeArray.getDimension(
                R.styleable.PieChart_pieChartMarginTextFirst,
                marginTextFirst
            )
            marginTextSecond = typeArray.getDimension(
                R.styleable.PieChart_pieChartMarginTextSecond,
                marginTextSecond
            )
            marginTextThird = typeArray.getDimension(
                R.styleable.PieChart_pieChartMarginTextThird,
                marginTextThird
            )
            marginSmallCircle = typeArray.getDimension(
                R.styleable.PieChart_pieChartMarginSmallCircle,
                marginSmallCircle
            )

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

            // Секция текста
            textCircleRadius = typeArray.getDimension(
                R.styleable.PieChart_pieChartTextCircleRadius,
                textCircleRadius
            )
            textAmountSize =
                typeArray.getDimension(R.styleable.PieChart_pieChartTextAmountSize, textAmountSize)
            textNumberSize =
                typeArray.getDimension(R.styleable.PieChart_pieChartTextNumberSize, textNumberSize)
            textDescriptionSize = typeArray.getDimension(
                R.styleable.PieChart_pieChartTextDescriptionSize,
                textDescriptionSize
            )
            textAmountColor =
                typeArray.getColor(R.styleable.PieChart_pieChartTextAmountColor, textAmountColor)
            textNumberColor =
                typeArray.getColor(R.styleable.PieChart_pieChartTextNumberColor, textNumberColor)
            textDescriptionColor = typeArray.getColor(
                R.styleable.PieChart_pieChartTextDescriptionColor,
                textDescriptionColor
            )
            textAmountStr = typeArray.getString(R.styleable.PieChart_pieChartTextAmount) ?: ""

            typeArray.recycle()
        }

        circlePadding += circleStrokeWidth

        // Инициализация кистей View
        initPains(amountTextPaint, textAmountSize, textAmountColor)
        initPains(numberTextPaint, textNumberSize, textNumberColor)
        initPains(descriptionTextPain, textDescriptionSize, textDescriptionColor, true)
    }

    /**
     * Метод жизненного цикла View.
     * Расчет необходимой ширины и высоты View.
     */
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {

        val initSizeWidth = resolveDefaultSize(widthMeasureSpec, DEFAULT_VIEW_SIZE_WIDTH)

        val textTextWidth = (initSizeWidth * TEXT_WIDTH_PERCENT)
        val initSizeHeight = calculateViewHeight(heightMeasureSpec, textTextWidth.toInt())

        textStartX = initSizeWidth - textTextWidth.toFloat()
        textStartY = initSizeHeight.toFloat() / 2 - textHeight / 2

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
        drawText(canvas)
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
    override fun setDataChart(list: List<Pair<Int, String>>) {
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
     * Метод отрисовки всего текста диаграммы на Canvas.
     */
    private fun drawText(canvas: Canvas) {

        canvas.drawText(totalAmount.toString(), textAmountXNumber, textAmountY, amountTextPaint)
        canvas.drawText(
            textAmountStr,
            textAmountXDescription,
            textAmountYDescription,
            descriptionTextPain
        )
    }

    /**
     * Метод инициализации переданной TextPaint
     */
    private fun initPains(
        textPaint: TextPaint,
        textSize: Float,
        textColor: Int,
        isDescription: Boolean = false
    ) {
        textPaint.color = textColor
        textPaint.textSize = textSize
        textPaint.isAntiAlias = true

        if (!isDescription) textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
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
     * Метод расчёта высоты всего текста, включая отступы.
     */
    private fun calculateViewHeight(heightMeasureSpec: Int, textWidth: Int): Int {
        val initSizeHeight = resolveDefaultSize(heightMeasureSpec, DEFAULT_VIEW_SIZE_HEIGHT)
        textHeight = (dataList.size * marginText).toInt()

        val textHeightWithPadding = textHeight + paddingTop + paddingBottom
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

        textAmountY = circleCenterY

        val sizeTextAmountNumber = getWidthOfAmountText(
            totalAmount.toString(),
            amountTextPaint
        )

        textAmountXNumber = circleCenterX - sizeTextAmountNumber.width() / 2
        textAmountXDescription =
            circleCenterX - getWidthOfAmountText(textAmountStr, descriptionTextPain).width() / 2
        textAmountYDescription = circleCenterY + sizeTextAmountNumber.height() + marginTextThird
    }

    /**
     * Метод заполнения поля [percentageCircleList]
     */
    private fun calculatePercentageOfData() {
        totalAmount = dataList.fold(0) { res, value -> res + value.first }

        var startAt = circleSectionSpace
        percentageCircleList = dataList.mapIndexed { index, pair ->
            var percent = pair.first * 100 / totalAmount.toFloat() - circleSectionSpace
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

    /**
     * Метод обертки текста в класс [Rect]
     */
    private fun getWidthOfAmountText(text: String, textPaint: TextPaint): Rect {
        val bounds = Rect()
        textPaint.getTextBounds(text, 0, text.length, bounds)
        return bounds
    }

}