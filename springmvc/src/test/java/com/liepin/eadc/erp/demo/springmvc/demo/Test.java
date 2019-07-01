package com.liepin.eadc.erp.demo.springmvc.demo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class Test {


    public static void main(String[] args) throws IOException {
        InputStream resourceAsStream = Test.class.getClassLoader().getResourceAsStream("data.txt");

        BufferedReader reader = new BufferedReader(new InputStreamReader(resourceAsStream));

        String line = null;

        Map<String, String> map = new HashMap<String, String>();
        while ((line = reader.readLine()) != null) {
            String userId = line.substring(line.indexOf("userid") + 7, line.indexOf(")！"));
            String employeeId = line.substring(line.indexOf("employeeId=") + 11);
            map.put(userId, employeeId);
        }

        System.out.println(map.size());
        reader.close();

        Set<Entry<String, String>> entrySet = map.entrySet();

        Iterator<Entry<String, String>> iterator = entrySet.iterator();

        while (iterator.hasNext()) {
            Entry<String, String> next = iterator.next();
            System.out.println(next.getValue());
        }
    }
}
