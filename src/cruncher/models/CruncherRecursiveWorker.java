package cruncher.models;

import javafx.concurrent.Task;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class CruncherRecursiveWorker extends Task<HashMap<String, Integer>> {

    private String data;
    private int start;
    private int end;
    private int arity;
    private HashMap<String, Integer> endResult;
    public CruncherRecursiveWorker(HashMap<String, Integer> endResult, String data, int start, int end, int arity) {
        this.data = data;
        this.start = start;
        this.end = end;
        this.arity = arity;
        this.endResult = endResult;
    }
    @Override
    public HashMap<String, Integer> call() throws Exception {
        String[] newData = data.substring(start, end).split(" ");
        StringBuilder bag;
        for (int i = 0; i < newData.length; i++) {
            bag = new StringBuilder();
            try {
                for (int j = i; j < i + arity && i + arity < newData.length + 1; j++) {
                    bag.append(newData[j].replaceAll("[.,!? ]", "")).append(" ");
                }
            } catch (IndexOutOfBoundsException ignore) {
            }
            String bagText;
            if (bag.length() > 0)
                bagText = bag.toString().substring(0, bag.length() - 1);
            else
                bagText = null;

            if (bagText != null) {
                List<String> myBag = Arrays.asList(bagText.split(" "));
                myBag.sort(Comparator.comparing(String::toString));
                endResult.merge(myBag.toString(), 1, Integer::sum);
            }
        }
        return endResult;
    }
}
