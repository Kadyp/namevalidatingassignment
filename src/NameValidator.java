import org.apache.commons.codec.language.Soundex;
import org.apache.commons.text.similarity.LevenshteinDistance;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NameValidator {
    private static final String LINE_SEPARATOR_KEY = "line.separator";
    private static final Soundex SOUNDEX = new Soundex();
    private static final int MAX_EDIT_DISTANCE = 2;
    private static final LevenshteinDistance LEVENSHTEIN_DISTANCE = new LevenshteinDistance(MAX_EDIT_DISTANCE);

    public void validate(
            final String personNameInput,
            final String blacklistPathInput,
            final String noisePathInput
    ) {
        Set<String> noiseWords = initializeNoiseContent(noisePathInput);
        Set<String> blacklistedNames = initializeBlacklistContent(blacklistPathInput);

        String nameWithoutNoise = removeNoiseFromName(personNameInput, noiseWords);

        checkIfNameForbidden(nameWithoutNoise, blacklistedNames);
    }

    private void checkIfNameForbidden(String personName, Set<String> blacklistedNames) {
        String regex = getIgnoreWordExpressionFrom(personName);
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        for (String blackListedName: blacklistedNames) {
            Matcher matcher = pattern.matcher(blackListedName);
            boolean exactMatch = matcher.matches(); // check if the line matches the input with any word order
            boolean phoneticMatch = false; // check if the line matches the input with similar sound
            boolean editDistanceMatch = false; // check if the line matches the input with similar spelling

            try {
                String regex2 = "[^a-zA-Z]";
                Pattern pattern2 = Pattern.compile(regex2);
                Matcher matcherName = pattern2.matcher(personName);
                Matcher matcherLine = pattern2.matcher(blackListedName);

                if (!matcherName.find() || !matcherLine.find()) {
                    phoneticMatch = SOUNDEX.soundex(personName).equals(SOUNDEX.soundex(blackListedName));
                }

            } catch (IllegalArgumentException e) {
                System.err.println("The character is not mapped by Soundex: " + e.getMessage());
            }

            editDistanceMatch = LEVENSHTEIN_DISTANCE.apply(personName, blackListedName) != -1; // check if the line matches the input with similar spelling

            if (exactMatch || phoneticMatch || editDistanceMatch) { // print the line if any of the conditions are true
                System.out.println(blackListedName);
            }
        }
    }

    public String getIgnoreWordExpressionFrom(String input) {
        String[] words = input.split(" ");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            sb.append("(?=.*\\b").append(words[i]).append("\\b)");
        }
        sb.append(".*$");
        return sb.toString();
    }

    private Set<String> initializeNoiseContent(String noisePath) {
        String sanitizedInput = sanitizeNoiseFile(noisePath);
        HashSet<String> stringSet = new HashSet<>();
        String noiseFileContent = getFileContent(sanitizedInput);
        for (String fileLine : noiseFileContent.split(System.getProperty(LINE_SEPARATOR_KEY))) {
            stringSet.add(fileLine);
        }
        return stringSet;
    }
    private Set<String> initializeBlacklistContent(String blacklistPath) {
        String sanitizedInput = sanitizeBlacklist(blacklistPath);
        HashSet<String> stringSet = new HashSet<>();
        String noiseFileContent = getFileContent(sanitizedInput);
        for (String fileLine : noiseFileContent.split(System.getProperty(LINE_SEPARATOR_KEY))) {
            stringSet.add(fileLine);
        }
        return stringSet;
    }

    private String removeNoiseFromName(String name, Set<String> noiseWords) {
        if(isEmpty(name)) {
            throw new IllegalArgumentException("Person name empty but required");
        }
        name = name.replace(",", "");
        String[] words = name.split(" ");

        StringBuilder stringBuilder = new StringBuilder();
        for (String word : words) {
            if (!noiseWords.contains(word)) {
                stringBuilder.append(word).append(" ");
            }
        }
        return stringBuilder.toString().trim().toLowerCase();
    }
    private String sanitizeBlacklist(String blackList) {
        if(isEmpty(blackList)) {
            throw new IllegalArgumentException("Blacklist path empty but required");
        }
        return blackList.trim();
    }
    private String sanitizeNoiseFile(String noiseFile) {
        if(isEmpty(noiseFile)) {
            throw new IllegalArgumentException("Noise file empty but required");
        }
        return noiseFile.trim();
    }

    private boolean isEmpty(String value){
        return value.trim().isEmpty();
    }

    private static String getFileContent(String filePath) {
        try {

            BufferedReader reader = new BufferedReader(new FileReader(filePath));
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
                stringBuilder.append(System.getProperty(LINE_SEPARATOR_KEY));
            }
            stringBuilder.deleteCharAt(stringBuilder.length() - 1);
            reader.close();
            return stringBuilder.toString();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
