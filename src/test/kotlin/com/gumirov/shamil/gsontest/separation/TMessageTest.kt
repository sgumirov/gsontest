package com.gumirov.shamil.gsontest.separation

import org.junit.Assert
import org.junit.Test
import java.util.*

class TMessageTest {
    @Test
    fun testConversion(){
        val date = Message.encodeDate(Date())
        val json = """{
            "headers":{
                "type": "com.gumirov.shamil.gsontest.separation.Enquiry",
                "profession": "software engineer",
                "urgent": "true"
            },
            "from": "another_friend",
            "body": "need a brogrammer",
            "date": "$date",
            "_id": "id123"
          }"""
        val gson = createJsonParser()
        val tm = gson.fromJson<TMessage>(json, TMessage::class.java)
        println("parsed="+tm)
        Assert.assertEquals("id123", tm.id)

        val whitelist = setOf(Enquiry::class.java.name)
        val converter = Transformer(whitelist)

        val e = converter.deserialize(tm)
        Assert.assertEquals("id123", e?.id)
        println("enquiry="+e)

        val tm2 = converter.serialize(e!!)
        Assert.assertEquals("id123", tm2?.id)
        println("TMsg2 = "+tm2)

        val json2 = gson.toJson(tm2)
        println("json2 = "+json2)
        val tm3 = gson.fromJson<TMessage>(json2, TMessage::class.java)
        Assert.assertEquals(tm3, tm)
    }
}