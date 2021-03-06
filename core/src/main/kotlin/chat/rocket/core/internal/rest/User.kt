package chat.rocket.core.internal.rest

import chat.rocket.common.RocketChatException
import chat.rocket.common.model.BaseResult
import chat.rocket.common.model.RoomType
import chat.rocket.common.model.User
import chat.rocket.common.util.CalendarISO8601Converter
import chat.rocket.core.RocketChatClient
import chat.rocket.core.internal.RestMultiResult
import chat.rocket.core.internal.RestResult
import chat.rocket.core.internal.model.*
import chat.rocket.core.model.*
import com.squareup.moshi.Types
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.InputStream

/**
 * Returns the current logged user information, useful to check if the Token from TokenProvider
 * is still valid.
 *
 * @see Myself
 * @see RocketChatException
 */
suspend fun RocketChatClient.me(): Myself {
    val httpUrl = requestUrl(restUrl, "me").build()
    val request = requestBuilderForAuthenticatedMethods(httpUrl).get().build()

    return handleRestCall(request, Myself::class.java)
}

/**
 * Returns the profile for the user.
 *
 * @param userId The ID of the user to update.
 * @param email The email address for the user.
 * @param name The display name of the user.
 * @param password The password for the user.
 * @param username The username for the user.
 * @return An [User] with an updated profile.
 */
suspend fun RocketChatClient.updateProfile(
        userId: String,
        email: String? = null,
        name: String? = null,
        password: String? = null,
        username: String? = null
): User {
    val payload = UserPayload(userId, UserPayloadData(name, password, username, email), null)
    val adapter = moshi.adapter(UserPayload::class.java)

    val payloadBody = adapter.toJson(payload)
    val body = RequestBody.create(MEDIA_TYPE_JSON, payloadBody)

    val httpUrl = requestUrl(restUrl, "users.update").build()
    val request = requestBuilderForAuthenticatedMethods(httpUrl).post(body).build()

    val type = Types.newParameterizedType(RestResult::class.java, User::class.java)
    return handleRestCall<RestResult<User>>(request, type).result()
}

suspend fun RocketChatClient.users(
        offset: Long,
        count: Long
): PagedResult<List<User>> {
    val httpUrl = requestUrl(restUrl, "users.list")
            .addQueryParameter("offset", offset.toString())
            .addQueryParameter("count", count.toString())
            .build()

    val request = requestBuilderForAuthenticatedMethods(httpUrl).get().build()

    val type = Types.newParameterizedType(
            RestResult::class.java,
            Types.newParameterizedType(List::class.java, User::class.java)
    )
    val result = handleRestCall<RestResult<List<User>>>(request, type)

    return PagedResult<List<User>>(result.result(), result.total() ?: 0, result.offset() ?: 0)
}

/**
 * Returns the profile for the user.
 *
 * @param userId The ID of the user to update.
 * @return An [User] with an updated profile.
 */
suspend fun RocketChatClient.getProfileByUserId(userId: String): User {
    val urlBuilder = requestUrl(restUrl, "users.info")
            .addQueryParameter("userId", userId)

    val request = requestBuilderForAuthenticatedMethods(urlBuilder.build()).get().build()

    val type = Types.newParameterizedType(RestResult::class.java, User::class.java)
    return handleRestCall<RestResult<User>>(request, type).result()
}

/**
 * Updates the profile for the user.
 *
 * @param username The username for the user.
 * @return An [User] with an updated profile.
 */
suspend fun RocketChatClient.getProfileByUsername(username: String): User {
    val urlBuilder = requestUrl(restUrl, "users.info")
            .addQueryParameter("username", username)

    val request = requestBuilderForAuthenticatedMethods(urlBuilder.build()).get().build()

    val type = Types.newParameterizedType(RestResult::class.java, User::class.java)
    return handleRestCall<RestResult<User>>(request, type).result()
}

