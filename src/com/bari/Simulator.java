package com.bari;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;

public class Simulator {

    public NetworkGraph graph;
    public boolean running = false;
    public HashSet<Thread> workingThreads = new HashSet<>();
    private ReentrantLock lock = new ReentrantLock(true);
    private Workspace parentWorkspace;

    Simulator(NetworkGraph graph, Workspace parentWorkspace) {
        this.graph = graph;
        this.parentWorkspace = parentWorkspace;
    }

    void simulateOSPF() {
        this.workingThreads.clear();
        for (NGNode node : this.graph.getNodes()) {
            Thread t = new Thread(new Runnable() {

                @Override
                public void run() {
                    running = true;
                    NetworkGraph ng = new NetworkGraph();
                    ForwardingTable forwardingTable = new ForwardingTable();
                    int sendCounter = (int) (graph.getNodes().size() * 1.5);
                    int nodeCount = 0;
                    node.name = "" + nodeCount++;
                    ng.getNodes().add(node);

                    for (NGConnection conn : node.getConnections()) {
                        conn.node.name = "" + nodeCount++;
                        ng.getNodes().add(conn.node);
                    }
                    while (running) {
                        String excludedNodeIP = null;
                        lock.lock();
                        while (sendCounter > 0 && !node.msgQueue.isEmpty()) {
                            // Message example: MSGTYPE ip0:TYPE ip0:TYPE:ip1:TYPE:cost:interfaceNumber
                            // ip1:TYPE:ip0:TYPE:cost:interfaceNumber
                            // System.out.printf("Node: %s, received msg: %s\n", node.name,
                            // node.msgQueue.peek());
                            String[] msgElements = node.msgQueue.poll().split(" ");
                            String msgType = msgElements[0];
                            if (msgType.equals("GRPH")) {
                                String mainNodeIP = msgElements[1].split(":")[0];
                                ShapeType mainNodeType = (msgElements[1].split(":")[1].equals("ROUTER"))
                                        ? ShapeType.ROUTER
                                        : ShapeType.HOST;
                                excludedNodeIP = mainNodeIP;
                                NGNode mainNode = null;
                                for (NGNode n : ng.getNodes()) {
                                    if (NGNode.getFormattedIP(n.ipAddress).equals(mainNodeIP)) {
                                        mainNode = n;
                                        break;
                                    }
                                }
                                if (mainNode == null)
                                    mainNode = new NGNode(mainNodeType, "" + nodeCount++, NGNode.parseIP(mainNodeIP),
                                            0);

                                for (String elem : toList(msgElements).subList(2, msgElements.length)) {
                                    String fromElemIp = elem.split(":")[0];
                                    ShapeType fromElemType = (elem.split(":")[1].equals("ROUTER")) ? ShapeType.ROUTER
                                            : ShapeType.HOST;
                                    String toElemIp = elem.split(":")[2];
                                    ShapeType toElemType = (elem.split(":")[3].equals("ROUTER")) ? ShapeType.ROUTER
                                            : ShapeType.HOST;
                                    int elemCost, elemInterfaceNumber;
                                    try {
                                        elemCost = Integer.parseInt(elem.split(":")[4]);
                                        elemInterfaceNumber = Integer.parseInt(elem.split(":")[5]);
                                    } catch (NumberFormatException e) {
                                        e.printStackTrace();
                                        continue;
                                    }

                                    NGNode fromNewNode = null,
                                            toNewNode = null;
                                    for (NGNode n : ng.getNodes()) {
                                        if (NGNode.getFormattedIP(n.ipAddress).equals(fromElemIp)) {
                                            fromNewNode = n;
                                        }
                                        if (NGNode.getFormattedIP(n.ipAddress).equals(toElemIp)) {
                                            toNewNode = n;
                                        }

                                        if (fromNewNode != null && toNewNode != null)
                                            break;
                                    }
                                    if (fromNewNode == null)
                                        fromNewNode = new NGNode(fromElemType, "" + nodeCount++,
                                                NGNode.parseIP(fromElemIp), 0);
                                    if (toNewNode == null)
                                        toNewNode = new NGNode(toElemType, "" + nodeCount++, NGNode.parseIP(toElemIp),
                                                0);

                                    fromNewNode.connect(toNewNode, elemCost, elemInterfaceNumber);
                                    toNewNode.connect(fromNewNode, elemCost, elemInterfaceNumber);

                                    ng.getNodes().add(fromNewNode);
                                    ng.getNodes().add(toNewNode);
                                }

                                ng.getNodes().add(mainNode);
                            } else if (msgType.equals("NUPDT")) {
                            }
                            sendCounter--;
                        }

                        String message = new String(String.format("GRPH %s:%s", NGNode.getFormattedIP(node.ipAddress),
                                node.type.toString()));
                        for (NGNode n : ng.getNodes()) {
                            for (NGConnection conn : n.getConnections()) {
                                String chseq = String.format(" %s:%s:%s:%s:%d:%d", NGNode.getFormattedIP(n.ipAddress),
                                        n.type.toString(),
                                        NGNode.getFormattedIP(conn.node.ipAddress), conn.node.type.toString(),
                                        conn.cost, conn.interfaceNumber);
                                if (!message.contains(chseq))
                                    message = message.concat(chseq);
                            }
                        }
                        for (NGConnection connection : node.getConnections()) {
                            if (NGNode.getFormattedIP(connection.node.ipAddress).equals(excludedNodeIP))
                                continue;
                            connection.node.msgQueue.offer(message);
                            // System.out.printf("Node: %s, sent message to: %s\n", node.name,
                            // connection.node.name);
                        }

                        if (ng.getNodes().size() > 1 && sendCounter > 0) {
                            if (node.type == ShapeType.ROUTER) {
                                for (NGNode n1 : ng.getNodes()) {
                                    if (n1.type != ShapeType.HOST)
                                        continue;

                                    for (NGNode n2 : ng.getNodes()) {
                                        if (n2.type != ShapeType.HOST
                                                || n2.getFormattedIP().equals(n1.getFormattedIP()))
                                            continue;

                                        ArrayList<String> route = getBestRoute(ng, n1, n2);
                                        if (!route.contains(node.getFormattedIP()))
                                            continue;

                                        String sourceAddress = n1.getFormattedIP(),
                                                destinationAddress = n2.getFormattedIP(),
                                                nextAddress;
                                        nextAddress = route.get(route.indexOf(node.getFormattedIP()) + 1);
                                        Integer interfaceNumber = null;
                                        for (NGConnection conn : node.getConnections()) {
                                            if (conn.node.getFormattedIP().equals(nextAddress)) {
                                                interfaceNumber = conn.interfaceNumber;
                                                break;
                                            }
                                        }
                                        forwardingTable.insert(sourceAddress, destinationAddress, interfaceNumber);
                                    }
                                }
                            } else if (node.type == ShapeType.HOST) {
                                for (NGNode n : ng.getNodes()) {
                                    if (n.type != ShapeType.HOST || n.getFormattedIP().equals(node.getFormattedIP()))
                                        continue;

                                    ArrayList<String> route = getBestRoute(ng, node, n);
                                    if (!route.contains(node.getFormattedIP()))
                                        continue;

                                    String sourceAddress = node.getFormattedIP(),
                                            destinationAddress = n.getFormattedIP(),
                                            nextAddress = route.get(route.indexOf(node.getFormattedIP()) + 1);
                                    Integer interfaceNumber = null;
                                    for (NGConnection conn : node.getConnections()) {
                                        if (conn.node.getFormattedIP().equals(nextAddress)) {
                                            interfaceNumber = conn.interfaceNumber;
                                            break;
                                        }
                                    }
                                    forwardingTable.insert(sourceAddress, destinationAddress, interfaceNumber);
                                }
                            }
                            for (WSShape shape : parentWorkspace.mainCanvas.shapes) {
                                if (NGNode.getFormattedIP(shape.ipAddress).equals(node.getFormattedIP())) {
                                    shape.forwardingTable = forwardingTable.copy();
                                    break;
                                }
                            }
                        }

                        // System.out.println("=> For node: " + node.name + " discovered nodes: " + ng.getNodes().size());

                        /*
                         * System.out.println("=> Network graph for node: " +
                         * NGNode.getFormattedIP(node.ipAddress));
                         * for (NGNode n : ng.getNodes()) {
                         * System.out.print(".. Node: " + NGNode.getFormattedIP(n.ipAddress) +
                         * ", connected to:");
                         * for (NGConnection conn : n.getConnections())
                         * System.out.print(" " + NGNode.getFormattedIP(conn.node.ipAddress));
                         * System.out.println();
                         * }
                         */

                        lock.unlock();

                        try {
                            Thread.sleep(randomInt(500, 1000));
                        } catch (InterruptedException e) {
                        }
                    }
                    running = false;
                }
            }, node.name + "-Thread");
            this.workingThreads.add(t);
        }

        for (Thread thread : this.workingThreads) {
            thread.start();
        }
    }

