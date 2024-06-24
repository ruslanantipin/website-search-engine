package searchengine;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jboss.jandex.Main;
import searchengine.dto.indexing.ParsingLemmas;

import java.util.HashMap;
import java.util.List;

public class tests {
    public static void main(String[] args) {

        try {


           // LuceneMorphology luceneMorph = new RussianLuceneMorphology();

        //ParsingLemmas parsingLemmas = new ParsingLemmas(luceneMorph);

        //HashMap<String,Integer> test = parsingLemmas.splitTextIntoWords("Петя ах привет как дела ДеЛа петя, делишки");
        //test.forEach((k,v) -> System.out.println("key " + k + " val " + v));

            //if (luceneMorph.checkString("ах")) {
                //List<String> wordBaseForms = luceneMorph.getMorphInfo("ах");
                //wordBaseForms.forEach(System.out::println);
                //System.out.println(String.valueOf(wordBaseForms));
           // }
        } catch (Exception ex){
            ex.printStackTrace();
        }

    }



}
