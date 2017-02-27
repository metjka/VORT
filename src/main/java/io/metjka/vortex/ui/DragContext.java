package io.metjka.vortex.ui;

import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TouchEvent;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Helper class used for event handling of dragging a Node.
 */
public class DragContext {

    /** The id of the finger/cursor that is currently dragging the Node */
    private int touchId;

    /** Touch ID representing the mouse cursor. */
    private static final int MOUSE_ID = -1;
    /** An unused touch ID. */
    private static final int NULL_ID = Integer.MIN_VALUE;
    
    /** The Node that can be dragged. */
    final Node node;         // Node that is being dragged

    /** Whether this node will go to the foreground when the user starts a drag gesture with it. */
    boolean goToForegroundOnContact;
    
    /** The x,y position in the Node where the dragging started. */
    private double localOffsetX, localOffsetY;
    
    /** reference to internal touch event handler */
    private final EventHandler<TouchEvent> touchHandler;

    /** reference to internal mouse event handler */
    private final EventHandler<MouseEvent> mouseHandler;
    
    /** the method to be called when the node is contact with touch or mouse, may be null */
    private Consumer<DragContext> contactAction;
    
    /** the method to be called when the node has been released, may be null */
    private Consumer<DragContext> releaseAction;
    
    /** the method to be called when a drag action has started, may be null */
    private Consumer<DragContext> dragInitAction;

    /** the method to be called when a drag action has finished, may be null */
    private Consumer<DragContext> dragFinishAction;

    /** the method to be called when a secondary action is performed, may be null. */
    private BiConsumer<Point2D, Boolean> secondaryClickAction;
    
    private boolean activated;
    
    /** bounds to wherein the dragging is constrained */
    private Bounds dragLimits;  
    
    /** the initial drag distance before it is considered a proper drag action */
    private double dragThreshold;
    
    /** whether a thresholded drag action has started */
    private boolean dragStarted;
    
    /** minimal movement offset before a node relocation is triggered, to reduce wasteful redraws */
    private double relocateThreshold;
    
    /**
     * Creates a DragContext keeping track of touch events, so that a Node is made draggable.
     * @param draggable the Node that is to be made draggable.
     */
    public DragContext(Node draggable) {
        this.node = draggable;
        this.goToForegroundOnContact = true;
        this.touchId = NULL_ID;
        this.activated = false;
        this.dragLimits = new BoundingBox(0, 0, Integer.MAX_VALUE, Integer.MAX_VALUE);
        this.dragThreshold = 10.0;
        this.dragStarted = false;
        this.relocateThreshold = 1.0;
        
        this.dragInitAction = null;
        this.dragFinishAction = null;
        
        touchHandler = event -> {
            EventType<TouchEvent> type = event.getEventType();
            
            if (type == TouchEvent.TOUCH_PRESSED) {
                if (this.touchId == DragContext.NULL_ID) {
                    this.touchId = event.getTouchPoint().getId();
                    this.handleTouchPressed(event.getTouchPoint().getX(), event.getTouchPoint().getY());
                }
                event.consume();
            } else if (type == TouchEvent.TOUCH_MOVED) {
                if (this.touchId == event.getTouchPoint().getId()) {
                    this.handleTouchMoved(event.getTouchPoint().getX(), event.getTouchPoint().getY());
                } else if (this.touchId == DragContext.NULL_ID) {
                    // this can happen when dragging from menu
                    this.touchId = event.getTouchPoint().getId();
                    this.handleTouchMoved(event.getTouchPoint().getX(), event.getTouchPoint().getY());
                }
                event.consume();
            } else if (type == TouchEvent.TOUCH_RELEASED) {
                long fingerCount = event.getTouchPoints().stream().filter(tp -> tp.belongsTo(this.node)).count();
                if (this.touchId > MOUSE_ID && fingerCount == 1) {
                    if (this.activated) {
                        this.activated = false;
                        if (this.releaseAction != null) {
                            this.releaseAction.accept(this);
                        }
                    }
                    
                    if (this.dragStarted) {
                        this.handleTouchReleased();
                    }
                    
                    this.touchId = DragContext.NULL_ID;
                } else if (!this.dragStarted && fingerCount == 2) {
                    if (this.secondaryClickAction != null) {
                        this.secondaryClickAction.accept(new Point2D(event.getTouchPoint().getX(), event.getTouchPoint().getY()), false);
                    }
                }
                
                event.consume();
            }
        };
        
        mouseHandler = event -> {
            if (event.isSynthesized()) {
                event.consume();
                return;
            }
            
            EventType<? extends MouseEvent> type = event.getEventType();
            
            if (type == MouseEvent.MOUSE_PRESSED) {
                if (this.touchId == DragContext.NULL_ID) {
                    this.touchId = DragContext.MOUSE_ID;
                    this.handleTouchPressed(event.getX(), event.getY());
                }
                event.consume();
            } else if (type == MouseEvent.MOUSE_DRAGGED) {
                
                if (this.touchId == DragContext.MOUSE_ID) {
                    this.handleTouchMoved(event.getX(), event.getY());
                }
                event.consume();
            } else if (type == MouseEvent.MOUSE_RELEASED) {
                if (this.touchId == DragContext.MOUSE_ID && this.activated && !event.isPrimaryButtonDown() && !event.isSecondaryButtonDown()) {
                    this.activated = false;
                    if (this.releaseAction != null)
                        this.releaseAction.accept(this);
                }
                
                if (event.getButton() == MouseButton.SECONDARY && !this.dragStarted) {
                    if (this.secondaryClickAction != null) {
                        this.secondaryClickAction.accept(new Point2D(event.getX(), event.getY()), true);
                    }
                } else if (this.touchId == DragContext.MOUSE_ID) {
                    this.touchId = DragContext.NULL_ID;
                    handleTouchReleased();
                }
                
                event.consume();
                this.touchId = DragContext.NULL_ID;
            }
        };
        
        draggable.addEventHandler(TouchEvent.ANY, touchHandler);
        draggable.addEventHandler(MouseEvent.ANY, mouseHandler);
    }
    
