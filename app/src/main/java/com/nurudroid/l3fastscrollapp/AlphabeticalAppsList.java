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
package com.nurudroid.l3fastscrollapp;

import android.content.Context;

import com.nurudroid.l3fastscrollapp.compat.AlphabeticIndexCompat;
import com.nurudroid.l3fastscrollapp.data.AppInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

/**
 * The alphabetically sorted list of applications.
 */
public class AlphabeticalAppsList {

    public static final String TAG = "AlphabeticalAppsList";

    private static final int FAST_SCROLL_FRACTION_DISTRIBUTE_BY_ROWS_FRACTION = 0;
    private static final int FAST_SCROLL_FRACTION_DISTRIBUTE_BY_NUM_SECTIONS = 1;

    private final int mFastScrollDistributionMode = FAST_SCROLL_FRACTION_DISTRIBUTE_BY_NUM_SECTIONS;

    /**
     * Info about a fast scroller section, depending if sections are merged, the fast scroller
     * sections will not be the same set as the section headers.
     */
    public static class FastScrollSectionInfo {
        // The section name
        public String sectionName;
        // The AdapterItem to scroll to for this section
        public AdapterItem fastScrollToItem;
        // The touch fraction that should map to this fast scroll section info
        public float touchFraction;

        public FastScrollSectionInfo(String sectionName) {
            this.sectionName = sectionName;
        }
    }

    /**
     * Info about a particular adapter item (can be either section or app)
     */
    public static class AdapterItem {
        /**
         * Common properties
         */
        // The index of this adapter item in the list
        public int position;
        // The type of this item
        public int viewType;

        /**
         * App-only properties
         */
        // The section name of this app.  Note that there can be multiple items with different
        // sectionNames in the same section
        public String sectionName = null;
        // The row that this item shows up on
        public int rowIndex;
        // The index of this app in the row
        public int rowAppIndex;
        // The associated AppInfo for the app
        public AppInfo appInfo = null;
        // The index of this app not including sections
        public int appIndex = -1;

        public static AdapterItem asApp(int pos, String sectionName, AppInfo appInfo,
                                        int appIndex) {
            AdapterItem item = new AdapterItem();
            item.viewType = AllAppsGridAdapter.VIEW_TYPE_ICON;
            item.position = pos;
            item.sectionName = sectionName;
            item.appInfo = appInfo;
            item.appIndex = appIndex;
            return item;
        }
    }

    // The set of apps from the system not including predictions
    private final List<AppInfo> mApps = new ArrayList<>();

    // The current set of adapter items
    private final ArrayList<AdapterItem> mAdapterItems = new ArrayList<>();
    // The set of sections that we allow fast-scrolling to (includes non-merged sections)
    private final List<FastScrollSectionInfo> mFastScrollerSections = new ArrayList<>();

    private final HashMap<CharSequence, String> mCachedSectionNames = new HashMap<>();
    private AllAppsGridAdapter mAdapter;
    private final AlphabeticIndexCompat mIndexer;
    private int mNumAppsPerRow;
    private int mNumAppRowsInAdapter;

    public AlphabeticalAppsList(Context context) {
        mIndexer = new AlphabeticIndexCompat(context);
    }

    /**
     * Sets the number of apps per row.
     */
    public void setNumAppsPerRow(int numAppsPerRow) {
        mNumAppsPerRow = numAppsPerRow;
        updateAdapterItems();
    }

    /**
     * Sets the adapter to notify when this dataset changes.
     */
    public void setAdapter(AllAppsGridAdapter adapter) {
        mAdapter = adapter;
    }

    /**
     * Returns all the apps.
     */
    public List<AppInfo> getApps() {
        return mApps;
    }

    /**
     * Returns fast scroller sections of all the current filtered applications.
     */
    public List<FastScrollSectionInfo> getFastScrollerSections() {
        return mFastScrollerSections;
    }

    /**
     * Returns the current filtered list of applications broken down into their sections.
     */
    public List<AdapterItem> getAdapterItems() {
        return mAdapterItems;
    }

    /**
     * Returns the number of rows of applications (not including predictions)
     */
    public int getNumAppRows() {
        return mNumAppRowsInAdapter;
    }

    /**
     * Sets the current set of apps.
     */
    public void setApps(List<AppInfo> apps) {
        addOrUpdateApps(apps);
    }

    /**
     * Adds or updates existing apps in the list
     */
    public void addOrUpdateApps(List<AppInfo> apps) {
        mApps.clear();
        mApps.addAll(apps);
        onAppsUpdated();
    }

    /**
     * Removes some apps from the list.
     */
    public void removeApps(List<AppInfo> apps) {
        for (AppInfo app : apps) {
            mApps.remove(app);
        }
        onAppsUpdated();
    }

