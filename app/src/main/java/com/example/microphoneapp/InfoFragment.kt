package com.example.microphoneapp

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity.AUDIO_SERVICE
import androidx.fragment.app.Fragment
import com.example.microphoneapp.data.DeviceInfo
import com.example.microphoneapp.databinding.FragmentInfoBinding

class InfoFragment(private val deviceFlag: Int, val title: String) : Fragment() {

    private lateinit var binding: FragmentInfoBinding

    var audioDevices: MutableList<AudioDeviceInfo> = mutableListOf()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        audioDevices.ifEmpty {
            val audioManager = context.getSystemService(AUDIO_SERVICE) as AudioManager
            audioDevices.addAll(audioManager.getDevices(deviceFlag))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.tvInfo.text = scanChannel()
    }

    private fun scanChannel(): String {
        val info = audioDevices.mapIndexed { index, audioDeviceInfo ->
            DeviceInfo(
                deviceInfo = audioDeviceInfo,
                isLast = index == audioDevices.size - 1
            ).print()
        }.joinToString(separator = "")
        return info
    }
}