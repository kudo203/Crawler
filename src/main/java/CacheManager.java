/**
 * Created by koosh on 20/6/17.
 */
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

public class CacheManager{

    private static HashMap cacheMap = new HashMap();
    private static Object lock = new Object();

    static{
        try{
            Thread cacheCleaner = new Thread(
                    new Runnable() {
                        int sleepTime = 5000;
                        public void run() {
                            try{
                                while(true){
                                    Set keySet = cacheMap.keySet();
                                    Iterator keys = keySet.iterator();

                                    while(keys.hasNext()){
                                        synchronized (lock){
                                            Object key = keys.next();
                                            Cacheable value = (Cacheable) cacheMap.get(key);
                                            if(value.isExpired()){
                                                cacheMap.remove(key);
                                            }
                                        }
                                    }
                                    Thread.sleep(sleepTime);
                                }
                            }
                            catch(Exception ex){
                                ex.printStackTrace();
                            }
                            return;
                        }
                    }
            );
            cacheCleaner.setPriority(Thread.MIN_PRIORITY);
            cacheCleaner.start();
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

    public CacheManager(){

    }

    public void putCache(Cacheable obj){
        if(cacheMap.size()<=2000)
            cacheMap.put(obj.getIdentifier(),obj);
    }

    public Cacheable getCache(Object identifier){
        Cacheable object = null;
        synchronized (lock){
            object = (Cacheable)cacheMap.get(identifier);
        }
        if(object==null)
            return null;
        if(object.isExpired()){
            cacheMap.remove(identifier);
            return null;
        }
        else
            return object;
    }
}
