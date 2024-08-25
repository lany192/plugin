package com.github.lany192.upload

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okio.IOException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import java.io.File

class UploadPlugin : Plugin<Project> {
    private lateinit var log: Logger
    override fun apply(project: Project) {
        log = project.logger
        log.lifecycle("Hello from plugin 'com.github.lany192.upload'")
        val extension = project.extensions.create("upload", PluginExtension::class.java)
        project.tasks.create("upload_file_to_server") {
            group = "upload"
            doLast {
                log.lifecycle("服务器地址：${extension.server_url}")
                log.lifecycle("待上传文件： ${extension.file_path}")
                log.lifecycle("接收的参数: ${extension.parameters.joinToString()}")

                if (extension.file_path == null || extension.server_url == null) {
                    throw IllegalArgumentException("File path and server URL must be provided.")
                }
                uploadFile(extension.file_path, extension.server_url)
            }
        }
    }

    private fun uploadFile(filePath: String, serverUrl: String) {
        val file = File(filePath)
        if (!file.exists()) {
            throw IllegalArgumentException("File does not exist: $filePath")
        }
        try {
            val request = Request.Builder()
                .url(serverUrl)
                .post(
                    MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart(
                            "file", file.name,
                            file.asRequestBody("application/octet-stream".toMediaTypeOrNull())
                        )
                        .build()
                )
                .build()
            val client = OkHttpClient()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Unexpected code $response")
                // 打印服务器响应体
                log.lifecycle("上传结果： ${response.body?.string()}")
            }
        } catch (e: Exception) {
            log.lifecycle("上传失败： ${e.message}")
            e.printStackTrace()
        }
    }
}