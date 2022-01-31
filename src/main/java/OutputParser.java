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
            "Kuinka monta autoa ajoi yli 400 km",
            "Kuinka monta yli 400 km ajaneista autoista joutui odottamaan",
            "Kuinka monta yli 400 km ajaneista autoista joutui odottamaan yli tunnin",
            "Kuinka monta yli 400 km ajaneista autoista joutui odottamaan yli 4 tuntia",
    };
    private static final HashMap<String, HashMap<String, ArrayList<Double>>> data = new HashMap<>();

    private static String getDataKey(int carCount, int standardDeviation, int chargingPowerCoefficient, int drivingEfficiencyCoefficient, int chargerCountCoefficient, boolean isWinter) {
        return Integer.toString(carCount) + Integer.toString(standardDeviation) + Integer.toString(chargingPowerCoefficient) + Integer.toString(drivingEfficiencyCoefficient) + Integer.toString(chargerCountCoefficient) + (isWinter ? "w" : "s");
    }

    public static void main(String[] args) throws IOException {
        File folder = new File("../saehkoeautosimulaatio/output/");
        File[] listOfFiles = folder.listFiles();
        ArrayList<File> statisticsToBeParsed = new ArrayList<>();
        ArrayList<File> carStatisticsToBeParsed = new ArrayList<>();
        ArrayList<File> carModelStatisticsToBeParsed = new ArrayList<>();
        assert listOfFiles != null;

        long simulationStartTime = System.currentTimeMillis();

        System.out.println("Getting files...");
        for (File file : listOfFiles) {
            if (file.getName().matches("^r[0-9]+-c[0-9]+-s[0-9]+-p[0-9]+-e[0-9]+-a[0-9]+-[ws]-statistics.csv$"))
                statisticsToBeParsed.add(file);
            else if (file.getName().matches("^r[0-9]+-c[0-9]+-s[0-9]+-p[0-9]+-e[0-9]+-a[0-9]+-[ws]-car_statistics.csv"))
                carStatisticsToBeParsed.add(file);
            else if (file.getName().matches("^r[0-9]+-c[0-9]+-s[0-9]+-p[0-9]+-e[0-9]+-a[0-9]+-[ws]-car_model_statistics.csv"))
                carModelStatisticsToBeParsed.add(file);
        }
        final int filesToParseThrough = statisticsToBeParsed.size() + carStatisticsToBeParsed.size() + carModelStatisticsToBeParsed.size();
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
            int[] carsWithRouteOver400kmWaiting = new int[] { 0, 0, 0 };
            double[] carsWaitingThresholds = new double[] { 0, 60, 240 };
            int amountOfCarsWithRouteOver200km = 0;
            int amountOfCarsWithRouteOver400km = 0;
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
                    amountOfCarsWithRouteOver200km++;
                    for (int j = 0; j < carsWaitingThresholds.length; j++) {
                        if (waitingTime > carsWaitingThresholds[j]) {
                            carsWithRouteOver200kmWaiting[j]++;
                        }
                    }
                }
                if (routeLength > 400) {
                    amountOfCarsWithRouteOver400km++;
                    for (int j = 0; j < carsWaitingThresholds.length; j++) {
                        if (waitingTime > carsWaitingThresholds[j]) {
                            carsWithRouteOver400kmWaiting[j]++;
                        }
                    }
                }
            }
            repeatData.get(columns[12]).add(largestWaitingTime);
            for (int i = 0; i < carsWaiting.length; i++) {
                repeatData.get(columns[13+i]).add((double) carsWaiting[i]);
            }
            repeatData.get(columns[23]).add((double) amountOfCarsWithRouteOver200km);
            for (int i = 0; i < carsWithRouteOver200kmWaiting.length; i++) {
                repeatData.get(columns[24+i]).add((double) carsWithRouteOver200kmWaiting[i]);
            }
            repeatData.get(columns[27]).add((double) amountOfCarsWithRouteOver400km);
            for (int i = 0; i < carsWithRouteOver400kmWaiting.length; i++) {
                repeatData.get(columns[28+i]).add((double) carsWithRouteOver400kmWaiting[i]);
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

        PrintWriter carModelStats = new PrintWriter("carModelOutput.csv");
        carModelStats.println(csvCarModelStatistics(filesParsed, filesToParseThrough, carModelStatisticsToBeParsed));
        carModelStats.close();

        long simulationEndTime = System.currentTimeMillis();

        long totalTime = simulationEndTime - simulationStartTime;


        System.out.printf("\nParsed %d files in %d minutes and %d seconds.\n", filesToParseThrough, (int) ((totalTime / (1000*60)) % 60), (int) (totalTime / 1000) % 60);
    }

    private static String csvCarModelStatistics(int filesParsed, int filesToParseThrough, ArrayList<File> carModelStatisticsToBeParsed) throws IOException {
        StringBuilder s = new StringBuilder();
        HashMap<String, double[]> carModelDataSums = new HashMap<>();
        for (File file : carModelStatisticsToBeParsed) {
            List<String> content = Files.readAllLines(file.toPath());
            for (int i = 1; i < content.size(); i++) {
                String[] values = content.get(i).split(";");
                if (values.length < 2) break;
                String carModel = values[0];
                if (Double.parseDouble(values[1])<=0) continue;
                if (carModelDataSums.containsKey(carModel)) {
                    carModelDataSums.get(carModel)[0]++;
                    carModelDataSums.get(carModel)[1] += Double.parseDouble(values[1]);
                    carModelDataSums.get(carModel)[2] += Double.parseDouble(values[2]);
                    carModelDataSums.get(carModel)[3] += Double.parseDouble(values[3]);
                    carModelDataSums.get(carModel)[4] += Double.parseDouble(values[4]);
                    carModelDataSums.get(carModel)[5] += Double.parseDouble(values[5]);
                    carModelDataSums.get(carModel)[6] += Double.parseDouble(values[6])/Double.parseDouble(values[9]);
                    carModelDataSums.get(carModel)[7] += Double.parseDouble(values[7])/Double.parseDouble(values[9]);
                    carModelDataSums.get(carModel)[8] += Double.parseDouble(values[8])/Double.parseDouble(values[9]);
                    carModelDataSums.get(carModel)[9] += Double.parseDouble(values[9]);
                    carModelDataSums.get(carModel)[10] += Double.parseDouble(values[10]);
                } else {
                    double[] list = new double[11];
                    list[0] = 1;
                    list[1] = Double.parseDouble(values[1]);
                    list[2] = Double.parseDouble(values[2]);
                    list[3] = Double.parseDouble(values[3]);
                    list[4] = Double.parseDouble(values[4]);
                    list[5] = Double.parseDouble(values[5]);
                    list[6] = Double.parseDouble(values[6])/Double.parseDouble(values[9]);
                    list[7] = Double.parseDouble(values[7])/Double.parseDouble(values[9]);
                    list[8] = Double.parseDouble(values[8])/Double.parseDouble(values[9]);
                    list[9] = Double.parseDouble(values[9]);
                    list[10] = Double.parseDouble(values[10]);
                    carModelDataSums.put(carModel, list);
                }
            }
            printState(++filesParsed / (double) filesToParseThrough);
        }
        s.append("Malli;Määrä;Akun koko (kWh);Max AC teho (kW);Max DC teho (kW);Ajotehokkuus (kWh/100km);Aika odottamassa (%);Aika valtatiellä (%);Aika laturilla (%);Kokonaisaika (min/auto);Latauskertojen määrä (Latauksien määrä/ 100km)\n");
        for (String key : carModelDataSums.keySet()) {
            s.append(key);
            double[] list = carModelDataSums.get(key);
            for (int i = 1; i < list.length; i++) {
                s.append(";"+list[i]/list[0]);
            }
            s.append("\n");
        }
        return s.toString();
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
