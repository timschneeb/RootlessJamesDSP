package me.timschneeberger.rootlessjamesdsp.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.google.android.material.transition.MaterialSharedAxis
import me.timschneeberger.rootlessjamesdsp.databinding.OnboardingPage2Binding
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ContextExtensions.dpToPx

class LimitationsFragment : Fragment() {
    private lateinit var binding: OnboardingPage2Binding

    override fun onCreate(savedInstanceState: Bundle?) {
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.X, true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.X, false)
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        layoutInflater: LayoutInflater,
        viewGroup: ViewGroup?,
        bundle: Bundle?
    ): View {
        val padding = 16.dpToPx
        binding = OnboardingPage2Binding.inflate(layoutInflater, viewGroup, false)
        binding.root.setPadding(padding, 0, padding, padding)
        binding.header.isVisible = false
        binding.notice.isVisible = false
        return binding.root
    }
}