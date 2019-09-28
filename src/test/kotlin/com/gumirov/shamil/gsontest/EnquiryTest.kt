package com.gumirov.shamil.gsontest

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import org.junit.Assert
import org.junit.Test

class EnquiryTest {
    @Test
    fun test(){
        val msg = "I need a fullstack software developer with good knowledge of C++, 5 years in fintech and \"MsCS\"."
        val e = SpecialistEnquiryMessage("software engineer", msg, "to")
        val gson = createMessageGson(hashSetOf(SpecialistEnquiryMessage::class.java.name))
        val serialized = gson.toJson(e)
        println(serialized)
        val parser = GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .create()
        val parsed = parser.fromJson<JsonObject>(serialized, JsonObject::class.java)
        Assert.assertEquals(SpecialistEnquiryMessage::class.java.name, parsed.getAsJsonObject(Message.HEADERS).get(Message.TYPE).asString)
        Assert.assertEquals("software engineer", parsed.getAsJsonObject(Message.HEADERS).get(SpecialistEnquiryMessage.PROFESSION).asString)
        Assert.assertEquals("to", parsed.get("to").asString)

        val deserialized = createMessageGson(hashSetOf(SpecialistEnquiryMessage::class.java.name)).fromJson<Message>(serialized, Message::class.java)
        Assert.assertTrue(deserialized is SpecialistEnquiryMessage)
        Assert.assertEquals("software engineer", (deserialized as SpecialistEnquiryMessage).profession)
        Assert.assertEquals("to", deserialized.to)
        Assert.assertEquals(msg, deserialized.body)
    }
}
