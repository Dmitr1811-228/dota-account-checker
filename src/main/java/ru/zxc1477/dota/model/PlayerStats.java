package ru.zxc1477.dota.model;

public record PlayerStats(
        int totalGames,
        double totalWinrate,
        double recentWinrate,
        String topHeroes,
        int maxBreakDays,
        String maxBreakDate
) {}