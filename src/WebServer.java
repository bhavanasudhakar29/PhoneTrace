import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.awt.Desktop;
import java.io.IOException;
import java.io.OutputStream;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * WEB FRONT END for PhoneTrace.
 *
 * Uses the HTTP server that comes built into Java (no extra libraries).
 * The browser page asks this class for a phone's history, and it answers
 * using the SAME HistoryService and RiskAnalyzer the console app uses.
 * The backend is untouched - it simply gets a second "front door".
 *
 *   GET /api/history?imei=...&seller=...  -> full report as JSON
 *   GET /api/phones                        -> registered phones (sample buttons)
 *   /api/... (RegistryApi)                 -> add, edit, delete, transfer, reset
 *   GET /                                  -> the web page (web/index.html)
 */
public class WebServer {

    private static final int PORT = 8080;

    private static final HistoryService historyService = new HistoryService();
    private static final RiskAnalyzer analyzer = new RiskAnalyzer();
    private static final PhoneDAO phoneDAO = new PhoneDAO();

    private static Path webDir;

    public static void main(String[] args) {
        webDir = findWebFolder();
        if (webDir == null) {
            System.out.println("Could not find the 'web' folder. Run this command from inside the src folder.");
            return;
        }
        try {
            DBConnection.getConnection();
        } catch (SQLException e) {
            System.out.println("Could not connect to MySQL: " + e.getMessage());
            System.out.println("Check the username/password in DBConnection.java and that MySQL is running.");
            return;
        }

        HttpServer server;
        try {
            server = HttpServer.create(new InetSocketAddress(PORT), 0);
        } catch (BindException e) {
            System.out.println("Port " + PORT + " is already in use. Is the web server already running in another window?");
            return;
        } catch (IOException e) {
            System.out.println("Could not start the web server: " + e.getMessage());
            return;
        }

        server.createContext("/api/history", WebServer::handleHistory);
        RegistryApi registry = new RegistryApi();
        server.createContext("/api/", registry::handle);              // record management
        // "/api/phones" is only the sample-button list; "/api/phones/add" etc. go to the registry
        server.createContext("/api/phones", ex -> {
            if (ex.getRequestURI().getPath().equals("/api/phones")) handlePhones(ex);
            else registry.handle(ex);
        });
        server.createContext("/", WebServer::handleStatic);
        server.start();

        String url = "http://localhost:" + PORT;
        System.out.println("==============================================");
        System.out.println("  PhoneTrace web is running at " + url);
        System.out.println("  Keep this window open. Press Ctrl+C to stop.");
        System.out.println("==============================================");
        openBrowser(url);
    }

    // ---------------------------------------------------------------- API

    private static void handleHistory(HttpExchange ex) throws IOException {
        try {
            Map<String, String> q = parseQuery(ex.getRequestURI().getRawQuery());
            String imei = q.getOrDefault("imei", "").trim();
            String seller = q.getOrDefault("seller", "").trim();

            if (!Phone.isValidImei(imei)) {
                sendJson(ex, 400, "{\"error\":\"An IMEI is exactly 15 digits. Dial *#06# on the phone to see it.\"}");
                return;
            }
            if (!seller.isEmpty() && !seller.matches("\\d{10}")) {
                sendJson(ex, 400, "{\"error\":\"The seller's number should be 10 digits, or leave it empty.\"}");
                return;
            }

            PhoneHistory history = historyService.buildHistory(imei, seller);
            if (history == null) {
                sendJson(ex, 200, "{\"found\":false,\"imei\":\"" + imei + "\"}");
                return;
            }
            AnalysisResult result = analyzer.analyze(history);
            sendJson(ex, 200, toJson(history, result));
        } catch (Exception e) {
            sendJson(ex, 500, "{\"error\":\"Server error: " + esc(String.valueOf(e.getMessage())) + "\"}");
        }
    }

    private static void handlePhones(HttpExchange ex) throws IOException {
        StringBuilder sb = new StringBuilder("[");
        List<Phone> phones = phoneDAO.getAll();
        for (int i = 0; i < phones.size(); i++) {
            Phone p = phones.get(i);
            if (i > 0) sb.append(',');
            sb.append("{\"imei\":\"").append(p.getImei())
              .append("\",\"brand\":\"").append(esc(p.getBrand()))
              .append("\",\"model\":\"").append(esc(p.getModel())).append("\"}");
        }
        sendJson(ex, 200, sb.append(']').toString());
    }