    /**
     * Updates internals when the set of apps are updated.
     */
    private void onAppsUpdated() {
        // Sort the list of apps
        mApps.sort(Comparator.comparingInt(t -> t.getTitle().charAt(0)));

        // Just compute the section headers for use below
        for (AppInfo info : mApps) {
            // Add the section to the cache
            getAndUpdateCachedSectionName(info.getTitle());
        }
        // Recompose the set of adapter items from the current set of apps
        updateAdapterItems();
    }

    /**
     * Updates the set of filtered apps with the current filter.  At this point, we expect
     * mCachedSectionNames to have been calculated for the set of all apps in mApps.
     */
    private void updateAdapterItems() {
        refillAdapterItems();
        refreshRecyclerView();
    }

    private void refreshRecyclerView() {
        if (mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        }
    }

    private void refillAdapterItems() {
        String lastSectionName = null;
        FastScrollSectionInfo lastFastScrollerSectionInfo = null;
        int position = 0;
        int appIndex = 0;

        // Prepare to update the list of sections, filtered apps, etc.
        mFastScrollerSections.clear();
        mAdapterItems.clear();

        // Recreate the filtered and sectioned apps (for convenience for the grid layout) from the
        // ordered set of sections
        for (AppInfo info : mApps) {
            String sectionName = getAndUpdateCachedSectionName(info.getTitle());

            // Create a new section if the section names do not match
            if (!sectionName.equals(lastSectionName)) {
                lastSectionName = sectionName;
                lastFastScrollerSectionInfo = new FastScrollSectionInfo(sectionName);
                mFastScrollerSections.add(lastFastScrollerSectionInfo);
            }

            // Create an app item
            AdapterItem appItem = AdapterItem.asApp(position++, sectionName, info, appIndex++);
            if (lastFastScrollerSectionInfo.fastScrollToItem == null) {
                lastFastScrollerSectionInfo.fastScrollToItem = appItem;
            }
            mAdapterItems.add(appItem);
        }

        if (mNumAppsPerRow != 0) {
            // Update the number of rows in the adapter after we do all the merging (otherwise, we
            // would have to shift the values again)
            int numAppsInSection = 0;
            int numAppsInRow = 0;
            int rowIndex = -1;
            for (AdapterItem item : mAdapterItems) {
                item.rowIndex = 0;
//                if (AllAppsGridAdapter.isHeaderViewType(item.viewType)) {
//                    numAppsInSection = 0;
//                } else if (AllAppsGridAdapter.isIconViewType(item.viewType)) {
                if (numAppsInSection % mNumAppsPerRow == 0) {
                    numAppsInRow = 0;
                    rowIndex++;
                }
                item.rowIndex = rowIndex;
                item.rowAppIndex = numAppsInRow;
                numAppsInSection++;
                numAppsInRow++;
            }
//            }
            mNumAppRowsInAdapter = rowIndex + 1;

            // Pre-calculate all the fast scroller fractions
            float rowFraction = 1f / mNumAppRowsInAdapter;
            for (FastScrollSectionInfo info : mFastScrollerSections) {
                AdapterItem item = info.fastScrollToItem;
                float subRowFraction = item.rowAppIndex * (rowFraction / mNumAppsPerRow);
                info.touchFraction = item.rowIndex * rowFraction + subRowFraction;
            }

//            switch (mFastScrollDistributionMode) {
//                case FAST_SCROLL_FRACTION_DISTRIBUTE_BY_ROWS_FRACTION:
//                    float rowFraction = 1f / mNumAppRowsInAdapter;
//                    for (FastScrollSectionInfo info : mFastScrollerSections) {
//                        AdapterItem item = info.fastScrollToItem;
//                        float subRowFraction = item.rowAppIndex * (rowFraction / mNumAppsPerRow);
//                        info.touchFraction = item.rowIndex * rowFraction + subRowFraction;
//                    }
//                    break;
//                case FAST_SCROLL_FRACTION_DISTRIBUTE_BY_NUM_SECTIONS:
//                    float perSectionTouchFraction = 1f / mFastScrollerSections.size();
//                    float cumulativeTouchFraction = 0f;
//                    for (FastScrollSectionInfo info : mFastScrollerSections) {
//
//                        info.touchFraction = cumulativeTouchFraction;
//                        cumulativeTouchFraction += perSectionTouchFraction;
//                    }
//                    break;
//            }
        }
    }

    /**
     * Returns the cached section name for the given title, recomputing and updating the cache if
     * the title has no cached section name.
     */
    private String getAndUpdateCachedSectionName(CharSequence title) {
        String sectionName = mCachedSectionNames.get(title);
        if (sectionName == null) {
            sectionName = mIndexer.computeSectionName(title);
            mCachedSectionNames.put(title, sectionName);
        }
        return sectionName;
    }

}
