import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

public class ToolLoader {

    public static Map<String, Tool> loadTools(
            String fileName) throws Exception {

        Map<String, Tool> tools =
                new HashMap<>();

        BufferedReader reader =
                new BufferedReader(
                        new FileReader(fileName)
                );

        // Skip header
        reader.readLine();

        String line;

        while ((line = reader.readLine()) != null) {

            if (line.trim().isEmpty()) {
                continue;
            }

            String[] parts = line.split(",");

            String toolId =
                    parts[0].trim();

            double memFootprint =
                   Double.parseDouble(
                           parts[1].trim()
                    ) / 1024.0;

            double imagePullMs =
                    Double.parseDouble(
                            parts[2].trim()
                    );

            double runtimeInitMs =
                    Double.parseDouble(
                            parts[3].trim()
                    );

            double execDurationMs =
                    Double.parseDouble(
                            parts[4].trim()
                    );

            Tool tool =
                    new Tool(
                            toolId,
                            memFootprint,
                            imagePullMs,
                            runtimeInitMs,
                            execDurationMs
                    );

            tools.put(toolId, tool);
        }

        reader.close();

        return tools;
    }
}
