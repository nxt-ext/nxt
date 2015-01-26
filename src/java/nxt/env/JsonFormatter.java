/* Indents an unformatted JSON text, sorting keys in objects.

   Copyright 2013 Carlos Rica <jasampler@gmail.com>

   This program is free software: you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation, either version 3 of the License, or
   (at your option) any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.
   
   You should have received a copy of the GNU General Public License
   along with this program.  If not, see <http://www.gnu.org/licenses/>
*/

package nxt.env;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JPanel;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JButton;
import javax.swing.JTextArea;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONValue;

/**
 * Shows a window with a text area where the user can paste an unformatted
 * JSON string and then make click in a button to get it indented.
 * Compile and run it adding to the classpath the json-simple JAR.
 */
public class JsonFormatter extends JPanel implements ActionListener {
    private JButton formatButton;
    private JTextArea jsonArea;

    private static final String TAB = "\t";

    /**
     * Generate an indented JSON string, sorting keys in objects.
     * @param str JSON string.
     */
    private static String formatJSONString(String str) {
        StringBuilder r = new StringBuilder();
        formatJSON(JSONValue.parse(str), r, "");
        return r.toString();
    }

    /**
     * Generate an indented JSON string, sorting keys in objects.
     * @param obj null, JSONObject, JSONArray or JSONValue (json-simple).
     * @param r StringBuilder where the result will be added to.
     * @param indent Accumulated tabulators string, initially should be "".
     */
    private static void formatJSON(Object obj, StringBuilder r,
                                   String indent) {
        if (obj == null) {
            r.append("null");
        }
        else if (obj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map map = (Map) obj;
            Object[] keys = map.keySet().toArray();
            Arrays.sort(keys);
            r.append("{\n");
            String indentTab = indent + TAB;
            for (int i = 0; i < keys.length; i++) {
                if (i > 0)
                    r.append(",\n");
                r.append(indentTab);
                r.append(JSONValue.toJSONString(keys[i]));
                r.append(": ");
                formatJSON(map.get(keys[i]), r, indentTab);
            }
            if (keys.length > 0)
                r.append("\n");
            r.append(indent);
            r.append("}");
        }
        else if (obj instanceof List) {
            @SuppressWarnings("unchecked")
            Iterator it = ((List) obj).iterator();
            r.append("[\n");
            if (it.hasNext()) {
                String indentTab = indent + TAB;
                r.append(indentTab);
                formatJSON(it.next(), r, indentTab);
                while (it.hasNext()) {
                    r.append(",\n");
                    r.append(indentTab);
                    formatJSON(it.next(), r, indentTab);
                }
                r.append("\n");
            }
            r.append(indent);
            r.append("]");
        }
        else {
            r.append(JSONValue.toJSONString(obj));
        }
    }

    public JsonFormatter() {
        super(new GridBagLayout());

        formatButton = new JButton("Format JSON");
        formatButton.addActionListener(this);

        jsonArea = new JTextArea(50, 60);
        jsonArea.setTabSize(4);
        jsonArea.setMargin(new Insets(5,5,5,5));
        jsonArea.setEditable(true);
        JScrollPane scrollPane = new JScrollPane(jsonArea);

        GridBagConstraints c = new GridBagConstraints();
        c.gridwidth = GridBagConstraints.REMAINDER;

        c.fill = GridBagConstraints.HORIZONTAL;
        this.add(formatButton, c);

        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1.0;
        c.weighty = 1.0;
        this.add(scrollPane, c);
    }

    public void actionPerformed(ActionEvent evt) {
        String json = formatJSONString(jsonArea.getText());
        jsonArea.setText(json);
    }

    private static void createAndShowGUI() {
        JFrame frame = new JFrame("JSON formatter");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        frame.add(new JsonFormatter());

        frame.pack();
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });
    }
}
