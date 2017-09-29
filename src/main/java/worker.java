
import com.fasterxml.jackson.databind.ObjectMapper;
import crawlercommons.robots.BaseRobotRules;
import crawlercommons.robots.SimpleRobotRules;
import crawlercommons.robots.SimpleRobotRulesParser;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import scala.Int;

import javax.print.Doc;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by koosh on 20/6/17.
 */
public class worker {

    //static global variables
    private static String USER_AGENT;
    private static int defRobotLife;
    private static Queue frontEnd;
    private static backendQManager backQueues;

    private static CacheManager cacheRobots;

    private static Matcher matcher;
    private static HashMap<String,Integer> relevanceScore;
    private static Stemmer stem;
    private static HashSet<String> visitedLinks;
    private static String LinkSelector;

    //linkset of the current wave
    private static Map<String,Integer> waveLinks;
    private static int depth;
    private static FileWriter fw;
    private static BufferedWriter bw;
    private static ObjectMapper mapper;
    private static int crawlCount;

    private static Pattern pattern;

    public static void main(String[] args) throws IOException {
        //initialization


        //load properties
        Properties properties = new Properties();
        FileReader propReader = new FileReader("conf.properties");
        //noinspection Since15
        properties.load(propReader);
        propReader.close();

        //extract properties
        USER_AGENT = properties.getProperty("userAgent");
        defRobotLife = Integer.parseInt(properties.getProperty("defRobotLife"));

        //Mercantile implementation
        frontEnd = new Queue();
        backQueues = new backendQManager();

        //Cache for caching of domain rules
        cacheRobots = new CacheManager();

        pattern = Pattern.compile("(https?:\\/\\/(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_\\+.~#?&//=]*))|([-a-zA-Z0-9@:%._\\+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_\\+.~#?&//=]*))|(\\/?([^:\\/\\s]+)((\\/\\w+)*\\/)([\\w\\-\\.]+[^#?\\s]+)(.*)?(#[\\w\\-]+)?$)");

        //Priority to words in the link and anchor text
        relevanceScore = loadWordScores();

        stem = new Stemmer();
        visitedLinks = new HashSet<String>();

        //linkRegex
        LinkSelector = "a[href]:not([href~=(?i).*(\\.(css|js|bmp|gif|jpe?g|png|tiff?|mid|mp2|mp3|mp4|wav|avi|mov|mpeg|ram|m4v|pdf" +
                "|rm|smil|wmv|swf|wma|zip|rar|gz|csv|xls|ppt|doc|docx|exe|dmg|midi|mid|qt|txt|ram|json))$)" +
                ":not([href~=(?i)^#])";

        //bfs depth
        depth = 0;

        fw = new FileWriter("E:\\documents1.txt");
        bw = new BufferedWriter(fw);

        //json mapper
        mapper = new ObjectMapper();

        //seed URLs
        frontEnd.enqueue("https://en.wikipedia.org/wiki/History_of_immigration_to_the_United_States");
        frontEnd.enqueue("https://en.wikipedia.org/wiki/Immigration_to_the_United_States");
        frontEnd.enqueue("https://en.wikipedia.org/wiki/Immigration_policy_of_Donald_Trump");
        frontEnd.enqueue("https://www.theguardian.com/us-news/2017/jan/27/donald-trump-executive-order-immigration-full-text");
        frontEnd.enqueue("https://www.usatoday.com/story/news/world/2017/01/28/what-you-need-know-trumps-refugee-ban/97183112/");
        frontEnd.enqueue("https://www.whitehouse.gov/the-press-office/2017/01/27/executive-order-protecting-nation-foreign-terrorist-entry-united-states");
        frontEnd.enqueue("http://www.aljazeera.com/news/2017/05/immigrant-arrests-soar-donald-trump-170518034252370.html");


        //end Initialization
        //frontEnd.enqueue("https://login.ncsl.org/SSO/Login.aspx?vi=7&vt=0c264c32daa67238b05dbd3d26ae1063f80b2dcee313d4d893d535eec16142c3bbefecc57cbae7c23332b1f806b9a296c72dfea0999846b677dcb9728e06b1d97bfbfe74998e9e059f5c4ef459ec738c&DPLF=Y");

        //loop for crawling documents
        while(true){
            try{
                TimeUnit.MILLISECONDS.sleep(500);
            }
            catch (Exception ex){
                System.out.println("did not sleep");
            }

            waveLinks = new HashMap<String,Integer>();

            while(!frontEnd.isEmpty())
                frontBackTransfer();

            if(waveLinks.size()==0)
                break;

            //transfer wave links to frontier
            waveToFrontier();

            depth++;
            System.out.println(depth+"                                       ");
        }
        System.out.println(depth);
        System.exit(0);
    }

