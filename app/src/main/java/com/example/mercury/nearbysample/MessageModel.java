package com.example.mercury.nearbysample;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;

public class MessageModel {

    private final String who;
    private final String text;

    public MessageModel(String who, String text) {
        this.who = who;
        this.text = text;
    }

    public static MessageModel empty(){
        return new MessageModel("", "");
    }

    public String getText() {
        return text;
    }

    public String getWho() {
        return who;
    }

    public static byte[] convertToBytes(MessageModel messageModel) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutput out = new ObjectOutputStream(bos)) {
            out.writeObject(messageModel);
            return bos.toByteArray();
        }
    }

    public static MessageModel convertFromBytes(byte[] bytes) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
             ObjectInput in = new ObjectInputStream(bis)) {
            return (MessageModel) in.readObject();
        }
    }

    @Override
    public String toString() {
        return "MessageModel{" +
                "who='" + who + '\'' +
                ", text='" + text + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MessageModel that = (MessageModel) o;

        if (who != null ? !who.equals(that.who) : that.who != null) return false;
        return text != null ? text.equals(that.text) : that.text == null;
    }

    @Override
    public int hashCode() {
        int result = who != null ? who.hashCode() : 0;
        result = 31 * result + (text != null ? text.hashCode() : 0);
        return result;
    }
}
