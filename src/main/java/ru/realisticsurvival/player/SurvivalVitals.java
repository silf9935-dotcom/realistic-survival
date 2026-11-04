package ru.realisticsurvival.player;

/** Server-side domain model for the first survival meters. Values are clamped to safe ranges. */
public final class SurvivalVitals {
    private double hunger = 100.0;
    private double thirst = 100.0;
    private double bodyTemperature = 37.0;
    private double fatigue;
    private double stamina = 100.0;
    private double sanity = 100.0;

    public double hunger() { return hunger; }
    public double thirst() { return thirst; }
    public double bodyTemperature() { return bodyTemperature; }
    public double fatigue() { return fatigue; }
    public double stamina() { return stamina; }
    public double sanity() { return sanity; }

    public void setHunger(double value) { hunger = clamp(value, 0.0, 100.0); }
    public void setThirst(double value) { thirst = clamp(value, 0.0, 100.0); }
    public void setBodyTemperature(double value) { bodyTemperature = clamp(value, 25.0, 43.0); }
    public void setFatigue(double value) { fatigue = clamp(value, 0.0, 100.0); }
    public void setStamina(double value) { stamina = clamp(value, 0.0, 100.0); }
    public void setSanity(double value) { sanity = clamp(value, 0.0, 100.0); }

    private static double clamp(double value, double min, double max) {
        if (!Double.isFinite(value)) return min;
        return Math.max(min, Math.min(max, value));
    }
}
