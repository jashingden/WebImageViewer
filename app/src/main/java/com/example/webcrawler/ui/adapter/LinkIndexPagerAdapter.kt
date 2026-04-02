package com.example.webcrawler.ui.adapter

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.webcrawler.ui.fragment.IndexPageFragment

class LinkIndexPagerAdapter(
    fragment: Fragment,
    private val indexIds: List<Long>
) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = indexIds.size

    override fun createFragment(position: Int): Fragment {
        return IndexPageFragment.newInstance(indexIds[position])
    }
}
