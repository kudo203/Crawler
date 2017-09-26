import crawlercommons.robots.BaseRobotRules;

import java.util.Calendar;
import java.util.Date;

/**
 * Created by koosh on 25/6/17.
 */
public class backQueue {
    private Queue1 q;
    private String hostID;
    private Date politeWait;
    private BaseRobotRules rules;

    public backQueue(String domain,BaseRobotRules rules){
        this.q = new Queue1();
        this.hostID = domain;
        this.politeWait = new Date();
        this.rules = rules;
    }

    public void enqueue(String item){
        this.q.enqueue(item);
    }

    public void updateDate(){
        this.politeWait = new Date();
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTime(this.politeWait);
        cal.add(cal.MILLISECOND, 500);
        this.politeWait = cal.getTime();
    }

    public String dequeue(){
        if(q.isEmpty())
            return null;
        return q.dequeue();
    }

    public String getHostID() {
        return hostID;
    }

    public boolean isEmpty(){
        return q.isEmpty();
    }

    public Date getPoliteWait() {
        return politeWait;
    }

    public BaseRobotRules getRules() {
        return rules;
    }
}
