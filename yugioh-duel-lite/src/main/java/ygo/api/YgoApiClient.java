package ygo.api;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import ygo.model.Card;

import javax.imageio.ImageIO;
import java.awt.Image;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Set;

public class YgoApiClient {
    private static final String RANDOM_CARD_URL = "https://db.ygoprodeck.com/api/v7/randomcard.php";
    private static final String USER_AGENT = "YuGiOhDuelLite/1.0 (laboratorio Univalle)";
    /** randomcard.php devuelve cualquier carta (también Spell/Trap): se reintenta hasta obtener un Monster. */
    private static final int MAX_ATTEMPTS = 30;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public Card fetchRandomMonster(Set<Integer> excludedIds) throws YgoApiException {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            JSONObject json = requestRandomCard();
            String type = json.optString("type", "");
            if (!type.contains("Monster")) continue;           // validación: solo cartas Monster
            if (excludedIds.contains(json.optInt("id", -1))) continue;
            try {
                return toCard(json);
            } catch (JSONException e) {
                throw new YgoApiException("No se pudo cargar la carta: datos incompletos en la respuesta.", e);
            }
        }
        throw new YgoApiException("No se pudo cargar la carta: la API no entregó una carta Monster válida.");
    }

    private JSONObject requestRandomCard() throws YgoApiException {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(RANDOM_CARD_URL))
                    .timeout(Duration.ofSeconds(15))
                    .header("Accept", "application/json")
                    .header("User-Agent", USER_AGENT)
                    .GET()
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());

            int code = response.statusCode();
            if (code == 429) {
                throw new YgoApiException("La API recibió demasiadas solicitudes (HTTP 429). Espera un momento y reintenta.");
            }
            if (code != 200) {
                throw new YgoApiException("No se pudo cargar la carta (HTTP " + code + ").");
            }
            return extractCard(response.body());
        } catch (IOException e) {
            throw new YgoApiException("Error de red: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new YgoApiException("La consulta fue interrumpida.", e);
        } catch (JSONException e) {
            throw new YgoApiException("No se pudo cargar la carta: respuesta inválida de la API.", e);
        }
    }

    /** La API responde {"data":[{carta}]}; también se toleran un objeto o un arreglo directos. */
    private JSONObject extractCard(String body) {
        Object root = new JSONTokener(body).nextValue();
        JSONArray array = null;
        if (root instanceof JSONArray) {
            array = (JSONArray) root;
        } else if (root instanceof JSONObject) {
            JSONObject obj = (JSONObject) root;
            if (!obj.has("data")) return obj;
            array = obj.getJSONArray("data");
        }
        if (array == null || array.length() == 0) throw new JSONException("respuesta vacía");
        return array.getJSONObject(0);
    }

    /** Convierte el JSON de la API en nuestro modelo Card y descarga su imagen. */
    private Card toCard(JSONObject json) {
        int id = json.getInt("id");
        String name = json.getString("name");
        String type = json.optString("type", "Monster");
        int atk = Math.max(0, json.optInt("atk", 0));
        int def = Math.max(0, json.optInt("def", 0)); // los Link Monsters no traen "def"

        String imageUrl = "";
        JSONArray images = json.optJSONArray("card_images");
        if (images != null && images.length() > 0) {
            JSONObject first = images.getJSONObject(0);
            // La versión "small" (168x246) es más liviana y alcanza para mostrarla en la ficha
            imageUrl = first.optString("image_url_small", first.optString("image_url", ""));
        }
        Image image = imageUrl.isEmpty() ? null : downloadImage(imageUrl);
        return new Card(id, name, type, atk, def, imageUrl, image);
    }

    /** Descarga la imagen; si falla devuelve null (la carta se muestra sin ilustración). */
    private Image downloadImage(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .header("User-Agent", USER_AGENT)
                    .GET()
                    .build();
            HttpResponse<byte[]> response = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200) return null;
            return ImageIO.read(new ByteArrayInputStream(response.body()));
        } catch (IOException e) {
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }
}
