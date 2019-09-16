package com.marmot.zk.utils;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.DeserializationConfig.Feature;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;
import org.codehaus.jackson.map.ser.CustomSerializerFactory;
import org.codehaus.jackson.map.type.TypeFactory;

public class JsonUtil {


    private static final Logger logger = Logger.getLogger(JsonUtil.class);

    private static ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static ObjectMapper UNICODE_SERIALIZER_OBJECT_MAPPER = new ObjectMapper();

    static {
        OBJECT_MAPPER.getSerializationConfig().setSerializationInclusion(Inclusion.NON_NULL);
        OBJECT_MAPPER.getDeserializationConfig().set(Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // 升级1.8.8版本后timestamp类型必须format，而默认没有支持那么完美，故需要扩展一些时间格式
        OBJECT_MAPPER.getDeserializationConfig().setDateFormat(ExtendStdDateFormat.instance);
        OBJECT_MAPPER.getJsonFactory().disable(JsonParser.Feature.INTERN_FIELD_NAMES);// 避免触发的String.intern(),
                                                                                      // 导致内存持续增加

        // 使Jackson JSON支持Unicode编码非ASCII字符
        CustomSerializerFactory serializerFactory = new CustomSerializerFactory();
        serializerFactory.addSpecificMapping(String.class, new StringUnicodeSerializer());
        UNICODE_SERIALIZER_OBJECT_MAPPER.setSerializerFactory(serializerFactory);
        UNICODE_SERIALIZER_OBJECT_MAPPER.getSerializationConfig().setSerializationInclusion(Inclusion.NON_NULL);
        UNICODE_SERIALIZER_OBJECT_MAPPER.getDeserializationConfig().set(Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    private static final Map<Class<?>, List<Field>> fieldCache = new HashMap<Class<?>, List<Field>>();

    /**
     * @desc json字符串转换为指定的class对象。其中以class中的field为准，如果json中没有此field，class对象中此field忽略
     *       第一次调用的class，会解析class field信息，放入内存，外后都是从内存获取
     * @param jsonStr json字符串
     * @param clazz 目标对象class，此对象及里面的对象必须拥有默认的构造函数
     */
    @SuppressWarnings("unchecked")
    public static Object json2otherObject(String jsonStr, Class<?> clazz) {
        Object clazzObj = null;
        try {
            Map<String, Object> map = OBJECT_MAPPER.readValue(jsonStr, Map.class);
            clazzObj = clazz.newInstance();
            List<Field> fields = getFields(clazz);
            for (Field field : fields) {
                Object value = map.get(field.getName());
                if (value == null) {
                    continue;
                }
                set(clazzObj, field, value);
            }
        } catch (Exception e) {
            logger.warn("json=" + jsonStr + ", clazz=" + clazz + " , json2otherObject fail", e);
        }
        return clazzObj;
    }

    // public static Object json2object(String jsonStr, Class<?> clazz) {
    // if (jsonStr == null || jsonStr.length() == 0) {
    // return null;
    // }
    //
    // Object object = null;
    // try {
    // if (jsonStr.startsWith("[") && jsonStr.endsWith("]")) {
    // object = OBJECT_MAPPER.readValue(jsonStr,
    // TypeFactory.collectionType(List.class, clazz));
    // } else {
    // object = OBJECT_MAPPER.readValue(jsonStr, clazz);
    // }
    // } catch (Exception e) {
    // logger.warn(e.getMessage(), e);
    // }
    //
    // return object;
    // }

    @SuppressWarnings("unchecked")
    public static <T> T json2objectDepth(String json, Class<T> clazz, Class<?>... genericClazz) {
        if (json == null) {
            return null;
        }
        try {
            if (String.class == clazz) {
                return (T) json;
            } else if (clazz.isEnum()) {
                return (T) OBJECT_MAPPER.readValue("\"" + json + "\"", clazz);
            } else if (clazz.isArray() && genericClazz.length == 0) {
                return (T) OBJECT_MAPPER.readValue(json, clazz);
            } else {
                return (T) OBJECT_MAPPER.readValue(json, TypeFactory.parametricType(clazz, genericClazz));
            }
        } catch (Exception e) {
            throw new RuntimeException("json2object invoke fail. [json=" + json + ", clazz=" + clazz + "]", e);
        }
    }

    public static Object json2object(String jsonStr, Class<?> clazz) {
        if (jsonStr == null || jsonStr.length() == 0) {
            return null;
        }

        Object object = null;
        try {
            if (jsonStr.startsWith("[") && jsonStr.endsWith("]")) {
                object = OBJECT_MAPPER.readValue(jsonStr, TypeFactory.collectionType(List.class, clazz));
            } else {
                object = OBJECT_MAPPER.readValue(jsonStr, clazz);
            }
        } catch (Exception e) {
            logger.warn("json=" + jsonStr + ", clazz=" + clazz + " , json2object fail", e);
        }

        return object;
    }

    /**
     * json转换为map
     * 
     * @param String jsonStr, json字符串
     * @return Map<String, Object>
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> json2map(String jsonStr) {
        Map<String, Object> map = null;
        try {
            map = OBJECT_MAPPER.readValue(jsonStr, Map.class);
        } catch (Exception e) {
            logger.warn("json=" + jsonStr + " , json2map fail", e);
        }
        return map;
    }

    /**
     * 重载方法 Map<String, Object> json2map(String jsonStr, String
     * fieldName,Class<?> clazz) fieldname="data"
     * 
     */
    public static Map<String, Object> json2map(String jsonStr, Class<?> clazz) {
        return json2map(jsonStr, "data", clazz);
    }

    /**
     * json非基本类型的转换
     * 
     * @param String jsonStr, json字符串
     * @param String fieldname, key名字
     * @param Class<?> clazz, fieldname的值需要转换的class
     * @return Map<String, Object>
     * 
     */
    @SuppressWarnings({ "unchecked" })
    public static Map<String, Object> json2map(String jsonStr, String fieldname, Class<?> clazz) {
        Map<String, Object> map = null;
        try {
            map = OBJECT_MAPPER.readValue(jsonStr, Map.class);
            Object value = map.get(fieldname);
            Class<?> valueClazz = value.getClass();

            // 处理对象
            if (valueClazz == LinkedHashMap.class) {
                // LinkedHashMap<String, Object> tmpMap = (LinkedHashMap) value;
                // Object clazzObj = getObject(clazz, tmpMap);
                Object clazzObj = OBJECT_MAPPER.readValue(getJsonData(jsonStr, fieldname), clazz);
                map.put(fieldname, clazzObj);
                // 处理对象list
            } else if (valueClazz == ArrayList.class) {
                // List list = new ArrayList();
                // ArrayList tmpList = (ArrayList) value;
                // for (Object obj : tmpList) {
                // LinkedHashMap<String, Object> tmpMap = (LinkedHashMap) obj;
                // Object clazzObj = getObject(clazz, tmpMap);
                // list.add(clazzObj);
                // }
                Object clazzObj = OBJECT_MAPPER.readValue(getJsonData(jsonStr, fieldname),
                        TypeFactory.collectionType(List.class, clazz));
                map.put(fieldname, clazzObj);
            }
            // 基本类型不处理
        } catch (Exception e) {
            logger.warn("json=" + jsonStr + ", fieldname=" + fieldname + ", clazz=" + clazz + " , json2map fail", e);
        }
        return map;
    }

    /**
     * 负责非基本类型对象的转换，map 转换为 指定的 class
     * 
     * @param Class<?> clazz
     * @param LinkedHashMap<String, Object> map
     * @return Object
     */
    public static Object getObject(Class<?> clazz, LinkedHashMap<String, Object> map) {
        Object clazzObj = null;
        try {
            clazzObj = clazz.newInstance();
            List<Field> fields = getFields(clazz);
            for (Field field : fields) {
                Object value = map.get(field.getName());
                if (value == null) {
                    continue;
                }
                set(clazzObj, field, value);
            }
        } catch (Exception e) {
            logger.warn("clazz=" + clazz + ", map=" + map + " , getObject fail", e);
        }
        return clazzObj;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static void set(Object targetObj, Field field, Object value) {
        try {
            Class<?> fieldClazz = field.getType();
            Type fieldType = field.getGenericType();
            Class<?> valueClazz = value.getClass();
            // 基本类型
            if (value instanceof String || value instanceof Integer || value instanceof Long || value instanceof Float
                    || value instanceof Double || value instanceof Boolean) {
                field.set(targetObj, value);
            } else {
                // 对象
                if (valueClazz == java.util.LinkedHashMap.class) {
                    List<Field> subFields = getFields(fieldClazz);
                    if (subFields == null) {
                        return;
                    }
                    Object subObject = fieldClazz.newInstance();
                    LinkedHashMap<String, Object> map = (LinkedHashMap) value;
                    for (Field subField : subFields) {
                        Object subValue = map.get(subField.getName());
                        if (subValue == null) {
                            continue;
                        }
                        set(subObject, subField, subValue);
                    }
                    field.set(targetObj, subObject);
                    // list结构
                } else if (valueClazz == ArrayList.class) {
                    // 对象
                    if (fieldType instanceof Class) {
                        // 对象数组
                        if (fieldClazz.isArray()) {
                            Class<?> paramClazz = (Class) fieldClazz.getComponentType();
                            ArrayList tmpList = (ArrayList) value;
                            Object objArr = Array.newInstance(paramClazz, tmpList.size());
                            for (int i = 0; i < tmpList.size(); i++) {
                                LinkedHashMap<String, Object> tmpMap = (LinkedHashMap) tmpList.get(i);
                                Object obj = getObject(paramClazz, tmpMap);
                                Array.set(objArr, i, obj);
                            }
                            field.set(targetObj, objArr);
                        } else {
                            field.set(targetObj, value);
                        }
                        // 范型
                    } else if (fieldType instanceof ParameterizedType) {
                        if (fieldClazz == List.class) {
                            Class<?> paramClazz = (Class) ((ParameterizedType) field.getGenericType())
                                    .getActualTypeArguments()[0];
                            if (paramClazz != String.class && paramClazz != Integer.class) {
                                ArrayList tmpList = (ArrayList) value;
                                List list = new ArrayList();
                                for (Object obj : tmpList) {
                                    LinkedHashMap<String, Object> tmpMap = (LinkedHashMap) obj;
                                    Object clazzObj = getObject(paramClazz, tmpMap);
                                    if (clazzObj != null) {
                                        list.add(clazzObj);
                                    }
                                }
                                field.set(targetObj, list);
                            } else {
                                field.set(targetObj, value);
                            }
                        } else {
                            field.set(targetObj, value);
                        }
                        // 基本类型数组
                    } else if (fieldType instanceof GenericArrayType) {
                        if (fieldClazz == String[].class) {
                            field.set(targetObj, ((ArrayList) value).toArray(new String[0]));
                        } else if (fieldClazz == Integer[].class) {
                            field.set(targetObj, ((ArrayList) value).toArray(new Integer[0]));
                        } else {
                            field.set(targetObj, value);
                        }
                        // 其他
                    } else {
                        field.set(targetObj, value);
                    }
                }
            }
        } catch (Exception e) {
            logger.warn(e.getMessage(), e);
        }
    }

    private static List<Field> getFields(Class<?> clazz) {
        List<Field> fields = fieldCache.get(clazz);
        if (fields == null) {
            synchronized (fieldCache) {
                fieldCache.put(clazz, fields = collectFieldInfo(clazz));
            }
        }
        return fields;
    }

    private static List<Field> collectFieldInfo(Class<?> clazz) {
        List<Field> list = new ArrayList<Field>();
        AccessibleObject[] fields = clazz.getDeclaredFields();
        for (AccessibleObject ao : fields) {
            ao.setAccessible(true);
            Field field = (Field) ao;
            list.add(field);
        }
        return list;
    }

    /**
     * 将对象转换为json串 name:wangzq Dec 6, 2011
     * 
     * @param obj
     * @return
     */
    public static String toJson(Object obj) {
        String jsonstr = null;
        try {
            jsonstr = OBJECT_MAPPER.writeValueAsString(obj);
        } catch (Exception e) {
            logger.warn("obj=" + obj + " , toJson fail", e);
        }
        return jsonstr;
    }

    public static String toUnicodeJson(Object obj) {
        String jsonstr = null;
        try {
            jsonstr = UNICODE_SERIALIZER_OBJECT_MAPPER.writeValueAsString(obj);
        } catch (Exception e) {
            logger.warn("obj=" + obj + " , toUnicodeJson fail", e);
        }
        return jsonstr;
    }

    /**
     * 将json串转换成对象
     * 
     * @param json
     * @param obj
     * @return
     */
    public static Object toObject(String json, Class<?> objclass) {
        if (json == null || json.length() == 0) {
            return null;
        }

        Object object = null;
        try {
            object = OBJECT_MAPPER.readValue(json, objclass);
        } catch (Exception e) {
            logger.warn("json=" + json + ", objclass=" + objclass + " , toObject fail", e);
        }

        return object;
    }

    public static Map<String, String> getRootJson(String json) {
        JsonNode rootNode = null;
        try {
            rootNode = OBJECT_MAPPER.readTree(json);
        } catch (Exception e) {
            logger.warn("json=" + json + " , getRootJson fail", e);
        }
        if (rootNode == null) {
            return null;
        }
        Map<String, String> map = new HashMap<String, String>();
        Iterator<String> names = rootNode.getFieldNames();
        // Iterator<JsonNode> values = rootNode.getElements();
        // for (; names.hasNext(); values.hasNext()) {
        // map.put(names.next(), values.next().toString());
        // }
        while (names.hasNext()) {
            String name = names.next();
            JsonNode node = rootNode.path(name);
            if (node.isNull()) {
                continue;
            }
            String value = null;
            if (node.isObject() || node.isArray() || node.isPojo()) {
                value = node.toString();
            } else {
                value = node.getValueAsText();
            }
            map.put(name, value);
        }
        return map;
    }

    public static String getJsonData(String json, String fieldname) {
        JsonNode rootNode = null;
        try {
            rootNode = OBJECT_MAPPER.readTree(json);
        } catch (Exception e) {
            logger.warn("json=" + json + ", fieldname=" + fieldname + " , getJsonData fail", e);
        }
        if (rootNode == null) {
            return null;
        }
        String value = null;
        Iterator<String> names = rootNode.getFieldNames();
        while (names.hasNext()) {
            String name = names.next();
            if (!fieldname.equals(name)) {
                continue;
            }
            JsonNode node = rootNode.path(name);
            if (node.isObject() || node.isArray() || node.isPojo()) {
                value = node.toString();
            } else {
                value = node.getValueAsText();
            }
            break;
        }
        return value;
    }

    /**
     * 根据fieldname，获取指定的未加工的子json
     * 
     * @param json
     * @param fieldname
     * @return
     */
    public static String getRawJsonData(String json, String fieldname) {
        JsonNode rootNode = null;
        try {
            rootNode = OBJECT_MAPPER.readTree(json);
        } catch (Exception e) {
            logger.warn("json=" + json + ", fieldname=" + fieldname + " , getRawJsonData fail", e);
        }
        if (rootNode == null) {
            return null;
        }
        String value = null;
        Iterator<String> names = rootNode.getFieldNames();
        while (names.hasNext()) {
            String name = names.next();
            if (!fieldname.equals(name)) {
                continue;
            }
            JsonNode node = rootNode.path(name);
            value = node.toString();
            break;
        }
        return value;
    }


}
