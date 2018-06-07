package ru.kontur.vostok.hercules.cli;

import com.mashape.unirest.http.Unirest;

public class StreamApiClient {

    public static void main(String[] args) throws Exception {

        System.out.println(Unirest.get("http://localhost:6306/ping").asString().getStatus());
        Unirest.shutdown();
    }
}
