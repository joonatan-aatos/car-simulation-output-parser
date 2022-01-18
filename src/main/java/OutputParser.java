import java.io.*;
import java.nio.file.Files;
import java.util.*;

public class OutputParser {

    private static final String[] columns = new String[] {
            "Autot",
            "Kulunut aika (s)",
            "Ajamassa (keskiarvo)",
            "Odottamassa (keskiarvo)",
            "Latautumassa (keskiarvo)",
            "Valtatiellä (mediaani)",
            "Odottamassa (mediaani)",
            "Latautumassa (mediaani)",
            "Suurin odotusaika",
            "Montako autoa joutui odottamaan yli 30 minuuttia",
            "Montako autoa joutui odottamaan yli tunnin",
            "Montako autoa joutui odottamaan yli 2 tuntia",
            "Montako autoa joutui odottamaan yli 4 tuntia",
            "Montako autoa jonotti suurimmillaan samanaikaisesti",
            "Montako autoa jonotti suurimmillaan samanaikaisesti (He-La)",
            "Montako autoa jonotti suurimmillaan samanaikaisesti (La-Jy)",
            "Montako autoa jonotti suurimmillaan samanaikaisesti (Jy-Ou)",
            "Montako autoa jonotti suurimmillaan samanaikaisesti (Ou-Ke)",
            "Montako autoa jonotti suurimmillaan samanaikaisesti (Ke-Ro)",
            "Montako autoa jonotti suurimmillaan samanaikaisesti (Ro-Ut)"
    };
    private static final HashMap<Integer, HashMap<String, ArrayList<Double>>> data = new HashMap<>();

    public static void main(String[] args) throws IOException {
        File folder = new File("../sähköautosimulaatio/output/");
        File[] listOfFiles = folder.listFiles();
        ArrayList<File> statisticsToBeParsed = new ArrayList<>();
        assert listOfFiles != null;
        for (File file : listOfFiles) {
            if (file.getName().matches("r[0-9]+-c[0-9]+-statistics.csv"))
                statisticsToBeParsed.add(file);
        }
        listOfFiles = null;
        for (File file : statisticsToBeParsed) {
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

            double drivingSum = 0;
            for (int i = 2; i < 7; i++) {
                drivingSum += Double.parseDouble(content.get(i+3).split(";")[1]);
            }
            repeatData.get(columns[2]).add(drivingSum);
            repeatData.get(columns[3]).add(Double.parseDouble(content.get(10).split(";")[1]));
            repeatData.get(columns[4]).add(Double.parseDouble(content.get(11).split(";")[1]));
            repeatData.get(columns[5]).add(Double.parseDouble(content.get(5).split(";")[3]));
            repeatData.get(columns[6]).add(Double.parseDouble(content.get(10).split(";")[3]));
            repeatData.get(columns[7]).add(Double.parseDouble(content.get(11).split(";")[3]));

            double maxWaitingCOunt = 0;
            for (int i = 27; i < content.size()-1; i++) {
                double currentWaitingCount = Double.parseDouble(content.get(i).split(";")[6]);
                if (currentWaitingCount > maxWaitingCOunt)
                    maxWaitingCOunt = currentWaitingCount;
            }
            repeatData.get(columns[13]).add(maxWaitingCOunt);

            double[] maxWaitingCountOnRoads = new double[] { 0, 0, 0, 0, 0, 0 };
            for (int i = 27; i < content.size()-1; i++) {
                for (int j = 0; j < 6; j++) {
                    double currentWaitingCount = Double.parseDouble(content.get(i).split(";")[18 + j]);
                    if (currentWaitingCount > maxWaitingCountOnRoads[j]) {
                        maxWaitingCountOnRoads[j] = currentWaitingCount;
                    }
                }
            }

            for (int i = 0; i < 6; i++) {
                repeatData.get(columns[14 + i]).add(maxWaitingCountOnRoads[i]);
                System.out.println(maxWaitingCountOnRoads[i]);
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
