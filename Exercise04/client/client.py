import sys
import os
import Ice
import agh.Demo

def main():
    proxies = {}
    with Ice.initialize(sys.argv) as communicator:
        base = "tcp -h server -p 10010"
        
        while True:
            print("Dostępne obiekty do wyboru:")
            if not proxies:
                print("Brak dostępnych obiektów")
            else:
                for k in proxies.keys():
                    print(f"\t-{k}: {proxies[k]}")
            ident = input("\nPodaj nazwę obiektu do wywołania lub 'exit' aby zakończyć: ").strip()

            if ident.lower() == "exit":
                print("Zakończenie programu.")
                break

            if not ident:
                continue

            if ident not in proxies:
                full_proxy = f"{ident}:{base}"

                proxies[ident] = agh.Demo.CounterPrx.uncheckedCast(communicator.stringToProxy(full_proxy))
                print(f"\nUtworzono lokalne proxy dla '{ident}'.")

            proxy = proxies[ident]
            while True:
                print(f"\nCo zrobić z obiektem {ident}?")
                print("\t1 => increment()")
                print("\t2 => getValue()")
                print("\t3 => reset()")
                print("\t0 => wróć do wyboru obiektu")
                
                action = input().strip()

                if action == '0':
                    print("Powrót do wyboru obiektu.")
                    break

                try:
                    if action == '1':
                        print("\nWysyłam increment() do serwera")
                        proxy.increment()
                        print("Wykonano increment()")
                    elif action == '2':
                        print("\nWysyłam getValue() do serwera")
                        val = proxy.getValue()
                        print(f"vaule = {val}")
                    elif action == '3':
                        print("\nWysyłam reset() do serwera")
                        proxy.reset()
                        print("Wykonano reset()")
                    else:
                        print("Nieznana akcja")

                except Ice.ObjectNotExistException:
                    print("\nBłąd: ObjectNotExistException!")
                    break
                except Exception as e:
                    print(f"\nBłąd: {e}")
                    break


if __name__ == "__main__":
    main()