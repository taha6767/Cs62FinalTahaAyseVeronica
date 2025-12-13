# 5C MatchMaking

## Description
This project is a small “dating app” backend + web API that users can match with other users if they mutually like eachother. It also learns a user’s MBTI preferences based on who they like, and then suggests compatible matches. 

### Features

1. **Mutual-Like Matching**: Users can use this program to search for people that they like in real life or want to be friends with and send them romantic or friendship likes. The other person cannot view that they have been liked, unless the feelings are mutual. When two users like eachother mutually in the same way(romantic or friendly), they are a match! They get notified that the feelings are mutual and they can take it from there.

2. **Mutual-Like Matching**: The program tracks the MBTIs of the people users like to understand the user's "type". Based on that, the user can ask for the program to reccomend another user to them. The recommended user's "type" (what MBTI they like) is also tracked. So the program gives matching reccomandations based on mutual compatibility. 

3. **MBTI-Based Autocomplete Search**: When the user is searching for someone to send a like to, the autocomplete function helps them find the person they are looking for based on what they have typed so far, and then their popularity on the app. So if a user likes "Ayşegül Kula", but only knows her nickname, "Ayșe", the autocomplete tools suggest people based on the search. Also, the autocomplete reccomandations are ranked based on the user's prefered MBTI type. So if a user who have been liking extraverts and feelers types "Ja", ENFP "James Charles" will be reccomanded before INTP "Jack Smith" who recieved 5 likes, which will be reccomanded before INTP "Jake Jackson" who recieved 3 likes.

## Requirements

- **Java 17+** recommended for Spring Boot.
- A standard Spring Boot build tool (**Maven**) depending on your repo setup

### External libraries
This project uses Spring Boot (`org.springframework.boot.*`, `org.springframework.web.bind.annotation.*`).  

## How to run

### Run the web server (Spring Boot)

From the repository root:

**If your repo uses Maven (`pom.xml` present):**
```bash
mvn spring-boot:run
```

Then you can call the API directly:
Server runs at: http://localhost:8080



> **CSV location:** `WebController` loads `userTest.csv` and `relationshipsTest.csv` using relative paths. Keep those CSVs in the project root. UserTest has less than 20 entries so it's easier to use in test runs. The whole 1000 data points 
is in users.csv and relationshipsNew.csv so in `WebController` you can choose to load them instead.



# Public API (Java classes)

Below is the “API” for the public methods.

## `People` (user model)

### Constructor

* `People(String name, String email)`

  * Creates a new user with empty MBTI preference stats and empty relationship lists.

**Example**

```java
People p = new People("User One", "user1@example.com");
```

### MBTI methods

* `void setMbtiSelfType(String mbtiType)`

  * Sets the user’s MBTI arraylist from a 4-letter string (e.g., `"ENFP"`).

**Example**

```java
p.setMbtiSelfType("ENFP");
```

* `void updateMbtiStats(String targetMbti)`

  * Updates this user’s *preference stats* based on liking someone with MBTI `targetMbti`.

**Example**

