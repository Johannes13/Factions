package com.massivecraft.factions.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Creature;
import org.bukkit.entity.CreatureType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class MiscUtil
{	
	public static CreatureType creatureTypeFromEntity(Entity entity)
	{
		if ( ! (entity instanceof Creature))
		{
			return null;
		}
		
		String name = entity.getClass().getSimpleName();
		name = name.substring(5); // Remove "Craft"
		
		return CreatureType.fromName(name);
	}
	
	// Inclusive range
	public static long[] range(long start, long end) {
		long[] values = new long[(int) Math.abs(end - start) + 1];
		
		if (end < start) {
			long oldstart = start;
			start = end;
			end = oldstart;
		}
	
		for (long i = start; i <= end; i++) {
			values[(int) (i - start)] = i;
		}
		
		return values;
	}
	
	/// TODO create tag whitelist!!
	public static HashSet<String> substanceChars = new HashSet<String>(Arrays.asList(new String []{
	"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F", "G", "H", 
	"I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z", 
	"a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", 
	"s", "t", "u", "v", "w", "x", "y", "z"
	}));
			
	public static String getComparisonString(String str)
	{
		String ret = "";
		
		for (char c : str.toCharArray())
		{
			if (substanceChars.contains(String.valueOf(c)))
			{
				ret += c;
			}
		}
		return ret.toLowerCase();
	}
	
	private static Plugin ess = null;
	private static ClassLoader esscl = null;
	private static Class<?> essic = null;
	private static Class<?> esstrade = null;
	private static Constructor<?> esstradec = null;
	private static Method essgetuser = null;
	private static Class<?> essuserc = null;
	private static Method essgettp = null;
	private static Class<?> esstpc = null;
	private static Method esstpcmd = null;
	
	public static void teleport(Player p, Location l) {
		try {
			// first we need the essentials plugin
			if (ess == null)
				ess = Bukkit.getPluginManager().getPlugin("Essentials");
			if (esscl == null)
				esscl = ess.getClass().getClassLoader();
			// ok, now we need the IEssentials interface
			if (essic == null)
				essic = esscl.loadClass("com.earth2me.essentials.IEssentials");
			// now the trade class
			if (esstrade == null)
				esstrade = esscl.loadClass("com.earth2me.essentials.Trade");
			// constructor of it
			if (esstradec == null)
				esstradec = esstrade.getConstructor(String.class, essic);
			// getter for the user
			if (essgetuser == null)
				essgetuser = essic.getMethod("getUser", Object.class);
			if (essuserc == null)
				essuserc = esscl.loadClass("com.earth2me.essentials.User");
			if (essgettp == null)
				essgettp = essuserc.getMethod("getTeleport");
			if (esstpc == null)
				esstpc = esscl.loadClass("com.earth2me.essentials.Teleport");
			if (esstpcmd == null)
				esstpcmd = esstpc.getMethod("teleport", Location.class, esstrade);
			Object trade = esstradec.newInstance(null, ess);
			Object user = essgetuser.invoke(ess, p);
			Object tp = essgettp.invoke(user);
			esstpcmd.invoke(tp, l, trade);
			// jeah, looks ugly..
			// the original is 1 line of code. But this does not really require Essentials, we have a fallback :)
		} catch (Exception e) {
			Logger.getLogger("Minecraft").log(Level.WARNING, "Exeception while trying to use Essentials to teleport", e);
			p.teleport(l);
		}
	}
	
}

