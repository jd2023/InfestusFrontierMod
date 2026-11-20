package org.jd.infestusfrontier.core.power;

/** Integer stores and conservative conversions. All capacities are initial tuning values. */
public record PowerStore(int heat, int water, int steam, int charge, int cycles) {
    public static final int CAPACITY = 16000;
    public static final int CELL_CAPACITY = 1000;
    public static final int HEAT_PER_PULSE = 20;
    public static final int BOILER_HEAT = 10;
    public static final int BOILER_WATER = 10;
    public static final int BOILER_STEAM = 40;
    public static final int GENERATOR_STEAM = 20;
    public static final int GENERATOR_CHARGE = 40;
    public static final PowerStore EMPTY = new PowerStore(0, 0, 0, 0, 0);

    public PowerStore {
        if (!valid(heat) || !valid(water) || !valid(steam) || !valid(charge) || cycles < 0)
            throw new IllegalArgumentException("power store");
    }

    private static boolean valid(int amount) {
        return amount >= 0 && amount <= CAPACITY;
    }

    public boolean canBoil() {
        return heat >= BOILER_HEAT && water >= BOILER_WATER && steam <= CAPACITY - BOILER_STEAM;
    }

    public PowerStore boil() {
        if (!canBoil()) return this;
        return new PowerStore(
                Math.subtractExact(heat, BOILER_HEAT),
                Math.subtractExact(water, BOILER_WATER),
                Math.addExact(steam, BOILER_STEAM),
                charge,
                nextCycle());
    }

    public boolean canGenerate() {
        return steam >= GENERATOR_STEAM && charge <= CAPACITY - GENERATOR_CHARGE;
    }

    public PowerStore generate() {
        if (!canGenerate()) return this;
        return new PowerStore(
                heat,
                water,
                Math.subtractExact(steam, GENERATOR_STEAM),
                Math.addExact(charge, GENERATOR_CHARGE),
                nextCycle());
    }

    public PowerStore heat(int value) {
        return new PowerStore(value, water, steam, charge, cycles);
    }

    public PowerStore water(int value) {
        return new PowerStore(heat, value, steam, charge, cycles);
    }

    public PowerStore steam(int value) {
        return new PowerStore(heat, water, value, charge, cycles);
    }

    public PowerStore charge(int value) {
        return new PowerStore(heat, water, steam, value, cycles);
    }

    private int nextCycle() {
        return cycles == Integer.MAX_VALUE ? cycles : Math.incrementExact(cycles);
    }
}
