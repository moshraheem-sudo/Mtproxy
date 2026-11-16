package com.example.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.TeleProxyApp

class ProxySyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? TeleProxyApp ?: return Result.failure()
        val summary = app.repository.syncAndVerify(maxTestCount = 60)
        return if (summary.errorMessage == null || summary.workingCount > 0) {
            Result.success()
        } else {
            Result.retry()
        }
    }
}
