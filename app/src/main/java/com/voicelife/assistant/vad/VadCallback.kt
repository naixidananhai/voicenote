package com.voicelife.assistant.vad

/**
 * VAD检测回调接口
 * 用于通知录音管理模块人声活动状态
 */
interface VadCallback {
    /**
     * 检测到人声开始
     */
    fun onVoiceStart()

    /**
     * 检测到人声结束
     */
    fun onVoiceEnd()

    /**
     * VAD检测出错
     * @param error 错误异常
     */
    fun onError(error: Exception)
}