    public static void waveToFrontier(){
        waveLinks = sortByValue(waveLinks);
        for(Map.Entry<String,Integer> entry:waveLinks.entrySet()){
            frontEnd.enqueue(entry.getKey());
        }
    }

    public static void frontBackTransfer() throws IOException{

        //end at a certain count
        if(crawlCount>=20000){
            System.exit(0);
        }

        // initialize the domain-specific backQueues
        backQueues = new backendQManager();

        backQueuePopulate();
        backQueues.addAllQueues();
        while(!backQueues.ifPriorityEmpty()){
            String dequeueLink = backQueues.dequeue();
            BaseRobotRules rule = backQueues.getCurrRules();
            linkRuleHandle(dequeueLink,rule);
        }
    }

    public static void linkRuleHandle(String link,BaseRobotRules rules) throws IOException{
        String normalizedLink = Urlnorm.norm(link);
        if(!(normalizedLink.equals("INVALID_URL"))&&!(visitedLinks.contains(normalizedLink))){
            if(rules!=null){
                if(!rules.isAllowed(link))
                    return;
            }
            String finalLink = initiateGetMeta(link);
            if(finalLink!=null){
                if(!visitedLinks.contains(finalLink)){
                    parsingWebPage(normalizedLink,finalLink);
                    visitedLinks.add(finalLink);
                }
            }
            visitedLinks.add(normalizedLink);
        }
    }

    public static void parsingWebPage(String normalizedLink,String originalLink){
        try{
            Connection.Response res = Jsoup.connect(originalLink).
                    userAgent(USER_AGENT).
                    referrer("http://www.google.com").
                    timeout(8000).
                    followRedirects(true).
                    execute();

            if(res.statusCode()==200){
                Document doc = res.parse();

                HashMap<String,String> unscoredLinks = linkExtractor(doc);
                HashMap<String,Integer> scoredLinks = scoreLinks(unscoredLinks);
                waveLinks.putAll(scoredLinks);

                docFrame frame = generateFrame(doc,res,normalizedLink,originalLink,new HashSet<String>(scoredLinks.keySet()));
                jsonCreator js = new jsonCreator(mapper);
                String json = js.jsonString(frame) + "\n";
                bw.write(json);
                bw.flush();
                crawlCount++;
                System.out.println("just crawled " +  originalLink + " crawl Count: " + crawlCount + " " + new Date().toString() + " depth:" + depth);
            }

        }
        catch(Exception ex){
            System.out.println(ex);
            System.out.println("didn't crawl " + originalLink);
        }
    }

    public static docFrame generateFrame(Document doc, Connection.Response res,
                                         String normalizedLink,String originalLink,
                                         HashSet<String> outLinks){
        docFrame frame = new docFrame();
        frame.setHTTPheader(res.headers().toString());
        frame.setAuthor("kd");
        frame.setDocno(normalizedLink);
        frame.setDepth(depth);
        frame.setHtml_Source(doc.html());
        frame.setOut_links(new ArrayList<String>(outLinks));
        frame.setText(doc.getElementsByTag("html").text());
        frame.setTitle(doc.title());
        frame.setUrl(originalLink);
        return frame;
    }

