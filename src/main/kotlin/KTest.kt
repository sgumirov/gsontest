import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

fun main(args: Array<String>){
    val l = listOf(TR("for_rec", "from_friend", "thankz"),
            TE("for_response", "another_friend", "thankz for repsonse1"))
    val builder = GsonBuilder()
    val s = builder.setPrettyPrinting().create().toJson(l)
    println(s)

    val listType = object : TypeToken<List<M>>() { }.type
    val gsonParser = builder.registerTypeAdapter(M::class.java, MDeserializer()).create()
    gsonParser.fromJson<List<M>>(s, listType).forEach{ println(it) }
    //unknown type test
    val unknownTypeJson = """[
      {
        "fw": "for_response",
        "type": "TUnknown",
        "f": "another_friend",
        "b": "thankz for repsonse1"
      }
    ]"""
    gsonParser.fromJson<List<M>>(unknownTypeJson, listType).forEach{ println(it) }
}

class MDeserializer: JsonDeserializer<M> {
    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): M {
        val jobj = json!!.asJsonObject //?: return null
        val klass = jobj["type"]?.asString
        if (!whitelist.contains(klass)) return context!!.deserialize(jobj, M::class.java)
        return context!!.deserialize(jobj, Class.forName(klass))
    }
    companion object {
        val whitelist = hashSetOf("TR", "TE", "M")
    }
}

open class M(val f: String, val b: String) {
    val type = javaClass.simpleName
    override fun toString(): String {
        return "$type(f='$f', b='$b', type='$type')"
    }
}

abstract class T (val fw: String, f: String, b: String): M(f, b)

class TR(fw: String, f: String, b: String) : T(fw, f, b)

class TE(fw: String, f: String, b: String) : T(fw, f, b)
