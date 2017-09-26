import java.util.NoSuchElementException;

/**
 * Created by koosh on 25/6/17.
 */
public class Queue1 {

    private Node first;
    private Node last;
    private int n;

    public Queue1(){
        first = null;
        last = null;
        n = 0;
    }

    public boolean isEmpty() {
        return first == null;
    }

    public String peek() {
        if (isEmpty()) throw new NoSuchElementException("Queue underflow");
        return first.getItem();
    }

    public void enqueue(String item) {
        Node oldlast = last;
        last = new Node(item,null);
        if (isEmpty())
            first = last;
        else
            oldlast.setNext(last);
        n++;
    }

    public String dequeue() {
        if (isEmpty()) throw new NoSuchElementException("Queue underflow");
        String item = first.getItem();
        first = first.getNext();
        n--;
        if (isEmpty()) last = null;   // to avoid loitering
        return item;
    }


}
