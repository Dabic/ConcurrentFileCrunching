package cruncher.models;

import fileInput.models.FileInputResult;
import javafx.concurrent.Task;

import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

public class CruncherWorker extends Task<String> {

    private int arity;
    private int limit;
    private String cruncherName;
    LinkedBlockingQueue<FileInputResult> inputQueue;
    LinkedBlockingQueue<HashMap<String, HashMap<String, Integer>>> outputQueue;
    public CruncherWorker(String cruncherName, int arity, int limit, LinkedBlockingQueue<HashMap<String, HashMap<String, Integer>>> outputQueue) {
        this.cruncherName = cruncherName;
        this.arity = arity;
        this.limit = limit;
        inputQueue = new LinkedBlockingQueue<>();
        this.outputQueue = outputQueue;

    }

    @Override
    protected String call() throws Exception {
        while (true) {
            try {
                FileInputResult fileInputResult = inputQueue.take();
                if (fileInputResult.isPoison()) {
                    succeeded();
                    System.out.println(cruncherName + " died");
                    break;
                }
                HashMap<String, Integer> endResult = null;
                try {
                    endResult = computeResult(fileInputResult.getFileData(), 0, fileInputResult.getFileData().length(), limit);
                } catch (OutOfMemoryError error) {
                    cancelled();
                }
                HashMap<String, HashMap<String, Integer>> outputResult = new HashMap<>();
                outputResult.put(fileInputResult.getFileName()+"-arity"+arity, endResult);
                updateMessage("ended," + fileInputResult.getFileName() + "," + cruncherName);
                outputQueue.add(outputResult);
                outputResult = null;
                endResult = null;
                System.gc();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
    public HashMap<String, Integer> computeResult(String data, int start, int end, int limit) {
        HashMap<String, Integer> endResult = new HashMap<>();
        if (end - start < limit) {
            start = calculateBound(start, data, "start");
            end = calculateBound(end, data, "end");

            String[] newData = data.substring(start, end).split(" ");
            StringBuilder bag;
            for (int i = 0; i < newData.length; i++) {
                bag = new StringBuilder();
                try {
                    for (int j = i; j < i + arity && i + arity < newData.length + 1; j++) {
                        bag.append(newData[j].replaceAll("[.,!? ]", "")).append(" ");
                    }
                } catch (IndexOutOfBoundsException ignore) {}
                String bagText;
                if (bag.length() > 0)
                    bagText =  bag.toString().substring(0, bag.length()-1);
                else
                    bagText = null;

                if (bagText != null) {
                    List<String> myBag = Arrays.asList(bagText.split(" "));
                    myBag.sort(Comparator.comparing(String::toString));
                    endResult.merge(myBag.toString(), 1, Integer::sum);
                }
            }
        } else {
            int mid = start + ((end - start) / 2);
            HashMap<String, Integer> leftResult = computeResult(data, start, mid, limit);
            HashMap<String, Integer> rightResult = computeResult(data, mid, end, limit);

            leftResult.forEach((k, v) -> endResult.merge(k, v, Integer::sum));
            rightResult.forEach((k, v) -> endResult.merge(k, v, Integer::sum));
        }
        return endResult;
    }
    int calculateBound(int bound, String data, String type) {
        int toRight = bound;
        int toRightCounter = 0;

        int toLeft = bound;
        int toLeftCounter = 0;

        if (bound == 0 || bound == data.length())
            return bound;

        while (toLeft > 0 && data.charAt(toLeft) != ' ') {
            toLeft--;
            toLeftCounter++;
        }
        while (toRight < data.length() && data.charAt(toRight) != ' ') {
            toRight++;
            toRightCounter++;
        }
        if (toRight == data.length()) {
            return toRight;
        } else if (toLeft == 0){
            return toLeft;
        } else {
            if (type.equals("end")) {
                //toRightCounter > toLeftCounter ? toLeft: toRight
                return toRight;
            } else {
                int result = toRightCounter > toLeftCounter ? toLeft: toRight;
                return toRight+1;
            }
        }
    }

    public int getArity() {
        return arity;
    }

    public String getCruncherName() {
        return cruncherName;
    }

    public LinkedBlockingQueue<FileInputResult> getInputQueue() {
        return inputQueue;
    }
}
