package ru.realisticsurvival;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.realisticsurvival.core.config.SurvivalConfig;
import ru.realisticsurvival.player.SurvivalRuntime;
import ru.realisticsurvival.player.InjurySystem;

public final class RealisticSurvival implements ModInitializer {
    public static final String MOD_ID = "realistic_survival";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        SurvivalConfig.load();
        SurvivalRuntime.register();
        InjurySystem.register();
        LOGGER.info("Realistic Survival survival systems initialized.");
    }
}
