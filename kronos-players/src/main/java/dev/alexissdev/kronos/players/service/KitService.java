package dev.alexissdev.kronos.players.service;

import dev.alexissdev.kronos.players.domain.KitType;
import org.bukkit.inventory.PlayerInventory;

public interface KitService {

    void applyKit(PlayerInventory inventory, KitType type);
}
