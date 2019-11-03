package com.oriole.motaclient.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.mmscomputing.device.scanner.Scanner;
import uk.co.mmscomputing.device.scanner.ScannerIOException;
import uk.co.mmscomputing.device.scanner.ScannerIOMetadata;
import uk.co.mmscomputing.device.scanner.ScannerListener;
import uk.co.mmscomputing.device.twain.jtwain;


import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

import static com.oriole.motaclient.Constant.ScannerFileSavePath;

public class ScannerTWAIN implements ScannerListener {

    private static final long serialVersionUID = 1L;
    private Scanner scanner;
    private static final Logger logger = LoggerFactory.getLogger(ScannerTWAIN.class);

    private Byte scanFinishFlag=0;
    private String fileName = null;

    /**
     * This is the default constructor
     */
    public ScannerTWAIN() {
        super();
        try {
            scanner = Scanner.getDevice();
            scanner.addListener(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* Get the scan! */
    public boolean getScan(String fileName) {
        this.fileName=fileName;
        try {
            jtwain.getSource().setShowUI(false);
            scanner.acquire();
            while (true){
                if(scanFinishFlag==1){
                   return true;
                }else if (scanFinishFlag==-1){
                    return false;
                }else{
                    Thread.sleep(1000);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void update(ScannerIOMetadata.Type var1, ScannerIOMetadata var2) {
        if (var1.equals(ScannerIOMetadata.ACQUIRED)) {
            logger.info("[MOTA Client] Scanner State: ACQUIRED");
            BufferedImage var3 = var2.getImage();
            try {
                ImageIO.write(var3, "png", new File(ScannerFileSavePath + fileName + ".png"));
            } catch (Exception var5) {

            }
        } else if (var1.equals(ScannerIOMetadata.NEGOTIATE)) {
            logger.info("[MOTA Client] Scanner State: NEGOTIATE");
//            ScannerDevice var6 = var2.getDevice();
            BufferedImage var3 = var2.getImage();
            try {
                ImageIO.write(var3, "png", new File(ScannerFileSavePath + fileName + ".png"));
            } catch (Exception var5) {
            }
        } else if (var1.equals(ScannerIOMetadata.STATECHANGE)) {
            logger.info("[MOTA Client] Scanner State: STATECHANGE");
            if (var2.isFinished()) {
                scanFinishFlag=1;
                logger.info("[MOTA Client] Scanner State: FINISH");
            }
        } else if (var1.equals(ScannerIOMetadata.EXCEPTION)) {
            scanFinishFlag=-1;
            var2.getException().printStackTrace();
        }

    }

}