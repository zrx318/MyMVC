package com.enjoy.zrx.controller;

import com.enjoy.zrx.annotation.EnjoyAutowired;
import com.enjoy.zrx.annotation.EnjoyController;
import com.enjoy.zrx.annotation.EnjoyRequestMapping;
import com.enjoy.zrx.annotation.EnjoyRequestParam;
import com.enjoy.zrx.service.ZrxService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@EnjoyController
@EnjoyRequestMapping("/zrx")
public class ZrxController {
    @EnjoyAutowired("ZrxServiceImpl")
    private ZrxService zrxService;

    @EnjoyRequestMapping("/query")
    public void query(HttpServletRequest request, HttpServletResponse response,
                      @EnjoyRequestParam("name") String name,@EnjoyRequestParam("age") String age) {
        try {

            PrintWriter writer = response.getWriter();
            String query = zrxService.query(name, age);
            writer.write(query);

        } catch (IOException e) {
            e.printStackTrace();
        }


    }
}
