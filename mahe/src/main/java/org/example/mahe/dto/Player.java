package org.example.mahe.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter @Setter
public class Player {
    private String id;
    private String nickname;
    private Map<String, Object> attributes = new HashMap<>();

    public Player(String nickname, String id) {
        this.nickname = nickname;
        this.id = id;
    }

    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    public Object getAttribute(String key) {
        return attributes.get(key);
    }

    public int getInt(String key) {
        Object val = attributes.get(key);
        if (val instanceof Number) {
            return ((Number) val).intValue();
        }
        return 0;
    }

    public String getString(String key) {
        Object val = attributes.get(key);
        return val != null ? val.toString() : "";
    }

    public boolean getBoolean(String key) {
        Object val = attributes.get(key);
        if (val instanceof Boolean) {
            return (Boolean) val;
        }
        return val != null && Boolean.parseBoolean(val.toString());
    }

    public void set(String key, Object value) {
        attributes.put(key, value);
    }
}
