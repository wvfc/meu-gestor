package com.meugestor.app.worker

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import com.meugestor.app.MainActivity
import com.meugestor.app.MeuGestorApp
import com.meugestor.app.data.database.entity.TransactionType
import com.meugestor.app.domain.effectiveDueDate
import com.meugestor.app.util.CurrencyUtils
import com.meugestor.app.util.DateUtils
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Worker responsável por verificar lançamentos próximos do vencimento
 * e enviar notificações ao usuário.
 */
class ReminderWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? MeuGestorApp ?: return Result.success()
        if (!app.userPreferences.notificationsEnabled.first()) return Result.success()
        if (!canPostNotifications(context)) return Result.success()

        createNotificationChannel(context)

        val today = DateUtils.todayDate()
        val tomorrow = today.plusDays(1)
        val pending = app.database.transactionDao().getPendingTransactionsOnce()

        val overdue = pending.filter { tx -> tx.effectiveDueDate()?.isBefore(today) == true }
        val dueSoon = pending.filter { tx ->
            val due = tx.effectiveDueDate() ?: return@filter false
            !due.isBefore(today) && !due.isAfter(tomorrow)
        }
        if (overdue.isEmpty() && dueSoon.isEmpty()) return Result.success()

        val lines = buildList {
            if (dueSoon.isNotEmpty()) {
                val payable = dueSoon.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
                val receivable = dueSoon.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
                val detail = buildList {
                    if (payable > 0) add("${CurrencyUtils.formatBRL(payable)} a pagar")
                    if (receivable > 0) add("${CurrencyUtils.formatBRL(receivable)} a receber")
                }.joinToString(" e ")
                add("${dueSoon.size} item(ns) vencem hoje ou amanhã: $detail")
            }
            if (overdue.isNotEmpty()) {
                add("${overdue.size} item(ns) em atraso, total de ${CurrencyUtils.formatBRL(overdue.sumOf { it.amount })}")
            }
        }

        postNotification(context, title = "Lembrete financeiro", lines = lines)
        return Result.success()
    }

    companion object {
        const val CHANNEL_ID = "meu_gestor_reminders"
        const val WORK_NAME = "MeuGestorDailyReminder"
        private const val NOTIFICATION_ID = 1001

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<ReminderWorker>(
                repeatInterval = 1,
                repeatIntervalTimeUnit = TimeUnit.DAYS
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }

        fun canPostNotifications(context: Context): Boolean {
            val permissionGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            return permissionGranted && NotificationManagerCompat.from(context).areNotificationsEnabled()
        }

        private fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Lembretes Financeiros",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Alertas de vencimentos e metas"
                }
                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
            }
        }

        // A permissão é verificada em canPostNotifications() antes desta chamada.
        @SuppressLint("MissingPermission")
        private fun postNotification(context: Context, title: String, lines: List<String>) {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            val style = NotificationCompat.InboxStyle()
            lines.forEach { style.addLine(it) }

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle(title)
                .setContentText(lines.first())
                .setStyle(style)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()

            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        }
    }
}
