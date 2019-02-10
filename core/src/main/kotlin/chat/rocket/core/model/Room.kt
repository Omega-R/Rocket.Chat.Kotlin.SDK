package chat.rocket.core.model

import chat.rocket.common.internal.ISO8601Date
import chat.rocket.common.model.BaseRoom
import chat.rocket.common.model.RoomType
import chat.rocket.common.model.SimpleUser
import com.squareup.moshi.Json
import se.ansman.kotshi.JsonDefaultValueBoolean
import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class Room(
    @Json(name = "_id") override val id: String,
    @Json(name = "t") override val type: RoomType,
    @Json(name = "u") override val user: SimpleUser?,
    val name: String?,
    @Json(name = "fname") override val fullName: String?,
    @Json(name = "ro") @JsonDefaultValueBoolean(false) override val readonly: Boolean,
    @Json(name = "_updatedAt") @ISO8601Date override val updatedAt: Long?,

    val topic: String?,
    val description: String?,
    val announcement: String?,
    val lastMessage: Message?,
    @Json(name = "muted") val mutedUsers: Array<String>?,
    @JsonDefaultValueBoolean(false) val broadcast: Boolean
) : BaseRoom

//"_id": "Lymsiu4Mn6xjTAan4RtMDEYc28fQ5aHpf4",
//"_updatedAt": "2018-03-26T19:11:50.711Z",
//"t": "d",
//"msgs": 0,
//"ts": "2018-03-26T19:11:50.711Z",
//"meta": {
//    "revision": 0,
//    "created": 1522094603745,
//    "version": 0
//},
//"$loki": 65,
//"usernames": [
//"rocket.cat",
//"user.test"
//]

//"_id": "ByehQjC44FwMeiLbX",
//"name": "channelname",
//"t": "c",
//"usernames": [
//"example"
//],
//"msgs": 0,
//"u": {
//    "_id": "aobEdbYhXfu5hkeqG",
//    "username": "example"
//},
//"ts": "2016-05-30T13:42:25.304Z"