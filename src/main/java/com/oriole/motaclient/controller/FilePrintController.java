package com.oriole.motaclient.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.oriole.motaclient.entity.BusinessException;
import com.oriole.motaclient.entity.MsgEntity;
import com.oriole.motaclient.utils.ADFileManagement;
import com.oriole.motaclient.utils.CommonUtils;
import com.oriole.motaclient.utils.JSONFileIO;
import com.oriole.motaclient.utils.PdfImageMerge;
import javafx.util.Pair;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.printing.PDFPageable;
import org.apache.pdfbox.printing.PDFPrintable;
import org.apache.pdfbox.printing.Scaling;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.web.bind.annotation.*;

import javax.print.DocFlavor;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.*;
import javax.servlet.http.HttpServletRequest;
import java.awt.print.*;
import java.io.*;
import java.util.ArrayList;
import static com.oriole.motaclient.Constant.*;
import static com.oriole.motaclient.utils.CommonUtils.hexToString;
import static com.oriole.motaclient.utils.ReportPrinterStatus.*;

/**
 * 获取文件打印设置并根据设置对文件进行打印
 * 此类包含一系列方法将提供文件打印、广告添加处理、调整打印设置等功能
 * 控制端（VUE-MOTA）将根据情况调用若干方法完成最终打印操作
 * 值得注意的是，printWithAttributes方法是必须调用的
 *
 * @author NeoSunJz
 * @version V2.1.3 Beta
 */
@EnableAutoConfiguration
@RestController
public class FilePrintController {

