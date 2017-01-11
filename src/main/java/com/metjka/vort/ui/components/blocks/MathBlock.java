package com.metjka.vort.ui.components.blocks;

import com.google.common.collect.ImmutableList;
import com.metjka.vort.ui.ToplevelPane;
import com.metjka.vort.ui.Type;
import com.metjka.vort.ui.components.connections.InputAnchor;
import com.metjka.vort.ui.components.connections.OutputAnchor;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;

@Slf4j
public class MathBlock extends ValueBlock<Integer> {

    private static final Integer DEFAULT_VALUE = 0;

    enum Method {
        ADD, EXT, MIX, MIN, MAX, MULL
    }

    private Integer inValue1;
    private Integer inValue2;

    private InputAnchor inputAnchor1;
    private InputAnchor inputAnchor2;

    @FXML
    private Pane inputSpace1;

    @FXML
    private Pane inputSpace2;

    @FXML
    private Pane outputSpace;

    @FXML
    private TextField textField;

    @FXML
    private ComboBox<String> methodComboBox;

    private Method method;

    public MathBlock(ToplevelPane pane) {
        super(pane, "MathBlock");
        value1 = DEFAULT_VALUE;

        inputAnchor1 = new InputAnchor(this, Type.NUMBER);
        inputAnchor2 = new InputAnchor(this, Type.NUMBER);

        inputSpace1.getChildren().add(0, inputAnchor1);
        inputSpace2.getChildren().add(0, inputAnchor2);

        methodComboBox.getSelectionModel().select(0);
        method = Method.ADD;

        methodComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            log.info(newValue);
            method = getMethod(newValue);
            update();
        });

        log.info("Created new MathBlock");
    }

    private Method getMethod(String methodString) {
        switch (methodString) {
            case "ADD":
                return Method.ADD;
            case "EXT":
                return Method.EXT;
            case "MIX":
                return Method.MIX;
            case "MIN":
                return Method.MIN;
            case "MAX":
                return Method.MAX;
            case "MULL":
                return Method.MULL;
        }
        throw new IllegalArgumentException("Wrong method name!");
    }

    private int calculate(Integer a, Integer b) {
        switch (method) {
            case ADD:
                return a + b;
            case EXT:
                return a - b;
            case MIX:
                return (a + b) / 2;
            case MIN:
                return Math.min(a, b);
            case MAX:
                return Math.max(a, b);
            case MULL:
                return a * b;
        }
        return 0;
    }

    @Override
    public List<InputAnchor> getAllInputs() {
        return ImmutableList.of(inputAnchor1, inputAnchor2);
    }

    @Override
    public List<OutputAnchor> getAllOutputs() {
        return ImmutableList.of(outputAnchor);
    }

    @Override
    public void update() {

        inputAnchor1.invalidateVisualState();
        inputAnchor2.invalidateVisualState();
        log.info("Start of update in MathBlock, hash: {}", this.hashCode());

        if (inputAnchor1.getOppositeAnchor().isPresent() && inputAnchor2.getOppositeAnchor().isPresent()) {
            OutputAnchor outputAnchor1 = inputAnchor1.getOppositeAnchor().get();
            OutputAnchor outputAnchor2 = inputAnchor2.getOppositeAnchor().get();

            Block block1 = outputAnchor1.getBlock();
            int position1 = outputAnchor2.getPosition();

            Block block2 = outputAnchor2.getBlock();
            int position2 = outputAnchor2.getPosition();

            inValue1 = getValueFromBlock(block1, position1);
            inValue2 = getValueFromBlock(block2, position2);

            if (inValue1 != null && inValue2 != null) {
                value1 = calculate(inValue1, inValue2);
            } else {
                value1 = DEFAULT_VALUE;
            }

            textField.setText(value1.toString());

            sendUpdateDownSteam();
        }

    }

    private Integer getValueFromBlock(Block block1, int position1) {
        switch (position1) {
            case 1: {
                if (block1 instanceof OneOutputBlock) {
                    return (Integer) ((OneOutputBlock) block1).getValue1();
                } else throw new IllegalArgumentException();
            }
            case 2: {
                if (block1 instanceof TwoOutputBlock) {
                    return (Integer) ((OneOutputBlock) block1).getValue1();
                } else throw new IllegalArgumentException();
            }
            default:
                break;
        }
        return null;
    }

    @Override
    public Optional<Block> getNewCopy() {
        return null;
    }

    public Integer getValue1() {
        return value1;
    }

}
