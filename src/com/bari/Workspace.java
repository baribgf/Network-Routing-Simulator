package com.bari;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Scanner;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicBoolean;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

import javax.imageio.ImageIO;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.w3c.dom.Document;

enum ShapeType {
    ROUTER,
    HOST,
    NONE
}

enum WorkspaceTheme {
    LIGHT,
    DARK
}

enum WorkspaceMode {
    DRAWING,
    LINKING,
    NONE
}

enum SimulationType {
    OSPF,
    BGP
}

class Vector2D {

    public int x, y;

    Vector2D(int x, int y) {
        this.x = x;
        this.y = y;
    }
}

class ImageDimensionsMap {

    public static Vector2D get(ShapeType key) {
        switch (key) {
            case ROUTER:
                return new Vector2D(120, 60);
            case HOST:
                return new Vector2D(123, 82);
            default:
                return new Vector2D(0, 0);
        }
    }
}

class ForwardingTableEntry {

    String sourceAddress, destinationAddress;
    int interfaceNumber;

    ForwardingTableEntry(String sourceAddress, String destinationAddress, int interfaceNumber) {
        this.sourceAddress = sourceAddress;
        this.destinationAddress = destinationAddress;
        this.interfaceNumber = interfaceNumber;
    }
}

class ForwardingTable {

    private HashSet<ForwardingTableEntry> tableArray = new HashSet<>();

    ForwardingTable() {
    }

    public void insert(String sourceAddress, String destinationAddress, int interfaceNumber) {
        for (ForwardingTableEntry e : this.tableArray) {
            if (e.sourceAddress.equals(sourceAddress) && e.destinationAddress.equals(destinationAddress)) {
                if (e.interfaceNumber != interfaceNumber)
                    e.interfaceNumber = interfaceNumber;

                return;
            }
        }
        this.tableArray.add(new ForwardingTableEntry(sourceAddress, destinationAddress, interfaceNumber));
    }

    public void remove(String sourceAddress, String destinationAddress) {
        for (ForwardingTableEntry e : this.tableArray) {
            if (e.sourceAddress.equals(sourceAddress) && e.destinationAddress.equals(destinationAddress)) {
                this.tableArray.remove(e);
                break;
            }
        }
    }

    public HashSet<ForwardingTableEntry> getTableArray() {
        return this.tableArray;
    }

    public ForwardingTable copy() {
        ForwardingTable newForwardingTable = new ForwardingTable();
        for (ForwardingTableEntry e : this.tableArray)
            newForwardingTable.insert(e.sourceAddress, e.destinationAddress, e.interfaceNumber);
        return newForwardingTable;
    }
}

class WSShape {

    public ShapeType type;
    public int x, y, mousePointX, mousePointY;
    public String name;
    public BitSet ipAddress = new BitSet(32);
    public int ASID;
    public ArrayList<WSLink> connectedLinks = new ArrayList<>();
    public ForwardingTable forwardingTable = new ForwardingTable();

    WSShape(ShapeType type, String name, BitSet ipAddress, int asid, int x, int y) {
        this.type = type;
        this.name = name;
        this.x = x;
        this.y = y;
        this.mousePointX = 0;
        this.mousePointY = 0;
        this.ipAddress = ipAddress;
        this.ASID = asid;
    }

    Vector2D center() {
        return new Vector2D(this.x + ImageDimensionsMap.get(this.type).x / 2,
                this.y + ImageDimensionsMap.get(this.type).y / 2);
    }

    LinkedHashMap<String, Object> getProperties() {
        LinkedHashMap<String, Object> props = new LinkedHashMap<>();
        props.put("Name", this.name);
        props.put("IP", NGNode.getFormattedIP(this.ipAddress));
        props.put("AS Id", this.ASID);
        return props;
    }
}

class WSLink {

    public WSShape fromShape, toShape;
    public int cost;

    WSLink(WSShape fromShape, WSShape toShape, int cost) {
        this.fromShape = fromShape;
        this.toShape = toShape;
        this.cost = cost;
    }

