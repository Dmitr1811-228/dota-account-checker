package ru.zxc1477.dota;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import ru.zxc1477.dota.model.PlayerStats;
import ru.zxc1477.dota.service.OpenDotaService;

public class MainApp extends Application {

    private final OpenDotaService apiService = new OpenDotaService();

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Dota 2 Account Checker");

        VBox root = new VBox(10);
        root.setPadding(new Insets(20));

        Label instructionLabel = new Label("Введи ссылку на профиль (Dotabuff / SteamID):");
        TextField inputField = new TextField();
        Button checkButton = new Button("Проверить");
        TextArea resultArea = new TextArea();
        resultArea.setEditable(false);
        resultArea.setPrefHeight(200);

        checkButton.setOnAction(e -> {
            resultArea.setText("Загрузка данных...");
            new Thread(() -> {
                try {
                    String accountId = apiService.extractAccountId(inputField.getText());
                    PlayerStats stats = apiService.getStats(accountId);

                    String result = String.format("""
                            📊 Статистика аккаунта:
                            Всего игр: %d
                            Общий винрейт: %.2f%%
                            Винрейт за последние 20 игр: %.2f%%
                            Любимые герои (ID): %s
                            Максимальный перерыв: %d дней (закончился %s)
                            """,
                            stats.totalGames(), stats.totalWinrate(), stats.recentWinrate(),
                            stats.topHeroes(), stats.maxBreakDays(), stats.maxBreakDate()
                    );

                    javafx.application.Platform.runLater(() -> resultArea.setText(result));
                } catch (Exception ex) {
                    javafx.application.Platform.runLater(() ->
                            resultArea.setText("Ошибка: " + ex.getMessage())
                    );
                }
            }).start();
        });

        root.getChildren().addAll(instructionLabel, inputField, checkButton, resultArea);
        primaryStage.setScene(new Scene(root, 400, 350));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}