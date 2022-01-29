import java.io.*;
import java.nio.file.Files;
import java.util.*;

public class OutputParser {

    private static final String[] columns = new String[] {
            "Autot",
            "Keskihajonta",
            "Kulunut aika (s)",
            "Lataustehon kerroin",
            "Ajamisen tehokkuuden kerroin",
            "Latureiden lukumäärän kerroin",
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
            "Montako autoa jonotti suurimmillaan samanaikaisesti (Ro-Ut)",
            "Kuinka monta autoa ajoi yli 200 km",
            "Kuinka monta yli 200 km ajaneista autoista joutui odottamaan",
            "Kuinka monta yli 200 km ajaneista autoista joutui odottamaan yli tunnin",
            "Kuinka monta yli 200 km ajaneista autoista joutui odottamaan yli 4 tuntia",
    };
    private static final HashMap<String, HashMap<String, ArrayList<Double>>> data = new HashMap<>();

    private static String getDataKey(int carCount, int standardDeviation, int chargingPowerCoefficient, int drivingEfficiencyCoefficient, int chargerCountCoefficient, boolean isWinter) {
        return Integer.toString(carCount) + Integer.toString(standardDeviation) + Integer.toString(chargingPowerCoefficient) + Integer.toString(drivingEfficiencyCoefficient) + Integer.toString(chargerCountCoefficient) + (isWinter ? "w" : "s");
    }

