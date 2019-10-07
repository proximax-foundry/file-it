package io.proximax.app.fx.control.text;

import io.proximax.download.DownloadParameter;
import io.proximax.download.DownloadResult;
import io.proximax.download.Downloader;
import io.proximax.app.controller.AbstractController;
import io.proximax.app.core.ui.IApp;
import io.proximax.app.db.LocalAccount;
import io.proximax.app.db.LocalFile;
import io.proximax.app.utils.CONST;
import io.proximax.app.utils.LocalFileHelpers;
import io.proximax.upload.UploadParameter;
import io.proximax.upload.UploadResult;
import io.proximax.upload.Uploader;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Function;

import javafx.beans.binding.BooleanBinding;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.IndexRange;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Separator;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.stage.FileChooser;

import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.GenericStyledArea;
import org.fxmisc.richtext.StyledTextArea;
import org.fxmisc.richtext.TextExt;
import org.fxmisc.richtext.model.Codec;
import org.fxmisc.richtext.model.Paragraph;
import org.fxmisc.richtext.model.ReadOnlyStyledDocument;
import org.fxmisc.richtext.model.SegmentOps;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyledDocument;
import org.fxmisc.richtext.model.StyledSegment;
import org.fxmisc.richtext.model.TextOps;
import org.fxmisc.richtext.model.TwoDimensional;
import org.reactfx.SuspendableNo;
import org.reactfx.util.Either;
import org.reactfx.util.Tuple2;

public class RichTextEditor extends AbstractController {

    @FXML
    VBox vbox;
    // the saved/loaded files and their format are arbitrary and may change across versions
    private static final String RTFX_FILE_EXTENSION = ".rtfx";
    private static final String TXT_FILE_EXTENSION = ".txt";

    private final TextOps<String, TextStyle> styledTextOps = SegmentOps.styledTextOps();
    private final LinkedImageOps<TextStyle> linkedImageOps = new LinkedImageOps<>();

    private final GenericStyledArea<ParStyle, Either<String, LinkedImage>, TextStyle> area
            = new GenericStyledArea<>(
                    ParStyle.EMPTY, // default paragraph style
                    (paragraph, style) -> paragraph.setStyle(style.toCss()), // paragraph style setter

                    TextStyle.EMPTY.updateFontSize(12).updateFontFamily("Serif").updateTextColor(Color.BLACK), // default segment style
                    styledTextOps._or(linkedImageOps, (s1, s2) -> Optional.empty()), // segment operations
                    seg -> createNode(seg, (text, style) -> text.setStyle(style.toCss())));                     // Node creator and segment style setter

    {
        area.setWrapText(true);
        area.setStyleCodecs(
                ParStyle.CODEC,
                Codec.styledSegmentCodec(Codec.eitherCodec(Codec.STRING_CODEC, LinkedImage.codec()), TextStyle.CODEC));
    }

    private boolean isRichText = false;

    private final SuspendableNo updatingToolbar = new SuspendableNo();

    private LocalAccount localAccount = null;
    private LocalFile localFile = null;

    public RichTextEditor(LocalAccount localAccount, LocalFile localFile) {
        super(true);
        this.localAccount = localAccount;
        this.localFile = localFile;
    }

    public RichTextEditor(LocalAccount localAccount) {
        super(true);
        this.localAccount = localAccount;
    }

    public RichTextEditor() {
        super(true);
    }

    @Override
    protected void initialize() {
        reloadContent();
        area.requestFocus();
    }

