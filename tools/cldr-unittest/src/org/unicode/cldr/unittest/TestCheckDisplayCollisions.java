package org.unicode.cldr.unittest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.test.CheckCLDR.CheckStatus;
import org.unicode.cldr.test.CheckCLDR.Options;
import org.unicode.cldr.test.CheckDisplayCollisions;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.SimpleXMLSource;
import org.unicode.cldr.util.XMLSource;

import com.google.common.collect.ImmutableList;

public class TestCheckDisplayCollisions extends TestFmwkPlus {
    private static final String ukRegion = "//ldml/localeDisplayNames/territories/territory[@type=\"GB\"]";
    private static final String englandSubdivision = "//ldml/localeDisplayNames/subdivisions/subdivision[@type=\"gbeng\"]";
    private static final String scorpioEmoji = "//ldml/annotations/annotation[@cp=\"♏\"][@type=\"tts\"]";
    private static final String scorpionEmoji = "//ldml/annotations/annotation[@cp=\"🦂\"][@type=\"tts\"]";
    private static final String japanRegion = "//ldml/localeDisplayNames/territories/territory[@type=\"JP\"]";
    private static final String japanMap = "//ldml/annotations/annotation[@cp=\"🗾\"][@type=\"tts\"]";

    public static void main(String[] args) {
        new TestCheckDisplayCollisions().run(args);
    }

    public void testInheritance() {
        XMLSource rootSource = new SimpleXMLSource("root");
        CLDRFile root = new CLDRFile(rootSource);

        XMLSource enSource = new SimpleXMLSource("en");
        CLDRFile en = new CLDRFile(enSource);

        XMLSource frSource = new SimpleXMLSource("fr");
        frSource.putValueAtPath(scorpionEmoji, "scorpion");
        frSource.putValueAtPath(scorpioEmoji, "scorpion zodiac");
        frSource.putValueAtPath(englandSubdivision, "Angleterre");
        frSource.putValueAtPath(ukRegion, "Royaume-Uni");
        frSource.putValueAtDPath(japanRegion, "Japon");
        frSource.putValueAtDPath(japanMap, "carte du Japon");
        CLDRFile fr = new CLDRFile(frSource);

        XMLSource frCaSource = new SimpleXMLSource("fr_CA");
        frCaSource.putValueAtPath(scorpioEmoji, "scorpion");
        frCaSource.putValueAtPath(ukRegion, "Angleterre");
        frCaSource.putValueAtDPath(japanMap, "Japon");
        CLDRFile frCA = new CLDRFile(frCaSource);

        TestFactory factory = new TestFactory();
        factory.addFile(root);
        factory.addFile(en);
        factory.addFile(fr);
        factory.addFile(frCA);
        

        CheckDisplayCollisions cdc = new CheckDisplayCollisions(factory);
        cdc.setEnglishFile(CLDRConfig.getInstance().getEnglish());
        
        CLDRFile frResolved = factory.make("fr", true);
        checkFile(cdc, fr, frResolved);

        CLDRFile frCaResolved = factory.make("fr_CA", true);
        checkFile(cdc, frCA, frCaResolved, scorpionEmoji, scorpioEmoji, englandSubdivision, ukRegion, japanMap);
    }

    private void checkFile(CheckDisplayCollisions cdc, CLDRFile frCa, CLDRFile frCaResolved, String... expecteds) {
        Set<String> expected = new TreeSet<>(Arrays.asList(expecteds));
        List<CheckStatus> possibleErrors = new ArrayList<>();
        Options options = new Options();
        cdc.setCldrFileToCheck(frCa, options, possibleErrors);
        if (!possibleErrors.isEmpty()) {
            errln("init: " + possibleErrors);
            possibleErrors.clear();
        }
        Map<String,List<CheckStatus>> found = new HashMap<>();
        for (String path : frCaResolved) {
            String value = frCaResolved.getStringValue(path);
            //System.out.println(path + "\t" + value);
            if (path.equals(japanMap)) {
                int debug = 0;
            }
            cdc.check(path, path, value, options, possibleErrors);
            if (!possibleErrors.isEmpty()) {
                found.put(path, ImmutableList.copyOf(possibleErrors));
                possibleErrors.clear();
            }
        }
        for (Entry<String, List<CheckStatus>> entry : found.entrySet()) {
            String path = entry.getKey();
            if (expected.contains(path)) {
                expected.remove(path);
            } else {
                errln(frCa.getLocaleID() + " unexpected error: " + path + " : " + entry.getValue());
            }
        }
        assertEquals(frCa.getLocaleID() + " missing error: ", Collections.emptySet(), expected);
    }
}
