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
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

public class APITestServlet extends HttpServlet {

    private static final String header1 =
            "<!DOCTYPE html>\n" +
            "<html>\n" +
            "<head>\n" +
            "    <meta charset=\"UTF-8\"/>\n" +
            "    <meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\">" +
            "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">" + 
            "    <title>Nxt http API</title>\n" +
            "    <link href=\"css/bootstrap.min.css\" rel=\"stylesheet\" type=\"text/css\" />" +
            "    <style type=\"text/css\">\n" +
            "        table {border-collapse: collapse;}\n" +
            "        td {padding: 10px;}\n" +
            "        .result {white-space: pre; font-family: monospace; overflow: auto;}\n" +
            "    </style>\n" +
            "    <script type=\"text/javascript\">\n" +
            "        function submitForm(form) {\n" +
            "            var url = '/nxt';\n" +
            "            var params = {};\n" +
            "            for (i = 0; i < form.elements.length; i++) {\n" +
            "                type = form.method;\n" +
            "                if (form.elements[i].type != 'button' && form.elements[i].value) {\n" +
            "                    params[form.elements[i].name] = form.elements[i].value;\n" +
            "                }\n" +
            "            }\n" +
            "            $.ajax({\n" +
            "                url: url,\n" +
            "                type: type,\n" +
            "                data: params\n" +
            "            })\n" +
            "            .done(function(result) {\n" +
            "                var resultStr = JSON.stringify(JSON.parse(result), null, 4);\n" +
            "                var uri = 'http://' + window.location.host + this.url;\n" +
            "                //form.getElementsByClassName(\"uri\")[0].textContent = uri;\n" +
            "                //form.getElementsByClassName(\"uri-link\")[0].href = uri;\n" +
            "                form.getElementsByClassName(\"result\")[0].textContent = resultStr;\n" +
            "            })\n" +
            "            .error(function() {\n" +
            "                alert('API not available, check if Nxt Server is running!');\n" +
            "            });\n" +
            "            return false;\n" +
            "        }\n" +
            "    </script>\n" +
            "</head>\n" +
            "<body>\n" +
            "<div class=\"navbar navbar-default\" role=\"navigation\">" +
            "   <div class=\"container\">" + 
            "       <div class=\"navbar-header\">" +
            "           <a class=\"navbar-brand\" href=\"/test\">Nxt http API</a>" + 
            "       </div>" +
            "       <div class=\"navbar-collapse collapse\">" +
            "           <ul class=\"nav navbar-nav navbar-right\">" +
            "               <li><input type=\"text\" class=\"form-control\" id=\"search\" " + 
            "                    placeholder=\"Search\" style=\"margin-top:8px;\"></li>\n" +
            "               <li><a href=\"https://wiki.nxtcrypto.org/wiki/Nxt_API\" target=\"_blank\" style=\"margin-left:20px;\">Wiki Docs</a></li>" +
            "           </ul>" +
            "       </div>" +
            "   </div>" + 
            "</div>" +
            "<div class=\"container\">" +
            "<div class=\"row\" style=\"margin-bottom:15px;\">" +
            "  <div class=\"col-xs-4 col-sm-3 col-md-2\">" +
            "    <ul class=\"nav nav-pills nav-stacked\">";
    private static final String header2 =
            "    </ul>" +
            "  </div> <!-- col -->" +
            "  <div  class=\"col-xs-8 col-sm-9 col-md-10\">" +
            "    <div class=\"panel-group\" id=\"accordion\">";

    private static final String footer =
            "    </div> <!-- panel-group -->" +
            "  </div> <!-- col -->" +
            "</div> <!-- row -->" +
            "</div> <!-- container -->" +
            "<script src=\"js/3rdparty/jquery.js\"></script>" +
            "<script src=\"js/3rdparty/bootstrap.js\" type=\"text/javascript\"></script>" +
            "<script>" + 
            "  $(document).ready(function() {" + 
            "    $(\".collapse-link\").click(function(event) {" +
            "       event.preventDefault();" +    
            "    });" +
            "  });" + 
            "</script>" +
            "</body>\n" +
            "</html>\n";

    private static final List<String> allRequestTypes = new ArrayList<>(APIServlet.apiRequestHandlers.keySet());
    static {
        Collections.sort(allRequestTypes);
    }

    private static final SortedMap<String, SortedSet<String>> requestTags = new TreeMap<>();
    static {
        for (Map.Entry<String, APIServlet.APIRequestHandler> entry : APIServlet.apiRequestHandlers.entrySet()) {
            String requestType = entry.getKey();
            Set<APITag> apiTags = entry.getValue().getAPITags();
            for (APITag apiTag : apiTags) {
                SortedSet<String> set = requestTags.get(apiTag.name());
                if (set == null) {
                    set = new TreeSet<>();
                    requestTags.put(apiTag.name(), set);
                }
                set.add(requestType);
            }
        }
    }

