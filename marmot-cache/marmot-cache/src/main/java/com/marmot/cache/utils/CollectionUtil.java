package com.marmot.cache.utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CollectionUtil {


    public static Map<String, Double> transform(Map<Double, String> source) {
        Map<String, Double> target = new HashMap<String, Double>(source.size());
        for (Map.Entry<Double, String> entry : source.entrySet()) {
            target.put(entry.getValue(), entry.getKey());
        }
        return target;
    }

    public static Set<String> split(String... servers) {
        Set<String> set = new HashSet<String>();
        for (String server : servers) {
            if (server == null) {
                continue;
            }
            String[] array = server.split(",");
            for (String s : array) {
                set.add(s.trim());
            }
        }
        return set;
    }


}
