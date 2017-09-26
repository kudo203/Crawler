import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;

//noinspection Since15
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public class Urlnorm{
    private final Pattern server_authority = Pattern.compile("^(?:([^\\@]+)\\@)?([^\\:\\[\\]]+|\\[[a-fA-F0-9\\:\\.]+\\])(?:\\:(.*?))?$");
    public static final String[] ALLOWED_PORTOCOLS = new String[] { "http", "https" };
    public static final Set<String> PROTOCOL_SET = new HashSet<String>(Arrays.asList(ALLOWED_PORTOCOLS));
    private final static Pattern hasNormalizablePathPattern = Pattern.compile("/[./]|[.]/");

    public static String norm(String url) {

        try {
            URL url1 = new URL(decode(url));
            return normalizeUrl(url1);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return "INVALID_URL";
    }

    private static String normalizeUrl(URL url1) {
        StringBuilder sb = new StringBuilder();
//        System.out.println("protocol = " + url1.getProtocol());
//        System.out.println("authority = " + url1.getAuthority());
//        System.out.println("host = " + url1.getHost());
//        System.out.println("port = " + url1.getPort());
//        System.out.println("path = " + url1.getPath());
//        System.out.println("query = " + url1.getQuery());
//        System.out.println("filename = " + url1.getFile());
//        System.out.println("ref = " + url1.getRef());

        String protocol = url1.getProtocol().toLowerCase();
        String host = url1.getHost().toLowerCase().replaceFirst("www.","");
        if(!PROTOCOL_SET.contains(protocol))
            return "INVALID_URL";

        sb.append("http://");
        sb.append(host);

        //noinspection Since15
        if(url1.getPath() != null && !url1.getPath().isEmpty()){
            try{
                sb.append(normPath(url1));
            }
            catch (MalformedURLException e){

            }
        }



        //noinspection Since15
        if(url1.getQuery()!= null && !url1.getQuery().isEmpty())
            sb.append("?"+ url1.getQuery());

        //noinspection Since15
        if(host.contains("google") && url1.getRef()!= null && !url1.getRef().isEmpty())
            sb.append("#"+url1.getRef());

        if(sb.charAt(sb.length()-1) == '/')
            sb.setLength(sb.length() - 1);


        return sb.toString();
    }

    private static String decode(String value) {
        String decoded
                = null;
        try {
            //noinspection Since15
            decoded = URLDecoder.decode(value, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return decoded;
    }


    private static String normPath(URL url) throws MalformedURLException {
        String file;
        String ans = "";
        if (hasNormalizablePathPattern.matcher(url.getPath()).find()) {
            // only normalize the path if there is something to normalize
            // to avoid needless work
            try {
                file = url.getFile();
                // URI.normalize() does not normalize leading dot segments,
                // see also http://tools.ietf.org/html/rfc3986#section-5.2.4
                int start = 0;
                while (start < file.length()) {
                    if(file.startsWith("/./",start)){
                        start += 2;
                    }
                    else{
                        if(file.startsWith("/../",start)){
                            start += 3;
                        }
                        else{
                            ans += file.charAt(start);
                            start++;
                        }
                    }
                }
                return ans;
            }
            catch (Exception e) {
                file = url.getFile();
            }
            //URISyntax
        } else {
            file = url.getFile();
        }

        // if path is empty return a single slash
        if (file.isEmpty()) {
            file = "/";
        }

        return file;
    }
}