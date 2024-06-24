package searchengine.dto.indexing;

import lombok.RequiredArgsConstructor;
import org.apache.lucene.morphology.LuceneMorphology;
import searchengine.repositories.DBConnection;

import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RequiredArgsConstructor
public class ParsingLemmas {
    private static String lineSeparator = System.lineSeparator();
    private static String unionWord = ".+СОЮЗ";
    private static String particleWord = ".+ЧАСТ";
    private static String prepositionWord = ".+ПРЕДЛ";
    private static String interjection = ".+МЕЖД";
    private final LuceneMorphology luceneMorphology;

    private StringBuilder insertQuery = new StringBuilder();
    private StringBuilder insertQueryIndex = new StringBuilder();

    public void splitTextIntoWords(String text, int siteId, int pageId){

        //HashMap<String, Integer> result = new HashMap<>();
        //ArrayList<String> lemmas = new ArrayList<String>();
        //Integer countMentions = 0;
        String regex = "[А-Яа-я]+";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(text);
        insertQuery = new StringBuilder();
        insertQueryIndex = new StringBuilder();
        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();
            String nextWord = text.substring(start, end).toLowerCase();
            if(!luceneMorphology.checkString(nextWord)
                || !isSeparateWord(nextWord))
            {
                continue;
            }
            String normalForm = getNormalForm(nextWord);
            countWords(normalForm, siteId, pageId);
            //lemmas.add(normalForm);

            //countMentions = result.get(normalForm);
            //result.put(normalForm, (countMentions == null ? 0 : countMentions) + 1);
        }
        try {
            DBConnection.executeMultiInsert(insertQuery, insertQueryIndex);
        }catch (SQLException e){
            e.printStackTrace();
        }

        //return result;
    }

    public HashMap<String, Integer> splitTextIntoWords1(String text){

        HashMap<String, Integer> result = new HashMap<>();
        //ArrayList<String> lemmas = new ArrayList<String>();
        Integer countMentions = 0;
        String regex = "[А-Яа-я]+";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(text);
        insertQuery = new StringBuilder();
        insertQueryIndex = new StringBuilder();
        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();
            String nextWord = text.substring(start, end).toLowerCase();
            if(!luceneMorphology.checkString(nextWord)
                    || !isSeparateWord(nextWord))
            {
                continue;
            }
            String normalForm = getNormalForm(nextWord);
            //countWords(normalForm, siteId, pageId);
            //lemmas.add(normalForm);

            countMentions = result.get(normalForm);
            result.put(normalForm, (countMentions == null ? 0 : countMentions) + 1);
        }
        //try {
            //DBConnection.executeMultiInsert(insertQuery, insertQueryIndex);
        //}catch (SQLException e){
            //e.printStackTrace();
        //}

        return result;
    }

    private void countWords(String word, int siteId, int pageId)
    {
        insertQuery.append((insertQuery.length() == 0 ? "" : " UNION ") +
                "(SELECT '" + siteId + "', '" + word + "', 1)");
        insertQueryIndex.append((insertQueryIndex.length() == 0 ? "" : " UNION ALL ") +
                " (SELECT id, '" + pageId + "', '1' FROM lemma WHERE lemma = '" + word + "')");
        /*insertQueryIndex.append("INSERT INTO index_table(lemma_id, page_id, rank_column)" +
                " (select id, '" + pageId + "', '1' from lemma where lemma = '" + word + "')"
                + " ON DUPLICATE KEY UPDATE `rank_column`=`rank_column` + 1;");*/

    }

    private Boolean isSeparateWord(String word)
    {
        List<String> wordBaseForms = luceneMorphology.getMorphInfo(word);
        for (String baseForm : wordBaseForms)
        {
            return !(baseForm.matches(unionWord) || baseForm.matches(particleWord)
                    || baseForm.matches(prepositionWord)
                    || baseForm.matches(interjection));
        }
        return false;
    }

    private String getNormalForm(String word)
    {
        List<String> wordBaseForms = luceneMorphology.getNormalForms(word);
        return String.valueOf(wordBaseForms);
    }

    public static String delTag(String html)
    {
        String htmlWithoutTag = "";
        String regEx = "((<script)|(<style))([\\s\\S]*?)((<\\/script>)|(<\\/style>))|(\\<(\\/?[^\\>]+)\\>)";
        htmlWithoutTag = html.replaceAll(regEx, "");

        return htmlWithoutTag;
    }

    public String getSniped(String text, String[] lemmas)
    {
        String sniped = "";
        int wordsBeforeAndAfter = 20 / (lemmas.length * 2);
        Stack<String> words = new Stack<>();
        TreeSet<String> lemmasSet = new TreeSet<>();
        lemmasSet.addAll(Arrays.stream(lemmas).toList());
        String[] lastWordsBeforeLemma = new String[wordsBeforeAndAfter];
        String result = "";
        String regex = "[А-Яа-я]+";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(text);
        int countWords = 0;
        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();
            String nextWord = text.substring(start, end).toLowerCase();
            if (!luceneMorphology.checkString(nextWord)
                    || !isSeparateWord(nextWord)) {
                words.add(nextWord);
                continue;
            }
            String normalForm = getNormalForm(nextWord);
            if (lemmasSet.contains(normalForm)){
                String word3 = words.size() > 0 ? words.pop() + " " : "";
                String word2 = words.size() > 0 ? words.pop() + " " : "";
                String word1 = "; " + (words.size() > 0 ? words.pop() + " " : "");
                sniped = sniped + word1 + word2 + word3 + "<b>" + nextWord + "</b>";
                words.add("");
                words.add("");
                words.add("");
                countWords = 3;
            } else if(countWords > 0){
                sniped = sniped + " " + nextWord;
                countWords--;
            } else {
                words.add(nextWord);
            }
        }
        return sniped;
    }

}
