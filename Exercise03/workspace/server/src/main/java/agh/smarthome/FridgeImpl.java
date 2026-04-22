package agh.smarthome;

import io.grpc.stub.StreamObserver;
import agh.smarthome.FridgeServiceGrpc.FridgeServiceImplBase;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class FridgeImpl extends FridgeServiceImplBase {
    private static class InternalFridgeState {
        float currFridgeTemp; float targetFridgeTemp;
        Float currFreezeTemp; Float targetFreezeTemp;

        InternalFridgeState(float cF, float tF, Float cFr,  Float tFr) {
            this.currFridgeTemp = cF; this.targetFridgeTemp = tF;
            this.currFreezeTemp = cFr; this.targetFreezeTemp = tFr;
        }
    }
    private final Map<String, InternalFridgeState> fridgeStates = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public FridgeImpl() {
        scheduler.scheduleAtFixedRate(this::updateTemp, 0, 1, TimeUnit.SECONDS);
    }

    private float newTemp(float curr, float target) {
        if (Math.abs(curr - target) < 0.05f) return target;
        return curr < target ? curr + 0.1f : curr - 0.1f;
    }

    private void updateTemp() {
        for (InternalFridgeState state : fridgeStates.values()) {
            state.currFridgeTemp = newTemp(state.currFridgeTemp, state.targetFridgeTemp);
            if(state.targetFreezeTemp != null) {
                state.currFreezeTemp = newTemp(state.currFreezeTemp, state.targetFreezeTemp);
            }
        }
    }

    public void registerFridge(String deviceId, float targetFridgeTemp, Float targetFreezeTemp) {
        fridgeStates.put(deviceId, new InternalFridgeState(20f, targetFridgeTemp, 20f, targetFreezeTemp));
    }

    @Override
    public void setConfig(SetFridgeConfigRequest request, StreamObserver<SetFridgeConfigResponse> responseObserver) {
        String deviceId = request.getDeviceId();

        if (!fridgeStates.containsKey(deviceId)) {
            SetFridgeConfigResponse response = SetFridgeConfigResponse.newBuilder()
                    .setError(ErrorDetails.newBuilder()
                            .setCode(ErrorDetails.ErrorCode.DEVICE_NOT_FOUND)
                            .setMessage("Lodówka o ID " + deviceId + " nie istnieje.")
                            .build())
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            return;
        }

        InternalFridgeState state = fridgeStates.get(deviceId);

        state.targetFridgeTemp = request.getConfig().getTargetFridgeTemp();
        if (request.getConfig().hasTargetFreezerTemp()) {
            state.targetFreezeTemp = request.getConfig().getTargetFreezerTemp();
        }

        SetFridgeConfigResponse response = SetFridgeConfigResponse.newBuilder()
                .setSuccess(true)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getStatus(GetFridgeStatusRequest request, StreamObserver<GetFridgeStatusResponse> responseObserver) {
        String deviceId = request.getDeviceId();

        if (!fridgeStates.containsKey(deviceId)) {
            GetFridgeStatusResponse response = GetFridgeStatusResponse.newBuilder()
                    .setError(ErrorDetails.newBuilder()
                            .setCode(ErrorDetails.ErrorCode.DEVICE_NOT_FOUND)
                            .setMessage("Lodówka o ID " + deviceId + " nie istnieje.")
                            .build())
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            return;
        }

        InternalFridgeState state = fridgeStates.get(deviceId);

        FridgeStatus.Builder status = FridgeStatus.newBuilder()
                .setCurrentFridgeTemp(state.currFridgeTemp)
                .setTargetFridgeTemp(state.targetFridgeTemp);
        if (state.targetFreezeTemp != null) {
            status
                .setTargetFreezerTemp(state.targetFreezeTemp)
                .setCurrentFreezerTemp(state.currFreezeTemp);
        }

        GetFridgeStatusResponse response = GetFridgeStatusResponse.newBuilder()
                .setStatus(status.build()).build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}