/**
 * Created by koosh on 20/6/17.
 */
public interface Cacheable {
    public boolean isExpired();
    public String getIdentifier();
}