/**
 * Updates own basic information for the user.
 *
 * @param email The email address for the user.
 * @param currentPassword The password for the user encrypted in SHA256.
 * @param newPassword The new password for the user.
 * @param username The username for the user.
 * @param name The display name of the user.
 * @return An [User] with an updated profile.
 */
suspend fun RocketChatClient.updateOwnBasicInformation(
        email: String? = null,
        currentPassword: String? = null,
        newPassword: String? = null,
        username: String? = null,
        name: String? = null
): User {
    val payload =
            OwnBasicInformationPayload(OwnBasicInformationPayloadData(email, currentPassword, newPassword, username, name))
    val adapter = moshi.adapter(OwnBasicInformationPayload::class.java)

    val payloadBody = adapter.toJson(payload)
    val body = RequestBody.create(MEDIA_TYPE_JSON, payloadBody)

    val httpUrl = requestUrl(restUrl, "users.updateOwnBasicInfo").build()
    val request = requestBuilderForAuthenticatedMethods(httpUrl).post(body).build()

    val type = Types.newParameterizedType(RestResult::class.java, User::class.java)
    return handleRestCall<RestResult<User>>(request, type).result()
}

/**
 * Resets the user's avatar.
 *
 * @param userId The ID of the user to reset the avatar.
 *
 * @return True if the avatar was reset, false otherwise.
 */
suspend fun RocketChatClient.resetAvatar(userId: String): Boolean {
    val payload = UserPayload(userId, null, null)
    val adapter = moshi.adapter(UserPayload::class.java)

    val payloadBody = adapter.toJson(payload)
    val body = RequestBody.create(MEDIA_TYPE_JSON, payloadBody)

    val httpUrl = requestUrl(restUrl, "users.resetAvatar").build()
    val request = requestBuilderForAuthenticatedMethods(httpUrl).post(body).build()

    return handleRestCall<BaseResult>(request, BaseResult::class.java).success
}

/**
 * Sets the user's avatar.
 *
 * @param file The file to set the avatar.
 * @param mimeType The MIME type of the file. Allowed MIME types are: *image/gif*, *image/png*, *image/jpeg*, *image/bmp* and *image/webp*.
 *
 * @return True if the avatar was setted up, false otherwise.
 */
suspend fun RocketChatClient.setAvatar(fileName: String, mimeType: String, inputStreamProvider: () -> InputStream?): Boolean {
    if (mimeType != "image/gif" && mimeType != "image/png" && mimeType != "image/jpeg" && mimeType != "image/bmp" && mimeType != "image/webp") {
        throw RocketChatException("Invalid image type $mimeType")
    }

    val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("image", fileName,
                    InputStreamRequestBody(MediaType.parse(mimeType), inputStreamProvider)
            )
            .build()

    val httpUrl = requestUrl(restUrl, "users.setAvatar").build()
    val request = requestBuilderForAuthenticatedMethods(httpUrl).post(body).build()

    return handleRestCall<BaseResult>(request, BaseResult::class.java).success
}

/**
 * Sets the user's avatar.
 *
 * @param avatarUrl Url of the avatar for the user
 *
 * @return True if the avatar was setted up, false otherwise.
 */
suspend fun RocketChatClient.setAvatar(avatarUrl: String): Boolean {
    val payload = UserPayload(null, null, avatarUrl)
    val adapter = moshi.adapter(UserPayload::class.java)

    val payloadBody = adapter.toJson(payload)
    val body = RequestBody.create(MEDIA_TYPE_JSON, payloadBody)

    val httpUrl = requestUrl(restUrl, "users.setAvatar").build()
    val request = requestBuilderForAuthenticatedMethods(httpUrl).post(body).build()

    return handleRestCall<BaseResult>(request, BaseResult::class.java).success
}

/**
 * get the user's avatar.
 *
 * @param avatarUrl Url of the avatar for the user
 *
 * @return True if the avatar was setted up, false otherwise.
 */
