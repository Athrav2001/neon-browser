package com.neo.downloader.downloaditem.hls

import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode

internal object HlsRemuxBridge {
    fun remuxToMp4(inputPath: String, outputPath: String, timeoutMinutes: Long): Boolean {
        val timeoutSeconds = (timeoutMinutes.coerceAtLeast(1L) * 60L)
        val command = buildString {
            append("-timelimit ")
            append(timeoutSeconds)
            append(" ")
            append("-y -fflags +genpts -avoid_negative_ts make_zero -i ")
            append(quote(inputPath))
            append(" -map 0:v:0? -map 0:a:0? -map 0:s:0? -dn ")
            append(" -c copy -movflags +faststart ")
            append(quote(outputPath))
        }
        val session = FFmpegKit.execute(command)
        return ReturnCode.isSuccess(session.returnCode)
    }

    private fun quote(path: String): String {
        return "'${path.replace("'", "'\\''")}'"
    }
}
