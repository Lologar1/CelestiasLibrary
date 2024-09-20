package me.analyzers.utilities;

import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static me.analyzers.Main.*;

public class MathUtils {
    public static Object getRandom(Object[] array) {
        int rnd = new Random().nextInt(array.length);
        return array[rnd];
    }

    public static ArrayList<String> advanceUntilModifier(ArrayList<String> tokens) {
        ArrayList<String> filters = new ArrayList<>();
        StringBuilder currentLexeme = new StringBuilder();
        while (!tokens.isEmpty() && !tokens.get(0).startsWith("+")) { //Assume starts without first modifier
            String first = tokens.remove(0);
            currentLexeme.append(first.replace(",", "")).append(" ");
            if (first.endsWith(",") || tokens.isEmpty() || tokens.get(0).startsWith("+")) {
                filters.add(currentLexeme.deleteCharAt(currentLexeme.length()-1).toString()); //Remove last space
                currentLexeme = new StringBuilder();
            }
        }
        return filters;
    }

    static ArrayList<Integer> matching = new ArrayList<>();

    public static void search(String command) throws IOException {
        matching = new ArrayList<>();
        ArrayList<String> tokens = new ArrayList<>(Arrays.stream(command.split(" ")).skip(1).toList()); //Skipping search token
        if (Collections.frequency(tokens, "+order") > 1) {
            System.out.println(librarian + ": You can only apply one type of ordering, silly !");
            return;
        }
        String searchTarget = tokens.remove(0);
        boolean author = false;
        if (searchTarget.equals("author")) {
            author = true;
        } else {
            if (!searchTarget.equals("story")) {
                System.out.println(librarian + ": You must specify a search target of either " + ANSIColors.YELLOW + "author" + ANSIColors.RESET
                + " or " + ANSIColors.YELLOW + "story" + ANSIColors.RESET + ".");
                return;
            }
        }
        //Example : search +filter twilight sparkle, description friendship, title twilight +order rating +filter likes>=100
        try {
            if (!handle(tokens)) {
                return;
            }
        } catch (Exception e) {
            System.out.println(librarian + ": Hey, you sure you spelled that correctly ? I think some syntax error's hiding in there somewhere.");
            return;
        }
        if (matching.isEmpty()) {
            System.out.println(librarian + ": No matching targets.");
            return;
        }
        LinkedHashSet<Integer> m = new LinkedHashSet<>(matching);
        matching.clear();
        matching.addAll(m);
        if (author) {
            System.out.println(librarian + ": Matching authors, in order (found " + matching.size() + ") :");
            for (int story : matching) {
                System.out.print(name.get(story) + " (" + story + ")" + (matching.indexOf(story)==matching.size()-1?"":", "));
            }
        } else {
            System.out.println(librarian + ": Matching stories, in order (found " + matching.size() + ") :");
            for (int story : matching) {
                JSONObject json = new JSONObject(Files.readString(metadata.resolve(String.valueOf(story))));
                System.out.print(json.getString("title") + " (" + story + ")" + (matching.indexOf(story)==matching.size()-1?"":", "));
            }
        }
        System.out.println();
    }

