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

            // tools.csv stores mem_mb in megabytes, but SystemState's
            // capacity (see Main.java) is in gigabytes — convert here so
            // canFit() compares like units. Without this, every tool
            // "needs" ~1000x more capacity than exists and nothing can
            // ever be warmed, regardless of policy.
            double memFootprintMb =
                    Double.parseDouble(
                            parts[1].trim()
                    );

            double memFootprint = memFootprintMb / 1024.0;

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
