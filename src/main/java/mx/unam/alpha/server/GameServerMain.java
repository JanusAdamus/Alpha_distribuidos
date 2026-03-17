package mx.unam.alpha.server;

import mx.unam.alpha.common.config.AppConfig;

import java.nio.file.Path;

public class GameServerMain {

    public static void main(String[] args) throws Exception {
        AppConfig config = AppConfig.load();
        AlphaServerRuntime runtime = new AlphaServerRuntime(config);
        Path rootDirectory = findProjectRoot();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            runtime.close();
        }));

        runtime.start();
        System.out.println("Broker JMS: " + config.getJmsBrokerUrl());
        System.out.println("Tópicos: " + config.getMonsterTopic() + ", "
                + config.getEventsTopic() + ", " + config.getWinnerTopic());
        new ServerLauncherController(config, runtime, rootDirectory).show();
    }

    private static Path findProjectRoot() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            if (current.resolve("pom.xml").toFile().exists()) {
                return current;
            }
            current = current.getParent();
        }
        return Path.of("").toAbsolutePath();
    }
}
