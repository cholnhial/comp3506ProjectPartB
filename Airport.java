import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;


public class Airport extends AirportBase {
    private final AdjacencyMap<TerminalBase, ShuttleBase> adjacencyMap;
    private final Map<TerminalBase, AdjacencyMap<TerminalBase, ShuttleBase>.Vertex<TerminalBase>> terminalVertices;
    private final Map<ShuttleBase, AdjacencyMap<TerminalBase, ShuttleBase>.Edge<ShuttleBase>> shuttleEdges;
    private final Map<ShuttleBase, Integer> shuttleCapacity;

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
        shuttleCapacity = new HashMap<>();
    }


    @Override
    public TerminalBase opposite(ShuttleBase shuttle, TerminalBase terminal) {
        var vertex = terminalVertices.get(terminal);
        var edge = shuttleEdges.get(shuttle);

        var oppositeVertex = adjacencyMap.opposite(vertex, edge);

        return oppositeVertex == null ? null : oppositeVertex.getElement();
    }

    @Override
    public TerminalBase insertTerminal(TerminalBase terminal) {
        if (terminal != null) {
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
            shuttleCapacity.put(shuttle, this.getCapacity());
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
        var originVertex = terminalVertices.get(origin);
        var destinationVertex = terminalVertices.get(destination);
        if (originVertex == null || destinationVertex == null) {
            return null;
        }

      var shortestPathMap =   GraphUtilities.shortestPathBFS(this.adjacencyMap, originVertex, destinationVertex, (e) -> {
          return e.getTime();
      });

        // Sort in order traveled
        var sortedMap = shortestPathMap.entrySet().
                stream().
                sorted(Map.Entry.comparingByValue()).
                collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

        return getPath(destination, sortedMap);
    }

    /**
     * Helper method to build path
     *
     * @param destination the destination vertex path
     * @param sortedMap   the sorted map returned from Dijkstra's algorithm
     * @return the path
     */
    private Path getPath(TerminalBase destination, LinkedHashMap<AdjacencyMap<TerminalBase,
            ShuttleBase>.Vertex<TerminalBase>, Integer> sortedMap) {
        int totalTime = 0;
        List<TerminalBase> path = new ArrayList<>();
        var iter = sortedMap.keySet().iterator();
        var lastOrigin = iter.next();
        totalTime += lastOrigin.getElement().getWaitingTime();
        path.add(lastOrigin.getElement());
        while (iter.hasNext()) {
            var d = iter.next();
            ShuttleBase shuttle = shuttleBetween(lastOrigin.getElement(), d.getElement());
            int capacity = shuttleCapacity.get(shuttle);
            capacity--;
            shuttleCapacity.put(shuttle, capacity);
            if (capacity == 0) {
                shuttleCapacity.remove(shuttle);
                adjacencyMap.removeEdge(shuttleEdges.get(shuttle));
                shuttleEdges.remove(shuttle);
            }
            if (!d.getElement().getId().equals(destination.getId())) {
                totalTime += d.getElement().getWaitingTime();
            }
            totalTime += shuttle.getTime();
            lastOrigin = d;
            path.add(lastOrigin.getElement());
        }

        return new Path(path, totalTime);
    }

    /**
     * Helper method that returns the edge between two vertices
     *
     * @param origin      vertex
     * @param destination vertex
     * @return the edge between the two vertices or null
     */
    private ShuttleBase shuttleBetween(TerminalBase origin, TerminalBase destination) {
        var originVertex = terminalVertices.get(origin);
        var destinationVertex = terminalVertices.get(destination);
        if (originVertex == null || destinationVertex == null) {
            return null;
        }

        var edge = adjacencyMap.getEdge(originVertex, destinationVertex);
        if (edge != null) {
            return edge.getElement();
        }
        return null;
    }


    @Override
    public Path findFastestPath(TerminalBase origin, TerminalBase destination) {
        var originVertex = terminalVertices.get(origin);
        var destinationVertex = terminalVertices.get(destination);

        var fastestPathMap =
                GraphUtilities.fastestPathDijkstra(this.adjacencyMap, originVertex, destinationVertex, (e) -> {
                    return e.getTime();
                });

        // Sort in order traveled
        var sortedMap = fastestPathMap.entrySet().
                stream().
                sorted(Map.Entry.comparingByValue()).
                collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

        return getPath(destination, sortedMap);
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

}

/**
 *  Represents a position in a list
 * @param <E> type
 */
interface Position<E> {
    E getElement() throws IllegalStateException;
}

/**
 *  A Generic minimal implementation of a positional list
 *  to be used with AdjacencyMap
 * @param <E> type
 */
class PositionalLinkedList<E> {

    private  class Node<E> implements Position<E> {
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


    public int size() { return size; }

    public boolean isEmpty() { return size == 0; }

    private Position<E> addBetween(E e, Node<E> pred, Node<E> succ) {
        Node<E> newest = new Node<>(e, pred, succ);
        pred.setNext(newest);
        succ.setPrev(newest);
        size++;
        return newest;
    }

    public Position<E> addLast(E e) {
        return addBetween(e, tail.getPrev(), tail);
    }


    public E remove(Position<E> p) {
        Node<E> node = (Node<E>) p;
        Node<E> previous = node.getPrev();
        Node<E> preceding = node.getNext();
        previous.setNext(preceding);
        preceding.setPrev(previous);
        size--;
        E answer = node.getElement();
        node.setElement(null);
        node.setNext(null);
        node.setPrev(null);
        return answer;
    }

    public void forEach(Consumer<E> c) {
        Node<E> walk = head.getNext();
        while (walk != tail) {
            c.accept(walk.getElement());
            walk = walk.getNext();
        }
    }

}


/**
 *
 * Implemented using Lecture notes and course resources
 *
 * @param <V> Generic class for vertex
 * @param <E> Generic class for Edge
 */
class AdjacencyMap<V, E> {
    private final PositionalLinkedList<Vertex<V>> vertices = new PositionalLinkedList<>();
    private final PositionalLinkedList<Edge<E>> edges = new PositionalLinkedList<>();


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


        public Position<Edge<E>> getPosition() {
            return position;
        }

        public void setPosition(Position<Edge<E>> position) {
            this.position = position;
        }

        public Vertex<V>[] getEndpoints() {
            return endpoints;
        }

    }

    public Edge<E> getEdge(Vertex<V> u, Vertex<V> v) {
        return u.getOutgoing().get(v);
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

    public PositionalLinkedList<Vertex<V>> getVertices() {
        return vertices;
    }

    public PositionalLinkedList<Edge<E>> getEdges() {
        return edges;
    }
}

/**
 *
 *
 * A Generic bare bone Heap Adaptable Priority Queue to help with Dijkstra's Algorithm
 *
 * @param <K>
 * @param <V>
 */
class PriorityQueue<K,V> {
    private ArrayList<Entry<K,V>> heap = new ArrayList<>();
    private Comparator<K> comparator;

    public PriorityQueue() {
       this.comparator = (a, b) -> ((Comparable<K>) a).compareTo(b);
    }

    public int size() {
        return heap.size();
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    /* Swap the entries at indices i and j in the array list */
    private void heapSwap(int i, int j) {
        Entry<K,V> temporary = heap.get(i);
        heap.set(i, heap.get(j));
        heap.set(j, temporary);
    }

    private void swap(int i, int j) {
        heapSwap(i,j); // perform heap swap
        // Reset the  entry's index
        heap.get(i).setIndex(i);
        heap.get(j).setIndex(j);
    }

    /**
     *  Restores the heap property through moving the entry at index j upward or downward
     * @param j index
     */
    private void bubble(int j) {
        if (j > 0 && compareEntryKeys(heap.get(j), heap.get(parentOf(j))) < 0)
            upHeap(j);
        else
            downHeap(j);                   //  it may not need to move
    }

    public Entry<K,V> insert(K key, V value) {
        Entry<K,V> newestEntry = new Entry<>(key, value, heap.size());
        heap.add(newestEntry);                // adds new entry to the end of the list
        upHeap(heap.size() - 1);         // perform up heap on newly added entry
        return newestEntry;
    }

    public void remove(Entry<K,V> entry)  {
        if (entry != null) {
            int j = entry.getIndex();
            if (j == heap.size() - 1)        // entry is in last position
                heap.remove(heap.size() - 1);  // so we just remove it
            else {
                swap(j, heap.size() - 1);      // swap entry with last position
                heap.remove(heap.size() - 1);  // then we get rid of it
                bubble(j);                     // then fix entry displaced by the swapping
            }
        }
    }

    public Entry<K,V> removeMin() {
        if (heap.isEmpty()) return null;
        Entry<K,V> entry = heap.get(0);
        swap(0, heap.size() - 1);
        heap.remove(heap.size() - 1);
        downHeap(0);
        return entry;
    }



    public void replaceKey(Entry<K,V> entry, K key) {
        if (entry != null && key != null) {
            entry.setKey(key);
            bubble(entry.getIndex());
        }
    }

    private int compareEntryKeys(Entry<K,V> a, Entry<K,V> b) {
        return comparator.compare(a.getKey(), b.getKey());
    }


    // Utility methods
    public int parentOf(int j) { return (j-1) / 2; }
    public int leftOf(int j) { return 2*j + 1; }
    public int rightOf(int j) { return 2*j + 2; }
    public boolean hasLeft(int j) { return leftOf(j) < heap.size(); }
    public boolean hasRight(int j) { return rightOf(j) < heap.size(); }

    protected void upHeap(int j) {
        while (j > 0) {
            int p = parentOf(j);
            if (compareEntryKeys(heap.get(j), heap.get(p)) >= 0) break;
            swap(j, p);
            j = p;
        }
    }

    protected void downHeap(int j) {
        while (hasLeft(j)) {
            int leftIndex = leftOf(j);
            int smallestChildIndex = leftIndex;
            if (hasRight(j)) {
                int rightIndex = rightOf(j);
                if (compareEntryKeys(heap.get(leftIndex), heap.get(rightIndex)) > 0)
                    smallestChildIndex = rightIndex;
            }
            if (compareEntryKeys(heap.get(smallestChildIndex), heap.get(j)) >= 0)
                break;
            swap(j, smallestChildIndex);
            j = smallestChildIndex;
        }
    }

    public static class Entry<K,V> {
        private K key;
        private V value;
        int index;

        public Entry(K key, V value, int index) {
            this.key = key;
            this.value = value;
            this.index = index;
        }

        public K getKey() {
            return key;
        }
        public V getValue() {
            return value;
        }

        public void setKey(K key) {
            this.key = key;
        }

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }
    }
}

/**
 *  Utility class containing graph algorithms
 */
class GraphUtilities {

    public static <V, E> Map<AdjacencyMap<V,E>.Vertex<V>, Integer>
    fastestPathDijkstra(AdjacencyMap<V,E> g, AdjacencyMap<V,E>.Vertex<V> src,
                        AdjacencyMap<V,E>.Vertex<V> dest,
                        Function<E, Integer> edgeConverter) {
        Map<AdjacencyMap<V,E>.Vertex<V>, Integer> d = new HashMap<>();
        Map<AdjacencyMap<V,E>.Vertex<V>, AdjacencyMap<V,E>.Vertex<V>> parent = new HashMap<>();
        Map<AdjacencyMap<V, E>.Vertex<V>, Integer> cloud = new HashMap<>();
        PriorityQueue<Integer, AdjacencyMap<V,E>.Vertex<V>> pq;
        pq = new PriorityQueue<>();
        Map<AdjacencyMap<V,E>.Vertex<V>,PriorityQueue.Entry<Integer,AdjacencyMap<V,E>.Vertex<V>>> pqTokens;
        pqTokens = new HashMap<>();

        /*
         * For all vertices, give the src vertices a distance of 0
         * all others get a distance of INFINITY (INT_MAX_VALUE);
         *
         * we use internal iteration of PositionalList
         */
        g.getVertices().forEach(v -> {
            if (v == src) {
                d.put(v, 0);
                parent.put(v, null);
            } else {
                d.put(v, Integer.MAX_VALUE);
            }
            pqTokens.put(v, pq.insert(d.get(v), v));
        });

        /*
         * We begin adding reachable vertices to the cloud
         */
        while (!pq.isEmpty()) {
            PriorityQueue.Entry<Integer, AdjacencyMap<V,E>.Vertex<V>> entry = pq.removeMin();
            int key = entry.getKey();
            AdjacencyMap<V,E>.Vertex<V> u = entry.getValue();
            cloud.put(u, key);                             // the  distance to u vertex (actual)
            pqTokens.remove(u);                            // u is removed from the priorty queue
            for (AdjacencyMap<V,E>.Edge<E> e : g.outgoingEdges(u)) {
                AdjacencyMap<V,E>.Vertex<V> v = g.opposite(u,e);
                if (cloud.get(v) == null) {
                    // do relaxation on edge (u,v)
                    int weight = edgeConverter.apply(e.getElement());
                    if (d.get(u) + weight < d.get(v)) {              // is it a better path to v
                        d.put(v, d.get(u) + weight);                   // then update the distance
                        pq.replaceKey(pqTokens.get(v), d.get(v));   // update the pq entry
                        parent.put(v, u);
                    }
                }
            }
        }

        Map<AdjacencyMap<V, E>.Vertex<V>, Integer> result = new HashMap<>();
        result.put(src, cloud.get(src));
        var currentVertex = dest;
        while(parent.get(currentVertex) != null) {
            result.put(currentVertex, cloud.get(currentVertex));
            currentVertex = parent.get(currentVertex);
        }

        return result; // the vertices containing the fastest path
    }


    public static <V,E>  Map<AdjacencyMap<V,E>.Vertex<V>, Integer> shortestPathBFS(AdjacencyMap<V,E> g,
                                                                                   AdjacencyMap<V,E>.Vertex<V> s,
                                                                                   AdjacencyMap<V,E>.Vertex<V> d,
                                                                                   Function<E, Integer> edgeConverter) {
        Set<AdjacencyMap<V,E>.Vertex<V>> known = new HashSet<>();
        PositionalLinkedList<AdjacencyMap<V,E>.Vertex<V>> level = new PositionalLinkedList<>();
        known.add(s);
        Map<AdjacencyMap<V,E>.Vertex<V>, Integer> dis = new HashMap<>();
        g.getVertices().forEach(v -> {
            dis.put(v, 0);
        });
        Map<AdjacencyMap<V,E>.Vertex<V>, AdjacencyMap<V,E>.Vertex<V>> parent = new HashMap<>();
        parent.put(s, null);
        level.addLast(s);
        while (!level.isEmpty()) {
            PositionalLinkedList<AdjacencyMap<V,E>.Vertex<V>> nextLevel = new PositionalLinkedList<>();
            level.forEach(u -> {
                        for (AdjacencyMap<V,E>.Edge<E> e : g.outgoingEdges(u)) {
                            AdjacencyMap<V, E>.Vertex<V> v = g.opposite(u, e);
                            if (!known.contains(v)) {
                                parent.put(v, u);
                                int weight = edgeConverter.apply(e.getElement());
                                dis.put(v, dis.get(u) + weight);
                                known.add(v);
                                nextLevel.addLast(v);
                            }
                        }
            });
            level = nextLevel;
        }

        Map<AdjacencyMap<V, E>.Vertex<V>, Integer> result = new HashMap<>();
        result.put(s, dis.get(s));
        var currentVertex = d;
        while(parent.get(currentVertex) != null) {
            result.put(currentVertex, dis.get(currentVertex));
            currentVertex = parent.get(currentVertex);
        }

        return result; // the vertices containing the shortest path
    }


}