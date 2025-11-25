package com.example.microphoneapp

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.media.AudioFormat
import android.media.AudioFormat.CHANNEL_IN_BACK
import android.media.AudioFormat.CHANNEL_IN_BACK_PROCESSED
import android.media.AudioFormat.CHANNEL_IN_FRONT
import android.media.AudioFormat.CHANNEL_IN_FRONT_PROCESSED
import android.media.AudioFormat.CHANNEL_IN_LEFT
import android.media.AudioFormat.CHANNEL_IN_LEFT_PROCESSED
import android.media.AudioFormat.CHANNEL_IN_RIGHT
import android.media.AudioFormat.CHANNEL_IN_RIGHT_PROCESSED
import android.media.AudioRecord
import android.media.MediaRecorder
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.LifecycleCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.io.File
import java.io.FileDescriptor
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.time.Duration.Companion.seconds

class AudioRecorder(private val context: Context) {

    private var isRecording = false

    private val CHANNEL_IN_7POINT1: Int =
        (CHANNEL_IN_LEFT or CHANNEL_IN_RIGHT or CHANNEL_IN_FRONT or CHANNEL_IN_BACK or
                CHANNEL_IN_LEFT_PROCESSED or CHANNEL_IN_RIGHT_PROCESSED or
                CHANNEL_IN_FRONT_PROCESSED or CHANNEL_IN_BACK_PROCESSED)

    private val CHANNEL_IN_5POINT1: Int = (CHANNEL_IN_LEFT or CHANNEL_IN_RIGHT or
            CHANNEL_IN_FRONT or CHANNEL_IN_BACK or
            CHANNEL_IN_LEFT_PROCESSED or CHANNEL_IN_RIGHT_PROCESSED)


    private var audioRecord: AudioRecord? = null

    private var audioJob: Job? = null

