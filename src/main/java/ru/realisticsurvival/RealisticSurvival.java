package ru.realisticsurvival;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.realisticsurvival.core.config.SurvivalConfig;
import ru.realisticsurvival.item.MedicalItems;
import ru.realisticsurvival.npc.CampGenerator;
import ru.realisticsurvival.npc.TerritorySystem;
import ru.realisticsurvival.npc.WorldNpcSystem;
import ru.realisticsurvival.player.InjurySystem;
import ru.realisticsurvival.player.SurvivalRuntime;
import ru.realisticsurvival.world.NatureGenerator;

public final class RealisticSurvival implements ModInitializer {
    public static final String MOD_ID = "realistic_survival";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        SurvivalConfig.load();
        MedicalItems.initialize();
        SurvivalRuntime.register();
        InjurySystem.register();
        WorldNpcSystem.register();
        TerritorySystem.register();
        CampGenerator.register();
        NatureGenerator.register();
        // Diagnostic test: NatureGenerator is enabled; AbandonedSites remains disabled.
        LOGGER.warn("Realistic Survival diagnostic mode: NatureGenerator enabled; AbandonedSites disabled.");
        LOGGER.info("Realistic Survival 1.0.0 systems initialized.");
    }
}
