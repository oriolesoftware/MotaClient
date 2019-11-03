package com.oriole.motaclient.utils;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaView;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.List;

public class ADPlayer {

    public static void startImageTask(Stage stage, String path, long millis) throws Exception{

        ImageView imageView=new ImageView();
        imageView.setImage(new Image(path));
        Pane pane = new Pane();
        pane.getChildren().add(imageView);
        imageView.fitHeightProperty().bind(pane.heightProperty());
        imageView.fitWidthProperty().bind(pane.widthProperty());

        Scene scene = new Scene(pane);
        stage.setScene(scene);

        Thread thread = new Thread(() -> {
            //控制关闭的线程
            try {
                Thread.sleep(millis);
                if (stage.isShowing()) {
                    Platform.runLater(() -> stage.close());
                }
            } catch (Exception exp) {
                exp.printStackTrace();
            }
        });

        thread.setDaemon(true);
        thread.start();
        showInADScreen(stage).showAndWait();
        System.out.println("[MOTA Client] Image AD is over");
        Platform.exit();
    }

    public static void startMediaTask(Stage stage,String path,long millis) throws Exception{

        javafx.scene.media.MediaPlayer mediaPlayer= new javafx.scene.media.MediaPlayer(new Media(path));

        MediaView mediaPlayerView=new MediaView();
        mediaPlayerView.setMediaPlayer(mediaPlayer);
        Pane pane = new Pane();
        pane.getChildren().add(mediaPlayerView);
        mediaPlayerView.fitHeightProperty().bind(pane.heightProperty());
        mediaPlayerView.fitWidthProperty().bind(pane.widthProperty());

        Scene scene = new Scene(pane);
        stage.setScene(scene);

        Thread thread = new Thread(() -> {
            //控制关闭的线程
            try {
                Thread.sleep(millis);
                if (stage.isShowing()) {
                    Platform.runLater(() -> stage.close());
                }
            } catch (Exception exp) {
                exp.printStackTrace();
            }
        });

        thread.setDaemon(true);
        thread.start();
        mediaPlayer.play();
        showInADScreen(stage).showAndWait();
        System.out.println("[MOTA Client] Video AD is over");
        mediaPlayer.dispose();
        Platform.exit();
    }

    public static Stage showInADScreen(Stage primaryStage){
        primaryStage.initStyle(StageStyle.UNDECORATED);//设定窗口无边框
        List<Screen> screenList=Screen.getScreens();
        primaryStage.setX(screenList.get(screenList.size()-1).getBounds().getMinX());
        primaryStage.setY(screenList.get(screenList.size()-1).getBounds().getMinY());
        primaryStage.setWidth(screenList.get(screenList.size()-1).getBounds().getWidth());
        primaryStage.setHeight(screenList.get(screenList.size()-1).getBounds().getHeight());
        primaryStage.setMaximized(true);
        primaryStage.setAlwaysOnTop(false);

        return primaryStage;
    }
}
