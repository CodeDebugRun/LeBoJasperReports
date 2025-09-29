package de.reports.gui.components;

import de.reports.i18n.MessageBundle;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PaginationComponent - Modular pagination UI component
 * Provides pagination controls with German localization
 *
 * Features:
 * - Previous/Next navigation
 * - Page number display and input
 * - Records per page selection
 * - Total records and pages info
 * - Callback interface for page changes
 */
public class PaginationComponent {
    private static final Logger logger = LoggerFactory.getLogger(PaginationComponent.class);

    // UI Components
    private VBox container;
    private HBox controlsBox;
    private Button firstPageButton;
    private Button previousPageButton;
    private TextField pageInputField;
    private Label pageInfoLabel;
    private Button nextPageButton;
    private Button lastPageButton;
    private ComboBox<Integer> recordsPerPageCombo;
    private Label totalRecordsLabel;

    // Pagination State
    private int currentPage = 1;
    private int totalPages = 1;
    private int recordsPerPage = 100;
    private long totalRecords = 0;

    // Callback Interface
    private PaginationCallback callback;

    public interface PaginationCallback {
        void onPageChanged(int page, int recordsPerPage);
        void onRecordsPerPageChanged(int recordsPerPage);
    }

    // Constructor
    public PaginationComponent() {
        initializeComponents();
        setupLayout();
        setupEventHandlers();
        updateUI();
    }

    private void initializeComponents() {
        // Container
        container = new VBox();
        container.setSpacing(10);
        container.setPadding(new Insets(10));
        container.getStyleClass().add("pagination-container");

        // Control buttons
        firstPageButton = new Button("<<");
        firstPageButton.setTooltip(new Tooltip(MessageBundle.getMessage("pagination.first.tooltip")));

        previousPageButton = new Button("<");
        previousPageButton.setTooltip(new Tooltip(MessageBundle.getMessage("pagination.previous.tooltip")));

        nextPageButton = new Button(">");
        nextPageButton.setTooltip(new Tooltip(MessageBundle.getMessage("pagination.next.tooltip")));

        lastPageButton = new Button(">>");
        lastPageButton.setTooltip(new Tooltip(MessageBundle.getMessage("pagination.last.tooltip")));

        // Page input
        pageInputField = new TextField();
        pageInputField.setPrefWidth(60);
        pageInputField.setPromptText("1");
        pageInputField.setTooltip(new Tooltip(MessageBundle.getMessage("pagination.page.input.tooltip")));

        // Page info
        pageInfoLabel = new Label();

        // Records per page
        recordsPerPageCombo = new ComboBox<>();
        recordsPerPageCombo.getItems().addAll(50, 100, 200, 500, 1000);
        recordsPerPageCombo.setValue(100);
        recordsPerPageCombo.setTooltip(new Tooltip(MessageBundle.getMessage("pagination.records.per.page.tooltip")));

        // Total records label
        totalRecordsLabel = new Label();
    }

    private void setupLayout() {
        // Controls box
        controlsBox = new HBox();
        controlsBox.setSpacing(5);
        controlsBox.setAlignment(Pos.CENTER);

        // Navigation controls
        controlsBox.getChildren().addAll(
            firstPageButton,
            previousPageButton,
            new Label(MessageBundle.getMessage("pagination.page.label")),
            pageInputField,
            pageInfoLabel,
            nextPageButton,
            lastPageButton
        );

        // Spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        controlsBox.getChildren().add(spacer);

        // Records per page
        controlsBox.getChildren().addAll(
            new Label(MessageBundle.getMessage("pagination.records.per.page.label")),
            recordsPerPageCombo
        );

        // Add to container
        container.getChildren().addAll(
            controlsBox,
            totalRecordsLabel
        );
    }

    private void setupEventHandlers() {
        // First page
        firstPageButton.setOnAction(e -> goToPage(1));

        // Previous page
        previousPageButton.setOnAction(e -> goToPage(currentPage - 1));

        // Next page
        nextPageButton.setOnAction(e -> goToPage(currentPage + 1));

        // Last page
        lastPageButton.setOnAction(e -> goToPage(totalPages));

        // Page input
        pageInputField.setOnAction(e -> {
            try {
                int page = Integer.parseInt(pageInputField.getText());
                goToPage(page);
            } catch (NumberFormatException ex) {
                pageInputField.setText(String.valueOf(currentPage));
            }
        });

        // Records per page changed
        recordsPerPageCombo.setOnAction(e -> {
            int newRecordsPerPage = recordsPerPageCombo.getValue();
            if (newRecordsPerPage != recordsPerPage) {
                recordsPerPage = newRecordsPerPage;
                // Reset to page 1 when changing records per page
                currentPage = 1;
                updatePagination();
                if (callback != null) {
                    callback.onRecordsPerPageChanged(recordsPerPage);
                }
            }
        });
    }

    private void goToPage(int page) {
        if (page < 1 || page > totalPages) {
            return;
        }

        currentPage = page;
        updateUI();

        if (callback != null) {
            callback.onPageChanged(currentPage, recordsPerPage);
        }
    }

    public void updatePagination() {
        calculateTotalPages();
        updateUI();
    }

    private void calculateTotalPages() {
        if (totalRecords == 0) {
            totalPages = 1;
        } else {
            totalPages = (int) Math.ceil((double) totalRecords / recordsPerPage);
        }

        // Ensure current page is valid
        if (currentPage > totalPages) {
            currentPage = totalPages;
        }
        if (currentPage < 1) {
            currentPage = 1;
        }
    }

    private void updateUI() {
        // Page input
        pageInputField.setText(String.valueOf(currentPage));

        // Page info
        pageInfoLabel.setText(MessageBundle.getMessage("pagination.page.info",
            String.valueOf(currentPage), String.valueOf(totalPages)));

        // Button states
        firstPageButton.setDisable(currentPage <= 1);
        previousPageButton.setDisable(currentPage <= 1);
        nextPageButton.setDisable(currentPage >= totalPages);
        lastPageButton.setDisable(currentPage >= totalPages);

        // Total records info
        long startRecord = (currentPage - 1) * recordsPerPage + 1;
        long endRecord = Math.min(currentPage * recordsPerPage, totalRecords);

        totalRecordsLabel.setText(MessageBundle.getMessage("pagination.records.info",
            String.valueOf(startRecord),
            String.valueOf(endRecord),
            String.valueOf(totalRecords)));
    }

    // Public API
    public VBox getContainer() {
        return container;
    }

    public void setCallback(PaginationCallback callback) {
        this.callback = callback;
    }

    public void setTotalRecords(long totalRecords) {
        this.totalRecords = totalRecords;
        updatePagination();
    }

    public void setCurrentPage(int page) {
        if (page >= 1 && page <= totalPages) {
            currentPage = page;
            updateUI();
        }
    }

    public void setRecordsPerPage(int recordsPerPage) {
        this.recordsPerPage = recordsPerPage;
        recordsPerPageCombo.setValue(recordsPerPage);
        updatePagination();
    }

    // Getters
    public int getCurrentPage() {
        return currentPage;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public int getRecordsPerPage() {
        return recordsPerPage;
    }

    public long getTotalRecords() {
        return totalRecords;
    }

    // Reset pagination
    public void reset() {
        currentPage = 1;
        totalRecords = 0;
        updatePagination();
    }
}