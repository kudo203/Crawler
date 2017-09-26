import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.Serializable;

/**
 * Created by koosh on 26/6/17.
 */
public class jsonCreator implements Serializable{
    private static ObjectMapper mapper;

    public jsonCreator(ObjectMapper mapper){
        this.mapper = mapper;
    }

    public static String jsonString(docFrame frame) throws IOException{
        return mapper.writeValueAsString(frame);
    }
}