    protected Scene createSceneEx(String fxml) throws IOException {
        Button loadBtn = createButton("new", this::loadDocument,
                "Load document.");
        Button saveBtn = createButton("savefile", this::saveDocument,
                "Save document");
        CheckBox wrapToggle = new CheckBox("Wrap");
        wrapToggle.setSelected(true);
        area.wrapTextProperty().bind(wrapToggle.selectedProperty());
        Button undoBtn = createButton("undo", area::undo, "Undo");
        Button redoBtn = createButton("redo", area::redo, "Redo");
        Button cutBtn = createButton("cut", area::cut, "Cut");
        Button copyBtn = createButton("copy", area::copy, "Copy");
        Button pasteBtn = createButton("paste", area::paste, "Paste");
        Button boldBtn = createButton("bold", this::toggleBold, "Bold");
        Button italicBtn = createButton("italic", this::toggleItalic, "Italic");
        Button underlineBtn = createButton("underline", this::toggleUnderline, "Underline");
        Button strikeBtn = createButton("strikethrough", this::toggleStrikethrough, "Strike Trough");
        Button insertImageBtn = createButton("insertimage", this::insertImage, "Insert Image");
        ToggleGroup alignmentGrp = new ToggleGroup();
        ToggleButton alignLeftBtn = createToggleButton(alignmentGrp, "align-left", this::alignLeft, "Align left");
        ToggleButton alignCenterBtn = createToggleButton(alignmentGrp, "align-center", this::alignCenter, "Align center");
        ToggleButton alignRightBtn = createToggleButton(alignmentGrp, "align-right", this::alignRight, "Align right");
        ToggleButton alignJustifyBtn = createToggleButton(alignmentGrp, "align-justify", this::alignJustify, "Justify");
        ColorPicker paragraphBackgroundPicker = new ColorPicker();
        ComboBox<Integer> sizeCombo = new ComboBox<>(FXCollections.observableArrayList(5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 16, 18, 20, 22, 24, 28, 32, 36, 40, 48, 56, 64, 72));
        sizeCombo.getSelectionModel().select(Integer.valueOf(12));
        sizeCombo.setTooltip(new Tooltip("Font size"));
        ComboBox<String> familyCombo = new ComboBox<>(FXCollections.observableList(Font.getFamilies()));
        familyCombo.getSelectionModel().select("Serif");
        familyCombo.setTooltip(new Tooltip("Font family"));
        ColorPicker textColorPicker = new ColorPicker(Color.BLACK);
        ColorPicker backgroundColorPicker = new ColorPicker();

        paragraphBackgroundPicker.setTooltip(new Tooltip("Paragraph background"));
        textColorPicker.setTooltip(new Tooltip("Text color"));
        backgroundColorPicker.setTooltip(new Tooltip("Text background"));

        paragraphBackgroundPicker.valueProperty().addListener((o, old, color) -> updateParagraphBackground(color));
        sizeCombo.setOnAction(evt -> updateFontSize(sizeCombo.getValue()));
        familyCombo.setOnAction(evt -> updateFontFamily(familyCombo.getValue()));
        textColorPicker.valueProperty().addListener((o, old, color) -> updateTextColor(color));
        backgroundColorPicker.valueProperty().addListener((o, old, color) -> updateBackgroundColor(color));

        undoBtn.disableProperty().bind(area.undoAvailableProperty().map(x -> !x));
        redoBtn.disableProperty().bind(area.redoAvailableProperty().map(x -> !x));

        BooleanBinding selectionEmpty = new BooleanBinding() {
            {
                bind(area.selectionProperty());
            }

            @Override
            protected boolean computeValue() {
                return area.getSelection().getLength() == 0;
            }
        };

        cutBtn.disableProperty().bind(selectionEmpty);
        copyBtn.disableProperty().bind(selectionEmpty);

        area.beingUpdatedProperty().addListener((o, old, beingUpdated) -> {
            if (!beingUpdated) {
                boolean bold, italic, underline, strike;
                Integer fontSize;
                String fontFamily;
                Color textColor;
                Color backgroundColor;

                IndexRange selection = area.getSelection();
                if (selection.getLength() != 0) {
                    StyleSpans<TextStyle> styles = area.getStyleSpans(selection);
                    bold = styles.styleStream().anyMatch(s -> s.bold.orElse(false));
                    italic = styles.styleStream().anyMatch(s -> s.italic.orElse(false));
                    underline = styles.styleStream().anyMatch(s -> s.underline.orElse(false));
                    strike = styles.styleStream().anyMatch(s -> s.strikethrough.orElse(false));
                    int[] sizes = styles.styleStream().mapToInt(s -> s.fontSize.orElse(-1)).distinct().toArray();
                    fontSize = sizes.length == 1 ? sizes[0] : -1;
                    String[] families = styles.styleStream().map(s -> s.fontFamily.orElse(null)).distinct().toArray(String[]::new);
                    fontFamily = families.length == 1 ? families[0] : null;
                    Color[] colors = styles.styleStream().map(s -> s.textColor.orElse(null)).distinct().toArray(Color[]::new);
                    textColor = colors.length == 1 ? colors[0] : null;
                    Color[] backgrounds = styles.styleStream().map(s -> s.backgroundColor.orElse(null)).distinct().toArray(i -> new Color[i]);
                    backgroundColor = backgrounds.length == 1 ? backgrounds[0] : null;
                } else {
                    int p = area.getCurrentParagraph();
                    int col = area.getCaretColumn();
                    TextStyle style = area.getStyleAtPosition(p, col);
                    bold = style.bold.orElse(false);
                    italic = style.italic.orElse(false);
                    underline = style.underline.orElse(false);
                    strike = style.strikethrough.orElse(false);
                    fontSize = style.fontSize.orElse(-1);
                    fontFamily = style.fontFamily.orElse(null);
                    textColor = style.textColor.orElse(null);
                    backgroundColor = style.backgroundColor.orElse(null);
                }

                int startPar = area.offsetToPosition(selection.getStart(), TwoDimensional.Bias.Forward).getMajor();
                int endPar = area.offsetToPosition(selection.getEnd(), TwoDimensional.Bias.Backward).getMajor();
                List<Paragraph<ParStyle, Either<String, LinkedImage>, TextStyle>> pars = area.getParagraphs().subList(startPar, endPar + 1);

                @SuppressWarnings("unchecked")
                Optional<TextAlignment>[] alignments = pars.stream().map(p -> p.getParagraphStyle().alignment).distinct().toArray(Optional[]::new);
                Optional<TextAlignment> alignment = alignments.length == 1 ? alignments[0] : Optional.empty();

                @SuppressWarnings("unchecked")
                Optional<Color>[] paragraphBackgrounds = pars.stream().map(p -> p.getParagraphStyle().backgroundColor).distinct().toArray(Optional[]::new);
                Optional<Color> paragraphBackground = paragraphBackgrounds.length == 1 ? paragraphBackgrounds[0] : Optional.empty();

                updatingToolbar.suspendWhile(() -> {
                    if (bold) {
                        if (!boldBtn.getStyleClass().contains("pressed")) {
                            boldBtn.getStyleClass().add("pressed");
                        }
                    } else {
                        boldBtn.getStyleClass().remove("pressed");
                    }

                    if (italic) {
                        if (!italicBtn.getStyleClass().contains("pressed")) {
                            italicBtn.getStyleClass().add("pressed");
                        }
                    } else {
                        italicBtn.getStyleClass().remove("pressed");
                    }

                    if (underline) {
                        if (!underlineBtn.getStyleClass().contains("pressed")) {
                            underlineBtn.getStyleClass().add("pressed");
                        }
                    } else {
                        underlineBtn.getStyleClass().remove("pressed");
                    }

                    if (strike) {
                        if (!strikeBtn.getStyleClass().contains("pressed")) {
                            strikeBtn.getStyleClass().add("pressed");
                        }
                    } else {
                        strikeBtn.getStyleClass().remove("pressed");
                    }

                    if (alignment.isPresent()) {
                        TextAlignment al = alignment.get();
                        switch (al) {
                            case LEFT:
                                alignmentGrp.selectToggle(alignLeftBtn);
                                break;
                            case CENTER:
                                alignmentGrp.selectToggle(alignCenterBtn);
                                break;
                            case RIGHT:
                                alignmentGrp.selectToggle(alignRightBtn);
                                break;
                            case JUSTIFY:
                                alignmentGrp.selectToggle(alignJustifyBtn);
                                break;
                        }
                    } else {
                        alignmentGrp.selectToggle(null);
                    }

                    paragraphBackgroundPicker.setValue(paragraphBackground.orElse(null));

                    if (fontSize != -1) {
                        sizeCombo.getSelectionModel().select(fontSize);
                    } else {
                        sizeCombo.getSelectionModel().clearSelection();
                    }

                    if (fontFamily != null) {
                        familyCombo.getSelectionModel().select(fontFamily);
                    } else {
                        familyCombo.getSelectionModel().clearSelection();
                    }

                    if (textColor != null) {
                        textColorPicker.setValue(textColor);
                    }

                    backgroundColorPicker.setValue(backgroundColor);
                });
            }
        });
        VBox vbox = new VBox();
        VirtualizedScrollPane<GenericStyledArea<ParStyle, Either<String, LinkedImage>, TextStyle>> vsPane = new VirtualizedScrollPane<>(area);
        VBox.setVgrow(vsPane, Priority.ALWAYS);
        if (isSupportRichText()) {
            ToolBar toolBar1 = new ToolBar(
                    loadBtn, saveBtn, new Separator(Orientation.VERTICAL),
                    wrapToggle, new Separator(Orientation.VERTICAL),
                    undoBtn, redoBtn, new Separator(Orientation.VERTICAL),
                    cutBtn, copyBtn, pasteBtn, new Separator(Orientation.VERTICAL),
                    boldBtn, italicBtn, underlineBtn, strikeBtn, new Separator(Orientation.VERTICAL),
                    alignLeftBtn, alignCenterBtn, alignRightBtn, alignJustifyBtn, new Separator(Orientation.VERTICAL),
                    insertImageBtn, new Separator(Orientation.VERTICAL),
                    paragraphBackgroundPicker);
            ToolBar toolBar2 = new ToolBar(sizeCombo, familyCombo, textColorPicker, backgroundColorPicker);
            vbox.getChildren().addAll(toolBar1, toolBar2, vsPane);
        } else {
            loadBtn.setDisable(true);
//            saveBtn.setDisable(true);
            ToolBar toolBar1 = new ToolBar(
                    loadBtn, saveBtn, new Separator(Orientation.VERTICAL),
                    wrapToggle, new Separator(Orientation.VERTICAL),
                    undoBtn, redoBtn, new Separator(Orientation.VERTICAL),
                    cutBtn, copyBtn, pasteBtn, new Separator(Orientation.VERTICAL));
            vbox.getChildren().addAll(toolBar1, vsPane);
        }
        Scene scene = new Scene(vbox, 800, 600);
        mainPane = vbox;
        shadowPane = null;
        return scene;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Button loadBtn = createButton("new", this::loadDocument,
                "Load document.");
        Button saveBtn = createButton("savefile", this::saveDocument,
                "Save document");
        ToggleButton wrapToggle = createToggleButton(null, "wrap", null, "Wrap");
        wrapToggle.setSelected(true);
        area.wrapTextProperty().bind(wrapToggle.selectedProperty());
        Button undoBtn = createButton("undo", area::undo, "Undo");
        Button redoBtn = createButton("redo", area::redo, "Redo");
        Button cutBtn = createButton("cut", area::cut, "Cut");
        Button copyBtn = createButton("copy", area::copy, "Copy");
        Button pasteBtn = createButton("paste", area::paste, "Paste");
        Button boldBtn = createButton("bold", this::toggleBold, "Bold");
        Button italicBtn = createButton("italic", this::toggleItalic, "Italic");
        Button underlineBtn = createButton("underline", this::toggleUnderline, "Underline");
        Button strikeBtn = createButton("strikethrough", this::toggleStrikethrough, "Strike Trough");
        Button insertImageBtn = createButton("insertimage", this::insertImage, "Insert Image");
        ToggleGroup alignmentGrp = new ToggleGroup();
        ToggleButton alignLeftBtn = createToggleButton(alignmentGrp, "align-left", this::alignLeft, "Align left");
        ToggleButton alignCenterBtn = createToggleButton(alignmentGrp, "align-center", this::alignCenter, "Align center");
        ToggleButton alignRightBtn = createToggleButton(alignmentGrp, "align-right", this::alignRight, "Align right");
        ToggleButton alignJustifyBtn = createToggleButton(alignmentGrp, "align-justify", this::alignJustify, "Justify");
        ColorPicker paragraphBackgroundPicker = new ColorPicker();
        ComboBox<Integer> sizeCombo = new ComboBox<>(FXCollections.observableArrayList(5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 16, 18, 20, 22, 24, 28, 32, 36, 40, 48, 56, 64, 72));
        sizeCombo.getSelectionModel().select(Integer.valueOf(12));
        sizeCombo.setTooltip(new Tooltip("Font size"));
        ComboBox<String> familyCombo = new ComboBox<>(FXCollections.observableList(Font.getFamilies()));
        familyCombo.getSelectionModel().select("Serif");
        familyCombo.setTooltip(new Tooltip("Font family"));
        ColorPicker textColorPicker = new ColorPicker(Color.BLACK);
        ColorPicker backgroundColorPicker = new ColorPicker();

        paragraphBackgroundPicker.setTooltip(new Tooltip("Paragraph background"));
        textColorPicker.setTooltip(new Tooltip("Text color"));
        backgroundColorPicker.setTooltip(new Tooltip("Text background"));

        paragraphBackgroundPicker.valueProperty().addListener((o, old, color) -> updateParagraphBackground(color));
        sizeCombo.setOnAction(evt -> updateFontSize(sizeCombo.getValue()));
        familyCombo.setOnAction(evt -> updateFontFamily(familyCombo.getValue()));
        textColorPicker.valueProperty().addListener((o, old, color) -> updateTextColor(color));
        backgroundColorPicker.valueProperty().addListener((o, old, color) -> updateBackgroundColor(color));

        undoBtn.disableProperty().bind(area.undoAvailableProperty().map(x -> !x));
        redoBtn.disableProperty().bind(area.redoAvailableProperty().map(x -> !x));

        BooleanBinding selectionEmpty = new BooleanBinding() {
            {
                bind(area.selectionProperty());
            }

            @Override
            protected boolean computeValue() {
                return area.getSelection().getLength() == 0;
            }
        };

        cutBtn.disableProperty().bind(selectionEmpty);
        copyBtn.disableProperty().bind(selectionEmpty);

        area.beingUpdatedProperty().addListener((o, old, beingUpdated) -> {
            if (!beingUpdated) {
                boolean bold, italic, underline, strike;
                Integer fontSize;
                String fontFamily;
                Color textColor;
                Color backgroundColor;

                IndexRange selection = area.getSelection();
                if (selection.getLength() != 0) {
                    StyleSpans<TextStyle> styles = area.getStyleSpans(selection);
                    bold = styles.styleStream().anyMatch(s -> s.bold.orElse(false));
                    italic = styles.styleStream().anyMatch(s -> s.italic.orElse(false));
                    underline = styles.styleStream().anyMatch(s -> s.underline.orElse(false));
                    strike = styles.styleStream().anyMatch(s -> s.strikethrough.orElse(false));
                    int[] sizes = styles.styleStream().mapToInt(s -> s.fontSize.orElse(-1)).distinct().toArray();
                    fontSize = sizes.length == 1 ? sizes[0] : -1;
                    String[] families = styles.styleStream().map(s -> s.fontFamily.orElse(null)).distinct().toArray(String[]::new);
                    fontFamily = families.length == 1 ? families[0] : null;
                    Color[] colors = styles.styleStream().map(s -> s.textColor.orElse(null)).distinct().toArray(Color[]::new);
                    textColor = colors.length == 1 ? colors[0] : null;
                    Color[] backgrounds = styles.styleStream().map(s -> s.backgroundColor.orElse(null)).distinct().toArray(i -> new Color[i]);
                    backgroundColor = backgrounds.length == 1 ? backgrounds[0] : null;
                } else {
                    int p = area.getCurrentParagraph();
                    int col = area.getCaretColumn();
                    TextStyle style = area.getStyleAtPosition(p, col);
                    bold = style.bold.orElse(false);
                    italic = style.italic.orElse(false);
                    underline = style.underline.orElse(false);
                    strike = style.strikethrough.orElse(false);
                    fontSize = style.fontSize.orElse(-1);
                    fontFamily = style.fontFamily.orElse(null);
                    textColor = style.textColor.orElse(null);
                    backgroundColor = style.backgroundColor.orElse(null);
                }

                int startPar = area.offsetToPosition(selection.getStart(), TwoDimensional.Bias.Forward).getMajor();
                int endPar = area.offsetToPosition(selection.getEnd(), TwoDimensional.Bias.Backward).getMajor();
                List<Paragraph<ParStyle, Either<String, LinkedImage>, TextStyle>> pars = area.getParagraphs().subList(startPar, endPar + 1);

                @SuppressWarnings("unchecked")
                Optional<TextAlignment>[] alignments = pars.stream().map(p -> p.getParagraphStyle().alignment).distinct().toArray(Optional[]::new);
                Optional<TextAlignment> alignment = alignments.length == 1 ? alignments[0] : Optional.empty();

                @SuppressWarnings("unchecked")
                Optional<Color>[] paragraphBackgrounds = pars.stream().map(p -> p.getParagraphStyle().backgroundColor).distinct().toArray(Optional[]::new);
                Optional<Color> paragraphBackground = paragraphBackgrounds.length == 1 ? paragraphBackgrounds[0] : Optional.empty();

                updatingToolbar.suspendWhile(() -> {
                    if (bold) {
                        if (!boldBtn.getStyleClass().contains("pressed")) {
                            boldBtn.getStyleClass().add("pressed");
                        }
                    } else {
                        boldBtn.getStyleClass().remove("pressed");
                    }

                    if (italic) {
                        if (!italicBtn.getStyleClass().contains("pressed")) {
                            italicBtn.getStyleClass().add("pressed");
                        }
                    } else {
                        italicBtn.getStyleClass().remove("pressed");
                    }

                    if (underline) {
                        if (!underlineBtn.getStyleClass().contains("pressed")) {
                            underlineBtn.getStyleClass().add("pressed");
                        }
                    } else {
                        underlineBtn.getStyleClass().remove("pressed");
                    }

                    if (strike) {
                        if (!strikeBtn.getStyleClass().contains("pressed")) {
                            strikeBtn.getStyleClass().add("pressed");
                        }
                    } else {
                        strikeBtn.getStyleClass().remove("pressed");
                    }

                    if (alignment.isPresent()) {
                        TextAlignment al = alignment.get();
                        switch (al) {
                            case LEFT:
                                alignmentGrp.selectToggle(alignLeftBtn);
                                break;
                            case CENTER:
                                alignmentGrp.selectToggle(alignCenterBtn);
                                break;
                            case RIGHT:
                                alignmentGrp.selectToggle(alignRightBtn);
                                break;
                            case JUSTIFY:
                                alignmentGrp.selectToggle(alignJustifyBtn);
                                break;
                        }
                    } else {
                        alignmentGrp.selectToggle(null);
                    }

                    paragraphBackgroundPicker.setValue(paragraphBackground.orElse(null));

                    if (fontSize != -1) {
                        sizeCombo.getSelectionModel().select(fontSize);
                    } else {
                        sizeCombo.getSelectionModel().clearSelection();
                    }

                    if (fontFamily != null) {
                        familyCombo.getSelectionModel().select(fontFamily);
                    } else {
                        familyCombo.getSelectionModel().clearSelection();
                    }

                    if (textColor != null) {
                        textColorPicker.setValue(textColor);
                    }

                    backgroundColorPicker.setValue(backgroundColor);
                });
            }
        });
        VirtualizedScrollPane<GenericStyledArea<ParStyle, Either<String, LinkedImage>, TextStyle>> vsPane = new VirtualizedScrollPane<>(area);
        VBox.setVgrow(vsPane, Priority.ALWAYS);
        if (isSupportRichText()) {
            ToolBar toolBar1 = new ToolBar(
                    loadBtn, saveBtn, new Separator(Orientation.VERTICAL),
                    wrapToggle, new Separator(Orientation.VERTICAL),
                    undoBtn, redoBtn, new Separator(Orientation.VERTICAL),
                    cutBtn, copyBtn, pasteBtn, new Separator(Orientation.VERTICAL),
                    boldBtn, italicBtn, underlineBtn, strikeBtn, new Separator(Orientation.VERTICAL),
                    alignLeftBtn, alignCenterBtn, alignRightBtn, alignJustifyBtn, new Separator(Orientation.VERTICAL),
                    insertImageBtn, new Separator(Orientation.VERTICAL),
                    paragraphBackgroundPicker);
            ToolBar toolBar2 = new ToolBar(sizeCombo, familyCombo, textColorPicker, backgroundColorPicker);
            vbox.getChildren().addAll(toolBar1, toolBar2, vsPane);
        } else {
            loadBtn.setDisable(true);
//            saveBtn.setDisable(true);
            ToolBar toolBar1 = new ToolBar(
                    loadBtn, saveBtn, new Separator(Orientation.VERTICAL),
                    wrapToggle, new Separator(Orientation.VERTICAL),
                    undoBtn, redoBtn, new Separator(Orientation.VERTICAL),
                    cutBtn, copyBtn, pasteBtn, new Separator(Orientation.VERTICAL));
            vbox.getChildren().addAll(toolBar1, vsPane);
        }
    }

