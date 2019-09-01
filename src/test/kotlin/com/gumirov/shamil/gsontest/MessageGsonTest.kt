package com.gumirov.shamil.gsontest

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import org.junit.Assert
import org.junit.Test

import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat

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

class MessageGsonTest {
    companion object {
        val whitelist = hashSetOf(TR::class.java.name, TE::class.java.name)
    }
    @Test
    fun serializationTest() {
        val l = listOf(
                TR("for_rec", "from_friend", "thankz", "link"),
                TE("for_response", "another_friend", "thankz for repsonse1"),
                Message("from", "body")
        )
        val gson = GsonBuilder()
                .registerTypeAdapter(Message::class.java, MessageDeserializer(whitelist))
                .registerTypeAdapterFactory(MessageAdapterFactory())
                .excludeFieldsWithoutExposeAnnotation()
                .setPrettyPrinting()
                .create()
        val s = gson.toJson(l)

        println("serialize list: " + s)

        val parser = GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .create()
        val listType = object : TypeToken<List<JsonObject>>() {}.type
        val parsed = parser.fromJson<List<JsonObject>>(s, listType)
        parsed.forEach { Assert.assertNotNull(it.get("date")) }

        Assert.assertEquals(TR::class.java.name, parsed[0].getAsJsonObject("headers").get(Message.TYPE).asString)
        Assert.assertEquals("thankz", parsed[0].get("body").asString)
        Assert.assertEquals("from_friend", parsed[0].get("from").asString)
        Assert.assertEquals("for_rec", parsed[0].getAsJsonObject("headers").get("fw").asString)
        Assert.assertEquals("link", parsed[0].getAsJsonObject("headers").get("link").asString)

        Assert.assertEquals(TE::class.java.name, parsed[1].getAsJsonObject("headers").get(Message.TYPE).asString)
        Assert.assertEquals("thankz for repsonse1", parsed[1].get("body").asString)
        Assert.assertEquals("another_friend", parsed[1].get("from").asString)
        Assert.assertEquals("for_response", parsed[1].getAsJsonObject("headers").get("fw").asString)

        Assert.assertEquals(Message::class.java.name, parsed[2].getAsJsonObject("headers").get(Message.TYPE).asString)
        Assert.assertEquals("body", parsed[2].get("body").asString)
        Assert.assertEquals("from", parsed[2].get("from").asString)
    }

    @Test
    fun deserializeTest() {
        val listType = object : TypeToken<List<Message>>() {}.type
        val gson = GsonBuilder()
                .registerTypeAdapter(Message::class.java, MessageDeserializer(whitelist))
                .registerTypeAdapterFactory(MessageAdapterFactory())
                .excludeFieldsWithoutExposeAnnotation()
                .setPrettyPrinting()
                .create()
        val unknownTypeJson = """[
          {
            "headers":{
                "type": "TUnknown",
                "fw": "for_response",
                "link": "a8724sd34r"
            },
            "from": "another_friend",
            "body": "thankz for repsonse1"
          },
          {
            "headers":{
                "fw": "for_response",
                "link": "a8724sd34r"
            },
            "from": "another_friend",
            "body": "thankz for repsonse1"
          }
        ]"""
        println("deserialize unknown type: ")
        gson.fromJson<List<Message>>(unknownTypeJson, listType).forEach { m ->
            println(m)
            assertThat(m.headers.get("type"), anyOf(equalTo("TUnknown"), equalTo(Message::class.java.name)))
            Assert.assertEquals("headers length", 3, m.headers.size)
            println("-> serialize: ")
            println(gson.toJson(m))
        }

        val json = """[{
                "headers":{
                  "type": "com.gumirov.shamil.gsontest.TR",
                  "fw": "for_response",
                  "link": "a8724sd34r"
                },
                "from": "another_friend",
                "body": "thankz for repsonse1"
              }
            |]""".trimMargin()
        val parsed = gson.fromJson<List<Message>>(json, listType)
        Assert.assertEquals(TR::class.java, parsed[0]::class.java)
        Assert.assertEquals("for_response", (parsed[0] as TR).fw)
        Assert.assertEquals("a8724sd34r", parsed[0].headers.get("link"))
        Assert.assertEquals("a8724sd34r", (parsed[0] as TR).link)
        Assert.assertEquals("another_friend", parsed[0].from)
        Assert.assertEquals("thankz for repsonse1", parsed[0].body)
    }
}