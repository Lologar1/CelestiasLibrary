package me.analyzers;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import me.analyzers.utilities.ANSIColors;
import me.analyzers.utilities.Dialogue;
import me.analyzers.utilities.MathUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Main {
    public static final String librarian = ANSIColors.CYAN + "Complete Catalogue" + ANSIColors.RESET;
    static long start;

    public static void main(String[] args) throws IOException, ParseException {
        System.out.println(ANSIColors.GREEN + "Welcome to Celestia's Library v1.0 by Analyzers !");
        System.out.println(ANSIColors.YELLOW + "Loading files...");
        long start = System.currentTimeMillis();
        try {
            load();
            System.out.println(ANSIColors.GREEN + "Done ! Took " + (System.currentTimeMillis()-start)/1000 + " seconds.");
        } catch (Exception e) {
            System.out.println("Can't load files ! Try recompiling.");
        }
        System.out.println(librarian + ": " + Dialogue.greetings + " My name is " + librarian + " and I'm in charge of this fine library. " + Dialogue.offerHelp);
        boolean exit = false;
        Scanner input = new Scanner(System.in);
        while (!exit) {
            System.out.print(ANSIColors.BLUE + ">" + ANSIColors.RESET);
            String command = input.nextLine().toLowerCase();
            exit = !handleCommand(command);
        }
        System.out.println(librarian + ": You are exiting " + ANSIColors.GREEN + "Celestia's Library" + ANSIColors.RESET + ". " + Dialogue.goodbye);
    }

    public static boolean handleCommand(String command) throws IOException, ParseException {
        //Recompile : will re-generate the index using the one provided in the archive.

        if (command.startsWith("search")) {
            MathUtils.search(command);
            return true;
        }

        if (command.startsWith("view")) {
            MathUtils.view(command);
            return true;
        }

        if (command.startsWith("tag ")) {
            String[] tok = command.split(" ", 2);
            if (tok.length != 2) {
                System.out.println(librarian + ": Usage: " + ANSIColors.RED + "tag" + ANSIColors.RESET + " [tag to verify]");
                return true;
            }
            MathUtils.tag(tok[1]);
            return true;
        }

        if (command.startsWith("fetch")) {
            String[] tok = command.split(" ");
            if (tok.length != 2) {
                System.out.println(librarian + ": Usage: " + ANSIColors.RED + "fetch" + ANSIColors.RESET + " [story id]");
                return true;
            }
            MathUtils.fetch(tok[1]);
            return true;
        }

        switch (command) {
            case "exit" -> {
                return false;
            }
            case "setup" -> {
                System.out.println(librarian + ": To properly setup the archive, first decompress a Fimfiction archive (from Fimfarchive) into the same directory as Celestia's Library.");
                System.out.println(librarian + ": From there, simply run " + ANSIColors.RED + "recompile" + ANSIColors.RESET + ".");
            }
            case "help" -> {
                System.out.println(librarian + ": As chief archivist of " + ANSIColors.GREEN + "Celestia's Library" + ANSIColors.RESET
                        + ", it is my duty to help you to the best of my abilities !");
                System.out.println(librarian + ": If you believe it is necessary, ask me to " + ANSIColors.RED + "recompile" + ANSIColors.RESET
                        + " and I'll refresh our index of the library's inventory !");
                System.out.println(librarian + ": If you have trouble figuring out how stuff works around here, don't be shy and ask me for some " + ANSIColors.RED
                        + "info" + ANSIColors.RESET + " !");
                System.out.println(librarian + ": Get started anytime and " + ANSIColors.RED + "search" + ANSIColors.RESET
                        + "up some books. I dare say we have the finest in all Equestria !");
                System.out.println(librarian + ": For more specific queries, you can always " + ANSIColors.RED + "view" + ANSIColors.RESET + " a "
                        + ANSIColors.YELLOW + "story" + ANSIColors.RESET + " or " + ANSIColors.YELLOW + "author" + ANSIColors.RESET + ", using its unique ID ! ");
                System.out.println(librarian + ": Once you know which book you want, I'll go " + ANSIColors.RED + "fetch" + ANSIColors.RESET + " it for you in no time.");
                System.out.println(librarian + ": When you're finished, simply take the " + ANSIColors.RED + "exit" + ANSIColors.RESET + " at the back." +
                        " I hear there's a nice garden close by.");
                System.out.println(librarian + ": P.S. If you're unsure where to begin, run " + ANSIColors.RED + "setup" + ANSIColors.RESET
                        + " to get the proper directions for a first-time browsing !");
            }
            case "recompile" -> {
                boolean success = recompile();
                if (!success) {
                    break;
                }
                long durationMillis = System.currentTimeMillis()-start;
                int durationMinutes = (int) (durationMillis/60000);
                int durationSeconds = (int) durationMillis/1000 - durationMinutes * 60;
                System.out.println(librarian + ": Phew! Now that was intense. Glad it's done ! Took " + durationMinutes + " minutes " + durationSeconds + " seconds.");
                System.out.println(librarian + ": Fun fact ! There are " + totalstories + " stories totalling " + totalwords + " words, averaging ~" + totalwords/ totalstories + " words per story !");
            }
            case "info" -> {
                System.out.println(librarian + ": The " + ANSIColors.RED + "search" + ANSIColors.RESET + "ing syntax is quite simple ! Simply state " + ANSIColors.RED
                        + "search" + ANSIColors.RESET + " followed by either " + ANSIColors.GREEN + "author" + ANSIColors.RESET + " or " + ANSIColors.GREEN + "story" + ANSIColors.RESET
                        + " and add any numbers of " + ANSIColors.YELLOW + "+filter" + ANSIColors.RESET + " filters or impose an " + ANSIColors.YELLOW + "+order" + ANSIColors.RESET
                        + " ordering after that. ");
                System.out.println("If you don't know them, simply ask me for the " + ANSIColors.RED + "list" + ANSIColors.RESET + " of filters and orders,"
                        + " I'd be happy to provide it ! Multiple arguments to a filter need to be separated by a comma, whereas defining a new ordering or filter needs only"
                        + " a space. The keyword " + ANSIColors.YELLOW + "reversed" + ANSIColors.RESET + " may be given to an ordering as an additional argument to reverse the order.");
                System.out.println(librarian + ": Note that the special set operators " + ANSIColors.YELLOW + "|" + ANSIColors.RESET + " (union) or " + ANSIColors.YELLOW
                        + "!" + ANSIColors.RESET + " (exclusion) may be applied before any argument. By default the set operation is intersection. Operations are applied in order of writing.");
                System.out.println(librarian + ": It's also possible to filter using a regular expression by having the keyword " + ANSIColors.YELLOW + "regex" + ANSIColors.RESET
                        + " after the filter. E.g. " + ANSIColors.RED + "search" + ANSIColors.YELLOW + " story +filter description regex sci.*fi" + ANSIColors.RESET);
                System.out.println(librarian + ": For example, searching for stories about Twilight Sparkle and Vinyl Scratch (DJ P0N-3)" +
                        " and Princess Luna, but without princess celestia, with descriptions containing the word 'pony', ordered by rating is as simple as saying :");
                System.out.println(librarian + ": " + ANSIColors.RED + "search" + ANSIColors.YELLOW +
                        " story +filter twilight sparkle, dj p0n-3, princess luna, !princess celestia, description pony +order rating" + ANSIColors.RESET);
            }
            case "list" -> {
                System.out.println(librarian + ": Here's a comprehensive list of all possible attributes for easy searching !");
                System.out.println(librarian + ": (Author) Ordering : stories, followers");
                System.out.println(librarian + ": (Author) Filtering : biography, name");
                System.out.println(librarian + ": (Story) Ordering : likes, dislikes, words, rating, views, newest, comments, chapters ");
                System.out.println(librarian + ": (Story) Filtering : author:authorid, description, title, or any tags. To display all tags, ask me for " + ANSIColors.RED + "tags" + ANSIColors.RESET + ".");
                System.out.println(librarian + ": You may search for a tag's existence by typing " + ANSIColors.RED + "tag" + ANSIColors.RESET + " [tag to verify]");
            }
            case "tags" -> {
                System.out.println(librarian + ": Current tags :");
                System.out.println(Arrays.toString(tagfilters.keySet().stream().sorted().toArray()));

            }
            default -> System.out.println(librarian + ": " + Dialogue.confused + " Did you need some " + ANSIColors.RED + "help" + ANSIColors.RESET + "?" +
                    " Just say the word !");
        }
        return true;
    }

    public static Path data = Paths.get("data");
    public static Path filters = data.resolve("filters");
    public static Path ordering = data.resolve("ordering");
    public static Path metadata = data.resolve("metadata");

    static long totalwords = 0;
    static long totalstories = 0;

    //Story orders
    public static ConcurrentHashMap<Integer, Integer> ratings = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<Integer, Integer> words = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<Integer, Integer> likes = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<Integer, Integer> dislikes = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<Integer, Integer> views = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<Integer, Integer> newest = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<Integer, Integer> chapters = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<Integer, Integer> comments = new ConcurrentHashMap<>();

    //Author orders
    public static ConcurrentHashMap<Integer, Integer> followers = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<Integer, Integer> stories = new ConcurrentHashMap<>();

    //Story filters and tag filters
    public static ConcurrentHashMap<Integer, String> description = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<Integer, String> title = new ConcurrentHashMap<>();

    //Author filters
    public static ConcurrentHashMap<Integer, String> biography = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<Integer, String> name = new ConcurrentHashMap<>();

    //Tag (uses JSON)
    public static ConcurrentHashMap<String, List<Integer>> tagfilters = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<Integer, List<Integer>> storylist = new ConcurrentHashMap<>();

    static boolean recompile() throws IOException, ParseException {
        //Reset for multiple recompilings
        ratings.clear();
        words.clear();
        likes.clear();
        dislikes.clear();
        views.clear();
        newest.clear();
        chapters.clear();
        comments.clear();
        followers.clear();
        stories.clear();
        description.clear();
        title.clear();
        biography.clear();
        name.clear();
        tagfilters.clear();
        storylist.clear();

        totalwords = 0;
        totalstories = 0;
        start = System.currentTimeMillis();
        Path archive = Paths.get("index.json");
        if(!Files.exists(archive)) {
            System.out.println(librarian + ": I can't seem to find the " + ANSIColors.RED + "index.json" + ANSIColors.RESET
            + " file ! Make sure we're both in the same folder, please.");
            return false;
        }

        System.out.println(librarian + ": Clearing out old files...");
        FileUtils.deleteDirectory(data.toFile());
        Files.createDirectory(data);
        Files.createDirectory(filters);
        Files.createDirectory(ordering);
        Files.createDirectory(metadata);

        BufferedReader br = Files.newBufferedReader(archive);
        System.out.println(librarian + ": Processing stories...");
        br.lines().parallel().forEach(line -> {
            try {
                handle(line);
            } catch (IOException | ParseException e) {
                System.out.println(ANSIColors.RED + line + ANSIColors.RESET);
                System.out.println(ANSIColors.BRIGHT_RED);
                e.printStackTrace();
                System.out.println(librarian + ": Critical error parsing data ! Make sure to have Fimfarchive properly decompressed, failing that please contact "
                + ANSIColors.CYAN + "Princess Analyzers" + ANSIColors.RESET + " over on discord ! (analyzers)");
            }
            totalstories++;
            if (totalstories %20000 == 0) {
                System.out.println(librarian + ": " + totalstories + " stories processed so far !");
            }
        });

        //Committing everything to files
        //Note that filename is the same as the keyword used ! Invalid keywords lead to non-existent files, so add a method to list those.
        save();
        return true;
    }

    static void handle(String story) throws IOException, ParseException {
        if (story.length() == 1) {
            return; //Remove leading and trailing brackets
        }
        String json = story.split(":", 2)[1];

        JSONObject info = new JSONObject(json);

        JSONObject author = info.getJSONObject("author");
        String authorname = author.optString("name", "Unnamed Author");
        String authorbio = author.optString("bio_html", "");
        int followercount = Math.abs(author.optInt("num_followers", 0));
        int storycount = Math.abs(author.optInt("num_stories", 0));

        JSONArray tags = info.getJSONArray("tags");
        ArrayList<String> storyTags = new ArrayList<>();
        for (Object tag : tags) {
            storyTags.add(((JSONObject) tag).getString("name").toLowerCase(Locale.ROOT));
        }

        int id = info.getInt("id");
        int authorid = author.getInt("id");

        List<Integer> storiesWritten;
        if (storylist.containsKey(authorid)) {
            storiesWritten = storylist.get(authorid);
        } else {
            storiesWritten = Collections.synchronizedList(new ArrayList<>());
        }
        storiesWritten.add(id);
        storylist.put(authorid, storiesWritten);

        String storytitle = info.optString("title", "(Null title) ID " + id);
        String url = info.optString("url", "No URL");
        String completion = info.optString("completion_status", "Unmarked");
        String content_rating = info.optString("content_rating", "Unrated");

        storyTags.add(completion);
        storyTags.add(content_rating);

        String date_published;
        if (info.isNull("date_published")) {
            if (!info.isNull("date_updated")) {
                date_published = info.getString("date_updated");
            } else {
                date_published = info.getString("date_modified");
            }
        } else {
            date_published = info.getString("date_published");
        }
        date_published = date_published.substring(0, 10);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date date = sdf.parse(date_published);
        long time_published = date.getTime();
        String short_desc = info.optString("short_description", "");
        String long_desc = info.optString("description_html", "");

        int chaptercount = Math.abs(info.optInt("num_chapters", 0));
        int commentcount = Math.abs(info.optInt("num_comments", 0));
        int dislikecount = Math.abs(info.optInt("num_dislikes", 0));
        int likecount = Math.abs(info.optInt("num_likes", 0));
        int viewcount = Math.abs(info.optInt("num_views", 0));
        int wordcount = Math.abs(info.optInt("num_words", 0));
        totalwords += wordcount;
        int rating = Math.abs(info.optInt("rating", 0));

        //Creating associated metadata for each story
        Path storyMetadata = metadata.resolve(String.valueOf(id));
        Files.createFile(storyMetadata);
        JSONObject metadata = new JSONObject();
        metadata.put("title", storytitle);
        metadata.put("author", authorname);
        metadata.put("url", url);
        metadata.put("completion", completion);
        metadata.put("contentrating", content_rating);
        metadata.put("datepublished", date_published);
        metadata.put("timepublished", time_published);
        metadata.put("chaptercount", chaptercount);
        metadata.put("commentcount",  commentcount);
        metadata.put("dislikes", dislikecount);
        metadata.put("likes", likecount);
        metadata.put("views", viewcount);
        metadata.put("words", wordcount);
        metadata.put("rating", rating);
        metadata.put("shortdesc", short_desc);
        metadata.put("longdesc", long_desc);
        Files.writeString(storyMetadata, metadata.toString());

        ratings.put(id, rating);
        words.put(id, wordcount);
        likes.put(id, likecount);
        dislikes.put(id, dislikecount);
        views.put(id, viewcount);
        newest.put(id, (int) time_published);
        chapters.put(id, chaptercount);
        comments.put(id, commentcount);

        if (!followers.containsValue(authorid)) {
            followers.put(authorid, followercount);
        }

        if (!stories.containsValue(authorid)) {
            stories.put(authorid, storycount);
        }

        String combinedDesc = short_desc.toLowerCase() + " " + long_desc.toLowerCase();
        if (short_desc.isEmpty() && long_desc.isEmpty()) {
            description.put(id, "No description RND" + new Random().nextInt(10000));
        } else if (description.containsValue(combinedDesc)) {
            System.out.println(ANSIColors.YELLOW + "Duplicate story description for story " + id
                    + ", modifying.");
            description.put(id, combinedDesc + " [Duplicate RND" + new Random().nextInt(100) + "]");
        } else {
            description.put(id, combinedDesc); //TODO do same thing with author and description
        }
        title.put(id, storytitle.toLowerCase()); //TODO PLEASE change the search-by-filters to feature ID first !!!

        if (!biography.containsKey(authorid)) {
            if (authorbio.isEmpty()) {
                biography.put(authorid, "No biography RND" + new Random().nextInt(10000));
            } else if (biography.containsValue(authorbio.toLowerCase())) {
                System.out.println(ANSIColors.YELLOW + "Duplicate author biography for user " + authorid
                + ", modifying.");
                biography.put(authorid, authorbio.toLowerCase() + " [Duplicate RND" + new Random().nextInt(100) + "]");
            } else {
                biography.put(authorid, authorbio.toLowerCase());
            }
        }

        if (!name.containsKey(authorid)) {
            if (name.containsValue(authorbio.toLowerCase())) {
                System.out.println(ANSIColors.YELLOW + "Duplicate author name for user " + authorid
                        + ", modifying.");
                name.put(authorid, authorname.toLowerCase() + " [Duplicate RND" + new Random().nextInt(100) + "]");
            } else {
                name.put(authorid, authorname.toLowerCase());
            }

        }
        for (String tag : storyTags) {
            if (!tagfilters.containsKey(tag)) {
                tagfilters.put(tag, Collections.synchronizedList(new ArrayList<>()));
            }
            List<Integer> storiesWithTag = tagfilters.get(tag);
            storiesWithTag.add(id);
        }
    }

    public static HashMap<String, ConcurrentHashMap> nameToHolder;
    static {
        nameToHolder = new HashMap<>();
        nameToHolder.put("rating", ratings);
        nameToHolder.put("words", words);
        nameToHolder.put("likes", likes);
        nameToHolder.put("dislikes", dislikes);
        nameToHolder.put("views", views);
        nameToHolder.put("newest", newest);
        nameToHolder.put("chapters", chapters);
        nameToHolder.put("comments", comments);
        nameToHolder.put("followers", followers);
        nameToHolder.put("stories", stories);
        nameToHolder.put("description", description);
        nameToHolder.put("title", title);
        nameToHolder.put("biography", biography);
        nameToHolder.put("name", name);
        nameToHolder.put("tags", tagfilters);
        nameToHolder.put("storylist", storylist);
    }

    static void save() throws IOException {
        for (String key : nameToHolder.keySet()) {
            Path path = ordering.resolve(key);
            if ("description tags title biography name".contains(key)) { //Hardcoded ! WARNING
                path = filters.resolve(key);
            }
            Files.createFile(path);
            try (FileOutputStream fileOutputStream = new FileOutputStream(path.toFile());
                 ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream)) {
                objectOutputStream.writeObject(nameToHolder.get(key));
                objectOutputStream.flush();
            }
        }
    }

    static void load() {
        nameToHolder.keySet().stream().parallel().forEach(key -> {
            Path path = ordering.resolve(key);
            if (!Files.exists(path)) {
                path = filters.resolve(key);
            }
            try (FileInputStream fileInputStream = new FileInputStream(path.toFile());
                 ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream)) {
                AbstractMap container = nameToHolder.get(key);
                container.putAll((AbstractMap) objectInputStream.readObject());
            } catch (ClassNotFoundException | IOException e) {
                System.out.println(ANSIColors.RED + "Error: cannot retrieve file " + path + ANSIColors.RESET);
                System.out.println(librarian + ": Critical error retrieving files ! Make sure to have Fimfarchive properly decompressed, failing that please contact "
                        + ANSIColors.CYAN + "Princess Analyzers" + ANSIColors.RESET + " over on discord ! (analyzers)");
            }
        });

    }
}