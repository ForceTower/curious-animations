package dev.forcetower.music.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.animation.PathInterpolator
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import dev.forcetower.music.R
import dev.forcetower.music.model.LyricLine

class LyricsLineSimpleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : ConstraintLayout(context, attrs, defStyle) {
    var active = false
    private val mainText: TextView

    private val scaleHighlight: Float = resources.getFloat(R.dimen.lyrics_line_scale_highlight)
    private val scaleNormal: Float = resources.getFloat(R.dimen.lyrics_line_scale_normal)
    private val activeAlpha: Float = resources.getFloat(R.dimen.player_vibrant_primary_alpha)
    private val inactiveAlpha: Float = resources.getFloat(R.dimen.player_vibrant_tertiary_alpha)

    private var currentAnimator: AnimatorSet? = null

    init {
        // We ARE the binding root — Apple's `L9.T` is this ConstraintLayout
        // (classes2_decomp.c:1279780-1279782).
        LayoutInflater.from(context).inflate(R.layout.lyrics_line, this, true)
        mainText = findViewById(R.id.song_lyrics_line)
        clipChildren = false
        clipToPadding = false
    }

    /** Set the main line text from a [dev.forcetower.music.model.LyricLine]. */
    fun setLine(line: LyricLine) {
        mainText.text = line.text()
        mainText.textAlignment = TEXT_ALIGNMENT_TEXT_START
    }

    fun setActiveState(active: Boolean) {
        this.active = active
        cancelAnimator()
        val alpha = if (active) activeAlpha else inactiveAlpha
        val scale = if (active) scaleHighlight else scaleNormal
        mainText.alpha = alpha
        mainText.scaleX = scale
        mainText.scaleY = scale
    }

    fun runActivate() {
        active = true
        endPreviousAnimator()
        val alpha = ObjectAnimator.ofFloat(mainText, ALPHA, inactiveAlpha, activeAlpha).apply {
            startDelay = ACT_ALPHA_DELAY_MS
            duration = ACT_ALPHA_DURATION_MS
            interpolator = ALPHA_INTERPOLATOR
        }
        val scale = makeActScaleAnim(mainText)
        val set = AnimatorSet().apply { playTogether(alpha, scale) }
        attachClearListener(set)
        currentAnimator = set
        set.start()
    }

    fun runDeactivate() {
        active = false
        endPreviousAnimator()
        val alpha = ObjectAnimator.ofFloat(mainText, ALPHA, activeAlpha, inactiveAlpha).apply {
            startDelay = DEACT_ALPHA_DELAY_MS
            duration = DEACT_ALPHA_DURATION_MS
            interpolator = ALPHA_INTERPOLATOR
        }
        val scale = makeDeactScaleAnim(mainText)
        val set = AnimatorSet().apply { playTogether(alpha, scale) }
        attachClearListener(set)
        currentAnimator = set
        set.start()
    }

    private fun makeActScaleAnim(tv: TextView): ObjectAnimator {
        val sx = PropertyValuesHolder.ofFloat(SCALE_X, scaleNormal, scaleHighlight)
        val sy = PropertyValuesHolder.ofFloat(SCALE_Y, scaleNormal, scaleHighlight)
        return ObjectAnimator.ofPropertyValuesHolder(tv, sx, sy).apply {
            startDelay = ACT_SCALE_DELAY_MS
            duration = ACT_SCALE_DURATION_MS
            interpolator = SCALE_INTERPOLATOR
        }
    }

    private fun makeDeactScaleAnim(tv: TextView): ObjectAnimator {
        val sx = PropertyValuesHolder.ofFloat(SCALE_X, scaleHighlight, scaleNormal)
        val sy = PropertyValuesHolder.ofFloat(SCALE_Y, scaleHighlight, scaleNormal)
        return ObjectAnimator.ofPropertyValuesHolder(tv, sx, sy).apply {
            startDelay = DEACT_SCALE_DELAY_MS
            duration = DEACT_SCALE_DURATION_MS
            interpolator = SCALE_INTERPOLATOR
        }
    }

    private fun endPreviousAnimator() {
        currentAnimator?.end()
        currentAnimator = null
    }

    private fun cancelAnimator() {
        currentAnimator?.cancel()
        currentAnimator = null
    }

    private fun attachClearListener(set: AnimatorSet) {
        set.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                if (currentAnimator === set) currentAnimator = null
            }
        })
    }

    companion object {
        private val SCALE_INTERPOLATOR = PathInterpolator(0.4f, 0.1f, 0f, 1f)
        private val ALPHA_INTERPOLATOR = PathInterpolator(0.39f, 0.575f, 0.565f, 1f)

        private const val ACT_ALPHA_DELAY_MS = 250L
        private const val ACT_ALPHA_DURATION_MS = 250L

        private const val DEACT_ALPHA_DELAY_MS = 250L
        private const val DEACT_ALPHA_DURATION_MS = 350L

        private const val ACT_SCALE_DELAY_MS = 150L
        private const val ACT_SCALE_DURATION_MS = 500L

        private const val DEACT_SCALE_DELAY_MS = 50L
        private const val DEACT_SCALE_DURATION_MS = 350L
    }
}