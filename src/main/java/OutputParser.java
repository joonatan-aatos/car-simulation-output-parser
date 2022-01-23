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
            "Suurin odotusaika (min)",
            "Montako autoa joutui odottamaan",
            "Montako autoa joutui odottamaan yli tunnin",
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
        ArrayList<File> carStatisticsToBeParsed = new ArrayList<>();
        assert listOfFiles != null;
        for (File file : listOfFiles) {
            if (file.getName().matches("r[0-9]+-c[0-9]+-s[0-9]+-statistics.csv"))
                statisticsToBeParsed.add(file);
            else if (file.getName().matches("r[0-9]+-c[0-9]+-s[0-9]+-car_statistics.csv"))
                carStatisticsToBeParsed.add(file);
        }
        listOfFiles = null;
        for (File file : statisticsToBeParsed) {
            List<String> content = Files.readAllLines(file.toPath());
            int carCount = Integer.parseInt(file.getName().replaceAll("r[0-9]+-c", "").replaceAll("-s[0-9]+-statistics.csv", ""));
            HashMap<String, ArrayList<Double>> repeatData = data.get(carCount);
            if (repeatData == null) {
                repeatData = new HashMap<>();
                for (String column : columns) {
                    repeatData.put(column, new ArrayList<>());
                }
                data.put(carCount, repeatData);
            }
            repeatData.get(columns[0]).add((double) carCount);
            int totalTime = Integer.parseInt(content.get(2).split(";")[1]);
            repeatData.get(columns[1]).add((double) totalTime);

            double drivingSum = 0;
            for (int i = 2; i < 7; i++) {
                drivingSum += Double.parseDouble(content.get(i+4).split(";")[1]);
            }
            repeatData.get(columns[2]).add(drivingSum);
            repeatData.get(columns[3]).add(Double.parseDouble(content.get(11).split(";")[1]));
            repeatData.get(columns[4]).add(Double.parseDouble(content.get(12).split(";")[1]));
            repeatData.get(columns[5]).add(Double.parseDouble(content.get(6).split(";")[3]));
            repeatData.get(columns[6]).add(Double.parseDouble(content.get(11).split(";")[3]));
            repeatData.get(columns[7]).add(Double.parseDouble(content.get(12).split(";")[3]));

            double maxWaitingCOunt = 0;
            for (int i = 28; i < content.size()-1; i++) {
                double currentWaitingCount = Double.parseDouble(content.get(i).split(";")[6]);
                if (currentWaitingCount > maxWaitingCOunt)
                    maxWaitingCOunt = currentWaitingCount;
            }
            repeatData.get(columns[12]).add(maxWaitingCOunt);

            double[] maxWaitingCountOnRoads = new double[] { 0, 0, 0, 0, 0, 0 };
            for (int i = 28; i < content.size()-1; i++) {
                for (int j = 0; j < 6; j++) {
                    double currentWaitingCount = Double.parseDouble(content.get(i).split(";")[18 + j]);
                    if (currentWaitingCount > maxWaitingCountOnRoads[j]) {
                        maxWaitingCountOnRoads[j] = currentWaitingCount;
                    }
                }
            }
            for (int i = 0; i < 6; i++) {
                repeatData.get(columns[13+i]).add(maxWaitingCountOnRoads[i]);
            }
        }

        for (File file : carStatisticsToBeParsed) {
            List<String> content = Files.readAllLines(file.toPath());
            int carCount = Integer.parseInt(file.getName().replaceAll("r[0-9]+-c", "").replaceAll("-s[0-9]+-car_statistics.csv", ""));
            HashMap<String, ArrayList<Double>> repeatData = data.get(carCount);

            double largestWaitingTime = 0;
            int[] carsWaiting = new int[] { 0, 0, 0 };
            double[] carsWaitingThresholds = new double[] { 0, 1, 4 };
            for (int i = 1; i < content.size()-1; i++) {
                double waitingTime = Double.parseDouble(content.get(i).split(";")[7]);
                if (waitingTime > largestWaitingTime)
                    largestWaitingTime = waitingTime;
                for (int j = 0; j < carsWaitingThresholds.length; j++) {
                    if (waitingTime > carsWaitingThresholds[j]) {
                        carsWaiting[j]++;
                    }
                }
            }
            repeatData.get(columns[8]).add(largestWaitingTime);
            for (int i = 0; i < carsWaiting.length; i++) {
                repeatData.get(columns[9+i]).add((double) carsWaiting[i]);
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
                s.append(value.isPresent() ? value.getAsDouble() : -1).append(";");
            }
            s.append("\n");
        }
        PrintWriter printWriter = new PrintWriter("output.csv");
        printWriter.println(s.toString());
        printWriter.close();
    }
}