    // Builds the JSON answer by hand (no external JSON library needed)
    private static String toJson(PhoneHistory h, AnalysisResult r) {
        StringBuilder sb = new StringBuilder("{\"found\":true");
        Phone p = h.getPhone();
        sb.append(",\"phone\":{\"imei\":\"").append(p.getImei())
          .append("\",\"brand\":\"").append(esc(p.getBrand()))
          .append("\",\"model\":\"").append(esc(p.getModel())).append("\"}");

        sb.append(",\"owners\":[");
        List<Ownership> owners = h.getOwnerships();
        for (int i = 0; i < owners.size(); i++) {
            Ownership o = owners.get(i);
            if (i > 0) sb.append(',');
            sb.append("{\"name\":\"").append(esc(o.getOwnerName()))
              .append("\",\"phone\":\"").append(mask(o.getOwnerPhone()))
              .append("\",\"from\":\"").append(o.getFromDate())
              .append("\",\"to\":").append(o.getToDate() == null ? "null" : "\"" + o.getToDate() + "\"")
              .append(",\"current\":").append(o.isCurrent()).append('}');
        }

        sb.append("],\"repairs\":[");
        List<Repair> repairs = h.getRepairs();
        for (int i = 0; i < repairs.size(); i++) {
            Repair rp = repairs.get(i);
            if (i > 0) sb.append(',');
            sb.append("{\"part\":\"").append(esc(rp.getPart()))
              .append("\",\"shop\":\"").append(esc(rp.getShopName()))
              .append("\",\"date\":\"").append(rp.getRepairDate())
              .append("\",\"genuine\":").append(rp.isGenuine()).append('}');
        }

        sb.append("],\"thefts\":[");
        List<TheftReport> thefts = h.getTheftReports();
        for (int i = 0; i < thefts.size(); i++) {
            TheftReport t = thefts.get(i);
            if (i > 0) sb.append(',');
            sb.append("{\"date\":\"").append(t.getReportDate())
              .append("\",\"status\":\"").append(t.getStatus()).append("\"}");
        }

        sb.append("],\"flags\":[");
        for (int i = 0; i < r.getFlagMessages().size(); i++) {
            if (i > 0) sb.append(',');
            sb.append("{\"message\":\"").append(esc(r.getFlagMessages().get(i)))
              .append("\",\"penalty\":").append(r.getFlagPenalties().get(i)).append('}');
        }

        sb.append("],\"score\":").append(r.getScore())
          .append(",\"verdict\":\"").append(esc(r.getVerdict()))
          .append("\",\"level\":\"").append(r.getLevel()).append("\"}");
        return sb.toString();
    }

    // ---------------------------------------------------------------- web page files

    private static void handleStatic(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        if (path.equals("/")) path = "/index.html";
        Path file = webDir.resolve(path.substring(1)).normalize();

        // Only serve files that are really inside the web folder
        if (!file.startsWith(webDir) || !Files.isRegularFile(file)) {
            send(ex, 404, "text/plain; charset=utf-8", "Not found".getBytes(StandardCharsets.UTF_8));
            return;
        }
        send(ex, 200, contentType(file), Files.readAllBytes(file));
    }

    // ---------------------------------------------------------------- helpers

    private static Path findWebFolder() {
        Path[] candidates = { Paths.get("..", "web"), Paths.get("web") };
        for (Path c : candidates) {
            Path abs = c.toAbsolutePath().normalize();
            if (Files.isRegularFile(abs.resolve("index.html"))) return abs;
        }
        return null;
    }

    static Map<String, String> parseQuery(String raw) {
        Map<String, String> map = new HashMap<>();
        if (raw == null || raw.isEmpty()) return map;
        for (String pair : raw.split("&")) {
            int i = pair.indexOf('=');
            if (i > 0) {
                map.put(URLDecoder.decode(pair.substring(0, i), StandardCharsets.UTF_8),
                        URLDecoder.decode(pair.substring(i + 1), StandardCharsets.UTF_8));
            }
        }
        return map;
    }

    static String esc(String s) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n");  break;
                case '\r': sb.append("\\r");  break;
                case '\t': sb.append("\\t");  break;
                default:
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String mask(String phone) {
        return "******" + phone.substring(phone.length() - 4);
    }

    private static String contentType(Path file) {
        String name = file.getFileName().toString().toLowerCase();
        if (name.endsWith(".html")) return "text/html; charset=utf-8";
        if (name.endsWith(".js"))   return "application/javascript; charset=utf-8";
        if (name.endsWith(".css"))  return "text/css; charset=utf-8";
        if (name.endsWith(".svg"))  return "image/svg+xml";
        return "application/octet-stream";
    }

    static void sendJson(HttpExchange ex, int status, String json) throws IOException {
        send(ex, status, "application/json; charset=utf-8", json.getBytes(StandardCharsets.UTF_8));
    }

    private static void send(HttpExchange ex, int status, String type, byte[] body) throws IOException {
        ex.getResponseHeaders().set("Content-Type", type);
        ex.getResponseHeaders().set("Cache-Control", "no-store");
        ex.sendResponseHeaders(status, body.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(body);
        }
    }

    private static void openBrowser(String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
            }
        } catch (Throwable ignored) {
            // If the browser can't be opened automatically, the user opens the URL manually
        }
    }
}
