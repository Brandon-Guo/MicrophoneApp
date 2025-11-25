package com.example.microphoneapp

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.example.microphoneapp.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.launch


class MainActivity : AppCompatActivity() {

//    private val audioDevices by lazy {
//        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
//        /**
//         * GET_DEVICES_OUTPUTS : 喇叭、耳機
//         * GET_DEVICES_INPUTS :  麥克風
//         */
////        audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
//        audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)
//    }

    private val audioRecorder by lazy {
        AudioRecorder(context = this@MainActivity)
    }

    private var spinnerInputSelectPosition = 0
    private var spinnerOutputSelectPosition = 0

//    private var recordInputCheckId = -1
//    private var recordOutputCheckId = -1

    private lateinit var binding: ActivityMainBinding


    private val deviceList = mutableListOf<AudioDeviceInfo>()
    private val deviceNames = mutableListOf<String>()
    private val deviceSpinnerAdapter by lazy {
        ArrayAdapter(
            this, // Context
            android.R.layout.simple_spinner_item, // 下拉顯示樣式
            deviceNames
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
    }

    private val rateList = mutableListOf<Int>()
    private val rateNames = mutableListOf<String>()
    private val rateSpinnerAdapter by lazy {
        ArrayAdapter(
            this, // Context
            android.R.layout.simple_spinner_item, // 下拉顯示樣式
            rateNames
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

    }

    private val channelIndexMaskList = mutableListOf<Int>()
    private val channelIndexMaskNames = mutableListOf<String>()
    private val channelIndexMaskSpinnerAdapter by lazy {
        ArrayAdapter(
            this, // Context
            android.R.layout.simple_spinner_item, // 下拉顯示樣式
            channelIndexMaskNames
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
    }

    private val channelMaskList = mutableListOf<Int>()
    private val channelMaskNames = mutableListOf<String>()
    private val channelMaskSpinnerAdapter by lazy {
        ArrayAdapter(
            this, // Context
            android.R.layout.simple_spinner_item, // 下拉顯示樣式
            channelMaskNames
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

//        val deviceList = mutableListOf<AudioDeviceInfo>()
//        val deviceNames = mutableListOf<String>()
//        val spinnerAdapter = ArrayAdapter(
//            this, // Context
//            android.R.layout.simple_spinner_item, // 下拉顯示樣式
//            deviceNames
//        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        val viewPagerAdapter = ViewPagerAdapter(this@MainActivity)
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {

            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)

                deviceList.clear()
                deviceNames.clear()
                viewPagerAdapter.getFragment(position).audioDevices.apply {
                    deviceList.addAll(this)
                    deviceNames.addAll(this.map { "Id: ${it.id}/ Address: (${it.address})" })
                    deviceSpinnerAdapter.notifyDataSetChanged()
                }

                if (position == 0) {
                    binding.spinnerOptions.setSelection(spinnerInputSelectPosition)

//                    binding.radio1Channel.isEnabled = true
//                    binding.radio2Channel.isEnabled = true
//                    binding.radio6Channel.isEnabled = true
//                    binding.radio8Channel.isEnabled = true
//                    binding.selectGroup.check(recordInputCheckId)
                } else if (position == 1) {

                    binding.spinnerOptions.setSelection(spinnerOutputSelectPosition)

//                    binding.radio1Channel.isEnabled = false
//                    binding.radio2Channel.isEnabled = false
//                    binding.radio6Channel.isEnabled = false
//                    binding.radio8Channel.isEnabled = false
//                    binding.selectGroup.check(recordOutputCheckId)
                }
                binding.btnRecordAudioStart.isEnabled = deviceList.isNotEmpty()
            }
        })

        TabLayoutMediator(binding.tabLayout, binding.viewPager.apply {
            adapter = viewPagerAdapter
        }) { tab, position ->
            tab.text = viewPagerAdapter.getFragmentTitle(position)
        }.attach()


//        binding.selectGroup.setOnCheckedChangeListener { _, checkedId ->
//            selectedMask = when (checkedId) {
//                R.id.radio_1_channel -> 1
//
//                R.id.radio_2_channel -> 2
//
//                R.id.radio_6_channel -> 3
//
//                R.id.radio_8_channel -> 4
//
//                else -> 1
//            }
//
//
//            if (binding.tabLayout.selectedTabPosition == 0) {
//                recordInputCheckId = checkedId
//            } else if (binding.tabLayout.selectedTabPosition == 1) {
//                recordOutputCheckId = checkedId
//            }
//
//            binding.btnRecordAudioStart.isEnabled = (checkedId != -1)
//        }
//        binding.selectGroup.check(R.id.radio_8_channel)

        binding.btnRecordAudioStart.setOnClickListener { view ->
            checkPermissions(onWork = {
                Log.i("AudioRecorder", "點擊按鈕並且權限允許")
                deviceList[binding.spinnerOptions.selectedItemPosition].let {

                    val sampleRate: Int = rateList[binding.spinnerRate.selectedItemPosition]
                    val channelMask: Int =
                        channelMaskList[binding.spinnerChannelMask.selectedItemPosition]
                    val channelIndexMask: Int =
                        channelIndexMaskList[binding.spinnerChannelIndexMask.selectedItemPosition]

                    Log.i(
                        "AudioRecorder",
                        "執行前選項資訊：SampleRate[$sampleRate] / ChannelIndexMask[$channelIndexMask] / ChannelMask[$channelMask]"
                    )
                    audioRecorder.startRecording(
                        this@MainActivity.lifecycleScope,
                        selectSampleRate = sampleRate,
                        selectChannelIndexMask = channelIndexMask,
                        selectChannelMask = channelMask,
                        onWork = { isSuccess ->
                            if (isSuccess) {
                                view.isEnabled = !view.isEnabled
                                binding.btnRecordAudioStop.isEnabled = true

//                                binding.radio1Channel.isEnabled = false
//                                binding.radio2Channel.isEnabled = false
//                                binding.radio6Channel.isEnabled = false
//                                binding.radio8Channel.isEnabled = false
                            }
                        },
                        onMessage = { msg, always ->
                            warningSnackbar(msg, always)
                        })
                }
            })
        }

        binding.btnRecordAudioStop.setOnClickListener {
            lifecycleScope.launch {
                audioRecorder.stopRecording()
            }

            it.isEnabled = !it.isEnabled
            binding.btnRecordAudioStart.isEnabled = true

//            binding.radio1Channel.isEnabled = true
//            binding.radio2Channel.isEnabled = true
//            binding.radio6Channel.isEnabled = true
//            binding.radio8Channel.isEnabled = true
        }


        binding.spinnerOptions.adapter = deviceSpinnerAdapter
        binding.spinnerOptions.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?, view: View?, position: Int, id: Long
                ) {

                    if (binding.tabLayout.selectedTabPosition == 0) {
                        spinnerInputSelectPosition = position
                    } else if (binding.tabLayout.selectedTabPosition == 1) {
                        spinnerOutputSelectPosition = position
                    }

                    deviceList[position].let { audioDeviceInfo ->
                        rateList.clear()
                        rateList.addAll(audioDeviceInfo.sampleRates.toList())
                        rateNames.clear()
                        rateNames.addAll(audioDeviceInfo.sampleRates.map { "Rate: $it" })
                        rateSpinnerAdapter.notifyDataSetChanged()
                        binding.spinnerRate.setSelection(
                            audioDeviceInfo.sampleRates.toList().indexOf(16000)
                        )

                        channelMaskList.clear()
                        channelMaskList.addAll(mutableListOf<Int>().apply {
                            add(-1)
                            addAll(audioDeviceInfo.channelMasks.toList())
                        })
                        channelMaskNames.clear()
                        channelMaskNames.addAll(mutableListOf<String>().apply {
                            addAll(audioDeviceInfo.channelMasks.apply {
                                sort()
                            }.map {
                                when (it) {
                                    AudioFormat.CHANNEL_IN_MONO -> "MONO"
                                    AudioFormat.CHANNEL_IN_STEREO -> "STEREO"
                                    else -> "未知代號($it)"
                                }
                            })

                            if (isEmpty()) {
                                add(0, "無資料")
                            } else {
                                add(0, "不選擇")
                            }
                        })
                        channelMaskSpinnerAdapter.notifyDataSetChanged()

                        channelIndexMaskList.clear()
                        channelIndexMaskList.addAll(mutableListOf<Int>().apply {
                            addAll(audioDeviceInfo.channelIndexMasks.toList())
                            if (isEmpty()) {
                                add(0, -1)
                            }
                        })
                        channelIndexMaskNames.clear()
                        channelIndexMaskNames.addAll(mutableListOf<String>().apply {
                            addAll(audioDeviceInfo.channelIndexMasks.map { "$it" })

                            if (isEmpty()) {
                                add(0, "無資料")
                            }
                        })
                        channelIndexMaskSpinnerAdapter.notifyDataSetChanged()
                        binding.spinnerChannelIndexMask.setSelection(
                            audioDeviceInfo.channelIndexMasks.indexOf(
                                0xFF
                            )
                        )
                    }
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {

                }
            }

        binding.spinnerRate.adapter = rateSpinnerAdapter
        binding.spinnerChannelIndexMask.adapter = channelIndexMaskSpinnerAdapter
        binding.spinnerChannelMask.adapter = channelMaskSpinnerAdapter
    }

    private fun warningSnackbar(message: String, isAlways: Boolean) {
        val duration = if (isAlways) Snackbar.LENGTH_INDEFINITE else Snackbar.LENGTH_SHORT
        Snackbar.make(
            findViewById(android.R.id.content), message, duration
        ).show()
    }

    private fun checkPermissions(onWork: () -> Unit) {
        val neededPermissions = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) neededPermissions.add(Manifest.permission.RECORD_AUDIO)

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) neededPermissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)

        if (neededPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, neededPermissions.toTypedArray(), 100)
        } else {
            onWork()
        }
    }
}