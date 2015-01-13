package nxt.http;

import nxt.util.Convert;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
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
            "    <link href=\"css/font-awesome.min.css\" rel=\"stylesheet\" type=\"text/css\" />" +
            "    <link href=\"css/highlight.style.css\" rel=\"stylesheet\" type=\"text/css\" />" +
            "    <style type=\"text/css\">\n" +
            "        table {border-collapse: collapse;}\n" +
            "        td {padding: 10px;}\n" +
            "        .result {white-space: pre; font-family: monospace; overflow: auto;}\n" +
            "    </style>\n" +
            "</head>\n" +
            "<body>\n" +
            "<div class=\"navbar navbar-default\" role=\"navigation\">" +
            "   <div class=\"container\" style=\"min-width: 90%;\">" +
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
            "<div class=\"container\" style=\"min-width: 90%;\">" +
            "<div class=\"row\">" + 
            "  <div class=\"col-xs-12\" style=\"margin-bottom:10px;\">" +
            "    <div class=\"pull-right\">" +
            "      <div class=\"btn-group\">" +
            "        <button type=\"button\" class=\"btn btn-default btn-sm dropdown-toggle\" data-toggle=\"dropdown\">" +
            "          <i class=\"fa fa-check-circle-o\"></i> <i class=\"fa fa-circle-o\"></i>" +
            "        </button>" +
            "        <ul class=\"dropdown-menu\" role=\"menu\" style=\"font-size:12px;\">" +
            "          <li><a href=\"#\" id=\"navi-select-all-d-add-btn\">Select All Displayed (Add)</a></li>" +
            "          <li><a href=\"#\" id=\"navi-select-all-d-replace-btn\">Select All Displayed (Replace)</a></li>" +
            "          <li><a href=\"#\" id=\"navi-deselect-all-d-btn\">Deselect All Displayed</a></li>" +
            "          <li><a href=\"#\" id=\"navi-deselect-all-btn\">Deselect All</a></li>" +
            "        </ul>" +
            "      </div>" +
            "      <button type=\"button\" id=\"navi-show-fields\" data-navi-val=\"ALL\" class=\"btn btn-default btn-sm\" style=\"width:165px;\">Show Non-Empty Fields</button>" +
            "      <button type=\"button\" id=\"navi-show-tabs\" data-navi-val=\"ALL\" class=\"btn btn-default btn-sm\" style=\"width:130px;\">Show Open Tabs</button>" +
            "    </div>" +
            "  </div>" +
            "</div>" +
            "<div class=\"row\" style=\"margin-bottom:15px;\">" +
            "  <div class=\"col-xs-4 col-sm-3 col-md-2\">" +
            "    <ul class=\"nav nav-pills nav-stacked\">";
    private static final String header2 =
            "    </ul>" +
            "  </div> <!-- col -->" +
            "  <div  class=\"col-xs-8 col-sm-9 col-md-10\">" +
            "    <div class=\"panel-group\" id=\"accordion\">";

    private static final String footer1 =
            "    </div> <!-- panel-group -->" +
            "  </div> <!-- col -->" +
            "</div> <!-- row -->" +
            "</div> <!-- container -->" +
            "<script src=\"js/3rdparty/jquery.js\"></script>" +
            "<script src=\"js/3rdparty/bootstrap.js\" type=\"text/javascript\"></script>" +
            "<script src=\"js/3rdparty/highlight.pack.js\" type=\"text/javascript\"></script>" +
            "<script src=\"js/ats.js\" type=\"text/javascript\"></script>" +
            "<script src=\"js/ats.util.js\" type=\"text/javascript\"></script>" +
            "<script>" + 
            "  $(document).ready(function() {";

    private static final String footer2 =
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
        if (requestTag.equals("") 
                & !req.getParameterMap().containsKey("requestType")
                & !req.getParameterMap().containsKey("requestTypes")) {
            buf.append(" class=\"active\"");
        }
        buf.append("><a href=\"/test\">ALL</a></li>");
        buf.append("<li");
        if (req.getParameterMap().containsKey("requestTypes")) {
            buf.append(" class=\"active\"");
        }
        buf.append("><a href=\"/test?requestTypes=\" id=\"navi-selected\">SELECTED</a></li>");
        for (APITag apiTag : APITag.values()) {
            if (requestTags.get(apiTag.name()) != null) {
                buf.append("<li");
                if (requestTag.equals(apiTag.name())) {
                    buf.append(" class=\"active\"");
                }
                buf.append("><a href=\"/test?requestTag=").append(apiTag.name()).append("\">");
                buf.append(apiTag.getDisplayName()).append("</a></li>").append(" ");
            }
        }
        return buf.toString();
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate, private");
        resp.setHeader("Pragma", "no-cache");
        resp.setDateHeader("Expires", 0);
        resp.setContentType("text/html; charset=UTF-8");

        if (! API.isAllowed(req.getRemoteHost())) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        try (PrintWriter writer = resp.getWriter()) {
            writer.print(header1);
            writer.print(buildLinks(req));
            writer.print(header2);
            String requestType = Convert.nullToEmpty(req.getParameter("requestType"));
            APIServlet.APIRequestHandler requestHandler = APIServlet.apiRequestHandlers.get(requestType);
            StringBuilder bufJSCalls = new StringBuilder();
            if (requestHandler != null) {
                writer.print(form(requestType, true, requestHandler.getClass().getName(), requestHandler.getParameters(), requestHandler.requirePost()));
                bufJSCalls.append("ATS.apiCalls.push(\"").append(requestType).append("\");\n");
            } else if (!req.getParameterMap().containsKey("requestTypes")) {
                String requestTag = Convert.nullToEmpty(req.getParameter("requestTag"));
                Set<String> taggedTypes = requestTags.get(requestTag);
                for (String type : (taggedTypes != null ? taggedTypes : allRequestTypes)) {
                    requestHandler = APIServlet.apiRequestHandlers.get(type);
                    writer.print(form(type, false, requestHandler.getClass().getName(), APIServlet.apiRequestHandlers.get(type).getParameters(), 
                                      APIServlet.apiRequestHandlers.get(type).requirePost()));
                    bufJSCalls.append("ATS.apiCalls.push(\"").append(type).append("\");\n");
                }
            } else {
                String requestTypes = Convert.nullToEmpty(req.getParameter("requestTypes"));
                if (!requestTypes.equals("")) {
                    Set<String> selectedRequestTypes = new TreeSet<>(Arrays.asList(requestTypes.split("_")));
                    for (String type: selectedRequestTypes) {
                        requestHandler = APIServlet.apiRequestHandlers.get(type);
                        writer.print(form(type, false, requestHandler.getClass().getName(), APIServlet.apiRequestHandlers.get(type).getParameters(), 
                                          APIServlet.apiRequestHandlers.get(type).requirePost()));
                        bufJSCalls.append("ATS.apiCalls.push(\"").append(type).append("\");\n");
                    }
                } else {
                    writer.print(fullTextMessage("No API calls selected.", "info"));
                }
            }
            writer.print(footer1);
            writer.print(bufJSCalls.toString());
            writer.print(footer2);
        }

    }

    private static String fullTextMessage(String msg, String msgType) {
        return "<div class=\"alert alert-" + msgType + "\" role=\"alert\">" + msg + "</div>";
    }

    private static String form(String requestType, boolean singleView, String className, List<String> parameters, boolean requirePost) {
        StringBuilder buf = new StringBuilder();
        buf.append("<div class=\"panel panel-default api-call-All\" ");
        buf.append("id=\"api-call-").append(requestType).append("\">");
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
        buf.append(className.replace('.','/')).append(".html\" target=\"_blank\">javadoc</a>&nbsp;&nbsp;");
        buf.append("&nbsp;&nbsp;&nbsp;<input type=\"checkbox\" class=\"api-call-sel-ALL\" ");
        buf.append("id=\"api-call-sel-").append(requestType).append("\">");
        buf.append("</span>");
        buf.append("</h4>");
        buf.append("</div> <!-- panel-heading -->");
        buf.append("<div id=\"collapse").append(requestType).append("\" class=\"panel-collapse collapse");
        if (singleView) {
            buf.append(" in");
        }
        buf.append("\">");
        buf.append("<div class=\"panel-body\">");
        buf.append("<form action=\"/nxt\" method=\"POST\" onsubmit=\"return ATS.submitForm(this);\">");
        buf.append("<input type=\"hidden\" name=\"requestType\" value=\"").append(requestType).append("\"/>");
        buf.append("<div class=\"col-xs-12 col-lg-6\" style=\"min-width: 40%;\">");
        buf.append("<table class=\"table\">");
        for (String parameter : parameters) {
            buf.append("<tr class=\"api-call-input-tr\">");
            buf.append("<td>").append(parameter).append(":</td>");
            buf.append("<td><input type=\"");
            buf.append("secretPhrase".equals(parameter) || "adminPassword".equals(parameter) ? "password" : "text");
            buf.append("\" name=\"").append(parameter).append("\" style=\"width:100%;min-width:200px;\"/></td>");
            buf.append("</tr>");
        }
        buf.append("<tr>");
        buf.append("<td colspan=\"2\"><input type=\"submit\" class=\"btn btn-default\" value=\"submit\"/></td>");
        buf.append("</tr>");
        buf.append("</table>");
        buf.append("</div>");
        buf.append("<div class=\"col-xs-12 col-lg-6\" style=\"min-width: 50%;\">");
        buf.append("<h5 style=\"margin-top:0px;\">");
        if (!requirePost) {
            buf.append("<span style=\"float:right;\" class=\"uri-link\">");
            buf.append("</span>");
        } else {
            buf.append("<span style=\"float:right;font-size:12px;font-weight:normal;\">POST only</span>");
        }
        buf.append("Response</h5>");
        buf.append("<pre class=\"hljs json\"><code class=\"result\">JSON response</code></pre>");
        buf.append("</div>");
        buf.append("</form>");
        buf.append("</div> <!-- panel-body -->");
        buf.append("</div> <!-- panel-collapse -->");
        buf.append("</div> <!-- panel -->");
        return buf.toString();
    }

}
