package com.massivecraft.factions.listeners;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;

import com.massivecraft.factions.Board;
import com.massivecraft.factions.Conf;
import com.massivecraft.factions.FLocation;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.P;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.struct.Relation;


public class FactionsBlockListener extends BlockListener
{
	public P p;
	public FactionsBlockListener(P p)
	{
		this.p = p;
	}
	
	@Override
	public void onBlockPlace(BlockPlaceEvent event)
	{
		if (event.isCancelled()) return;
		if ( ! event.canBuild()) return;
		
		// special case for flint&steel, which should only be prevented by DenyUsage list
		if (event.getBlockPlaced().getType() == Material.FIRE)
		{
			return;
		}

		if ( ! playerCanBuildDestroyBlock(event.getPlayer(), event.getBlock().getLocation(), "build", false))
		{
			event.setCancelled(true);
		}
	}

	@Override
	public void onBlockBreak(BlockBreakEvent event)
	{
		if (event.isCancelled()) return;

		if ( ! playerCanBuildDestroyBlock(event.getPlayer(), event.getBlock().getLocation(), "destroy", false))
		{
			event.setCancelled(true);
		}
	}

	@Override
	public void onBlockDamage(BlockDamageEvent event)
	{
		if (event.isCancelled()) return;

		if (event.getInstaBreak() && ! playerCanBuildDestroyBlock(event.getPlayer(), event.getBlock().getLocation(), "destroy", false))
		{
			event.setCancelled(true);
		}
	}

	@Override
	public void onBlockPistonExtend(BlockPistonExtendEvent event)
	{
		if (event.isCancelled()) return;
		if ( ! Conf.pistonProtectionThroughDenyBuild) return;

		Faction pistonFaction = Board.getFactionAt(new FLocation(event.getBlock()));

		// target end-of-the-line empty (air) block which is being pushed into, including if piston itself would extend into air
		Block targetBlock = event.getBlock().getRelative(event.getDirection(), event.getLength() + 1);

		// if potentially pushing into air in another territory, we need to check it out
		if (targetBlock.isEmpty() && !canPistonMoveBlock(pistonFaction, targetBlock.getLocation()))
		{
			event.setCancelled(true);
			return;
		}

		/*
		 * note that I originally was testing the territory of each affected block, but since I found that pistons can only push
		 * up to 12 blocks and the width of any territory is 16 blocks, it should be safe (and much more lightweight) to test
		 * only the final target block as done above
		 */
	}

	@Override
	public void onBlockPistonRetract(BlockPistonRetractEvent event)
	{
		// if not a sticky piston, retraction should be fine
		if (event.isCancelled() || !event.isSticky() || !Conf.pistonProtectionThroughDenyBuild)
		{
			return;
		}

		Location targetLoc = event.getRetractLocation();

		// if potentially retracted block is just air, no worries
		if (targetLoc.getBlock().isEmpty())
		{
			return;
		}

		Faction pistonFaction = Board.getFactionAt(new FLocation(event.getBlock()));

		if (!canPistonMoveBlock(pistonFaction, targetLoc))
		{
			event.setCancelled(true);
			return;
		}
	}

	private boolean canPistonMoveBlock(Faction pistonFaction, Location target)
	{

		Faction otherFaction = Board.getFactionAt(new FLocation(target));

		if (pistonFaction == otherFaction)
		{
			return true;
		}

		if (otherFaction.isNone())
		{
			if (!Conf.wildernessDenyBuild || Conf.worldsNoWildernessProtection.contains(target.getWorld().getName()))
			{
				return true;
			}
			return false;
		}
		else if (otherFaction.isSafeZone())
		{
			if ( ! Conf.safeZoneDenyBuild)
			{
				return true;
			}
			return false;
		}
		else if (otherFaction.isWarZone())
		{
			if ( ! Conf.warZoneDenyBuild)
			{
				return true;
			}
			return false;
		}

		Relation rel = pistonFaction.getRelationTo(otherFaction);
		boolean online = otherFaction.hasPlayersOnline();

		if
		(
			(online && (rel.isEnemy() ? Conf.territoryEnemyDenyBuild : (rel.isAlly() ? Conf.territoryAllyDenyBuild : Conf.territoryDenyBuild)))
			||
			(!online && (rel.isEnemy() ? Conf.territoryEnemyDenyBuildWhenOffline : (rel.isAlly() ? Conf.territoryAllyDenyBuildWhenOffline : Conf.territoryDenyBuildWhenOffline)))
		)
		{
			return false;
		}

		return true;
	}