    private Process process;

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
     *                一张多页拼合打印（T/F）multiPage\一张多页打印 行:multiPageRowNum\一张多页打印 列:multiPageColNum\
     *                一张多页打印 页面前进顺序（Z,anti_Z,N,anti_N）:multiPageOrder\
     *                一张多页打印 方向（transverse‘横向’,lengthwise‘纵向’）:multi\
     *                一张多页打印 行列模式（2‘二行一列’,4‘二行二列’,6‘二行三列’,9‘三行三列’,16‘四行四列’,Custom‘自定义’）:multiPageCount
     *                ]
     *                以及randomCode控制端的单次任务标识串（文件名）
     * @return JSON(msgEntity)，成功后msg部分不返回有处理必要的内容
     */
    @RequestMapping(value = "/setPrintingParam", produces = "application/json; charset=utf-8")
    public MsgEntity setPrintingParam(HttpServletRequest request) throws Exception {
        JSONObject PrintingParam = new JSONObject();
        PrintingParam.put("pageRange", request.getParameter("pageRange"));
        PrintingParam.put("nowPage", request.getParameter("nowPage"));
        PrintingParam.put("etartPage", request.getParameter("startPage"));
        PrintingParam.put("endPage", request.getParameter("endPage"));
        PrintingParam.put("PageParity", request.getParameter("pageParity"));

        PrintingParam.put("copies", request.getParameter("copies"));
        PrintingParam.put("collate", request.getParameter("collate"));

        PrintingParam.put("multiPage", request.getParameter("multiPage"));
        PrintingParam.put("multiPageRowNum", request.getParameter("multiPageRowNum"));
        PrintingParam.put("multiPageColNum", request.getParameter("multiPageColNum"));
        PrintingParam.put("multiPageOrder", request.getParameter("multiPageOrder"));
        PrintingParam.put("multiPageOrientation", request.getParameter("multiPageOrientation"));
        PrintingParam.put("multiPageCount", request.getParameter("multiPageCount"));

        PrintingParam.put("duplex", request.getParameter("duplex"));
        PrintingParam.put("turningMode", request.getParameter("turningMode"));


        if (PrintingParam.get("multiPage").equals("true")) {
            //若一张多页激活，则将获得的拼合文件覆盖原始文件
            CommonUtils.copyFile(PdfPagePicSavePath + request.getParameter("randomCode") + ".pdf", PrintFileSavePath + request.getParameter("randomCode") + ".pdf");
        }
        String jsonFileName = request.getParameter("randomCode") + ".json";
        JSONFileIO.WriteFile(PrintingParam, PrintConfigSavePath, jsonFileName);
        return new MsgEntity("SUCCESS", "1", "[MOTA Client] Settings saved successfully");
    }

    /**
     * 页边距广告拼合
     * <p>
     * 此方法将调用{@link PdfImageMerge}类，利用提供参数拼合免费打印的广告
     * 由DownloadADSavePath处读取ADList.json文件（定时从服务器维护）以得到需要拼合的广告图片，并根据fileName自PrintFileSavePath处获得pdf文件
     * 利用mergeADImage方法处理并将新产生文件覆盖旧文件
     * （具体路径参考{@link com.oriole.motaclient.Constant}）
     * 拼合成功后向控制端发送成功返回，否则发送具体错误信息
     *
     * @param fileName             控制端的单次任务标识串（文件名）
     * @param multiPageOrientation (一张多页时）拼合页面方向
     * @return JSON(msgEntity)，成功后msg部分不返回有处理必要的内容
     */
    @RequestMapping(value = "/adAdd", produces = "application/json; charset=utf-8")
    public MsgEntity adAdd(@RequestParam String fileName, @RequestParam String randomCode, @RequestParam String multiPageOrientation) throws Exception {
        ADFileManagement adFileManagement = ADFileManagement.getInstance();

        ArrayList<Pair<Integer, String>> marginADPath = new ArrayList<>();
        ArrayList<Pair<Integer, String>> wholePageADPath = new ArrayList<>();

        JSONArray marginADList = adFileManagement.getMarginImg();
        for (int i = 0; i < marginADList.size(); i++) {
            JSONObject temp = marginADList.getJSONObject(i);
            marginADPath.add(new Pair<>(temp.getInteger("aid"), DownloadADSavePath + temp.get("adPicFile")));
        }

        JSONArray wholePageADList = adFileManagement.getWholePageImg();
        for (int i = 0; i < wholePageADList.size(); i++) {
            JSONObject temp = wholePageADList.getJSONObject(i);
            wholePageADPath.add(new Pair<>(temp.getInteger("aid"), DownloadADSavePath + temp.get("adPicFile")));
        }

        String FileAbsolutePath = PrintFileSavePath + fileName + ".pdf";
        String FileNewPath = PrintFileSavePath + fileName + "_ADTemp.pdf";
        PdfImageMerge pdfImageMerge = new PdfImageMerge();
        pdfImageMerge.mergeADImage(marginADPath, wholePageADPath, randomCode, FileAbsolutePath, FileNewPath, multiPageOrientation);
        //覆盖源文件
        CommonUtils.copyFile(FileNewPath, FileAbsolutePath);
        return new MsgEntity("SUCCESS", "-1", "[MOTA Client] Successful insertion of advertisements");
    }

    @RequestMapping(value = "/getPaperConsumedNum", produces = "application/json; charset=utf-8")
    public MsgEntity getPaperConsumedNum(@RequestParam String fileName) throws Exception {
        PDDocument document = PDDocument.load(new File(PrintFileSavePath + fileName + ".pdf"));
        // 读取JSON设置
        JSONObject printParam = JSON.parseObject(JSONFileIO.ReadFile(PrintConfigSavePath + fileName + ".json"));
        int paperConsumedNum;
        if (printParam.get("duplex").equals("true")) {
            paperConsumedNum = (int) Math.ceil((printParam.getInteger("copies") * document.getNumberOfPages()) / 2.0);
        } else {
            paperConsumedNum = printParam.getInteger("copies") * document.getNumberOfPages();
        }
        return new MsgEntity("SUCCESS", "1", String.valueOf(paperConsumedNum));
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
    public static MsgEntity printWithAttributes(@RequestParam String fileName, @RequestParam String randomCode, @RequestParam String printMode) throws Exception {

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
            paper.setSize(570, 832);
            // 设置打印位置 坐标
            paper.setImageableArea(10, 5, paper.getWidth(), paper.getHeight()); // no margins
            PageFormat pageFormat = new PageFormat();
            pageFormat.setPaper(paper);
            book.append(new PDFPrintable(document, Scaling.SCALE_TO_FIT), pageFormat, document.getNumberOfPages());
            job.setPageable(book);

            // 设置打印机
            job.setPrintService(defaultService);
            // 设置打印机属性
            PrintRequestAttributeSet attr = new HashPrintRequestAttributeSet();

            // 读取JSON设置
            JSONObject printParam = JSON.parseObject(JSONFileIO.ReadFile(PrintConfigSavePath + fileName + ".json"));

            // 根据JSON配置打印机属性
            // 打印质量
            if (printMode.equals("1")) {
                attr.add(PrintQuality.NORMAL);
            } else {
                attr.add(PrintQuality.HIGH);
            }
            // 打印份数
            attr.add(new Copies(Integer.valueOf((String) printParam.get("copies"))));
            // 打印页面
            if (printParam.get("pageRange").equals("selectPage")) {
                attr.add(new PageRanges(printParam.getInteger("startPage"), printParam.getInteger("endPage")));
            } else if (printParam.get("pageRange").equals("currentPage")) {
                attr.add(new PageRanges(printParam.getInteger("nowPage")));
            }
            // 是否逐份打印
            if (printParam.get("collate").equals("true")) {
                attr.add(SheetCollate.COLLATED);
            } else {
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

            //预估打印时间
            int estimatedTime = 0;
            if (printParam.get("duplex").equals("true")) {
                estimatedTime = 2 + 6 + printParam.getInteger("copies") * document.getNumberOfPages() * 12;
            } else {
                estimatedTime = 2 + 6 + printParam.getInteger("copies") * document.getNumberOfPages() * 2;
            }

            JSONObject data = new JSONObject();
            if (printParam.get("duplex").equals("true")) {
                data.put("paperConsumedNum", (int) Math.ceil((printParam.getInteger("copies") * document.getNumberOfPages()) / 2.0));
            } else {
                data.put("paperConsumedNum", printParam.getInteger("copies") * document.getNumberOfPages());
            }
            data.put("printMode", printMode);
            data.put("printingStatus", "2");
            reportPrintingProcess(DeviceID, randomCode, data);

            return new MsgEntity("SUCCESS", "1", String.valueOf(estimatedTime));
        } else {
            throw new BusinessException("-1", "No printer found!");
        }
    }

    /**
     * 打印机状态监听
     * <p>
     * 此方法将调用通过运行时进程调用tshark.exe，利用USBPcap，对打印机通讯进行监控
     * 选取串口数据包大于60的（客户端环境只有打印设备可达到）校验是否有"@PJL USTATUS DEVICE"标志
     * 若有，对code进行处理，获得打印的最终状态，并给出结果
     * 设备就绪后发送成功返回，否则发送具体错误信息
     *
     * @return JSON(msgEntity)，成功后msg部分不返回有处理必要的内容
     */
    @RequestMapping(value = "/PrinterStatusMonitor", produces = "application/json; charset=utf-8")
    public MsgEntity PrinterStatusMonitor(@RequestParam String randomCode) throws Exception {
        String code = "";
        Boolean isGetPrinterResponse = false;
        process = Runtime.getRuntime().exec(WiresharkPathConfig + "tshark.exe -i \"" + WiresharkUSBPcapConfig + "\" -T fields -e usb.capdata -Y \"usb.data_len>60 and usb.data_len<120\" -l");
        BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String str;
        while ((str = br.readLine()) != null) {
            String result = hexToString(str);
            if (result.contains("@PJL USTATUS DEVICE")) {
                code = result.substring(result.indexOf("CODE=") + 5, result.indexOf("DISPLAY") - 2);
                String display = result.substring(result.indexOf("DISPLAY=") + 8, result.indexOf("ONLINE") - 2);
                if (code.equals("10001") || code.equals("10003") || code.equals("40000")) {
                    //若获得代码为10001（就绪）、10003（处理中）、40000（休眠），置isGetPrinterResponse为TRUE
                    isGetPrinterResponse = true;
                } else {
                    process.destroy();
                    switch (Integer.valueOf(code)) {
                        case 40010:
                            reportPrintingError(DeviceID, randomCode, code, "需要更换墨粉");
                            throw new BusinessException(code, "打印机粉组件无粉或已经损坏");
                        case 41213:
                            reportPrintingError(DeviceID, randomCode, code, "需要添加打印纸");
                            throw new BusinessException(code, "打印机缺纸或进纸器故障");
                        default:
                            reportPrintingError(DeviceID, randomCode, code, "异常：" + display);
                            throw new BusinessException(code, "异常：" + display);
                    }
                }
            }
        }

        //当USB端口监听的Process终止时，将检查打印机工作的最终状态
        if (!isGetPrinterResponse) {
            throw new BusinessException("-1", "设备未正常响应");
        } else {
            JSONObject data = new JSONObject();
            data.put("printingStatus", "3");
            reportPrintingProcess(DeviceID, randomCode, data);
            //若为付费打印则不报告
            reportADPrintingResult(randomCode);
            return new MsgEntity("SUCCESS", "1", "打印顺利结束");
        }
    }

    @RequestMapping(value = "/finishPrint", produces = "application/json; charset=utf-8")
    public MsgEntity finishPrint() throws Exception {
        process.destroy();
        return new MsgEntity("SUCCESS", "1", "");
    }

}
