package server;

import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.Identity;
import com.zeroc.Ice.ObjectAdapter;
import com.zeroc.Ice.Util;
import com.zeroc.Ice.InitializationException;

public class IceServer {
	public void t1(String[] args) {
		int status = 0;
		Communicator communicator = null;

		try {
			// 1. Inicjalizacja ICE - utworzenie communicatora
			communicator = Util.initialize(args);
            ObjectAdapter adapter = null;

            // 2. Konfiguracja adaptera
			try {
                // METODA 1 (polecana produkcyjnie): Konfiguracja adaptera Adapter1 jest w pliku konfiguracyjnym podanym jako parametr uruchomienia serwera (--Ice.Config=server.config)
                adapter = communicator.createObjectAdapter("Adapter1");
            }
            catch(InitializationException e) { //gdy plik konfiguracyjny nie został użyty lub nie zawiera definicji adaptera
                System.out.println("(using a hard-coded configuration)");
                // METODA 2 (niepolecana, dopuszczalna testowo): Konfiguracja adaptera Adapter1 jest w kodzie źródłowym
                //ObjectAdapter adapter = communicator.createObjectAdapterWithEndpoints("Adapter1", "tcp -h 127.0.0.2 -p 10000");
                //ObjectAdapter adapter = communicator.createObjectAdapterWithEndpoints("Adapter1", "tcp -h 127.0.0.2 -p 10000 : udp -h 127.0.0.2 -p 10000");
                adapter = communicator.createObjectAdapterWithEndpoints("Adapter2", "tcp -h 127.0.0.2 -p 10000 -z : udp -h 127.0.0.2 -p 10000 -z");
            }

			// 3. Utworzenie serwanta/serwantów
			CalcI calcServant1 = new CalcI();
			CalcI calcServant2 = new CalcI();

			// 4. Dodanie wpisów do tablicy ASM, skojarzenie nazwy obiektu (Identity) z serwantem
			adapter.add(calcServant1, new Identity("calc11", "calc"));
			adapter.add(calcServant2, new Identity("calc22", "calc"));
            adapter.add(calcServant2, new Identity("calc33", "calc"));

			// 5. Aktywacja adaptera i wejście w pętlę przetwarzania żądań
			adapter.activate();

			System.out.println("Entering event processing loop...");

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


	public static void main(String[] args) {
		IceServer app = new IceServer();
		app.t1(args);
	}
}

/*
• Jak nazywają się zmienne wskazujące na serwantów implementujących kalkulator?
calcServant1 i calcServant2
• Ile sztuk obiektów obecnie udostępnia klientom serwer? Jak się one nazywają?
2 sztuki: calc11 i calc22
• Z którym obiektem ICE skomunikował się klient?
z calc11
• Ile wpisów zawiera obecnie tablica ASM adaptera? Czy odwzorowanie między obiektem i serwantem jest 1:1?
2 wpisy, odwzorowanie 1:1
• Z którym obiektem Java (tj. serwantem) (nazwa zmiennej wskazującej na niego) komunikował się klient?
calcServant1
• W jaki sposób klient uzyskał referencję do tego konkretnego obiektu (tj. co zawiera referencja)?
przez metodę stringToProxy — tożsamość obiektu, endpointy, protokoły, adresy, porty
• Co się stanie jeśli klient zrealizuje wywołanie na nieistniejącym obecnie obiekcie (przetestuj)?
com.zeroc.Ice.ObjectNotExistException

Wireshark:
 - rozumie protokół Ice
 - Request ma: id, identity, operacje, dane
 - Reply ma: id, status, wynik
 - żeby połączyć się z innymi, to należy: zmienić adres na prawdziwy a nie localhostowy i ustawić firewalla
 */