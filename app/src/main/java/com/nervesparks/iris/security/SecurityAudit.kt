package com.nervesparks.iris.security

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import com.nervesparks.iris.data.UserPreferencesRepository
import timber.log.Timber
/**
 * PHASE 2.5: Security Audit - Comprehensive security monitoring and reporting
 * Provides security auditing capabilities and recommendations
 */
class SecurityAudit(
    private val context: Context,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val encryptedPrefs: EncryptedPreferences
) {

    private val tag = "SecurityAudit"

    data class SecurityReport(
        val overallSecurityLevel: SecurityLevel,
        val issues: List<SecurityIssue>,
        val recommendations: List<String>,
        val lastAuditTime: Long = System.currentTimeMillis()
    )

    enum class SecurityLevel {
        CRITICAL, HIGH, MEDIUM, LOW, SECURE
    }

    data class SecurityIssue(
        val severity: SecurityLevel,
        val title: String,
        val description: String,
        val recommendation: String
    )

    /**
     * Performs comprehensive security audit
     */
    fun performSecurityAudit(): SecurityReport {
        Timber.tag(tag).d("Performing comprehensive security audit")

        val issues = mutableListOf<SecurityIssue>()
        val recommendations = mutableListOf<String>()

        // Check encrypted data integrity
        checkEncryptedDataIntegrity(issues, recommendations)

        // Check API key security
        checkApiKeySecurity(issues, recommendations)

        // Check network security
        checkNetworkSecurity(issues, recommendations)

        // Check app permissions
        checkAppPermissions(issues, recommendations)

        // Check device security
        checkDeviceSecurity(issues, recommendations)

        // Determine overall security level
        val overallLevel = determineOverallSecurityLevel(issues)

        val report = SecurityReport(
            overallSecurityLevel = overallLevel,
            issues = issues,
            recommendations = recommendations
        )

        Timber.tag(tag).d("Security audit completed. Level: $overallLevel, Issues: ${issues.size}")
        return report
    }

    private fun checkEncryptedDataIntegrity(
        issues: MutableList<SecurityIssue>,
        recommendations: MutableList<String>
    ) {
        try {
            // Check if sensitive data is properly encrypted
            val hasApiKeys = encryptedPrefs.hasEncryptedData("huggingface_token") ||
                            encryptedPrefs.hasEncryptedData("google_api_key")

            if (!hasApiKeys) {
                issues.add(SecurityIssue(
                    SecurityLevel.MEDIUM,
                    "No Encrypted API Keys Found",
                    "No API keys are stored in encrypted storage",
                    "Set up API keys through secure settings"
                ))
                recommendations.add("Configure API keys in settings for secure storage")
            }

            // Check for data integrity
            val allKeys = encryptedPrefs.getAllEncryptedKeys()
            if (allKeys.size > 10) {
                issues.add(SecurityIssue(
                    SecurityLevel.LOW,
                    "Large Number of Encrypted Keys",
                    "${allKeys.size} encrypted keys found",
                    "Review and clean up unnecessary encrypted data"
                ))
            }

        } catch (e: Exception) {
            Timber.tag(tag).e(e, "Error checking encrypted data integrity")
            issues.add(SecurityIssue(
                SecurityLevel.HIGH,
                "Encrypted Storage Error",
                "Unable to access encrypted storage: ${e.message}",
                "Check device security and storage permissions"
            ))
        }
    }

    private fun checkApiKeySecurity(
        issues: MutableList<SecurityIssue>,
        recommendations: MutableList<String>
    ) {
        // Check if API keys are present and valid
        val hfToken = userPreferencesRepository.huggingFaceToken
        val googleApiKey = userPreferencesRepository.googleApiKey

        if (hfToken.isNotEmpty() && !InputValidator.isValidHuggingFaceToken(hfToken)) {
            issues.add(SecurityIssue(
                SecurityLevel.HIGH,
                "Invalid HuggingFace Token",
                "Stored HuggingFace token format is invalid",
                "Update HuggingFace token in settings"
            ))
        }

        if (googleApiKey.isNotEmpty() && !InputValidator.isValidApiKey(googleApiKey)) {
            issues.add(SecurityIssue(
                SecurityLevel.HIGH,
                "Invalid Google API Key",
                "Stored Google API key format is invalid",
                "Update Google API key in settings"
            ))
        }

        if (hfToken.isEmpty() && googleApiKey.isEmpty()) {
            recommendations.add("Consider configuring API keys for enhanced functionality")
        }
    }

    private fun checkNetworkSecurity(
        issues: MutableList<SecurityIssue>,
        recommendations: MutableList<String>
    ) {
        // Check SSL pinning status
        recommendations.add("Ensure SSL certificate pinning is configured for production")

        // Check network permissions
        try {
            val packageInfo: PackageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }

            val hasInternet = packageInfo.requestedPermissions?.contains("android.permission.INTERNET") == true
            if (!hasInternet) {
                issues.add(SecurityIssue(
                    SecurityLevel.HIGH,
                    "Missing Internet Permission",
                    "App lacks internet permission for network operations",
                    "Add INTERNET permission to manifest"
                ))
            }
        } catch (e: Exception) {
            Timber.tag(tag).e(e, "Error checking network permissions")
        }
    }

    private fun checkAppPermissions(
        issues: MutableList<SecurityIssue>,
        recommendations: MutableList<String>
    ) {
        try {
            val packageInfo: PackageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }

            val permissions = packageInfo.requestedPermissions ?: emptyArray()

            // Check for dangerous permissions
            val dangerousPermissions = listOf(
                "android.permission.RECORD_AUDIO",
                "android.permission.CAMERA",
                "android.permission.WRITE_EXTERNAL_STORAGE"
            )

            val grantedDangerous = dangerousPermissions.filter { it in permissions }

            if (grantedDangerous.isNotEmpty()) {
                issues.add(SecurityIssue(
                    SecurityLevel.LOW,
                    "Dangerous Permissions Granted",
                    "App has ${grantedDangerous.size} dangerous permissions",
                    "Review permission usage and consider runtime permission requests"
                ))
            }

        } catch (e: Exception) {
            Timber.tag(tag).e(e, "Error checking app permissions")
        }
    }

    private fun checkDeviceSecurity(
        issues: MutableList<SecurityIssue>,
        recommendations: MutableList<String>
    ) {
        // Check Android version for security
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            issues.add(SecurityIssue(
                SecurityLevel.MEDIUM,
                "Outdated Android Version",
                "Device running Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
                "Consider updating to Android 8.0 or higher for better security"
            ))
        }

        // Check if device is rooted (basic check)
        val isRooted = checkRootStatus()
        if (isRooted) {
            issues.add(SecurityIssue(
                SecurityLevel.HIGH,
                "Device Rooted",
                "Device appears to be rooted",
                "Rooted devices are more vulnerable to security threats"
            ))
        }

        recommendations.add("Keep device and app updated for security patches")
        recommendations.add("Use strong passwords and biometric authentication when available")
    }

    private fun checkRootStatus(): Boolean {
        return try {
            val paths = arrayOf(
                "/system/xbin/su",
                "/system/bin/su",
                "/system/xbin/busybox",
                "/system/bin/busybox"
            )
            paths.any { java.io.File(it).exists() }
        } catch (e: Exception) {
            false
        }
    }

    private fun determineOverallSecurityLevel(issues: List<SecurityIssue>): SecurityLevel {
        val criticalCount = issues.count { it.severity == SecurityLevel.CRITICAL }
        val highCount = issues.count { it.severity == SecurityLevel.HIGH }
        val mediumCount = issues.count { it.severity == SecurityLevel.MEDIUM }

        return when {
            criticalCount > 0 -> SecurityLevel.CRITICAL
            highCount > 0 -> SecurityLevel.HIGH
            mediumCount > 2 -> SecurityLevel.MEDIUM
            issues.isNotEmpty() -> SecurityLevel.LOW
            else -> SecurityLevel.SECURE
        }
    }

    /**
     * Get security recommendations
     */
    fun getSecurityRecommendations(): List<String> {
        return listOf(
            "Always use encrypted storage for sensitive data",
            "Validate all user inputs before processing",
            "Keep API keys and tokens secure and rotated regularly",
            "Use SSL pinning for critical network endpoints",
            "Implement proper error handling without exposing sensitive information",
            "Regularly audit and update security configurations",
            "Use biometric authentication for sensitive operations",
            "Keep app and device updated with latest security patches"
        )
    }
}
