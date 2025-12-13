package com.example.datingapp;

import org.springframework.web.bind.annotation.*;

/**
 * @author LLM
 */
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * REST controller used by the single-page HTML frontend.
 *
 * Responsibilities:
 * - Load users + relationships from CSV on startup
 * - Provide endpoints used by the UI (login/register/autocomplete/interact/match/lists/table)
 * - Enforce privacy for the table view unless in Editor mode (isAdmin=true)
 *
 * NOTE: "Recommended matches" are stored here in the controller (in-memory) so we do NOT
 * have to add methods/fields to People.java.
 */
@RestController
@CrossOrigin(origins = "*")
public class WebController {

    private static PeopleHashTable database;
    private static final List<String> globalMatchHistory = new ArrayList<>();

    // Recommended matches per user (email -> set of recommended emails)
    // LinkedHashSet keeps insertion order and avoids duplicates.
    private static final Map<String, LinkedHashSet<String>> recommendedByUser = new HashMap<>();

    public WebController() {
        if (database != null) return;

        database = new PeopleHashTable();

        // The UI expects real data from users.csv / relationshipsNew.csv.
        // Working directory can vary in Spring Boot, so try a few common locations.
        String usersPath = resolveCsvPath("userTest.csv");
        String relPath = resolveCsvPath("relationshipsTest.csv");

        try {
            database.loadPeopleFromCSV(usersPath);
            database.loadRelationships(relPath);
            System.out.println("Database loaded successfully: " + usersPath + " + " + relPath);
        } catch (Exception e) {
            System.out.println("CSV files not found, starting empty. Details: " + e.getMessage());
        }
    }

    private String resolveCsvPath(String filename) {
        // 1) repo root
        if (Files.exists(Path.of(filename))) return filename;
        // 2) common Maven layout
        Path p = Path.of("src", "main", "resources", filename);
        if (Files.exists(p)) return p.toString();
        // 3) fall back to filename anyway (lets FileReader throw a useful error)
        return filename;
    }

    // --- Recommended match storage (in-memory) ---

    private static void addRecommendation(String aEmail, String bEmail) {
        if (aEmail == null || bEmail == null) return;

        String a = aEmail.trim().toLowerCase();
        String b = bEmail.trim().toLowerCase();

        if (a.isEmpty() || b.isEmpty()) return;
        if (a.equals(b)) return;

        recommendedByUser.computeIfAbsent(a, k -> new LinkedHashSet<>()).add(b);
    }

    private static List<String> getRecommendations(String email) {
        if (email == null) return new ArrayList<>();
        String key = email.trim().toLowerCase();
        LinkedHashSet<String> set = recommendedByUser.getOrDefault(key, new LinkedHashSet<>());
        return new ArrayList<>(set);
    }

    /**
     * Table view data.
     * - If isAdmin=true (Editor View), include all private lists for all users.
     * - Otherwise, only include private lists for the viewer (viewerEmail).
     */
    @GetMapping("/api/table")
    public List<PeopleDto> getTable(@RequestParam(required = false) String viewerEmail,
                                    @RequestParam(defaultValue = "false") boolean isAdmin) {
        List<PeopleDto> displayList = new ArrayList<>();
        for (People p : database.getAllPeople()) {
            boolean includeLists = isAdmin || (viewerEmail != null && viewerEmail.equalsIgnoreCase(p.getEmail()));
            displayList.add(new PeopleDto(p, includeLists));
        }
        return displayList;
    }

    @PostMapping("/api/login")
    public Map<String, Object> login(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        People user = database.get(email);

        Map<String, Object> response = new HashMap<>();
        if (user != null) {
            response.put("status", "success");
            response.put("user", new PeopleDto(user, true));
        } else {
            response.put("status", "error");
            response.put("message", "Email not found");
        }
        return response;
    }

