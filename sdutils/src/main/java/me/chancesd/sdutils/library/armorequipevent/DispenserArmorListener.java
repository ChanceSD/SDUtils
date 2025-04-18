package me.chancesd.sdutils.library.armorequipevent;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseArmorEvent;

/**
 * @author Arnah
 * @since Feb 08, 2019
 */
class DispenserArmorListener implements Listener {

	@EventHandler
	public void onArmorDispense(final BlockDispenseArmorEvent event) {
		final ArmorType type = ArmorType.matchType(event.getItem());
		if (type != null && event.getTargetEntity() instanceof Player) {
			final Player p = (Player) event.getTargetEntity();
			final ArmorEquipEvent armorEquipEvent = new ArmorEquipEvent(p, ArmorEquipEvent.EquipMethod.DISPENSER, type, null, event.getItem());
			Bukkit.getServer().getPluginManager().callEvent(armorEquipEvent);
			if (armorEquipEvent.isCancelled()) {
				event.setCancelled(true);
			}
		}
	}
}