import java.io.File;

public class OutputParser {

    public static void main(String[] args) {
        File folder = new File("../sähköautosimulaatio/output/");
        File[] listOfFiles = folder.listFiles();
        for (File file : listOfFiles) {
            System.out.println(file.getName());
        }
    }
}
