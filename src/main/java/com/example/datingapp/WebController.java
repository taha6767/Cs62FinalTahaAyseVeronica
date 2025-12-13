package com.example.datingapp;

import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@CrossOrigin(origins = "*")
public class WebController {

    private static PeopleHashTable database;
    private static List<String> globalMatchHistory = new ArrayList<>();

    public WebController() {
        if (database == null) {
            database = new PeopleHashTable();
            try {
                database.loadPeopleFromCSV("userTest.csv");
                database.loadRelationships("relationshipsTest.csv");
                System.out.println("Database loaded successfully!");
            } catch (Exception e) {
                System.out.println("CSV files not found, starting empty.");
            }
        }
    }

    // MODIFIED: Accepts isAdmin to show full lists for Editor Mode
    @GetMapping("/api/table")
    public List<PeopleDto> getTable(@RequestParam(required = false) String viewerEmail, 
                                    @RequestParam(defaultValue = "false") boolean isAdmin) {
        List<PeopleDto> displayList = new ArrayList<>();
        for (People p : database.getAllPeople()) {
            // Show details if Admin OR if the viewer is looking at themselves
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
            response.put("message", "Login successful");
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
        if (request.sourceEmail != null && request.sourceEmail.equalsIgnoreCase(request.targetEmail)) {
            response.put("status", "error");
            response.put("message", "You cannot interact with yourself.");
            return response;
        }

        LikeMatcher matcher = new LikeMatcher(source);

        if ("like".equalsIgnoreCase(request.type)) {
            target.incrementLikedByCount();
            matcher.RomanticLiker(target);
            if (source.getLikedEmailsMatch() != null && source.getLikedEmailsMatch().contains(target.getEmail())) {
                response.put("message", "💘 MATCH! You and " + target.getName() + " liked each other.");
            } else {
                response.put("message", "You LIKED " + target.getName());
            }
        } else if ("friend".equalsIgnoreCase(request.type)) {
            matcher.FriendLiker(target);
            if (source.getFriendEmailsMatch() != null && source.getFriendEmailsMatch().contains(target.getEmail())) {
                response.put("message", "🧩 FRIEND MATCH! You and " + target.getName() + " friend-liked each other.");
            } else {
                response.put("message", "You FRIEND-LIKED " + target.getName());
            }
        } else {
            response.put("status", "error");
            response.put("message", "Invalid interaction type.");
            return response;
        }

        response.put("status", "success");
        return response;
    }

    @GetMapping("/api/match")
    public PeopleDto findMatch(@RequestParam String email) {
        People me = database.get(email);
        People match = database.findMatch(email);
        
        if (match != null) {
            if (me != null) {
                String record = me.getName() + " (" + me.getMbtiRaw() + ") matched with " + match.getName() + " (" + match.getMbtiRaw() + ")";
                if (!globalMatchHistory.contains(record)) {
                    globalMatchHistory.add(record);
                }
            }
            return new PeopleDto(match, false);
        }
        return null;
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
            response.put("message", "Email not found");
            return response;
        }
        response.put("status", "success");
        response.put("liked", resolveToDtos(me.getLikedEmails()));
        response.put("friendLiked", resolveToDtos(me.getFriendEmails()));
        response.put("matches", resolveToDtos(me.getLikedEmailsMatch()));
        response.put("friendMatches", resolveToDtos(me.getFriendEmailsMatch()));
        return response;
    }

    private List<PeopleDto> resolveToDtos(List<String> emails) {
        List<PeopleDto> out = new ArrayList<>();
        if (emails == null) return out;
        for (String e : emails) {
            if (e == null) continue;
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

static class PeopleDto {
        public String name;
        public String email;
        public String mbti;
        public List<Integer> stats;
        public int likedByCount;
        
        // NEW: Fields to visualize gender
        public String gender;
        public String genderPreferences; 

        // Private lists (only for admin/self)
        public List<String> likedEmails;
        public List<String> friendEmails;

        public PeopleDto(People p, boolean includePrivateLists) {
            this.name = p.getName();
            this.email = p.getEmail();
            this.mbti = p.getMbtiRaw();
            this.stats = p.getMbtiStats();
            this.likedByCount = p.getLikedByCount();

            // 1. Get the gender directly
            this.gender = p.getGender();

            // 2. Convert the preferences list (["men", "women"]) into a clean String ("men, women")
            List<String> prefs = p.getGenderPreferences();
            if (prefs != null && !prefs.isEmpty()) {
                this.genderPreferences = String.join(", ", prefs);
            } else {
                this.genderPreferences = "All / Unspecified";
            }

            if (includePrivateLists) {
                this.likedEmails = p.getLikedEmails();
                this.friendEmails = p.getFriendEmails();
            } else {
                this.likedEmails = new ArrayList<>();
                this.friendEmails = new ArrayList<>();
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