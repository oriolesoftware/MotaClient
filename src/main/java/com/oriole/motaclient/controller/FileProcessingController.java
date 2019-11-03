package com.oriole.motaclient.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfReader;
import com.oriole.motaclient.entity.MsgEntity;
import com.oriole.motaclient.utils.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URLDecoder;

import static com.oriole.motaclient.Constant.*;

/**
 * 对获得的文件进行恰当的处理以供打印设备打印或控制端用户进行预览
 * 此类包含一系列方法将提供打印预览、文件预处理、一张多页处理等功能
 * 控制端（VUE-MOTA）将根据情况调用若干方法完成打印文件处理，并为用户提供预览
 * 值得注意的是，preProcessing方法是必须调用的
 *
 * @author NeoSunJz
 * @version V2.1.3 Beta
 */
@EnableAutoConfiguration
@RestController
public class FileProcessingController {
    /**
     * 文件预处理
     * <p>
     * 此方法将根据具体文件的类型，调用相关的处理方法将文件转换为保真的PDF文件
     * 若读取到以DOC,DOCX,PPT,PPTX,ELS,ELSX为后缀的文件，使用{@link FileToPdf}类*2pdf方法调用WPS处理相关文件生成PDF
     * 若读取到以JPG,BMP,PNG为后缀的文件，使用{@link FileToPdf}类png2pdf方法生成PDF
     * 若读取到以PDF为后缀的文件,使用{@link CommonUtils}工具类的copyFile工具直接拷贝PDF
     * 最终保存至PrintFileSavePath下（具体路径参考{@link com.oriole.motaclient.Constant}）
     * 预处理成功后向控制端发送成功返回，否则发送具体错误信息
     *
     * @param fileAbsolutePath 执行端可获取的文件唯一路径
     * @param fileType         文件类型
     * @param randomCode       控制端的单次任务标识串（随机字符串）
     * @return JSON(msgEntity)，成功后msg部分不返回有处理必要的内容
     * @throws Exception
     */
    @RequestMapping(value = "/preProcessing", produces = "application/json; charset=utf-8")
    public MsgEntity preProcessing(@RequestParam String fileAbsolutePath, @RequestParam String fileType, @RequestParam String randomCode) throws Exception {
        //创建FileToPdf工具类
        FileToPdf fileToPdf = new FileToPdf();

        //检查目标文件夹是否存在
        File checkFolderExists = new File(PrintFileSavePath);
        if (!checkFolderExists.getAbsoluteFile().exists()) {
            checkFolderExists.getAbsoluteFile().mkdirs();
        }

        //转换后的pdf文件名
        String pdfFilePath = PrintFileSavePath + randomCode + ".pdf";

        //获得待转换文件名（组）
        //当且仅当文件为图片类型时才会出现文件组
        JSONArray fileAbsolutePathArray = JSONArray.parseArray(URLDecoder.decode(fileAbsolutePath, "utf-8"));

        switch (fileType) {
            case "doc":
            case "docx":
                fileToPdf.doc2pdf((String) fileAbsolutePathArray.get(0), pdfFilePath);
                break;
            case "ppt":
            case "pptx":
                fileToPdf.ppt2pdf((String) fileAbsolutePathArray.get(0), pdfFilePath);
                break;
            case "xls":
            case "xlsx":
                fileToPdf.excel2Pdf((String) fileAbsolutePathArray.get(0), pdfFilePath);
                break;
            case "png":
            case "bmp":
            case "jpg":
                fileToPdf.png2Pdf(fileAbsolutePathArray, pdfFilePath);
                break;
            case "pdf":
                CommonUtils.copyFile((String) fileAbsolutePathArray.get(0), pdfFilePath);
                break;
        }
        return new MsgEntity("SUCCESS", "1", "[MOTA Client] Successful file pre processing");

    }

    /**
     * 一张一页模式下的页面预览
     * <p>
     * 此方法将根据文件名与页号从PdfPagePicSavePath处获得图片，并将图片以Byte字节形式返回给前端
     * （具体路径参考{@link com.oriole.motaclient.Constant}）
     *
     * @param fileName 控制端的单次任务标识串（文件名）
     * @param page     欲获取的页面序号
     * @return ResponseEntity
     */
    @RequestMapping(value = "/getSinglePageThumbnail", produces = "application/json; charset=utf-8")
    public ResponseEntity<byte[]> getSinglePageThumbnail(@RequestParam String fileName, @RequestParam int page) throws Exception {
        FileInputStream inputStream = new FileInputStream(PdfPagePicSavePath + fileName + "_" + page + ".png");
        int i = inputStream.available();

        byte[] thumbnail = new byte[i];
        inputStream.read(thumbnail);
        inputStream.close();

        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);

