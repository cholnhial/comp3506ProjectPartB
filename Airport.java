import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Airport extends AirportBase {
    private AdjacencyMap<TerminalBase, ShuttleBase> adjacencyMap;
    private Map<TerminalBase, AdjacencyMap<TerminalBase, ShuttleBase>.Vertex<TerminalBase>> terminalVertices;
    private Map<ShuttleBase, AdjacencyMap<TerminalBase, ShuttleBase>.Edge<ShuttleBase>> shuttleEdges;

    /**
     * Creates a new AirportBase instance with the given capacity.
     *
     * @param capacity capacity of the airport shuttles
     *                 (same for all shuttles)
     */
    public Airport(int capacity) {
        super(capacity);
        adjacencyMap = new AdjacencyMap<>();
        terminalVertices = new HashMap<>(); // represents vertices
        shuttleEdges = new HashMap<>();
    }


    @Override
    public TerminalBase opposite(ShuttleBase shuttle, TerminalBase terminal) {
        var vertex = terminalVertices.get(terminal);
        var edge  = shuttleEdges.get(shuttle);

        var oppositeVertex =  adjacencyMap.opposite(vertex, edge);

        return  oppositeVertex == null ? null : oppositeVertex.getElement();
    }

    @Override
    public TerminalBase insertTerminal(TerminalBase terminal) {
        if(terminal != null) {
            var vertex = adjacencyMap.insertVertex(terminal);
            terminalVertices.put(terminal, vertex);
            return vertex.getElement();
        }

        return null;
    }

    @Override
    public ShuttleBase insertShuttle(TerminalBase origin, TerminalBase destination, int time) {
        if (origin != null && destination != null) {
            var originVertex = terminalVertices.get(origin);
            var destinationVertex = terminalVertices.get(destination);
            ShuttleBase shuttle = new Shuttle(origin, destination, time);
            var edge = adjacencyMap.insertEdge(originVertex, destinationVertex, shuttle);
            shuttleEdges.put(shuttle, edge);
            return shuttle;
        }

        return null;
    }

    @Override
    public boolean removeTerminal(TerminalBase terminal) {
        if (terminal != null) {
            AdjacencyMap<TerminalBase, ShuttleBase>.Vertex<TerminalBase> vertex = terminalVertices.get(terminal);
            terminalVertices.remove(terminal);
            adjacencyMap.removeVertex(vertex);
            return true;
        }

        return false;
    }

    @Override
    public boolean removeShuttle(ShuttleBase shuttle) {
        if (shuttle != null) {
            AdjacencyMap<TerminalBase, ShuttleBase>.Edge<ShuttleBase> edge = shuttleEdges.get(shuttle);
            shuttleEdges.remove(shuttle);
            adjacencyMap.removeEdge(edge);
            return true;
        }

        return false;
    }

    @Override
    public List<ShuttleBase> outgoingShuttles(TerminalBase terminal) {
        if (terminal != null) {
            AdjacencyMap<TerminalBase, ShuttleBase>.Vertex<TerminalBase> vertex = terminalVertices.get(terminal);
            return adjacencyMap.outgoingEdges(vertex).stream().map(AdjacencyMap.Edge::getElement)
                    .collect(Collectors.toList());
        }

        return null;
    }

    @Override
    public Path findShortestPath(TerminalBase origin, TerminalBase destination) {
        return null;
    }

    @Override
    public Path findFastestPath(TerminalBase origin, TerminalBase destination) {
        return null;
    }

    /* Implement all the necessary methods of the Airport here */

    static class Terminal extends TerminalBase {
        /**
         * Creates a new TerminalBase instance with the given terminal ID
         * and waiting time.
         *
         * @param id          terminal ID
         * @param waitingTime waiting time for the terminal, in minutes
         */
        public Terminal(String id, int waitingTime) {
            super(id, waitingTime);
        }

        /* Implement all the necessary methods of the Terminal here */
    }

    static class Shuttle extends ShuttleBase {
        /**
         * Creates a new ShuttleBase instance, travelling from origin to
         * destination and requiring 'time' minutes to travel.
         *
         * @param origin      origin terminal
         * @param destination destination terminal
         * @param time        time required to travel, in minutes
         */
        public Shuttle(TerminalBase origin, TerminalBase destination, int time) {
            super(origin, destination, time);
        }

        /* Implement all the necessary methods of the Shuttle here */
    }

    /*
        !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        REMOVE THE MAIN FUNCTION BEFORE SUBMITTING TO THE AUTOGRADER
        !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

        The following main function is provided for simple debugging only

        Note: to enable assertions, you need to add the "-ea" flag to the
        VM options of Airport's run configuration
     */
    public static void main(String[] args) {
        Airport a = new Airport(3);
        Terminal terminalA = (Terminal) a.insertTerminal(new Terminal("A", 1));
        Terminal terminalB = (Terminal) a.insertTerminal(new Terminal("B", 3));
        Terminal terminalC = (Terminal) a.insertTerminal(new Terminal("C", 4));
        Terminal terminalD = (Terminal) a.insertTerminal(new Terminal("D", 2));

        Shuttle shuttle1 = (Shuttle) a.insertShuttle(terminalA, terminalB, 2);
        Shuttle shuttle2 = (Shuttle) a.insertShuttle(terminalA, terminalC, 5);
        Shuttle shuttle3 = (Shuttle) a.insertShuttle(terminalA, terminalD, 18);
        Shuttle shuttle4 = (Shuttle) a.insertShuttle(terminalB, terminalD, 8);
        Shuttle shuttle5 = (Shuttle) a.insertShuttle(terminalC, terminalD, 15);

        // Opposite
        assert a.opposite(shuttle1, terminalA).getId().equals("B");

        // Outgoing Shuttles
        assert a.outgoingShuttles(terminalA).stream()
                .map(ShuttleBase::getTime)
                .collect(Collectors.toList()).containsAll(List.of(2, 5, 18));

        // Remove Terminal
        a.removeTerminal(terminalC);
        assert a.outgoingShuttles(terminalA).stream()
                .map(ShuttleBase::getTime)
                .collect(Collectors.toList()).containsAll(List.of(2, 18));

        // Shortest path
        Path shortestPath = a.findShortestPath(terminalA, terminalD);
        assert shortestPath.terminals.stream()
                .map(TerminalBase::getId)
                .collect(Collectors.toList()).equals(List.of("A", "D"));
        assert shortestPath.time == 19;

        // Fastest path
        Path fastestPath = a.findFastestPath(terminalA, terminalD);
        assert fastestPath.terminals.stream()
                .map(TerminalBase::getId)
                .collect(Collectors.toList()).equals(List.of("A", "B", "D"));
        assert fastestPath.time == 14;
    }
}


