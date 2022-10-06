package me.timschneeberger.rootlessjamesdsp.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import me.timschneeberger.rootlessjamesdsp.R
import me.timschneeberger.rootlessjamesdsp.databinding.ActivityGraphicEqBinding
import me.timschneeberger.rootlessjamesdsp.fragment.GraphicEqualizerFragment

class GraphicEqualizerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityGraphicEqBinding.inflate(layoutInflater)

        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.params, GraphicEqualizerFragment.newInstance())
                .commit()
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { onBackPressed() }
    }
}