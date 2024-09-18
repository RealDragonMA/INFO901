package fr.usmb.process;

public class LamportClock {

    private int clock = 0;

    /**
     * Get the current value of the clock
     * @return {@link Integer} the current value of the clock
     */
    public synchronized int get() {
        return clock;
    }

    /**
     * Increment the clock by 1
     */
    public synchronized void increment() {
        clock++;
    }

    /**
     * Update the clock with the maximum value between the current value and the received value + 1
     * @param received {@link Integer} the received value
     */
    public synchronized void update(int received) {
        clock = Math.max(clock, received) + 1;
    }

}
