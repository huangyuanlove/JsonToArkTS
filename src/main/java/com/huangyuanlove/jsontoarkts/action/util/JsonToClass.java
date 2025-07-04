package com.huangyuanlove.jsontoarkts.action.util;


import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.huangyuanlove.jsontoarkts.action.GenerateConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class JsonToClass {

    private final HashMap<String, HashMap<String, String>> classMap = new HashMap<>();

    private final GenerateConfig generateConfig;

    public JsonToClass(GenerateConfig generateConfig) {
        this.generateConfig = generateConfig;
    }

    public void visitRoot(JsonElement root, String className) {
        if (root.isJsonObject()) {
            visitObject(root.getAsJsonObject(), className);

        } else if (root.isJsonArray()) {
            visitArray(root.getAsJsonArray(), className, null);

        } else if (root.isJsonNull()) {
            //pass
        } else if (root.isJsonPrimitive()) {
            //pass
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
        entrySet.forEach(entry -> {
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
                //pass
            } else if (jsonElement.isJsonPrimitive()) {
                //pass
            }
        });
    }

    private void visitArray(JsonArray jsonArray, String className, String propName) {
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
            if (generateConfig.withTrace) {
                result.append("@ObservedV2").append("\n");
            }

            result.append("export class ").append(k).append("{\n");

            v.forEach((propName, propType) -> {
                result.append("\t");
                if (generateConfig.withTrace) {
                    result.append("@Trace ");
                }
                result.append(propName);

                if (generateConfig.nullable) {
                    result.append(" ?: ").append(propType);
                } else {
                    result.append(" : ").append(propType);
                }
                //没有勾选可空，一定需要默认值
                if (generateConfig.withDefaultValue || !generateConfig.nullable) {
                    result.append(" = ");


                    if ("string".equals(propType)) {
                        result.append("\"").append("\"");
                    } else if ("boolean".equals(propType)) {
                        result.append("false");

                    } else if ("number".equals(propType)) {
                        result.append("0");
                    } else if (propType.contains("[]")) {
                        result.append("[]");
                    } else {
                        result.append("new ").append(propType).append("()");
                    }

                }
                result.append("\n");

            });

            if(generateConfig.withFromJson){
                result.append("fromJSON(jsonStr:string):").append(k).append("{\n")
                        .append("let json:").append(k).append(" =  JSON.parse(jsonStr) as ").append(k).append("\n")
                        .append("return this.fromObject(json)\n}");

                result.append("fromObject(obj:").append(k).append("):").append(k).append("{\n");
                result.append("let tmp:").append(k).append(" = new ").append(k).append("()\n");
                result.append("if(obj){");

                //处理属性
                v.forEach((propName, propType) -> {
                    if ("string".equals(propType) || "boolean".equals(propType) || "number".equals(propType)) {
                        result.append("if(obj.").append(propName).append(" != undefined){\n");
                        result.append("tmp.").append(propName).append("= obj.").append(propName).append(";\n}");
                    }else if (propType.indexOf("[]") > 0) {
                        result.append("if(obj.").append(propName).append(" != undefined){\n");
                        result.append("tmp.").append(propName).append(" = []").append(";\n");
                        result.append("obj.").append(propName).append(".forEach((value) => {").append("\n");
                        if (propType.startsWith("string") || propType.startsWith("boolean") || propType.startsWith("number")) {
                            result.append("tmp.").append(propName).append(".push(value)\n");
                        } else {
                            result.append("tmp.").append(propName).append(".push(")
                                    .append("new ").append(propType.replace("[]","")).append("().fromObject(value)")
                                    .append(")\n");
                        }
                        result    .append("})");
                        result.append("\n}");
                    }else{
                        result.append("if(obj.").append(propName).append(" != undefined){\n");
                        result.append("tmp.").append(propName).append("= new ").append(propType).append("().fromObject(obj.").append(propName).append(");\n}");
                    }

                });

                result.append("}");
                result.append("return tmp");

                result.append("}");
            }



            result.append("}\n\n");

        });
        return result.toString();
    }


}

