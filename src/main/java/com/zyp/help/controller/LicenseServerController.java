package com.zyp.help.controller;

import cn.hutool.core.util.RandomUtil;
import com.zyp.help.model.ObtainTicketResponse;
import com.zyp.help.model.PingResponse;
import com.zyp.help.model.ProlongTicketResponse;
import com.zyp.help.model.ReleaseTicketResponse;
import com.zyp.help.util.LicenseServerUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * 许可证服务器控制器
 * 
 * <p>此控制器模拟JetBrains官方许可证服务器的行为，提供以下主要功能：
 * <ul>
 *   <li>获取许可证凭证（obtainTicket）</li>
 *   <li>检查服务器连通性（ping）</li>
 *   <li>延长许可证有效期（prolongTicket）</li>
 *   <li>释放许可证凭证（releaseTicket）</li>
 * </ul>
 * 
 * <p>所有的接口都返回XML格式的数据，并使用RSA签名保证数据的安全性。
 * 
 * <p>路径说明：
 * 所有的请求路径都以 {@code /rpc/} 开头，这是为了兼容JetBrains官方许可证服务器的API设计。
 * 
 * @author zyp
 * @version 1.0.0
 * @since 1.0.0
 */

@Slf4j(topic = "许可证服务器")  // 使用自定义日志主题
@RestController
@RequestMapping(value = "/rpc", produces = "text/xml; charset=UTF-8")  // XML输出格式，兼容JetBrains协议
public class LicenseServerController {

    /**
     * 获取许可证凭证接口
     * 
     * <p>这是JetBrains产品首次连接许可证服务器时调用的接口。
     * 该接口会生成一个许可证凭证（ticket），并返回相关的许可证信息。
     * 
     * <p>请求参数说明：
     * <ul>
     *   <li>hostName - 客户端主机名，用于标识许可证使用者</li>
     *   <li>machineId - 客户端机器唯一标识符，用于硬件绑定</li>
     *   <li>salt - 加密盐值，用于增强通信安全性</li>
     * </ul>
     * 
     * <p>返回内容包括：
     * <ul>
     *   <li>ticketId - 凭证唯一标识符</li>
     *   <li>ticketProperties - 凭证属性，包含许可证类型和有效期信息</li>
     *   <li>serverLease - 服务器租约信息</li>
     *   <li>confirmationStamp - 确认时间戳，用于验证请求的有效性</li>
     * </ul>
     * 
     * @param request HTTP请求对象，包含客户端参数
     * @return 签名后的XML响应数据
     */
    @RequestMapping("/obtainTicket.action")
    public Object obtainTicket(HttpServletRequest request) {
        // 提取请求参数
        String hostName = request.getParameter("hostName");
        String machineId = request.getParameter("machineId");
        String salt = request.getParameter("salt");
        
        log.debug("接收到获取许可证凭证请求 - 主机: {}, 机器ID: {}", hostName, machineId);

        // 构建响应对象
        ObtainTicketResponse response = new ObtainTicketResponse();
        response.setAction("NONE");  // 无需额外操作
        response.setConfirmationStamp(LicenseServerUtils.getConfirmationStamp(machineId));  // 生成确认时间戳
        response.setLeaseSignature(LicenseServerUtils.getLeaseSignature());  // 生成租约签名
        response.setMessage("");  // 无错误消息
        response.setProlongationPeriod("600000");  // 延长周期（毫秒）
        response.setResponseCode("OK");  // 响应成功
        response.setSalt(salt);  // 返回原始盐值
        response.setServerLease(LicenseServerUtils.LEASES_CONTENT);  // 服务器租约内容
        response.setServerUid(LicenseServerUtils.SERVER_UID);  // 服务器唯一标识
        response.setTicketId(RandomUtil.randomString(10));  // 生成随机凭证ID
        
        // 设置凭证属性，标识为个人许可证
        response.setTicketProperties(String.format(
            "licensee=%s\tlicenseeType=5\tmetadata=0120211231PSAN000005", 
            hostName
        )); // Personal License 个人许可证标识
        
        response.setValidationDeadlinePeriod("-1");  // 无验证截止日期
        response.setValidationPeriod("600000");  // 验证周期（毫秒）
        
        log.debug("许可证凭证生成成功 - 凭证ID: {}", response.getTicketId());
        
        // 返回签名后的XML数据
        return LicenseServerUtils.getSignXml(response);
    }

