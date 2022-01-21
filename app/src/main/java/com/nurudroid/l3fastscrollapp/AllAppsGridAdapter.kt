/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.nurudroid.l3fastscrollapp

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * The grid view adapter of all the apps.
 */
class AllAppsGridAdapter(
    val context: Context,
    private val mApps: AlphabeticalAppsList
) : RecyclerView.Adapter<AllAppsGridAdapter.ViewHolder>() {

    interface BindViewCallback {
        fun onBindView(holder: ViewHolder?)
    }

    /**
     * ViewHolder for each icon.
     */
    class ViewHolder(v: View?) : RecyclerView.ViewHolder(v!!)

    private var mBindViewCallback: BindViewCallback? = null

    /**
     * Sets the callback for when views are bound.
     */
    fun setBindViewCallback(cb: BindViewCallback?) {
        mBindViewCallback = cb
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return when (viewType) {
            VIEW_TYPE_ICON -> {
                val icon = LayoutInflater.from(context).inflate(R.layout.item_box, parent, false)
                ViewHolder(icon)
            }
            else -> throw RuntimeException("Unexpected view type")
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when (holder.itemViewType) {
            VIEW_TYPE_ICON -> {
                val info = mApps.adapterItems[position].appInfo
                (holder.itemView as TextView).text = info.title
            }
        }
        mBindViewCallback?.onBindView(holder)
    }

    override fun onFailedToRecycleView(holder: ViewHolder): Boolean {
        // Always recycle and we will reset the view when it is bound
        return true
    }

    override fun getItemCount(): Int {
        return mApps.adapterItems.size
    }

    override fun getItemViewType(position: Int): Int {
        val item = mApps.adapterItems[position]
        return item.viewType
    }

    companion object {
        const val TAG = "AppsGridAdapter"

        // A normal icon
        const val VIEW_TYPE_ICON = 1 shl 1

        fun isViewType(viewType: Int, viewTypeMask: Int): Boolean {
            return viewType and viewTypeMask != 0
        }
    }
}