/**
 * Created by koosh on 25/6/17.
 */
public class Node {
    private String item;
    private Node next;

    public Node(String item, Node next){
        this.item = item;
        this.next = next;
    }

    public Node getNext() {
        return next;
    }

    public String getItem() {
        return item;
    }

    public void setItem(String item) {
        this.item = item;
    }

    public void setNext(Node next) {
        this.next = next;
    }
}
