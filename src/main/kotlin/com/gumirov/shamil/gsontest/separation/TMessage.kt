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
data class TMessage constructor(
        @Expose var to: String? = null,
        @Expose var from: String? = null,
        @Expose var body: String? = null,
        @Expose var date: String? = null,
        @Expose @SerializedName("_id") var id: String? = null,
        @Expose(serialize = false, deserialize = false) var type: String = TMessage::class.java.name,
        @Expose var headers: MutableMap<String, String> = mutableMapOf(kotlin.Pair(TYPE, type))
) {
    companion object {
        const val TYPE = "type"
        const val HEADERS = "headers"
    }

//    constructor() : this("")

    override fun equals(other: Any?): Boolean {
        return super.equals(other) || other is TMessage && (
                other.type == type && other.to == to && other.id == id && other.from == from && other.date == date &&
                        //other.headers.size == headers.size && headers.keys.containsAll(other.headers.keys) &&
                        other.headers.equals(headers)
                )
    }

    override fun toString(): String {
        return "$type(from='$from', body='$body', type='$type', headers:\n  $headers\n)"
    }
}

class TMessageDeserializer: JsonDeserializer<TMessage> {
    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): TMessage {
        val jobj = json!!.asJsonObject //?: return null
        val klass = jobj[TMessage.HEADERS]?.asJsonObject?.get(TMessage.TYPE)?.asString
        return Gson().fromJson(jobj, TMessage::class.java).also {
            it.headers.put(TMessage.TYPE, klass ?: TMessage::class.java.name)
            it.type = klass ?: TMessage::class.java.name
        }
    }
}

fun createJsonParser() = GsonBuilder()
        .registerTypeAdapter(TMessage::class.java, TMessageDeserializer())
        .excludeFieldsWithoutExposeAnnotation()
        .setPrettyPrinting()
        .create()

abstract class Message (var to: String? = null, var from: String? = null, var body: String? = null, var date: DateTime? = null, var id: String? = null) {
    companion object {
        private val dateParser = ISODateTimeFormat.dateTimeParser() //SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'")
        private val datePrinter = ISODateTimeFormat.dateTime() //SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'")
        fun parseDate(date: String?): DateTime? = if (date == null) null else dateParser.parseDateTime(date)
        fun encodeDate(date: DateTime?): String? = if (date == null) null else datePrinter.print(date)
        fun encodeDate(date: Date?): String? = if (date == null) null else datePrinter.print(DateTime(date))
    }

//    constructor(): this(to = null)

    abstract fun serialize(headers: MutableMap<String, String>)
    abstract fun deserialize(headers: Map<String, String>)

    override fun toString(): String {
        return "Message(to=$to, from=$from, body=$body, date=$date)"
    }
}

class Transformer(val whitelist: Set<String>)
{
    fun deserialize(source: TMessage): Message? {
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
        return null
    }

    fun serialize(source: Message): TMessage? {
        val klass = source::class.java.name
        if (whitelist.contains(klass)){
            return TMessage(source.to, source.from, source.body, Message.encodeDate(source.date), source.id).also {
                source.serialize(it.headers)
                it.type = klass
                it.headers.put(TMessage.TYPE, klass)
            }
        }
        return null
    }
}

class Enquiry(body: String? = null, to: String? = null, from: String? = null, date: Date? = null, id: String? = null)
    : Message(to, from, body, DateTime(date), id)
{
    companion object {
        private const val PROFESSION = "profession"
        private const val URGENT = "urgent"
    }

    lateinit var profession: String
    var isUrgent: Boolean = false

//    constructor(): this(null)

    constructor(body: String?, profession: String, isUrgent: Boolean, to: String?, from: String?, date: Date?, id: String?):
            this(body, to, from, date, id)
    {
        this.profession = profession
        this.isUrgent = isUrgent
    }

    override fun deserialize(headers: Map<String, String>) {
        profession = headers.getOrDefault(PROFESSION, "")
        isUrgent = java.lang.Boolean.parseBoolean(headers.get(URGENT))
    }

    override fun serialize(headers: MutableMap<String, String>) {
        headers.put(URGENT, isUrgent.toString())
        headers.put(PROFESSION, profession)
    }

    override fun toString(): String {
        return "Enquiry(profession='$profession', isUrgent=$isUrgent) "+super.toString()
    }
}
