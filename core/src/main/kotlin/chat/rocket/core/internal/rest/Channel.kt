package chat.rocket.core.internal.rest

import chat.rocket.common.model.RoomType
import chat.rocket.core.RocketChatClient
import chat.rocket.core.internal.RestResult
import chat.rocket.core.internal.model.CreateNewChannelPayload
import chat.rocket.core.internal.model.UserNamePayload
import chat.rocket.core.internal.model.UserPayload
import chat.rocket.core.internal.model.UserPayloadData
import chat.rocket.core.model.Room
import com.squareup.moshi.Types
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.RequestBody

/**
 * Creates a new chat room.
 *
 * @param roomType The type of the room.
 * @param name Name of the chat room
 * @param usersList The list of users who are invited to join the chat room.
 * @param readOnly Tells whether to keep the new chat room read only or not.
 */
suspend fun RocketChatClient.createChannel(
    roomType: RoomType,
    name: String,
    usersList: List<String>?,
    readOnly: Boolean? = false
): Room = withContext(Dispatchers.Default) {
    val payload = CreateNewChannelPayload(name, usersList, readOnly)
    val adapter = moshi.adapter(CreateNewChannelPayload::class.java)
    val payloadBody = adapter.toJson(payload)
    val body = RequestBody.create(MEDIA_TYPE_JSON, payloadBody)

    val url = requestUrl(restUrl, getRestApiMethodNameByRoomType(roomType, "create")).build()

    val request = requestBuilderForAuthenticatedMethods(url).post(body).build()
    val type = Types.newParameterizedType(RestResult::class.java, Room::class.java)

    return@withContext handleRestCall<RestResult<Room>>(request, type).result()
}


/**
 * Creates a new chat room.
 *
 * @param roomType The type of the room.
 * @param name Name of the chat room
 * @param usersList The list of users who are invited to join the chat room.
 * @param readOnly Tells whether to keep the new chat room read only or not.
 */
suspend fun RocketChatClient.createDirectMessageRoom(
        username: String
): Room = withContext(Dispatchers.Default) {
    val payload = UserNamePayload(username)
    val adapter = moshi.adapter(UserNamePayload::class.java)
    val payloadBody = adapter.toJson(payload)
    val body = RequestBody.create(MEDIA_TYPE_JSON, payloadBody)

    val url = requestUrl(restUrl, "im.create").build()

    val request = requestBuilderForAuthenticatedMethods(url).post(body).build()
    val type = Types.newParameterizedType(RestResult::class.java, Room::class.java)

    return@withContext handleRestCall<RestResult<Room>>(request, type).result()
}