	public static boolean playerCanBuildDestroyBlock(Player player, Location location, String action, boolean justCheck)
	{
		FPlayer me = FPlayers.i.get(player);
		
		if (me.isAdminBypassing())
		{
			return true;
		}

		FLocation loc = new FLocation(location);
		Faction otherFaction = Board.getFactionAt(loc);
		

		if (otherFaction.isNone())
		{
			if (!Conf.wildernessDenyBuild || Conf.worldsNoWildernessProtection.contains(location.getWorld().getName()))
			{
				return true; // This is not faction territory. Use whatever you like here.
			}
			if (!justCheck)
			{
				me.sendMessage("You can't "+action+" in the wilderness.");
			}
			return false;
		}
		else if (otherFaction.isSafeZone())
		{
			if (!Conf.safeZoneDenyBuild || Permission.MANAGE_SAFE_ZONE.has(player))
			{
				return true;
			}
			if (!justCheck) {
				me.sendMessage("You can't "+action+" in a safe zone.");
			}
			return false;
		}
		else if (otherFaction.isWarZone())
		{
			if (!Conf.warZoneDenyBuild || Permission.MANAGE_WAR_ZONE.has(player))
			{
				return true;
			}
			if (!justCheck)
			{
				me.sendMessage("You can't "+action+" in a war zone.");
			}
			return false;
		}
		
		Faction myFaction = me.getFaction();
		Relation rel = myFaction.getRelationTo(otherFaction);
		boolean ownershipFail = Conf.ownedAreasEnabled && (Conf.ownedAreaDenyBuild || Conf.ownedAreaPainBuild) && !otherFaction.playerHasOwnershipRights(me, loc);
		
		// Cancel and/or cause pain (depending on configuration) if we are not in our own territory
		if (!rel.isMember())
		{
			boolean online = otherFaction.hasPlayersOnline();
			boolean pain = (!justCheck) && rel.confPainBuild(online);
			boolean deny = rel.confDenyBuild(online);

			//hurt the player for building/destroying?
			if (pain)
			{
				player.damage(Conf.actionDeniedPainAmount);
				if (!deny)
				{
					me.sendMessage("You are hurt for "+action+" in the territory of "+otherFaction.getTag(myFaction));
					if (!Conf.ownedAreaDenyBuild)
					{
						return true;
					}
				}
			}
			if (deny)
			{
				if (!justCheck)
				{
					me.sendMessage("You can't "+action+" in the territory of "+otherFaction.getTag(myFaction));
				}
				return false;
			}
		}
		// Also cancel and/or cause pain if player doesn't have ownership rights for this claim
		else if (rel.isMember() && ownershipFail && ! Permission.OWNERSHIP_BYPASS.has(player))
		{
			if (Conf.ownedAreaPainBuild && !justCheck)
			{
				player.damage(Conf.actionDeniedPainAmount);
				if (!Conf.ownedAreaDenyBuild)
				{
					me.sendMessage("You are hurt for "+action+" in this territory, it is owned by: "+myFaction.getOwnerListString(loc));
				}
			}
			if (Conf.ownedAreaDenyBuild)
			{
				if (!justCheck)
				{
					me.sendMessage("You can't "+action+" in this territory, it is owned by: "+myFaction.getOwnerListString(loc));
				}
				return false;
			}
		}
		
		return true;
	}
}
