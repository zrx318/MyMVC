package com.enjoy.zrx.service.impl;

import com.enjoy.zrx.annotation.EnjoyService;
import com.enjoy.zrx.service.ZrxService;

@EnjoyService("ZrxServiceImpl")
public class ZrxServiceImpl implements ZrxService {
    @Override
    public String query(String name, String age) {
        return "name"+"====="+name+","+"age"+"======"+age;
    }
}
