package com.oriole.motaclient.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.oriole.motaclient.entity.MsgEntity;
import com.oriole.motaclient.utils.CommonUtils;
import com.oriole.motaclient.utils.JSONFileIO;
import com.oriole.motaclient.utils.PdfImageMerge;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.printing.PDFPageable;
import org.apache.pdfbox.printing.PDFPrintable;
import org.apache.pdfbox.printing.Scaling;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.print.DocFlavor;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.*;
import javax.servlet.http.HttpServletRequest;
import java.awt.print.*;
import java.io.*;
import java.util.List;

import static com.oriole.motaclient.Constant.*;

/**
 * 获取文件打印设置并根据设置对文件进行打印
 * 此类包含一系列方法将提供文件打印、广告添加处理、调整打印设置等功能
 * 控制端（VUE-MOTA）将根据情况调用若干方法完成最终打印操作
 * 值得注意的是，printWithAttributes方法是必须调用的
 *
 * @author NeoSunJz
 * @version V1.0.1 Beta
 */
@EnableAutoConfiguration
@Controller
public class FilePrintController {

    /**
     * 打印设置
     * <p>
     * 此方法将把控制端请求时，HttpServletRequest内所包含的键值对存入json临时文件
     * 以备在打印时获取以控制打印设备
     *
     * @param request 是一个HttpServletRequest，需要包含
     *                [
     *                文档打印起始页:startPage\文档打印终结页:endPage\文档打印份数（1-N）:copies\
     *                文档打印范围（AllPage‘全部打印’、CurrentPage‘当前页面’、SelectPage‘选定页面’）:pageRange\双面打印（T/F):duplex\
     *                双面打印翻页方式（DUPLEX‘双面长边’、TUMBLE‘双面短边’）:turningMode\
     *                页面奇偶性（NONE‘默认，全部打印’、OddPage‘仅奇数页’、EvenPage‘仅偶数页’）:pageParity\逐份打印（T/F）:collate\
     *                一张多页拼合打印（T/F）multiPage\一张多页打印 行:row_num\一张多页打印 列:col_num\
     *                一张多页打印 页面前进顺序（Z,anti_Z,N,anti_N）:pageOrder\
     *                一张多页打印 方向（transverse‘横向’,lengthwise‘纵向’）:pageOrientation\
     *                一张多页打印 行列模式（2‘二行一列’,4‘二行二列’,6‘二行三列’,9‘三行三列’,16‘四行四列’,Custom‘自定义’）:pageCount
     *                ]
     *                以及randomCode控制端的单次任务标识串（文件名）
     * @return JSON(msgEntity)，成功后msg部分不返回有处理必要的内容
     */
    @RequestMapping(value = "/setPrintingParam", produces = "application/json; charset=utf-8")
    @ResponseBody
    public String setPrintingParam(HttpServletRequest request) {
        JSONObject PrintingParam = new JSONObject();
        PrintingParam.put("pageRange", request.getParameter("pageRange"));
        PrintingParam.put("nowPage", request.getParameter("nowPage"));
        PrintingParam.put("etartPage", request.getParameter("startPage"));
        PrintingParam.put("endPage", request.getParameter("endPage"));
        PrintingParam.put("PageParity", request.getParameter("pageParity"));

        PrintingParam.put("copies", request.getParameter("copies"));
        PrintingParam.put("collate", request.getParameter("collate"));

        PrintingParam.put("multiPage", request.getParameter("multiPage"));
        PrintingParam.put("row_num", request.getParameter("row_num"));
        PrintingParam.put("col_num", request.getParameter("col_num"));
        PrintingParam.put("pageOrder", request.getParameter("pageOrder"));
        PrintingParam.put("pageOrientation", request.getParameter("pageOrientation"));
        PrintingParam.put("pageCount", request.getParameter("pageCount"));

        PrintingParam.put("duplex", request.getParameter("duplex"));
        PrintingParam.put("turningMode", request.getParameter("turningMode"));

        try {
            if (PrintingParam.get("multiPage").equals("true")) {
                CommonUtils.CopyFile(PdfPagePicSavePath + request.getParameter("randomCode") + ".pdf", PrintFileSavePath + request.getParameter("randomCode") + ".pdf");
            }
            String jsonFileName = request.getParameter("randomCode") + ".json";
            JSONFileIO.WriteFile(PrintingParam, PrintConfigSavePath, jsonFileName);
            MsgEntity msgEntity = new MsgEntity("SUCCESS", "1", "[MOTA Client] Settings saved successfully");
            return JSON.toJSONString(msgEntity);
        } catch (Exception e) {
            e.printStackTrace();
            MsgEntity msgEntity = new MsgEntity("ERROR", "-1", "[MOTA Client] " +e.toString());
            return JSON.toJSONString(msgEntity);
        }
    }

    /**
     * 页边距广告拼合
     * <p>
     * 此方法将调用{@link PdfImageMerge}类，利用提供参数拼合免费打印的广告
     * 由DownloadADSavePath处读取Path.json文件（定时从服务器维护），并根据fileName自PrintFileSavePath处获得pdf文件
     * 利用mergeADImage方法处理并将新产生文件覆盖旧文件
     * （具体路径参考{@link com.oriole.motaclient.Constant}）
     * 拼合成功后向控制端发送成功返回，否则发送具体错误信息
     *
     * @param fileName 控制端的单次任务标识串（文件名）
     * @param direction (一张多页时）拼合页面方向
     * @return JSON(msgEntity)，成功后msg部分不返回有处理必要的内容
     */
    @RequestMapping(value = "/adAdd", produces = "application/json; charset=utf-8")
    @ResponseBody
    public String adAdd(@RequestParam String fileName, @RequestParam String direction) {

        List<String> Path = JSON.parseArray(JSONFileIO.ReadFile(DownloadADSavePath + "Path.json"), String.class);
        try {
            String FileAbsolutePath = PrintFileSavePath + fileName + ".pdf";
            String FileNewPath = PrintFileSavePath + fileName + "temp.pdf";
            PdfImageMerge.mergeADImage(Path, FileAbsolutePath, FileNewPath, direction);
            CommonUtils.CopyFile(FileNewPath,FileAbsolutePath);
            MsgEntity msgEntity = new MsgEntity("SUCCESS", "-1", "[MOTA Client] Successful insertion of advertisements");
            return JSON.toJSONString(msgEntity);
        } catch (Exception e) {
            e.printStackTrace();
            MsgEntity msgEntity = new MsgEntity("ERROR", "-1","[MOTA Client] " + e.toString());
            return JSON.toJSONString(msgEntity);
        }

    }