interface Position<E> {
    E getElement() throws IllegalStateException;
}

class PositionalLinkedList<E> {

    private static class Node<E> implements Position<E> {
        private E element;

        private Node<E> prev;

        private Node<E> next;

        public Node(E e, Node<E> p, Node<E> n) {
            element = e;
            prev = p;
            next = n;
        }

        public E getElement() {
            if (next == null)
                    return null;
            return element;
        }

        public Node<E> getPrev() {
            return prev;
        }

        public Node<E> getNext() {
            return next;
        }

        public void setElement(E e) {
            element = e;
        }

        public void setPrev(Node<E> p) {
            prev = p;
        }

        public void setNext(Node<E> n) {
            next = n;
        }
    }

    private Node<E> head;

    private Node<E> tail;
    private int size = 0;

    public PositionalLinkedList() {
        head = new Node<>(null, null, null);
        tail = new Node<>(null, head, null);
        head.setNext(tail);
    }

    private Position<E> position(Node<E> node) {
        if (node == head || node == tail)
            return null;   // do not expose user to the sentinels
        return node;
    }

    public int size() { return size; }

    public boolean isEmpty() { return size == 0; }

    public Position<E> first() {
        return position(head.getNext());
    }

    public Position<E> last() {
        return position(tail.getPrev());
    }

    public Position<E> before(Position<E> p) throws IllegalArgumentException {
        Node<E> node = (Node<E>) p;
        return position(node.getPrev());
    }

    public Position<E> after(Position<E> p) throws IllegalArgumentException {
        Node<E> node = (Node<E>) p;
        return position(node.getNext());
    }

    private Position<E> addBetween(E e, Node<E> pred, Node<E> succ) {
        Node<E> newest = new Node<>(e, pred, succ);  // create and link a new node
        pred.setNext(newest);
        succ.setPrev(newest);
        size++;
        return newest;
    }

    public Position<E> addFirst(E e) {
        return addBetween(e, head, head.getNext());       // just after the header
    }

    public Position<E> addLast(E e) {
        return addBetween(e, tail.getPrev(), tail);     // just before the trailer
    }


    public Position<E> addBefore(Position<E> p, E e)
            throws IllegalArgumentException {
        Node<E> node = (Node<E>) p;
        return addBetween(e, node.getPrev(), node);
    }


    public Position<E> addAfter(Position<E> p, E e)
            throws IllegalArgumentException {
        Node<E> node = (Node<E>) p;
        return addBetween(e, node, node.getNext());
    }


