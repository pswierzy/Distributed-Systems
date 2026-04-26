package agh.smarthome;

import java.util.List;
import io.grpc.stub.StreamObserver;
import agh.smarthome.DeviceRegistryGrpc.DeviceRegistryImplBase;


public class DeviceRegistryImpl extends DeviceRegistryImplBase {
    private final List<DeviceDescriptor> availableDevices;

    public DeviceRegistryImpl(List<DeviceDescriptor> availableDevices) {
        this.availableDevices = availableDevices;
    }

    @Override
    public void listDevices(ListDevicesRequest request, StreamObserver<ListDevicesResponse> responseObserver) {
        ListDevicesResponse response = ListDevicesResponse.newBuilder()
                .addAllDevices(availableDevices)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
