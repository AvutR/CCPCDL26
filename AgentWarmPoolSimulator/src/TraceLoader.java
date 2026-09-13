import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TraceLoader {

    public static List<Request> loadTrace(String fileName)
            throws Exception {

        List<Request> requests = new ArrayList<>();

        BufferedReader reader =
                new BufferedReader(new FileReader(fileName));

        String line;

        // Skip header
        reader.readLine();

        while ((line = reader.readLine()) != null) {

            String[] parts = line.split(",");

            String tenantId = parts[0];

            int timestep =
                    Integer.parseInt(parts[1]);

            String trueTool = parts[2];

            String predictionText = parts[3];

            double weight =
                    Double.parseDouble(parts[4]);

            Map<String, Double> probabilities =
                    parsePrediction(predictionText);

            Prediction prediction =
                    new Prediction(probabilities);

            Request request =
                    new Request(
                            tenantId,
                            timestep,
                            trueTool,
                            prediction,
                            weight
                    );

            requests.add(request);
        }

        reader.close();

        return requests;
    }

    private static Map<String, Double> parsePrediction(
            String text) {

        Map<String, Double> result =
                new HashMap<>();

        String[] entries = text.split("\\|");

        for (String entry : entries) {

            String[] pair = entry.split(":");

            String tool = pair[0];

            double probability =
                    Double.parseDouble(pair[1]);

            result.put(tool, probability);
        }

        return result;
    }
}