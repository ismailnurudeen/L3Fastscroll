package com.nurudroid.l3fastscrollapp

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.nurudroid.l3fastscrollapp.data.AppInfo
import kotlin.random.Random

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val rvItems = AlphabeticalAppsList(this)
        rvItems.setNumAppsPerRow(4)
        val items = (1..100).map { num ->
            val alphabet = ('A'..'Z').map { it.toString() }
            AppInfo(title = "${alphabet[Random.nextInt(alphabet.size)]}-Box $num")
        }
        rvItems.apps = items
        val mAdapter = AllAppsGridAdapter(this, rvItems)
        rvItems.setAdapter(mAdapter)

       val mLayoutManager = AppsGridLayoutManager(this,4)

        // Load the all apps recycler view
        val mainRv = findViewById<AllAppsRecyclerView>(R.id.main_rv)
        mainRv.apply {
            apps = rvItems
            setNumAppsPerRow(4)
            layoutManager = mLayoutManager
            adapter = mAdapter
            setHasFixedSize(true)

            // No animations will occur when changes occur to the items in this RecyclerView.
            itemAnimator = null
        }
    }
}

/**
 * A subclass of GridLayoutManager
 */
class AppsGridLayoutManager(context: Context?, span: Int = 1) :
    GridLayoutManager(context, span, VERTICAL, false)
