package agh.counter;

import agh.Demo.Counter;
import com.zeroc.Ice.Current;

public class DedicatedCounterI implements Counter {
    private int value = 0;

    @Override
    public void increment(Current __current) {
        value++;
        System.out.println("[DEDICATED] Dla obiektu: " + __current.id.name + ", Nowa wartosc: " + value);
    }

    @Override
    public int getValue(Current __current) {
        return value;
    }

    @Override
    public void reset(Current __current) {
        value = 0;
        System.out.println("[DEDICATED] Dla obiektu: " + __current.id.name + ", Reset: 0");
    }
}