suspend fun RocketChatClient.getAvatar(userId: String): String {

    val urlBuilder = requestUrl(restUrl, "users.getAvatar")
            .addQueryParameter("userId", userId)

    val request = requestBuilderForAuthenticatedMethods(urlBuilder.build()).get().build()

    return handleRestCall(request)
}


/**
 * Return the users ChatRooms (Room + Subscription)
 *
 * @param timestamp Timestamp of the last call to get only updates and removes, defaults to 0 which loads all rooms
 * @param filterCustom Filter custom rooms from the response, default true
 */
suspend fun RocketChatClient.chatRooms(timestamp: Long = 0, filterCustom: Boolean = true): RestMultiResult<List<ChatRoom>, List<Removed>> {
    val rooms = async { listRooms(timestamp) }
    val subscriptions = async { listSubscriptions(timestamp) }

    return combine(rooms.await(), subscriptions.await(), filterCustom)
}

suspend fun RocketChatClient.getSubscription(roomId: String): Subscription? {
    val httpUrl = requestUrl(restUrl, "subscriptions.getOne")
            .addQueryParameter("roomId", roomId)
            .build()
    val request = requestBuilderForAuthenticatedMethods(httpUrl).get().build()

    val type = Types.newParameterizedType(RestResult::class.java, Subscription::class.java)
    return handleRestCall<RestResult<Subscription>>(request, type).result()
}

/**
 * Return all the roles specific to the current user.
 *
 * @return UserRole object specifying current user roles.
 */
suspend fun RocketChatClient.roles(): UserRole = withContext(Dispatchers.Default) {
    val httpUrl = requestUrl(restUrl, "user.roles").build()
    val request = requestBuilderForAuthenticatedMethods(httpUrl).get().build()
    return@withContext handleRestCall<UserRole>(request, UserRole::class.java)
}

internal fun RocketChatClient.combine(
        rooms: RestMultiResult<List<Room>, List<Removed>>,
        subscriptions: RestMultiResult<List<Subscription>, List<Removed>>,
        filterCustom: Boolean
): RestMultiResult<List<ChatRoom>, List<Removed>> {
    val update = combine(rooms.update, subscriptions.update, filterCustom)
    val remove = combineRemoved(rooms.remove, subscriptions.remove, filterCustom)

    logger.debug { "Rooms: update(${rooms.update.size}, remove(${rooms.remove.size}" }
    logger.debug { "Subscriptions: update(${subscriptions.update.size}, remove(${subscriptions.remove.size}" }
    logger.debug { "Combined: update(${update.size}), remove(${remove.size})" }

    return RestMultiResult.create(update, remove)
}

internal fun RocketChatClient.combine(rooms: List<Room>?, subscriptions: List<Subscription>?, filterCustom: Boolean): List<ChatRoom> {
    val map = HashMap<String, Room>()
    rooms?.forEach {
        map[it.id] = it
    }

    val chatRooms = ArrayList<ChatRoom>(subscriptions?.size ?: 0)

    subscriptions?.forEach {
        val room = map[it.roomId]
        val subscription = it
        // In case of any inconsistency we just ignore the room/subscription...
        // This should be a very, very rare situation, like the user leaving/joining a channel
        // between the 2 calls.
        room?.let {
            chatRooms.add(ChatRoom.create(room, subscription, this))
        }
    }

    return chatRooms.filterNot { chatRoom -> filterCustom && chatRoom.type is RoomType.Custom }
}

internal fun RocketChatClient.combineRemoved(rooms: List<Removed>?, subscriptions: List<Removed>?, filterCustom: Boolean): List<Removed> {
    val map = HashMap<String, Removed>()
    rooms?.forEach {
        map[it.id] = it
    }

    val removed = ArrayList<Removed>(subscriptions?.size ?: 0)

    subscriptions?.forEach {
        val room = map[it.id]
        val subscription = it
        // In case of any inconsistency we just ignore the room/subscription...
        // This should be a very, very rare situation, like the user leaving/joining a channel
        // between the 2 calls.
        room?.let {
            removed.add(it)
        }
    }

    return removed
}

