import grpc
import sys

import common_pb2
import common_pb2_grpc
import camera_pb2
import camera_pb2_grpc
import fridge_pb2
import fridge_pb2_grpc

def print_error(error_details):
    code_name = common_pb2.ErrorDetails.ErrorCode.Name(error_details.code)
    print(f"\nBŁĄD [{code_name}]: {error_details.message}")

def handle_camera(channel, device):
    stub = camera_pb2_grpc.CameraServiceStub(channel)
    
    while True:
        print(f"\n--- KAMERA: {device.device_id} ---")
        print("1. Pobierz status")
        print("2. Ustaw pozycję PTZ")
        print("3. Ustaw nagrywanie")
        print("0. Wróć")
        
        choice = input("> ")
        if choice == '0':
            break
        elif choice == '1':
            try:
                req = camera_pb2.GetCameraStatusRequest(device_id=device.device_id)
                res = stub.GetStatus(req)
                
                if res.WhichOneof('result') == 'error':
                    print_error(res.error)
                else:
                    print(f"\n[Status] Nagrywanie: {'Tak' if res.status.is_recording else 'Nie'}")
                    if res.status.HasField('current_position'):
                        pos = res.status.current_position
                        print(f"[Pozycja] Pan: {pos.pan}°, Tilt: {pos.tilt}°, Zoom: {pos.zoom}x")
            except grpc.RpcError as e:
                print(f"Błąd sieciowy: {e.details()}")
                
        elif choice == '2':
            if device.type != common_pb2.CAMERA_PTZ:
                print("\nOSTRZEŻENIE: To jest kamera statyczna.")
                
            try:
                p = float(input("Pan (-180.0 do 180.0): "))
                t = float(input("Tilt (-90.0 do 90.0): "))
                z = float(input("Zoom (np. 1.0): "))
                
                vector = camera_pb2.PTZVector(pan=p, tilt=t, zoom=z)
                req = camera_pb2.SetCameraPositionRequest(device_id=device.device_id, position=vector)
                
                res = stub.SetPosition(req)
                if res.WhichOneof('result') == 'error':
                    print_error(res.error)
                else:
                    print("\nSukces: Pozycja kamery została zaktualizowana.")
            except ValueError:
                print("\nBłąd: Podano nieprawidłową wartość liczbową.")
            except grpc.RpcError as e:
                print(f"Błąd sieciowy: {e.details()}")
        elif choice == '3':
            try:
                val = input("Nagrywanie ma być włączone? (y/n): ").lower()
                is_recording = True if val == 'y' else False

                req = camera_pb2.SetRecordingRequest(
                    device_id = device.device_id,
                    is_recording = is_recording
                )

                res = stub.SetRecording(req)

                if res.WhichOneof('result') == 'error':
                    print_error(res.error)
                else:
                    stan = "WŁĄCZONE" if is_recording else "WYŁĄCZONE"
                    print(f"\nSukces: Nagrywanie zostało {stan}.")

            except grpc.RpcError as e:
                print(f"Błąd sieciowy: {e.details()}")



def handle_fridge(channel, device):
    stub = fridge_pb2_grpc.FridgeServiceStub(channel)
    
    while True:
        print(f"\n--- LODÓWKA: {device.device_id} ---")
        print("1. Pobierz aktualny status")
        print("2. Ustaw nową konfigurację")
        print("0. Wróć")
        
        choice = input("> ")
        if choice == '0':
            break
        elif choice == '1':
            try:
                req = fridge_pb2.GetFridgeStatusRequest(device_id=device.device_id)
                res = stub.GetStatus(req)
                
                if res.WhichOneof('result') == 'error':
                    print(f"\nBłąd: {res.error}") 
                else:
                    s = res.status
                    print(f"\n[ODCZYT CZUJNIKÓW - {device.device_id}]")
                    print(f"-> Lodówka: {s.current_fridge_temp:.1f}°C (Cel: {s.target_fridge_temp:.1f}°C)")
                    
                    if s.HasField('current_freezer_temp'):
                        print(f"-> Zamrażarka: {s.current_freezer_temp:.1f}°C (Cel: {s.target_freezer_temp:.1f}°C)")
            except grpc.RpcError as e:
                print(f"Błąd sieciowy: {e.details()}")

        elif choice == '2':
            try:
                t_fridge = float(input("Docelowa temp. lodówki (°C): "))
                
                # Budujemy konfigurację zgodnie z nowymi nazwami pól w proto
                config = fridge_pb2.FridgeConfig(target_fridge_temp=t_fridge)
                
                if device.type == common_pb2.FRIDGE_WITH_FREEZER:
                    t_freezer = float(input("Docelowa temp. zamrażarki (°C): "))
                    config.target_freezer_temp = t_freezer
                
                req = fridge_pb2.SetFridgeConfigRequest(device_id=device.device_id, config=config)
                res = stub.SetConfig(req)
                
                if res.WhichOneof('result') == 'error':
                    print_error(res.error)
                else:
                    print("\nSukces: Nowe parametry zostały wysłane do serwera.")
            except ValueError:
                print("\nBłąd: Podaj poprawną liczbę.")
            except grpc.RpcError as e:
                print(f"Błąd sieciowy: {e.details()}")

def interact_with_server(address):
    print(f"\nNawiązywanie połączenia z {address}...")
    
    with grpc.insecure_channel(address) as channel:
        registry = common_pb2_grpc.DeviceRegistryStub(channel)
        
        try:
            req = common_pb2.ListDevicesRequest()
            res = registry.ListDevices(req)
            devices = res.devices
        except grpc.RpcError as e:
            print(f"\n Nie udało się połączyć: {e.details()}")
            return

        if not devices:
            print("\nBrak urządzeń na tym serwerze.")
            return

        while True:
            print(f"\n=== URZĄDZENIA ({address}) ===")
            for idx, device in enumerate(devices, 1):
                type_name = common_pb2.DeviceType.Name(device.type)
                print(f"[{idx}] {device.device_id} ({type_name})")
            print("[0] Wróć do wyboru serwera")
            
            choice = input("\nWybierz urządzenie: ")
            
            if choice == '0':
                break
                
            try:
                idx = int(choice) - 1
                if idx < 0 or idx >= len(devices):
                    print("Nieprawidłowy wybór.")
                    continue
                    
                selected_device = devices[idx]
                
                if selected_device.type in [common_pb2.CAMERA_STATIC, common_pb2.CAMERA_PTZ]:
                    handle_camera(channel, selected_device)
                elif selected_device.type in [common_pb2.FRIDGE_STANDARD, common_pb2.FRIDGE_WITH_FREEZER]:
                    handle_fridge(channel, selected_device)
                else:
                    print("\nNieobsługiwany typ urządzenia.")
            except ValueError:
                print("Proszę podać numer.")

def main():
    while True:
        print("===== KLIENT - OBSŁUGA SERWERA =====")
        print("1. Połącz z Piętrem 1")
        print("2. Połącz z Piętrem 2")
        print("0. Zakończ")

        choice = input("> ")
        if choice == '1':
            interact_with_server('server-floor1:50051')
        elif choice == '2':
            interact_with_server('server-floor2:50052')
        elif choice == '0':
            sys.exit(0)
        else:
            print("Nieprawidłowy wybór.")

if __name__ == '__main__':
    main()