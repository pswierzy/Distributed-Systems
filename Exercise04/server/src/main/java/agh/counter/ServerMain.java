package agh.counter;

import agh.Demo.Counter;
import com.zeroc.Ice.*;

import java.lang.Exception;

public class ServerMain {
    public static void main(String[] args) {
        int status = 0;
        Communicator communicator = null;

        try {
            communicator = Util.initialize(args);
            ObjectAdapter adapter = null;
            try {
                adapter = communicator.createObjectAdapter("Adapter1");
            }
            catch(InitializationException e) {
                System.out.println("(using a hard-coded configuration)");
                adapter = communicator.createObjectAdapterWithEndpoints("Adapter2", "tcp -h 127.0.0.2 -p 10000 -z : udp -h 127.0.0.2 -p 10000 -z");
            }

            Counter sharedServant = new SharedCounterI();
            adapter.addDefaultServant(sharedServant, "shared");
            System.out.println("[SERVER] Default servant zarejestrowany");

            ServantLocator locator = new MyServantLocator();
            adapter.addServantLocator(locator, "dedicated");
            System.out.println("[SERVER] Servant locator zarejestrowany");

            adapter.activate();
            System.out.println("[SERVER] Processing loop...");
            communicator.waitForShutdown();

        } catch (Exception e) {
            e.printStackTrace(System.err);
            status = 1;
        }
        if (communicator != null) {
            try {
                communicator.destroy();
            } catch (Exception e) {
                e.printStackTrace(System.err);
                status = 1;
            }
        }
        System.exit(status);
    }
}
