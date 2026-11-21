package com.travelvault.ui;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.travelvault.CulinaryMemory;
import com.travelvault.LandmarkMemory;
import com.travelvault.Memory;
import com.travelvault.service.TravelVaultService;

import java.awt.Desktop;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WebMapServer {
    private static final int PORT = 8080;
    private static final Path DATA_FILE = Paths.get("data", "travel_memories.csv");
    private static final Path PINS_FILE = Paths.get("data", "map_pins.txt");
    private static final Path PHOTOS_DIR = Paths.get("data", "photos");

    public static void main(String[] args) throws IOException {
        Files.createDirectories(PHOTOS_DIR);
        TravelVaultService service = new TravelVaultService(DATA_FILE);
        try {
            service.load();
        } catch (IOException ignored) {
        }

        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        server.createContext("/", exchange -> {
            byte[] html = buildHtmlPage().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, html.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(html);
            }
        });

        server.createContext("/photos/", exchange -> {
            String fileName = Paths.get(exchange.getRequestURI().getPath()).getFileName().toString();
            Path photoPath = PHOTOS_DIR.resolve(fileName).normalize();
            if (!photoPath.startsWith(PHOTOS_DIR.toAbsolutePath().normalize()) && !photoPath.startsWith(PHOTOS_DIR)) {
                exchange.sendResponseHeaders(403, -1);
                return;
            }
            if (!Files.exists(photoPath)) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }
            String contentType = fileName.endsWith(".png") ? "image/png" : "image/jpeg";
            byte[] bytes = Files.readAllBytes(photoPath);
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        server.createContext("/api/pins", exchange -> {
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 200, loadPinsAsJson());
            } else if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                Map<String, String> params = parseForm(body);

                String title = nonBlankOr(params.get("title"), "Pinned Memory");
                String city = nonBlankOr(params.get("city"), "Pinned Spot");
                String country = nonBlankOr(params.get("country"), "World");
                String continent = nonBlankOr(params.get("continent"), "Global");
                String category = nonBlankOr(params.get("category"), "LANDMARK");
                String dateStr = nonBlankOr(params.get("date"), LocalDate.now().toString());
                LocalDate date = LocalDate.parse(dateStr);

                int rating = 5;
                try {
                    String r = params.get("rating");
                    if (r != null && !r.isBlank()) rating = Integer.parseInt(r.trim());
                } catch (NumberFormatException ignored) {
                }

                String notes = params.getOrDefault("notes", "").trim();
                String specific = nonBlankOr(params.get("specific"),
                        "LANDMARK".equalsIgnoreCase(category) ? "Classic" : "Local Specialty");

                double amount = 0.0;
                try {
                    String a = params.get("amount");
                    if (a != null && !a.isBlank()) amount = Double.parseDouble(a.trim());
                } catch (NumberFormatException ignored) {
                }

                double lat = Double.parseDouble(nonBlankOr(params.get("lat"), "46.0727"));
                double lng = Double.parseDouble(nonBlankOr(params.get("lng"), "18.2323"));
                String photoDataUrl = params.getOrDefault("photoData", "");

                String savedPhotoUrl = savePhotoIfPresent(photoDataUrl);

                Memory memory = "LANDMARK".equalsIgnoreCase(category)
                        ? new LandmarkMemory(title, date, rating, notes, specific, amount)
                        : new CulinaryMemory(title, date, rating, notes, specific, amount);

                service.addMemory(country, continent, city, memory);
                service.save();
                savePinRecord(title, city, country, category, rating, date.toString(),
                        memory.getSummary(), lat, lng, savedPhotoUrl);

                sendJson(exchange, 200, "{\"status\":\"ok\"}");
            }
        });

        server.setExecutor(null);
        server.start();
        String url = "http://localhost:" + PORT;
        System.out.println("Travel Memory Map running at: " + url);

        if (Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().browse(new URI(url));
            } catch (Exception ignored) {
            }
        }
    }

    private static String nonBlankOr(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value.trim();
    }

    private static String savePhotoIfPresent(String dataUrl) throws IOException {
        if (dataUrl == null || !dataUrl.startsWith("data:image/")) {
            return "";
        }
        int commaIndex = dataUrl.indexOf(',');
        if (commaIndex < 0) {
            return "";
        }
        String header = dataUrl.substring(0, commaIndex);
        String base64Part = dataUrl.substring(commaIndex + 1);
        String ext = header.contains("image/png") ? ".png" : ".jpg";
        String fileName = UUID.randomUUID() + ext;

        byte[] imageBytes = Base64.getDecoder().decode(base64Part);
        Files.createDirectories(PHOTOS_DIR);
        Files.write(PHOTOS_DIR.resolve(fileName), imageBytes);
        return "/photos/" + fileName;
    }

    private static void savePinRecord(String title, String city, String country,
                                      String category, int rating, String date, String summary,
                                      double lat, double lng, String photoUrl) throws IOException {
        if (PINS_FILE.getParent() != null) {
            Files.createDirectories(PINS_FILE.getParent());
        }
        String line = String.join("|",
                clean(title), clean(city), clean(country), clean(category),
                String.valueOf(rating), clean(date), clean(summary),
                String.valueOf(lat), String.valueOf(lng), clean(photoUrl)
        ) + System.lineSeparator();
        Files.writeString(PINS_FILE, line, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    private static String loadPinsAsJson() {
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        if (Files.exists(PINS_FILE)) {
            try {
                for (String line : Files.readAllLines(PINS_FILE, StandardCharsets.UTF_8)) {
                    String[] p = line.split("\\|", -1);
                    if (p.length < 10) continue;
                    if (!first) sb.append(",");
                    first = false;
                    sb.append(String.format(
                            "{\"title\":\"%s\",\"city\":\"%s\",\"country\":\"%s\",\"category\":\"%s\",\"rating\":%s,\"date\":\"%s\",\"summary\":\"%s\",\"lat\":%s,\"lng\":%s,\"photoUrl\":\"%s\"}",
                            escJson(p[0]), escJson(p[1]), escJson(p[2]), escJson(p[3]),
                            p[4], escJson(p[5]), escJson(p[6]), p[7], p[8], escJson(p[9])
                    ));
                }
            } catch (IOException ignored) {
            }
        }
        sb.append("]");
        return sb.toString();
    }

    private static Map<String, String> parseForm(String body) {
        Map<String, String> map = new HashMap<>();
        for (String pair : body.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                map.put(URLDecoder.decode(kv[0], StandardCharsets.UTF_8),
                        URLDecoder.decode(kv[1], StandardCharsets.UTF_8));
            }
        }
        return map;
    }

    private static void sendJson(HttpExchange exchange, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String clean(String s) {
        return s == null ? "" : s.replace("|", "/").replace("\n", " ").replace("\r", " ");
    }

    private static String escJson(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String buildHtmlPage() {
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8" />
                  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                  <title>Travel Memory Vault</title>
                  <link rel="preconnect" href="https://fonts.googleapis.com">
                  <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700&display=swap" rel="stylesheet">
                  <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
                  <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
                  <style>
                    * { box-sizing: border-box; }
                    body { margin: 0; font-family: 'Plus Jakarta Sans', system-ui, sans-serif; display: flex; height: 100vh; background: #090d16; color: #f1f5f9; overflow: hidden; }
                    #map { flex: 1; height: 100%; z-index: 1; background: #0b1120; }
                    .dark-tiles { filter: invert(100%) hue-rotate(180deg) brightness(94%) contrast(90%) saturate(85%); }
                    #sidebar {
                      width: 390px; padding: 22px; background: rgba(15, 23, 42, 0.96);
                      border-left: 1px solid rgba(148, 163, 184, 0.14); overflow-y: auto; z-index: 2;
                      box-shadow: -10px 0 35px rgba(0, 0, 0, 0.6);
                    }
                    .brand { display: flex; align-items: center; justify-content: space-between; margin-bottom: 14px; }
                    .brand h1 { margin: 0; font-size: 1.3rem; font-weight: 700; background: linear-gradient(90deg, #38bdf8, #a78bfa); -webkit-background-clip: text; -webkit-text-fill-color: transparent; }
                    .badge-count { background: rgba(56, 189, 248, 0.15); color: #38bdf8; padding: 4px 11px; border-radius: 999px; font-size: 0.78rem; font-weight: 600; }
                    .hint {
                      background: rgba(56, 189, 248, 0.08); border: 1px solid rgba(56, 189, 248, 0.25);
                      padding: 10px 12px; border-radius: 10px; font-size: 0.83rem; color: #bae6fd; margin-bottom: 12px;
                    }
                    .grid-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
                    label { display: block; font-size: 0.72rem; font-weight: 600; text-transform: uppercase; letter-spacing: 0.05em; margin-top: 10px; color: #94a3b8; }
                    .opt { font-weight: 400; text-transform: none; color: #64748b; }
                    input, select {
                      width: 100%; padding: 9px 11px; margin-top: 4px; border-radius: 9px;
                      border: 1px solid #293548; background: #162032; color: #f8fafc; font-family: inherit; font-size: 0.87rem;
                      transition: border-color 0.2s, box-shadow 0.2s;
                    }
                    input:focus, select:focus { outline: none; border-color: #38bdf8; box-shadow: 0 0 0 3px rgba(56, 189, 248, 0.18); }
                    details { margin-top: 10px; background: rgba(30, 41, 59, 0.35); border-radius: 10px; padding: 8px 11px; border: 1px solid rgba(148, 163, 184, 0.1); }
                    summary { cursor: pointer; font-size: 0.8rem; font-weight: 600; color: #94a3b8; user-select: none; }
                    .photo-drop {
                      margin-top: 6px; border: 1.5px dashed #475569; border-radius: 10px; padding: 12px;
                      text-align: center; cursor: pointer; background: rgba(30, 41, 59, 0.45); transition: 0.2s;
                    }
                    .photo-drop:hover { border-color: #38bdf8; background: rgba(56, 189, 248, 0.08); }
                    #photoPreview { max-width: 100%; max-height: 130px; border-radius: 8px; margin-top: 8px; display: none; object-fit: cover; }
                    button.primary {
                      width: 100%; margin-top: 15px; padding: 11px; border: none; border-radius: 10px;
                      background: linear-gradient(135deg, #0ea5e9, #8b5cf6); color: white; font-weight: 600; font-size: 0.92rem;
                      cursor: pointer; box-shadow: 0 4px 16px rgba(56, 189, 248, 0.3); transition: transform 0.15s, opacity 0.15s;
                    }
                    button.primary:hover { opacity: 0.94; transform: translateY(-1px); }
                    .filters { display: flex; gap: 6px; margin-top: 18px; margin-bottom: 10px; }
                    .filter-btn {
                      flex: 1; padding: 6px 8px; border-radius: 8px; border: 1px solid #293548;
                      background: #162032; color: #cbd5e1; font-size: 0.78rem; font-weight: 600; cursor: pointer;
                    }
                    .filter-btn.active { background: #38bdf8; color: #0f172a; border-color: #38bdf8; }
                    .pin-card {
                      background: #162032; border-radius: 12px; overflow: hidden; margin-top: 12px;
                      border: 1px solid rgba(148, 163, 184, 0.12); transition: transform 0.15s; cursor: pointer;
                    }
                    .pin-card:hover { transform: translateY(-2px); border-color: rgba(56, 189, 248, 0.45); }
                    .pin-card img { width: 100%; height: 130px; object-fit: cover; display: block; }
                    .pin-body { padding: 11px 13px; }
                    .tag { display: inline-block; font-size: 0.68rem; font-weight: 700; padding: 2px 8px; border-radius: 999px; margin-bottom: 5px; }
                    .tag-landmark { background: rgba(56, 189, 248, 0.18); color: #38bdf8; }
                    .tag-culinary { background: rgba(251, 146, 60, 0.18); color: #fb923c; }
                    .leaflet-popup-content-wrapper { border-radius: 12px; background: #1e293b; color: #f8fafc; }
                    .leaflet-popup-tip { background: #1e293b; }
                    .popup-card img { width: 220px; height: 130px; object-fit: cover; border-radius: 8px; margin-bottom: 6px; display: block; }
                    .custom-pin {
                      width: 26px; height: 26px; border-radius: 50% 50% 50% 0; transform: rotate(-45deg);
                      border: 2.5px solid #fff; box-shadow: 0 4px 12px rgba(0,0,0,0.6);
                    }
                  </style>
                </head>
                <body>
                  <div id="map"></div>
                  <div id="sidebar">
                    <div class="brand">
                      <h1>Travel Memory Vault</h1>
                      <span class="badge-count" id="totalBadge">0 Pins</span>
                    </div>
                    <div class="hint" id="coordsHint">Click anywhere on the map to pick a spot.</div>

                    <form id="memoryForm">
                      <input type="hidden" id="lat" value="46.0727" />
                      <input type="hidden" id="lng" value="18.2323" />
                      <input type="hidden" id="photoData" value="" />

                      <label>Title <span style="color:#38bdf8;">*</span></label>
                      <input id="title" required placeholder="e.g., Pécs Cathedral or Eiffel Tower" />

                      <div class="grid-2">
                        <div>
                          <label>Category</label>
                          <select id="category">
                            <option value="LANDMARK">Landmark</option>
                            <option value="CULINARY">Culinary</option>
                          </select>
                        </div>
                        <div>
                          <label>Rating <span class="opt">(1–5)</span></label>
                          <input id="rating" type="number" min="1" max="5" value="5" />
                        </div>
                      </div>

                      <label>Upload Photo <span class="opt">(optional)</span></label>
                      <div class="photo-drop" onclick="document.getElementById('photoInput').click()">
                        <span id="photoText" style="font-size:0.82rem;color:#94a3b8;">Click to attach a travel photo</span>
                        <input id="photoInput" type="file" accept="image/*" style="display:none" />
                        <img id="photoPreview" alt="Preview" />
                      </div>

                      <details>
                        <summary>+ More Details (City, Country, Cost, Notes — all optional)</summary>
                        <div class="grid-2">
                          <div>
                            <label>City</label>
                            <input id="city" placeholder="e.g., Pécs" />
                          </div>
                          <div>
                            <label>Country</label>
                            <input id="country" placeholder="e.g., Hungary" />
                          </div>
                        </div>
                        <div class="grid-2">
                          <div>
                            <label>Continent</label>
                            <input id="continent" placeholder="e.g., Europe" />
                          </div>
                          <div>
                            <label>Date Visited</label>
                            <input id="date" type="date" />
                          </div>
                        </div>
                        <div class="grid-2">
                          <div>
                            <label>Style / Dish</label>
                            <input id="specific" placeholder="Optional" />
                          </div>
                          <div>
                            <label>Cost (EUR)</label>
                            <input id="amount" type="number" step="0.01" placeholder="0" />
                          </div>
                        </div>
                        <label>Notes</label>
                        <input id="notes" placeholder="Optional memory note..." />
                      </details>

                      <button class="primary" type="submit">Save & Pin Memory</button>
                    </form>

                    <div class="filters">
                      <button class="filter-btn active" data-filter="ALL">All</button>
                      <button class="filter-btn" data-filter="LANDMARK">Landmarks</button>
                      <button class="filter-btn" data-filter="CULINARY">Culinary</button>
                    </div>
                    <div id="pinList"></div>
                  </div>

                  <script>
                    document.getElementById('date').valueAsDate = new Date();

                    const map = L.map('map').setView([47.5, 18.5], 4);
                    L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
                      attribution: '&copy; OpenStreetMap contributors',
                      maxZoom: 19,
                      className: 'dark-tiles'
                    }).addTo(map);

                    let tempMarker = null;
                    let allPins = [];
                    let activeFilter = 'ALL';
                    const markersLayer = L.layerGroup().addTo(map);

                    function makePinIcon(category) {
                      const color = category === 'CULINARY' ? '#f97316' : '#38bdf8';
                      return L.divIcon({
                        className: '',
                        html: `<div class="custom-pin" style="background:${color};"></div>`,
                        iconSize: [26, 26],
                        iconAnchor: [13, 26],
                        popupAnchor: [0, -26]
                      });
                    }

                    map.on('click', function(e) {
                      const lat = e.latlng.lat.toFixed(5);
                      const lng = e.latlng.lng.toFixed(5);
                      document.getElementById('lat').value = lat;
                      document.getElementById('lng').value = lng;
                      document.getElementById('coordsHint').innerHTML = `Selected spot: <b>${lat}, ${lng}</b>`;
                      if (tempMarker) map.removeLayer(tempMarker);
                      tempMarker = L.marker([lat, lng]).addTo(map);
                    });

                    document.getElementById('photoInput').addEventListener('change', function(e) {
                      const file = e.target.files[0];
                      if (!file) return;
                      const reader = new FileReader();
                      reader.onload = function(evt) {
                        const img = new Image();
                        img.onload = function() {
                          const canvas = document.createElement('canvas');
                          const maxW = 800;
                          const scale = Math.min(1, maxW / img.width);
                          canvas.width = img.width * scale;
                          canvas.height = img.height * scale;
                          canvas.getContext('2d').drawImage(img, 0, 0, canvas.width, canvas.height);
                          const compressed = canvas.toDataURL('image/jpeg', 0.82);
                          document.getElementById('photoData').value = compressed;
                          const preview = document.getElementById('photoPreview');
                          preview.src = compressed;
                          preview.style.display = 'block';
                          document.getElementById('photoText').textContent = file.name;
                        };
                        img.src = evt.target.result;
                      };
                      reader.readAsDataURL(file);
                    });

                    document.querySelectorAll('.filter-btn').forEach(btn => {
                      btn.addEventListener('click', () => {
                        document.querySelectorAll('.filter-btn').forEach(b => b.classList.remove('active'));
                        btn.classList.add('active');
                        activeFilter = btn.dataset.filter;
                        renderPins();
                      });
                    });

                    function renderPins() {
                      markersLayer.clearLayers();
                      const list = document.getElementById('pinList');
                      list.innerHTML = '';
                      const filtered = activeFilter === 'ALL' ? allPins : allPins.filter(p => p.category === activeFilter);
                      document.getElementById('totalBadge').textContent = `${filtered.length} Pin${filtered.length === 1 ? '' : 's'}`;

                      filtered.forEach(p => {
                        const marker = L.marker([p.lat, p.lng], { icon: makePinIcon(p.category) }).addTo(markersLayer);
                        const imgHtml = p.photoUrl ? `<img src="${p.photoUrl}" alt="${p.title}" />` : '';
                        marker.bindPopup(`
                          <div class="popup-card">
                            ${imgHtml}
                            <b>${p.title}</b> (${p.city}, ${p.country})<br/>
                            <small style="color:#94a3b8;">${'★'.repeat(p.rating)}${'☆'.repeat(5 - p.rating)} · ${p.date}</small><br/>
                            <p style="margin:5px 0 0;font-size:12px;">${p.summary}</p>
                          </div>
                        `);

                        const card = document.createElement('div');
                        card.className = 'pin-card';
                        const badgeClass = p.category === 'CULINARY' ? 'tag-culinary' : 'tag-landmark';
                        card.innerHTML = `
                          ${p.photoUrl ? `<img src="${p.photoUrl}" alt="${p.title}" />` : ''}
                          <div class="pin-body">
                            <span class="tag ${badgeClass}">${p.category}</span>
                            <div style="font-weight:700;font-size:0.95rem;">${p.title}</div>
                            <div style="color:#94a3b8;font-size:0.8rem;margin:2px 0 6px;">${p.city}, ${p.country} · ${'★'.repeat(p.rating)}</div>
                            <div style="font-size:0.8rem;color:#cbd5e1;">${p.summary}</div>
                          </div>
                        `;
                        card.addEventListener('click', () => {
                          map.flyTo([p.lat, p.lng], 13);
                          marker.openPopup();
                        });
                        list.appendChild(card);
                      });
                    }

                    async function loadPins() {
                      const res = await fetch('/api/pins');
                      allPins = await res.json();
                      renderPins();
                    }

                    document.getElementById('memoryForm').addEventListener('submit', async (e) => {
                      e.preventDefault();
                      const data = new URLSearchParams({
                        lat: document.getElementById('lat').value,
                        lng: document.getElementById('lng').value,
                        category: document.getElementById('category').value,
                        title: document.getElementById('title').value,
                        city: document.getElementById('city').value,
                        country: document.getElementById('country').value,
                        continent: document.getElementById('continent').value,
                        date: document.getElementById('date').value,
                        rating: document.getElementById('rating').value,
                        specific: document.getElementById('specific').value,
                        amount: document.getElementById('amount').value,
                        notes: document.getElementById('notes').value,
                        photoData: document.getElementById('photoData').value
                      });
                      await fetch('/api/pins', { method: 'POST', body: data });
                      if (tempMarker) { map.removeLayer(tempMarker); tempMarker = null; }
                      document.getElementById('title').value = '';
                      document.getElementById('notes').value = '';
                      document.getElementById('photoData').value = '';
                      document.getElementById('photoPreview').style.display = 'none';
                      document.getElementById('photoText').textContent = 'Click to attach a travel photo';
                      await loadPins();
                    });

                    loadPins();
                  </script>
                </body>
                </html>
                """;
    }
}