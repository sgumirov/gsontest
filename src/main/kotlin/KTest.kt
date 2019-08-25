import com.google.gson.GsonBuilder
import com.google.gson.annotations.Expose

fun main(args: Array<String>){
    val l = listOf(TR("for_rec", "from_friend", "thankz"),
            TE("for_response", "another_friend", "thankz for repsonse1"))
    val builder = GsonBuilder()
    val s = builder.setPrettyPrinting().create().toJson(l)
    print(s)
}

open class M(val f: String, val b: String) {
    @Expose(serialize = false) val type = javaClass.simpleName

    val headers: Map<String, String> = mutableMapOf(Pair(TYPE, type))

    companion object {
        const val TYPE = "type"
    }
}

abstract class T (val fw: String, f: String, b: String): M(f, b)

class TR(fw: String, f: String, b: String) : T(fw, f, b)

class TE(fw: String, f: String, b: String) : T(fw, f, b)
