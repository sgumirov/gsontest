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
    val builder = GsonBuilder().setPrettyPrinting()
    val s = builder.create().toJson(l)
    println("serialize list: " + s)
    val listType = object : TypeToken<List<M>>() { }.type
    val gsonParser = builder.registerTypeAdapter(M::class.java, MDeserializer())
            .create()
    gsonParser.fromJson<List<M>>(s, listType).forEach{ println("deserialize: $it") }
}

fun deserializeUnknownTypeTest(){
    val builder = GsonBuilder().setPrettyPrinting()
    val listType = object : TypeToken<List<M>>() { }.type
    val gsonParser = builder.registerTypeAdapter(M::class.java, MDeserializer())
            .create()
    val unknownTypeJson = """[
      {
        "fw": "for_response",
        "headers":{"type": "TUnknown"},
        "f": "another_friend",
        "b": "thankz for repsonse1"
      }
    ]"""
    println("deserialize unknown type: ")
    gsonParser.fromJson<List<M>>(unknownTypeJson, listType).forEach{
        println(it)
        println("-> serialize: ")
        println(builder.create().toJson(it))
    }
}

class MDeserializer: JsonDeserializer<M> {
    companion object {
        val whitelist = hashSetOf("TR", "TE")
    }

    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): M {
        val jobj = json!!.asJsonObject //?: return null
        val klass = jobj[M.HEADERS]?.asJsonObject?.get(M.TYPE)?.asString
        if (!whitelist.contains(klass)) {
            return Gson().fromJson(jobj, M::class.java).also { it.headers[M.TYPE] = klass ?: "M" }
        }
        return context!!.deserialize(jobj, Class.forName(klass))
    }
}

open class M(val f: String? = null, val b: String? = null) {
    @Expose(serialize = false)
    private val type = javaClass.name

    var headers: MutableMap<String, String> = mutableMapOf(Pair(TYPE, type))

    override fun toString(): String {
        return "$type(f='$f', b='$b', type='$type', headers:\n  $headers\n)"
    }

    companion object {
        const val TYPE = "type"
        const val HEADERS = "headers"
    }
}

abstract class T (val fw: String, f: String, b: String): M(f, b)

class TR(fw: String, f: String, b: String) : T(fw, f, b)

class TE(fw: String, f: String, b: String) : T(fw, f, b)
