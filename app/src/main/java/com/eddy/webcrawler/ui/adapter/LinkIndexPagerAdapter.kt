package com.eddy.webcrawler.ui.adapter

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.eddy.webcrawler.ui.fragment.IndexPageFragment

class LinkIndexPagerAdapter(
    fragment: Fragment
) : FragmentStateAdapter(fragment) {

    private var indexIds: List<Long> = emptyList()

    fun updateIndices(newIndices: List<Long>) {
        if (indexIds != newIndices) {
            indexIds = newIndices
            notifyDataSetChanged()
        }
    }

    override fun getItemCount(): Int = indexIds.size

    override fun createFragment(position: Int): Fragment {
        return IndexPageFragment.newInstance(indexIds[position])
    }

    override fun getItemId(position: Int): Long {
        return indexIds[position]
    }

    override fun containsItem(itemId: Long): Boolean {
        return indexIds.contains(itemId)
    }
}
