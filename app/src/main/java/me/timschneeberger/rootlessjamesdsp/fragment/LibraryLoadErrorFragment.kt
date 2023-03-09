package me.timschneeberger.rootlessjamesdsp.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import me.timschneeberger.rootlessjamesdsp.databinding.FragmentLibraryLoadErrorBinding
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ContextExtensions.openPlayStoreApp

class LibraryLoadErrorFragment : Fragment() {
    private lateinit var binding: FragmentLibraryLoadErrorBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentLibraryLoadErrorBinding.inflate(layoutInflater, container, false)
        binding.rootlessNotice.setOnClickListener {
            requireContext().openPlayStoreApp("me.timschneeberger.rootlessjamesdsp")
        }
        return binding.root
    }

    companion object {
        fun newInstance(): LibraryLoadErrorFragment {
            return LibraryLoadErrorFragment()
        }
    }
}