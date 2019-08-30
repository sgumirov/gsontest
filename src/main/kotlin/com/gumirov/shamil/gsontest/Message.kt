package com.gumirov.shamil.gsontest

import com.google.gson.*
import com.google.gson.annotations.Expose
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import java.lang.reflect.Type
import java.util.*

class MessageAdapterFactory: TypeAdapterFactory {
    override fun <T> create(gson: Gson?, type: TypeToken<T>?): TypeAdapter<T> {
        val adapterDelegate = gson?.getDelegateAdapter(this, type)
        return object: TypeAdapter<T>(){
            val mDeserializer = MessageDeserializer()
            override fun write(out: JsonWriter?, value: T) {
                if (value is Message) value.serialize()
                adapterDelegate?.write(out, value)
            }

            override fun read(inO: JsonReader?): T? {
                val res = adapterDelegate?.read(inO)
                if (res is Message) res.deserialize()
                return res
            }
        }
    }
}

class MessageDeserializer: JsonDeserializer<Message> {
    companion object {
        val whitelist = hashSetOf(TR::class.java.name, TE::class.java.name)
    }

    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): Message {
        val jobj = json!!.asJsonObject //?: return null
        val klass = jobj[Message.HEADERS]?.asJsonObject?.get(Message.TYPE)?.asString
        if (!whitelist.contains(klass)) {
            //not in whitelist, will set type as Message
            return Gson().fromJson(jobj, Message::class.java).also {
                it.headers.put(Message.TYPE, klass ?: Message::class.java.name)
            }
        }
        return context!!.deserialize<Message>(jobj, Class.forName(klass))
    }
}

open class Message(@Expose val from: String? = null, @Expose val body: String? = null, @Expose val to: String? = null, @Expose val date: Date? = Date()) {
    @Expose(serialize = false)
    private val type = javaClass.name

    @Expose
    val headers: MutableMap<String, String> = mutableMapOf(Pair(TYPE, type))

    override fun toString(): String {
        return "$type(from='$from', body='$body', type='$type', headers:\n  $headers\n)"
    }

    open fun serialize(){}
    open fun deserialize(){}

    companion object {
        const val TYPE = "type"
        const val HEADERS = "headers"
    }
}

abstract class T (var fw: String, f: String, b: String): Message(f, b) {
    override fun serialize() {
        headers.put("fw", fw)
    }

    override fun deserialize() {
        if (headers.containsKey("fw")) fw = headers.get("fw")!!
    }
}

class TR(fw: String, f: String, b: String, var link: String?) : T(fw, f, b) {
    override fun serialize() {
        super.serialize()
        val link = this.link
        if (link != null) headers.put("link", link)
    }

    override fun deserialize() {
        super.deserialize()
        link = headers.get("link")
    }
}

class TE(fw: String, f: String, b: String) : T(fw, f, b)
