package com.sunisland.island

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.setPadding
import com.sunisland.astronomy.EventChoice
import com.sunisland.data.AppSettings
import com.sunisland.data.IslandFont
import com.sunisland.data.IslandLayout
import java.time.Duration
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

class IslandView(
    context: Context,
    private val onPositionChanged: (Int, Int) -> Unit
) : LinearLayout(context) {
    private val label = TextView(context)
    private val countdown = TextView(context)
    private val time = TextView(context)
    private var settings = AppSettings()
    private var eventTitle = "SUNSET"
    private var target: ZonedDateTime? = null
    private var downRawX = 0f
    private var downRawY = 0f
    private var startX = 0
    private var startY = 0
    private var draggable = true
    private var notchMode = false

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER
        setPadding(dp(12))
        isClickable = true
        label.text = eventTitle
        label.gravity = Gravity.CENTER
        countdown.gravity = Gravity.CENTER
        time.gravity = Gravity.CENTER
        addView(label, LayoutParams(-1, LayoutParams.WRAP_CONTENT))
        addView(countdown, LayoutParams(-1, LayoutParams.WRAP_CONTENT))
        addView(time, LayoutParams(-1, LayoutParams.WRAP_CONTENT))
        setOnTouchListener { _, ev ->
            if (!draggable) return@setOnTouchListener false
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downRawX = ev.rawX; downRawY = ev.rawY
                    (layoutParams as? WindowManager.LayoutParams)?.let { startX = it.x; startY = it.y }
                    true
                }
                MotionEvent.ACTION_MOVE, MotionEvent.ACTION_UP -> {
                    val lp = layoutParams as? WindowManager.LayoutParams ?: return@setOnTouchListener true
                    if (ev.actionMasked == MotionEvent.ACTION_DOWN) return@setOnTouchListener true
                    val dx = ev.rawX - downRawX
                    val dy = ev.rawY - downRawY
                    if (kotlin.math.abs(dx) > 1 || kotlin.math.abs(dy) > 1) {
                        lp.x = startX + dx.roundToInt()
                        lp.y = startY + dy.roundToInt()
                        onPositionChanged(lp.x, lp.y)
                    }
                    true
                }
                else -> true
            }
        }
    }

    fun beginDrag(lp: WindowManager.LayoutParams) { startX = lp.x; startY = lp.y }

    fun bind(settings: AppSettings, title: String, target: ZonedDateTime?) {
        this.settings = settings
        this.eventTitle = title
        this.target = target
        label.text = title
        style()
        updateCountdown()
    }

    fun updateCountdown() {
        val t = target ?: return
        val seconds = maxOf(0L, Duration.between(ZonedDateTime.now(t.zone), t).seconds)
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        countdown.text = if (settings.showSeconds) String.format(Locale.getDefault(), "%02d:%02d:%02d", h, m, s)
        else String.format(Locale.getDefault(), "%02d:%02d", h, m)
        time.text = DateTimeFormatter.ofPattern("HH:mm").format(t)
    }

    private fun style() {
        val alpha = (settings.opacity.coerceIn(20, 100) / 100f)
        draggable = settings.layout != IslandLayout.NOTCH
        notchMode = settings.layout == IslandLayout.NOTCH
        setBackgroundColor(Color.argb((255 * alpha).roundToInt(), 8, 10, 14))
        label.setTextColor(Color.WHITE)
        countdown.setTextColor(Color.WHITE)
        time.setTextColor(Color.LTGRAY)
        label.textSize = settings.labelSp.toFloat()
        countdown.textSize = settings.countdownSp.toFloat()
        time.textSize = 9f
        val typeface = when (settings.font) {
            IslandFont.SYSTEM, IslandFont.ROUNDED, IslandFont.DISPLAY -> Typeface.create("sans", if (settings.fontBold) Typeface.BOLD else Typeface.NORMAL)
            IslandFont.SERIF -> Typeface.create("serif", if (settings.fontBold) Typeface.BOLD else Typeface.NORMAL)
            IslandFont.MONO -> Typeface.create(Typeface.MONOSPACE, if (settings.fontBold) Typeface.BOLD else Typeface.NORMAL)
        }
        label.typeface = typeface; countdown.typeface = typeface; time.typeface = typeface
        when (settings.layout) {
            IslandLayout.NOTCH -> {
                label.visibility = View.GONE; countdown.visibility = View.GONE; time.visibility = View.GONE
            }
            IslandLayout.COMPACT, IslandLayout.MINIMAL -> { label.visibility = View.GONE; time.visibility = View.GONE }
            IslandLayout.ICON -> { label.text = "☀  $eventTitle"; time.visibility = View.GONE }
            IslandLayout.PROGRESS, IslandLayout.DETAILED -> { label.visibility = View.VISIBLE; time.visibility = View.VISIBLE }
        }
        if (notchMode) {
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.argb((255 * alpha).roundToInt(), 0, 0, 0))
                cornerRadius = (layoutParams.height / 2f)
            }
            post { if (notchMode && width > 0 && height > 0) background = buildNotchBackground(width, height) }
        } else {
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.argb((255 * alpha).roundToInt(), 8, 10, 14))
                cornerRadius = dp(settings.cornerRadiusDp).toFloat()
                setStroke(dp(1), Color.argb(60, 255, 255, 255))
            }
        }
    }

    private fun buildNotchBackground(w: Int, h: Int): android.graphics.drawable.Drawable {
        val alpha = (settings.opacity.coerceIn(20, 100) / 100f)
        val pill = android.graphics.drawable.GradientDrawable().apply {
            setColor(Color.argb((255 * alpha).roundToInt(), 0, 0, 0))
            cornerRadius = (h / 2f)
        }
        val dot = dp(9)
        val ring = dp(13)
        val cx = (w - dot) / 2
        val cy = (h - dot) / 2
        val rx = (w - ring) / 2
        val ry = (h - ring) / 2
        val lens = android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.OVAL
            setColor(Color.argb(255, 10, 12, 16))
        }
        val lensRing = android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.OVAL
            setColor(Color.argb(255, 58, 62, 70))
        }
        return android.graphics.drawable.LayerDrawable(arrayOf(pill, lensRing, lens)).apply {
            setLayerInset(1, rx, ry, rx, ry)
            setLayerInset(2, cx, cy, cx, cy)
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()
}
