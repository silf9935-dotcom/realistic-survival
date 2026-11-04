package ru.realisticsurvival.item;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.CreativeModeTabs;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import ru.realisticsurvival.RealisticSurvival;

public final class MedicalItems {
    public static final Item BANDAGE = register("bandage", new Item.Properties().stacksTo(16));

    private MedicalItems() {}

    private static Item register(String path, Item.Properties properties) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM,
                ResourceLocation.fromNamespaceAndPath(RealisticSurvival.MOD_ID, path));
        return Registry.register(BuiltInRegistries.ITEM, key, new Item(properties.setId(key)));
    }

    public static void initialize() {
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.INGREDIENTS)
                .register(entries -> entries.accept(BANDAGE));
    }
}