    public static void main(String[] args) throws IOException {
        File folder = new File("../ajo3-tulokset/");
        File[] listOfFiles = folder.listFiles();
        ArrayList<File> statisticsToBeParsed = new ArrayList<>();
        ArrayList<File> carStatisticsToBeParsed = new ArrayList<>();
        assert listOfFiles != null;

        long simulationStartTime = System.currentTimeMillis();

        System.out.println("Getting files...");
        for (File file : listOfFiles) {
            if (file.getName().matches("^r[0-9]+-c[0-9]+-s[0-9]+-p[0-9]+-e[0-9]+-a[0-9]+-[ws]-statistics.csv$"))
                statisticsToBeParsed.add(file);
            else if (file.getName().matches("^r[0-9]+-c[0-9]+-s[0-9]+-p[0-9]+-e[0-9]+-a[0-9]+-[ws]-car_statistics.csv"))
                carStatisticsToBeParsed.add(file);
        }
        final int filesToParseThrough = statisticsToBeParsed.size() + carStatisticsToBeParsed.size();
        int filesParsed = 0;
        listOfFiles = null;
        printState(0);
        for (File file : statisticsToBeParsed) {
            List<String> content = Files.readAllLines(file.toPath());
            String[] nameData = file.getName().replaceAll("-statistics\\.csv", "").split("((?!-s$)-[rcspea])|-");
            int carCount = Integer.parseInt(nameData[1]);
            int standardDeviation = Integer.parseInt(nameData[2]);
            int chargerPowerCoefficient = Integer.parseInt(nameData[3]);
            int drivingEfficiencyCoefficient = Integer.parseInt(nameData[4]);
            int chargerCountCoefficient = Integer.parseInt(nameData[5]);
            boolean winter = nameData[6].equals("w");
            String key = getDataKey(carCount, standardDeviation, chargerPowerCoefficient, drivingEfficiencyCoefficient, chargerCountCoefficient, winter);
            HashMap<String, ArrayList<Double>> repeatData = data.get(key);
            if (repeatData == null) {
                repeatData = new HashMap<>();
                for (String column : columns) {
                    repeatData.put(column, new ArrayList<>());
                }
                data.put(key, repeatData);
            }
            repeatData.get(columns[0]).add((double) carCount);
            int totalTime = Integer.parseInt(content.get(2).split(";")[1]);
            repeatData.get(columns[1]).add((double) standardDeviation);
            repeatData.get(columns[2]).add((double) totalTime);
            repeatData.get(columns[3]).add((double) chargerPowerCoefficient);
            repeatData.get(columns[4]).add((double) drivingEfficiencyCoefficient);
            repeatData.get(columns[5]).add((double) chargerCountCoefficient);

            double drivingSum = 0;
            for (int i = 2; i < 7; i++) {
                drivingSum += Double.parseDouble(content.get(i+4).split(";")[1]);
            }
            repeatData.get(columns[6]).add(drivingSum);
            repeatData.get(columns[7]).add(Double.parseDouble(content.get(11).split(";")[1]));
            repeatData.get(columns[8]).add(Double.parseDouble(content.get(12).split(";")[1]));
            repeatData.get(columns[9]).add(Double.parseDouble(content.get(6).split(";")[3]));
            repeatData.get(columns[10]).add(Double.parseDouble(content.get(11).split(";")[3]));
            repeatData.get(columns[11]).add(Double.parseDouble(content.get(12).split(";")[3]));

            double maxWaitingCOunt = 0;
            double[] maxWaitingCountOnRoads = new double[] { 0, 0, 0, 0, 0, 0 };
            for (int i = 28; i < content.size()-1; i++) {
                String[] line = content.get(i).split(";");
                double currentWaitingCount = Double.parseDouble(line[6]);
                if (currentWaitingCount > maxWaitingCOunt)
                    maxWaitingCOunt = currentWaitingCount;

                for (int j = 0; j < 6; j++) {
                    currentWaitingCount = Double.parseDouble(line[18 + j]);
                    if (currentWaitingCount > maxWaitingCountOnRoads[j]) {
                        maxWaitingCountOnRoads[j] = currentWaitingCount;
                    }
                }
            }
            repeatData.get(columns[16]).add(maxWaitingCOunt);
            for (int i = 0; i < 6; i++) {
                repeatData.get(columns[17+i]).add(maxWaitingCountOnRoads[i]);
            }

            printState(++filesParsed / (double) filesToParseThrough);
        }

        for (File file : carStatisticsToBeParsed) {
            List<String> content = Files.readAllLines(file.toPath());
            String[] nameData = file.getName().replaceAll("-car_statistics\\.csv", "").split("((?!-s$)-[rcspea])|-");
            int carCount = Integer.parseInt(nameData[1]);
            int standardDeviation = Integer.parseInt(nameData[2]);
            int chargerPowerCoefficient = Integer.parseInt(nameData[3]);
            int drivingEfficiencyCoefficient = Integer.parseInt(nameData[4]);
            int chargerCountCoefficient = Integer.parseInt(nameData[5]);
            boolean winter = nameData[6].equals("w");
            String key = getDataKey(carCount, standardDeviation, chargerPowerCoefficient, drivingEfficiencyCoefficient, chargerCountCoefficient, winter);
            HashMap<String, ArrayList<Double>> repeatData = data.get(key);

            double largestWaitingTime = 0;
            int[] carsWaiting = new int[] { 0, 0, 0 };
            int[] carsWithRouteOver200kmWaiting = new int[] { 0, 0, 0 };
            double[] carsWaitingThresholds = new double[] { 0, 60, 240 };
            int amountOfCarsWithRouteOver100km = 0;
            for (int i = 1; i < content.size()-1; i++) {
                String[] row = content.get(i).split(";");
                double waitingTime = Double.parseDouble(row[7]);
                if (waitingTime > largestWaitingTime)
                    largestWaitingTime = waitingTime;
                for (int j = 0; j < carsWaitingThresholds.length; j++) {
                    if (waitingTime > carsWaitingThresholds[j]) {
                        carsWaiting[j]++;
                    }
                }
                double routeLength = Double.parseDouble(row[12]);
                if (routeLength > 200) {
                    amountOfCarsWithRouteOver100km++;
                    for (int j = 0; j < carsWaitingThresholds.length; j++) {
                        if (waitingTime > carsWaitingThresholds[j]) {
                            carsWithRouteOver200kmWaiting[j]++;
                        }
                    }
                }
            }
            repeatData.get(columns[12]).add(largestWaitingTime);
            for (int i = 0; i < carsWaiting.length; i++) {
                repeatData.get(columns[13+i]).add((double) carsWaiting[i]);
            }
            repeatData.get(columns[23]).add((double) amountOfCarsWithRouteOver100km);
            for (int i = 0; i < carsWithRouteOver200kmWaiting.length; i++) {
                repeatData.get(columns[24+i]).add((double) carsWithRouteOver200kmWaiting[i]);
            }

            printState(++filesParsed / (double) filesToParseThrough);
        }

        StringBuilder s = new StringBuilder();
        for (String column : columns) {
            s.append(column).append(";");
        }
        s.append("\n");
        ArrayList<String> keys = new ArrayList<>(data.keySet());
        Collections.sort(keys);
        for (String key : keys) {
            HashMap<String, ArrayList<Double>> repeatData = data.get(key);
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

        long simulationEndTime = System.currentTimeMillis();

        long totalTime = simulationEndTime - simulationStartTime;

        System.out.printf("\nParsed %d files in %d minutes and %d seconds.\n", filesToParseThrough, (int) ((totalTime / (1000*60)) % 60), (int) (totalTime / 1000) % 60);
    }

    private static void printState(double progress) {
        int barLength = 50;
        StringBuilder s = new StringBuilder();
        s.append("\rProgress: [");
        for (int i = 0; i < barLength; i++) {
            if (i <= progress*barLength)
                s.append("|");
            else
                s.append(" ");
        }
        s.append(String.format("] %.1f%%   ", progress*100));

        System.out.print(s);
    }
}
