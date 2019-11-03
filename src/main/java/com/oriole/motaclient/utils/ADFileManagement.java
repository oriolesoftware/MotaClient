package com.oriole.motaclient.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import javafx.util.Pair;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import static com.oriole.motaclient.Constant.*;
import static com.oriole.motaclient.utils.CommonUtils.*;

public class ADFileManagement {
    //单例模式
    private static ADFileManagement instance = new ADFileManagement();

    private ADFileManagement() {
    }

    public static ADFileManagement getInstance() {
        return instance;
    }

    private ArrayList<String> pendingDeleteADList =new ArrayList<>();
    private ArrayList<String> pendingDownloadADList=new ArrayList<>();

    private JSONArray newUpdateADList=new JSONArray();
    private JSONArray validAllADList=new JSONArray();

    private JSONArray marginImg=new JSONArray();
    private JSONArray wholePageImg=new JSONArray();
    private JSONArray controlScreenImg=new JSONArray();
    private JSONArray adScreenImgOrVideo=new JSONArray();

    {
        try {
            validAllADList = JSONArray.parseArray(JSONFileIO.ReadFile(DownloadADSavePath + "ADList.json"));
            classifyAD();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void classifyAD(){
        for (int i = 0; i < validAllADList.size(); i++) {
            JSONObject adTemp=validAllADList.getJSONObject(i);
            JSONObject adMode=(JSONObject)adTemp.get("advertisementType");
            switch (adMode.getInteger("mode")){
                case 1:
                    marginImg.add(adTemp);
                    break;
                case 2:
                    wholePageImg.add(adTemp);
                    break;
                case 3:
                    controlScreenImg.add(adTemp);
                    break;
                case 4:
                case 5:
                    adScreenImgOrVideo.add(adTemp);
                    break;
            }
        }
    }

    private void downloadNewAD(ArrayList<String> fileList) throws Exception{
        for (int i = 0; i < fileList.size(); i++) {
            try {
                DownloadFromServer(DownloadADSavePath + fileList.get(i), Url + DownloadFileUrl + "fileName=" + fileList.get(i) + "&location=ADPicUpload");
            } catch (Exception e) {
                this.pendingDownloadADList.add(fileList.get(i));
                e.printStackTrace();
                throw e;
            }
        }
        if(!pendingDownloadADList.isEmpty()) {
            //对失败等待列表的广告再次进行下载
            for (int i = 0; i < this.pendingDownloadADList.size(); i++) {
                try {
                    DownloadFromServer(DownloadADSavePath + this.pendingDownloadADList.get(i), Url + DownloadFileUrl + "fileName=" + this.pendingDownloadADList.get(i) + "&location=ADPicUpload");
                    this.pendingDownloadADList.remove(i);
                } catch (Exception e) {
                }
            }
            if (pendingDownloadADList.isEmpty()) {
                validAllADList = newUpdateADList;
                classifyAD();
                JSONFileIO.WriteFile(validAllADList, DownloadADSavePath, "ADList.json");
            }
        }
    }

    private void deleteOldAD(ArrayList<String> fileList) throws Exception{
        //本次置旧的广告下次处理删除
        for (int i = 0; i < this.pendingDeleteADList.size(); i++) {
            deleteFile(DownloadADSavePath+this.pendingDeleteADList.get(i));
        }
        this.pendingDeleteADList = fileList;
    }

    private boolean checkADUpdate() throws Exception{
        if(pendingDownloadADList.isEmpty()) {
            JSONObject data = new JSONObject(true);
            data.put("deviceID", DeviceID);
            String getJsonString = getResponse(Url + UpdateADUrl, data);
            JSONObject jsonObject = JSONObject.parseObject(getJsonString);
            if (jsonObject.get("state").equals("SUCCESS") && jsonObject.get("code").equals("1")) {
                this.newUpdateADList = (JSONArray) jsonObject.get("msg");
                return true;
            } else {
                return false;
            }
        }else {
            System.out.println("[MOTA Client] Advertisements downloading");
            return false;
        }
    }

    private void doADUpdate(ArrayList<String> needDownloadADList, ArrayList<String> needDeleteADList) throws Exception{
        //不传入needDeleteADList则单独删除待删除内容，传入则先删除待删除内容后缓存本次内容
        deleteOldAD(needDeleteADList);
        //不传入needDownloadADList则单独检查是否需要重新下载，传入则先下载需要下载的内容，然后下载未完成内容
        downloadNewAD(needDownloadADList);
        System.out.println("[MOTA Client] Advertisements have been updated!");
    }

    public void UpdateADList() throws Exception {
        ArrayList<String> needDeleteADList = new ArrayList<>();
        ArrayList<String> needDownloadADList = new ArrayList<>();
        if (checkADUpdate()) {

            JSONArray adListOld = validAllADList;
            JSONArray adListNew = newUpdateADList;


            Set<JSONObject> sameItem = new HashSet<JSONObject>();
            Set<JSONObject> temp = new HashSet<JSONObject>();

            if (adListOld != null) {
                for (int i = 0; i < adListOld.size(); i++) {
                    temp.add(JSON.parseObject(adListOld.getJSONObject(i).toString(), Feature.OrderedField));
                }
                for (int j = 0; j < adListNew.size(); j++) {
                    JSONObject jsonObject1 = JSON.parseObject(adListNew.getJSONObject(j).toString(), Feature.OrderedField);
                    if (temp.add(jsonObject1)) {
                        needDownloadADList.add((String) jsonObject1.get("adPicFile"));
                    } else {
                        sameItem.add(jsonObject1);
                    }
                }
                for (int i = 0; i < adListOld.size(); i++) {
                    JSONObject jsonObject2 = JSON.parseObject(adListOld.getJSONObject(i).toString(), Feature.OrderedField);
                    if (sameItem.add(jsonObject2)) {
                        needDeleteADList.add((String) jsonObject2.get("adPicFile"));
                    }
                }
            } else {
                for (int j = 0; j < adListNew.size(); j++) {
                    needDownloadADList.add((String) adListNew.getJSONObject(j).get("adPicFile"));
                }
            }
            System.out.println("[MOTA Client] Advertising need to be updated!");
        } else {
            System.out.println("[MOTA Client] Advertising need not to be updated!");
        }
        doADUpdate(needDownloadADList, needDeleteADList);
    }

    public JSONArray getMarginImg() {
        if(marginImg.size()!=0) {
            return marginImg;
        }else{
            JSONArray jsonArray = new JSONArray();
            for(int i=1;i<=defaultMarginImgCount;i++) {
                JSONObject jsonObject=new JSONObject();
                jsonObject.put("adPicFile", "defaultMarginImg_"+i+".png");
                jsonArray.add(jsonObject);
            }
            return jsonArray;
        }
    }

    public JSONArray getWholePageImg() {
        if(wholePageImg.size()!=0) {
            return wholePageImg;
        }else{
            JSONArray jsonArray = new JSONArray();
            for(int i=1;i<=defaultWholePageImgCount;i++) {
                JSONObject jsonObject=new JSONObject();
                jsonObject.put("adPicFile", "defaultWholePageImg_"+i+".png");
                jsonArray.add(jsonObject);
            }
            return jsonArray;
        }
    }

    public JSONArray getControlScreenImg() {
        if(controlScreenImg.size()!=0) {
            return controlScreenImg;
        }else{
            JSONArray jsonArray = new JSONArray();
            for(int i=1;i<=defaultControlScreenImgCount;i++) {
                JSONObject jsonObject=new JSONObject();
                jsonObject.put("adPicFile", "defaultControlScreenImg_"+i+".png");
                jsonObject.put("adPlayerDuration", "10");
                jsonArray.add(jsonObject);
            }
            return jsonArray;
        }
    }

    public JSONArray getAdScreenImgOrVideo() {
        if(adScreenImgOrVideo.size()!=0) {
            return adScreenImgOrVideo;
        }else{
            JSONArray jsonArray = new JSONArray();
            for(int i=1;i<=defaultADScreenImgOrVideoCount;i++) {
                JSONObject jsonObject=new JSONObject();
                jsonObject.put("adPicFile", "defaultADScreenImgOrVideo"+i+".png");
                jsonObject.put("adPlayerDuration", "10");
                jsonObject.put("advertisementType", "{\"mode\":4}");
                jsonArray.add(jsonObject);
            }
            return jsonArray;
        }
    }
}