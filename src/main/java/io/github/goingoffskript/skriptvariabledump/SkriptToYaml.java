package io.github.goingoffskript.skriptvariabledump;

import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.util.Color;
import ch.njol.skript.util.Date;
import ch.njol.skript.util.Timespan;
import pl.tlinkowski.annotation.basic.NullOr;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class SkriptToYaml
{
    private SkriptToYaml() {}
    
    private static final Map<Class<?>, BiConsumer<Object, Map<String, Object>>> ADAPTERS = new HashMap<>();
    
    static
    {
        adapts(ClassInfo.class, (info, map) -> {
            map.put("type", info.getName().getSingular());
        });
        
        adapts(Color.class, (color, map) -> {
            map.put("color", color.getName());
        });
        
        adapts(Date.class, (date, map) -> {
            map.put("timestamp", date.getTimestamp());
        });
       
        adapts(Timespan.class, (timespan, map) -> {
            map.put("milliseconds", timespan.getMilliSeconds());
        });
    }
    
    @SuppressWarnings("unchecked")
    private static <T> void adapts(Class<T> clazz, BiConsumer<T, Map<String, Object>> adapter)
    {
        ADAPTERS.put(clazz, (object, map) -> {
            map.put("==", clazz.getSimpleName());
            adapter.accept((T) object, map);
        });
    }
    
    public static Object adapt(Object object)
    {
        @NullOr BiConsumer<Object, Map<String, Object>> adapter = ADAPTERS.get(object.getClass());
        if (adapter == null) { return object; }
        
        Map<String, Object> map = new LinkedHashMap<>();
        adapter.accept(object, map);
        return map;
    }
}
