package com.gumirov.shamil.gsontest.separation

import org.joda.time.DateTime
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.util.*

class NetMessageTest {
    var gson = createJsonParser()
    var now = Date()
    var whitelist = setOf(Enquiry::class.java.name)
    var transformator = Transformer(whitelist)

    @Before
    fun setup() {
        gson = createJsonParser()
        now = Date()
        whitelist = setOf(Enquiry::class.java.name)
        transformator = Transformer(whitelist)
    }

    @Test
    fun testConversionFromJson(){
        val date = Message.encodeDate(now)
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
        val tm = gson.fromJson<NetMessage>(json, NetMessage::class.java)
        Assert.assertEquals("id123", tm.id)

        val e = transformator.deserialize(tm)
        Assert.assertEquals("id123", e.id)

        val tm2 = transformator.serialize(e)
        Assert.assertEquals("id123", tm2.id)

        val json2 = gson.toJson(tm2)
        val tm3 = gson.fromJson<NetMessage>(json2, NetMessage::class.java)

        Assert.assertEquals(tm3, tm)
        Assert.assertEquals(now, e.date?.toDate())
        Assert.assertEquals(DateTime(now), e.date)
        Assert.assertEquals(Message.encodeDate(DateTime(now)), tm3.date)
        Assert.assertEquals(Message.encodeDate(now), tm3.date)
    }

    @Test
    fun testConversionFromEnquiry(){
        val e4 = Enquiry("body", "prof", true, "to", "from", now, "id123")
        val tm4 = transformator.serialize(e4)
        Assert.assertEquals("body", tm4.body)
        Assert.assertEquals("from", tm4.from)
        Assert.assertEquals("to", tm4.to)
        Assert.assertEquals("id123", tm4.id)
        val _e4 = transformator.deserialize(tm4)
        Assert.assertEquals(e4, _e4)
        val json4 = gson.toJson(tm4)
        val tm5 = gson.fromJson<NetMessage>(json4, NetMessage::class.java)
        Assert.assertEquals(tm4, tm5)
        val e5 = transformator.deserialize(tm5)
        Assert.assertEquals(e4, e5)
    }
}

class Enquiry(body: String? = null, to: String? = null, from: String? = null, date: Date? = null, id: String? = null)
    : Message(to, from, body, DateTime(date), id)
{
    companion object {
        private const val PROFESSION = "profession"
        private const val URGENT = "urgent"
    }

    lateinit var profession: String
    var isUrgent: Boolean = false

    constructor(body: String?, profession: String, isUrgent: Boolean, to: String?, from: String?, date: Date?, id: String?):
            this(body, to, from, date, id)
    {
        this.profession = profession
        this.isUrgent = isUrgent
    }

    override fun deserialize(headers: Map<String, String>) {
        profession = headers.getOrDefault(PROFESSION, "")
        isUrgent = java.lang.Boolean.parseBoolean(headers.get(URGENT))
    }

    override fun serialize(headers: MutableMap<String, String>) {
        headers.put(URGENT, isUrgent.toString())
        headers.put(PROFESSION, profession)
    }

    override fun toString(): String {
        return "Enquiry(profession='$profession', isUrgent=$isUrgent) "+super.toString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Enquiry

        if (profession != other.profession) return false
        if (isUrgent != other.isUrgent) return false
        if (to != other.to) return false
        if (from != other.from) return false
        if (id != other.id) return false
        if (date != other.date) return false

        return true
    }

    override fun hashCode(): Int {
        var result = profession.hashCode()
        result = 31 * result + isUrgent.hashCode()
        result = 31 * result + (from?.hashCode() ?: 0)
        result = 31 * result + (to?.hashCode() ?: 0)
        result = 31 * result + (id?.hashCode() ?: 0)
        result = 31 * result + (date?.hashCode() ?: 0)
        return result
    }
}