suspend fun RocketChatClient.listSubscriptions(timestamp: Long = 0): RestMultiResult<List<Subscription>, List<Removed>> {
    val urlBuilder = requestUrl(restUrl, "subscriptions.get")
    val date = CalendarISO8601Converter().fromTimestamp(timestamp)
    urlBuilder.addQueryParameter("updatedSince", date)

    val request = requestBuilderForAuthenticatedMethods(urlBuilder.build()).get().build()

    val type = Types.newParameterizedType(RestMultiResult::class.java,
            Types.newParameterizedType(List::class.java, Subscription::class.java),
            Types.newParameterizedType(List::class.java, Removed::class.java))

    val response = handleRestCall<RestMultiResult<List<Subscription>, List<Removed>>>(request, type)

    // Some subscriptions doesn't have a name, but just a fname (some livechats)
    // Copy fname to name, and filter any subscription that still doesn't have a name
    val subs = response.update.map { subscription ->
        if (subscription.name == null && subscription.fullName != null) {
            subscription.copy(name = subscription.fullName)
        } else {
            subscription
        }
    }.filterNot { subscription -> subscription.name.isNullOrEmpty() }

    return RestMultiResult.create(subs, response.remove)
}

suspend fun RocketChatClient.listRooms(timestamp: Long = 0): RestMultiResult<List<Room>, List<Removed>> {
    val urlBuilder = requestUrl(restUrl, "rooms.get")
    val date = CalendarISO8601Converter().fromTimestamp(timestamp)
    urlBuilder.addQueryParameter("updatedSince", date)

    val request = requestBuilderForAuthenticatedMethods(urlBuilder.build()).get().build()

    val type = Types.newParameterizedType(RestMultiResult::class.java,
            Types.newParameterizedType(List::class.java, Room::class.java),
            Types.newParameterizedType(List::class.java, Removed::class.java))
    return handleRestCall(request, type)
}

suspend fun RocketChatClient.listChannels(joinedOnly: Boolean = true, offset: Long = 0, count: Long = 0): List<Room> {

    val urlBuilder = requestUrl(restUrl, if (joinedOnly) "channels.list.joined" else "channels.list")
            .addQueryParameter("offset", offset.toString())
            .addQueryParameter("count", count.toString())

    val request = requestBuilderForAuthenticatedMethods(urlBuilder.build()).get().build()
    val type = Types.newParameterizedType(
            RestResult::class.java,
            Types.newParameterizedType(List::class.java, Room::class.java))

    return handleRestCall<RestResult<List<Room>>>(request, type).result()
}

suspend fun RocketChatClient.listGroups(offset: Long = 0, count: Long = 0): List<Room> {

    val urlBuilder = requestUrl(restUrl, "groups.listAll")
            .addQueryParameter("offset", offset.toString())
            .addQueryParameter("count", count.toString())

    val request = requestBuilderForAuthenticatedMethods(urlBuilder.build()).get().build()
    val type = Types.newParameterizedType(
            RestResult::class.java,
            Types.newParameterizedType(List::class.java, Room::class.java))

    return handleRestCall<RestResult<List<Room>>>(request, type).result()
}


suspend fun RocketChatClient.listIms(offset: Long = 0, count: Long = 0): List<Room> {

    val urlBuilder = requestUrl(restUrl, "im.list")
            .addQueryParameter("offset", offset.toString())
            .addQueryParameter("count", count.toString())

    val request = requestBuilderForAuthenticatedMethods(urlBuilder.build()).get().build()

    val type = Types.newParameterizedType(
            RestResult::class.java,
            Types.newParameterizedType(List::class.java, Room::class.java))

    return handleRestCall<RestResult<List<Room>>>(request, type).result()
}