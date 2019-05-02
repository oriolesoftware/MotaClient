package com.oriole.motaclient.websocket;

import com.alibaba.fastjson.JSON;
import com.oriole.motaclient.entity.MsgEntity;
import com.oriole.motaclient.utils.PdfSplicing;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import static com.oriole.motaclient.Constant.PdfPagePicSavePath;
import static com.oriole.motaclient.Constant.PrintFileSavePath;

/**
 * 预加载Websocket处理类
 * 注意，此类是专用类，不具备泛化可能
 *
 * @author NeoSunJz
 * @version V1.0.1 Beta
 */
@Component
@ServerEndpoint("/PreviewLoading/{FileName}")
public class PreviewLoadingWSServer {
    private Session session;
    private static PreviewLoadingWSServer previewLoadingWSServer;

    @OnOpen
    public void onOpen(Session session,@PathParam("FileName") String FileName) {

        this.session = session;
        previewLoadingWSServer=this;
        PdfToImg(FileName);
    }

    @OnClose
    public void onClose() {
        this.session = null;
    }

    @OnError
    public void onError(Throwable e) {
        e.printStackTrace();
    }

    static public void pushBySys(String Msg) {
        try {
            previewLoadingWSServer.session.getBasicRemote().sendText(Msg);
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    /**
     * 此方法用于将PDF文件转为图片并存入临时文件夹
     *
     * @param fileName 待处理PDF文件名（文件需位于PdfPagePicSavePath内）
     */
    private static void PdfToImg(String fileName){
        MsgEntity msgEntity;
        File checkFolderExists = new File(PdfPagePicSavePath);
        if (!checkFolderExists.getAbsoluteFile().exists()) {
            checkFolderExists.getAbsoluteFile().mkdirs();
        }
        try {
            File file = new File(PrintFileSavePath+fileName+".pdf");
            ArrayList<byte[]> imageList = new ArrayList<byte[]>();
            PDDocument pdf = PDDocument.load(file);
            int pageCount = pdf.getNumberOfPages();

            //向控制端汇报需要完成的总数
            msgEntity = new MsgEntity("SUCCESS", "01", String.valueOf(pageCount));
            PreviewLoadingWSServer.pushBySys(JSON.toJSONString(msgEntity));

            PDFRenderer renderer = new PDFRenderer(pdf);
            for (int page = 0; page < pageCount; page++) {
                BufferedImage image = renderer.renderImageWithDPI(page, 200, ImageType.GRAY);
                ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
                ImageIO.write(image, "png", new File(PdfPagePicSavePath + fileName + "_" + page + ".png"));

                //向控制端汇报当前完成的页
                msgEntity = new MsgEntity("SUCCESS", "02", String.valueOf(page+1));
                PreviewLoadingWSServer.pushBySys(JSON.toJSONString(msgEntity));

            }
            pdf.close();
        }catch (IOException e){
            msgEntity = new MsgEntity("ERROR", "-1", e.toString());
            PreviewLoadingWSServer.pushBySys(JSON.toJSONString(msgEntity));
        }
    }
}
