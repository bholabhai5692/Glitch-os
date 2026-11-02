package com.example.tools.otp

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

object AppIdentifier {
    fun inspectApp(context: Context, targetPackageName: String?): AppDiagnosticInfo {
        val pm = context.packageManager
        val pkgName = targetPackageName ?: context.packageName
        try {
            val pkgInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(pkgName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(pkgName, 0)
            }

            val appInfo = pkgInfo.applicationInfo
            val targetSdk = appInfo?.targetSdkVersion ?: 0
            val vName = pkgInfo.versionName
            val vCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) pkgInfo.longVersionCode else @Suppress("DEPRECATION") pkgInfo.versionCode.toLong()

            return AppDiagnosticInfo(
                packageName = pkgName,
                versionName = vName,
                versionCode = vCode,
                targetSdk = targetSdk,
                status = "AVAILABLE",
                message = if (appInfo == null) "App details are incomplete; Android did not provide application metadata." else null
            )
        } catch (e: PackageManager.NameNotFoundException) {
            return AppDiagnosticInfo(
                packageName = pkgName,
                versionName = null,
                versionCode = 0,
                targetSdk = 0,
                status = "APP_NOT_FOUND",
                message = "App not found. Check the package name and Android package visibility."
            )
        } catch (e: Exception) {
            return AppDiagnosticInfo(
                packageName = pkgName,
                versionName = null,
                versionCode = 0,
                targetSdk = 0,
                status = "ERROR",
                message = "Could not read app details. Confirm the app is installed and visible to Android."
            )
        }
    }
}
