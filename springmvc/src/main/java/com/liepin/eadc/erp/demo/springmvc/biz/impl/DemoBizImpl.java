package com.liepin.eadc.erp.demo.springmvc.biz.impl;

import org.springframework.stereotype.Service;

import com.liepin.eadc.erp.demo.springmvc.biz.IDemoBiz;
import com.liepin.eadc.erp.demo.springmvc.form.PersonForm;

@Service
public class DemoBizImpl implements IDemoBiz {

    public String sayHello(String name) {
        return "Hello " + name + " ,this is spring mvc";
    }

    public String add(int a, int b) {
        return "a + b = " + (a + b);
    }

    public PersonForm getPersonForm() {
        PersonForm form = new PersonForm();

        form.setAge(18);
        form.setName("缪正生");
        return form;
    }


}
