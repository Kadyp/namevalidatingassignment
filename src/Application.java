public class Application {

    public static void main(String[] args) {
        String personName = "bin laden, osama";
        String blacklistFilePath = "data/blacklist";
        String noiseFilePath = "data/noise_words.txt";

        NameValidator validator = new NameValidator();
        validator.validate(personName, blacklistFilePath, noiseFilePath);

    }
}
