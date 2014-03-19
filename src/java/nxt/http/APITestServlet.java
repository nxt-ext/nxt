package nxt.http;

import nxt.util.Convert;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class APITestServlet extends HttpServlet {

    private static final String header =
            "<!DOCTYPE html>\n" +
            "<html>\n" +
            "<head>\n" +
            "    <meta charset=\"UTF-8\"/>\n" +
            "    <title>NRS</title>\n" +
            "    <style type=\"text/css\">\n" +
            "        table {border-collapse: collapse;}\n" +
            "        td {padding: 10px;}\n" +
            "        .result {white-space: pre; font-family: monospace;}\n" +
            "    </style>\n" +
            "    <script type=\"text/javascript\">\n" +
            "        function submitForm(form) {\n" +
            "            var url = '/nxt';\n" +
            "            var params = '';\n" +
            "            for (i = 0; i < form.elements.length; i++) {\n" +
            "                if (! form.elements[i].name) {\n" +
            "                    continue;\n" +
            "                }\n" +
            "                if (i > 0) {\n" +
            "                    params += '&';\n" +
            "                }\n" +
            "                params += encodeURIComponent(form.elements[i].name);\n" +
            "                params += '=';\n" +
            "                params += encodeURIComponent(form.elements[i].value);\n" +
            "            }\n" +
            "            var request = new XMLHttpRequest();\n" +
            "            request.open(\"POST\", url, false);\n" +
            "            request.setRequestHeader(\"Content-type\", \"application/x-www-form-urlencoded\");\n" +
            "            request.send(params);\n" +
            "            var result = JSON.stringify(JSON.parse(request.responseText), null, 4);\n" +
            "            form.getElementsByClassName(\"result\")[0].textContent = result;\n" +
            "            return false;\n" +
            "        }\n" +
            "    </script>\n" +
            "</head>\n" +
            "<body>\n" +
            "<h3>Nxt http API</h3>\n";

    private static final String footer =
            "</body>\n" +
            "</html>\n";

    private static final List<String> requestTypes = new ArrayList<>(APIServlet.apiRequestHandlers.keySet());
    static {
        Collections.sort(requestTypes);
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate, private");
        resp.setHeader("Pragma", "no-cache");
        resp.setDateHeader("Expires", 0);
        resp.setContentType("text/html; charset=UTF-8");

        try (PrintWriter writer = resp.getWriter()) {
            writer.print(header);
            String requestType = Convert.nullToEmpty(req.getParameter("requestType"));
            APIServlet.APIRequestHandler requestHandler = APIServlet.apiRequestHandlers.get(requestType);
            if (requestHandler != null) {
                writer.print(form(requestType, requestHandler.getParameters()));
            } else {
                for (String type : requestTypes) {
                    writer.print(form(type, APIServlet.apiRequestHandlers.get(type).getParameters()));
                }
            }
            writer.print(footer);
        }

    }

    private static String form(String requestType, List<String> parameters) {
        StringBuilder buf = new StringBuilder();
        buf.append("<b>").append(requestType).append(":</b><br/>");
        buf.append("<form action=\"/nxt\" method=\"POST\" onsubmit=\"return submitForm(this);\">");
        buf.append("<input type=\"hidden\" name=\"requestType\" value=\"").append(requestType).append("\"/>");
        buf.append("<table>");
        for (String parameter : parameters) {
            buf.append("<tr>");
            buf.append("<td>").append(parameter).append(":</td>");
            buf.append("<td><input type=\"");
            buf.append("secretPhrase".equals(parameter) ? "password" : "text");
            buf.append("\" name=\"").append(parameter).append("\"/></td>");
            buf.append("</tr>");
        }
        buf.append("<tr>");
        buf.append("<td colspan=\"2\"><input type=\"submit\" value=\"submit\"/></td>");
        buf.append("</tr>");
        buf.append("</table>");
        buf.append("<div class=\"result\"></div>");
        buf.append("</form>");
        buf.append("<hr>");
        return buf.toString();
    }

}