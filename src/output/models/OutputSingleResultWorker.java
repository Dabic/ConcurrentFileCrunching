package output.models;

import javafx.concurrent.Task;

import java.util.*;

public class OutputSingleResultWorker extends Task<SortedSet<Map.Entry<String, Integer>>> {
    HashMap<String, Integer> map;
    public OutputSingleResultWorker() {
        this.map = new HashMap<>();
    }

    @Override
    protected SortedSet<Map.Entry<String, Integer>> call() throws Exception {
        return sortedMap(map);
    }

    public SortedSet<Map.Entry<String, Integer>> sortedMap(Map<String, Integer> map) {
        int i = 0;
        final double endR = map.size()*Math.log(map.size());
        int endRInt = ((int) endR);
        final int[] progress = new int[10];
        int progressLen = 0;
        int startProgress = endRInt/10;
        int currentProgress = 0;
        for (int prog = 1; prog < 11; prog++) {
            progress[progressLen++] = startProgress*prog;
        }
        SortedSet<Map.Entry<String, Integer>> sortedEntries = new TreeSet<Map.Entry<String, Integer>>(new Comparator<Map.Entry<String, Integer>>() {
            @Override
            public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
                int res = o2.getValue().compareTo(o1.getValue());
                return res != 0 ? res : 1;
            }
        });
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            i++;
            if (inProgress(currentProgress, ((int)(i*Math.log(i))), progress)) {
                currentProgress = currentProgress + 1;
                updateProgress(currentProgress, 10);
            }
            sortedEntries.add(entry);
        }
        return sortedEntries;
    }

    public boolean inProgress(int currentProgress, int prog, int[] progress) {
        if (progress[currentProgress] < prog)
            return true;
        return false;
    }
    public void setMap(HashMap<String, Integer> map) {
        this.map = map;
    }
}