    private void handleTouchPressed(double localX, double localY) {
        this.localOffsetX = localX;
        this.localOffsetY = localY;

        if (this.goToForegroundOnContact) {
            node.toFront();
        }
        
        if (!this.activated) {
            this.activated = true;
            if (this.contactAction != null) {
                this.contactAction.accept(this);
            }
        }
    }

    private void handleTouchMoved(double localX, double localY) {
        double diffX = localX - this.localOffsetX;
        double diffY = localY - this.localOffsetY;
        // check if the movement distance surpassed the threshold
        if (this.dragStarted || (diffX*diffX + diffY*diffY > this.dragThreshold*this.dragThreshold)) {
            if (! this.dragStarted) {
                this.dragStarted = true;
                // first call the drag initiation action if available 
                if (this.dragInitAction != null) {
                    this.dragInitAction.accept(this);
                }
            }
            
            // skip actual node relocation if the movement is too small 
            if ((Math.abs(diffX) > this.relocateThreshold) || (Math.abs(diffY) > this.relocateThreshold)) {
                double moveX = node.getLayoutX() + diffX;
                double moveY = node.getLayoutY() + diffY;
                // limit the movement by clamping on the drag boundaries
                double newX = Math.min(Math.max(moveX, this.dragLimits.getMinX()), this.dragLimits.getMaxX());
                double newY = Math.min(Math.max(moveY, this.dragLimits.getMinY()), this.dragLimits.getMaxY());
                node.relocate(newX, newY);
            }
        }
    }

    private void handleTouchReleased() {
        this.dragStarted = false;
        if (this.dragFinishAction != null) {
            this.dragFinishAction.accept(this);
        }
    }
    
    /** Make the attached Node stop acting on drag actions by removing drag event handlers */
    public void removeDragEventHandlers() {
        node.removeEventHandler(TouchEvent.ANY, touchHandler);
        node.removeEventHandler(MouseEvent.ANY, mouseHandler);
    }
    
    /**
     * The Node that is being dragged
     */
    public Node getDraggable() {
        return this.node;
    }
    
    /** Sets whether the attached node will go to foreground on contact.  */
    public void setGoToForegroundOnContact(boolean goToForegroundOnContact) {
        this.goToForegroundOnContact = goToForegroundOnContact;
    }

    /**
     * @param bounds to wherein the dragging is constrained.
     */
    public void setDragLimits(Bounds bounds) {
        this.dragLimits = bounds;
    }
    
    /**
     * @param threshold the initial drag distance before it is considered a proper drag action
     */
    public void setDragThreshold(double threshold) {
        this.dragThreshold = threshold;
    }

    /**
     * @param threshold minimal movement offset before a node relocation is triggered, to reduce wasteful redraws
     */
    public void setRelocateThreshold(double threshold) {
        this.relocateThreshold = threshold;
    }

    /**
     * @param action the method to be called when the node is contacted with touch or mouse, may be null 
     */
    public void setContactAction(Consumer<DragContext> action) {
        this.contactAction = action;
    }

    /**
     * @param action the method to be called when the node has been released, may be null 
     */
    public void setReleaseAction(Consumer<DragContext> action) {
        this.releaseAction = action;
    }
    
    /**
     * @param action the method to be called when a drag action has started, may be null 
     */
    public void setDragInitAction(Consumer<DragContext> action) {
        this.dragInitAction = action;
    }

    /**
     * @param action the method to be called when a secondary action is performed, may be null.
     * Secondary actions are either a right click on the mouse or a tap on the same node by a second finger
     * The boolean passed to the action is whether the action performed with a mouse
     */
    public void setSecondaryClickAction(BiConsumer<Point2D, Boolean> action) {
        this.secondaryClickAction = action;
    }

    /**
     * @param action the method to be called when a drag action has finished, may be null
     */
    public void setDragFinishAction(Consumer<DragContext> action) {
        this.dragFinishAction = action;
    }

    public boolean isActivated() {
        return this.activated;
    }
    
    @Override
    public String toString() {
        return String.format("DragContext [draggable = %s, ,touchId = %d, localOffsetX = %f, localOffsetY = %f]", node.toString(), touchId, localOffsetX, localOffsetY);
    }

}