    //UPGRADE This
    public static void backQueuePopulate() throws IOException{
        while(true){
            //break if front end is empty
            if(frontEnd.isEmpty())
                break;

            //check if the transfer can be made
            String plausibleLink = frontEnd.peek();
            try{

                URL url = new URL(plausibleLink);
                if(url.getAuthority()!=null){
                    if(backQueues.canWeEnqueue(url)){

                        //finally dequeue element
                        frontEnd.dequeue();

                        BaseRobotRules rules = getRobotRules(plausibleLink);
                        backQueues.enqueueCurr(plausibleLink,rules);

                    }
                    else
                        break;
                }
                else
                    frontEnd.dequeue();
            }
            catch(Exception ex){
                frontEnd.dequeue();
                System.out.println(ex + ": " + plausibleLink);
            }
        }
    }

    public static BaseRobotRules getRobotRules(String link) throws IOException{
        URL url = new URL(link);
        String hostID = url.getProtocol() + "://" + url.getHost()
                + (url.getPort() > -1 ? ":" + url.getPort() : "");

        wrappedCacheObject cacheRules = (wrappedCacheObject)cacheRobots.getCache(hostID);
        if(cacheRules==null){
            BaseRobotRules rules =  parseRulesSet(hostID);
            Cacheable linkRules = new wrappedCacheObject(hostID,rules,defRobotLife);
            cacheRobots.putCache(linkRules);
            return rules;
        }
        else
            return cacheRules.getRules();
    }

    public static HashMap<String,Integer> scoreLinks(HashMap<String,String> nextWaveLinks){
        HashMap<String,Integer> ans = new HashMap<String,Integer>();
        for(Map.Entry<String,String> entry:nextWaveLinks.entrySet()){
            String link = entry.getKey();
            String title = entry.getValue();
            int score = 0;
            ArrayList<String> listOfWords = convertLinkToWords(link + " " + title);
            for(int j = 0; j < listOfWords.size(); j++){
                String word = listOfWords.get(j);
                Integer val = relevanceScore.get(word);
                if(val!=null)
                    score+=val;
            }
            if(score>5)
                ans.put(link,score);
        }
        return ans;
    }
    public static Map<String, Integer> sortByValue(Map<String, Integer> unsortMap) {

        // 1. Convert Map to List of Map
        List<Map.Entry<String, Integer>> list =
                new LinkedList<Map.Entry<String, Integer>>(unsortMap.entrySet());

        // 2. Sort list with Collections.sort(), provide a custom Comparator
        //    Try switch the o1 o2 position for a different order
        Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {
            public int compare(Map.Entry<String, Integer> o1,
                               Map.Entry<String, Integer> o2) {
                return (o2.getValue()).compareTo(o1.getValue());
            }
        });


