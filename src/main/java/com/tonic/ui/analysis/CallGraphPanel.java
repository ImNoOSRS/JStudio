package com.tonic.ui.analysis;

import com.mxgraph.layout.hierarchical.mxHierarchicalLayout;
import com.mxgraph.model.mxCell;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.util.mxCellRenderer;
import com.mxgraph.util.mxConstants;
import com.mxgraph.view.mxGraph;
import com.mxgraph.view.mxStylesheet;
import com.tonic.analysis.callgraph.CallGraph;
import com.tonic.analysis.callgraph.CallGraphNode;
import com.tonic.analysis.callgraph.CallSite;
import com.tonic.analysis.callgraph.MethodReference;
import com.tonic.parser.MethodEntry;
import com.tonic.ui.event.EventBus;
import com.tonic.ui.event.events.ClassSelectedEvent;
import com.tonic.ui.event.events.MethodSelectedEvent;
import com.tonic.ui.model.ClassEntryModel;
import com.tonic.ui.model.MethodEntryModel;
import com.tonic.ui.model.ProjectModel;
import com.tonic.ui.theme.JStudioTheme;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Call graph visualization panel using JGraphX.
 */
public class CallGraphPanel extends JPanel {

    private final ProjectModel project;
    private final mxGraph graph;
    private final mxGraphComponent graphComponent;
    private final JTextArea statusArea;
    private final JComboBox<String> focusCombo;
    private final JSpinner depthSpinner;

    private CallGraph callGraph;
    private MethodReference focusMethod;
    private int maxDepth = 3;

    // Map graph cells back to method references for click handling
    private Map<Object, MethodReference> cellToMethodMap = new HashMap<>();
    private JPopupMenu contextMenu;

