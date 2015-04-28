package nxt.util;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.util.Version;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

public final class Search {

    private static final Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_30);

    public static String[] parseTags(String tags, int minTagLength, int maxTagLength, int maxTagCount) {
        if (tags.trim().length() == 0) {
            return Convert.EMPTY_STRING;
        }
        List<String> list = new ArrayList<>();
        try (TokenStream stream = analyzer.tokenStream(null, new StringReader(tags))) {
            CharTermAttribute attribute = stream.addAttribute(CharTermAttribute.class);
            String tag;
            while (stream.incrementToken() && list.size() < maxTagCount && (tag = attribute.toString()).length() <= maxTagLength && tag.length() >= minTagLength) {
                if (!list.contains(tag)) {
                    list.add(tag);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e.toString(), e);
        }
        return list.toArray(new String[list.size()]);
    }

    private Search() {}

}
