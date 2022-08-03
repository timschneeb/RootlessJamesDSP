package me.timschneeberger.rootlessjamesdsp.activity

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import me.timschneeberger.rootlessjamesdsp.R
import me.timschneeberger.rootlessjamesdsp.databinding.ActivityBlocklistBinding
import me.timschneeberger.rootlessjamesdsp.fragment.BlocklistFragment

class BlocklistActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBlocklistBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBlocklistBinding.inflate(layoutInflater)

        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.blocklist_host, BlocklistFragment.newInstance())
                .commit()
        }

        binding.fab.setOnClickListener {
            (supportFragmentManager.findFragmentById(R.id.blocklist_host) as BlocklistFragment).showAppSelector()
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { onBackPressed() }
    }

    companion object {
        const val TAG = "BlocklistActivity"
    }
}