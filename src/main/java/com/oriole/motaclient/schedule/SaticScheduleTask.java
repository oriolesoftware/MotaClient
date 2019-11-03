package com.oriole.motaclient.schedule;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import com.oriole.motaclient.utils.ADFileManagement;
import com.oriole.motaclient.utils.JSONFileIO;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import static com.oriole.motaclient.Constant.*;
import static com.oriole.motaclient.utils.CommonUtils.*;

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

    private ADFileManagement adFileManagement= ADFileManagement.getInstance();

    /**
     * 自动更新广告资源（频率：每5分钟）
     * <p>
     * 此方法将从服务器自动获取广告信息、下载广告图片并更新本地的广告存储文件
     * 将从Url+DownloadFileUrl（资源获得链接，服务器）处，利用DeviceID参数获取广告资源
     * 若返回信息为1则解析获得msg内数据获得广告对应信息，下载新增图片并存入DownloadADSavePath，同时删除停用图片
     * 同时将广告对应信息存入DownloadADSavePath下的ADList.json
     * 若返回信息为0则没有广告需要更新
     */
    @Scheduled(cron = "0/30 * * * * ?")
    private void UpdateADScheduleTask() throws Exception {
        adFileManagement.UpdateADList();
    }
}
