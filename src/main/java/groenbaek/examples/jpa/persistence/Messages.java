package groenbaek.examples.jpa.persistence;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Although static state should always be avoided, it is the easiest was to check what happens inside an EntityListener
 */
public class Messages {

    private static List<String> messages = Collections.synchronizedList(new ArrayList<>());

    public static void addMessage(String message) {
        messages.add(message);
    }

    public static List<String> getMessages() {
        return new ArrayList<>(messages);
    }

    public static int size() {
        return messages.size();
    }

}
