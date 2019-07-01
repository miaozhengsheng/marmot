package com.liepin.eadc.erp.demo.springmvc.biz;

import com.liepin.eadc.erp.demo.springmvc.form.PersonForm;

public interface IDemoBiz {

    String sayHello(String name);

    String add(int a, int b);

    PersonForm getPersonForm();
}
