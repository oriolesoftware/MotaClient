package com.oriole.motaclient.entity;

/**
 * 返回控制端JSON串实体类
 *
 * @author NeoSunJz
 * @version V1.0.1 Beta
 */
public class MsgEntity {
    private String state;
    private String code;
    private String msg;

    /**
     * @param state 操作执行状态（SUCCESS/ERROR)
     * @param code 状态或操作代码
     * @param msg 附带返回信息
     */
    public MsgEntity(String state, String code, String msg) {
        this.state = state;
        this.code = code;
        this.msg = msg;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