    /**
     * 打印机执行打印(加入打印队列)
     * <p>
     * 此方法将调用{@link PrinterJob}类，执行打印任务创建
     * 利用{@link org.apache.pdfbox}对文档自动缩放以适应纸张
     * 利用JSON内的打印设置信息添加至构建的PrintRequestAttributeSet内
     * 并根据上述设置创建打印任务
     * 创建打印任务成功后向控制端发送成功返回，否则发送具体错误信息
     *
     * @param fileName 控制端的单次任务标识串（文件名）
     * @return JSON(msgEntity)，成功后msg部分不返回有处理必要的内容
     */
    @RequestMapping(value = "/printFile", produces = "application/json; charset=utf-8")
    @ResponseBody
    public static String printWithAttributes(@RequestParam String fileName) {

        try {
            PrinterJob job = PrinterJob.getPrinterJob();
            PDDocument document = PDDocument.load(new File(PrintFileSavePath + fileName + ".pdf"));

            // 构建打印请求属性集
            PrintRequestAttributeSet pras = new HashPrintRequestAttributeSet();
            // 设置打印格式，因为未确定文件类型，这里选择AUTOSENSE
            DocFlavor flavor = DocFlavor.INPUT_STREAM.AUTOSENSE;
            // 查找所有的可用打印服务
            PrintService printService[] = PrintServiceLookup.lookupPrintServices(flavor, pras);
            // 定位默认的打印服务
            PrintService defaultService = null;
            // 获取打印机
            String printer = PrinterName;
            for (int i = 0; i < printService.length; i++) {
                if (printService[i].getName().contains(printer)) {
                    defaultService = printService[i];
                    break;
                }
            }

            if (defaultService != null) {
                // 利用PDFBox对文档进行修正（自动缩放以适应纸张)
                Book book = new Book();
                job.setPageable(new PDFPageable(document));
                Paper paper = new Paper();
                // 以下两个选项是经过试验的，不要随意调整
                // 设置打印纸张大小
                paper.setSize(570,832);
                // 设置打印位置 坐标
                paper.setImageableArea(10, 5, paper.getWidth(), paper.getHeight()); // no margins
                PageFormat pageFormat = new PageFormat();
                pageFormat.setPaper(paper);
                book.append(new PDFPrintable(document, Scaling.SCALE_TO_FIT), pageFormat,document.getNumberOfPages());
                job.setPageable(book);

                // 设置打印机
                job.setPrintService(defaultService);
                // 设置打印机属性
                PrintRequestAttributeSet attr = new HashPrintRequestAttributeSet();

                // 读取JSON设置
                JSONObject printParam = JSON.parseObject(JSONFileIO.ReadFile(PrintConfigSavePath + fileName + ".json"));

                // 根据JSON配置打印机属性
                // 打印质量
                attr.add(PrintQuality.NORMAL);
                // 打印份数
                attr.add(new Copies(Integer.valueOf((String) printParam.get("copies"))));
                // 打印页面
                if (printParam.get("pageRange").equals("selectPage")) {
                    attr.add(new PageRanges(Integer.valueOf((String) printParam.get("startPage")), Integer.valueOf((String) printParam.get("endPage"))));
                } else if (printParam.get("pageRange").equals("currentPage")) {
                    attr.add(new PageRanges(Integer.valueOf((String) printParam.get("nowPage"))));
                }
                // 是否逐份打印
                if(printParam.get("collate").equals("true")){
                    attr.add(SheetCollate.COLLATED);
                }else{
                    attr.add(SheetCollate.UNCOLLATED);
                }
                // 是否双面及反转方式
                if (printParam.get("duplex").equals("true")) {
                    if (printParam.get("turningMode").equals("DUPLEX")) {
                        attr.add(Sides.DUPLEX);
                    } else {
                        attr.add(Sides.TUMBLE);
                    }
                }

                job.print(attr);
                document.close();

                MsgEntity msgEntity = new MsgEntity("SUCCESS", "1", "[MOTA Client] Printer Creation Task Successful");
                return JSON.toJSONString(msgEntity);
            }else{
                MsgEntity msgEntity = new MsgEntity("ERROR", "-1", "[MOTA Client] No printer found");
                return JSON.toJSONString(msgEntity);
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
            MsgEntity msgEntity = new MsgEntity("ERROR", "-1", "[MOTA Client] "+ e.toString());
            return JSON.toJSONString(msgEntity);
        } catch (IOException e) {
            e.printStackTrace();
            MsgEntity msgEntity = new MsgEntity("ERROR", "-1","[MOTA Client] "+ e.toString());
            return JSON.toJSONString(msgEntity);
        } catch (PrinterException e) {
            e.printStackTrace();
            MsgEntity msgEntity = new MsgEntity("ERROR", "-1","[MOTA Client] "+ e.toString());
            return JSON.toJSONString(msgEntity);
        }
    }

}
