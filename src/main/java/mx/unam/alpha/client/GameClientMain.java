package mx.unam.alpha.client;

import mx.unam.alpha.common.config.AppConfig;

public class GameClientMain {

    public static void main(String[] args) {
        AppConfig config = AppConfig.load();
        GameClientController controller = new GameClientController(config);
        controller.show();
    }
}
