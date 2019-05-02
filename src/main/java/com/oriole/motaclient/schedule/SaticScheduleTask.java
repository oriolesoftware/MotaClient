package com.oriole.motaclient.schedule;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.oriole.motaclient.utils.JSONFileIO;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

import static com.oriole.motaclient.Constant.*;
import static com.oriole.motaclient.utils.CommonUtils.DownloadFromServer;
import static com.oriole.motaclient.utils.CommonUtils.getURLContent;

@Component
@Configuration
@EnableScheduling
/**
 * 客户端定时任务执行
 *
 * @author NeoSunJz
 * @version V1.0.1 Beta
 */
public class SaticScheduleTask {

    /**
     * 自动更新广告资源（频率：每1小时）
     * <p>
     * 此方法将从服务器自动获取广告信息、下载广告图片并更新本地的广告存储文件
     * 将从Url+DownloadFileUrl（资源获得链接，服务器）处，利用DeviceID参数获取广告资源
     * 若返回信息为1则解析获得msg内数据获取下载路径地址组，下载图片并存入DownloadADSavePath
     * 同时将路径信息（下载的文件）存入DownloadADSavePath下的Path.json
     * 若返回信息为0则没有广告需要更新
     */
    @Scheduled(cron = "0 0 0/1 1/1 * ?")
    private void UpdateADScheduleTask() {
        String getJsonString = getURLContent(Url + UpdateADUrl + "DeviceID=" + DeviceID);
        JSONObject jsonObject = JSONObject.parseObject(getJsonString);
        if (jsonObject.get("state").equals("SUCCESS")) {
            if (jsonObject.get("code").equals("1")) {
                List<String> PathList = JSONObject.parseArray((String) jsonObject.get("msg"), String.class);
                for (int i = 0; i < PathList.size(); i++) {
                    System.out.println(PathList.get(i));
                    String url = Url + DownloadFileUrl + "fileName=" + PathList.get(i) + "&Location=ADPicUpload";
                    try {
                        DownloadFromServer(DownloadADSavePath + PathList.get(i), url);
                    }catch (Exception e){
                        e.printStackTrace();
                        System.out.println("[MOTA Client] " + e.toString());
                    }
                    PathList.set(i,DownloadADSavePath+PathList.get(i));
                }
                JSONArray path=JSONArray.parseArray(JSON.toJSONString(PathList));
                try {
                    JSONFileIO.WriteFile(path, DownloadADSavePath, "Path.json");
                    System.out.println("[MOTA Client] Advertisements have been updated!");
                } catch (IOException e) {
                    e.printStackTrace();
                    System.out.println("[MOTA Client] " + e.toString());
                }
            }else{
                System.out.println("[MOTA Client] Advertising is not updated!");
            }
        }
    }
}
