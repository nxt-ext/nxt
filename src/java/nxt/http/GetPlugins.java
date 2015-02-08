package nxt.http;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public final class GetPlugins extends APIServlet.APIRequestHandler {

    static final GetPlugins instance = new GetPlugins();

    private GetPlugins() {
        super(new APITag[] {APITag.INFO});
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) {

        JSONObject response = new JSONObject();
        Path path = Paths.get("./html/ui/plugins");
        if (Files.isReadable(path)) {
            return JSONResponses.fileNotFound(path.toString());
        }
        List<Path> directories = new ArrayList<>();
        try {
            Files.walkFileTree(path, EnumSet.noneOf(FileVisitOption.class), 1, new PluginDirListing(directories));
        } catch (IOException e) {
            return JSONResponses.fileNotFound(e.getMessage());
        }
        JSONArray plugins = new JSONArray();
        for (Path dir : directories) {
            plugins.add(Paths.get(dir.toString()).getFileName());
        }
        response.put("plugins", plugins);
        return response;
    }

    public static class PluginDirListing extends SimpleFileVisitor<Path> {

        List<Path> directories;

        public PluginDirListing(List<Path> directories) {
            this.directories = directories;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attr) {
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) {
            return FileVisitResult.CONTINUE;
        }
    }

}
