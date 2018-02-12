/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2018 Jelurida IP B.V.
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

import com.izforge.izpack.api.data.Panel;
import com.izforge.izpack.api.resource.Resources;
import com.izforge.izpack.gui.FlowLayout;
import com.izforge.izpack.gui.IzPanelConstraints;
import com.izforge.izpack.gui.IzPanelLayout;
import com.izforge.izpack.gui.LabelFactory;
import com.izforge.izpack.gui.log.Log;
import com.izforge.izpack.installer.data.GUIInstallData;
import com.izforge.izpack.installer.gui.InstallerFrame;
import com.izforge.izpack.installer.gui.IzPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static nxt.installer.ConfigHandler.Setting;
import static nxt.installer.ConfigHandler.VAR_CLEAN_INSTALL_DIR;
import static nxt.installer.ConfigHandler.VAR_FILE_CONTENTS;
import static nxt.installer.ConfigHandler.VAR_SHUTDOWN_SERVER;

public class ConfigPanel extends IzPanel implements ItemListener {
    private final ConfigHandler handler = new ConfigHandler();
    private List<JCheckBox> settingsChecks;
    private JCheckBox stopServerCheck;
    private JCheckBox uninstallCheck;

    public ConfigPanel(Panel panel, InstallerFrame parent, GUIInstallData installData, Resources resources, Log log) {
        this(panel, parent, installData, new IzPanelLayout(log), resources);
    }

    public ConfigPanel(Panel panel, InstallerFrame parent, GUIInstallData installData, LayoutManager2 layout, Resources resources) {
        super(panel, parent, installData, layout, resources);
    }

    @Override
    public void panelActivate() {
        super.panelActivate();
        if (settingsChecks == null) {
            stopServerCheck = new JCheckBox("Stop Server");
            uninstallCheck = new JCheckBox("Remove Installation");
            if (handler.isServerRunning()) {
                add(LabelFactory.create("A running server was detected. Do you want to stop it?"), NEXT_LINE);
                add(stopServerCheck, NEXT_LINE);
            }
            if (handler.isNxtInstallDir(installData.getInstallPath())) {
                add(IzPanelLayout.createParagraphGap());
                add(LabelFactory.create("An existing installation was found. Do you want to remove it?"), NEXT_LINE);
                add(uninstallCheck, NEXT_LINE);
            }

            settingsChecks = new LinkedList<>();
            List<Setting> settings = handler.readSettings();
            add(IzPanelLayout.createVerticalStrut(20));
            add(LabelFactory.create("Below you can define custom settings for this installation.", LEADING), NEXT_LINE);
            add(LabelFactory.create("They will be stored in file " + ConfigHandler.FILE_PATH +
                                    " which you can edit later.", LEADING), NEXT_LINE);
            for (Setting s: settings) {
                String toolTipText = "<html>" + s.description.replaceAll("\n", "<br>") + "</html>";
                JCheckBox check = new JCheckBox(s.getName());
                check.setToolTipText(toolTipText);
                check.putClientProperty("setting", s);
                check.addItemListener(this);
                settingsChecks.add(check);

                JPanel c = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
                c.setToolTipText(toolTipText);
                c.add(check);
                c.add(LabelFactory.create(parent.getIcons().get("help")));
                add(c, new IzPanelConstraints(DEFAULT_CONTROL_ALIGNMENT, DEFAULT_CONTROL_ALIGNMENT, 0, NEXT_ROW,
                        Byte.MAX_VALUE, Byte.MAX_VALUE, AUTOMATIC_GAP, AUTOMATIC_GAP, FULL_LINE_STRETCH, 0.0));
            }
            getLayoutHelper().completeLayout();
        }
    }

    @Override
    public boolean isValidated() {
        List<Setting> settings = new LinkedList<>();
        for (JCheckBox check: settingsChecks) {
            if (check.isSelected()) {
                settings.add((Setting) check.getClientProperty("setting"));
            }
        }
        installData.setVariable(VAR_FILE_CONTENTS, handler.writeSettings(settings));

        setVariable(VAR_SHUTDOWN_SERVER, stopServerCheck);
        setVariable(VAR_CLEAN_INSTALL_DIR, uninstallCheck);
        return true;
    }

    private void setVariable(String name, JCheckBox check) {
        installData.setVariable(name, check.isSelected() ? "true" : null);
    }

    @Override
    public void itemStateChanged(ItemEvent ev) {
        Map<String, String> properties = new HashMap<>();
        // Compile the complete set of properties currently enabled
        for (JCheckBox check: settingsChecks) {
            if (check.isSelected()) {
                Setting setting = (Setting) check.getClientProperty("setting");
                properties.putAll(setting.properties);
            }
        }
        // Disable those settings that conflict with enabled ones
        for (JCheckBox check: settingsChecks) {
            if (! check.isSelected()) {
                Setting setting = (Setting) check.getClientProperty("setting");
                boolean enable = true;
                for (Map.Entry<String, String> e : setting.properties.entrySet()) {
                    if (properties.containsKey(e.getKey()) &&
                        ! properties.get(e.getKey()).equals(e.getValue()))
                    {
                        enable = false;
                        break;
                    }
                }
                check.setEnabled(enable);
            }
        }
    }
}

