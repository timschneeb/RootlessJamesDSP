package me.timschneeberger.rootlessjamesdsp.preference
import android.content.Context
import android.util.AttributeSet
import androidx.preference.SwitchPreferenceCompat
import me.timschneeberger.rootlessjamesdsp.R


class MaterialSwitchPreference : SwitchPreferenceCompat {
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init()
    }

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context) : super(context) {
        init()
    }

    private fun init() {
        widgetLayoutResource = R.layout.preference_materialswitch
    }
}