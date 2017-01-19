package com.metjka.vort.ui;

import com.google.common.base.Charsets;
import com.metjka.vort.ui.serialize.Exporter;
import com.metjka.vort.ui.serialize.Importer;
import com.metjka.vort.ui.serialize.ViskellFormat;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * menu actions
 */
public class MenuActions {
    /**
     * The main overlay of which this menu is a part
     */
    protected MainOverlay overlay;

    private final ToplevelPane toplevelPane;

    /**
     * The File we're currently working on, if any.
     */
    private Optional<File> currentFile;

    public MenuActions(final MainOverlay ol, final ToplevelPane tl) {
        overlay = ol;
        toplevelPane = tl;
        newFile();
    }

    /**
     * Set the state to reflect that we are working on a new file
     */
    private void newFile() {
        this.currentFile = Optional.empty();
        VortApplication.getStage().setTitle("New file");
    }

    /**
     * Set the current file we are working on, including putting it's name in the window title
     *
     * @param currentFile we are now using
     */
    private void setFile(final File currentFile) {
        this.currentFile = Optional.of(currentFile);
        VortApplication.getStage().setTitle(currentFile.getName());
    }

    protected List<MenuItem> fileMenuItems() {
        List<MenuItem> list = new ArrayList<>();

        MenuItem menuNew = new MenuItem("New");
        menuNew.setAccelerator(new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN));
        menuNew.setOnAction(this::onNew);
        list.add(menuNew);

        MenuItem menuOpen = new MenuItem("Open...");
        menuOpen.setAccelerator(new KeyCodeCombination(KeyCode.O, KeyCombination.SHORTCUT_DOWN));
        menuOpen.setOnAction(this::onOpen);
        list.add(menuOpen);

        MenuItem menuSave = new MenuItem("Save");
        menuSave.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN));
        menuSave.setOnAction(this::onSave);
        list.add(menuSave);

        MenuItem menuSaveAs = new MenuItem("Save as...");
        menuSaveAs.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.SHIFT_DOWN, KeyCombination.SHORTCUT_DOWN));
        menuSaveAs.setOnAction(this::onSaveAs);
        list.add(menuSaveAs);

        return list;
    }

    protected void onNew(ActionEvent actionEvent) {
        toplevelPane.clearChildren();
        newFile();
    }

    /**
     * onOpen() acts more like a "load from file" than open that file. It reads all the objects from the specified file
     * and adds them to the UI, but doesn't assume that filename. The user can use this to read in objects to a file
     * they are already working on, to build-on the objects in the file - and then save to a different file later.
     *
     * They will have to choose the destination file when saving. They can choose to save to the same file they read
     * from if they want by selecting it in the dialog.
     *
     * @param actionEvent correspondign to the open request
     */
    protected void onOpen(ActionEvent actionEvent) {
        Window window = overlay.getScene().getWindow();
        File file = new FileChooser().showOpenDialog(window);

        if (file != null) {
            addChildrenFrom(file, toplevelPane);
        }
    }

    protected void onSave(ActionEvent actionEvent) {
        if (currentFile.isPresent()) {
            saveTo(currentFile.get());
        } else {
            onSaveAs(actionEvent);
        }
    }

    protected void onSaveAs(ActionEvent actionEvent) {
        Window window = overlay.getScene().getWindow();
        File file = new FileChooser().showSaveDialog(window);

        if (file != null) {
            saveTo(file);
            setFile(file);
        }
    }

    protected void addChildrenFrom(File file, ToplevelPane toplevelPane) {
        try (FileInputStream fis = new FileInputStream(file)) {
            Map<String, Object> layers = Importer.readLayers(fis);
            fis.close();

            // check we can read this version of the serialized file format
            Integer fileFormatversion = ((Double)layers.get(ViskellFormat.VERSION_NUMBER_LABEL)).intValue();
            if (ViskellFormat.SUPPORTED_IMPORT_VERSIONS.contains(fileFormatversion)) {
                toplevelPane.fromBundle(layers);
            } else {
                // TODO show a dialog telling the user this version of the file cannot be read, list the versions
                // that it can read, and pointing them to where they can download a newer version of the app to read the version
                System.err.println("This version of the app cannot read files saved in format version: " + fileFormatversion);
            }
        } catch (IOException e) {
            // TODO do something sensible here - like show a dialog
            e.printStackTrace();
        }
    }

    protected void saveTo(File file) {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(Exporter.export(toplevelPane).getBytes(Charsets.UTF_8));
            fos.close();
        } catch (IOException e) {
            // TODO do something sensible here
            e.printStackTrace();
        }
    }

    @SuppressWarnings("UnusedParameters")
    protected void toggleFullScreen(ActionEvent actionEvent) {
        Stage stage = VortApplication.getStage();
        if (stage.isFullScreen()) {
            stage.setFullScreen(false);
        } else {
            stage.setMaximized(true);
            stage.setFullScreen(true);
        }
    }

    @SuppressWarnings("UnusedParameters")
    protected void onQuit(ActionEvent actionEvent) {
        Platform.exit();
    }

    @SuppressWarnings("UnusedParameters")
    protected void zoomIn(ActionEvent actionEvent) {
        toplevelPane.zoom(1.1);
    }

    @SuppressWarnings("UnusedParameters")
    protected void zoomOut(ActionEvent actionEvent) {
        toplevelPane.zoom(1 / 1.1);
    }
}
