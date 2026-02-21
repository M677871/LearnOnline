package com.example.demo.common;

import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

/**
 * Small helper to normalize TableView visuals.
 */
public final class TableUtils {
    private TableUtils() {}

    @SafeVarargs
    public static <T> void style(TableView<T> table, TableColumn<?, ?>... columns) {
        if (table != null) {
            table.setTableMenuButtonVisible(true); // show column menu (+)
        }
        if (columns != null) {
            for (TableColumn<?, ?> col : columns) {
                if (col != null) col.setStyle("-fx-alignment: CENTER; -fx-text-fill: #FFFFFF;");
            }
        }
    }
}