    public CallGraphPanel(ProjectModel project) {
        this.project = project;

        setLayout(new BorderLayout());
        setBackground(JStudioTheme.getBgSecondary());

        // Control panel
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        controlPanel.setBackground(JStudioTheme.getBgSecondary());

        JButton buildButton = new JButton("Build Graph");
        buildButton.setBackground(JStudioTheme.getBgTertiary());
        buildButton.setForeground(JStudioTheme.getTextPrimary());
        buildButton.addActionListener(e -> buildCallGraph());
        controlPanel.add(buildButton);

        controlPanel.add(new JLabel("Focus:"));
        focusCombo = new JComboBox<>();
        focusCombo.setBackground(JStudioTheme.getBgTertiary());
        focusCombo.setForeground(JStudioTheme.getTextPrimary());
        focusCombo.addActionListener(e -> updateFocus());
        controlPanel.add(focusCombo);

        controlPanel.add(new JLabel("Depth:"));
        depthSpinner = new JSpinner(new SpinnerNumberModel(3, 1, 10, 1));
        depthSpinner.addChangeListener(e -> {
            maxDepth = (Integer) depthSpinner.getValue();
            if (focusMethod != null) {
                visualizeFocused();
            }
        });
        controlPanel.add(depthSpinner);

        JButton refreshButton = new JButton("Refresh");
        refreshButton.setBackground(JStudioTheme.getBgTertiary());
        refreshButton.setForeground(JStudioTheme.getTextPrimary());
        refreshButton.addActionListener(e -> visualizeFocused());
        controlPanel.add(refreshButton);

        JButton exportButton = new JButton("Export PNG");
        exportButton.setBackground(JStudioTheme.getBgTertiary());
        exportButton.setForeground(JStudioTheme.getTextPrimary());
        exportButton.addActionListener(e -> exportGraphAsPng());
        controlPanel.add(exportButton);

        add(controlPanel, BorderLayout.NORTH);

        // Graph component
        graph = new mxGraph();
        setupGraphStyles();
        graph.setAutoSizeCells(true);
        graph.setCellsEditable(false);
        graph.setCellsMovable(true);
        graph.setCellsResizable(false);

        graphComponent = new mxGraphComponent(graph);
        graphComponent.setBackground(JStudioTheme.getBgTertiary());
        graphComponent.getViewport().setBackground(JStudioTheme.getBgTertiary());
        graphComponent.setBorder(null);
        graphComponent.setToolTips(true);

        // Setup mouse interaction for nodes
        setupMouseListeners();
        setupContextMenu();

        add(graphComponent, BorderLayout.CENTER);

        // Status area
        statusArea = new JTextArea(3, 40);
        statusArea.setEditable(false);
        statusArea.setBackground(JStudioTheme.getBgTertiary());
        statusArea.setForeground(JStudioTheme.getTextSecondary());
        statusArea.setFont(JStudioTheme.getCodeFont(11));
        statusArea.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        JScrollPane statusScroll = new JScrollPane(statusArea);
        statusScroll.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, JStudioTheme.getBorder()));
        add(statusScroll, BorderLayout.SOUTH);

        updateStatus("No call graph built. Click 'Build Graph' to analyze.");

        // Register for method selection events
        EventBus.getInstance().register(MethodSelectedEvent.class, event -> {
            MethodEntryModel method = event.getMethodEntry();
            if (method != null && isShowing()) {
                focusOnMethod(method.getMethodEntry());
            }
        });
    }

    /**
     * Setup mouse listeners for node interaction.
     */
    private void setupMouseListeners() {
        graphComponent.getGraphControl().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Object cell = graphComponent.getCellAt(e.getX(), e.getY());
                if (cell == null) return;

                MethodReference method = cellToMethodMap.get(cell);
                if (method == null) return;

                if (e.getClickCount() == 2) {
                    // Double-click: focus on this method
                    focusMethod = method;
                    visualizeFocused();
                    updateComboSelection(method);
                    updateStatus("Focused on: " + method.getOwner() + "." + method.getName());
                } else if (e.getClickCount() == 1) {
                    // Single click: show method info
                    showMethodInfo(method);
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                maybeShowPopup(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                maybeShowPopup(e);
            }

            private void maybeShowPopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    Object cell = graphComponent.getCellAt(e.getX(), e.getY());
                    if (cell != null && cellToMethodMap.containsKey(cell)) {
                        contextMenu.show(graphComponent, e.getX(), e.getY());
                    }
                }
            }
        });
    }

    /**
     * Setup context menu for graph nodes.
     */
    private void setupContextMenu() {
        contextMenu = new JPopupMenu();
        contextMenu.setBackground(JStudioTheme.getBgSecondary());
        contextMenu.setBorder(BorderFactory.createLineBorder(JStudioTheme.getBorder()));

        JMenuItem focusItem = new JMenuItem("Focus on this method");
        focusItem.setBackground(JStudioTheme.getBgSecondary());
        focusItem.setForeground(JStudioTheme.getTextPrimary());
        focusItem.addActionListener(e -> {
            Object cell = graph.getSelectionCell();
            if (cell != null) {
                MethodReference method = cellToMethodMap.get(cell);
                if (method != null) {
                    focusMethod = method;
                    visualizeFocused();
                    updateComboSelection(method);
                }
            }
        });
        contextMenu.add(focusItem);

        JMenuItem showCallersItem = new JMenuItem("Show all callers");
        showCallersItem.setBackground(JStudioTheme.getBgSecondary());
        showCallersItem.setForeground(JStudioTheme.getTextPrimary());
        showCallersItem.addActionListener(e -> {
            Object cell = graph.getSelectionCell();
            if (cell != null && callGraph != null) {
                MethodReference method = cellToMethodMap.get(cell);
                if (method != null) {
                    Set<MethodReference> callers = callGraph.getCallers(method);
                    StringBuilder sb = new StringBuilder();
                    sb.append("Callers of ").append(method.getName()).append(":\n");
                    for (MethodReference caller : callers) {
                        sb.append("  - ").append(caller.getOwner()).append(".").append(caller.getName()).append("\n");
                    }
                    updateStatus(sb.toString());
                }
            }
        });
        contextMenu.add(showCallersItem);

        JMenuItem showCalleesItem = new JMenuItem("Show all callees");
        showCalleesItem.setBackground(JStudioTheme.getBgSecondary());
        showCalleesItem.setForeground(JStudioTheme.getTextPrimary());
        showCalleesItem.addActionListener(e -> {
            Object cell = graph.getSelectionCell();
            if (cell != null && callGraph != null) {
                MethodReference method = cellToMethodMap.get(cell);
                if (method != null) {
                    Set<MethodReference> callees = callGraph.getCallees(method);
                    StringBuilder sb = new StringBuilder();
                    sb.append("Callees of ").append(method.getName()).append(":\n");
                    for (MethodReference callee : callees) {
                        sb.append("  - ").append(callee.getOwner()).append(".").append(callee.getName()).append("\n");
                    }
                    updateStatus(sb.toString());
                }
            }
        });
        contextMenu.add(showCalleesItem);

        JMenuItem navigateItem = new JMenuItem("Navigate to source");
        navigateItem.setBackground(JStudioTheme.getBgSecondary());
        navigateItem.setForeground(JStudioTheme.getTextPrimary());
        navigateItem.addActionListener(e -> {
            Object cell = graph.getSelectionCell();
            if (cell != null) {
                MethodReference method = cellToMethodMap.get(cell);
                if (method != null) {
                    navigateToMethod(method);
                }
            }
        });
        contextMenu.add(navigateItem);
    }

    /**
     * Show info about a method in the status area.
     */
    private void showMethodInfo(MethodReference method) {
        if (callGraph == null) return;

        CallGraphNode node = callGraph.getNode(method);
        StringBuilder sb = new StringBuilder();
        sb.append(method.getOwner()).append(".").append(method.getName()).append(method.getDescriptor()).append("\n");

        if (node != null) {
            sb.append("Callers: ").append(node.getCallCount()).append(", ");
            sb.append("Callees: ").append(node.getCalleeCount()).append(", ");
            sb.append("In pool: ").append(node.isInPool() ? "yes" : "no (external)");
        }

        updateStatus(sb.toString());
    }

    /**
     * Navigate to the source of a method.
     */
    private void navigateToMethod(MethodReference method) {
        // Find the class and method in the project
        for (ClassEntryModel classEntry : project.getAllClasses()) {
            if (classEntry.getClassName().equals(method.getOwner())) {
                // Find the method
                for (MethodEntryModel methodModel : classEntry.getMethods()) {
                    MethodEntry me = methodModel.getMethodEntry();
                    if (me.getName().equals(method.getName()) && me.getDesc().equals(method.getDescriptor())) {
                        // Fire event to navigate
                        EventBus.getInstance().post(new ClassSelectedEvent(this, classEntry));
                        return;
                    }
                }
                // Method not found but class exists, navigate to class
                EventBus.getInstance().post(new ClassSelectedEvent(this, classEntry));
                return;
            }
        }
        updateStatus("Method not found in project: " + method.getOwner() + "." + method.getName());
    }

    /**
     * Update the combo box selection to match a method.
     */
    private void updateComboSelection(MethodReference method) {
        String label = method.getOwner() + "." + method.getName();
        for (int i = 0; i < focusCombo.getItemCount(); i++) {
            if (label.equals(focusCombo.getItemAt(i))) {
                focusCombo.setSelectedIndex(i);
                break;
            }
        }
    }

    private void setupGraphStyles() {
        mxStylesheet stylesheet = graph.getStylesheet();
        Map<String, Object> style = new HashMap<>();

        // Node style
        style.put(mxConstants.STYLE_SHAPE, mxConstants.SHAPE_RECTANGLE);
        style.put(mxConstants.STYLE_ROUNDED, true);
        style.put(mxConstants.STYLE_FILLCOLOR, "#252535");
        style.put(mxConstants.STYLE_STROKECOLOR, "#7AA2F7");
        style.put(mxConstants.STYLE_FONTCOLOR, "#E4E4EF");
        style.put(mxConstants.STYLE_FONTSIZE, 10);
        style.put(mxConstants.STYLE_SPACING, 4);
        stylesheet.putCellStyle("METHOD", style);

        // Focus node style
        Map<String, Object> focusStyle = new HashMap<>(style);
        focusStyle.put(mxConstants.STYLE_FILLCOLOR, "#3D4070");
        focusStyle.put(mxConstants.STYLE_STROKECOLOR, "#E0AF68");
        focusStyle.put(mxConstants.STYLE_STROKEWIDTH, 2);
        stylesheet.putCellStyle("FOCUS", focusStyle);

        // External node style
        Map<String, Object> externalStyle = new HashMap<>(style);
        externalStyle.put(mxConstants.STYLE_FILLCOLOR, "#1E1E2E");
        externalStyle.put(mxConstants.STYLE_STROKECOLOR, "#565F89");
        externalStyle.put(mxConstants.STYLE_FONTCOLOR, "#9090A8");
        stylesheet.putCellStyle("EXTERNAL", externalStyle);

        // Edge style
        Map<String, Object> edgeStyle = new HashMap<>();
        edgeStyle.put(mxConstants.STYLE_STROKECOLOR, "#565F89");
        edgeStyle.put(mxConstants.STYLE_ENDARROW, mxConstants.ARROW_CLASSIC);
        edgeStyle.put(mxConstants.STYLE_FONTCOLOR, "#9090A8");
        edgeStyle.put(mxConstants.STYLE_FONTSIZE, 9);
        stylesheet.putCellStyle("EDGE", edgeStyle);

        graph.getStylesheet().setDefaultEdgeStyle(edgeStyle);
    }

    /**
     * Build the call graph from the project.
     */
    public void buildCallGraph() {
        if (project.getClassPool() == null) {
            updateStatus("No project loaded. Open a JAR or class file first.");
            return;
        }

        updateStatus("Building call graph...");

        SwingWorker<CallGraph, Void> worker = new SwingWorker<CallGraph, Void>() {
            @Override
            protected CallGraph doInBackground() throws Exception {
                return CallGraph.build(project.getClassPool());
            }

            @Override
            protected void done() {
                try {
                    callGraph = get();
                    populateFocusCombo();

                    // Auto-select first method and visualize for immediate feedback
                    if (focusCombo.getItemCount() > 1) {
                        focusCombo.setSelectedIndex(1);  // Skip "(Select method to focus)"
                    }

                    updateStatus("Call graph built: " + callGraph.size() + " methods, " +
                            callGraph.edgeCount() + " edges. Select a method to explore.");
                } catch (Exception e) {
                    updateStatus("Failed to build call graph: " + e.getMessage());
                }
            }
        };

        worker.execute();
    }

    private void populateFocusCombo() {
        focusCombo.removeAllItems();
        focusCombo.addItem("(Select method to focus)");

        if (callGraph == null) return;

        for (CallGraphNode node : callGraph.getPoolNodes()) {
            MethodReference ref = node.getReference();
            String label = ref.getOwner() + "." + ref.getName();
            focusCombo.addItem(label);
        }
    }

    private void updateFocus() {
        if (callGraph == null) return;

        String selected = (String) focusCombo.getSelectedItem();
        if (selected == null || selected.startsWith("(")) {
            focusMethod = null;
            return;
        }

        // Find the method reference
        for (CallGraphNode node : callGraph.getPoolNodes()) {
            MethodReference ref = node.getReference();
            String label = ref.getOwner() + "." + ref.getName();
            if (label.equals(selected)) {
                focusMethod = ref;
                visualizeFocused();
                return;
            }
        }
    }

    private void visualizeFocused() {
        if (callGraph == null || focusMethod == null) return;

        // Clear the cell-to-method mapping
        cellToMethodMap.clear();

        graph.getModel().beginUpdate();
        try {
            // Clear existing cells
            graph.removeCells(graph.getChildCells(graph.getDefaultParent(), true, true));

            Object parent = graph.getDefaultParent();
            Map<MethodReference, Object> nodeMap = new HashMap<>();

            // Get callers and callees up to max depth
            Set<MethodReference> callers = collectCallers(focusMethod, maxDepth);
            Set<MethodReference> callees = collectCallees(focusMethod, maxDepth);

            // Create focus node
            String focusLabel = formatMethodLabel(focusMethod);
            Object focusNode = graph.insertVertex(parent, null, focusLabel,
                    0, 0, 150, 30, "FOCUS");
            nodeMap.put(focusMethod, focusNode);
            cellToMethodMap.put(focusNode, focusMethod);

            // Create caller nodes
            for (MethodReference caller : callers) {
                if (!nodeMap.containsKey(caller)) {
                    String label = formatMethodLabel(caller);
                    CallGraphNode cgNode = callGraph.getNode(caller);
                    String style = (cgNode != null && cgNode.isInPool()) ? "METHOD" : "EXTERNAL";
                    Object node = graph.insertVertex(parent, null, label,
                            0, 0, 150, 30, style);
                    nodeMap.put(caller, node);
                    cellToMethodMap.put(node, caller);
                }
            }

            // Create callee nodes
            for (MethodReference callee : callees) {
                if (!nodeMap.containsKey(callee)) {
                    String label = formatMethodLabel(callee);
                    CallGraphNode cgNode = callGraph.getNode(callee);
                    String style = (cgNode != null && cgNode.isInPool()) ? "METHOD" : "EXTERNAL";
                    Object node = graph.insertVertex(parent, null, label,
                            0, 0, 150, 30, style);
                    nodeMap.put(callee, node);
                    cellToMethodMap.put(node, callee);
                }
            }

            // Create edges for callers -> focus with tooltips
            for (MethodReference caller : callers) {
                if (callGraph.calls(caller, focusMethod)) {
                    String edgeLabel = getEdgeTooltip(caller, focusMethod);
                    Object edge = graph.insertEdge(parent, null, "", nodeMap.get(caller), nodeMap.get(focusMethod), "EDGE");
                    // Store tooltip as user object
                    if (edge instanceof mxCell) {
                        ((mxCell) edge).setValue(edgeLabel);
                    }
                }
            }

            // Create edges for focus -> callees with tooltips
            for (MethodReference callee : callees) {
                if (callGraph.calls(focusMethod, callee)) {
                    String edgeLabel = getEdgeTooltip(focusMethod, callee);
                    Object edge = graph.insertEdge(parent, null, "", nodeMap.get(focusMethod), nodeMap.get(callee), "EDGE");
                    if (edge instanceof mxCell) {
                        ((mxCell) edge).setValue(edgeLabel);
                    }
                }
            }

            // Layout
            mxHierarchicalLayout layout = new mxHierarchicalLayout(graph);
            layout.setInterRankCellSpacing(50);
            layout.setIntraCellSpacing(30);
            layout.execute(parent);

        } finally {
            graph.getModel().endUpdate();
        }

        updateStatus("Showing call graph for: " + focusMethod.getOwner() + "." + focusMethod.getName() +
                " (" + collectCallers(focusMethod, maxDepth).size() + " callers, " +
                collectCallees(focusMethod, maxDepth).size() + " callees)");
    }

    /**
     * Get tooltip text for an edge showing call site info.
     */
    private String getEdgeTooltip(MethodReference caller, MethodReference callee) {
        CallGraphNode callerNode = callGraph.getNode(caller);
        if (callerNode == null) return "";

        StringBuilder sb = new StringBuilder();
        for (CallSite site : callerNode.getOutgoingCalls()) {
            if (site.getTarget().equals(callee)) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(site.getInvokeType().name());
            }
        }
        return sb.toString();
    }

    private Set<MethodReference> collectCallers(MethodReference method, int depth) {
        Set<MethodReference> result = new LinkedHashSet<>();
        collectCallersRecursive(method, depth, result);
        return result;
    }

    private void collectCallersRecursive(MethodReference method, int depth, Set<MethodReference> result) {
        if (depth <= 0) return;
        Set<MethodReference> callers = callGraph.getCallers(method);
        for (MethodReference caller : callers) {
            if (result.add(caller)) {
                collectCallersRecursive(caller, depth - 1, result);
            }
        }
    }

    private Set<MethodReference> collectCallees(MethodReference method, int depth) {
        Set<MethodReference> result = new LinkedHashSet<>();
        collectCalleesRecursive(method, depth, result);
        return result;
    }

    private void collectCalleesRecursive(MethodReference method, int depth, Set<MethodReference> result) {
        if (depth <= 0) return;
        Set<MethodReference> callees = callGraph.getCallees(method);
        for (MethodReference callee : callees) {
            if (result.add(callee)) {
                collectCalleesRecursive(callee, depth - 1, result);
            }
        }
    }

    private String formatMethodLabel(MethodReference ref) {
        String owner = ref.getOwner();
        int lastSlash = owner.lastIndexOf('/');
        if (lastSlash >= 0) {
            owner = owner.substring(lastSlash + 1);
        }
        return owner + "." + ref.getName();
    }

    private void updateStatus(String message) {
        statusArea.setText(message);
    }

    /**
     * Refresh the panel.
     */
    public void refresh() {
        if (callGraph != null) {
            populateFocusCombo();
        }
    }

    /**
     * Focus on a specific method.
     */
    public void focusOnMethod(MethodEntry method) {
        if (callGraph == null) {
            buildCallGraph();
        }
        this.focusMethod = new MethodReference(method.getOwnerName(), method.getName(), method.getDesc());
        visualizeFocused();

        // Update combo box
        String label = method.getOwnerName() + "." + method.getName();
        for (int i = 0; i < focusCombo.getItemCount(); i++) {
            if (label.equals(focusCombo.getItemAt(i))) {
                focusCombo.setSelectedIndex(i);
                break;
            }
        }
    }

    /**
     * Get the call graph.
     */
    public CallGraph getCallGraph() {
        return callGraph;
    }

    /**
     * Export the current graph as a PNG image.
     */
    private void exportGraphAsPng() {
        if (focusMethod == null) {
            updateStatus("No graph to export. Build a call graph and select a method first.");
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export Call Graph as PNG");
        chooser.setFileFilter(new FileNameExtensionFilter("PNG Images", "png"));

        // Suggest a filename based on the focused method
        String suggestedName = focusMethod.getOwner().replace('/', '_') + "_" + focusMethod.getName() + "_callgraph.png";
        chooser.setSelectedFile(new File(suggestedName));

        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            if (!file.getName().toLowerCase().endsWith(".png")) {
                file = new File(file.getAbsolutePath() + ".png");
            }

            try {
                BufferedImage image = mxCellRenderer.createBufferedImage(
                        graph, null, 2, Color.WHITE, true, null);

                if (image != null) {
                    ImageIO.write(image, "PNG", file);
                    updateStatus("Graph exported to: " + file.getAbsolutePath());
                } else {
                    updateStatus("Failed to create image - graph may be empty.");
                }
            } catch (IOException ex) {
                updateStatus("Failed to export graph: " + ex.getMessage());
            }
        }
    }
}
