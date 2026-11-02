package com.example.tools.recover

object PhotoRecNative {
    init {
        System.loadLibrary("photorec_jni")
    }

    external fun runScan(sourceFd: Int, outputDirectory: String): Int

    external fun cancelScan()
}