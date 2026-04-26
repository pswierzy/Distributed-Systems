package agh.counter;

import agh.Demo.Counter;
import com.zeroc.Ice.Current;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SharedCounterI implements Counter {
    private final Map<String, Integer> counterMap = new ConcurrentHashMap<>();

    @Override
    public void increment(Current __current) {
        String objName = __current.id.name;
        int newValue = counterMap.merge(objName, 1 , Integer::sum);
        System.out.println("[SHARED] Dla obiektu: " + objName + ", Nowa wartosc: " + newValue);
    }

    @Override
    public int getValue(Current __current) {
        return counterMap.getOrDefault(__current.id.name, 0);
    }

    @Override
    public void reset(Current __current) {
        String objName = __current.id.name;
        counterMap.put(objName, 0);
        System.out.println("[SHARED] Dla obiektu: " + objName + ", Reset: 0");
    }
}
