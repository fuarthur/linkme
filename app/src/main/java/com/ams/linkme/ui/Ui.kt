package com.ams.linkme.ui

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.InputFilter
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.ams.linkme.BuildConfig
import com.ams.linkme.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

fun Context.dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

fun Context.color(id: Int): Int = ContextCompat.getColor(this, id)

fun Context.column(padding: Int = 0) =
    LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(padding), dp(padding), dp(padding), dp(padding))
    }

fun Context.row() =
    LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }

fun View.space(top: Int = 0, bottom: Int = 0, start: Int = 0, end: Int = 0) {
    val params =
        (layoutParams as? ViewGroup.MarginLayoutParams)
            ?: LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
    params.setMargins(context.dp(start), context.dp(top), context.dp(end), context.dp(bottom))
    layoutParams = params
}

fun Context.label(
    value: String,
    size: Float = 16f,
    bold: Boolean = false,
    secondary: Boolean = false,
) =
    TextView(this).apply {
        text = value
        textSize = size
        setTextColor(color(if (secondary) R.color.on_surface_variant else R.color.on_surface))
        if (bold) typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        setLineSpacing(dp(2).toFloat(), 1f)
    }

fun Context.button(
    value: String,
    outlined: Boolean = false,
    textOnly: Boolean = false,
): MaterialButton {
    val style =
        when {
            textOnly -> com.google.android.material.R.attr.borderlessButtonStyle
            outlined -> com.google.android.material.R.attr.materialButtonOutlinedStyle
            else -> com.google.android.material.R.attr.materialButtonStyle
        }
    return MaterialButton(this, null, style).apply {
        text = value
        isAllCaps = false
        minHeight = dp(52)
        cornerRadius = dp(16)
        insetTop = dp(4)
        insetBottom = dp(4)
        layoutParams =
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
    }
}

fun Context.field(
    title: String,
    idValue: Int,
    max: Int = 0,
    multiline: Boolean = false,
    password: Boolean = false,
    email: Boolean = false,
): Pair<TextInputLayout, TextInputEditText> {
    val box =
        TextInputLayout(this, null, com.google.android.material.R.attr.textInputOutlinedStyle)
            .apply {
                hint = title
                boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
                setBoxCornerRadii(
                    dp(14).toFloat(),
                    dp(14).toFloat(),
                    dp(14).toFloat(),
                    dp(14).toFloat(),
                )
                if (password) endIconMode = TextInputLayout.END_ICON_PASSWORD_TOGGLE
                if (max > 0) {
                    isCounterEnabled = true
                    counterMaxLength = max
                }
            }
    val editor =
        TextInputEditText(box.context).apply {
            id = idValue
            minHeight = dp(56)
            textSize = 16f
            inputType =
                when {
                    password -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                    email ->
                        InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                    multiline ->
                        InputType.TYPE_CLASS_TEXT or
                            InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                            InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                    else -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
                }
            if (max > 0) filters = arrayOf(InputFilter.LengthFilter(max))
            if (multiline) {
                minLines = 3
                maxLines = 5
                gravity = Gravity.TOP
            }
            if (password) isSaveEnabled = false
            if (android.os.Build.VERSION.SDK_INT >= 26) {
                if (password) setAutofillHints(View.AUTOFILL_HINT_PASSWORD)
                else if (email) setAutofillHints(View.AUTOFILL_HINT_EMAIL_ADDRESS)
                else importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
            }
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
    box.addView(
        editor,
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        ),
    )
    return box to editor
}

fun Context.avatar(name: String, size: Int = 52) =
    label(
            name
                .trim()
                .split(Regex("\\s+"))
                .filter { it.isNotBlank() }
                .take(2)
                .joinToString("") { it.take(1).uppercase() }
                .ifBlank { "L" },
            19f,
            true,
        )
        .apply {
            gravity = Gravity.CENTER
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            setTextColor(color(R.color.on_primary_container))
            background =
                GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(color(R.color.primary_container))
                }
            layoutParams = LinearLayout.LayoutParams(dp(size), dp(size))
        }

fun Context.spinner() =
    ProgressBar(this).apply {
        layoutParams =
            LinearLayout.LayoutParams(dp(36), dp(36)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                topMargin = dp(24)
                bottomMargin = dp(24)
            }
    }

fun Context.previewBanner(): TextView =
    label("Local preview · sample data", 12f, secondary = true).apply {
        visibility = if (BuildConfig.DEBUG && BuildConfig.USE_EMULATORS) View.VISIBLE else View.GONE
        gravity = Gravity.CENTER
        setPadding(dp(12), dp(4), dp(12), dp(4))
        setBackgroundColor(color(R.color.primary_container))
        setTextColor(color(R.color.on_primary_container))
    }

fun Context.scrollContent(content: View): ScrollView =
    ScrollView(this).apply {
        isFillViewport = true
        clipToPadding = false
        addView(
            content,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ),
        )
    }

fun AppCompatActivity.mount(root: View, ime: Boolean = false) {
    WindowCompat.setDecorFitsSystemWindows(window, false)
    setContentView(root)
    ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
        val bars =
            insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
        val keyboard = if (ime) insets.getInsets(WindowInsetsCompat.Type.ime()).bottom else 0
        view.setPadding(bars.left, bars.top, bars.right, maxOf(bars.bottom, keyboard))
        WindowInsetsCompat.CONSUMED
    }
    val night =
        resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK ==
            android.content.res.Configuration.UI_MODE_NIGHT_YES
    WindowCompat.getInsetsController(window, root).isAppearanceLightStatusBars = !night
    WindowCompat.getInsetsController(window, root).isAppearanceLightNavigationBars = !night
    root.setBackgroundColor(color(R.color.surface))
    ViewCompat.requestApplyInsets(root)
}

class ContentFrame(context: Context) : FrameLayout(context) {
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val available = MeasureSpec.getSize(widthMeasureSpec)
        val width = minOf(available, context.dp(720))
        super.onMeasure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), heightMeasureSpec)
    }
}
