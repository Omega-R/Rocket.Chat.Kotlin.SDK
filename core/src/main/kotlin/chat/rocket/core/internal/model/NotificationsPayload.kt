package chat.rocket.core.internal.model

import chat.rocket.core.internal.ForceString
import com.squareup.moshi.Json
import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class SaveNotificationPayload(
        @Json(name = "roomId")
        val roomId: String,
        @Json(name = "notifications")
        val notificationsPayload: NotificationsPayload
)

@JsonSerializable
data class NotificationsPayload(
        @ForceString
        @Json(name = "disableNotifications")
        val disableNotifications: Boolean = false
)