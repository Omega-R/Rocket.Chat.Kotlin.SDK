package chat.rocket.core.internal

import com.squareup.moshi.*

@JsonQualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ForceString

class ForceStringAdapter {

    @FromJson
    @ForceString
    fun fromJson(reader: JsonReader): Boolean {
        val peek = reader.peek()
        when (peek) {
            JsonReader.Token.NUMBER -> {
                return reader.nextInt() == 1
            }
            JsonReader.Token.STRING -> {
                return reader.nextString().toInt() == 1
            }
            else -> {
                reader.skipValue()
                return false
            }
        }
    }

    @ToJson
    fun toJson(@ForceString value: Boolean): String {
        return if (value) "1" else "0"
    }

}