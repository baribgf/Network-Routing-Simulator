package com.bari;

import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ConcurrentLinkedQueue;

class NGConnection {

    public NGNode node;
    public int cost;
    public int interfaceNumber;

    NGConnection(NGNode node, int cost, int interfaceNumber) {
        this.node = node;
        this.cost = cost;
        this.interfaceNumber = interfaceNumber;
    }
}

class NGNode {

    public ShapeType type;
    public String name;
    public BitSet ipAddress;
    public int ASID;
    public ConcurrentLinkedQueue<String> msgQueue = new ConcurrentLinkedQueue<>();
    public HashMap<BitSet, Integer> forwardingTable;
    private HashSet<NGConnection> connections = new HashSet<>();

    NGNode(ShapeType type, String name, BitSet ipAddress, int asid) {
        this.type = type;
        this.name = name;
        this.ipAddress = ipAddress;
        this.ASID = asid;
    }

    public void connect(NGNode node, int cost, int interfaceNumber) {
        if (this.equals(node))
            return;
        
        for (NGConnection connection : this.connections) {
            if (connection.node.equals(node) || NGNode.getFormattedIP(connection.node.ipAddress).equals(node.getFormattedIP()))
                return;
        }
        
        this.connections.add(new NGConnection(node, cost, interfaceNumber));
    }

    public void disconnect(NGNode node) {
        for (NGConnection connection : this.connections) {
            if (connection.node.equals(node)) {
                this.connections.remove(connection);
                break;
            }
        }
    }

    public HashSet<NGConnection> getConnections() {
        return this.connections;
    }

    public String getFormattedIP() {
        return NGNode.getFormattedIP(this.ipAddress);
    }

    public static String getFormattedIP(BitSet ipAddress) {
        int[] ba = ipBitSetToIntArray(ipAddress);
        return String.format("%d.%d.%d.%d", ba[0], ba[1], ba[2], ba[3]);
    }

    public static int[] ipBitSetToIntArray(BitSet bitSet) {
        byte[] firstRawByte = mirrorByteSet(bitSet.get(0, 8)).toByteArray(),
                secondRawByte = mirrorByteSet(bitSet.get(8, 16)).toByteArray(),
                thirdRawByte = mirrorByteSet(bitSet.get(16, 24)).toByteArray(),
                fourthRawByte = mirrorByteSet(bitSet.get(24, 32)).toByteArray();

        return new int[] {
                (firstRawByte.length > 0) ? Byte.toUnsignedInt(firstRawByte[0]) : 0,
                (secondRawByte.length > 0) ? Byte.toUnsignedInt(secondRawByte[0]) : 0,
                (thirdRawByte.length > 0) ? Byte.toUnsignedInt(thirdRawByte[0]) : 0,
                (fourthRawByte.length > 0) ? Byte.toUnsignedInt(fourthRawByte[0]) : 0
        };
    }

    public static BitSet parseIP(String ipString) {
        String[] strArr = ipString.split("\\.");
        BitSet bitSet = new BitSet();
        int index = 0;
        for (String s : strArr) {
            String binStr = String.format("%8s", Integer.toBinaryString(Integer.parseInt(s))).replace(' ', '0');
            for (char bitChar : binStr.toCharArray()) {
                if (bitChar == '1')
                    bitSet.set(index);
                index++;
            }
        }
        return bitSet;
    }

    public String toString() {
        return String.format("NGNode(%s, %s, %s, %d)", this.type.toString(), this.name,
                getFormattedIP(this.ipAddress), this.ASID);
    }

    private static BitSet mirrorByteSet(BitSet bitSet) {
        BitSet mirrored = new BitSet();
        for (int i = 0; i < bitSet.length(); i++) {
            if (bitSet.get(i))
                mirrored.set(8 - 1 - i);
        }
        return mirrored;
    }
}

public class NetworkGraph {

    private HashSet<NGNode> nodes = new HashSet<>();

    NetworkGraph() {
    }

    public HashSet<NGNode> getNodes() {
        return this.nodes;
    }
}
