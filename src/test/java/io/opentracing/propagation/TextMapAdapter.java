package io.opentracing.propagation;

import java.util.Iterator;
import java.util.Map;

/**
 * @author ashley.mercer@skylightipv.com
 */
public class TextMapAdapter implements TextMap {

    private final Map<String, String> map;

    public TextMapAdapter(Map<String, String> map) {
        this.map = map;
    }

    @Override
    public Iterator<Map.Entry<String, String>> iterator() {
        return map.entrySet().iterator();
    }

    @Override
    public void put(String key, String value) {
        map.put(key, value);
    }
}
