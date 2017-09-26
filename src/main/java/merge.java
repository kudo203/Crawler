import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.script.Script;
import org.elasticsearch.transport.client.PreBuiltTransportClient;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import java.io.BufferedReader;
import java.io.FileReader;

public class merge {

    private static Client client = null;
    private static int uniqueURLCount = 0;

    private static Client getTransportESClient() {
        if (client == null) {

            try {
                Settings settings = Settings.builder().put("cluster.name", "bazinga").build();
                client = new PreBuiltTransportClient(settings)
                        .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName("localhost"), 9300));
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        }

        return client;
    }

    public static String mergeDoc(String type, String docNo, String JSON,String indexName) {
        Client client = null;

        try {
            client = getTransportESClient();

            GetResponse response = client.prepareGet(indexName, type, docNo)
                    .setFetchSource(new String[]{"docno", "author"}, null)
                    .get();
            if (response.isExists()) {

                String newContri = response.getSource().get("author") + " kd";

                UpdateRequest updateRequest = new UpdateRequest(indexName, type, docNo)
                        .script(new Script("ctx._source.author = \"" + newContri + "\"")).timeout("5000ms");

                UpdateResponse updateResponse = client.update(updateRequest).get();

                if(updateResponse.status().getStatus()!=200)
                    System.out.println("Failed to merge: " + docNo);


            }
            else {
                IndexResponse indexResponse = client.prepareIndex(indexName, type,docNo)
                        .setSource(JSON)
                        .get();
                if(indexResponse.status().getStatus()==201){
                    uniqueURLCount += 1;
                }
                else
                    System.out.println("Failed to index: " + docNo);

            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {

        }
        return null;
    }


    public static void main(String[] args) {

        String filePath = "E:\\documents.txt";

        Set<String> visitedUrls = new HashSet<String>();

        ObjectMapper mapper = new ObjectMapper();
        int i = 0;
        try{
            BufferedReader br = new BufferedReader(new FileReader(filePath));
            for(String line; (line = br.readLine()) != null;){
                try {
                    JsonNode jsonNode = mapper.readTree(line);
                    String normURl  = jsonNode.get("docno").asText().toLowerCase();

                    if(!visitedUrls.contains(normURl)) {
                        mergeDoc("document", jsonNode.get("docno").asText(), line,"mi");
                        visitedUrls.add(normURl);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        catch(Exception ex){
            ex.printStackTrace();
        }
        finally {
            if (client != null)
                client.close();
        }
        System.out.println(uniqueURLCount);
    }

}