        return new ResponseEntity<byte[]>(thumbnail, headers, HttpStatus.CREATED);
    }

    /**
     * 一张多页模式下的页面预览
     * <p>
     * 此方法将根据文件名从PdfPagePicSavePath处获得拼合的PDF文件，
     * 根据页号从PDF获得该页图片，并将图片以Byte字节形式返回给前端
     * （具体路径参考{@link com.oriole.motaclient.Constant}）
     *
     * @param fileName 控制端的单次任务标识串（文件名）
     * @param page     欲获取的页面序号
     * @return ResponseEntity
     */
    @RequestMapping(value = "/getSplicingPageThumbnail", produces = "application/json; charset=utf-8")
    public ResponseEntity<byte[]> getSplicingPageThumbnail(@RequestParam String fileName, @RequestParam int page) throws Exception {
        File file = new File(PdfPagePicSavePath + fileName + ".pdf");
        PDDocument pdf = PDDocument.load(file);
        PDFRenderer renderer = new PDFRenderer(pdf);
        BufferedImage image = renderer.renderImageWithDPI(page, 200, ImageType.GRAY); // Windows native DPI
        ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "png", pngOutputStream);

        byte[] thumbnail = pngOutputStream.toByteArray();
        pdf.close();

        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);

        return new ResponseEntity<byte[]>(thumbnail, headers, HttpStatus.CREATED);
    }

    /**
     * 一张多页拼合
     * <p>
     * 此方法将调用PdfSplicing类{@link PdfSplicing}，利用提供参数拼合一张多页的PDF文件
     * 根据页号从PDF获得该页图片，并将图片以Byte字节形式返回给前端
     * （具体路径参考{@link com.oriole.motaclient.Constant}）
     * 拼合成功后向控制端发送成功返回，否则发送具体错误信息
     *
     * @param fileName  控制端的单次任务标识串（文件名）
     * @param pageCount 页面总数
     * @param multiPageColNum       一张多页列数
     * @param multiPageRowNum       一张多页行数
     * @param multiPageOrientation 拼合页面方向
     * @param multiPageOrder 拼合页面顺序
     * @return JSON(msgEntity)，成功将在msg部分返回新生成一张多页pdf文件页面总数
     */
    @RequestMapping(value = "/splicingPage", produces = "application/json; charset=utf-8")
    public MsgEntity splicingPage(@RequestParam String fileName, @RequestParam int pageCount,
                                  @RequestParam int multiPageColNum, @RequestParam int multiPageRowNum,
                                  @RequestParam String multiPageOrientation, @RequestParam String multiPageOrder) throws Exception {

        PdfSplicing pdfSplicing = new PdfSplicing(70, 70, 30, 30, 0, 10);
        int newPageCount = pdfSplicing.splicingPdfPage(fileName, pageCount, multiPageColNum, multiPageRowNum, multiPageOrientation, multiPageOrder);
        return new MsgEntity("SUCCESS", "1", String.valueOf(newPageCount));
    }

    /**
     * 文档类型预测
     * <p>
     * 此方法将根据给定的PDF文件，并获得文档页面的长宽比
     * 根据长宽比与常见场景尺寸对比以获得文档类型
     * (210/297一般为WORD/EXCEL转为pdf后的尺寸，4/3或16/9一般为PPT转为pdf后的尺寸）
     * 若对比失败则根据长宽比给出具有横纵属性定性的未知情况
     * <p>
     * 控制端将继续处理
     * 预处理成功后向控制端发送成功返回，否则发送具体错误信息
     *
     * @param fileName 控制端的单次任务标识串（文件名）
     * @return JSON(msgEntity)，成功将在msg部分返回文档预测类型
     */
    @RequestMapping(value = "/documentTypePrediction", produces = "application/json; charset=utf-8")
    public MsgEntity documentTypePrediction(@RequestParam String fileName) throws Exception {
        PdfReader reader = new PdfReader(PrintFileSavePath + fileName + ".pdf");
        Rectangle pageSize = reader.getPageSize(1);
        float realAspectRatio = pageSize.getWidth() / pageSize.getHeight();

        if (Math.abs(realAspectRatio - (4.0 / 3.0)) < 0.001 || Math.abs(realAspectRatio - (16.0 / 9.0)) < 0.001) {
            return new MsgEntity("SUCCESS", "1", "PPT");
        } else if (Math.abs(realAspectRatio - (210.0 / 297.0)) < 0.001) {
            return new MsgEntity("SUCCESS", "1", "A4_lengthwise");
        } else if (Math.abs(realAspectRatio - (297.0 / 210.0)) < 0.001) {
            return new MsgEntity("SUCCESS", "1", "A4_transverse");
        } else if (realAspectRatio <= 1) {
            return new MsgEntity("SUCCESS", "0", "Unknow_Lengthwise");
        } else {
            return new MsgEntity("SUCCESS", "0", "Unknow_Transverse");
        }
    }
}
