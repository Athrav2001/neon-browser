package com.neo.downloader.utils

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import java.io.File

object FileNameUtil {
    /**
     * make sure to validate name before using this function
     */
    fun getExtensionOrNull(filename: String): String? {
        return filename
            .substringAfterLast('.', "")
            .takeIf { it.isNotEmpty() }
    }

    fun numberedIfExists(filename: File): Flow<File> {
        return flow {
            if (!filename.exists()) {
                emit(filename)
            }
            val ext = filename.extension
                .takeIf { it.isNotEmpty() }
                ?.let { ".$it" }.orEmpty()
            val name = filename.nameWithoutExtension
            val parent = requireNotNull(filename.parentFile) {
                "File has no parent directory: ${filename.absolutePath}"
            }
            var counter = 1
            while (currentCoroutineContext().isActive) {
                val newFile = parent.resolve(
                    "${name}_${counter}${ext}"
                )
                if (!newFile.exists()) {
                    emit(newFile)
                }
                counter++
            }
        }
    }

    fun replaceExtension(filename: String, newExtension: String, appendIfNotExists: Boolean = true): String {
        val ext = getExtensionOrNull(filename) ?: if (appendIfNotExists) {
            return "$filename.$newExtension"
        } else {
            return filename
        }
        val filenameWithoutExtension = filename.dropLast(ext.length)
        return "$filenameWithoutExtension$newExtension"
    }
}