    public static boolean first = true;
    public static boolean handle(ArrayList<String> tokens) {
        String order = null;
        boolean reversed = false;

        while (!tokens.isEmpty()) {
            first = true;
            String modifierType = tokens.remove(0);
            ArrayList<String> content = advanceUntilModifier(tokens);
            if (modifierType.equals("+filter")) {
                for (String filter : content) {
                    boolean or = filter.charAt(0) == '|';
                    filter = filter.charAt(0) == '|' ? filter.substring(1) : filter;
                    boolean not = filter.charAt(0) == '!';
                    filter = filter.charAt(0) == '!' ? filter.substring(1) : filter;
                    if (or && not) {
                        System.out.println(librarian + ": can only have one set operation per filter !");
                        return false;
                    }
                    if (tagfilters.containsKey(filter) || filter.startsWith("author:")) {
                        //Is a tag, or an author filter
                        List<Integer> toAdd = tagfilters.get(filter);
                        if (filter.startsWith("author:")) {
                            int authorid = Integer.parseInt(filter.split(":")[1]);
                            toAdd = storylist.get(authorid);
                        }
                        if ((!not && first) || or) {
                            first = false;
                            matching.addAll(toAdd);
                        } else if (not) {
                            matching.removeAll(toAdd);
                        }else {
                            matching.retainAll(toAdd);}
                    } else {
                        //Is a word-filter. Search for file, if not found throw error
                        String[] s = filter.split(" ", 2);
                        if (s.length != 2) {
                            System.out.println(librarian + ": Please provide an adequate filter !");
                            return false;
                        }
                        String type = s[0];
                        Path filterings = filters.resolve(type);
                        if (!Files.exists(filterings)) {
                            System.out.println(librarian + ": There aren't any tags nor filters matching "
                                    + ANSIColors.YELLOW + type + ANSIColors.RESET + " ! if you need a refresher, simply ask for some "
                                    + ANSIColors.RED + "info" + ANSIColors.RESET + ".");
                            return false;
                        }

                        String match = s[1];
                        AbstractMap map = nameToHolder.get(type);
                        ArrayList<Integer> toIntersect = new ArrayList<>();
                        if (match.startsWith("regex ")) {
                            String regmatch = match.replace("regex ", "");
                            map.keySet().stream().filter(k -> ((String) map.get(k)).matches(regmatch)).forEach(k -> toIntersect.add((Integer) k) );
                        } else {
                            map.keySet().stream().filter(k -> ((String) map.get(k)).contains(match)).forEach(k -> toIntersect.add((Integer) k) );
                        }

                        if ((!not && first) || or) {
                            first = false;
                            matching.addAll(toIntersect);
                        } else if (not) {
                            matching.removeAll(toIntersect);
                        }else {
                            matching.retainAll(toIntersect);
                        }
                    } //TODO do the order-as-filter later !
                }
            } else if (modifierType.equals("+order")) {
                if (content.size() > 2 || content.isEmpty()) {
                    System.out.println(librarian + ": There can only be at most one order, along with the optional " +
                            ANSIColors.YELLOW + "reversed" + ANSIColors.RESET + " modifier.");
                    return false;
                }
                if (content.contains("reversed")) { reversed = true; content.remove("reversed"); }
                order = content.get(0); //This should be fine !
                if (!Files.exists(ordering.resolve(order))) {
                    System.out.println(librarian + ": The requested order " + ANSIColors.YELLOW + order + ANSIColors.RESET +
                            " doesn't seem to exist...");
                    return false;
                }
            } else {
                System.out.println(librarian + ": Syntax error, invalid modifier ! If you need a refresher, ask for some "
                + ANSIColors.RED + "info" + ANSIColors.RESET + ".");
                return false;
            }
        }
        if (order != null) {
            ConcurrentHashMap sortedByOrder = nameToHolder.get(order);
            if (reversed) {
                matching.sort(Comparator.comparingInt(t -> (int) sortedByOrder.get(t)));
            } else {
                matching.sort(Comparator.comparingInt(t -> (int) sortedByOrder.get(t)).reversed());
            }
        }
        return true;
    }

    public static void tag(String tag) {
        boolean exists = tagfilters.containsKey(tag);
        ArrayList<String> others = new ArrayList<>(tagfilters.keySet().stream().filter(t -> t.contains(tag)).toList());
        others.remove(tag);
        System.out.println(librarian + ": The tag " + ANSIColors.YELLOW + tag + ANSIColors.RESET + (exists ? " exists." : " doesn't exist."));
        if (!others.isEmpty()) {
            System.out.println(librarian + ": Here are some more tags also containing it : " + others);
        }
    }

