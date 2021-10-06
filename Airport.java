import java.util.*;
import java.util.stream.Collectors;

public class Airport extends AirportBase {

    /* Implement all the necessary methods of the Airport here */

    static class Terminal extends TerminalBase {

        /* Implement all the necessary methods of the Terminal here */
    }

    static class Shuttle extends ShuttleBase {

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
