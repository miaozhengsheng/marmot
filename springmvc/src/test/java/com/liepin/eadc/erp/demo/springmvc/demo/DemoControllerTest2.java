package com.liepin.eadc.erp.demo.springmvc.demo;

import org.springframework.web.context.support.XmlWebApplicationContext;

import com.liepin.eadc.erp.demo.springmvc.controller.DemoController;



public class DemoControllerTest2 {


    public static void main(String[] args) {

        XmlWebApplicationContext context = new XmlWebApplicationContext();

        context.setConfigLocation("classpath*:applicationContext-common.xml");
        context.refresh();

        DemoController controller = (DemoController) context.getBean("demoController");

        System.out.println(controller.getClass());

        context.close();
    }

}
