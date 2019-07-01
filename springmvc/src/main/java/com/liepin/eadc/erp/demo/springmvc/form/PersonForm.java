package com.liepin.eadc.erp.demo.springmvc.form;

import java.io.Serializable;

public class PersonForm implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private Integer age;

    private String name;

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "PersonForm [age=" + age + ", name=" + name + "]";
    }

}