    public Vector2D fromCenter() {
        int fromCenterX = this.fromShape.x + ImageDimensionsMap.get(this.fromShape.type).x / 2;
        int fromCenterY = this.fromShape.y + ImageDimensionsMap.get(this.fromShape.type).y / 2;
        return new Vector2D(fromCenterX, fromCenterY);
    }

    public Vector2D toCenter() {
        int toCenterX = this.toShape.x + ImageDimensionsMap.get(this.toShape.type).x / 2;
        int toCenterY = this.toShape.y + ImageDimensionsMap.get(this.toShape.type).y / 2;
        return new Vector2D(toCenterX, toCenterY);
    }
}

class ShapesAndLinksPair {
    HashSet<WSShape> shapes;
    HashSet<WSLink> links;

    ShapesAndLinksPair(HashSet<WSShape> shapes, HashSet<WSLink> links) {
        this.shapes = shapes;
        this.links = links;
    }
}

class WSCanvas extends JPanel {

    HashSet<WSShape> shapes = new HashSet<>();
    HashSet<WSLink> links = new HashSet<>();
    Vector2D visualFrom = null;
    Vector2D visualTo = null;
    Image routerImageNormal, routerImageSelected, hostImageNormal, hostImageSelected;
    HashSet<WSShape> selectedShapes = new HashSet<>();
    HashSet<WSLink> selectedLinks = new HashSet<>();
    Color textColor = Color.BLACK;
    Color bgColor = Color.WHITE;
    Color linkColor = Color.BLACK;
    Color selectedLinkColor = Color.BLUE;

