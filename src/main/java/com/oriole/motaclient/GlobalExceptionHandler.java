package com.oriole.motaclient;

import com.oriole.motaclient.entity.BusinessException;
import com.oriole.motaclient.entity.MsgEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.Date;

@ControllerAdvice
@ResponseBody
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public MsgEntity businessExceptionHandle(BusinessException ex) {
        logger.error("[MOTA Client] Business execution failed:", ex);
        ex.printStackTrace();
        return new MsgEntity("ERROR", ex.getCode(),"Business execution failed: "+ex.getMessage());
    }

    /**
     * 系统异常 预期以外异常
     * @param ex
     * @return
     */
    @ExceptionHandler(Exception.class)
    public MsgEntity systemUnexpectedExceptionHandle(Exception ex) {
        logger.error("[MOTA Client] SYSTEM FAIL - "+ex.getClass().getName(), ex);
        ex.printStackTrace();
        return new MsgEntity("ERROR","-1","[MOTA Client] "+ex.getClass().getName()+": "+ex.getMessage());
    }

}
