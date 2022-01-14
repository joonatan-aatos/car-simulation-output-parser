import java.io.*;
import java.nio.file.Files;
import java.util.*;

public class OutputParser {

    private static final String[] columns = new String[] {
            "Autot",
            "Kulunut aika (s)",
            "Valtatiellä",
            "Matkalla valtatielle",
            "Matkalla valtatieltä",
            "Matkalla laturille",
            "Matkalla laturilta",
            "Latautumassa",
            "Odottamassa",
            "Valtatiellä (mediaani)",
            "Matkalla valtatielle (mediaani)",
            "Matkalla valtatieltä (mediaani)",
            "Matkalla laturille (mediaani)",
            "Matkalla laturilta (mediaani)",
            "Latautumassa (mediaani)",
            "Odottamassa (mediaani)"
    };
    private static final HashMap<Integer, HashMap<String, ArrayList<Double>>> data = new HashMap<>();

    public static void main(String[] args) throws IOException {
        File folder = new File("../sähköautosimulaatio/output/");
        File[] listOfFiles = folder.listFiles();
        ArrayList<File> filesToBeParsed = new ArrayList<>();
        assert listOfFiles != null;
        for (File file : listOfFiles) {
            if (file.getName().matches("r[0-9]+-c[0-9]+-statistics.csv"))
                filesToBeParsed.add(file);
        }
        for (File file : filesToBeParsed) {
            List<String> content = Files.readAllLines(file.toPath());
            int carCount = Integer.parseInt(content.get(0).split(";")[1]);
            HashMap<String, ArrayList<Double>> repeatData = data.get(carCount);
            if (repeatData == null) {
                repeatData = new HashMap<>();
                for (String column : columns) {
                    repeatData.put(column, new ArrayList<>());
                }
                data.put(carCount, repeatData);
            }
            repeatData.get(columns[0]).add((double) carCount);
            int totalTime = Integer.parseInt(content.get(1).split(";")[1]);
            repeatData.get(columns[1]).add((double) totalTime);

            for (int i = 2; i < 9; i++) {
                repeatData.get(columns[i]).add(Double.parseDouble(content.get(5).split(";")[1]));
            }
            for (int i = 9; i < 16; i++) {
                repeatData.get(columns[i]).add(Double.parseDouble(content.get(5).split(";")[3]));
            }
        }
        StringBuilder s = new StringBuilder();
        for (String column : columns) {
            s.append(column).append(";");
        }
        s.append("\n");
        ArrayList<Integer> carCounts = new ArrayList<>(data.keySet());
        Collections.sort(carCounts);
        for (int carCount : carCounts) {
            HashMap<String, ArrayList<Double>> repeatData = data.get(carCount);
            for (String column : columns) {
                ArrayList<Double> values = repeatData.get(column);
                OptionalDouble value = values.stream().mapToDouble(a -> a).average();
                s.append(value.isPresent() ? value.getAsDouble() : 0).append(";");
            }
            s.append("\n");
        }
        PrintWriter printWriter = new PrintWriter("output.csv");
        printWriter.println(s.toString());
        printWriter.close();
    }
}
