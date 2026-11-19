package fr.estiam.traillog.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import fr.estiam.traillog.R
import fr.estiam.traillog.TrailLogApplication
import fr.estiam.traillog.model.FollowUpRules
import fr.estiam.traillog.ui.visits.VisitListActivity
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Tâche de fond périodique : compte les problèmes OUVERTS depuis au moins N jours
 * (N = préférence "Délai de relance") et publie une notification de synthèse.
 * Le calcul lui-même est dans FollowUpRules (fonction pure, testée en JUnit).
 */
class OverdueFollowUpWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as TrailLogApplication
        return try {
            // 1. Lire la préférence (délai en jours) et les problèmes encore ouverts
            val thresholdDays = app.settings.settings.first().followUpDays
            val openIssues = app.visits.openIssues()

            // 2. Calcul déterministe du nombre de relances en retard
            val overdue = FollowUpRules.overdueCount(openIssues, System.currentTimeMillis(), thresholdDays)
            Log.d(TAG, "Vérification : ${openIssues.size} problème(s) ouvert(s), seuil=$thresholdDays j, en retard=$overdue")

            // 3. Notification seulement s'il y a quelque chose à signaler
            if (overdue > 0) notify(applicationContext, overdue, thresholdDays)
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Échec de la vérification des relances", e)
            Result.retry()
        }
    }

    companion object {
        const val TAG = "OverdueFollowUp"
        private const val CHANNEL = "traillog_followups"
        private const val NOTIFICATION_ID = 4201
        private const val PERIODIC_NAME = "overdue_followups_periodic"
        private const val ONE_TIME_NAME = "overdue_followups_now"

        /** Canal de notification dédié (Android 8+). Sans effet s'il existe déjà. */
        fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= 26) {
                val channel = NotificationChannel(CHANNEL,
                    context.getString(R.string.followup_channel), NotificationManager.IMPORTANCE_DEFAULT)
                (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                    .createNotificationChannel(channel)
            }
        }

        /** Planification périodique (toutes les 6 h). KEEP : on ne relance pas le compteur à chaque démarrage. */
        fun schedulePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<OverdueFollowUpWorker>(6, TimeUnit.HOURS).build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(PERIODIC_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        /** Exécution immédiate (menu "Vérifier maintenant"), utile pour la démonstration. */
        fun runNow(context: Context) {
            WorkManager.getInstance(context).enqueueUniqueWork(ONE_TIME_NAME, ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<OverdueFollowUpWorker>().build())
        }

        private fun notify(context: Context, overdue: Int, thresholdDays: Int) {
            // Android 13+ : sans permission accordée, on n'essaie pas de notifier
            if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context,
                    android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "Permission POST_NOTIFICATIONS refusée : notification non affichée")
                return
            }
            ensureChannel(context)
            val intent = Intent(context, VisitListActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            val pending = PendingIntent.getActivity(context, NOTIFICATION_ID, intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            val text = context.resources.getQuantityString(R.plurals.followup_text, overdue, overdue, thresholdDays)
            val notification = NotificationCompat.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_mountain)
                .setContentTitle(context.getString(R.string.followup_title))
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setNumber(overdue)
                .setContentIntent(pending)
                .setAutoCancel(true)
                .build()
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        }
    }
}
