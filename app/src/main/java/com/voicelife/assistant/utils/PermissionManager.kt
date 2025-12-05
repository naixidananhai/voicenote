package com.voicelife.assistant.utils

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 权限管理器
 * 统一管理应用所需的各种权限
 *
 * 需要的权限:
 * - RECORD_AUDIO (录音)
 * - POST_NOTIFICATIONS (通知 - Android 13+)
 * - 忽略电池优化 (保活)
 */
@Singleton
class PermissionManager @Inject constructor(
    private val context: Context
) {

    companion object {
        const val REQUEST_RECORD_AUDIO = 100
        const val REQUEST_POST_NOTIFICATIONS = 101
        const val REQUEST_BATTERY_OPTIMIZATION = 102
    }

    /**
     * 检查录音权限
     */
    fun hasRecordAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * 检查通知权限 (Android 13+)
     */
    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true  // 低版本不需要
        }
    }

    /**
     * 检查是否在电池优化白名单
     */
    fun isIgnoringBatteryOptimizations(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            powerManager.isIgnoringBatteryOptimizations(context.packageName)
        } else {
            true
        }
    }

    /**
     * 检查所有必需权限
     */
    fun hasAllRequiredPermissions(): Boolean {
        return hasRecordAudioPermission() && hasNotificationPermission()
    }

    /**
     * 获取录音权限Intent
     */
    fun getRecordAudioPermission(): String {
        return Manifest.permission.RECORD_AUDIO
    }

    /**
     * 获取通知权限Intent (Android 13+)
     */
    fun getNotificationPermission(): String? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.POST_NOTIFICATIONS
        } else {
            null
        }
    }

    /**
     * 创建电池优化白名单Intent
     */
    fun createBatteryOptimizationIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        } else {
            Intent(Settings.ACTION_SETTINGS)
        }
    }

    /**
     * 创建应用设置Intent
     */
    fun createAppSettingsIntent(): Intent {
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
    }

    /**
     * 获取缺失的权限列表
     */
    fun getMissingPermissions(): List<PermissionInfo> {
        val missing = mutableListOf<PermissionInfo>()

        if (!hasRecordAudioPermission()) {
            missing.add(
                PermissionInfo(
                    permission = Manifest.permission.RECORD_AUDIO,
                    name = "录音权限",
                    description = "用于检测和录制人声",
                    isRequired = true
                )
            )
        }

        if (!hasNotificationPermission()) {
            missing.add(
                PermissionInfo(
                    permission = getNotificationPermission() ?: "",
                    name = "通知权限",
                    description = "显示服务运行状态",
                    isRequired = true
                )
            )
        }

        if (!isIgnoringBatteryOptimizations()) {
            missing.add(
                PermissionInfo(
                    permission = "BATTERY_OPTIMIZATION",
                    name = "电池优化白名单",
                    description = "允许后台持续运行",
                    isRequired = false
                )
            )
        }

        return missing
    }
}

/**
 * 权限信息
 */
data class PermissionInfo(
    val permission: String,
    val name: String,
    val description: String,
    val isRequired: Boolean
)
