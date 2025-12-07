package com.voicelife.assistant.utils

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import java.io.File

/**
 * 音频播放器
 * 用于播放录音文件
 */
class AudioPlayer(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var currentFilePath: String? = null
    private var playbackCallback: ((Boolean) -> Unit)? = null

    companion object {
        private const val TAG = "AudioPlayer"
    }

    /**
     * 播放音频文件
     * @param filePath 文件路径
     * @param callback 播放状态回调
     */
    fun play(filePath: String, callback: (Boolean) -> Unit) {
        try {
            // 如果正在播放其他文件，先停止
            if (currentFilePath != filePath) {
                stop()
            }

            // 检查文件是否存在
            val file = File(filePath)
            if (!file.exists()) {
                Log.e(TAG, "File not found: $filePath")
                callback(false)
                return
            }

            playbackCallback = callback

            // 如果已经有MediaPlayer实例且是同一个文件，则继续播放
            if (mediaPlayer != null && currentFilePath == filePath) {
                mediaPlayer?.start()
                callback(true)
                return
            }

            // 创建新的MediaPlayer
            mediaPlayer = MediaPlayer().apply {
                setDataSource(filePath)
                prepare()
                
                setOnCompletionListener {
                    Log.d(TAG, "Playback completed")
                    callback(false)
                    currentFilePath = null
                }

                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                    callback(false)
                    currentFilePath = null
                    true
                }

                start()
            }

            currentFilePath = filePath
            callback(true)
            Log.d(TAG, "Started playing: $filePath")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to play audio", e)
            callback(false)
            currentFilePath = null
        }
    }

    /**
     * 暂停播放
     */
    fun pause() {
        try {
            mediaPlayer?.pause()
            playbackCallback?.invoke(false)
            Log.d(TAG, "Playback paused")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to pause", e)
        }
    }

    /**
     * 停止播放
     */
    fun stop() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                reset()
                release()
            }
            mediaPlayer = null
            currentFilePath = null
            playbackCallback?.invoke(false)
            Log.d(TAG, "Playback stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop", e)
        }
    }

    /**
     * 是否正在播放
     */
    fun isPlaying(): Boolean {
        return try {
            mediaPlayer?.isPlaying ?: false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 获取当前播放位置（毫秒）
     */
    fun getCurrentPosition(): Int {
        return try {
            mediaPlayer?.currentPosition ?: 0
        } catch (e: Exception) {
            0
        }
    }

    /**
     * 获取总时长（毫秒）
     */
    fun getDuration(): Int {
        return try {
            mediaPlayer?.duration ?: 0
        } catch (e: Exception) {
            0
        }
    }

    /**
     * 跳转到指定位置
     */
    fun seekTo(position: Int) {
        try {
            mediaPlayer?.seekTo(position)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to seek", e)
        }
    }

    /**
     * 释放资源
     */
    fun release() {
        stop()
    }
}
