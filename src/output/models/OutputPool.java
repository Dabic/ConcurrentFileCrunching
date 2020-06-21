package output.models;

import javafx.application.Platform;
import javafx.concurrent.Task;
import observers.IPublisher;
import observers.ISubscriber;
import observers.notifications.OutputSingleResultNotification;
import observers.notifications.OutputSumResultsNotification;

import java.util.*;
import java.util.concurrent.*;

public class OutputPool implements IPublisher {
    private HashMap<String, HashMap<String, Integer>> results;
    private LinkedBlockingQueue<HashMap<String, HashMap<String, Integer>>> inputQueue;
    private Task<String> outputTask;
    private Thread outputThread;
    private ExecutorService outputPool;
    private List<ISubscriber> subscribers;

    public OutputPool(LinkedBlockingQueue<HashMap<String, HashMap<String, Integer>>> queue) {
        inputQueue = queue;
        results = new HashMap<>();
        outputPool = Executors.newSingleThreadExecutor();

        outputTask = new Task<String>() {
            @Override
            protected String call() throws Exception {
                while (true) {
                    try {
                        HashMap<String, HashMap<String, Integer>> resultMap = inputQueue.take();
                        for (Map.Entry<String, HashMap<String, Integer>> entry : resultMap.entrySet()) {
                            results.put(entry.getKey(), entry.getValue());
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        outputThread = new Thread(outputTask);
        outputThread.start();
    }

    public void getSingleResult(Task<SortedSet<Map.Entry<String, Integer>>> task, String resultName) {
        ((OutputSingleResultWorker) task).setMap(results.get(resultName));
        outputPool.execute(task);
        Thread helpThread = new Thread(new Task<String>() {
            @Override
            protected String call() throws Exception {
                SortedSet<Map.Entry<String, Integer>> set =  task.get();
                Platform.runLater(new Task<String>() {
                    @Override
                    protected String call() throws Exception {
                        notifySubscribers(new OutputSingleResultNotification(set));
                        return null;
                    }
                });
                return null;
            }
        });
        helpThread.start();
    }

    public void getResultsSum(String newResultName, Task<HashMap<String, Integer>> task, List<String> listResults) {
        List<Map.Entry<String, HashMap<String, Integer>>> resultsMap = new ArrayList<>();
        for (String res : listResults) {
            if (results.get(res) != null) {
                for (Map.Entry<String, HashMap<String, Integer>> entry : results.entrySet()) {
                    if (entry.getKey().equals(res)) {
                        resultsMap.add(entry);
                    }
                }
            } else {
                resultsMap.add(new Map.Entry<String, HashMap<String, Integer>>() {
                    @Override
                    public String getKey() {
                        return res;
                    }

                    @Override
                    public HashMap<String, Integer> getValue() {
                        return null;
                    }

                    @Override
                    public HashMap<String, Integer> setValue(HashMap<String, Integer> value) {
                        return null;
                    }
                });
            }
        }
        ((OutputSumResultsWorker)task).setMaps(resultsMap);
        ((OutputSumResultsWorker)task).setOutputPool(this);
        outputPool.execute(task);

        Thread help = new Thread(new Task<String>() {
            @Override
            protected String call() throws Exception {
                HashMap<String, Integer> resultSum =  task.get();
                results.put(newResultName, resultSum);
                Platform.runLater(new Task<String>() {
                    @Override
                    protected String call() throws Exception {
                        notifySubscribers(new OutputSumResultsNotification(newResultName));
                        return null;
                    }
                });
                return null;
            }
        });
        help.start();

    }

    public HashMap<String, HashMap<String, Integer>> getResults() {
        return results;
    }

    @Override
    public void addSubscriber(ISubscriber sub) {
        if(sub == null)
            return;
        if(this.subscribers ==null)
            this.subscribers = new ArrayList<>();
        if(this.subscribers.contains(sub))
            return;
        this.subscribers.add(sub);
    }

    @Override
    public void removeSubscriber(ISubscriber sub) {
        if(sub == null || this.subscribers == null || !this.subscribers.contains(sub))
            return;
        this.subscribers.remove(sub);
    }

    @Override
    public void notifySubscribers(Object notification) {
        if(notification == null || this.subscribers == null || this.subscribers.isEmpty())
            return;

        for(ISubscriber listener : subscribers){
            listener.update(notification);
        }
    }
}