    private void reloadContent() {
        if (localFile != null) {
            Alert alert = new Alert(
                    Alert.AlertType.INFORMATION,
                    "Operation in progress",
                    ButtonType.CANCEL
            );
            alert.setTitle("Load File");
            alert.setHeaderText("Please wait... ");
            ProgressIndicator progressIndicator = new ProgressIndicator();
            alert.setGraphic(progressIndicator);

            Task<Void> task = new Task<Void>() {
                final int N_ITERATIONS = 999;

                {
                    setOnFailed(a -> {
                        alert.close();
                        updateMessage("Failed");
                        close();
                        showParent();
                    });
                    setOnSucceeded(a -> {
                        alert.close();
                        updateMessage("Succeeded");
                    });
                    setOnCancelled(a -> {
                        alert.close();
                        updateMessage("Cancelled");
                        close();
                        showParent();
                    });
                }

                @Override
                protected Void call() throws Exception {
                    updateMessage("Processing");
                    updateProgress(100, N_ITERATIONS);
                    File fileCache = LocalFileHelpers.getSourceFile(localAccount, localFile);
                    if (fileCache == null) {
                        try {
                            fileCache = LocalFileHelpers.createFileCache(localAccount, localFile);
                            //need download
                            DownloadParameter parameter = LocalFileHelpers.createDownloadParameter(localFile);
                            Downloader download = new Downloader(localAccount.connectionConfig);
                            DownloadResult downloadResult = download.download(parameter);
                            InputStream byteStream = downloadResult.getData().getByteStream();
                            FileOutputStream fouts = new FileOutputStream(fileCache);
                            byte[] buffer = new byte[1024];
                            int read = 0;
                            long sum = 0;
                            while ((read = byteStream.read(buffer)) >= 0) {
                                fouts.write(buffer, 0, read);
                                sum += read;
                                updateProgress(100 + 800 * sum / localFile.fileSize, N_ITERATIONS);
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            failed();
                        }
                    }
                    final File file = fileCache;
                    IApp.runSafe(() -> {
                        try {
                            updateProgress(N_ITERATIONS, N_ITERATIONS);
                            load(file);
                        } catch (Exception ex) {
                        }
                    });
                    if (!isCancelled()) {
                        updateProgress(0, N_ITERATIONS);
                    }

                    return null;
                }
            };
            progressIndicator.progressProperty().bind(task.progressProperty());
            Thread taskThread = new Thread(
                    task,
                    "proxipad-thread-" + taskExecution.getAndIncrement()
            );
            taskThread.start();
            alert.initOwner(primaryStage);
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.CANCEL && task.isRunning()) {
                task.cancel();
            }
        }
    }

