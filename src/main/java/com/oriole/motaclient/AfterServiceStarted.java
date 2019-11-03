package com.oriole.motaclient;

import com.oriole.motaclient.controller.ADPlayController;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import static com.oriole.motaclient.Constant.ChromeConfig;

@Component
public class AfterServiceStarted implements ApplicationRunner {

    /**
     * 会在服务启动完成后立即执行
     */
    @Override
    public void run(ApplicationArguments args) throws Exception {
        Process process = Runtime.getRuntime().exec(ChromeConfig+"chrome.exe -kiosk http://localhost:8999/controllerView/");
        new ADPlayController().open();
    }
}