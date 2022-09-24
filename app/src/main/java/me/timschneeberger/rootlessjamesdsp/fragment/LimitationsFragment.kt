package me.timschneeberger.rootlessjamesdsp.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import me.timschneeberger.rootlessjamesdsp.databinding.OnboardingPage2Binding
import me.timschneeberger.rootlessjamesdsp.utils.ContextExtensions.dpToPx

class LimitationsFragment : Fragment() {
    private lateinit var binding: OnboardingPage2Binding

    override fun onCreateView(
        layoutInflater: LayoutInflater,
        viewGroup: ViewGroup?,
        bundle: Bundle?
    ): View {
        val padding = requireContext().dpToPx(16)
        binding = OnboardingPage2Binding.inflate(layoutInflater, viewGroup, false)
        binding.root.setPadding(padding, 0, padding, padding)
        binding.header.isVisible = false
        binding.notice.isVisible = false
        return binding.root
    }
}