    @PostMapping("/api/register")
    public Map<String, Object> registerUser(@RequestBody RegistrationRequest request) {
        People newPerson = new People(request.name, request.email);
        newPerson.setMbtiSelfType(request.mbti);

        // Gender fields come from the frontend as strings (e.g. "Woman" or "Woman, Man")
        if (request.gender != null) newPerson.setGender(request.gender);
        if (request.genderPrefs != null) newPerson.setGenderPreferencesFromString(request.genderPrefs);

        database.insert(newPerson);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "User created: " + request.name);
        return response;
    }

    @PostMapping("/api/interact")
    public Map<String, Object> interact(@RequestBody InteractionRequest request) {
        People source = database.get(request.sourceEmail);
        People target = database.get(request.targetEmail);

        Map<String, Object> response = new HashMap<>();

        if (source == null || target == null) {
            response.put("status", "error");
            response.put("message", "One or both emails not found.");
            return response;
        }
        if (source.getEmail().equalsIgnoreCase(target.getEmail())) {
            response.put("status", "error");
            response.put("message", "You cannot interact with yourself.");
            return response;
        }

        LikeMatcher matcher = new LikeMatcher(source);

        if ("like".equalsIgnoreCase(request.type)) {
            target.incrementLikedByCount();

            // Detect whether a new mutual match was created by this action
            int before = source.getLikedEmailsMatch() == null ? 0 : source.getLikedEmailsMatch().size();
            matcher.RomanticLiker(target);
            int after = source.getLikedEmailsMatch() == null ? 0 : source.getLikedEmailsMatch().size();

            if (after > before) {
                response.put("message", "💘 MATCH! You and " + target.getName() + " liked each other.");
            } else {
                response.put("message", "You LIKED " + target.getName());
            }

        } else if ("friend".equalsIgnoreCase(request.type)) {
            int before = source.getFriendEmailsMatch() == null ? 0 : source.getFriendEmailsMatch().size();
            matcher.FriendLiker(target);
            int after = source.getFriendEmailsMatch() == null ? 0 : source.getFriendEmailsMatch().size();

            if (after > before) {
                response.put("message", "🧩 FRIEND MATCH! You and " + target.getName() + " friend-liked each other.");
            } else {
                response.put("message", "You FRIEND-LIKED " + target.getName());
            }

        } else {
            response.put("status", "error");
            response.put("message", "Invalid interaction type");
            return response;
        }

        response.put("status", "success");
        return response;
    }

    /**
     * "Find Me A Match" endpoint.
     * Requirement: recommended matches must appear in BOTH users' pages.
     *
     * We store recommendations in-memory here (recommendedByUser) instead of People.java.
     */
    @GetMapping("/api/match")
    public PeopleDto findMatch(@RequestParam String email) {
        People me = database.get(email);
        People match = database.findMatch(email);

        if (match == null) return null;

        // Save to BOTH users so it shows on either profile's "Recommended" list
        addRecommendation(email, match.getEmail());
        addRecommendation(match.getEmail(), email);

        // Store a readable history string (Editor View "Global Matches")
        if (me != null) {
            String record = me.getName() + " (" + me.getMbtiRaw() + ") recommended with "
                    + match.getName() + " (" + match.getMbtiRaw() + ")";
            if (!globalMatchHistory.contains(record)) globalMatchHistory.add(record);
        }

        return new PeopleDto(match, false);
    }

    @GetMapping("/api/admin/matches")
    public List<String> getGlobalMatchHistory() {
        return globalMatchHistory;
    }

    @GetMapping("/api/lists")
    public Map<String, Object> lists(@RequestParam String email) {
        Map<String, Object> response = new HashMap<>();
        People me = database.get(email);

        if (me == null) {
            response.put("status", "error");
            response.put("message", "User not found");
            return response;
        }

        response.put("status", "success");
        response.put("liked", resolveToDtos(me.getLikedEmails()));
        response.put("friendLiked", resolveToDtos(me.getFriendEmails()));
        response.put("matches", resolveToDtos(me.getLikedEmailsMatch()));
        response.put("friendMatches", resolveToDtos(me.getFriendEmailsMatch()));

        // NEW: recommended list (stored in controller)
        response.put("recommended", resolveToDtos(getRecommendations(me.getEmail())));

        return response;
    }

    private List<PeopleDto> resolveToDtos(List<String> emails) {
        List<PeopleDto> out = new ArrayList<>();
        if (emails == null) return out;

        for (String e : emails) {
            People p = database.get(e);
            if (p != null) out.add(new PeopleDto(p, false));
        }
        return out;
    }

    @GetMapping("/api/autocomplete")
    public List<PeopleDto> autocomplete(@RequestParam String prefix) {
        List<PeopleDto> displayList = new ArrayList<>();
        ArrayList<People> matches = database.autocompleteByPopularity(prefix);
        for (People p : matches) displayList.add(new PeopleDto(p, false));
        return displayList;
    }

    @GetMapping("/api/search")
    public List<PeopleDto> search(@RequestParam String email, @RequestParam String name) {
        List<PeopleDto> displayList = new ArrayList<>();
        ArrayList<People> matches = database.searchByNameRankedByMbti(name, email);
        for (People p : matches) displayList.add(new PeopleDto(p, false));
        return displayList;
    }

    // --- DTO + Request classes ---

    static class PeopleDto {
        public String name;
        public String email;
        public String mbti;

        public List<Integer> stats;     // Pref vector
        public List<Integer> selfStats; // Self vector
        public int likedByCount;
        public int validLikes;

        public String gender;
        public String genderPreferences;

        // Private lists (only filled when includePrivateLists=true)
        public List<String> likedEmails;
        public List<String> friendEmails;
        public List<String> matches;
        public List<String> friendMatches;

        // Recommended list (we keep it here for editor/table view + debugging)
        public List<String> recommendedMatches;

        public PeopleDto(People p, boolean includePrivateLists) {
            this.name = p.getName();
            this.email = p.getEmail();
            this.mbti = p.getMbtiRaw();

            this.stats = p.getMbtiStats();
            this.selfStats = p.getMbtiSelfType();
            this.likedByCount = p.getLikedByCount();
            this.validLikes = p.getValidLikes();

            // Gender columns (these depend on PeopleHashTable.loadPeopleFromCSV loading them correctly)
            this.gender = p.getGender();
            List<String> prefs = p.getGenderPreferences();
            this.genderPreferences = (prefs != null && !prefs.isEmpty()) ? String.join(", ", prefs) : "All";

            if (includePrivateLists) {
                this.likedEmails = p.getLikedEmails();
                this.friendEmails = p.getFriendEmails();
                this.matches = p.getLikedEmailsMatch();
                this.friendMatches = p.getFriendEmailsMatch();

                // Pull recommendations from the controller map
                this.recommendedMatches = getRecommendations(p.getEmail());
            } else {
                this.likedEmails = new ArrayList<>();
                this.friendEmails = new ArrayList<>();
                this.matches = new ArrayList<>();
                this.friendMatches = new ArrayList<>();
                this.recommendedMatches = new ArrayList<>();
            }
        }
    }

    static class RegistrationRequest {
        public String name;
        public String email;
        public String mbti;
        public String gender;
        public String genderPrefs;
    }

    static class InteractionRequest {
        public String sourceEmail;
        public String targetEmail;
        public String type;
    }
}