        // 3. Loop the sorted list and put it into a new insertion order Map LinkedHashMap
        Map<String, Integer> sortedMap = new LinkedHashMap<String, Integer>();
        for (Map.Entry<String, Integer> entry : list) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }

        /*
        //classic iterator example
        for (Iterator<Map.Entry<String, Integer>> it = list.iterator(); it.hasNext(); ) {
            Map.Entry<String, Integer> entry = it.next();
            sortedMap.put(entry.getKey(), entry.getValue());
        }*/


        return sortedMap;
    }

    public static ArrayList<String> convertLinkToWords(String link){
        ArrayList<String> listOfWords = new ArrayList<String>();
        Pattern word = Pattern.compile("[a-zA-Z]+");
        matcher = word.matcher(link);

        while(matcher.find()){
            listOfWords.add(stem.stripAffixes(matcher.group().toLowerCase()));
        }
        return listOfWords;
    }

    public static HashMap<String,String> linkExtractor(Document doc) throws  IOException{
        HashMap<String,String> linkListHash = new HashMap<String, String>();

        Elements extractRawList = doc.select(LinkSelector);
        for(Element iter:extractRawList){
            String linkHref = iter.attr("href");
            String absHref = iter.attr("abs:href");
            String iterText = iter.text();

            if(!(linkHref.equals("")||iter.toString().contains("edit"))){
                /*
                matcher = pattern.matcher(linkHref);
                if(matcher.find()){
                    linkList.add(absHref);
                }
                */
                linkListHash.put(absHref,iterText);
            }
        }
        return linkListHash;
    }

    public static String initiateGetMeta(String link) throws IOException{
        return getMeta(link,0);
    }


    public static String getMeta(String link,int redirectCount) throws IOException{
        try{
            if(redirectCount<4){
                URL url = new URL(link);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(4000);
                try{
                    connection.setRequestMethod("HEAD");
                    if (isRedirect(connection.getResponseCode())) {
                        String newUrl = connection.getHeaderField("Location"); // get redirect url from "location" header field
                        redirectCount += 1;
                        return getMeta(newUrl,redirectCount);
                    }
                    String contentType = connection.getContentType();
                    if(contentType.contains("text")||contentType.contains("html"))
                        return link;
                }
                catch(Exception ex){
                    System.out.println("didn't get Head " + link );
                }
            }
            return null;
        }
        catch(StackOverflowError ex){
            System.out.println(ex + " , " + link);
            return null;
        }
    }

    //check status for redirects
    protected static boolean isRedirect(int statusCode) {
        if (statusCode != HttpURLConnection.HTTP_OK) {
            if (statusCode == HttpURLConnection.HTTP_MOVED_TEMP
                    || statusCode == HttpURLConnection.HTTP_MOVED_PERM
                    || statusCode == HttpURLConnection.HTTP_SEE_OTHER) {
                return true;
            }
        }
        return false;
    }

    public static BaseRobotRules parseRulesSet(String hostId){
        try{
            BaseRobotRules rules = null;
            HttpGet httpget = new HttpGet(hostId + "/robots.txt");
            HttpContext context = new BasicHttpContext();
            RequestConfig config = RequestConfig.custom()
                    .setConnectTimeout(3000)
                    .setConnectionRequestTimeout(1000)
                    .setSocketTimeout(3000).build();
            CloseableHttpClient httpclient =  HttpClientBuilder.create().setDefaultRequestConfig(config).build();
            //CloseableHttpClient httpclient = HttpClients.createDefault();
            HttpResponse response = httpclient.execute(httpget, context);
            if (response.getStatusLine() != null && response.getStatusLine().getStatusCode() == 404) {
                rules = new SimpleRobotRules(SimpleRobotRules.RobotRulesMode.ALLOW_ALL);
                // consume entity to deallocate connection
                EntityUtils.consumeQuietly(response.getEntity());
            } else {
                BufferedHttpEntity entity = new BufferedHttpEntity(response.getEntity());
                SimpleRobotRulesParser robotParser = new SimpleRobotRulesParser();
                rules = robotParser.parseContent(hostId, IOUtils.toByteArray(entity.getContent()),
                        "text/plain", USER_AGENT);
            }
            return rules;
        }
        catch(Exception ex){
            System.out.println(ex + ": Didn't get robots.txt for " + hostId);
            return null;
        }
    }

    public static HashMap<String,Integer> loadWordScores() throws IOException{
        FileReader fr = new FileReader("C:\\Users\\Koosh_20\\Desktop\\crawlerGit1\\relevantWords.txt");
        BufferedReader br = new BufferedReader(fr);

        HashMap<String,Integer> ans = new HashMap<String, Integer>();
        while(true){
            String line = br.readLine();
            if(line==null)
                break;
            String[] breakLine = line.split(",");
            ans.put(breakLine[0],Integer.parseInt(breakLine[1]));
        }

        return ans;
    }

}
