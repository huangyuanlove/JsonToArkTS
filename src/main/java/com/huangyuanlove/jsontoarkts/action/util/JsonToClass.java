package com.huangyuanlove.jsontoarkts.action.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class JsonToClass {
    private HashMap<String, HashMap<String, String>> classMap = new HashMap<>();
    private boolean withTrace;
    private DefaultProp defaultProp = DefaultProp.nullable;

    public JsonToClass(boolean withTrace, DefaultProp defaultProp) {
        this.withTrace = withTrace;
        this.defaultProp = defaultProp;
    }

    public void visitRoot(JsonElement root, String className) {
        if (root.isJsonObject()) {
            visitObject(root.getAsJsonObject(), className);

        } else if (root.isJsonArray()) {
            visitArray(root.getAsJsonArray(), className, null);

        } else if (root.isJsonNull()) {

        } else if (root.isJsonPrimitive()) {

        }
    }


    private String getClassNameWithCamelcase(String className) {
        className = className.substring(0, 1).toUpperCase() + className.substring(1);
        return toCamelCase(className);
    }

    private String toCamelCase(String name) {
        StringBuilder result = new StringBuilder();
        boolean nextUpper = false;

        for (int i = 0; i < name.length(); i++) {
            char currentChar = name.charAt(i);

            if (currentChar == '_') {
                nextUpper = true;
            } else {
                if (nextUpper) {
                    result.append(Character.toUpperCase(currentChar));
                    nextUpper = false;
                } else {
                    result.append(currentChar);
                }
            }
        }
        return result.toString();
    }

    private HashMap<String, String> getClassPropMap(String className) {
        className = getClassNameWithCamelcase(className);
        if (classMap.containsKey(className)) {
            return classMap.get(className);
        } else {
            HashMap<String, String> propMap = new HashMap<>();
            classMap.put(className, propMap);
            return propMap;
        }
    }

    private String getJsonPrimitive(JsonPrimitive jsonPrimitive) {
        String propType = "Object";
        if (jsonPrimitive.isBoolean()) {
            propType = "boolean";
        } else if (jsonPrimitive.isNumber()) {
            propType = "number";
        } else if (jsonPrimitive.isString()) {
            propType = "string";
        }
        return propType;
    }

    private void visitObject(JsonObject jsonObject, String className) {
        Set<Map.Entry<String, JsonElement>> entrySet = jsonObject.entrySet();
        if(entrySet.size() ==0){
            getClassPropMap(getClassNameWithCamelcase(className));
            return ;
        }
        entrySet.forEach(entry -> {
            System.out.println(entry.getKey() + " : " + entry.getValue() + " in " + className);
            HashMap<String, String> propMap = getClassPropMap(className);
            String propType = "Object";

            JsonElement valueElement = entry.getValue();

            if (valueElement.isJsonObject()) {
                propType = entry.getKey();
                propMap.put(entry.getKey(), getClassNameWithCamelcase(propType));
            } else if (valueElement.isJsonArray()) {
                //在 visitArray 中处理一下类型
            } else if (valueElement.isJsonPrimitive()) {
                propType = getJsonPrimitive(valueElement.getAsJsonPrimitive());
                propMap.put(entry.getKey(), propType);
            }


            JsonElement jsonElement = entry.getValue();
            if (jsonElement.isJsonObject()) {
                visitObject(jsonElement.getAsJsonObject(), entry.getKey());

            } else if (jsonElement.isJsonArray()) {

                visitArray(jsonElement.getAsJsonArray(), className, entry.getKey());

            } else if (jsonElement.isJsonNull()) {


            } else if (jsonElement.isJsonPrimitive()) {


            }
        });
    }

    private void visitArray(JsonArray jsonArray, String className, String propName) {

        if(jsonArray.size() == 0){
            getClassPropMap(className).put(propName, getClassNameWithCamelcase(propName) + "[]");
            getClassPropMap(getClassNameWithCamelcase(propName));
            return;
        }

        jsonArray.forEach(jsonElement -> {
            if (jsonElement.isJsonObject()) {

                getClassPropMap(className).put(propName, getClassNameWithCamelcase(propName) + "[]");

                visitObject(jsonElement.getAsJsonObject(), propName);

            } else if (jsonElement.isJsonArray()) {
                visitArray(jsonElement.getAsJsonArray(), className, propName);
            } else if (jsonElement.isJsonNull()) {

            } else if (jsonElement.isJsonPrimitive()) {

                getClassPropMap(className).put(propName, getJsonPrimitive(jsonElement.getAsJsonPrimitive()) + "[]");


            }
        });


    }


    public String toCode() {


        StringBuffer result = new StringBuffer();
        classMap.forEach((k, v) -> {
            if (withTrace) {
                result.append("@ObservedV2").append("\n");
            }

            result.append("export class ").append(k).append("{\n");

            v.forEach((propName, propType) -> {
                result.append("\t");
                if (withTrace) {
                    result.append("@Trace ");
                }
                result.append(propName);
                if (defaultProp == DefaultProp.defaultValue) {
                    result.append(":").append(propType);
                    if ("string".equals(propType)) {
                        result.append(" = \"").append("\"");
                    } else if ("boolean".equals(propType)) {
                        result.append(" = false");

                    } else if ("number".equals(propType)) {
                        result.append(" = 0");
                    } else if (propType.indexOf("[]") > 0) {
                        result.append(" = []");
                    } else {
                        result.append(" = new ").append(propType).append("()");
                    }


                } else if (defaultProp == DefaultProp.nullable) {
                    result.append("?:");
                    result.append(propType);
                }


                result.append("\n");
            });

            result.append("}\n\n");

        });
        return result.toString();
    }

}
