package com.oriole.motaclient.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.oriole.motaclient.utils.ADFileManagement;
import com.oriole.motaclient.utils.JSONFileIO;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import java.io.InputStream;

import static com.oriole.motaclient.Constant.DownloadADSavePath;
import static com.oriole.motaclient.utils.ADPlayer.*;

public class ADPlayController extends Application {

    private ADFileManagement adFileManagement= ADFileManagement.getInstance();

    public ADPlayController() {
    }

    public void open() {
        launch();
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        ADFileManagement adFileManagement= ADFileManagement.getInstance();

        ImageView imageView=new ImageView();
        InputStream inputStream = this.getClass().getResourceAsStream("/image/LogoPic.png");
        imageView.setImage(new Image(inputStream));
        Pane pane = new Pane();
        pane.getChildren().add(imageView);
        imageView.fitHeightProperty().bind(pane.heightProperty());
        imageView.fitWidthProperty().bind(pane.widthProperty());
        Scene scene = new Scene(pane);
        primaryStage.setScene(scene);
        showInADScreen(primaryStage).show();

        JSONArray jsonArray = adFileManagement.getAdScreenImgOrVideo();
        //4 - 图片    5 - 视频
        for (int i = 0; i%jsonArray.size() < jsonArray.size(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i%jsonArray.size());
            String filePath = "file:/" + DownloadADSavePath + jsonObject.getString("adPicFile");
            Integer adPlayerDuration=jsonObject.getIntValue("adPlayerDuration") * 1000;
            switch (jsonObject.getJSONObject("advertisementType").getInteger("mode")) {
                case 4:
                    startImageTask(new Stage(),filePath, adPlayerDuration);
                    break;
                case 5:
                    startMediaTask(new Stage(),filePath, adPlayerDuration);
                    break;
            }
            if(i%jsonArray.size()==jsonArray.size()-1){
                jsonArray = adFileManagement.getAdScreenImgOrVideo();
            }
        }
    }

}
