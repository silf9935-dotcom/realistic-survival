package ru.realisticsurvival;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.realisticsurvival.core.config.SurvivalConfig;
import ru.realisticsurvival.player.SurvivalRuntime;
import ru.realisticsurvival.player.InjurySystem;
import ru.realisticsurvival.item.MedicalItems;
import ru.realisticsurvival.npc.SurvivorNpcSystem;
import ru.realisticsurvival.npc.SurvivorCombatSystem;
import ru.realisticsurvival.npc.SurvivorProgressionSystem;
import ru.realisticsurvival.npc.SurvivorCampSystem;

public final class RealisticSurvival implements ModInitializer {
    public static final String MOD_ID = "realistic_survival";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        SurvivalConfig.load();
        MedicalItems.initialize();
        SurvivalRuntime.register();
        InjurySystem.register();
        SurvivorNpcSystem.register();
        SurvivorCombatSystem.register();
        SurvivorProgressionSystem.register();
        SurvivorCampSystem.register();
        LOGGER.info("Realistic Survival survival, injury and survivor NPC systems initialized.");
    }
}
