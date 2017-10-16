package com.example.mercury.nearbysample;

/**
 * Created by mercury on 10/16/17.
 */

public class Endpoint {

    final String id;
    final String name;

    public Endpoint(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

}
