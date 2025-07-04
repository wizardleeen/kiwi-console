package org.kiwi.console.util;

import java.util.Map;

public record YmlConfig(Map<String, Object> config) {

    public Object get(String key) {
        Object value = config.get(key);
        if (value == null) {
            throw new ConfigException("Cannot find configuration for: " + key);
        }
        return value;
    }

    public Object tryGet(String key1, String key2) {
        if (config.get(key1) instanceof Map<?,?> subMap)
            return subMap.get(key2);
        else
            return null;
    }

    public Object get(String key1, String key2) {
        Object value = config.get(key1);
        if (value instanceof Map<?, ?> map) {
            return map.get(key2);
        }
        throw new ConfigException("Config find configuration for: " + key1 + "." + key2);
    }

    public String tryGetString(String key1, String key2) {
        var value = tryGet(key1, key2);
        if (value != null) {
            if (value instanceof String s)
                return s;
            else
                throw new ConfigException("Invalid configuration for: " + key1 + "." + key2 + ", expected String");
        }
        else
            return null;
    }

    public String getString(String key1, String key2) {
        if (get(key1, key2) instanceof String s)
            return s;
        else
            throw new ConfigException("Invalid configuration for: " + key1 + "." + key2 + ", expected String");
    }


    public Object get(String key1, String key2, String key3) {
        Object value = config.get(key1);
        if (value instanceof Map<?, ?> map1) {
            value = map1.get(key2);
            if (value instanceof Map<?, ?> map2) {
                return map2.get(key3);
            }
        }
        throw new ConfigException("Cannot find configuration for: " + key1 + "." + key2 + "." + key3);
    }

    public String getString(String key1, String key2, String key3) {
        Object value = get(key1, key2, key3);
        if (value instanceof String s) {
            return s;
        } else {
            throw new ConfigException("Invalid configuration for: " + key1 + "." + key2 + "." + key3 + ", expected String");
        }
    }

    public Integer tryGetInt(String key1, String key2) {
        var value = tryGet(key1, key2);
        if (value != null) {
            if (value instanceof Integer i)
                return i;
            else
                throw new ConfigException("Invalid configuration for: " + key1 + "." + key2 + ", expected Integer");
        }
        else
            return null;
    }

    public long getLong(String key1, String key2) {
        var value = tryGet(key1, key2);
        if (value != null) {
            if (value instanceof Integer i)
                return i;
            else if (value instanceof Long l)
                return l;
            else
                throw new ConfigException("Invalid configuration for: " + key1 + "." + key2 + ", expected Integer");
        }
        else
            throw new ConfigException("Cannot find configuration for: " + key1 + "." + key2);
    }


}