```java
p.updateMbtiStats("ISTJ");
```
(This is gonna be used when the app is recommanding matches based on people's preferences, and for ranked autocomplete when searching for people)
### Likes/Friends tracking

* `void addLikedEmail(String email)`
* `void addFriendEmail(String email)`
* `void addLikedEmailMatch(String email)`
* `void addFriendEmailMatch(String email)`

**Example**

```java
p.addLikedEmail("user2@example.com");
p.addLikedEmailMatch("user2@example.com");
```

### Popularity


* `void incrementLikedByCount()`
* `int getLikedByCount()`

**Example**

```java
p.incrementLikedByCount();
int pop = p.getLikedByCount(); // pop = previous + 1
```
(This is used when ranking recommandations for autocomplete)
### Gender + dating preferences

* `void setGender(String gender)`

  * Stores gender as lowercase. Defaults to `"unspecified"` if empty.

* `void setGenderPreferencesFromString(String raw)`

  * Parses a comma-separated string into a list.

* `boolean isGenderCompatibleWith(People other)`

  * True if `other.gender` is in this user’s preference list, or if this user has no preferences saved.

* `boolean isMutuallyRomanticallyCompatible(People other)`

  * True if both users consider each other gender-compatible.

**Example**

```java
p.setGender("women");
p.setGenderPreferencesFromString("men,non-binary");

People q = new People("User Two", "user2@example.com");
q.setGender("men");
q.setGenderPreferencesFromString("women");

boolean ok = p.isMutuallyRomanticallyCompatible(q); // true
```

### Other Accessors

* `String getEmail()`, `String getName()`, `String getMbtiRaw()`
* `ArrayList<Integer> getMbtiStats()`, `ArrayList<Integer> getMbtiSelfType()`
* `ArrayList<String> getLikedEmails()`, `ArrayList<String> getFriendEmails()`
* `ArrayList<String> getLikedEmailsMatch()`, `ArrayList<String> getFriendEmailsMatch()`

---

## `LikeMatcher` (handles likes + mutual matches)

### Constructor

* `LikeMatcher(People p)`

  * Creates a matcher for the “liker” user.

### Public methods

* `void RomanticLiker(People p)`

  * Adds `p` to liker’s liked list, updates MBTI preference stats, checks for mutual romantic match.
  * If mutual, both users are added to each other’s romantic match lists and removed from liked lists.

**Example**

```java
LikeMatcher m = new LikeMatcher(user1);
m.RomanticLiker(user2);
```

* `void FriendLiker(People p)`

  * Same idea but for friend-like lists and friend matches.

**Example**

```java
LikeMatcher m = new LikeMatcher(user1);
m.FriendLiker(user2);
```

---

## `PeopleHashTable` (core database and algorithms)

Implements `MatchDatabase`.

### Constructors

* `PeopleHashTable()`
* `PeopleHashTable(int size)`

**Example**

```java
PeopleHashTable db = new PeopleHashTable();
```

### Database operations

* `void insert(People person)`
* `void remove(String email)`
* `People get(String email)`
* `boolean contains(String email)`
* `ArrayList<People> getAllPeople()`

**Example**

```java
db.insert(new People("User One", "user1@example.com"));
People p = db.get("user1@example.com");
boolean exists = db.contains("user1@example.com"); // true
```

### Loading data

* `void loadPeopleFromCSV(String filename)`

  * Reads a CSV and inserts users into the table.

* `void loadRelationships(String filename)`

  * Reads relationship edges and uses `LikeMatcher` to apply likes/friendlikes and populate like and match lists.

**Example**

```java
db.loadPeopleFromCSV("userTest.csv");
db.loadRelationships("relationshipsTest.csv");
```

### Matching

* `People findMatch(String email)`

  * Returns a compatible candidate or `null`.
  * Compatibility uses:

    * mutual MBTI preference compatibility 
        * For each pair of qualities (I/E,N/S,T/F,P/J), we say that a user prefers one over the other if more than half the people the user liked falls into one of the categories. For example, if I have liked 4 Thinkers and 1 Feelers, we say that I have a preference for Thinkers. If I have liked 3 Percievers and 2 Judgers, we say that I don't have a between percievers and judgers. Two users are only considered compatible for reccomanded matching if their MBTI types agree with the other person's 
    * mutual gender preference compatibility
        * Two people are only recommanded as matches if they both said that they are interested in dating people with the other person's gender.
    * avoids suggesting someone who is already a mutual-like match

**Example**

```java
People match = db.findMatch("example100007@hmc.edu");
if (match != null) System.out.println(match.getEmail());
```

### Autocomplete + Search

* `ArrayList<People> autocompleteByPopularity(String prefix)`

  * Finds users whose name contains a token matching `prefix`, sorted by `likedByCount` descending.

**Example**

```java
db.autocompleteByPopularity("jo"); // returns list of People
```

* `ArrayList<People> searchByNameRankedByMbti(String nameQuery, String currentUserEmail)`

  * Returns name matches ranked by MBTI compatibility score (then popularity).

**Example**

```java
db.searchByNameRankedByMbti("mi", "user1@example.com");
```

### Visualization

* `void printTable()`
* `void printDetailedRelations()`

**Example**

```java
db.printTable();
db.printDetailedRelations();
```

---

# Public API (Web endpoints)

The Spring Boot server exposes JSON endpoints from `WebController`.

## Base URL

* `http://localhost:8080`


## Authors

* Aysegul Kula
* Taha Disbudak
* Veronica Li
