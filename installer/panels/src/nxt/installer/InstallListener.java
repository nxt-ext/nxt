/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,
 * no part of the Nxt software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

package nxt.installer;

import com.izforge.izpack.api.data.InstallData;
import com.izforge.izpack.api.data.Pack;
import com.izforge.izpack.api.event.ProgressListener;
import com.izforge.izpack.api.event.ProgressNotifiers;
import com.izforge.izpack.event.AbstractProgressInstallerListener;
import static nxt.installer.ConfigHandler.VAR_CLEAN_INSTALL_DIR;
import static nxt.installer.ConfigHandler.VAR_SHUTDOWN_SERVER;

import java.util.List;

public class InstallListener extends AbstractProgressInstallerListener {

    private ConfigHandler handler = new ConfigHandler();

    public InstallListener(InstallData installData, ProgressNotifiers notifiers) {
        super(installData, notifiers);
    }

    @Override
    public void beforePacks(List<Pack> packs) {
        boolean shutdownServer = getVariable(VAR_SHUTDOWN_SERVER);
        if (shutdownServer && ! handler.shutdownServer()) {
            error("Failed to stop server");
        }

        if (getVariable(VAR_CLEAN_INSTALL_DIR)) {
            // We might have to retry removal a few times until server shutdown is complete
            boolean retry = shutdownServer;
            if (!handler.cleanNxtInstallDir(getInstallData().getInstallPath(), retry)) {
                error("Failed to remove existing installation");
            }
        }
    }

    @Override
    public void afterPacks(List<Pack> packs, ProgressListener listener) {
        ConfigHandler handler = new ConfigHandler();
        String config = getInstallData().getVariable(ConfigHandler.VAR_FILE_CONTENTS);
        if (config != null && ! config.isEmpty() &&
            ! handler.writeSettingsFile(config, getInstallData().getInstallPath()))
        {
            error("Failed to write settings file");
        }
    }

    @Override
    public boolean isFileListener() {
        return false;
    }

    private boolean getVariable(String name) {
        return "true".equals(getInstallData().getVariable(name));
    }

    private void error(String msg) {
        System.err.println("ERROR: " + msg);
    }
}
