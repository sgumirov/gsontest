import com.google.gson.*
import com.google.gson.annotations.Expose
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

fun main(args: Array<String>){
    serializeDeserializeTest()
    deserializeUnknownTypeTest()
}

fun serializeDeserializeTest() {
    val l = listOf(TR("for_rec", "from_friend", "thankz"),
            TE("for_response", "another_friend", "thankz for repsonse1"))
    val gson = GsonBuilder()
            .registerTypeAdapter(M::class.java, MDeserializer())
            .registerTypeAdapter(M::class.java, MSerializer())
            .excludeFieldsWithoutExposeAnnotation()
            .setPrettyPrinting().create()
    val s = gson.toJson(l)
    println("serialize list: " + s)
    val listType = object : TypeToken<List<M>>() { }.type
    gson.fromJson<List<M>>(s, listType).forEach{ println("deserialize: $it") }
}

fun deserializeUnknownTypeTest(){
    val listType = object : TypeToken<List<M>>() { }.type
    val gson = GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .setPrettyPrinting()
            .registerTypeAdapter(M::class.java, MDeserializer())
            .registerTypeAdapter(M::class.java, MSerializer())
            .setPrettyPrinting()
            .create()
    val unknownTypeJson = """[
      {
        "headers":[
          {
            "name": "type",
            "value": "TUnknown"
          },
          {
            "name": "fw",
            "value": "for_response"
          },
          {
            "name": "link",
            "value": "a8724sd34r"
          }
        ],
        "f": "another_friend",
        "b": "thankz for repsonse1"
      }
    ]"""
    println("deserialize unknown type: ")
    gson.fromJson<List<M>>(unknownTypeJson, listType).forEach{m ->
        println(m)
        println("-> serialize: ")
        println(gson.toJson(m))
    }
}

class MDeserializer: JsonDeserializer<M> {
    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): M {
        val j = json!!.asJsonObject
        val a = j.getAsJsonArray("headers")
        val map: Map<String, String> = a.associate {
            Pair(it.asJsonObject.get("name")?.asString ?: "", it.asJsonObject.get("value")?.asString ?: "")
        }
        return M(j.get("f").asString, j.get("b").asString, map)
    }
/*    fun getHeaderValue(jobj: JsonObject, name: String): String? = jobj[M.HEADERS]?.asJsonArray?.find {
        it.asJsonObject?.get("name")?.asString.equals(name)
    }?.asJsonObject?.get("value")?.asString
*/
}

class MSerializer: JsonSerializer<M> {
    override fun serialize(m: M?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
        val o = JsonObject()
        if (m?.f != null) o.addProperty("f", m.f)
        if (m?.b != null) o.addProperty("b", m.b)
        val a = JsonArray()
        m?.getHeaders()?.forEach { header -> a.add(JsonObject().also{
            it.addProperty("name", header.name)
            it.addProperty("value", header.value)
        }) }
        return o
    }
}

open class M(val f: String? = null, val b: String? = null, protected val deserializedHeaders: Map<String, String>? = null) {
    companion object {
        const val TYPE = "type"
        const val HEADERS = "headers"
    }

    private val type = javaClass.name

    open fun getHeaders(): List<Header> = listOf(Header(TYPE, type))

    //todo replace with Map?
    data class Header(val name: String, val value: String, val _id: String? = null)

    override fun toString(): String {
        return "$type(f='$f', b='$b', type='$type', headers:\n  " + getHeaders() + "\n)"
    }
}

abstract class T (f: String, b: String): M(f, b) {
    var fw: String? = null

    init {
        if (deserializedHeaders?.contains("fw") ?: false) fw = deserializedHeaders?.get("fw")
    }
    constructor(fw: String, f: String, b: String): this(f, b) {
        this.fw = fw
    }

    override fun getHeaders(): List<Header> {
        val fw = this.fw
        return if (fw != null) super.getHeaders() + Header("fw", fw) else super.getHeaders()
    }
}

class TR(fw: String, f: String, b: String) : T(fw, f, b)

class TE(fw: String, f: String, b: String) : T(fw, f, b)
