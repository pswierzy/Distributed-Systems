package agh.counter;

import com.zeroc.Ice.Current;
import com.zeroc.Ice.ServantLocator;
import com.zeroc.Ice.UserException;

public class MyServantLocator implements ServantLocator {

    @Override
    public LocateResult locate(Current __current) {
        System.out.println("[LOCATOR] Nowy servant dla: " + __current.id.name);

        com.zeroc.Ice.Object servant = new DedicatedCounterI();
        __current.adapter.add(servant, __current.id);

        return new LocateResult(servant, null);
    }

    @Override
    public void finished(Current __current, com.zeroc.Ice.Object object, Object o) {}

    @Override
    public void deactivate(String s) {}
}
