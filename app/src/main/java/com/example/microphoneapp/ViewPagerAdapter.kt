package com.example.microphoneapp

import android.media.AudioManager
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class ViewPagerAdapter(fragmentActivity: FragmentActivity) :
    FragmentStateAdapter(fragmentActivity) {

    private val fragments = listOf(
        InfoFragment(AudioManager.GET_DEVICES_INPUTS, "聲音輸入裝置"),
        InfoFragment(AudioManager.GET_DEVICES_OUTPUTS, "聲音輸出裝置"),
    )

    override fun getItemCount(): Int = fragments.size

    override fun createFragment(position: Int): Fragment = fragments[position]

    fun getFragment(tab: Int) = fragments[tab]

    fun getFragmentTitle(position: Int) = fragments[position].title
}