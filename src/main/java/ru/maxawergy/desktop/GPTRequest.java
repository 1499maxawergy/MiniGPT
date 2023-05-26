package ru.maxawergy.desktop;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.io.IOException;

public class GPTRequest {

    public static final String API_ENDPOINT = "https://api.openai.com/v1/chat/completions";

    public HttpClient client = HttpClient.newHttpClient();

    public String getCompletion(String token, String prompt) {
        JsonObject jsonBody = new JsonObject();
        jsonBody.addProperty("model", "gpt-3.5-turbo");
        JsonArray messages = new JsonArray();
        JsonObject roleAndContent = new JsonObject();
        roleAndContent.addProperty("role", "user");
        roleAndContent.addProperty("content", prompt);
        messages.add(roleAndContent);
        jsonBody.add("messages", messages);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_ENDPOINT))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody.toString()))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 401) {
                return "Your token is not valid. Change token to valid and try again";
            }
            if (response.statusCode() != 200) {
                throw new RuntimeException("Unexpected code" + response);
            }
            JsonObject jsonObject = new JsonParser().parse(response.body()).getAsJsonObject();
            return jsonObject.get("choices").getAsJsonArray().get(0).getAsJsonObject().get("message").getAsJsonObject().get("content").getAsString();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return "An error has been detected, try to restart application or try again later";
    }

}