    AtomicInteger taskExecution = new AtomicInteger(0);

    private void saveContent() {
        Alert alert = new Alert(
                Alert.AlertType.INFORMATION,
                "Operation in progress",
                ButtonType.CANCEL
        );
        alert.setTitle("Save File");
        alert.setHeaderText("Please wait... ");
        ProgressIndicator progressIndicator = new ProgressIndicator();
        alert.setGraphic(progressIndicator);

        Task<Void> task = new Task<Void>() {
            final int N_ITERATIONS = 6;

            {
                setOnFailed(a -> {
                    alert.close();
                    updateMessage("Failed");
                });
                setOnSucceeded(a -> {
                    alert.close();
                    updateMessage("Succeeded");
                    close();
                    showParent();
                });
                setOnCancelled(a -> {
                    alert.close();
                    updateMessage("Cancelled");
                });
            }

            @Override
            protected Void call() throws Exception {
                updateMessage("Processing");
                updateProgress(1, N_ITERATIONS);
                File file = new File(System.getProperty("java.io.tmpdir") + "/" + localFile.fileName);
                save(file);
                updateProgress(2, N_ITERATIONS);
                LocalFile saveFile = new LocalFile(localFile);
                saveFile.modified = file.lastModified();
                try {
                    updateProgress(3, N_ITERATIONS);
                    Uploader upload = new Uploader(localAccount.connectionConfig);
                    UploadParameter parameter = LocalFileHelpers.createUploadFileParameter(localAccount, saveFile, file);
                    UploadResult uploadResult = upload.upload(parameter);
                    updateProgress(4, N_ITERATIONS);
                    saveFile.uploadDate = System.currentTimeMillis();
                    saveFile.hash = uploadResult.getData().getDataHash();
                    saveFile.nemHash = uploadResult.getTransactionHash();
                    saveFile.fileSize = file.length();
                    saveFile.modified = file.lastModified();
                    LocalFileHelpers.updateFile(localAccount.fullName, localAccount.network, localFile, saveFile);
                    updateProgress(5, N_ITERATIONS);
                } catch (Exception ex) {
                    failed();
                }
                if (!isCancelled()) {
                    updateProgress(0, N_ITERATIONS);
                }
                return null;
            }
        };
        progressIndicator.progressProperty().bind(task.progressProperty());
        Thread taskThread = new Thread(
                task,
                "task-thread-" + taskExecution.getAndIncrement()
        );
        taskThread.start();
        alert.initOwner(primaryStage);
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.CANCEL && task.isRunning()) {
            task.cancel();
        }
    }

    private Node createNode(StyledSegment<Either<String, LinkedImage>, TextStyle> seg,
            BiConsumer<? super TextExt, TextStyle> applyStyle) {
        return seg.getSegment().unify(
                text -> StyledTextArea.createStyledTextNode(text, seg.getStyle(), applyStyle),
                LinkedImage::createNode
        );
    }

    @Deprecated
    private Button createButton(String styleClass, Runnable action) {
        return createButton(styleClass, action, null);
    }

    private Button createButton(String styleClass, Runnable action, String toolTip) {
        Button button = new Button();
        button.getStyleClass().add(styleClass);
        button.setOnAction(evt -> {
            action.run();
            area.requestFocus();
        });
        button.setPrefWidth(25);
        button.setPrefHeight(25);
        if (toolTip != null) {
            button.setTooltip(new Tooltip(toolTip));
        }
        return button;
    }

    private ToggleButton createToggleButton(ToggleGroup grp, String styleClass, Runnable action, String toolTip) {
        ToggleButton button = new ToggleButton();
        if (grp != null) {
            button.setToggleGroup(grp);
        }
        button.getStyleClass().add(styleClass);
        if (action != null) {
            button.setOnAction(evt -> {
                action.run();
                area.requestFocus();
            });
        }
        button.setPrefWidth(25);
        button.setPrefHeight(25);
        if (toolTip != null) {
            button.setTooltip(new Tooltip(toolTip));
        }
        return button;
    }

    private void toggleBold() {
        updateStyleInSelection(spans -> TextStyle.bold(!spans.styleStream().allMatch(style -> style.bold.orElse(false))));
    }

    private void toggleItalic() {
        updateStyleInSelection(spans -> TextStyle.italic(!spans.styleStream().allMatch(style -> style.italic.orElse(false))));
    }

    private void toggleUnderline() {
        updateStyleInSelection(spans -> TextStyle.underline(!spans.styleStream().allMatch(style -> style.underline.orElse(false))));
    }

    private void toggleStrikethrough() {
        updateStyleInSelection(spans -> TextStyle.strikethrough(!spans.styleStream().allMatch(style -> style.strikethrough.orElse(false))));
    }

    private void alignLeft() {
        updateParagraphStyleInSelection(ParStyle.alignLeft());
    }

    private void alignCenter() {
        updateParagraphStyleInSelection(ParStyle.alignCenter());
    }

    private void alignRight() {
        updateParagraphStyleInSelection(ParStyle.alignRight());
    }

    private void alignJustify() {
        updateParagraphStyleInSelection(ParStyle.alignJustify());
    }

    private void loadDocument() {
        String initialDir = System.getProperty("user.dir");
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Load document");
        fileChooser.setInitialDirectory(new File(initialDir));
//        fileChooser.setSelectedExtensionFilter(
//                new FileChooser.ExtensionFilter("Arbitrary RTFX file", "*" + RTFX_FILE_EXTENSION));
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("All files", "*.*"));
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("TXT files (*.txt)", "*" + TXT_FILE_EXTENSION));
        if (isSupportRichText()) {
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Arbitrary RTFX file", "*" + RTFX_FILE_EXTENSION));
        }
        File selectedFile = fileChooser.showOpenDialog(primaryStage);
        if (selectedFile != null) {
            area.clear();
            load(selectedFile);
        }
    }

    public void load(File file) {
        if (area.getStyleCodecs().isPresent()) {
            if (!isSupportRichText() || !file.getName().endsWith(RTFX_FILE_EXTENSION)) {
                // Write the content to the file
                try {
                    FileInputStream fis = new FileInputStream(file);
                    BufferedInputStream bis = new BufferedInputStream(fis);
                    // Read the file, and set its contents within the editor            
                    StringBuffer sb = new StringBuffer();
                    while (bis.available() > 0) {
                        sb.append((char) bis.read());
                    }
                    area.replaceSelection(sb.toString());
                } catch (Exception e) {
                }
            } else {
                Tuple2<Codec<ParStyle>, Codec<StyledSegment<Either<String, LinkedImage>, TextStyle>>> codecs = area.getStyleCodecs().get();
                Codec<StyledDocument<ParStyle, Either<String, LinkedImage>, TextStyle>> codec = ReadOnlyStyledDocument.codec(codecs._1, codecs._2, area.getSegOps());
                try {

                    FileInputStream fis = new FileInputStream(file);
                    DataInputStream dis = new DataInputStream(fis);
                    StyledDocument<ParStyle, Either<String, LinkedImage>, TextStyle> doc = codec.decode(dis);
                    fis.close();

                    if (doc != null) {
                        area.replaceSelection(doc);
                        return;
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void saveDocument() {
        if (localFile != null) {
            saveContent();
        } else {
            String initialDir = System.getProperty("user.dir");
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save document");
            fileChooser.setInitialDirectory(new File(initialDir));
            //fileChooser.setInitialFileName("example rtfx file" + RTFX_FILE_EXTENSION);
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("All files", "*.*"));
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("TXT files (*.txt)", "*" + TXT_FILE_EXTENSION));
            if (isSupportRichText()) {
                fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Arbitrary RTFX file", "*" + RTFX_FILE_EXTENSION));
            }
            fileChooser.setInitialFileName("proxibox" + TXT_FILE_EXTENSION);
            File selectedFile = fileChooser.showSaveDialog(primaryStage);
            if (selectedFile != null) {
                save(selectedFile);
            }
        }
    }

    private void save(File file) {
        // Write the content to the file
        if (!isSupportRichText() || !file.getName().endsWith(RTFX_FILE_EXTENSION)) {
            try {
                FileOutputStream fos = new FileOutputStream(file);
                BufferedOutputStream bos = new BufferedOutputStream(fos);
                String text = area.getText();
                bos.write(text.getBytes());
                bos.flush();
                bos.close();
                fos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            StyledDocument<ParStyle, Either<String, LinkedImage>, TextStyle> doc = area.getDocument();

            // Use the Codec to save the document in a binary format
            area.getStyleCodecs().ifPresent(codecs -> {
                Codec<StyledDocument<ParStyle, Either<String, LinkedImage>, TextStyle>> codec
                        = ReadOnlyStyledDocument.codec(codecs._1, codecs._2, area.getSegOps());
                try {
                    FileOutputStream fos = new FileOutputStream(file);
                    DataOutputStream dos = new DataOutputStream(fos);
                    codec.encode(dos, doc);
                    fos.close();
                } catch (IOException fnfe) {
                    fnfe.printStackTrace();
                }
            });
        }
    }

    /**
     * Action listener which inserts a new image at the current caret position.
     */
    private void insertImage() {
        String initialDir = System.getProperty("user.dir");
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Insert image");
        fileChooser.setInitialDirectory(new File(initialDir));
        File selectedFile = fileChooser.showOpenDialog(primaryStage);
        if (selectedFile != null) {
            String imagePath = selectedFile.getAbsolutePath();
            imagePath = imagePath.replace('\\', '/');
            ReadOnlyStyledDocument<ParStyle, Either<String, LinkedImage>, TextStyle> ros
                    = ReadOnlyStyledDocument.fromSegment(Either.right(new RealLinkedImage(imagePath)),
                            ParStyle.EMPTY, TextStyle.EMPTY, area.getSegOps());
            area.replaceSelection(ros);
        }
    }

    private void updateStyleInSelection(Function<StyleSpans<TextStyle>, TextStyle> mixinGetter) {
        IndexRange selection = area.getSelection();
        if (selection.getLength() != 0) {
            StyleSpans<TextStyle> styles = area.getStyleSpans(selection);
            TextStyle mixin = mixinGetter.apply(styles);
            StyleSpans<TextStyle> newStyles = styles.mapStyles(style -> style.updateWith(mixin));
            area.setStyleSpans(selection.getStart(), newStyles);
        }
    }

    private void updateStyleInSelection(TextStyle mixin) {
        IndexRange selection = area.getSelection();
        if (selection.getLength() != 0) {
            StyleSpans<TextStyle> styles = area.getStyleSpans(selection);
            StyleSpans<TextStyle> newStyles = styles.mapStyles(style -> style.updateWith(mixin));
            area.setStyleSpans(selection.getStart(), newStyles);
        }
    }

    private void updateParagraphStyleInSelection(Function<ParStyle, ParStyle> updater) {
        IndexRange selection = area.getSelection();
        int startPar = area.offsetToPosition(selection.getStart(), TwoDimensional.Bias.Forward).getMajor();
        int endPar = area.offsetToPosition(selection.getEnd(), TwoDimensional.Bias.Backward).getMajor();
        for (int i = startPar; i <= endPar; ++i) {
            Paragraph<ParStyle, Either<String, LinkedImage>, TextStyle> paragraph = area.getParagraph(i);
            area.setParagraphStyle(i, updater.apply(paragraph.getParagraphStyle()));
        }
    }

    private void updateParagraphStyleInSelection(ParStyle mixin) {
        updateParagraphStyleInSelection(style -> style.updateWith(mixin));
    }

    private void updateFontSize(Integer size) {
        if (!updatingToolbar.get()) {
            updateStyleInSelection(TextStyle.fontSize(size));
        }
    }

    private void updateFontFamily(String family) {
        if (!updatingToolbar.get()) {
            updateStyleInSelection(TextStyle.fontFamily(family));
        }
    }

    private void updateTextColor(Color color) {
        if (!updatingToolbar.get()) {
            updateStyleInSelection(TextStyle.textColor(color));
        }
    }

    private void updateBackgroundColor(Color color) {
        if (!updatingToolbar.get()) {
            updateStyleInSelection(TextStyle.backgroundColor(color));
        }
    }

    private void updateParagraphBackground(Color color) {
        if (!updatingToolbar.get()) {
            updateParagraphStyleInSelection(ParStyle.backgroundColor(color));
        }
    }

    public boolean isSupportRichText() {
        return isRichText;
    }

    @Override
    protected void dispose() {
        area.dispose();

    }

    @Override
    public String getTitle() {
        return CONST.PROXIPAD_TITLE;
    }

    @Override
    public String getFXML() {
        return CONST.PROXIPAD_FXML;
    }

}
