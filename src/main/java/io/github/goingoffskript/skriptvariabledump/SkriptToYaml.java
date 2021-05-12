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

/**
 * Skript data-to-YAML adapters.
 */
public class SkriptToYaml
{
    private SkriptToYaml() {}
    
    private static final Map<Class<?>, Adapter<?>> ADAPTERS = new HashMap<>();
    
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
    
    /**
     * Adapts data from a specific type into a
     * {@code Map<String, Object>}.
     *
     * @param <T>   data type
     */
    @FunctionalInterface
    public interface Adapter<T> extends BiConsumer<T, Map<String, Object>> {}
    
    /**
     * Registers an adapter for a specific data type.
     *
     * @param clazz     the data type's class
     * @param adapter   the adapter to register
     * @param <T>       data type
     */
    @SuppressWarnings("unchecked")
    public static <T> void adapts(Class<T> clazz, Adapter<T> adapter)
    {
        ADAPTERS.put(clazz, (object, map) -> {
            map.put("==", clazz.getSimpleName());
            adapter.accept((T) object, map);
        });
    }
    
    /**
     * Adapts an object into a string map or returns
     * it as-is if no adapter exists for its type.
     *
     * @param object    the object to adapt
     *
     * @return  the provided object
     *          or a {@code Map<String, Object>}
     */
    @SuppressWarnings("unchecked")
    public static Object adapt(Object object)
    {
        @NullOr Adapter<?> adapter = ADAPTERS.get(object.getClass());
        if (adapter == null) { return object; }
        
        Map<String, Object> map = new LinkedHashMap<>();
        ((Adapter<Object>) adapter).accept(object, map);
        return map;
    }
}
