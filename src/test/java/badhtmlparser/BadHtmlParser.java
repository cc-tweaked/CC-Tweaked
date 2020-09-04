package badhtmlparser;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.Iterator;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterators;
import com.google.common.collect.Multimap;
import com.google.common.collect.PeekingIterator;

public class BadHtmlParser {
    private static final Multimap<String, String> IDS = HashMultimap.create();

    public static void main(String[] args) throws FileNotFoundException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(BadHtmlParser.class.getResourceAsStream("/tags.html")));
        PeekingIterator<String> iterator = Iterators.peekingIterator(reader.lines()
                                                                           .iterator());
        while (iterator.hasNext()) {
            String str = iterator.peek();
            try {
                int count = Integer.parseInt(str);
                iterator.next();
                write(iterator, count);
            } catch (NumberFormatException e) {
                write(iterator, 1);
            }
        }


        System.out.println(IDS);
        print("wooden_chests");
        print("gold_blocks");

        // todo replace dyes by hand
    }

    private static void print(String val) throws FileNotFoundException {
        FileOutputStream fos = new FileOutputStream("src/main/resources/data/c/tags/items/" + val + ".json");
        PrintStream stream = new PrintStream(fos);
        stream.println("{\"replace\": false,\"values\":[");
        Iterator<String> iterator = IDS.get("c:" + val)
                                       .iterator();
        while (iterator.hasNext()) {
            String s = iterator.next();
            stream.printf("\"%s\"%s", s, iterator.hasNext() ? ',' : "");
        }
        stream.println("]}");
    }

    private static void write(Iterator<String> str, int entries) {
        String tag = str.next();
        for (int i = 0; i < entries; i++) {
            IDS.put(tag, str.next());
        }
    }
}
