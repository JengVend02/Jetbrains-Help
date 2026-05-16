package com.zyp.help.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

/**
 * 释放许可证凭证响应实体类
 * 
 * <p>此类封装了JetBrains许可证服务器在处理 "releaseTicket" 请求时返回的响应数据。
 * 释放请求用于在JetBrains产品关闭或用户主动退出时清理资源。
 * 
 * <p>此响应的结构与其他响应类型基本相同，
 * 主要用于确认许可证凭证已成功释放和清理相关资源。
 * 
 * @author zyp
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@XmlRootElement(name = "ReleaseTicketResponse")  // XML根元素名称
@XmlAccessorType(XmlAccessType.FIELD)  // 使用字段直接访问方式
public class ReleaseTicketResponse implements Serializable {
    
    /** 需要执行的动作，通常为 "NONE" */
    private String action;
    
    /** 确认时间戳，用于验证请求的有效性 */
    private String confirmationStamp;
    
    /** 租约签名，用于验证服务器的合法性 */
    private String leaseSignature;
    
    /** 响应消息，通常为空 */
    private String message;
    
    /** 响应码，"OK" 表示成功 */
    private String responseCode;
    
    /** 加密盐值，从请求中原样返回 */
    private String salt;
    
    /** 服务器租约内容 */
    private String serverLease;
    
    /** 服务器唯一标识符 */
    private String serverUid;
    
    /** 验证截止时间，-1 表示无截止时间 */
    private String validationDeadlinePeriod;
    
    /** 验证周期，表示多久后需要重新验证 */
    private String validationPeriod;
}
