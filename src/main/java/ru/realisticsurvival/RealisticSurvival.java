package ru.realisticsurvival;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.realisticsurvival.core.config.SurvivalConfig;

public final class RealisticSurvival implements ModInitializer {
    public static final String MOD_ID = "realistic_survival";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        SurvivalConfig.load();
        LOGGER.info("Realistic Survival foundation loaded. Gameplay systems are being added in stages.");
    }
}
