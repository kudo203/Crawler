import crawlercommons.robots.BaseRobotRules;

import java.io.IOException;
import java.net.URL;
import java.util.Comparator;
import java.util.Date;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

/**
 * Created by koosh on 25/6/17.
 */
public class backendQManager {
    private backQueue currQueue;
    private backQueue queue1;
    private backQueue queue2;
    private backQueue queue3;
    private Comparator<backQueue> idComparator;
    private Queue<backQueue> queuePriority;
    private BaseRobotRules currRules;

    public backendQManager(){
        //comparator unit for priority
        idComparator = new Comparator<backQueue>(){
            @Override
            public int compare(backQueue c1, backQueue c2) {
                return (int) (c1.getPoliteWait().getTime() - c2.getPoliteWait().getTime());
            }
        };

        // priority queue according to politeness wait time
        queuePriority = new PriorityQueue<backQueue>(3,idComparator);
    }

    //add queues into priority queue
    public void addAllQueues(){
        if(queue1!=null)
            if(!queue1.isEmpty())
                queuePriority.add(queue1);

        if(queue2!=null)
            if(!queue2.isEmpty())
                queuePriority.add(queue2);


        if(queue3!=null)
            if(!queue3.isEmpty())
                queuePriority.add(queue3);
    }

    //check enqueue possibility, basically stop if all queues are taken up by some domain
    public boolean canWeEnqueue(URL url) throws IOException{
        String hostID = url.getAuthority();
        if(currQueue!=null && currQueue==queue3){
            if(!hostID.equals(queue3.getHostID())){
                if(queue1!=null){
                    if(hostID.equals(queue1.getHostID())){
                        return true;
                    }
                    else{
                        if(queue2!=null){
                            if(hostID.equals(queue2.getHostID())){
                                return true;
                            }
                            else{
                                if(queue3!=null){
                                    if(hostID.equals(queue3.getHostID())){
                                        return true;
                                    }
                                }
                            }
                        }
                    }
                }
                return false;
            }
        }
        return true;
    }

    //assign a new domain to a queue
    public void assignNextCurrQueue(String item,BaseRobotRules rules) throws IOException{
        URL url = new URL(item);
        String hostID = url.getAuthority();
        if(currQueue==queue1){
            if(queue2==null)
                queue2 = new backQueue(hostID,rules);
            currQueue = queue2;
        }
        else{
            if(currQueue==queue2){
                if(queue3==null)
                    queue3 = new backQueue(hostID,rules);
                currQueue = queue3;
            }
        }
    }

    public void enqueueCurr(String item, BaseRobotRules rules) throws IOException{
        URL url = new URL(item);
        String hostID = url.getAuthority();
        if(currQueue==null){
            if(queue1==null){
                queue1 = new backQueue(hostID,rules);
            }
            currQueue = queue1;
            currQueue.enqueue(item);
        }
        else{
            if(findQueue(hostID,item)==0)
                if(!hostID.equals(currQueue.getHostID())){
                    assignNextCurrQueue(item,rules);
                    currQueue.enqueue(item);
                }
        }
    }

    public int findQueue(String hostID,String item){
        if(queue1!=null){
            if(hostID.equals(queue1.getHostID())){
                queue1.enqueue(item);
                return 1;
            }
            else{
                if(queue2!=null){
                    if(hostID.equals(queue2.getHostID())){
                        queue2.enqueue(item);
                        return 1;
                    }
                    else{
                        if(queue3!=null){
                            if(hostID.equals(queue3.getHostID())){
                                queue3.enqueue(item);
                                return 1;
                            }
                        }
                    }
                }
            }
        }
        return 0;
    }


    public String dequeue(){
        backQueue topPriority = queuePriority.poll();
        Date currTime = new Date();
        if(topPriority.getPoliteWait().getTime() > currTime.getTime())
            try{
                int sleepTime = (int)((topPriority.getPoliteWait().getTime()-currTime.getTime())/1000);
                long t = topPriority.getRules().getCrawlDelay();
                if(t>0){
                    int robotDelay = (int)(topPriority.getRules().getCrawlDelay()/1000);
                    if(sleepTime>robotDelay)
                        TimeUnit.SECONDS.sleep(sleepTime);
                    else
                        TimeUnit.SECONDS.sleep(sleepTime + (sleepTime - robotDelay));
                }
                else{
                    TimeUnit.SECONDS.sleep(sleepTime);
                }
            }
            catch (Exception ex){
                System.out.println("did not sleep");
            }
        String ans = topPriority.dequeue();
        if(!topPriority.isEmpty()){
            topPriority.updateDate();
            queuePriority.add(topPriority);
        }
        this.currRules = topPriority.getRules();
        return ans;
    }

    public boolean ifPriorityEmpty(){
        boolean ans = queuePriority.isEmpty();
        return queuePriority.isEmpty();
    }

    public BaseRobotRules getCurrRules() {
        return this.currRules;
    }
}
