package com.zyp.help.service;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.zyp.help.util.jrebel.JrebelSign;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

@Service
public class JrebelService {

    private static final String SERVER_GUID = "a1b4aea8-b031-4302-b602-670a990272cb";

    @SneakyThrows
    public void jrebelLeasesHandler(HttpServletRequest request, HttpServletResponse response) {
        response.setContentType("application/json; charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);
        String clientRandomness = request.getParameter("randomness");
        String username = request.getParameter("username");
        String guid = request.getParameter("guid");
        boolean offline = true;
        String validFrom = "";
        String validUntil = "";
        String clientTime = request.getParameter("clientTime");

        try {
            long clientTimeMillis = Long.parseLong(clientTime);
            validFrom = clientTime;
            validUntil = String.valueOf(clientTimeMillis + 180L * 24 * 60 * 60 * 1000);
        } catch (NumberFormatException ignored) {
            // 忽略非法输入，保持默认空值
        }
        JSONObject jsonObject = JSONUtil.createObj().set("serverVersion", "3.2.4")
            .set("serverProtocolVersion", "1.1")
            .set("serverGuid", SERVER_GUID)
            .set("groupType", "managed")
            .set("id", 1)
            .set("licenseType", 1)
            .set("evaluationLicense", false)
            .set("signature",
                "OJE9wGg2xncSb+VgnYT+9HGCFaLOk28tneMFhCbpVMKoC/Iq4LuaDKPirBjG4o394/UjCDGgTBpIrzcXNPdVxVr8PnQzpy7ZSToGO8wv/KIWZT9/ba7bDbA8/RZ4B37YkCeXhjaixpmoyz/CIZMnei4q7oWR7DYUOlOcEWDQhiY=")
            .set("serverRandomness", "H2ulzLlh7E0=")
            .set("statusCode", "SUCCESS")
            .set("offline", offline)
            .set("validFrom", validFrom)
            .set("validUntil", validUntil)
            .set("company", "Administrator")
            .set("orderId", "")
            .set("zeroIds", new JSONArray())
            .set("licenseValidFrom", validFrom)
            .set("licenseValidUntil", validUntil);
        if (clientRandomness == null || username == null || guid == null) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        } else {
            JrebelSign jrebelSign = new JrebelSign();
            jrebelSign.toLeaseCreateJson(clientRandomness, guid, offline, validFrom, validUntil);
            String signature = jrebelSign.getSignature();
            jsonObject.set("signature", signature);
            jsonObject.set("company", username);
            response.getWriter().write(jsonObject.toString());
        }
    }

    @SneakyThrows
    public void jrebelValidateHandler(HttpServletRequest request, HttpServletResponse response) {
        response.setContentType("application/json; charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);
        JSONObject jsonObject = JSONUtil.createObj().set("serverVersion", "3.2.4")
            .set("serverProtocolVersion", "1.1")
            .set("serverGuid", SERVER_GUID)
            .set("groupType", "managed")
            .set("statusCode", "SUCCESS")
            .set("company", "Administrator")
            .set("canGetLease", true)
            .set("licenseType", 1)
            .set("evaluationLicense", false)
            .set("seatPoolType", "standalone");
        response.getWriter().write(jsonObject.toString());
    }

    @SneakyThrows
    public void jrebelLeases1Handler(HttpServletRequest request, HttpServletResponse response) {
        response.setContentType("application/json; charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);
        JSONObject jsonObject = JSONUtil.createObj().set("serverVersion", "3.2.4")
            .set("serverProtocolVersion", "1.1")
            .set("serverGuid", SERVER_GUID)
            .set("groupType", "managed")
            .set("statusCode", "SUCCESS")
            .set("msg", null)
            .set("statusMessage", null);
        String username = request.getParameter("username");
        if(ObjectUtil.isNotEmpty(username)){
            jsonObject.set("company", username);
        }
        response.getWriter().write(jsonObject.toString());
    }

}
