package com.zyp.help.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

/**
 * 获取许可证凭证响应实体类
 * 
 * <p>此类封装了JetBrains许可证服务器在处理 "obtainTicket" 请求时返回的响应数据。
 * 该响应包含了许可证凭证的所有必要信息，包括凭证ID、属性、服务器信息等。
 * 
 * <p>使用JAXB注解支持XML序列化，保证与JetBrains官方许可证服务器协议的兼容性。
 * 
 * <p>响应结构说明：
 * <ul>
 *   <li>action - 需要执行的操作，通常为 "NONE"</li>
 *   <li>confirmationStamp - 确认时间戳，用于验证请求的有效性</li>
 *   <li>leaseSignature - 租约签名，用于验证服务器的合法性</li>
 *   <li>ticketId - 凭证唯一标识符</li>
 *   <li>ticketProperties - 凭证属性，包含许可证类型和有效期信息</li>
 * </ul>
 * 
 * @author zyp
 * @version 1.0.0
 * @since 1.0.0
 */


@Data
@AllArgsConstructor
@NoArgsConstructor
@XmlRootElement(name = "ObtainTicketResponse")  // XML根元素名称
@XmlAccessorType(XmlAccessType.FIELD)  // 使用字段直接访问方式
public class ObtainTicketResponse implements Serializable {
    
    // ==================== 响应字段定义 ====================
    
    /** 需要执行的动作，通常为 "NONE" 表示无需额外操作 */
    private String action;
    
    /** 确认时间戳，由服务器生成，用于验证请求的有效性和及时性 */
    private String confirmationStamp;
    
    /** 租约签名，用于验证服务器的合法性和权威性 */
    private String leaseSignature;
    
    /** 响应消息，通常为空，只在出现错误时包含错误信息 */
    private String message;
    
    /** 延长周期（毫秒），表示多久后可以申请延长许可证 */
    private String prolongationPeriod;
    
    /** 响应码，"OK" 表示成功，其他值表示各种错误情况 */
    private String responseCode;
    
    /** 加密盐值，从请求中原样返回，用于增强通信安全性 */
    private String salt;
    
    /** 服务器租约内容，包含过期时间和服务器标识 */
    private String serverLease;
    
    /** 服务器唯一标识符，用于标识许可证服务器实例 */
    private String serverUid;
    
    /** 许可证凭证的唯一标识符，由服务器随机生成 */
    private String ticketId;
    
    /** 
     * 许可证凭证属性，包含以下信息：
     * - licensee: 许可证持有者名称
     * - licenseeType: 许可证类型（如 5 表示个人许可证）
     * - metadata: 元数据信息，包含版本和有效期信息
     */
    private String ticketProperties;
    
    /** 验证截止时间（毫秒），-1 表示无截止时间 */
    private String validationDeadlinePeriod;
    
    /** 验证周期（毫秒），表示多久后需要重新验证 */
    private String validationPeriod;
}

