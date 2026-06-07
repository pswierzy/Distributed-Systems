package org.example;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.ZooKeeper;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;

public class ZkApp implements Watcher {

    private static final String ZNODE_A = "/a";
    private final ZooKeeper zk;
    private final String execCommand;
    private Process process;

    public ZkApp(String connectString, String execCommand) throws IOException {
        this.execCommand = execCommand;
        this.zk = new ZooKeeper(connectString, 3000, this);
    }

    @Override
    public void process(WatchedEvent event) {
        try {
            if (event.getState() == KeeperState.SyncConnected && event.getType() == EventType.None) {
                checkInitialState();
            }

            else if (event.getType() == EventType.NodeCreated && event.getPath().equals(ZNODE_A)) {
                startApp();
                zk.exists(ZNODE_A, this);
                zk.getChildren(ZNODE_A, this);
            }

            else if (event.getType() == EventType.NodeDeleted && event.getPath().equals(ZNODE_A)) {
                stopApp();
                zk.exists(ZNODE_A, this);
            }

            else if (event.getType() == EventType.NodeChildrenChanged && event.getPath().equals(ZNODE_A)) {
                try {
                    showChildrenCount();
                    zk.getChildren(ZNODE_A, this);
                }
                catch (KeeperException.NoNodeException _) {}
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void checkInitialState() throws KeeperException, InterruptedException {
        if (zk.exists(ZNODE_A, this) != null) {
            startApp();
            zk.getChildren(ZNODE_A, this);
        } else {
            stopApp();
        }
    }

    private void startApp() {
        if (process == null || !process.isAlive()) {
            System.out.println("Utworzono węzeł '/a'. Uruchamiam: " + execCommand);
            try {
                process = Runtime.getRuntime().exec(execCommand);
            } catch (IOException e) {
                System.err.println("Błąd podczas uruchamiania: " + e.getMessage());
            }
        }
    }

    private void stopApp() {
        if (process != null && process.isAlive()) {
            System.out.println("Usunięto węzeł '/a'. Zatrzymuję aplikację.");
            process.destroy();
            process = null;
        }
    }

    private void showChildrenCount() throws KeeperException, InterruptedException {
        List<String> children = zk.getChildren(ZNODE_A, false);
        System.out.println("Aktualna liczba potomków: " + children.size());
    }

    public void printTree() {
        try {
            if (zk.exists(ZNODE_A, false) != null) {
                System.out.println("Struktura drzewa dla " + ZNODE_A + ":");
                printNode(ZNODE_A, 0);
            } else {
                System.out.println("Węzeł " + ZNODE_A + " nie istnieje.");
            }
        } catch (KeeperException | InterruptedException e) {
            System.err.println("Błąd podczas pobierania drzewa: " + e.getMessage());
        }
    }

    private void printNode(String path, int depth) throws KeeperException, InterruptedException {
        StringBuilder indent = new StringBuilder();
        for (int i = 0; i < depth; i++) {
            indent.append("  ");
        }

        System.out.println(indent + "└── " + path.substring(path.lastIndexOf('/') + 1));

        List<String> children = zk.getChildren(path, false);
        for (String child : children) {
            String childPath = path.equals("/") ? path + child : path + "/" + child;
            printNode(childPath, depth + 1);
        }
    }

    public void close() throws InterruptedException {
        if (zk != null) {
            zk.close();
        }
        stopApp();
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Użycie: java ZkApp <adresy_serwerów_zookeeper> <komenda_uruchamiająca_aplikację>");
            return;
        }

        String connectString = args[0];
        String command = args[1];

        ZkApp app = new ZkApp(connectString, command);

        Scanner scanner = new Scanner(System.in);
        System.out.println("Aplikacja uruchomiona.");
        System.out.println("tree - pokaż drzewo '/a'");
        System.out.println("quit - wyjdź");

        while (true) {
            String input = scanner.nextLine();
            if ("quit".equalsIgnoreCase(input)) {
                break;
            } else if ("tree".equalsIgnoreCase(input)) {
                app.printTree();
            }
        }

        app.close();
        scanner.close();
        System.exit(0);
    }
}