    public E set(Position<E> p, E e) throws IllegalArgumentException {
        Node<E> node = (Node<E>) p;
        E answer = node.getElement();
        node.setElement(e);
        return answer;
    }

    public E remove(Position<E> p) throws IllegalArgumentException {
        Node<E> node = (Node<E>) p;
        Node<E> predecessor = node.getPrev();
        Node<E> successor = node.getNext();
        predecessor.setNext(successor);
        successor.setPrev(predecessor);
        size--;
        E answer = node.getElement();
        node.setElement(null);
        node.setNext(null);
        node.setPrev(null);
        return answer;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("(");
        Node<E> walk = head.getNext();
        while (walk != tail) {
            sb.append(walk.getElement());
            walk = walk.getNext();
            if (walk != tail)
                sb.append(", ");
        }
        sb.append(")");
        return sb.toString();
    }
}


/**
 *
 * Implemented using Lecture notes and course recommended resources
 *
 * @param <V> Generic class for vertex
 * @param <E> Generic class for Edge
 */
class AdjacencyMap<V, E> {
    private PositionalLinkedList<Vertex<V>> vertices = new PositionalLinkedList<>();
    private PositionalLinkedList<Edge<E>> edges = new PositionalLinkedList<>();


    public class Vertex<V> {
        private V element;
        private Position<Vertex<V>> position;
        private Map<Vertex<V>, Edge<E>> incoming, outgoing;

        public Vertex(V element) {
            this.element = element;
            incoming = new HashMap<>();
            outgoing = incoming;
        }

        public V getElement() {
            return element;
        }

        public Position<Vertex<V>> getPosition() {
            return position;
        }

        public void setPosition(Position<Vertex<V>> position) {
            this.position = position;
        }

        public Map<Vertex<V>, Edge<E>> getIncoming() {
            return incoming;
        }

        public Map<Vertex<V>, Edge<E>> getOutgoing() {
            return outgoing;
        }
    }

    public  class Edge<E> {
        private E element;
        private Position<Edge<E>> position;
        private Vertex<V>[ ] endpoints;

        public Edge(Vertex<V> u, Vertex<V> v, E elem) {
            element = elem;
            endpoints = (Vertex<V>[ ]) new Vertex[2];
            endpoints[0] = u;
            endpoints[1] = v;
        }

        public E getElement() {
            return element;
        }

        public void setElement(E element) {
            this.element = element;
        }

        public Position<Edge<E>> getPosition() {
            return position;
        }

        public void setPosition(Position<Edge<E>> position) {
            this.position = position;
        }

        public Vertex<V>[] getEndpoints() {
            return endpoints;
        }

        public void setEndpoints(Vertex<V>[] endpoints) {
            this.endpoints = endpoints;
        }
    }

    public Edge<E> getEdge(Vertex<V> u, Vertex<V> v) {
        return u.getOutgoing().get(v);
    }

    public Vertex<V> asVertex(V element) {
        return new Vertex<>(element);
    }


    public List<Edge<E>> outgoingEdges(Vertex<V> v) {
        return v.getOutgoing().values().stream().collect(Collectors.toList());
    }

    public Vertex<V> insertVertex(V element) {
        Vertex<V> v = new Vertex<>(element);
        // set the position so we can remove it later
        v.setPosition(vertices.addLast(v));
        return v;
    }

    public void removeVertex(Vertex<V> v) {
        var outgoing = new ArrayList<>(v.getOutgoing().values());
        for (Edge<E> e : outgoing)
            removeEdge(e);
        var incoming = new ArrayList<>(v.getIncoming().values());
        for (Edge<E> e : incoming)
            removeEdge(e);
        vertices.remove(v.getPosition());
        // make the vertex invalid
        v.setPosition(null);
    }

    public void removeEdge(Edge<E> e) {
        Vertex<V>[] verts = e.getEndpoints();
        verts[0].getOutgoing().remove(verts[1]);
        verts[1].getIncoming().remove(verts[0]);
        edges.remove(e.getPosition());
        // make the edge invalid
        e.setPosition(null);
    }

    public Vertex<V> opposite(Vertex<V> v, Edge<E> e) {
        Vertex<V>[] endpoints = e.getEndpoints();
        if (endpoints[0] == v)
            return endpoints[1];
        else if (endpoints[1] == v)
            return endpoints[0];
        else
            return null; // v is not incident to this edge
    }

    public Edge<E> insertEdge(Vertex<V> u, Vertex<V> v, E element)  {
        if (getEdge(u, v) == null) {
            Edge<E> e = new Edge<>(u, v, element);
            e.setPosition(edges.addLast(e));
            u.getOutgoing().put(v, e);
            v.getIncoming().put(u, e);
            return e;
        } else {
            return null;
        }
    }
}

