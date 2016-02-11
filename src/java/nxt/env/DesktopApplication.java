package nxt.env;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.concurrent.Worker;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import netscape.javascript.JSObject;
import nxt.Nxt;
import nxt.http.API;
import nxt.util.Logger;

public class DesktopApplication extends Application {

    static volatile boolean isLaunched;
    static volatile Stage stage;

    public static void launch() {
        if (!isLaunched) {
            isLaunched = true;
            Application.launch(DesktopApplication.class);
            return;
        }
        if (stage != null && !stage.isShowing()) {
            Platform.runLater(() -> stage.show());
        }
    }

    @Override
    public void start(Stage stage) throws Exception {
        DesktopApplication.stage = stage;
        Rectangle2D primaryScreenBounds = Screen.getPrimary().getVisualBounds();
        WebView browser = new WebView();

        int height = (int) Math.min(primaryScreenBounds.getMaxY() - 100, 1000);
        int width = (int) Math.min(primaryScreenBounds.getMaxX() - 100, 1618);
        browser.setMinHeight(height);
        browser.setMinWidth(width);
        WebEngine webEngine = browser.getEngine();
        webEngine.setUserDataDirectory(Nxt.getConfDir());
        Worker<Void> loadWorker = webEngine.getLoadWorker();
        ReadOnlyObjectProperty<Worker.State> stateProperty = loadWorker.stateProperty();
        stateProperty.addListener(
                (ov, oldState, newState) -> {
                    JSObject window = (JSObject)webEngine.executeScript("window");
                    window.setMember("java", this);
                    webEngine.executeScript("console.log = function(msg) { java.log(msg); };");
                    stage.setTitle("NXT Wallet " + webEngine.getLocation());
                });
        webEngine.load(API.getWelcomePageUri().toString());
        Scene scene = new Scene(browser);
        String address = API.getServerRootUri().toString();
        stage.getIcons().add(new Image(address + "/img/nxt-icon-32x32.png"));
        stage.initStyle(StageStyle.DECORATED);
        stage.setScene(scene);
        stage.sizeToScene();
        stage.show();
        Platform.setImplicitExit(false); // So that we can reopen the application in case the user closed it
    }

    public void stop() {
        Logger.logInfoMessage("Application stopped");
    }

    public void log(String message) {
        Logger.logInfoMessage(message);
    }

}
