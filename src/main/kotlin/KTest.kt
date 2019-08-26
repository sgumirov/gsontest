import com.google.gson.*
import com.google.gson.annotations.Expose
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

fun main(args: Array<String>){
//    serializeDeserializeTest()
    deserializeUnknownTypeTest()
}

fun serializeDeserializeTest() {
    val l = listOf(TR("for_rec", "from_friend", "thankz"),
            TE("for_response", "another_friend", "thankz for repsonse1"))
    val serializer = GsonBuilder()
            .registerTypeAdapter(M::class.java, MDeserializer())
            .excludeFieldsWithoutExposeAnnotation()
            .setPrettyPrinting().create()
    val s = serializer.toJson(l)
    println("serialize list: " + s)
    val listType = object : TypeToken<List<M>>() { }.type
    serializer.fromJson<List<M>>(s, listType).forEach{ println("deserialize: $it") }
}

fun deserializeUnknownTypeTest(){
    val listType = object : TypeToken<List<M>>() { }.type
    val gson = GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .setPrettyPrinting()
            .registerTypeAdapter(M::class.java, MDeserializer())
            .setPrettyPrinting()
            .create()
    val unknownTypeJson = """[
      {
        "fw": "for_response",
        "headers":[
          {
            "name": "type",
            "value": "TUnknown"
          }
        ],
        "f": "another_friend",
        "b": "thankz for repsonse1"
      }
    ]"""
    println("deserialize unknown type: ")
    gson.fromJson<List<M>>(unknownTypeJson, listType).forEach{
        println(it)
        println("-> serialize: ")
        println(gson.toJson(it))
    }
}

class MDeserializer: JsonDeserializer<M> {
    companion object {
        val whitelist = hashSetOf("TR", "TE")
    }

    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): M {
        val jobj = json!!.asJsonObject
        val klass = getHeaderValue(jobj, M.TYPE) ?: M::class.java.name
        if (!whitelist.contains(klass)) {
            return Gson().fromJson(jobj, M::class.java).also { m -> m.setHeaderValue(M.TYPE, klass ?: M::class.java.name) }
        }
        return context!!.deserialize(jobj, Class.forName(klass))
    }

    fun getHeaderValue(jobj: JsonObject, name: String): String? = jobj[M.HEADERS]?.asJsonArray?.find {
        it.asJsonObject?.get("name")?.asString.equals(name)
    }?.asJsonObject?.get("value")?.asString
}

open class M(@Expose val f: String? = null, @Expose val b: String? = null) {
    companion object {
        const val TYPE = "type"
        const val HEADERS = "headers"
    }

    private val type = javaClass.name

    @Expose val headers: MutableList<Header> = mutableListOf(Header(TYPE, type))

    fun setHeaderValue(name: String, value: String) {
        val header = Header(name, value)
        with(headers.find { it.name == name }) {
            this ?: return@with
            headers.remove(this)
        }
        headers.add(header)
    }

    override fun toString(): String {
        return "$type(f='$f', b='$b', type='$type', headers:\n  $headers\n)"
    }

    data class Header(val name: String, val value: String, val _id: String? = null)
}

abstract class T (@Expose val fw: String, f: String, b: String): M(f, b)

class TR(fw: String, f: String, b: String) : T(fw, f, b)

class TE(fw: String, f: String, b: String) : T(fw, f, b)
