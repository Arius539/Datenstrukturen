package org.fpj.javafxController;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.control.ScrollPane;
import javafx.stage.Window;
import org.controlsfx.control.textfield.AutoCompletionBinding;
import org.controlsfx.control.textfield.TextFields;
import org.fpj.AlertService;
import org.fpj.Data.InfinitePager;
import org.fpj.Data.UiHelpers;
import org.fpj.Exceptions.DataNotPresentException;
import org.fpj.exportImport.application.WallCommentCsvExporter;
import org.fpj.exportImport.domain.FileHandling;
import org.fpj.messaging.domain.DirectMessage;
import org.fpj.users.application.UserService;
import org.fpj.users.domain.User;
import org.fpj.wall.domain.WallComment;
import org.fpj.wall.application.WallCommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import java.util.List;
import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_PROTOTYPE;

@Component
@Scope(SCOPE_PROTOTYPE)
public class WallCommentViewController {

    // FXML-Referenzen
    @FXML
    private TextField searchField;

    @FXML
    private Label headlineLabel;

    @FXML
    private GridPane commentGrid;

    @FXML
    private TextArea newCommentTextArea;

    @FXML
    private Button sendButton;

    @FXML
    private ScrollPane scrollPane;

    @FXML private RadioButton rbWrittenBy;
    @FXML private RadioButton rbWrittenFor;
    @FXML private Button exportButton;
    @FXML private Button reloadButton;
    @FXML private ToggleGroup commentFilterToggleGroup;


    @Autowired
    private WallCommentService wallCommentService;

    @Autowired
    private UserService userService;

    WallCommentCsvExporter wallCommentCsvExporter =new WallCommentCsvExporter();

    private static final int PAGE_SIZE_COMMENTS = 30;
    private static final double PAGE_PRE_FETCH_THRESHOLD = 0.1; // 10% vor dem Ende nachladen
    private static final int COLUMNS = 3;

    private InfinitePager<WallComment> commentsPager;

    private User currentUser;
    private User wallOwner;

    private AlertService alertService;

    @FXML
    private void initialize() {
       this.alertService  = new AlertService();
    }

    public void load(User currentUser, User wallCommentOwner){
        this.currentUser = currentUser;
        this.wallOwner = wallCommentOwner;
        setUpAutoCompletion();
        this.openWall();
    }

    private void setUpAutoCompletion() {
        AutoCompletionBinding<String> binding = TextFields.bindAutoCompletion(searchField, request -> {
            String term = request.getUserText();
            if (term == null || term.isBlank()) return List.of();
            return userService.usernameContaining(term);
        });

        binding.setOnAutoCompleted(event -> {
            String selected = event.getCompletion();
            searchField.setText(selected);
            openWallForUsername(selected);
        });
    }

    private void openWallForUsername(String username) {
        try{
            this.wallOwner = userService.findByUsername(username);
            reload();
        }catch (DataNotPresentException e){
            this.alertService.error("Error", "Error", "Es ist ein Fehler beim Laden der Pinnwand aufgetreten, versuche es erneut oder starte die Anwendung neu.");
        }
        catch (Exception e){
            this.alertService.error("Error", "Error", "Es ist ein unerwarteter Fehler beim Laden der Pinnwand aufgetreten, versuche es erneut oder starte die Anwendung neu.");
        }
    }

    private void openWall() {
        setHeadlineLabel();
        initComments();
        setupScrollPanel();
    }

    private void reload(){
        reloadComments();
        setHeadlineLabel();
    }

    private void reloadComments() {
        commentGrid.getChildren().clear();
        if (commentsPager != null) {
            commentsPager.resetAndLoadFirstPage();
            setupScrollPanel();
        } else {
            initComments();
            setupScrollPanel();
        }
    }

    private boolean isGetByAuthor(){
        return rbWrittenBy.isSelected();
    }

    private void setHeadlineLabel() {
        if (headlineLabel == null || wallOwner == null) {
            return;
        }
        String ownerName = wallOwner.getUsername();
        String currentName = currentUser != null ? currentUser.getUsername() : null;

        if (currentName != null && currentName.equals(ownerName)) {
            headlineLabel.setText("Deine Pinnwand");
        } else {
            headlineLabel.setText(ownerName);
        }
    }

    private void initComments() {
        commentGrid.getChildren().clear();

        commentsPager = new InfinitePager<>(
                PAGE_SIZE_COMMENTS,
                (pageIndex, pageSize) -> {
                    PageRequest pageRequest = PageRequest.of(pageIndex, pageSize);
                    if(isGetByAuthor()){
                        return wallCommentService.getWallCommentsByAuthor(wallOwner.getId(), pageRequest);
                    }else {
                        return wallCommentService.getWallCommentsCreatedByWallOwner(wallOwner.getId(), pageRequest);
                    }
                },
                page -> {
                    List<WallComment> content = page.getContent();

                    int beforeCount = commentGrid.getChildren().size();

                    for (int i = 0; i < content.size(); i++) {
                        WallComment c = content.get(i);
                        int logicalIndex = beforeCount + i;
                        int col = logicalIndex % COLUMNS;
                        int row = logicalIndex / COLUMNS;

                        Node card = createCommentCard(c);
                        commentGrid.add(card, col, row);
                        GridPane.setFillWidth(card, false);
                        GridPane.setHgrow(card, Priority.NEVER);
                        GridPane.setVgrow(card, Priority.NEVER);

                        GridPane.setHalignment(card, HPos.CENTER);
                        GridPane.setValignment(card, VPos.TOP);
                    }

                    if (beforeCount == 0 && !content.isEmpty()) {
                        Platform.runLater(() -> commentGrid.requestLayout());
                    }
                },
                ex -> showError("Pinnwand-Kommentare konnten nicht geladen werden: " +
                        (ex != null ? ex.getMessage() : "Unbekannter Fehler")),
                "wall-comments-loader-"
        );

        commentsPager.resetAndLoadFirstPage();
    }

