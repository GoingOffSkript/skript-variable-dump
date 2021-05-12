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

public class SkriptVariableDumper
{
    private SkriptVariableDumper() {}
    
    private static final AtomicBoolean IS_DUMPING_VARIABLES = new AtomicBoolean(false);
    
    private static final @NullOr Method GET_VARIABLES;
    
    static
    {
        @NullOr Method getVariables = null;
        
        try
        {
            getVariables = Variables.class.getDeclaredMethod("getVariables");
            getVariables.setAccessible(true);
        }
        catch (NoSuchMethodException e) { e.printStackTrace(); }
        
        GET_VARIABLES = getVariables;
    }
    
    private static final @NullOr Method GET_READ_LOCK;
    
    static
    {
        @NullOr Method getReadLock = null;
        
        try
        {
            getReadLock = Variables.class.getDeclaredMethod("getReadLock");
            getReadLock.setAccessible(true);
        }
        catch (NoSuchMethodException e) { e.printStackTrace(); }
        
        GET_READ_LOCK = getReadLock;
    }
    
    static boolean isInvalid() { return GET_VARIABLES == null || GET_READ_LOCK == null; }
    
    @SuppressWarnings("unchecked")
    private static Map<String, Object> variables()
    {
        if (GET_VARIABLES == null) { throw new IllegalStateException(); }
        try { return (Map<String, Object>) GET_VARIABLES.invoke(null); }
        catch (IllegalAccessException | InvocationTargetException e) { throw new RuntimeException(e); }
    }
    
    private static Lock readLock()
    {
        if (GET_READ_LOCK == null) { throw new IllegalStateException(); }
        try { return (Lock) GET_READ_LOCK.invoke(null); }
        catch (IllegalAccessException | InvocationTargetException e) { throw new RuntimeException(e); }
    }
    
    @SuppressWarnings("ConstantConditions")
    private static String key(Map.Entry<String, Object> entry)
    {
        @NullOr String key = entry.getKey();
        return (key == null || key.isEmpty()) ? "<none>" : key;
    }
    
    @SuppressWarnings("ConstantConditions")
    private static @NullOr Object value(Map.Entry<String, Object> entry)
    {
        @NullOr Object value = entry.getValue();
        return (value == null) ? null : SkriptToYaml.adapt(value);
    }
    
    @SuppressWarnings("unchecked")
    private static void dump(ConfigurationSection section, Map<String, Object> vars)
    {
        for (Map.Entry<String, Object> entry : vars.entrySet())
        {
            String key = key(entry);
            @NullOr Object value = value(entry);
            
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
            boolean available = IS_DUMPING_VARIABLES.compareAndSet(false, true);
            
            if (!available)
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
