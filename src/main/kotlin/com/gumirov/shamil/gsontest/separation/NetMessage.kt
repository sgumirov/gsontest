package com.gumirov.shamil.gsontest.separation

import com.google.gson.*
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import java.lang.reflect.Type
import java.util.*

/**
 * Transport level basic class.
 */
data class NetMessage constructor(
        @Expose var to: String? = null,
        @Expose var from: String? = null,
        @Expose var body: String? = null,
        @Expose var date: String? = null,
        @Expose @SerializedName("_id") var id: String? = null,
        @Expose(serialize = false, deserialize = false) var type: String = NetMessage::class.java.name,
        @Expose var headers: MutableMap<String, String> = mutableMapOf(kotlin.Pair(TYPE, type))
) {
    companion object {
        const val TYPE = "type"
        const val HEADERS = "headers"
    }

    override fun equals(other: Any?): Boolean {
        return super.equals(other) || other is NetMessage && (
                other.type == type && other.to == to && other.id == id && other.from == from && other.date == date &&
                        //other.headers.size == headers.size && headers.keys.containsAll(other.headers.keys) &&
                        other.headers.equals(headers)
                )
    }

    override fun toString(): String {
        return "$type(from='$from', body='$body', type='$type', headers:\n  $headers\n)"
    }
}

/**
 * Transport level JSON deserializer.
 */
class NetMessageDeserializer: JsonDeserializer<NetMessage> {
    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): NetMessage {
        val jobj = json!!.asJsonObject //?: return null
        val klass = jobj[NetMessage.HEADERS]?.asJsonObject?.get(NetMessage.TYPE)?.asString
        return Gson().fromJson(jobj, NetMessage::class.java).also {
            it.headers.put(NetMessage.TYPE, klass ?: NetMessage::class.java.name)
            it.type = klass ?: NetMessage::class.java.name
        }
    }
}

/**
 * Helper function to set up gson.
 */
fun createJsonParser() = GsonBuilder()
        .registerTypeAdapter(NetMessage::class.java, NetMessageDeserializer())
        .excludeFieldsWithoutExposeAnnotation()
        .setPrettyPrinting()
        .create()

/**
 * Base class for domain-level. Any descendants maps to [NetMessage] with help of [serialize] and [deserialize]. Custom
 * fields and type are stored in headers.
 */
abstract class Message (var to: String? = null, var from: String? = null, var body: String? = null, var date: DateTime? = null, var id: String? = null) {
    companion object {
        private val dateParser = ISODateTimeFormat.dateTimeParser()
        private val datePrinter = ISODateTimeFormat.dateTime()
        fun parseDate(date: String?): DateTime? = if (date == null) null else dateParser.parseDateTime(date)
        fun encodeDate(date: DateTime?): String? = if (date == null) null else datePrinter.print(date)
        fun encodeDate(date: Date?): String? = if (date == null) null else datePrinter.print(DateTime(date))
    }

    /**
     * Write all custom fields to [headers] map provided.
     */
    abstract fun serialize(headers: MutableMap<String, String>)

    /**
     * Read all custom fields from [headers] map.
     */
    abstract fun deserialize(headers: Map<String, String>)

    override fun toString(): String {
        return "Message(to=$to, from=$from, body=$body, date=$date)"
    }
}

/**
 * Transforms object between transport ([NetMessage]) and domain (descendants of [Message]) levels.
 */
class Transformer(val whitelist: Set<String>)
{
    fun deserialize(source: NetMessage): Message {
        if (whitelist.contains(source.type)) {
            return (Class.forName(source.type).getConstructor().newInstance() as Message).also { with(it) {
                to = source.to
                body = source.body
                date = Message.parseDate(source.date)
                id = source.id
                from = source.from
                deserialize(source.headers)
            }}
        }
        throw IllegalArgumentException("NetMessage type class not in whitelist: " + source.type)
    }

    fun serialize(source: Message): NetMessage {
        val klass = source::class.java.name
        if (whitelist.contains(klass)){
            return NetMessage(source.to, source.from, source.body, Message.encodeDate(source.date), source.id).also {
                source.serialize(it.headers)
                it.type = klass
                it.headers.put(NetMessage.TYPE, klass)
            }
        }
        throw IllegalArgumentException("NetMessage type class not in whitelist: " + klass)
    }
}
