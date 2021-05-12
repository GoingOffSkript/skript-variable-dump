package com.github.goingoffskript.skriptvariabledump;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class VariableDumpPlugin extends JavaPlugin
{
    @Override
    public void onEnable()
    {
        if (SkriptVariableDumper.isInvalid()) { setEnabled(false); }
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        getServer().getScheduler().runTaskAsynchronously(this, SkriptVariableDumper.task(sender));
        return true;
    }
}