    @SuppressLint("MissingPermission")
    fun startRecording(
        scope: LifecycleCoroutineScope,
        selectSampleRate: Int,
        selectChannelIndexMask: Int,
        selectChannelMask: Int,
        onWork: (isSuccess: Boolean) -> Unit,
        onMessage: (message: String, isAlways: Boolean) -> Unit
    ) {

        val channelMask = when (selectChannelMask) {
            1 -> AudioFormat.CHANNEL_IN_MONO
            2 -> AudioFormat.CHANNEL_IN_STEREO
//            3 -> CHANNEL_IN_5POINT1
//            4 -> CHANNEL_IN_7POINT1
            else -> AudioFormat.CHANNEL_IN_STEREO
        }

//        val sampleRate = 48000
        val encoding = AudioFormat.ENCODING_PCM_16BIT
//        val bufferSize = AudioRecord.getMinBufferSize(
//            sampleRate,
//            channelMask,
//            encoding
//        )

        try {
//            if (bufferSize <= 0) {
//                onWork.invoke(false)
//                Log.i("AudioRecorder", "無效的 buffer size，裝置不支援此配置。")
//                onMessage.invoke("無效的 buffer size，裝置不支援此配置。", false)
//                return
//            }

            val format = AudioFormat.Builder().apply {
                setEncoding(encoding)
                setSampleRate(selectSampleRate)

                if (selectChannelMask != -1) {
                    setChannelMask(selectChannelMask)
                }

                onMessage.invoke("selectIndexMask : $selectChannelIndexMask", false)
                if (selectChannelIndexMask != -1) {
                    setChannelIndexMask(selectChannelIndexMask)
                }
            }.build()

            audioRecord = AudioRecord.Builder()
                .setAudioSource(MediaRecorder.AudioSource.MIC)
                .setAudioFormat(format)
//                .setBufferSizeInBytes(bufferSize)
                .setBufferSizeInBytes(4098 * 8)
                .build()


            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.i("AudioRecorder", "AudioRecord 初始化失敗（不支援此 channel mask）")

                onWork.invoke(false)
                onMessage.invoke("AudioRecord 初始化失敗（不支援此 channel mask）", false)
                return
            }

            audioJob?.cancel()
            audioJob = scope.launch {
                delay(1.5.seconds)
                onWork.invoke(true)
//                writeWavFile(sampleRate, format.channelCount, onMessage)
                writeWavMusic(selectSampleRate, format.channelCount, onMessage)
            }
        } catch (e: Exception) {
            onWork.invoke(false)
            Log.i("AudioRecorder", "錄音發生錯誤: ${e.message}")
            onMessage.invoke("錄音發生錯誤: ${e.message}", false)
        }
    }

    suspend fun stopRecording() {
        isRecording = false
        audioJob?.cancelAndJoin() // 取消協程
    }

    private suspend fun writeWavFile(
        sampleRate: Int,
        channels: Int,
        onMessage: (message: String, isAlways: Boolean) -> Unit
    ) {
        val output = File(context.getExternalFilesDir(null), "record_${channels}ch.wav")
//        val output = File(
//            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
//            "record_${channels}ch.wav"
//        )
        var totalBytes = 0L
        val buffer = ByteArray(4096)
        val header = ByteArray(44)

        withContext(Dispatchers.IO) {
            val fos = FileOutputStream(output)
            fos.write(header)

            try {
                withContext(Dispatchers.Main) {
                    Log.i(
                        "AudioRecorder",
                        "writeWavFile => 開始錄音 (${channels}ch) → ${output.name}"
                    )
                    onMessage.invoke("開始錄音 (${channels}ch) → ${output.name}", false)

                    delay(1.seconds)
                    Log.i("AudioRecorder", "writeWavFile => Recording started...")
                    onMessage.invoke("Recording started...", true)
                }

                isRecording = true
                audioRecord?.startRecording()

                while (isRecording && isActive) {
                    val read =
                        audioRecord?.read(buffer, 0, buffer.size, AudioRecord.READ_NON_BLOCKING)
                            ?: 0

                    if (read > 0) {
                        fos.write(buffer, 0, read)
                        totalBytes += read
                    } else {
                        yield() // 讓協程有機會檢測取消
                        // 或 delay(1) 也可以
                    }
                }

                audioRecord?.stop()
            } finally {
                Log.i(
                    "AudioRecorder",
                    "writeWavFile => 停止錄音 (${channels}ch, ${totalBytes / 1024} KB)\n" + output.absolutePath
                )
                onMessage.invoke(
                    "停止錄音 (${channels}ch, ${totalBytes / 1024} KB)\n" + output.absolutePath,
                    false
                )

                audioRecord?.stop()
                audioRecord?.release()
                audioRecord = null

                // 更新 WAV header
                writeWavHeader(output, sampleRate, channels, totalBytes)
                fos.close()
            }
        }
    }

    private fun writeWavHeader(file: File, sampleRate: Int, channels: Int, totalBytes: Long) {
        val fos = RandomAccessFile(file, "rw")
        val byteRate = sampleRate * channels * 2
        val totalDataLen = totalBytes + 36

        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray())
            putInt(totalDataLen.toInt())
            put("WAVE".toByteArray())
            put("fmt ".toByteArray())
            putInt(16)
            putShort(1)
            putShort(channels.toShort())
            putInt(sampleRate)
            putInt(byteRate)
            putShort((channels * 2).toShort())
            putShort(16)
            put("data".toByteArray())
            putInt(totalBytes.toInt())
        }.array()

        fos.seek(0)
        fos.write(header)
        fos.close()
    }

    private suspend fun writeWavMusic(
        sampleRate: Int,
        channels: Int,
        onMessage: (message: String, isAlways: Boolean) -> Unit
    ) {
        val contentValues = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, "record_${channels}ch.wav")
            put(MediaStore.Audio.Media.MIME_TYPE, "audio/wav")
            put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_MUSIC)
            put(MediaStore.Audio.Media.IS_PENDING, 1) // Android 10+
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, contentValues)
            ?: run {
                Log.i("AudioRecorder", "writeWavMusic => 無法建立檔案 URI")
                onMessage.invoke("無法建立檔案 URI", false)
                return
            }

        var totalBytes = 0L
        val buffer = ByteArray(4096)
        val header = ByteArray(44)

        withContext(Dispatchers.IO) {
            resolver.openFileDescriptor(uri, "w")?.use { pfd ->
                FileOutputStream(pfd.fileDescriptor).use { fos ->
                    fos.write(header) // 先寫 WAV header

                    try {
                        withContext(Dispatchers.Main) {
                            Log.i(
                                "AudioRecorder",
                                "writeWavMusic => 開始錄音 (${channels}ch) → $uri"
                            )
                            onMessage.invoke("開始錄音 (${channels}ch) → $uri", false)

                            delay(1500)

                            Log.i("AudioRecorder", "writeWavMusic => Recording started...")
                            onMessage.invoke("Recording started...", true)
                        }

                        isRecording = true
                        audioRecord?.startRecording()

                        while (isRecording && isActive) {
                            val read = audioRecord?.read(
                                buffer,
                                0,
                                buffer.size,
                                AudioRecord.READ_NON_BLOCKING
                            ) ?: 0
                            if (read > 0) {
                                fos.write(buffer, 0, read)
                                totalBytes += read
                            } else {
                                yield()
                            }
                        }

                        audioRecord?.stop()
                    } finally {
                        Log.i(
                            "AudioRecorder",
                            "writeWavMusic => 錄音完成 (${channels}ch, ${totalBytes / 1024} KB)\n" +
                                    "URI: $uri"
                        )
                        onMessage.invoke(
                            "錄音完成 (${channels}ch, ${totalBytes / 1024} KB)\nURI: $uri",
                            false
                        )

                        audioRecord?.stop()
                        audioRecord?.release()
                        audioRecord = null

                        writeWavHeader(pfd.fileDescriptor, sampleRate, channels, totalBytes)

                        // 更新 MediaStore 狀態
                        contentValues.clear()
                        contentValues.put(MediaStore.Audio.Media.IS_PENDING, 0)
                        resolver.update(uri, contentValues, null, null)
                    }
                }
            }
        }
    }

    private fun writeWavHeader(
        fd: FileDescriptor,
        sampleRate: Int,
        channels: Int,
        totalBytes: Long
    ) {
        val byteRate = sampleRate * channels * 2
        val totalDataLen = totalBytes + 36

        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray())
            putInt(totalDataLen.toInt())
            put("WAVE".toByteArray())
            put("fmt ".toByteArray())
            putInt(16)
            putShort(1)
            putShort(channels.toShort())
            putInt(sampleRate)
            putInt(byteRate)
            putShort((channels * 2).toShort())
            putShort(16)
            put("data".toByteArray())
            putInt(totalBytes.toInt())
        }.array()

        FileOutputStream(fd).channel.use { it.position(0); it.write(ByteBuffer.wrap(header)) }
    }

    private fun getPathFromUri(uri: Uri): String? {
        val projection = arrayOf(MediaStore.MediaColumns.DATA)
        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
            if (cursor.moveToFirst()) {
                return cursor.getString(columnIndex)
            }
        }
        return null
    }
}