    WSCanvas() {
        try {
            this.routerImageNormal = ImageIO.read(new File("../assets/router-normal.png")).getScaledInstance(
                    ImageDimensionsMap.get(ShapeType.ROUTER).x,
                    ImageDimensionsMap.get(ShapeType.ROUTER).y, Image.SCALE_DEFAULT);
            this.routerImageSelected = ImageIO.read(new File("../assets/router-selected.png")).getScaledInstance(
                    ImageDimensionsMap.get(ShapeType.ROUTER).x,
                    ImageDimensionsMap.get(ShapeType.ROUTER).y, Image.SCALE_DEFAULT);
            this.hostImageNormal = ImageIO.read(new File("../assets/host-normal.png")).getScaledInstance(
                    ImageDimensionsMap.get(ShapeType.HOST).x,
                    ImageDimensionsMap.get(ShapeType.HOST).y, Image.SCALE_DEFAULT);
            this.hostImageSelected = ImageIO.read(new File("../assets/host-selected.png")).getScaledInstance(
                    ImageDimensionsMap.get(ShapeType.HOST).x,
                    ImageDimensionsMap.get(ShapeType.HOST).y, Image.SCALE_DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.setBackground(this.bgColor);
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setStroke(new BasicStroke(2));

        Color tmpColor = g.getColor();
        g.setColor(linkColor);
        for (WSLink link : this.links) {
            int x1 = link.fromCenter().x,
                    y1 = link.fromCenter().y,
                    x2 = link.toCenter().x,
                    y2 = link.toCenter().y;
            g2d.drawLine(x1, y1, x2, y2);
            g.drawString("c=" + String.valueOf(link.cost), (x1 + x2) / 2, (y1 + y2) / 2);
        }

        if (this.visualFrom != null && this.visualTo != null)
            g2d.drawLine(visualFrom.x, visualFrom.y, visualTo.x, visualTo.y);
        g.setColor(tmpColor);

        tmpColor = g.getColor();
        g.setColor(selectedLinkColor);
        for (WSLink selectedLink : this.selectedLinks) {
            int x1 = selectedLink.fromCenter().x,
                    y1 = selectedLink.fromCenter().y,
                    x2 = selectedLink.toCenter().x,
                    y2 = selectedLink.toCenter().y;
            g.drawLine(x1, y1, x2, y2);
            g.drawString("c=" + String.valueOf(selectedLink.cost), (x1 + x2) / 2, (y1 + y2) / 2);
        }
        g.setColor(tmpColor);

        for (WSShape shape : this.shapes) {
            switch (shape.type) {
                case ROUTER:
                    g.drawImage(
                            (this.selectedShapes.contains(shape)) ? this.routerImageSelected : this.routerImageNormal,
                            shape.x, shape.y, null);
                    break;
                case HOST:
                    g.drawImage(
                            (this.selectedShapes.contains(shape)) ? this.hostImageSelected : this.hostImageNormal,
                            shape.x, shape.y, null);
                    break;
                default:
                    break;
            }
            tmpColor = g.getColor();
            g.setColor(textColor);
            g.drawString(shape.name, shape.x, shape.y);
            g.setColor(tmpColor);
        }

        for (WSLink link : this.links) {
            Vector2D from = link.fromCenter(),
                    to = link.toCenter();

            int x1 = from.x + (to.x - from.x) / 4,
                    y1 = from.y + (to.y - from.y) / 4,
                    x2 = to.x + (from.x - to.x) / 4,
                    y2 = to.y + (from.y - to.y) / 4;

            Font tmpFont = g.getFont();
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
            g.drawString(String.valueOf(link.fromShape.connectedLinks.indexOf(link)), x1, y1);
            g.drawString(String.valueOf(link.toShape.connectedLinks.indexOf(link)), x2, y2);
            g.setFont(tmpFont);
        }
    }

    public void setVisualLink(Vector2D from, Vector2D to) {
        this.visualFrom = from;
        this.visualTo = to;
        this.repaint();
    }
}

public class Workspace extends JPanel {

    public boolean ctrlPressed = false;
    public WSCanvas mainCanvas = new WSCanvas();
    public JToggleButton toggledButton = null;
    public ShapeType shapeType = ShapeType.NONE;
    public String projectSourcePath;
    public MainFrame parent;
    public SimulationType currentSimulationType;

    private BorderLayout baseLayout;
    private int routerShapeNameCounter = 0, hostShapeNameCounter = 0;
    private WSShape fromShape = null;
    private WSShape shapeToDrag = null;
    private Cursor defaultCursor = new Cursor(Cursor.DEFAULT_CURSOR);
    private Cursor placementCursor = new Cursor(Cursor.HAND_CURSOR);
    private Cursor linkingCursor = new Cursor(Cursor.CROSSHAIR_CURSOR);
    private NetworkGraph mainGraph = new NetworkGraph();
    private Simulator mainSimulator = new Simulator(mainGraph, this);
    private DocumentBuilder documentBuilder;
    private Transformer documentTransformer;
    private Document projectDocument;
    private Element projectRootElement;
    private Stack<ShapesAndLinksPair> undoStack = new Stack<>();
    private Stack<ShapesAndLinksPair> redoStack = new Stack<>();
    private WorkspaceTheme theme = WorkspaceTheme.LIGHT;
    private WorkspaceMode mode = WorkspaceMode.NONE;
    private Thread forwardingTableUpdaterThread = new Thread(new Runnable() {

        @Override
        public void run() {
            while (true) {
                if (forwardingTableUpdaterThreadRunning.get()) {
                    parent.sidePanel.setForwardingTable(forwardingTableUpdaterThreadShape.forwardingTable);
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    });
    private AtomicBoolean forwardingTableUpdaterThreadRunning = new AtomicBoolean(false);
    private WSShape forwardingTableUpdaterThreadShape;

    Workspace(MainFrame parent) {
        try {
            this.documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            this.documentTransformer = TransformerFactory.newInstance().newTransformer();
        } catch (ParserConfigurationException | TransformerConfigurationException
                | TransformerFactoryConfigurationError e) {
            e.printStackTrace();
        }
        this.forwardingTableUpdaterThread.start();
        this.documentTransformer.setOutputProperty("indent", "yes");
        this.projectDocument = this.documentBuilder.newDocument();
        this.projectRootElement = this.projectDocument.createElement("project");
        this.projectDocument.appendChild(this.projectRootElement);
        this.parent = parent;
        parent.setTitle(parent.mainTitle + " - Untitled");
        this.baseLayout = new BorderLayout();

        this.setLayout(this.baseLayout);
        this.setFocusable(true);

        this.addKeyListener(new KeyAdapter() {

            @Override
            public void keyPressed(KeyEvent event) {
                switch (event.getKeyCode()) {
                    case KeyEvent.VK_CONTROL:
                        ctrlPressed = true;
                        break;
                }
            }

            @Override
            public void keyReleased(KeyEvent event) {
                if (event.getKeyCode() == KeyEvent.VK_DELETE) {
                    deleteSelection();
                } else if (event.getKeyCode() == KeyEvent.VK_CONTROL) {
                    ctrlPressed = false;
                } else if (event.getKeyCode() == KeyEvent.VK_Z) {
                    if (ctrlPressed)
                        performUndo();
                } else if (event.getKeyCode() == KeyEvent.VK_Y) {
                    if (ctrlPressed)
                        performRedo();
                } else if (event.getKeyCode() == KeyEvent.VK_UP) {
                    if (mainCanvas.selectedLinks.size() > 0) {
                        ((WSLink) mainCanvas.selectedLinks.toArray()[0]).cost++;
                        mainCanvas.repaint();
                    }
                } else if (event.getKeyCode() == KeyEvent.VK_DOWN) {
                    if (mainCanvas.selectedLinks.size() > 0) {
                        ((WSLink) mainCanvas.selectedLinks.toArray()[0]).cost = Math
                                .max(((WSLink) mainCanvas.selectedLinks.toArray()[0]).cost - 1, 0);
                        mainCanvas.repaint();
                    }
                }
            }
        });

        this.mainCanvas.setPreferredSize(new Dimension(1000, 1000));
        this.mainCanvas.addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent event) {
                switch (event.getButton()) {
                    case MouseEvent.BUTTON1:
                        boolean found = false;
                        for (WSShape shape : mainCanvas.shapes) {
                            if (shape.x <= event.getX()
                                    && event.getX() <= shape.x + ImageDimensionsMap.get(shape.type).x
                                    && shape.y <= event.getY()
                                    && event.getY() <= shape.y + ImageDimensionsMap.get(shape.type).y) {

                                found = true;

                                parent.sidePanel.setProperties(shape.getProperties());
                                parent.sidePanel.clearForwardingTable();
                                forwardingTableUpdaterThreadShape = shape;
                                forwardingTableUpdaterThreadRunning.set(true);

                                if (event.getClickCount() == 2) {
                                    mode = (mode == WorkspaceMode.LINKING) ? WorkspaceMode.NONE : WorkspaceMode.LINKING;
                                    if (mode == WorkspaceMode.LINKING) {
                                        setMode(WorkspaceMode.LINKING);
                                        fromShape = shape;
                                    } else {
                                        setMode(WorkspaceMode.NONE);
                                        fromShape = null;
                                        mainCanvas.setVisualLink(null, null);
                                    }
                                }

                                shape.mousePointX = event.getX() - shape.x;
                                shape.mousePointY = event.getY() - shape.y;
                                shapeToDrag = shape;

                                if (mode == WorkspaceMode.LINKING) {
                                    performLinking(fromShape, shape, 1);
                                    fromShape = shape;
                                    mainCanvas.setVisualLink(null, null);
                                    clearShapeSelection();
                                } else
                                    performShapeSelection(shape);

                                break;
                            }
                        }

                        if (!found) {
                            parent.sidePanel.clearProperties();
                            forwardingTableUpdaterThreadRunning.set(false);
                            parent.sidePanel.clearForwardingTable();
                            clearShapeSelection();

                            for (WSLink link : mainCanvas.links) {
                                int dy = link.fromCenter().y - link.toCenter().y;
                                int dx = link.fromCenter().x - link.toCenter().x;
                                double linkSlope = (dx != 0) ? (double) dy / (double) dx : 1000;
                                double linkIntercept = link.toCenter().y - linkSlope * link.toCenter().x;
                                double absDist = (event.getY() - (linkSlope * event.getX() + linkIntercept))
                                        * Math.cos(Math.atan(linkSlope));

                                int fromToDistX = Math.max(Math.abs(link.fromCenter().x - link.toCenter().x), 5);
                                int fromXDistX = Math.abs(link.fromCenter().x - event.getX());
                                int toXDistX = Math.abs(link.toCenter().x - event.getX());
                                int fromToDistY = Math.max(Math.abs(link.fromCenter().y - link.toCenter().y), 5);
                                int fromXDistY = Math.abs(link.fromCenter().y - event.getY());
                                int toXDistY = Math.abs(link.toCenter().y - event.getY());

                                if (Math.abs(absDist) <= 5 &&
                                        fromToDistX > fromXDistX && fromToDistX > toXDistX
                                        && fromToDistY > fromXDistY && fromToDistY > toXDistY) {

                                    performLinkSelection(link);
                                    found = true;
                                    break;
                                }
                            }

                            if (!found)
                                clearLinkSelection();
                        }

                        break;
                }
            }

            @Override
            public void mouseReleased(MouseEvent event) {
                switch (event.getButton()) {
                    case MouseEvent.BUTTON1:
                        if (mode == WorkspaceMode.DRAWING) {
                            WSShape newShape;
                            BitSet autoIpAddress = new BitSet();
                            autoIpAddress.set(0, 32, true);
                            int x = event.getX() - ImageDimensionsMap.get(shapeType).x / 2;
                            int y = event.getY() - ImageDimensionsMap.get(shapeType).y / 2;
                            switch (shapeType) {
                                case ROUTER:
                                    newShape = new WSShape(shapeType, "Router-" + routerShapeNameCounter, autoIpAddress,
                                            0, x, y);
                                    routerShapeNameCounter++;
                                    break;
                                case HOST:
                                    newShape = new WSShape(shapeType, "Host-" + hostShapeNameCounter, autoIpAddress, 0,
                                            x, y);
                                    hostShapeNameCounter++;
                                    break;
                                default:
                                    return;
                            }
                            performDrawing(newShape);
                        }

                        break;
                }

                shapeToDrag = null;
            }
        });
        this.mainCanvas.addMouseMotionListener(new MouseMotionListener() {

            @Override
            public void mouseDragged(MouseEvent event) {
                if (shapeToDrag == null)
                    return;

                shapeToDrag.x = event.getX() - shapeToDrag.mousePointX;
                shapeToDrag.y = event.getY() - shapeToDrag.mousePointY;
                mainCanvas.repaint();
            }

            @Override
            public void mouseMoved(MouseEvent event) {
                if (fromShape != null) {
                    mainCanvas.setVisualLink(fromShape.center(),
                            new Vector2D(event.getX(), event.getY()));
                    mainCanvas.repaint();
                }
            }
        });

        JScrollPane jsp = new JScrollPane(this.mainCanvas);
        this.add(jsp, BorderLayout.CENTER);

        this.setBackground(Color.WHITE);
    }

    public void performDrawing(WSShape shape) {

        this.undoStack.add(new ShapesAndLinksPair(new HashSet<>(mainCanvas.shapes), new HashSet<>(mainCanvas.links)));
        parent.menuBar.undoAction.setEnabled(true);
        this.mainCanvas.shapes.add(shape);
        this.mainCanvas.repaint();
    }

    public void performLinking(WSShape fromShape, WSShape toShape, int cost) {
        if (fromShape.equals(toShape) || (fromShape.type == ShapeType.HOST && toShape.type == ShapeType.HOST))
            return;

        for (WSLink link : mainCanvas.links) {
            if (link.fromShape.equals(fromShape) && link.toShape.equals(toShape)
                    || link.fromShape.equals(toShape) && link.toShape.equals(fromShape))
                return;
        }
        WSLink newLink = new WSLink(fromShape, toShape, cost);
        fromShape.connectedLinks.add(newLink);
        toShape.connectedLinks.add(newLink);
        this.undoStack.add(new ShapesAndLinksPair(new HashSet<>(mainCanvas.shapes), new HashSet<>(mainCanvas.links)));
        parent.menuBar.undoAction.setEnabled(true);
        this.mainCanvas.links.add(newLink);
        mainCanvas.repaint();

        if (this.mainSimulator.running) {
            this.stopSimulation();
            for (Thread t : this.mainSimulator.workingThreads) {
                t.interrupt();
            }
            this.startSimulation(currentSimulationType);
        }
    }

    public void performShapeSelection(WSShape shape) {
        this.parent.menuBar.deleteAction.setEnabled(true);
        if (!ctrlPressed)
            this.mainCanvas.selectedShapes.clear();
        this.mainCanvas.selectedShapes.add(shape);
        this.mainCanvas.repaint();
    }

    public void performLinkSelection(WSLink link) {
        this.parent.menuBar.deleteAction.setEnabled(true);
        if (!ctrlPressed)
            this.mainCanvas.selectedLinks.clear();
        this.mainCanvas.selectedLinks.add(link);
        this.mainCanvas.repaint();
    }

    public void clearShapeSelection() {
        this.parent.menuBar.deleteAction.setEnabled(false);
        this.mainCanvas.selectedShapes.clear();
        this.mainCanvas.repaint();
    }

    public void clearLinkSelection() {
        this.parent.menuBar.deleteAction.setEnabled(false);
        this.mainCanvas.selectedLinks.clear();
        this.mainCanvas.repaint();
    }

    public void deleteSelection() {
        for (WSShape shape : this.mainCanvas.selectedShapes)
            this.mainCanvas.links.removeAll(shape.connectedLinks);

        this.mainCanvas.shapes.removeAll(this.mainCanvas.selectedShapes);
        for (WSShape shape : this.mainCanvas.shapes) {
            HashSet<WSLink> linksToRemove = new HashSet<>();
            for (WSLink link : shape.connectedLinks) {
                boolean found = false;
                for (WSShape s : mainCanvas.selectedShapes) {
                    if (NGNode.getFormattedIP(link.fromShape.ipAddress).equals(NGNode.getFormattedIP(s.ipAddress))
                            || NGNode.getFormattedIP(link.toShape.ipAddress)
                                    .equals(NGNode.getFormattedIP(s.ipAddress))) {
                        found = true;
                        break;
                    }
                }
                if (found)
                    linksToRemove.add(link);
            }
            shape.connectedLinks.removeAll(linksToRemove);
        }
        this.mainCanvas.selectedShapes.clear();
        this.mainCanvas.links.removeAll(this.mainCanvas.selectedLinks);
        this.mainCanvas.selectedLinks.clear();
        this.mainCanvas.repaint();
        if (this.mainSimulator.running) {
            this.stopSimulation();
            for (Thread t : this.mainSimulator.workingThreads) {
                t.interrupt();
            }
            this.startSimulation(currentSimulationType);
        }
    }

    public void initializeProjectSource() {
        mainCanvas.shapes.clear();
        mainCanvas.links.clear();
        this.mainCanvas.repaint();
        this.projectDocument = this.documentBuilder.newDocument();
        this.projectRootElement = this.projectDocument.createElement("project");
        this.projectDocument.appendChild(this.projectRootElement);
        this.projectSourcePath = null;
        parent.setTitle(parent.mainTitle + " - Untitled");
    }

    public void saveProjectSource(String filepath) {

        this.projectDocument = this.documentBuilder.newDocument();
        this.projectRootElement = this.projectDocument.createElement("project");
        this.projectDocument.appendChild(this.projectRootElement);

        for (WSShape shape : mainCanvas.shapes) {
            Element shapeElement = projectDocument.createElement("shape");
            shapeElement.setAttribute("location", String.format("(%d, %d)", shape.x, shape.y));
            shapeElement.setAttribute("name", shape.name);
            shapeElement.setAttribute("type", (shape.type == ShapeType.ROUTER) ? "router" : "host");
            shapeElement.setAttribute("ip", NGNode.getFormattedIP(shape.ipAddress));
            shapeElement.setAttribute("asid", String.valueOf(shape.ASID));
            projectRootElement.appendChild(shapeElement);
        }

        for (WSLink link : mainCanvas.links) {
            Element linkElement = projectDocument.createElement("link");
            linkElement.setAttribute("to", link.toShape.name);
            linkElement.setAttribute("from", link.fromShape.name);
            linkElement.setAttribute("cost", String.valueOf(link.cost));
            projectRootElement.appendChild(linkElement);
        }

        projectRootElement.setAttribute("theme", (this.theme == WorkspaceTheme.LIGHT) ? "light" : "dark");

        try {
            File outputFile = new File(filepath);
            if (outputFile.exists())
                outputFile.delete();

            this.documentTransformer.transform(new DOMSource(projectDocument),
                    new StreamResult(outputFile));
        } catch (TransformerException e) {
            e.printStackTrace();
        }

        this.projectDocument = this.documentBuilder.newDocument();
        this.projectRootElement = this.projectDocument.createElement("project");
        this.projectDocument.appendChild(this.projectRootElement);

        this.projectSourcePath = filepath;
        String filename = Paths.get(filepath).getFileName().toString();
        if (filename.endsWith(".xml"))
            filename = filename.substring(0, filename.length() - 4);

        parent.setTitle(parent.mainTitle + " - " + filename);
    }

    public void openProjectSource(String filepath) {
        this.stopSimulation();
        try {
            this.projectDocument = this.documentBuilder.parse(new File(filepath));
            this.projectRootElement = (Element) this.projectDocument.getElementsByTagName("project").item(0);

            mainCanvas.shapes.clear();
            mainCanvas.links.clear();
            undoStack.clear();
            redoStack.clear();

            HashMap<String, WSShape> shapesNames = new HashMap<>();

            NodeList shapesList = projectRootElement.getElementsByTagName("shape");
            routerShapeNameCounter = 0;
            hostShapeNameCounter = 0;
            for (int i = 0; i < shapesList.getLength(); i++) {
                Element shapeElem = (Element) shapesList.item(i);
                ShapeType type;
                if (shapeElem.getAttribute("type").equals("router")) {
                    type = ShapeType.ROUTER;
                    routerShapeNameCounter++;
                } else {
                    type = ShapeType.HOST;
                    hostShapeNameCounter++;
                }
                String name = shapeElem.getAttribute("name");
                Scanner scanner = new Scanner(shapeElem.getAttribute("location"));
                scanner.useDelimiter("[^\\d-]+");
                int x = scanner.nextInt(), y = scanner.nextInt();
                BitSet ipAddress = NGNode.parseIP(shapeElem.getAttribute("ip"));
                int asid = Integer.parseInt(shapeElem.getAttribute("asid"));
                WSShape shape = new WSShape(type, name, ipAddress, asid, x, y);
                performDrawing(shape);
                shapesNames.put(name, shape);
            }

            NodeList linksList = projectRootElement.getElementsByTagName("link");
            for (int i = 0; i < linksList.getLength(); i++) {
                Element linkElem = (Element) linksList.item(i);
                WSShape from = shapesNames.get(linkElem.getAttribute("from")),
                        to = shapesNames.get(linkElem.getAttribute("to"));
                int cost = Integer.parseInt(linkElem.getAttribute("cost"));
                performLinking(from, to, cost);
            }

            mainCanvas.repaint();

            if (projectRootElement.getAttribute("theme").equals("light")) {
                this.applyTheme(WorkspaceTheme.LIGHT);
                this.parent.menuBar.lightThemeRadioButton.setSelected(true);
                this.parent.menuBar.darkThemeRadioButton.setSelected(false);
            } else if (projectRootElement.getAttribute("theme").equals("dark")) {
                this.applyTheme(WorkspaceTheme.DARK);
                this.parent.menuBar.darkThemeRadioButton.setSelected(true);
                this.parent.menuBar.lightThemeRadioButton.setSelected(false);
            }

        } catch (SAXException | IOException e) {
            e.printStackTrace();
        }

        this.projectSourcePath = filepath;
        String filename = Paths.get(filepath).getFileName().toString();
        if (filename.endsWith(".xml"))
            filename = filename.substring(0, filename.length() - 4);

        parent.setTitle(parent.mainTitle + " - " + filename);
    }

    public void performUndo() {
        try {
            ShapesAndLinksPair pair = undoStack.pop();
            redoStack.add(new ShapesAndLinksPair(new HashSet<>(mainCanvas.shapes), new HashSet<>(mainCanvas.links)));
            parent.menuBar.redoAction.setEnabled(true);
            mainCanvas.shapes.clear();
            mainCanvas.links.clear();
            mainCanvas.shapes.addAll(pair.shapes);
            mainCanvas.links.addAll(pair.links);
            mainCanvas.repaint();
            undoStack.add(undoStack.pop());
        } catch (EmptyStackException e) {
            parent.menuBar.undoAction.setEnabled(false);
        }

        if (this.mainSimulator.running) {
            this.stopSimulation();
            for (Thread t : this.mainSimulator.workingThreads) {
                t.interrupt();
            }
            this.startSimulation(currentSimulationType);
        }
    }

    public void performRedo() {
        try {
            ShapesAndLinksPair pair = redoStack.pop();
            undoStack.add(new ShapesAndLinksPair(new HashSet<>(mainCanvas.shapes), new HashSet<>(mainCanvas.links)));
            parent.menuBar.undoAction.setEnabled(true);
            mainCanvas.shapes.clear();
            mainCanvas.links.clear();
            mainCanvas.shapes.addAll(pair.shapes);
            mainCanvas.links.addAll(pair.links);
            mainCanvas.repaint();
            redoStack.add(redoStack.pop());
        } catch (EmptyStackException e) {
            parent.menuBar.redoAction.setEnabled(false);
        }
        if (this.mainSimulator.running) {
            this.stopSimulation();
            for (Thread t : this.mainSimulator.workingThreads) {
                t.interrupt();
            }
            this.startSimulation(currentSimulationType);
        }
    }

    public void applyTheme(WorkspaceTheme theme) {
        switch (theme) {
            case LIGHT:
                this.mainCanvas.textColor = Color.BLACK;
                this.mainCanvas.bgColor = Color.WHITE;
                this.mainCanvas.setBackground(Color.WHITE);
                this.mainCanvas.linkColor = Color.BLACK;
                this.mainCanvas.selectedLinkColor = Color.BLUE;
                this.theme = WorkspaceTheme.LIGHT;
                break;

            case DARK:
                this.mainCanvas.textColor = Color.WHITE;
                this.mainCanvas.bgColor = Color.BLACK;
                this.mainCanvas.setBackground(Color.BLACK);
                this.mainCanvas.linkColor = Color.WHITE;
                this.mainCanvas.selectedLinkColor = Color.BLUE;
                this.theme = WorkspaceTheme.DARK;
                break;
            default:
                return;
        }

        this.mainCanvas.repaint();
    }

    public void setMode(WorkspaceMode mode) {
        this.mode = mode;
        switch (mode) {
            case DRAWING:
                this.setCursor(placementCursor);
                break;

            case LINKING:
                this.setCursor(linkingCursor);
                break;

            default:
                this.setCursor(defaultCursor);
                break;
        }
    }

    public void startSimulation(SimulationType simType) {
        HashMap<WSShape, NGNode> visitedNodes = new HashMap<>();
        mainGraph.getNodes().clear();
        for (WSShape shape : mainCanvas.shapes) {
            NGNode node;
            if (visitedNodes.keySet().contains(shape)) {
                node = visitedNodes.get(shape);
            } else {
                node = new NGNode(shape.type, shape.name, shape.ipAddress, shape.ASID);
            }
            visitedNodes.put(shape, node);
            for (WSLink link : shape.connectedLinks) {
                WSShape neighborShape = (!shape.equals(link.fromShape)) ? link.fromShape : link.toShape;
                NGNode neighborNode;
                if (visitedNodes.keySet().contains(neighborShape)) {
                    neighborNode = visitedNodes.get(neighborShape);
                } else {
                    neighborNode = new NGNode(neighborShape.type, neighborShape.name, neighborShape.ipAddress,
                            neighborShape.ASID);
                    visitedNodes.put(neighborShape, neighborNode);
                }

                node.connect(neighborNode, link.cost, shape.connectedLinks.indexOf(link));
                neighborNode.connect(node, link.cost, neighborShape.connectedLinks.indexOf(link));
            }
            mainGraph.getNodes().add(node);
        }

        switch (simType) {
            case OSPF:
                this.mainSimulator.simulateOSPF();
                break;

            case BGP:
                this.mainSimulator.simulateBGP();
                break;
        }
    }

    public void stopSimulation() {
        this.mainSimulator.stop();
    }
}