    public static void fetch(String ids) throws IOException {
        int id = 0;
        try {
            id = Integer.parseInt(ids);
        } catch (NumberFormatException e) {
            System.out.println(librarian + ": Usage: " + ANSIColors.RED + "fetch" + ANSIColors.RESET + " [story id]");
            return;
        }

        Path storydata = metadata.resolve(String.valueOf(id));
        if (!Files.exists(storydata)) {
            System.out.println(librarian + ": I can't find the requested file in the archive... Make sure everything is properly " + ANSIColors.RED + "setup" + ANSIColors.RESET + " first !");
            return;
        }
        JSONObject storyJSON = new JSONObject(Files.readString(storydata));
        String author = storyJSON.getString("author").toLowerCase();
        String title = storyJSON.getString("title").toLowerCase().replace(" ", "_");
        String fetch = author.replace(" ", "_") + "-" + name.keySet().stream().filter(k -> name.get(k).equals(author)).findFirst().get();
        String adaptedname = title.replaceAll("[^a-zA-Z0-9_-]", "");
        Path p = Paths.get("epub").resolve(String.valueOf(fetch.charAt(0))).resolve(fetch).resolve(adaptedname + "-" + id + ".epub");
        if (!Files.exists(p)) {
            System.out.println(librarian + ": I'm sorry my little pony, I can't find the story " + adaptedname + " in the archive. The formatting spell might be acting up again. You might want to talk to "
            + ANSIColors.CYAN + "Princess Analyzers" + ANSIColors.RESET + " about that. (Discord : analyzers)");
            return;
        }
        Path to = Paths.get("output");
        if (!Files.exists(to)) {
            Files.createDirectory(to);
        }
        title += ".epub";
        Path target = to.resolve(title);
        if (Files.exists(target)) {
            Files.delete(target);
            return;
        }
        Files.copy(p, target);
        System.out.println(librarian + ": Saved your file under " + ANSIColors.GREEN + title + ANSIColors.RESET
                + " in the " + ANSIColors.YELLOW + "output" + ANSIColors.RESET + " directory !");
    }

    public static void view(String command) throws IOException {
        String[] params = command.split(" ");
        if (params.length < 3) {
            System.out.println(librarian + ": Syntax : view story/author (id)");
            return;
        }
        boolean author = params[1].equals("author");
        int id;
        try {
            id = Integer.parseInt(params[2]);
        } catch (Exception e) {
            System.out.println(librarian + ": Syntax : view story/author (id)");
            return;
        }


        if (author) {
            System.out.println(librarian + ": Searching for author ID " + id);
            boolean isPresent = name.containsKey(id);
            if (!isPresent) {
                System.out.println(librarian + ": I'm sorry my little pony, but that author doesn't exist !");
                return;
            }
            System.out.println("Fimfiction handle : " + name.get(id)); //TODO one day, add fimfiction url
            boolean bio = biography.containsKey(id);
            if (bio) {
                System.out.println("Biography : " + biography.get(id).replaceAll("</p>", " ").replaceAll("\\<[^>]*>", ""));
            }
            System.out.println("Followers : " + followers.get(id));
            System.out.println("Stories written : " + stories.get(id));
            StringBuilder storyList = new StringBuilder();
            storylist.get(id).forEach(s -> {
                try {
                    storyList.append(new JSONObject(Files.readString(metadata.resolve(s.toString())))
                            .getString("title")).append(" (").append(s).append("), ");
                } catch (IOException e) {
                    System.out.println("Error reading story ID metadata for story " + s);
                }
            });
            System.out.println("Story list : " + storyList.substring(0, storyList.length() - 2));
        } else {
            if (!params[1].equals("story")) {
                System.out.println(librarian + ": Syntax : view story/author (id)");
                return;
            }
            System.out.println(librarian + ": Searching for story ID " + id);
            if (!Files.exists(metadata.resolve(String.valueOf(id)))) {
                System.out.println(librarian + ": I'm sorry my little pony, but that story doesn't exist !");
                return;
            }
            JSONObject json = new JSONObject(Files.readString(metadata.resolve(String.valueOf(id))));
            System.out.println(json.getString("title") + ", by " + json.getString("author") + " (" + json.getInt("words") + " words, " + json.getInt("chaptercount") + " chapters)");
            System.out.println("Fimfiction URL : " + json.getString("url"));
            System.out.println("Rating : " + json.getInt("rating") + ", with " + json.getInt("likes") + " likes, and " + json.getInt("dislikes") + " dislikes.");
            System.out.println("Status : " + json.getString("completion"));
            System.out.println("Content rating : " + json.getString("contentrating"));
            System.out.println("Viewed " + json.getInt("views") + " times");
            System.out.println("Last updated : " + json.getString("datepublished"));
            System.out.println("Short description : " + json.getString("shortdesc").replaceAll("</p>", " ").replaceAll("\\<[^>]*>",""));
            System.out.println("Long description : " + json.getString("longdesc").replaceAll("</p>", " ").replaceAll("\\<[^>]*>",""));
        }
    }
}
