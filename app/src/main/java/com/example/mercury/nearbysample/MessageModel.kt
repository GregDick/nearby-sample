package com.example.mercury.nearbysample

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.ObjectInput
import java.io.ObjectInputStream
import java.io.ObjectOutput
import java.io.ObjectOutputStream
import java.io.Serializable

class MessageModel(val who: String?, val text: String?) : Serializable {

    override fun toString(): String {
        return "MessageModel{" +
                "who='" + who + '\''.toString() +
                ", text='" + text + '\''.toString() +
                '}'.toString()
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false

        val that = o as MessageModel?

        if (if (who != null) who != that!!.who else that!!.who != null) return false
        return if (text != null) text == that.text else that.text == null
    }

    override fun hashCode(): Int {
        var result = who?.hashCode() ?: 0
        result = 31 * result + (text?.hashCode() ?: 0)
        return result
    }

    companion object {

        fun empty(): MessageModel {
            return MessageModel("", "")
        }

        @Throws(IOException::class)
        fun convertToBytes(messageModel: MessageModel): ByteArray {
            ByteArrayOutputStream().use { bos ->
                ObjectOutputStream(bos).use { out ->
                    out.writeObject(messageModel)
                    return bos.toByteArray()
                }
            }
        }

        @Throws(IOException::class, ClassNotFoundException::class)
        fun convertFromBytes(bytes: ByteArray): MessageModel {
            ByteArrayInputStream(bytes).use { bis -> ObjectInputStream(bis).use { `in` -> return `in`.readObject() as MessageModel } }
        }
    }
}