    /**
     * 许可证服务器连通性检查接口
     * 
     * <p>JetBrains产品会定期调用此接口来检查许可证服务器的可用性。
     * 该接口主要用于：
     * <ul>
     *   <li>验证服务器是否正常运行</li>
     *   <li>检查网络连通性</li>
     *   <li>更新服务器状态信息</li>
     * </ul>
     * 
     * <p>请求参数与 {@link #obtainTicket} 相同，但响应的内容略有不同。
     * 
     * @param request HTTP请求对象，包含客户端参数
     * @return 签名后的XML响应数据
     */
    @RequestMapping("/ping.action")
    public Object ping(HttpServletRequest request) {
        // 提取请求参数
        String hostName = request.getParameter("hostName");
        String machineId = request.getParameter("machineId");
        String salt = request.getParameter("salt");
        
        log.debug("接收到Ping请求 - 主机: {}, 机器ID: {}", hostName, machineId);

        // 构建响应对象
        PingResponse response = new PingResponse();
        response.setAction("NONE");  // 无需额外操作
        response.setConfirmationStamp(LicenseServerUtils.getConfirmationStamp(machineId));  // 生成确认时间戳
        response.setLeaseSignature(LicenseServerUtils.getLeaseSignature());  // 生成租约签名
        response.setMessage("");  // 无错误消息
        response.setResponseCode("OK");  // 响应成功
        response.setSalt(salt);  // 返回原始盐值
        response.setServerLease(LicenseServerUtils.LEASES_CONTENT);  // 服务器租约内容
        response.setServerUid(LicenseServerUtils.SERVER_UID);  // 服务器唯一标识
        response.setValidationDeadlinePeriod("-1");  // 无验证截止日期
        response.setValidationPeriod("600000");  // 验证周期（毫秒）
        
        log.debug("Ping响应生成成功");

        // 返回签名后的XML数据
        return LicenseServerUtils.getSignXml(response);
    }

    /**
     * 延长许可证有效期接口
     * 
     * <p>当现有的许可证凭证即将过期时，JetBrains产品会调用此接口来延长凭证的有效期。
     * 该接口主要功能：
     * <ul>
     *   <li>更新许可证的有效期</li>
     *   <li>延长凭证的使用时间</li>
     *   <li>确保产品的连续使用</li>
     * </ul>
     * 
     * <p>此接口的响应格式与 {@link #obtainTicket} 基本相同。
     * 
     * @param request HTTP请求对象，包含客户端参数
     * @return 签名后的XML响应数据
     */
    @RequestMapping("/prolongTicket.action")
    public Object prolongTicket(HttpServletRequest request) {
        // 提取请求参数
        String hostName = request.getParameter("hostName");
        String machineId = request.getParameter("machineId");
        String salt = request.getParameter("salt");
        
        log.debug("接收到延长许可证请求 - 主机: {}, 机器ID: {}", hostName, machineId);

        // 构建响应对象
        ProlongTicketResponse response = new ProlongTicketResponse();
        response.setAction("NONE");  // 无需额外操作
        response.setConfirmationStamp(LicenseServerUtils.getConfirmationStamp(machineId));  // 生成确认时间戳
        response.setLeaseSignature(LicenseServerUtils.getLeaseSignature());  // 生成租约签名
        response.setMessage("");  // 无错误消息
        response.setResponseCode("OK");  // 响应成功
        response.setSalt(salt);  // 返回原始盐值
        response.setServerLease(LicenseServerUtils.LEASES_CONTENT);  // 服务器租约内容
        response.setServerUid(LicenseServerUtils.SERVER_UID);  // 服务器唯一标识
        response.setValidationDeadlinePeriod("-1");  // 无验证截止日期
        response.setValidationPeriod("600000");  // 验证周期（毫秒）
        
        log.debug("许可证延长响应生成成功");

        // 返回签名后的XML数据
        return LicenseServerUtils.getSignXml(response);
    }

    /**
     * 释放许可证凭证接口
     * 
     * <p>当JetBrains产品关闭或用户主动退出时，会调用此接口来释放占用的许可证凭证。
     * 该接口主要功能：
     * <ul>
     *   <li>清理占用的许可证资源</li>
     *   <li>释放服务器端的相关缓存</li>
     *   <li>确保许可证能够被其他客户端使用</li>
     * </ul>
     * 
     * <p>此接口通常在产品关闭时自动调用，也可以手动调用来主动释放许可证。
     * 
     * @param request HTTP请求对象，包含客户端参数
     * @return 签名后的XML响应数据
     */
    @RequestMapping("/releaseTicket.action")
    public Object releaseTicket(HttpServletRequest request) {
        // 提取请求参数
        String hostName = request.getParameter("hostName");
        String machineId = request.getParameter("machineId");
        String salt = request.getParameter("salt");
        
        log.debug("接收到释放许可证请求 - 主机: {}, 机器ID: {}", hostName, machineId);

        // 构建响应对象
        ReleaseTicketResponse response = new ReleaseTicketResponse();
        response.setAction("NONE");  // 无需额外操作
        response.setConfirmationStamp(LicenseServerUtils.getConfirmationStamp(machineId));  // 生成确认时间戳
        response.setLeaseSignature(LicenseServerUtils.getLeaseSignature());  // 生成租约签名
        response.setMessage("");  // 无错误消息
        response.setResponseCode("OK");  // 响应成功
        response.setSalt(salt);  // 返回原始盐值
        response.setServerLease(LicenseServerUtils.LEASES_CONTENT);  // 服务器租约内容
        response.setServerUid(LicenseServerUtils.SERVER_UID);  // 服务器唯一标识
        response.setValidationDeadlinePeriod("-1");  // 无验证截止日期
        response.setValidationPeriod("600000");  // 验证周期（毫秒）
        
        log.debug("许可证释放响应生成成功");

        // 返回签名后的XML数据
        return LicenseServerUtils.getSignXml(response);
    }
}
