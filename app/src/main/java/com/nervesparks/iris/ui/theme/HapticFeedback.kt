package com.nervesparks.iris.ui.theme

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.annotation.RequiresApi

/**
 * Centralized haptic feedback system for consistent user experience
 */
object HapticFeedback {
    
    /**
     * Provides haptic feedback for button presses
     */
    fun buttonPress(context: Context) {
        provideHapticFeedback(context, 50, VibrationEffect.DEFAULT_AMPLITUDE)
    }
    
    /**
     * Provides haptic feedback for long presses
     */
    fun longPress(context: Context) {
        provideHapticFeedback(context, 100, VibrationEffect.DEFAULT_AMPLITUDE)
    }
    
    /**
     * Provides haptic feedback for selection changes
     */
    fun selectionChange(context: Context) {
        provideHapticFeedback(context, 30, VibrationEffect.DEFAULT_AMPLITUDE)
    }
    
    /**
     * Provides haptic feedback for errors
     */
    fun error(context: Context) {
        provideHapticFeedback(context, 200, VibrationEffect.DEFAULT_AMPLITUDE)
    }
    
    /**
     * Provides haptic feedback for success actions
     */
    fun success(context: Context) {
        provideHapticFeedback(context, 150, VibrationEffect.DEFAULT_AMPLITUDE)
    }
    
    /**
     * Provides haptic feedback for notifications
     */
    fun notification(context: Context) {
        provideHapticFeedback(context, 80, VibrationEffect.DEFAULT_AMPLITUDE)
    }
    
    /**
     * Provides haptic feedback for model loading/processing
     */
    fun modelProcessing(context: Context) {
        provideHapticFeedback(context, 300, VibrationEffect.DEFAULT_AMPLITUDE)
    }
    
    /**
     * Provides haptic feedback for chat actions (send, receive)
     */
    fun chatAction(context: Context) {
        provideHapticFeedback(context, 60, VibrationEffect.DEFAULT_AMPLITUDE)
    }
    
    /**
     * Internal method to provide haptic feedback
     */
    private fun provideHapticFeedback(context: Context, duration: Long, amplitude: Int) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

        if (vibrator?.hasVibrator() == true) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    VibrationEffect.createOneShot(
                        duration,
                        amplitude
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(duration)
            }
        }
    }
} 