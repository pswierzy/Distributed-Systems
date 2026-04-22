package agh.smarthome;

import io.grpc.stub.StreamObserver;
import agh.smarthome.CameraServiceGrpc.CameraServiceImplBase;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CameraImpl extends CameraServiceImplBase {
    private final Map<String, CameraStatus.Builder> cameraStates = new ConcurrentHashMap<>();
    private final Map<String, DeviceType> cameraTypes = new ConcurrentHashMap<>();

    public void registerCamera(String deviceId, DeviceType type) {
        cameraStates.put(deviceId, CameraStatus.newBuilder()
                .setIsRecording(true)
                .setCurrentPosition(PTZVector.newBuilder()
                                .setPan(0f)
                                .setTilt(0f)
                                .setZoom(1f)
                                .build()));
        cameraTypes.put(deviceId, type);
    }

    @Override
    public void setPosition(SetCameraPositionRequest request, StreamObserver<CameraResponse> responseObserver) {
        String deviceId = request.getDeviceId();

        if (!cameraStates.containsKey(deviceId)) {
            CameraResponse response = CameraResponse.newBuilder()
                    .setError(ErrorDetails.newBuilder()
                            .setCode(ErrorDetails.ErrorCode.DEVICE_NOT_FOUND)
                            .setMessage("Kamera o ID " + deviceId + " nie istnieje na tym serwerze.")
                            .build())
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            return;
        }

        if (cameraTypes.get(deviceId) != DeviceType.CAMERA_PTZ) {
            CameraResponse response = CameraResponse.newBuilder()
                    .setError(ErrorDetails.newBuilder()
                            .setCode(ErrorDetails.ErrorCode.HARDWARE_ERROR)
                            .setMessage("Odmowa: Urządzenie " + deviceId + " to kamera statyczna i nie obsługuje obrotu (PTZ).")
                            .build())
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            return;
        }
        cameraStates.get(deviceId).setCurrentPosition(request.getPosition());

        CameraResponse response = CameraResponse.newBuilder()
                .setSuccess(true)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void setRecording(SetRecordingRequest request, StreamObserver<CameraResponse> responseObserver) {
        String deviceId = request.getDeviceId();

        if (!cameraStates.containsKey(deviceId)) {
            CameraResponse response = CameraResponse.newBuilder()
                    .setError(ErrorDetails.newBuilder()
                            .setCode(ErrorDetails.ErrorCode.DEVICE_NOT_FOUND)
                            .setMessage("Kamera o ID " + deviceId + " nie istnieje na tym serwerze.")
                            .build())
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            return;
        }

        cameraStates.get(deviceId).setIsRecording(request.getIsRecording());

        CameraResponse response = CameraResponse.newBuilder()
                .setSuccess(true)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getStatus(GetCameraStatusRequest request, StreamObserver<GetCameraStatusResponse> responseObserver) {
        String deviceId = request.getDeviceId();

        if (!cameraStates.containsKey(deviceId)) {
            GetCameraStatusResponse response = GetCameraStatusResponse.newBuilder()
                    .setError(ErrorDetails.newBuilder()
                            .setCode(ErrorDetails.ErrorCode.DEVICE_NOT_FOUND)
                            .setMessage("Kamera o ID " + deviceId + " nie istnieje.")
                            .build())
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            return;
        }

        GetCameraStatusResponse response = GetCameraStatusResponse.newBuilder()
                .setStatus(cameraStates.get(deviceId).build())
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}