    void simulateBGP() {
        this.running = true;
    }

    void stop() {
        this.running = false;
    }

    public static int randomInt(int min, int max) {
        if (min > max) {
            int tmp = min;
            min = max;
            max = tmp;
        }

        Random random = new Random();
        return random.nextInt((max - min) + 1) + min;
    }

    public static ArrayList<String> getBestRoute(NetworkGraph g, NGNode from, NGNode to) {
        ArrayList<String> result = new ArrayList<>();
        ArrayList<String> nonVisited = new ArrayList<>();
        HashMap<String, Double> costsTable = new HashMap<>();
        HashMap<String, String> parentsTable = new HashMap<>();
        for (NGNode node : g.getNodes()) {
            nonVisited.add(node.getFormattedIP());
            costsTable.put(node.getFormattedIP(), (node.equals(from)) ? 0.0 : Double.POSITIVE_INFINITY);
        }

        NGNode currentNode = from;
        NGNode currentNodeParent = null;
        while (!nonVisited.isEmpty() && !currentNode.getFormattedIP().equals(to.getFormattedIP())) {
            nonVisited.remove(currentNode.getFormattedIP());
            // System.out.println("Current node: " + currentNode.getFormattedIP() + ", to
            // node: " + to.getFormattedIP());

            double minCost = Double.POSITIVE_INFINITY;
            NGNode neighborNode = null;

            for (NGConnection conn : shuffleHashSet(currentNode.getConnections())) {
                if (currentNodeParent != null && conn.node.getFormattedIP().equals(currentNodeParent.getFormattedIP()))
                    continue;

                if (conn.cost < minCost) {
                    minCost = conn.cost;
                    neighborNode = conn.node;
                }
            }

            if (neighborNode == null) {
                for (NGNode n : g.getNodes()) {
                    if (!n.getFormattedIP().equals(to.getFormattedIP())) {
                        currentNode = n;
                        break;
                    }
                }
                currentNodeParent = null;
                continue;
            }

            for (NGConnection conn : currentNode.getConnections()) {
                if (currentNodeParent != null && conn.node.getFormattedIP().equals(currentNodeParent.getFormattedIP()))
                    continue;

                try {
                    Double m = costsTable.get(currentNode.getFormattedIP());
                    if (m + conn.cost < costsTable.get(conn.node.getFormattedIP())) {
                        costsTable.put(conn.node.getFormattedIP(), m + conn.cost);
                        parentsTable.put(conn.node.getFormattedIP(), currentNode.getFormattedIP());
                    }
                } catch (NullPointerException e) {
                    return new ArrayList<>();
                }
            }

            currentNodeParent = currentNode;
            currentNode = neighborNode;

            Double m = costsTable.get(currentNode.getFormattedIP());
            if (m + minCost < costsTable.get(currentNode.getFormattedIP())) {
                costsTable.replace(currentNode.getFormattedIP(), m + minCost);
                parentsTable.put(currentNode.getFormattedIP(), currentNodeParent.getFormattedIP());
            }
        }

        /*
         * System.out.println("=== COSTs:");
         * for (String item : costsTable.keySet()) {
         * System.out.println(item + ": " + costsTable.get(item));
         * }
         * 
         * System.out.println("=== PARENTs:");
         * for (String item : parentsTable.keySet()) {
         * System.out.println(item + ": " + parentsTable.get(item));
         * }
         * System.out.println("============");
         */

        String currentNodeIp = to.getFormattedIP();
        while (currentNodeIp != null) {
            result.add(currentNodeIp);
            currentNodeIp = parentsTable.get(currentNodeIp);
        }
        Collections.reverse(result);
        return result;
    }

    private static ArrayList<String> toList(String[] arr) {
        ArrayList<String> list = new ArrayList<>();
        for (String string : arr) {
            list.add(string);
        }
        return list;
    }

    private static ArrayList<NGConnection> shuffleHashSet(HashSet<NGConnection> set) {
        ArrayList<NGConnection> lst = new ArrayList<>(set);
        Collections.shuffle(lst);
        return lst;
    }
}
