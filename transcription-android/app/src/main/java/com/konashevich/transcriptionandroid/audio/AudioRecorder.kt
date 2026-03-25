package com.konashevich.pressscribe.audio

import android.content.Context
import android.os.Build
import android.media.MediaRecorder
import java.io.File

class AudioRecorder(
    private val context: Context,
) {
    private var mediaRecorder: MediaRecorder? = null
    private var outputFile: File? = null

    fun start(output: File) {
        stopAndDiscard()
        output.parentFile?.mkdirs()

        val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        recorder.setAudioEncodingBitRate(128_000)
        recorder.setAudioSamplingRate(44_100)
        recorder.setOutputFile(output.absolutePath)
        recorder.prepare()
        recorder.start()

        mediaRecorder = recorder
        outputFile = output
    }

    fun stop(): File? {
        val recorder = mediaRecorder ?: return null
        val file = outputFile
        return try {
            recorder.stop()
            file?.takeIf { it.exists() && it.length() > 0L }
        } catch (_: RuntimeException) {
            file?.delete()
            null
        } finally {
            recorder.reset()
            recorder.release()
            mediaRecorder = null
            outputFile = null
        }
    }

    fun stopAndDiscard() {
        mediaRecorder?.let { recorder ->
            runCatching { recorder.stop() }
            runCatching { recorder.reset() }
            runCatching { recorder.release() }
        }
        outputFile?.delete()
        mediaRecorder = null
        outputFile = null
    }

    fun currentLevel(): Float {
        val recorder = mediaRecorder ?: return 0f
        val amplitude = runCatching { recorder.maxAmplitude }.getOrDefault(0)
        if (amplitude <= 0) {
            return 0f
        }

        // Smooth the meter so quiet speech still lights the edge without clipping the effect.
        return (amplitude / 32767f)
            .coerceIn(0f, 1f)
            .let { kotlin.math.sqrt(it) }
    }
}
