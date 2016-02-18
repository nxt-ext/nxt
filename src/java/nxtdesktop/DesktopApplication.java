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
import nxt.TaggedData;
import nxt.http.API;
import nxt.util.Convert;
import nxt.util.Logger;
import nxt.util.TrustAllSSLProvider;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class DesktopApplication extends Application {

    static volatile boolean isLaunched;
    static volatile Stage stage;
    JSObject nrs;

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
        loadWorker.stateProperty().addListener(
                (ov, oldState, newState) -> {
                    JSObject window = (JSObject)webEngine.executeScript("window");
                    window.setMember("java", this);
                    webEngine.executeScript("console.log = function(msg) { java.log(msg); };");
                    stage.setTitle("NXT Desktop - " + webEngine.getLocation());
                    if (newState == Worker.State.SUCCEEDED) {
                        nrs = (JSObject) webEngine.executeScript("NRS");
                    }
                });

        webEngine.locationProperty().addListener((observable, oldValue, newValue) -> {
            webViewURLChange(newValue);
        });

        String url = API.getWelcomePageUri().toString();
        if (url.startsWith("https")) {
            HttpsURLConnection.setDefaultSSLSocketFactory(TrustAllSSLProvider.getSslSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier(TrustAllSSLProvider.getHostNameVerifier());
        }
        String defaultAccount = Nxt.getStringProperty("nxt.defaultDesktopAccount");
        if (defaultAccount != null && !defaultAccount.equals("")) {
            url += "?account=" + defaultAccount;
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

    private void webViewURLChange(String newValue) {
        Logger.logInfoMessage("webview address changed to " + newValue);
        URL url;
        try {
            url = new URL(newValue);
        } catch (MalformedURLException e) {
            Logger.logInfoMessage("Malformed URL " + newValue, e);
            return;
        }
        String query = url.getQuery();
        if (query == null) {
            return;
        }
        String[] paramPairs = query.split("&");
        Map<String, String> params = new HashMap<>();
        for (String paramPair : paramPairs) {
            String[] keyValuePair = paramPair.split("=");
            if (keyValuePair.length == 2) {
                params.put(keyValuePair[0], keyValuePair[1]);
            }
        }
        if (!"downloadTaggedData".equals(params.get("requestType"))) {
            Logger.logInfoMessage(String.format("requestType %s is not a download request", params.get("requestType")));
            return;
        }
        long transactionId = Convert.parseUnsignedLong(params.get("transaction"));
        TaggedData taggedData = TaggedData.getData(transactionId);
        boolean retrieve = "true".equals(params.get("retrieve"));
        if (taggedData == null && retrieve) {
            if (Nxt.getBlockchainProcessor().restorePrunedTransaction(transactionId) == null) {
                growl("Pruned transaction data not currently available from any peer");
                return;
            }
            taggedData = TaggedData.getData(transactionId);
        }
        if (taggedData == null) {
            growl("Tagged data not found");
            return;
        }
        byte[] data = taggedData.getData();
        String filename = taggedData.getFilename();
        if (filename == null || filename.trim().isEmpty()) {
            filename = taggedData.getName().trim();
        }
        Path folderPath = Paths.get(System.getProperty("user.home"), "downloads");
        Path path = Paths.get(folderPath.toString(), filename);
        Logger.logInfoMessage("Downloading data to " + path.toAbsolutePath());
        try {
            OutputStream outputStream = Files.newOutputStream(path);
            outputStream.write(data);
            outputStream.close();
            growl(String.format("File %s saved to folder %s", filename, folderPath));
        } catch (IOException e) {
            growl("Download failed " + e.getMessage(), e);
        }
    }

    public void stop() {
        System.out.println("DesktopApplication stopped"); // Should never happen
    }

    public void log(String message) {
        Logger.logInfoMessage(message);
    }

    public void growl(String msg) {
        growl(msg, null);
    }

    public void growl(String msg, Exception e) {
        if (e == null) {
            Logger.logInfoMessage(msg);
        } else {
            Logger.logInfoMessage(msg, e);
        }
        nrs.call("growl", msg);
    }

}
