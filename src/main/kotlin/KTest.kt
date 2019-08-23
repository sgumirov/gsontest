import com.google.gson.GsonBuilder

fun main(args: Array<String>){
    val l = listOf(TR("for_rec", "from_friend", "thankz"),
            TE("for_response", "another_friend", "thankz for repsonse1"))
    val builder = GsonBuilder()
    val s = builder.setPrettyPrinting().create().toJson(l)
    print(s)
}

open class M(val f: String, val b: String) {
    val type = javaClass.simpleName
}

abstract class T (val fw: String, f: String, b: String): M(f, b)

class TR(fw: String, f: String, b: String) : T(fw, f, b)

class TE(fw: String, f: String, b: String) : T(fw, f, b)
