package io.github.goingoffskript.skriptvariabledump;

import ch.njol.skript.Skript;
import ch.njol.skript.variables.Variables;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import pl.tlinkowski.annotation.basic.NullOr;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;

// No reason to expose this class, make it package-private
final class SkriptVariableDumper
{
    private SkriptVariableDumper() {}
    
    private static final AtomicBoolean IS_DUMPING_VARIABLES = new AtomicBoolean(false);
    
    private static final @NullOr Method GET_VARIABLES = method("getVariables");
    
    private static final @NullOr Method GET_READ_LOCK = method("getReadLock");
    
    private static @NullOr Method method(String declaredMethodName)
    {
        try
        {
            Method method = Variables.class.getDeclaredMethod(declaredMethodName);
            method.setAccessible(true);
            return method;
        }
        catch (NoSuchMethodException e)
        {
            e.printStackTrace();
            return null;
        }
    }
    
    static boolean isInvalid() { return GET_VARIABLES == null || GET_READ_LOCK == null; }
    
    @SuppressWarnings("unchecked")
    private static <T> T invoke(@NullOr Method method)
    {
        if (method == null) { throw new IllegalArgumentException(); }
        try { return (T) method.invoke(null); }
        catch (IllegalAccessException | InvocationTargetException e) { throw new RuntimeException(e); }
    }
    
    private static Map<String, Object> variables() { return invoke(GET_VARIABLES); }
    
    private static Lock readLock() { return invoke(GET_READ_LOCK); }
    
    private static String key(@NullOr String key)
    {
        return (key == null || key.isEmpty()) ? "<none>" : key;
    }
    
    private static @NullOr Object value(@NullOr Object value)
    {
        return (value == null) ? null : SkriptToYaml.adapt(value);
    }
    
    @SuppressWarnings("unchecked")
    private static void dump(ConfigurationSection section, Map<String, Object> vars)
    {
        for (Map.Entry<String, Object> entry : vars.entrySet())
        {
            String key = key(entry.getKey());
            @NullOr Object value = value(entry.getValue());
            
            if (value instanceof Map) { dump(section.createSection(key), (Map<String, Object>) value); }
            else { section.set(key, value); }
        }
    }
    
    private static Path dumpFilePath(Path directory)
    {
        for (int i = 1 ;; i++)
        {
            Path path = directory.resolve("skript-variables-dump." + LocalDate.now() + "_" + i + ".yml");
            if (!Files.isRegularFile(path)) { return path; }
        }
    }
    
    static Runnable task(CommandSender sender)
    {
        return () ->
        {
            boolean isAvailable = IS_DUMPING_VARIABLES.compareAndSet(false, true);
            
            if (!isAvailable)
            {
                sender.sendMessage("Already dumping variables, be patient...");
                return;
            }
            
            sender.sendMessage("Dumping skript variables...");
            
            YamlConfiguration data = new YamlConfiguration();
            data.options().pathSeparator('\0');
            
            data.options().header(
                "Skript Variable Dump: " + LocalDateTime.now() + "\n" +
                "Skript Version: " + Skript.getVersion() + "\n"
            );
            
            try
            {
                readLock().lock();
                try { dump(data, variables()); }
                finally { readLock().unlock(); }
                
                Path dumpsDir = Skript.getInstance().getDataFolder().toPath().resolve("dumps");
                Path dumpFile = dumpFilePath(dumpsDir);
                
                try
                {
                    if (!Files.isDirectory(dumpsDir)) { Files.createDirectories(dumpsDir); }
                    
                    byte[] bytes = data.saveToString().getBytes(StandardCharsets.UTF_8);
                    Files.write(dumpFile, bytes, StandardOpenOption.CREATE_NEW);
                    
                    sender.sendMessage("Saved variable dump: " + ChatColor.GOLD + dumpFile);
                }
                catch (IOException e)
                {
                    sender.sendMessage("Failed to dump variables: " + ChatColor.RED + e.getMessage());
                    e.printStackTrace();
                }
            }
            finally { IS_DUMPING_VARIABLES.set(false); }
        };
    }
}
