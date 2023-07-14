package ru.solrudev.ackpine.impl.database.dao

import androidx.annotation.RestrictTo
import androidx.room.Dao
import androidx.room.Query

@RestrictTo(RestrictTo.Scope.LIBRARY)
@Dao
internal interface NotificationIdDao {

	@Query("SELECT notification_id FROM sessions_notification_ids WHERE session_id = :sessionId")
	fun getNotificationId(sessionId: String): Int?

	@Query("UPDATE sessions_notification_ids SET notification_id = :notificationId WHERE session_id = :sessionId")
	fun setNotificationId(sessionId: String, notificationId: Int)

	@Query("INSERT INTO sessions_notification_ids(session_id, notification_id) VALUES (:sessionId, -1)")
	fun initNotificationId(sessionId: String)
}