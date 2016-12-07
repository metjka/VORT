package com.metjka.vort.ui.components;

import com.google.common.collect.ImmutableMap;
import com.metjka.vort.ui.ToplevelPane;
import com.metjka.vort.ui.serialize.Bundleable;
import javafx.scene.control.TextInputDialog;

import java.util.Map;
import java.util.Optional;

/**
 * Block with a constant value that is editable as a plain text expression.
 */
public class ConstantBlock extends ValueBlock implements Bundleable {

    private boolean hasValidValue;

    public ConstantBlock(ToplevelPane pane) {
        super("ValueBlock", pane, TypeScope.unique("x"));
        this.setValue("undefined");
        this.hasValidValue = false;
        this.outputSpace.setVisible(false);
    }

    public ConstantBlock(ToplevelPane pane, Type type, String value, boolean hasValidValue) {
        super("ValueBlock", pane, type);
        this.setValue(value);
        this.hasValidValue = hasValidValue;
    }

    @Override
    protected ImmutableMap<String, Object> toBundleFragment() {
        return ImmutableMap.of(
                "value", getValue(),
                "type", type.toString(), // TODO this seems to create some kind of UTF-8 problem
                "hasValidValue", hasValidValue);
    }

    public static ConstantBlock fromBundleFragment(ToplevelPane pane, Map<String, Object> bundleFragment) throws ClassNotFoundException {
        String value = (String) bundleFragment.get("value");
        boolean hasValidValue = (Boolean) bundleFragment.get("hasValidValue");

        if (hasValidValue) {
            // Recover the type from the saved value.
            try {
                Type type = pane.getGhciSession().pullType(value, pane.getEnvInstance());
                return new ConstantBlock(pane, type, value, true);
            } catch (HaskellException e) {
                // Should not happen as it was typechecked before saving. 
            }
        }

        // Constant with invalid value or bad type. 
        ConstantBlock block = new ConstantBlock(pane);
        block.setValue(value);
        return block;
    }

    public void editValue(Optional<String> startValue) {
        TextInputDialog dialog = new TextInputDialog(startValue.orElse(this.getValue()));
        dialog.setTitle("Edit constant block");
        dialog.setHeaderText("Type a Haskell expression");

        Optional<String> result = dialog.showAndWait();

        result.ifPresent(value -> {
            this.setValue(value);
            GhciSession ghci = this.getToplevel().getGhciSession();

            try {
                Type type = ghci.pullType(value, this.getToplevel().getEnvInstance());
                this.output.setExactRequiredType(type);
                this.hasValidValue = true;
                this.outputSpace.setVisible(true);
            } catch (HaskellException e) {
                this.hasValidValue = false;
                this.outputSpace.setVisible(false);
            }

            this.initiateConnectionChanges();
        });
    }

    @Override
    public ConnectionAnchor getAssociatedAnchor() {
        if (hasValidValue) {
            return output;
        } else {
            return null;
        }
    }

    @Override
    public Optional<Block> getNewCopy() {
        return Optional.of(new ConstantBlock(this.getToplevel(), this.output.binder.getFreshAnnotationType(), this.value.getText(), this.hasValidValue));
    }

}
