package com.liepin.eadc.erp.demo.springmvc.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.liepin.eadc.erp.demo.springmvc.biz.IDemoBiz;
import com.liepin.eadc.erp.demo.springmvc.form.PersonForm;

@RequestMapping("/demo")
@Controller
public class DemoController {

    @Autowired
    private IDemoBiz demoBiz;

    @RequestMapping("/sayhello.json")
    public void sayHello(String name, HttpServletResponse res) throws IOException {

        res.getWriter().write(demoBiz.sayHello(name));
    }

    @RequestMapping("/add.json")
    public void add(int a, int b, HttpServletResponse res) throws IOException {
        res.getWriter().write(demoBiz.add(a, b));
    }


    @RequestMapping("/testreqbody")
    @ResponseBody
    public void testReqBody(@RequestBody Map<String, Object> content) throws IOException {

        System.out.println(content.toString());
        // res.getWriter().write(content.toString());
    }

    @RequestMapping("/view")
    public String getPeopleListInfo() {
        return "/view/ab";
    }

    @RequestMapping("/listparamters")
    public void listParamTest(ArrayList<PersonForm> array, HttpServletResponse response) throws IOException {

        response.getWriter().write(StringUtils.collectionToCommaDelimitedString(array));
    }

    @Override
    public String toString() {
        return super.toString();
    }



}
