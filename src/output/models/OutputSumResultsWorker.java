package output.models;

import javafx.concurrent.Task;

import java.util.*;

public class OutputSumResultsWorker extends Task<HashMap<String, Integer>> {

    private List<Map.Entry<String, HashMap<String, Integer>>> maps;
    private OutputPool outputPool;
    private HashMap<String, Integer> result;
    private int mapsDone = 0;
    private int mapsDoneMax = 0;
    private List<String> doneMaps;
    public OutputSumResultsWorker() {
        maps = new ArrayList<>();
        result = new HashMap<>();
        doneMaps = new ArrayList<>();
    }
    @Override
    protected HashMap<String, Integer> call() throws Exception {
        while (mapsDone != mapsDoneMax) {
            for (Map.Entry<String, HashMap<String, Integer>> entry : maps) {
                if (!entry.getKey().startsWith("*")) {
                    if (!doneMaps.contains(entry.getKey())) {
                        for (Map.Entry<String, Integer> subEntry : entry.getValue().entrySet()) {
                            result.merge(subEntry.getKey(), subEntry.getValue(), Integer::sum);
                        }
                        doneMaps.add(entry.getKey());
                        mapsDone++;
                        updateProgress(mapsDone, mapsDoneMax);
                    }
                } else {
                    if (outputPool.getResults().get(entry.getKey().substring(1)) != null) {
                        if (!doneMaps.contains(entry.getKey())) {
                            HashMap<String, Integer> subMap = outputPool.getResults().get(entry.getKey().substring(1));
                            for (Map.Entry<String, Integer> subEntry : subMap.entrySet()) {
                                result.merge(subEntry.getKey(), subEntry.getValue(), Integer::sum);
                            }
                            mapsDone++;
                            updateProgress(mapsDone, mapsDoneMax);
                            doneMaps.add(entry.getKey());
                        }
                    }
                }

            }
        }
        return result;
    }

    public void setMaps(List<Map.Entry<String, HashMap<String, Integer>>> maps) {
        this.maps = maps;
        this.mapsDoneMax = maps.size();
    }

    public void setOutputPool(OutputPool outputPool) {
        this.outputPool = outputPool;
    }
}
