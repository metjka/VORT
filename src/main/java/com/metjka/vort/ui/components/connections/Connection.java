package com.metjka.vort.ui.components.connections;

import com.google.common.collect.ImmutableMap;
import com.metjka.vort.ui.Type;
import com.metjka.vort.ui.BlockContainer;
import com.metjka.vort.ui.ComponentLoader;
import com.metjka.vort.ui.components.blocks.Block;
import com.metjka.vort.ui.serialize.Bundleable;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Point2D;
import javafx.scene.paint.Color;
import javafx.scene.shape.CubicCurve;
import javafx.scene.transform.Transform;

import java.util.Map;
import java.util.Optional;
import java.util.Set;


/**
 * This is a Connection that represents a flow between an {@link InputAnchor}
 * and {@link OutputAnchor}. Both anchors are stored referenced respectively as
 * startAnchor and endAnchor {@link Optional} within this class.
 * Visually a connection is represented as a cubic Bezier curve.
 * <p>
 * Connection is also a changeListener for a Transform, in order to be able to
 * update the Line's position when the anchor's positions change.
 */
public class Connection extends CubicCurve implements
        ChangeListener<Transform>, Bundleable, ComponentLoader {

    /**
     * Control offset for this bezier curve of this line.
     * It determines how a far a line attempts to goes straight from its end points.
     */
    public static final double BEZIER_CONTROL_OFFSET = 150f;

    /**
     * Labels for serialization to and from JSON
     */
    private static final String SOURCE_LABEL = "from";
    private static final String SINK_LABEL = "to";

    /**
     * Starting point of this Line that can be Anchored onto other objects.
     */
    private final OutputAnchor startAnchor;
    /**
     * Ending point of this Line that can be Anchored onto other objects.
     */
    private final InputAnchor endAnchor;

    /**
     * Whether this connection produced an error in the latest type unification.
     */
    private boolean errorState;

    /**
     * Whether this connection is impossible due to scope restrictions
     */
    private boolean scopeError;

    /**
     * Construct a new Connection.
     *
     * @param source The OutputAnchor this connection comes from
     * @param sink   The InputAnchor this connection goes to
     */
    public Connection(OutputAnchor source, InputAnchor sink) {
        this.setMouseTransparent(true);
        this.setFill(null);

        this.startAnchor = source;
        this.endAnchor = sink;
        this.errorState = false;
        this.scopeError = false;

        source.getPane().addConnection(this);
        this.invalidateAnchorPositions();
        this.startAnchor.addConnection(this);
        this.startAnchor.localToSceneTransformProperty().addListener(this);
        this.endAnchor.setConnection(this);
        this.endAnchor.localToSceneTransformProperty().addListener(this);

    }

    /**
     * @return the output anchor of this connection.
     */
    public OutputAnchor getStartAnchor() {
        return this.startAnchor;
    }

    /**
     * @return the input anchor of this connection.
     */
    public InputAnchor getEndAnchor() {
        return this.endAnchor;
    }

    /**
     * Handles the upward connections changes through an connection.
     * Also perform typechecking for this connection.
     *
     * @param finalPhase whether the change propagation is in the second (final) phase.
     */
    public void handleConnectionChangesUpwards(boolean finalPhase) {
        // first make sure the output anchor block and types are fresh
        if (!finalPhase) {
            this.startAnchor.prepareConnectionChanges();
        }

        // for connections in error state typechecking is delayed to the final phase to keep error locations stable
        if (finalPhase == this.errorState) {
        }

        // continue with propagating connections changes in the output anchor block 
        this.startAnchor.handleConnectionChanges();
    }

    /**
     * Removes this Connection, disconnecting its anchors and removing this Connection from the pane it is on.
     */
    public final void remove() {
        this.startAnchor.localToSceneTransformProperty().removeListener(this);
        this.endAnchor.localToSceneTransformProperty().removeListener(this);
        this.startAnchor.dropConnection(this);
        this.endAnchor.removeConnections();
        this.startAnchor.getPane().removeConnection(this);
        // propagate the connection changes of both anchors simultaneously in two phases to avoid duplicate work 
        this.startAnchor.handleConnectionChanges();
        this.endAnchor.handleConnectionChanges();
        this.startAnchor.handleConnectionChanges();
        this.endAnchor.handleConnectionChanges();
    }

    @Override
    public final void changed(ObservableValue<? extends Transform> observable, Transform oldValue, Transform newValue) {
        this.invalidateAnchorPositions();
    }

    /**
     * Update the UI positions of both start and end anchors.
     */
    private void invalidateAnchorPositions() {
        this.setStartPosition(this.startAnchor.getAttachmentPoint());
        this.setEndPosition(this.endAnchor.getAttachmentPoint());
    }

    @Override
    public String toString() {
        return "Connection connecting \n(out) " + startAnchor + "   to\n(in)  " + endAnchor;
    }

    @Override
    public Map<String, Object> toBundle() {
        ImmutableMap.Builder<String, Object> bundle = ImmutableMap.builder();
        bundle.put(SOURCE_LABEL, this.startAnchor.toBundle());
        bundle.put(SINK_LABEL, this.endAnchor.toBundle());
        return bundle.build();
    }

    public static void fromBundle(Map<String, Object> connectionBundle,
                                  Map<Integer, Block> blockLookupTable) {
        Map<String, Object> source = (Map<String, Object>) connectionBundle.get(SOURCE_LABEL);
        Integer sourceId = ((Double) source.get(ConnectionAnchor.BLOCK_LABEL)).intValue();
        Block sourceBlock = blockLookupTable.get(sourceId);
        OutputAnchor sourceAnchor = sourceBlock.getAllOutputs().get(0);

        Map<String, Object> sink = (Map<String, Object>) connectionBundle.get(SINK_LABEL);
        Integer sinkId = ((Double) sink.get(ConnectionAnchor.BLOCK_LABEL)).intValue();
        Integer sinkAnchorNumber = ((Double) sink.get(ConnectionAnchor.ANCHOR_LABEL)).intValue();
        Block sinkBlock = blockLookupTable.get(sinkId);
        InputAnchor sinkAnchor = sinkBlock.getAllInputs().get(sinkAnchorNumber);

        Connection connection = new Connection(sourceAnchor, sinkAnchor);
        connection.invalidateVisualState();
        sinkBlock.invalidateVisualState();
    }

    /**
     * Sets the start coordinates for this Connection.
     *
     * @param point Coordinates local to this Line's parent.
     */
    private void setStartPosition(Point2D point) {
        this.setStartX(point.getX());
        this.setStartY(point.getY());
        updateBezierControlPoints(this);
    }

    /**
     * Sets the end coordinates for this Connection.
     *
     * @param point coordinates local to this Line's parent.
     */
    private void setEndPosition(Point2D point) {
        this.setEndX(point.getX());
        this.setEndY(point.getY());
        updateBezierControlPoints(this);
    }

    /**
     * Returns the current bezier offset based on the current start and end positions.
     */
    private static double getBezierYOffset(CubicCurve wire) {
        double distX = Math.abs(wire.getEndX() - wire.getStartX()) / 3;
        double diffY = wire.getEndY() - wire.getStartY();
        double distY = diffY > 0 ? diffY / 2 : Math.max(0, -diffY - 10);
        if (distY < BEZIER_CONTROL_OFFSET) {
            if (distX < BEZIER_CONTROL_OFFSET) {
                // short lines are extra flexible
                return Math.max(1, Math.max(distX, distY));
            } else {
                return BEZIER_CONTROL_OFFSET;
            }
        } else {
            return Math.cbrt(distY / BEZIER_CONTROL_OFFSET) * BEZIER_CONTROL_OFFSET;
        }
    }

    /**
     * Updates the Bezier offset (curviness) according to the current start and end positions.
     */
    protected static void updateBezierControlPoints(CubicCurve wire) {
        double yOffset = getBezierYOffset(wire);
        wire.setControlX1(wire.getStartX());
        wire.setControlY1(wire.getStartY() + yOffset);
        wire.setControlX2(wire.getEndX());
        wire.setControlY2(wire.getEndY() - yOffset);
    }

    protected static double lengthSquared(CubicCurve wire) {
        double diffX = wire.getStartX() - wire.getEndX();
        double diffY = wire.getStartY() - wire.getEndY();
        return diffX * diffX + diffY * diffY;
    }

    public void invalidateVisualState() {
        this.scopeError = !this.endAnchor.getContainer().isContainedWithin(this.startAnchor.getContainer());

        if (this.errorState) {
            this.setStroke(Color.RED);
            this.getStrokeDashArray().clear();
            this.setStrokeWidth(3);

        } else if (this.scopeError) {
            this.setStroke(Color.RED);
            this.setStrokeWidth(3);
            if (this.getStrokeDashArray().isEmpty()) {
                this.getStrokeDashArray().addAll(10.0, 10.0);
            }

        } else {
            this.setStroke(Color.BLACK);
            this.getStrokeDashArray().clear();
            this.setStrokeWidth(3);
        }
    }

    public boolean hasTypeError() {
        return this.errorState;
    }

    public boolean hasScopeError() {
        return this.scopeError;
    }

}