    /**
     * Scroll-Listener: kurz vor dem unteren Ende der ScrollPane weitere Seiten laden.
     */
    private void setupScrollPanel() {
        if (scrollPane != null) {
            scrollPane.vvalueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal.doubleValue() >= 1.0 - PAGE_PRE_FETCH_THRESHOLD && commentsPager != null) {
                    commentsPager.ensureLoadedForScroll();
                }
            });
        }
    }

    @FXML
    private void onSendClicked() {
        String input = newCommentTextArea.getText();
        if (input == null || input.isBlank()) {
            return;
        }

        try {
            WallComment comment = new WallComment();
            comment.setContent(UiHelpers.truncate( input, input.length()));
            comment.setAuthor(currentUser);
            comment.setWallOwner(wallOwner);
            WallComment created = wallCommentService.add(comment);
            newCommentTextArea.clear();
            addComment(created);
        } catch (IllegalArgumentException e) {
            showError("Kommentar konnte nicht gespeichert werden: " + e.getMessage());
        } catch (Exception e) {
            showError("Es ist ein unerwarteter Fehler aufgetreten: Kommentar konnte nicht gespeichert werden: " + e.getMessage());
        }
    }

    private void addComment(WallComment comment) {
        int startIndex = commentGrid.getChildren().size();
        int col = startIndex % COLUMNS;
        int row = startIndex / COLUMNS;

        Node card = createCommentCard(comment);
        commentGrid.add(card, col, row);
    }


    private Node createCommentCard(WallComment comment) {
        VBox box = new VBox(8);
        box.getStyleClass().add("comment-card");

        box.setMaxWidth(Region.USE_PREF_SIZE);
        box.setMaxHeight(Region.USE_PREF_SIZE);

        box.setFillWidth(true);

        Label textLabel = new Label(comment.getContent());
        textLabel.setWrapText(true);
        textLabel.setMaxWidth(Region.USE_PREF_SIZE);
        VBox.setVgrow(textLabel, Priority.NEVER);

        String metaText = buildMetaText(comment);
        Label metaLabel = new Label(metaText);
        metaLabel.getStyleClass().add("comment-meta-label");

        metaLabel.setOnMouseClicked(event -> onMetaClicked(comment));
        metaLabel.setOnMouseEntered(e -> metaLabel.getStyleClass().add("hover"));
        metaLabel.setOnMouseExited(e -> metaLabel.getStyleClass().remove("hover"));

        box.getChildren().addAll(textLabel, metaLabel);
        return box;
    }


    private String buildMetaText(WallComment comment) {
        String rawName;
        if (isGetByAuthor()) {
            rawName = comment.getWallOwner() != null
                    ? comment.getWallOwner().getUsername()
                    : "Unbekannt";
        } else {
            rawName = comment.getAuthor() != null
                    ? comment.getAuthor().getUsername()
                    : "Unbekannt";
        }

        String authorName = (currentUser != null && rawName.equals(currentUser.getUsername())) ? "Du" : rawName;
        return authorName + " · " + UiHelpers.formatInstantToDate(comment.getCreatedAt());
    }

    private void onMetaClicked(WallComment comment) {
        User toUser= this.isGetByAuthor() ? comment.getWallOwner():  comment.getAuthor();
        this.wallOwner = toUser;
        this.reload();
    }

    @FXML
    private void onReloadWall() {
        reloadComments();
    }

    @FXML
    private void onExport() {
        try {
            if(!currentUser.getUsername().equals(wallOwner.getUsername())) throw new IllegalArgumentException("Du kannst nur die Pinnwandkommentare an deiner Wand exportieren");
            if(wallCommentCsvExporter.isRunning()) {
                throw new IllegalStateException("Ein andere Exporter instanz läuft noch. Warte bitte bis diese abgeschlossen ist.");
            }
            Window window = exportButton.getScene().getWindow();
            String path = FileHandling.openFileChooserAndGetPath(window);
            if (path == null) throw new IllegalStateException("Das auswählen des Dateipfades ist fehlgeschlagen");
            List<WallComment> comments= isGetByAuthor()? wallCommentService.toListByAuthor(this.currentUser.getId()): wallCommentService.toListByWallOwner(this.currentUser.getId());
            wallCommentCsvExporter.export(comments.iterator(),FileHandling.openFileAsOutStream(path));
            info("Der Export der Pinnwandkommentare war erfolgreich. Du findest die Einträge in: "+path);
        }catch (IllegalArgumentException | IllegalStateException e){
            showError("Fehler beim exportieren der Pinnwandkommentare: " + e.getMessage());
        } catch (Exception e) {
            showError("Ein Unbekannter Fehler ist aufgetreten: " + e.getMessage());
        }
    }


    @FXML
    private void selectionTypeChanged(){
        reloadComments();
    }

    private void showError(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Fehler");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    private void info(String text) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, text, ButtonType.OK);
        a.setHeaderText(null);
        a.setTitle("Info");
        a.showAndWait();
    }
}
