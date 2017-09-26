/**
 * Created by koosh on 20/6/17.
 */
import crawlercommons.robots.BaseRobotRules;

import java.util.Date;

public class wrappedCacheObject implements Cacheable{

    private Date dateOFExpiration = null;
    private String identifier = null;

    public BaseRobotRules obj = null;

    public wrappedCacheObject(String identifier, BaseRobotRules obj,int liveMinutes){
        this.identifier = identifier;
        this.obj = obj;

        if(liveMinutes!=0){
            this.dateOFExpiration = new java.util.Date();
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.setTime(this.dateOFExpiration);
            cal.add(cal.MINUTE, liveMinutes);
            this.dateOFExpiration = cal.getTime();
        }
    }

    public boolean isExpired(){
        if(this.dateOFExpiration!=null){
            if(this.dateOFExpiration.before(new Date())){
                return true;
            }
            else
                return false;
        }
        else
            return false;
    }

    public String getIdentifier() {
        return this.identifier;
    }

    public BaseRobotRules getRules(){
        updateLiveMinutes();
        return this.obj;
    }

    public void updateLiveMinutes(){
        this.dateOFExpiration = new java.util.Date();
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTime(this.dateOFExpiration);
        cal.add(cal.SECOND, 5);
        this.dateOFExpiration = cal.getTime();
    }
}
