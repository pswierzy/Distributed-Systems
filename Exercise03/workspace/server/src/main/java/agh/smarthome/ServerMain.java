package agh.smarthome;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import io.grpc.Server;
import io.grpc.ServerBuilder;


public class ServerMain
{
    private static final Logger logger = Logger.getLogger(ServerMain.class.getName());

    private String address = "127.0.0.5";
    private int port = 50050;
    private Server server;

    private SocketAddress socket;

    private void start(int floor_no) throws IOException
    {
        port += floor_no;
        try { socket = new InetSocketAddress(InetAddress.getByName(address), port);	} catch(UnknownHostException e) {};

        CameraImpl camera = new CameraImpl();
        FridgeImpl fridge = new FridgeImpl();
        DeviceRegistryImpl registryService = null;

        if (floor_no == 1) {

            fridge.registerFridge("fridge-main-fl1", 3f, -10f);
            fridge.registerFridge("small-fridge-fl1", 3f, null);

            camera.registerCamera("cam-1-1", DeviceType.CAMERA_STATIC);
            camera.registerCamera("cam-1-2-ptz", DeviceType.CAMERA_PTZ);
            camera.registerCamera("cam-1-3", DeviceType.CAMERA_STATIC);

            registryService = new DeviceRegistryImpl(Arrays.asList(
                    DeviceDescriptor.newBuilder().setDeviceId("fridge-main-fl1").setType(DeviceType.FRIDGE_WITH_FREEZER).build(),
                    DeviceDescriptor.newBuilder().setDeviceId("small-fridge-fl1").setType(DeviceType.FRIDGE_STANDARD).build(),
                    DeviceDescriptor.newBuilder().setDeviceId("cam-1-1").setType(DeviceType.CAMERA_STATIC).build(),
                    DeviceDescriptor.newBuilder().setDeviceId("cam-1-2-ptz").setType(DeviceType.CAMERA_PTZ).build(),
                    DeviceDescriptor.newBuilder().setDeviceId("cam-1-3").setType(DeviceType.CAMERA_STATIC).build()
            ));
        } else if (floor_no == 2) {
            fridge.registerFridge("fridge-main-fl2", 4f, -15f);
            fridge.registerFridge("small-fridge-fl2", 2f, null);

            camera.registerCamera("cam-2-1", DeviceType.CAMERA_STATIC);
            camera.registerCamera("cam-2-2-ptz", DeviceType.CAMERA_PTZ);
            camera.registerCamera("cam-2-3", DeviceType.CAMERA_STATIC);

            registryService = new DeviceRegistryImpl(Arrays.asList(
                    DeviceDescriptor.newBuilder().setDeviceId("fridge-main-fl2").setType(DeviceType.FRIDGE_WITH_FREEZER).build(),
                    DeviceDescriptor.newBuilder().setDeviceId("small-fridge-fl2").setType(DeviceType.FRIDGE_STANDARD).build(),
                    DeviceDescriptor.newBuilder().setDeviceId("cam-2-1").setType(DeviceType.CAMERA_STATIC).build(),
                    DeviceDescriptor.newBuilder().setDeviceId("cam-2-2-ptz").setType(DeviceType.CAMERA_PTZ).build(),
                    DeviceDescriptor.newBuilder().setDeviceId("cam-2-3").setType(DeviceType.CAMERA_STATIC).build()
            ));
        }

        server = ServerBuilder.forPort(port)
                .executor(Executors.newFixedThreadPool(16))
                .addService(registryService)
                .addService(camera)
                .addService(fridge)
                .build().start();

        logger.info("Server started, listening on " + port + " | Floor: " + floor_no);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                // Use stderr here since the logger may have been reset by its JVM shutdown hook.
                System.err.println("Shutting down gRPC server...");
                ServerMain.this.stop();
                System.err.println("Server shut down.");
            }
        });
    }

    private void stop() {
        if (server != null) {
            server.shutdown();
        }
    }

    /**
     * Await termination on the main thread since the grpc library uses daemon threads.
     */
    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    /**
     * Main launches the server from the command line.
     */
    public static void main(String[] args) throws IOException, InterruptedException {
        int floor_no = Integer.parseInt(args[0]);

        final ServerMain server = new ServerMain();
        server.start(floor_no);
        server.blockUntilShutdown();
    }

}