    private static String buildLinks(HttpServletRequest req) {
        StringBuilder buf = new StringBuilder();
        String requestTag = Convert.nullToEmpty(req.getParameter("requestTag"));
        buf.append("<li");
        if (requestTag.equals("")) {
            buf.append(" class=\"active\"");
        }
        buf.append("><a href=\"/test\">All</a></li>");
        for (APITag apiTag : APITag.values()) {
            buf.append("<li");
            if (requestTag.equals(apiTag.name())) {
                buf.append(" class=\"active\"");
            }
            buf.append("><a href=\"/test?requestTag=").append(apiTag.name()).append("\">");
            buf.append(apiTag.getDisplayName()).append("</a></li>").append(" ");
        }
        return buf.toString();
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate, private");
        resp.setHeader("Pragma", "no-cache");
        resp.setDateHeader("Expires", 0);
        resp.setContentType("text/html; charset=UTF-8");

        try (PrintWriter writer = resp.getWriter()) {
            writer.print(header1);
            writer.print(buildLinks(req));
            writer.print(header2);
            String requestType = Convert.nullToEmpty(req.getParameter("requestType"));
            APIServlet.APIRequestHandler requestHandler = APIServlet.apiRequestHandlers.get(requestType);
            if (requestHandler != null) {
                writer.print(form(requestType, true, requestHandler.getClass().getName(), requestHandler.getParameters()));
            } else {
                String requestTag = Convert.nullToEmpty(req.getParameter("requestTag"));
                Set<String> taggedTypes = requestTags.get(requestTag);
                for (String type : (taggedTypes != null ? taggedTypes : allRequestTypes)) {
                    requestHandler = APIServlet.apiRequestHandlers.get(type);
                    writer.print(form(type, false, requestHandler.getClass().getName(), APIServlet.apiRequestHandlers.get(type).getParameters()));
                }
            }
            writer.print(footer);
        }

    }

    private static String form(String requestType, boolean singleView, String className, List<String> parameters) {
        StringBuilder buf = new StringBuilder();
        buf.append("<div class=\"panel panel-default\">");
        buf.append("<div class=\"panel-heading\">");
        buf.append("<h4 class=\"panel-title\">");
        buf.append("<a data-toggle=\"collapse\" class=\"collapse-link\" data-target=\"#collapse").append(requestType).append("\" href=\"#\">");
        buf.append(requestType);
        buf.append("</a>");
        buf.append("<span style=\"float:right;font-weight:normal;font-size:14px;\">");
        if (!singleView) {
            buf.append("<a href=\"/test?requestType=").append(requestType);
            buf.append("\" target=\"_blank\" style=\"font-weight:normal;font-size:14px;color:#777;\"><span class=\"glyphicon glyphicon-new-window\"></span></a>");
            buf.append(" &nbsp;&nbsp;");
        }
        buf.append("<a style=\"font-weight:normal;font-size:14px;color:#777;\" href=\"/doc/");
        buf.append(className.replace('.','/')).append(".html\" target=\"_blank\">javadoc</a>");
        buf.append("</span>");
        buf.append("</h4>");
        buf.append("</div> <!-- panel-heading -->");
        buf.append("<div id=\"collapse").append(requestType).append("\" class=\"panel-collapse collapse");
        if (singleView) {
            buf.append(" in");
        }
        buf.append("\">");
        buf.append("<div class=\"panel-body\">");
        buf.append("<form action=\"/nxt\" method=\"POST\" onsubmit=\"return submitForm(this);\">");
        buf.append("<input type=\"hidden\" name=\"requestType\" value=\"").append(requestType).append("\"/>");
        buf.append("<div class=\"col-xs-12 col-lg-6\">");
        buf.append("<table class=\"table\">");
        for (String parameter : parameters) {
            buf.append("<tr>");
            buf.append("<td>").append(parameter).append(":</td>");
            buf.append("<td><input type=\"");
            buf.append("secretPhrase".equals(parameter) ? "password" : "text");
            buf.append("\" name=\"").append(parameter).append("\" style=\"width:200px;\"/></td>");
            buf.append("</tr>");
        }
        buf.append("<tr>");
        buf.append("<td colspan=\"2\"><input type=\"submit\" class=\"btn btn-default\" value=\"submit\"/></td>");
        buf.append("</tr>");
        buf.append("</table>");
        buf.append("</div>");
        buf.append("<pre class=\"result col-xs-12 col-lg-6\">JSON response</pre>");
        buf.append("</form>");
        buf.append("</div> <!-- panel-body -->");
        buf.append("</div> <!-- panel-collapse -->");
        buf.append("</div> <!-- panel -->");
        return buf.toString();
    }

}
