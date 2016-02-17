/******************************************************************************
 * Copyright © 2013-2016 The Nxt Core Developers.                             *
 *                                                                            *
 * See the AUTHORS.txt, DEVELOPER-AGREEMENT.txt and LICENSE.txt files at      *
 * the top-level directory of this distribution for the individual copyright  *
 * holder information and the developer policies on copyright and licensing.  *
 *                                                                            *
 * Unless otherwise agreed in a custom licensing agreement, no part of the    *
 * Nxt software, including this file, may be copied, modified, propagated,    *
 * or distributed except according to the terms contained in the LICENSE.txt  *
 * file.                                                                      *
 *                                                                            *
 * Removal or modification of this copyright notice is prohibited.            *
 *                                                                            *
 ******************************************************************************/

package nxtdesktop;

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
import nxt.util.TrustAllSSLProvider;

import javax.net.ssl.HttpsURLConnection;

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

    public static void shutdown() {
        System.out.println("shutting down JavaFX platform");
        Platform.exit();
        System.out.println("JavaFX platform shutdown complete");
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
        String url = API.getWelcomePageUri().toString();
        if (url.startsWith("https")) {
            HttpsURLConnection.setDefaultSSLSocketFactory(TrustAllSSLProvider.getSslSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier(TrustAllSSLProvider.getHostNameVerifier());
        }
        webEngine.load(url);
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
        System.out.println("DesktopApplication stopped"); // Should never happen
    }

    public void log(String message) {
        Logger.logInfoMessage(message);
    }

}
