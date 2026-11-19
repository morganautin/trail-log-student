package fr.estiam.traillog.ui.visits

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.ProgressBar
import androidx.core.view.ViewCompat
import fr.estiam.traillog.R
import fr.estiam.traillog.model.ConditionLevel

/**
 * Vue personnalisée "indicateur d'état" d'un sentier (projet final).
 *
 * Rendu : une jauge arrondie remplie selon la note (0..100), colorée selon le niveau
 * (rouge = mauvais, orange = moyen, vert = bon), avec des repères aux seuils 40 et 70
 * et un repère rond à l'extrémité du remplissage.
 *
 * Accessibilité :
 * - contentDescription générée automatiquement ("État du sentier : 72 sur 100, bon") ;
 * - exposée à TalkBack comme une barre de progression (RangeInfo 0..100) ;
 * - région "live" polie : TalkBack annonce le nouvel état quand la note change.
 */
class ConditionIndicatorView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#E3E6E1") }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#8A948C"); strokeWidth = 2 * density
    }
    private val knobPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val knobStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 3 * density }
    private val rect = RectF()

    /** Note actuelle, toujours ramenée dans 0..100. */
    var score: Int = 0
        private set

    init {
        // Lecture de l'attribut XML app:conditionScore (valeur de prévisualisation)
        val a = context.obtainStyledAttributes(attrs, R.styleable.ConditionIndicatorView, defStyleAttr, 0)
        val initial = try { a.getInt(R.styleable.ConditionIndicatorView_conditionScore, 0) } finally { a.recycle() }
        isFocusable = true
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        // Annonce automatique par TalkBack quand la description change
        accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        setScore(initial)
    }

    /** Met à jour la note, les couleurs, la description accessible, puis redessine. */
    fun setScore(value: Int) {
        score = value.coerceIn(0, 100)
        val level = ConditionLevel.fromScore(score)
        val color = colorFor(level)
        fillPaint.color = color
        knobStroke.color = color
        val levelLabel = context.getString(labelRes(level))
        contentDescription = context.getString(R.string.condition_a11y, score, levelLabel)
        ViewCompat.setStateDescription(this, levelLabel)
        invalidate()
    }

    /** Hauteur par défaut raisonnable si le layout utilise wrap_content. */
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desired = (40 * density).toInt() + paddingTop + paddingBottom
        val w = View.MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(w, View.resolveSize(desired, heightMeasureSpec))
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val left = paddingLeft + 12 * density
        val right = width - paddingRight - 12 * density
        if (right <= left) return
        val centerY = paddingTop + (height - paddingTop - paddingBottom) / 2f
        val half = 7 * density
        val radius = half

        // 1. Piste grise (100 %)
        rect.set(left, centerY - half, right, centerY + half)
        canvas.drawRoundRect(rect, radius, radius, trackPaint)

        // 2. Remplissage proportionnel à la note, couleur du niveau
        val fillRight = left + (right - left) * score / 100f
        if (score > 0) {
            rect.set(left, centerY - half, fillRight.coerceAtLeast(left + 2 * radius), centerY + half)
            canvas.drawRoundRect(rect, radius, radius, fillPaint)
        }

        // 3. Repères des seuils 40 et 70 (changement de niveau)
        for (threshold in listOf(40, 70)) {
            val x = left + (right - left) * threshold / 100f
            canvas.drawLine(x, centerY - half - 4 * density, x, centerY + half + 4 * density, tickPaint)
        }

        // 4. Repère rond à l'extrémité : la forme reste lisible même sans percevoir la couleur
        val knobRadius = 10 * density
        canvas.drawCircle(fillRight, centerY, knobRadius, knobPaint)
        canvas.drawCircle(fillRight, centerY, knobRadius, knobStroke)
    }

    /** Présente la vue à TalkBack comme une barre de progression 0..100. */
    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        info.className = ProgressBar::class.java.name
        @Suppress("DEPRECATION")
        info.rangeInfo = AccessibilityNodeInfo.RangeInfo.obtain(
            AccessibilityNodeInfo.RangeInfo.RANGE_TYPE_INT, 0f, 100f, score.toFloat())
    }

    companion object {
        fun colorFor(level: ConditionLevel): Int = when (level) {
            ConditionLevel.GOOD -> Color.parseColor("#2E7D32")
            ConditionLevel.FAIR -> Color.parseColor("#E08A00")
            ConditionLevel.POOR -> Color.parseColor("#C62828")
        }

        fun labelRes(level: ConditionLevel): Int = when (level) {
            ConditionLevel.GOOD -> R.string.condition_good
            ConditionLevel.FAIR -> R.string.condition_fair
            ConditionLevel.POOR -> R.string.condition_poor
        }
    }
}
