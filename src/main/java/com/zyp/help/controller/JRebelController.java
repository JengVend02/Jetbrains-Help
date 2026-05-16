package com.zyp.help.controller;

import com.zyp.help.service.JrebelService;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j(topic = "JRebel激活控制器")
@RestController
@RequiredArgsConstructor
public class JRebelController {

    private final JrebelService jrebelService;

    @SneakyThrows
    @RequestMapping("/guid")
    public void guid(HttpServletRequest request, HttpServletResponse response) {
        response.setContentType("text/html; charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);
        String body = UUID.randomUUID().toString();
        response.getWriter().print(body);
    }

    @RequestMapping({"/jrebel/leases"})
    public void jrebelLeases(HttpServletRequest request, HttpServletResponse response) {
        jrebelService.jrebelLeasesHandler(request, response);
    }

    @RequestMapping("/jrebel/leases/1")
    public void jrebelLeases1(HttpServletRequest request, HttpServletResponse response) {
        jrebelService.jrebelLeases1Handler(request, response);
    }

    @RequestMapping("/agent/leases")
    public void agentLeases(HttpServletRequest request, HttpServletResponse response) {
        jrebelService.jrebelLeasesHandler(request, response);
    }

    @RequestMapping("/agent/leases/1")
    public void agentLeases1(HttpServletRequest request, HttpServletResponse response) {
        jrebelService.jrebelLeases1Handler(request, response);
    }

    @RequestMapping("/jrebel/validate-connection")
    public void jrebelValidateHandler(HttpServletRequest request, HttpServletResponse response) {
        jrebelService.jrebelValidateHandler(request, response);
    }

}
