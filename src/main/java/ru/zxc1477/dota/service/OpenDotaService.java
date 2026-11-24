package ru.zxc1477.dota.service;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ru.zxc1477.dota.model.PlayerStats;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class OpenDotaService {
    private static final String API_URL = "https://api.opendota.com/api/players/";
    private final HttpClient client = HttpClient.newHttpClient();

    public String extractAccountId(String input) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(\\d{8,10})").matcher(input);
        if (matcher.find()) return matcher.group(1);
        throw new IllegalArgumentException("Не найден ID игрока в ссылке!");
    }

    public PlayerStats getStats(String accountId) throws Exception {
        JsonObject wlData = fetchJson(accountId + "/wl").getAsJsonObject();
        int wins = wlData.get("win").getAsInt();
        int losses = wlData.get("lose").getAsInt();
        int totalGames = wins + losses;
        double totalWinrate = totalGames == 0 ? 0 : (double) wins / totalGames * 100;

        JsonArray recentMatches = fetchJson(accountId + "/recentMatches").getAsJsonArray();
        int recentWins = 0;
        for (JsonElement match : recentMatches) {
            JsonObject m = match.getAsJsonObject();
            boolean radiantWin = m.get("radiant_win").getAsBoolean();
            int playerSlot = m.get("player_slot").getAsInt();
            boolean isRadiant = playerSlot < 128;
            if (radiantWin == isRadiant) recentWins++;
        }
        double recentWinrate = recentMatches.isEmpty() ? 0 : (double) recentWins / recentMatches.size() * 100;

        JsonArray heroes = fetchJson(accountId + "/heroes").getAsJsonArray();
        StringBuilder topHeroes = new StringBuilder();
        for (int i = 0; i < Math.min(3, heroes.size()); i++) {
            topHeroes.append("HeroID: ").append(heroes.get(i).getAsJsonObject().get("hero_id").getAsString()).append(" ");
        }

        JsonArray allMatches = fetchJson(accountId + "/matches").getAsJsonArray();
        long maxBreakSec = 0;
        long breakEndTimeSec = 0;

        for (int i = 0; i < allMatches.size() - 1; i++) {
            long timeCurrent = allMatches.get(i).getAsJsonObject().get("start_time").getAsLong();
            long timePrevious = allMatches.get(i+1).getAsJsonObject().get("start_time").getAsLong();
            long diff = timeCurrent - timePrevious;

            if (diff > maxBreakSec) {
                maxBreakSec = diff;
                breakEndTimeSec = timeCurrent;
            }
        }

        int maxBreakDays = (int) (maxBreakSec / (60 * 60 * 24));
        String breakDate = Instant.ofEpochSecond(breakEndTimeSec)
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));

        return new PlayerStats(totalGames, totalWinrate, recentWinrate, topHeroes.toString().trim(), maxBreakDays, breakDate);
    }

    private JsonElement fetchJson(String endpoint) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL + endpoint))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return JsonParser.parseString(response.body());
    }
}
