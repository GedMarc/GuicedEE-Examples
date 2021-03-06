/*
 * Copyright 2011-2020 PrimeFaces Extensions
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.primefaces.extensions.showcase.util;

import java.awt.Color;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import jakarta.el.MethodExpression;
import jakarta.faces.FacesException;
import jakarta.faces.component.UIComponent;
import jakarta.faces.component.UIPanel;
import jakarta.faces.component.html.HtmlCommandButton;
import jakarta.faces.component.html.HtmlCommandLink;
import jakarta.faces.component.html.HtmlOutputText;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.faces.event.ActionEvent;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.usermodel.*;
import org.primefaces.component.api.DynamicColumn;
import org.primefaces.component.api.UIColumn;
import org.primefaces.component.column.Column;
import org.primefaces.component.columngroup.ColumnGroup;
import org.primefaces.component.datalist.DataList;
import org.primefaces.component.datatable.DataTable;
import org.primefaces.component.rowexpansion.RowExpansion;
import org.primefaces.component.subtable.SubTable;
import org.primefaces.expression.SearchExpressionFacade;
import org.primefaces.extensions.component.exporter.Exporter;
import org.primefaces.util.Constants;

/**
 * <code>Exporter</code> component.
 *
 * @author Sudheer Jonna / last modified by $Author$
 * @since 0.7.0
 */
public class ExcelCustomExporter extends Exporter {

    private CellStyle cellStyle;
    private CellStyle facetStyle;
    private CellStyle titleStyle;
    private Color facetBackground;
    private Short facetFontSize;
    private Color facetFontColor;
    private String facetFontStyle;
    private String fontName;
    private Short cellFontSize;
    private Color cellFontColor;
    private String cellFontStyle;
    private String datasetPadding;
    private XSSFWorkbook wb;

    @Override
    public void export(ActionEvent event, String tableId, FacesContext context, String filename, String tableTitle, boolean pageOnly, boolean selectionOnly,
                String encodingType, MethodExpression preProcessor, MethodExpression postProcessor, boolean subTable) throws IOException {

        wb = new XSSFWorkbook();
        String safeName = WorkbookUtil.createSafeSheetName(filename);
        Sheet sheet = wb.createSheet(safeName);

        cellStyle = wb.createCellStyle();
        facetStyle = wb.createCellStyle();
        titleStyle = wb.createCellStyle();
        createCustomFonts();

        int maxColumns = 0;
        StringTokenizer st = new StringTokenizer(tableId, ",");
        while (st.hasMoreElements()) {
            String tableName = (String) st.nextElement();
            UIComponent component = SearchExpressionFacade.resolveComponent(context, event.getComponent(), tableName);
            if (component == null) {
                throw new FacesException("Cannot find component \"" + tableName + "\" in view.");
            }
            if (!(component instanceof DataTable || component instanceof DataList)) {
                throw new FacesException(
                            "Unsupported datasource target:\"" + component.getClass().getName() + "\", exporter must target a PrimeFaces DataTable/DataList.");
            }

            DataList list = null;
            DataTable table = null;
            int cols = 0;
            if (preProcessor != null) {
                preProcessor.invoke(context.getELContext(), new Object[] {wb});
            }
            if (tableTitle != null && !tableTitle.isEmpty() && !tableId.contains("" + ",")) {
                Row titleRow = sheet.createRow(sheet.getLastRowNum());
                int cellIndex = titleRow.getLastCellNum() == -1 ? 0 : titleRow.getLastCellNum();
                Cell cell = titleRow.createCell(cellIndex);
                cell.setCellValue(new XSSFRichTextString(tableTitle));
                Font titleFont = wb.createFont();
                titleFont.setBold(true);
                titleStyle.setFont(titleFont);
                cell.setCellStyle(titleStyle);
                sheet.createRow(sheet.getLastRowNum() + 3);

            }
            if (component instanceof DataList) {
                list = (DataList) component;

                if (list.getHeader() != null) {
                    tableFacet(context, sheet, list, "header");
                }
                if (pageOnly) {
                    exportPageOnly(context, list, sheet);
                }
                else {
                    exportAll(context, list, sheet);
                }
                cols = list.getRowCount();
            }
            else {

                table = (DataTable) component;
                int columnsCount = Exporter.getColumnsCount(table);

                if (table.getHeader() != null && !subTable) {
                    tableFacet(context, sheet, table, columnsCount, "header");

                }
                if (!subTable) {
                    tableColumnGroup(sheet, table, "header");
                }

                addColumnFacets(table, sheet, ColumnType.HEADER);

                if (pageOnly) {
                    exportPageOnly(context, table, sheet);
                }
                else if (selectionOnly) {
                    exportSelectionOnly(context, table, sheet);
                }
                else {
                    exportAll(context, table, sheet, subTable);
                }

                if (table.hasFooterColumn() && !subTable) {
                    addColumnFacets(table, sheet, ColumnType.FOOTER);
                }
                if (!subTable) {
                    tableColumnGroup(sheet, table, "footer");
                }
                table.setRowIndex(-1);
                if (postProcessor != null) {
                    postProcessor.invoke(context.getELContext(), new Object[] {wb});
                }
                cols = table.getColumnsCount();

                if (maxColumns < cols) {
                    maxColumns = cols;
                }
            }
            sheet.createRow(sheet.getLastRowNum() + Integer.parseInt(datasetPadding));
        }

        if (!subTable) {
            for (int i = 0; i < maxColumns; i++) {
                sheet.autoSizeColumn((short) i);
            }
        }

        PrintSetup printSetup = sheet.getPrintSetup();
        printSetup.setLandscape(true);
        printSetup.setPaperSize(PrintSetup.A4_PAPERSIZE);
        sheet.setPrintGridlines(true);

        writeExcelToResponse(context.getExternalContext(), wb, filename);

    }

    protected void exportAll(FacesContext context, DataTable table, Sheet sheet, Boolean subTable) {

        int first = table.getFirst();
        int rowCount = table.getRowCount();
        int rows = table.getRows();
        boolean lazy = table.isLazy();
        int i = 0;
        if (subTable) {
            int subTableCount = table.getRowCount();
            SubTable subtable = table.getSubTable();
            int subTableColumnsCount = Exporter.getColumnsCount(subtable);

            if (table.getHeader() != null) {
                tableFacet(context, sheet, table, subTableColumnsCount, "header");
            }

            tableColumnGroup(sheet, table, "header");

            while (subTableCount > 0) {

                subTableCount--;
                table.setRowIndex(i);
                i++;
                if (subtable.getHeader() != null) {
                    tableFacet(context, sheet, subtable, subTableColumnsCount, "header");
                }

                if (Exporter.hasHeaderColumn(subtable)) {
                    addColumnFacets(subtable, sheet, ColumnType.HEADER);
                }

                exportAll(context, subtable, sheet);

                if (Exporter.hasFooterColumn(subtable)) {

                    addColumnFacets(subtable, sheet, ColumnType.FOOTER);
                }

                if (subtable.getFooter() != null) {
                    tableFacet(context, sheet, subtable, subTableColumnsCount, "footer");
                }

                subtable.setRowIndex(-1);
                subtable = table.getSubTable();
            }

            tableColumnGroup(sheet, table, "footer");

            if (table.hasFooterColumn()) {
                tableFacet(context, sheet, table, subTableColumnsCount, "footer");
            }
        }
        else {
            if (lazy) {
                for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
                    if (rowIndex % rows == 0) {
                        table.setFirst(rowIndex);
                        table.loadLazyData();
                    }

                    exportRow(table, sheet, rowIndex);
                }

                // restore
                table.setFirst(first);
                table.loadLazyData();
            }
            else {
                for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
                    exportRow(table, sheet, rowIndex);
                }
                // restore
                table.setFirst(first);
            }
        }
    }

    protected void exportAll(FacesContext context, SubTable table, Sheet sheet) {
        int first = table.getFirst();
        int rowCount = table.getRowCount();
        tableColumnGroup(sheet, table, "header");
        if (Exporter.hasHeaderColumn(table)) {
            addColumnFacets(table, sheet, ColumnType.HEADER);
        }
        for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
            exportRow(table, sheet, rowIndex);
        }
        if (Exporter.hasFooterColumn(table)) {
            addColumnFacets(table, sheet, ColumnType.FOOTER);
        }
        tableColumnGroup(sheet, table, "footer");
        // restore
        table.setFirst(first);
    }

    protected void exportAll(FacesContext context, DataList list, Sheet sheet) {
        int first = list.getFirst();
        int rowCount = list.getRowCount();

        for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
            exportRow(list, sheet, rowIndex);
        }
        // restore
        list.setFirst(first);
    }

    protected void exportPageOnly(FacesContext context, DataTable table, Sheet sheet) {
        int first = table.getFirst();
        int rowsToExport = first + table.getRows();

        for (int rowIndex = first; rowIndex < rowsToExport; rowIndex++) {
            exportRow(table, sheet, rowIndex);
        }
    }

    protected void exportPageOnly(FacesContext context, DataList list, Sheet sheet) {
        int first = list.getFirst();
        int rowsToExport = first + list.getRows();

        for (int rowIndex = first; rowIndex < rowsToExport; rowIndex++) {
            exportRow(list, sheet, rowIndex);
        }
    }

    protected void exportSelectionOnly(FacesContext context, DataTable table, Sheet sheet) {
        Object selection = table.getSelection();
        String var = table.getVar();

        if (selection != null) {
            Map<String, Object> requestMap = context.getExternalContext().getRequestMap();

            if (selection.getClass().isArray()) {
                int size = Array.getLength(selection);

                for (int i = 0; i < size; i++) {
                    requestMap.put(var, Array.get(selection, i));

                    exportCells(table, sheet, 0);
                }
            }
            else {
                requestMap.put(var, selection);
                exportCells(table, sheet, 0);
            }
        }
    }

    protected void tableFacet(FacesContext context, Sheet sheet, DataTable table, int columnCount, String facetType) {
        Map<String, UIComponent> map = table.getFacets();
        UIComponent component = map.get(facetType);
        if (component != null) {
            String headerValue = null;
            if (component instanceof HtmlCommandButton) {
                headerValue = exportValue(context, component);
            }
            else if (component instanceof HtmlCommandLink) {
                headerValue = exportValue(context, component);
            }
            else if (component instanceof UIPanel) {
                String header = "";
                for (UIComponent child : component.getChildren()) {
                    headerValue = exportValue(context, child);
                    header = header + headerValue;
                }
                headerValue = header;
            }
            else {
                headerValue = Exporter.exportFacetValue(context, component);
            }

            int sheetRowIndex = sheet.getLastRowNum() + 1;
            Row row = sheet.createRow(sheetRowIndex);
            Cell cell = row.createCell((short) 0);
            cell.setCellValue(headerValue);
            cell.setCellStyle(facetStyle);

            sheet.addMergedRegion(new CellRangeAddress(
                        sheetRowIndex, // first row (0-based)
                        sheetRowIndex, // last row (0-based)
                        0, // first column (0-based)
                        columnCount + 1 // last column (0-based)
            ));

        }
    }

    protected void tableFacet(FacesContext context, Sheet sheet, SubTable table, int columnCount, String facetType) {
        Map<String, UIComponent> map = table.getFacets();
        UIComponent component = map.get(facetType);
        if (component != null) {
            String headerValue = null;
            if (component instanceof HtmlCommandButton) {
                headerValue = exportValue(context, component);
            }
            else if (component instanceof HtmlCommandLink) {
                headerValue = exportValue(context, component);
            }
            else if (component instanceof UIPanel) {
                String header = "";
                for (UIComponent child : component.getChildren()) {
                    headerValue = exportValue(context, child);
                    header = header + headerValue;
                }
                headerValue = header;
            }
            else {
                headerValue = Exporter.exportFacetValue(context, component);
            }

            int sheetRowIndex = sheet.getLastRowNum() + 1;
            Row row = sheet.createRow(sheetRowIndex);
            Cell cell = row.createCell((short) 0);
            cell.setCellValue(headerValue);
            cell.setCellStyle(facetStyle);

            sheet.addMergedRegion(new CellRangeAddress(
                        sheetRowIndex, // first row (0-based)
                        sheetRowIndex, // last row (0-based)
                        0, // first column (0-based)
                        columnCount // last column (0-based)
            ));

        }
    }

    protected void tableFacet(FacesContext context, Sheet sheet, DataList list, String facetType) {
        Map<String, UIComponent> map = list.getFacets();
        UIComponent component = map.get(facetType);
        if (component != null) {
            String headerValue = null;
            if (component instanceof HtmlCommandButton) {
                headerValue = exportValue(context, component);
            }
            else if (component instanceof HtmlCommandLink) {
                headerValue = exportValue(context, component);
            }
            else {
                headerValue = Exporter.exportFacetValue(context, component);
            }

            int sheetRowIndex = sheet.getLastRowNum() + 1;
            Row row = sheet.createRow(sheetRowIndex);
            Cell cell = row.createCell((short) 0);
            cell.setCellValue(headerValue);
            cell.setCellStyle(facetStyle);

            sheet.addMergedRegion(new CellRangeAddress(
                        sheetRowIndex, // first row (0-based)
                        sheetRowIndex, // last row (0-based)
                        0, // first column (0-based)
                        1 // last column (0-based)
            ));

        }
    }

    protected void tableColumnGroup(Sheet sheet, DataTable table, String facetType) {
        ColumnGroup cg = table.getColumnGroup(facetType);
        List<UIComponent> headerComponentList = null;
        if (cg != null) {
            headerComponentList = cg.getChildren();
        }
        if (headerComponentList != null) {
            for (UIComponent component : headerComponentList) {
                if (component instanceof org.primefaces.component.row.Row) {
                    org.primefaces.component.row.Row row = (org.primefaces.component.row.Row) component;
                    int sheetRowIndex = sheet.getLastRowNum() + 1;
                    Row xlRow = sheet.createRow(sheetRowIndex);
                    int i = 0;
                    for (UIComponent rowComponent : row.getChildren()) {
                        UIColumn column = (UIColumn) rowComponent;
                        String value = null;
                        if (facetType.equalsIgnoreCase("header")) {
                            value = column.getHeaderText();
                        }
                        else {
                            value = column.getFooterText();
                        }
                        int rowSpan = column.getRowspan();
                        int colSpan = column.getColspan();

                        Cell cell = xlRow.getCell(i);

                        if (rowSpan > 1 || colSpan > 1) {
                            if (rowSpan > 1) {
                                cell = xlRow.createCell((short) i);
                                Boolean rowSpanFlag = false;
                                for (int j = 0; j < sheet.getNumMergedRegions(); j++) {
                                    CellRangeAddress merged = sheet.getMergedRegion(j);
                                    if (merged.isInRange(sheetRowIndex, i)) {
                                        rowSpanFlag = true;
                                    }

                                }
                                if (!rowSpanFlag) {
                                    cell.setCellValue(value);
                                    cell.setCellStyle(facetStyle);
                                    sheet.addMergedRegion(new CellRangeAddress(
                                                sheetRowIndex, // first row (0-based)
                                                sheetRowIndex + rowSpan - 1, // last row (0-based)
                                                i, // first column (0-based)
                                                i // last column (0-based)
                                    ));
                                }
                            }
                            if (colSpan > 1) {
                                cell = xlRow.createCell((short) i);

                                for (int j = 0; j < sheet.getNumMergedRegions(); j++) {
                                    CellRangeAddress merged = sheet.getMergedRegion(j);
                                    if (merged.isInRange(sheetRowIndex, i)) {
                                        cell = xlRow.createCell((short) ++i);
                                    }
                                }
                                cell.setCellValue(value);
                                cell.setCellStyle(facetStyle);
                                sheet.addMergedRegion(new CellRangeAddress(
                                            sheetRowIndex, // first row (0-based)
                                            sheetRowIndex, // last row (0-based)
                                            i, // first column (0-based)
                                            i + colSpan - 1 // last column (0-based)
                                ));
                                i = i + colSpan - 1;
                            }
                        }
                        else {
                            cell = xlRow.createCell((short) i);
                            for (int j = 0; j < sheet.getNumMergedRegions(); j++) {
                                CellRangeAddress merged = sheet.getMergedRegion(j);
                                if (merged.isInRange(sheetRowIndex, i)) {
                                    cell = xlRow.createCell((short) ++i);
                                }
                            }
                            cell.setCellValue(value);
                            cell.setCellStyle(facetStyle);
                        }

                        i++;
                    }
                }

            }

        }
    }

    protected void tableColumnGroup(Sheet sheet, SubTable table, String facetType) {
        ColumnGroup cg = table.getColumnGroup(facetType);
        List<UIComponent> headerComponentList = null;
        if (cg != null) {
            headerComponentList = cg.getChildren();
        }
        if (headerComponentList != null) {
            for (UIComponent component : headerComponentList) {
                if (component instanceof org.primefaces.component.row.Row) {
                    org.primefaces.component.row.Row row = (org.primefaces.component.row.Row) component;
                    int sheetRowIndex = sheet.getLastRowNum() + 1;
                    Row xlRow = sheet.createRow(sheetRowIndex);
                    int i = 0;
                    for (UIComponent rowComponent : row.getChildren()) {
                        UIColumn column = (UIColumn) rowComponent;
                        String value = null;
                        if (facetType.equalsIgnoreCase("header")) {
                            value = column.getHeaderText();
                        }
                        else {
                            value = column.getFooterText();
                        }
                        int rowSpan = column.getRowspan();
                        int colSpan = column.getColspan();

                        Cell cell = xlRow.getCell(i);

                        if (rowSpan > 1 || colSpan > 1) {

                            if (rowSpan > 1) {
                                cell = xlRow.createCell((short) i);
                                Boolean rowSpanFlag = false;
                                for (int j = 0; j < sheet.getNumMergedRegions(); j++) {
                                    CellRangeAddress merged = sheet.getMergedRegion(j);
                                    if (merged.isInRange(sheetRowIndex, i)) {
                                        rowSpanFlag = true;
                                    }

                                }
                                if (!rowSpanFlag) {
                                    cell.setCellStyle(cellStyle);
                                    cell.setCellValue(value);
                                    sheet.addMergedRegion(new CellRangeAddress(
                                                sheetRowIndex, // first row (0-based)
                                                sheetRowIndex + rowSpan - 1, // last row (0-based)
                                                i, // first column (0-based)
                                                i // last column (0-based)
                                    ));
                                }
                            }
                            if (colSpan > 1) {
                                cell = xlRow.createCell((short) i);
                                for (int j = 0; j < sheet.getNumMergedRegions(); j++) {
                                    CellRangeAddress merged = sheet.getMergedRegion(j);
                                    if (merged.isInRange(sheetRowIndex, i)) {
                                        cell = xlRow.createCell((short) ++i);
                                    }
                                }
                                cell.setCellStyle(cellStyle);
                                cell.setCellValue(value);
                                sheet.addMergedRegion(new CellRangeAddress(
                                            sheetRowIndex, // first row (0-based)
                                            sheetRowIndex, // last row (0-based)
                                            i, // first column (0-based)
                                            i + colSpan - 1 // last column (0-based)
                                ));
                                i = i + colSpan - 1;
                            }
                        }
                        else {
                            cell = xlRow.createCell((short) i);
                            for (int j = 0; j < sheet.getNumMergedRegions(); j++) {
                                CellRangeAddress merged = sheet.getMergedRegion(j);
                                if (merged.isInRange(sheetRowIndex, i)) {
                                    cell = xlRow.createCell((short) ++i);
                                }
                            }
                            cell.setCellValue(value);
                            cell.setCellStyle(facetStyle);

                        }
                        i++;
                    }
                }

            }
        }

    }

    protected void exportRow(DataTable table, Sheet sheet, int rowIndex) {
        table.setRowIndex(rowIndex);
        if (!table.isRowAvailable()) {
            return;
        }

        exportCells(table, sheet, rowIndex);
    }

    protected void exportRow(SubTable table, Sheet sheet, int rowIndex) {
        table.setRowIndex(rowIndex);

        if (!table.isRowAvailable()) {
            return;
        }

        exportCells(table, sheet);
    }

    protected void exportRow(DataList list, Sheet sheet, int rowIndex) {
        list.setRowIndex(rowIndex);

        if (!list.isRowAvailable()) {
            return;
        }

        exportCells(list, sheet);
    }

    protected void exportCells(DataTable table, Sheet sheet, int rowIndex) {
        int sheetRowIndex = sheet.getLastRowNum() + 1;
        Row row = sheet.createRow(sheetRowIndex);

        for (UIColumn col : table.getColumns()) {
            if (!col.isRendered()) {
                continue;
            }

            if (col instanceof DynamicColumn) {
                ((DynamicColumn) col).applyModel();
            }

            if (col.isExportable()) {
                // Adding RowIndex for custom Export
                UIComponent component = (UIComponent) col;
                if (component.getId().equalsIgnoreCase("subject")) {

                    Cell cell = row.createCell(0);
                    String value = rowIndex + "";
                    cell.setCellValue(new XSSFRichTextString(value));
                }
                addColumnValue(row, col.getChildren(), "content");
            }
        }
        FacesContext context = null;
        if (table.getRowIndex() == 0) {
            for (UIComponent component : table.getChildren()) {
                if (component instanceof RowExpansion) {
                    RowExpansion rowExpansion = (RowExpansion) component;
                    if (rowExpansion.getChildren() != null) {
                        if (rowExpansion.getChildren().get(0) instanceof DataTable) {
                            DataTable childTable = (DataTable) rowExpansion.getChildren().get(0);
                            childTable.setRowIndex(-1);
                        }
                        if (rowExpansion.getChildren().get(0) instanceof DataList) {
                            DataList childList = (DataList) rowExpansion.getChildren().get(0);
                            childList.setRowIndex(-1);
                        }
                    }

                }
            }
        }
        table.setRowIndex(table.getRowIndex() + 1);
        for (UIComponent component : table.getChildren()) {
            if (component instanceof RowExpansion) {
                RowExpansion rowExpansion = (RowExpansion) component;
                if (rowExpansion.getChildren() != null) {
                    if (rowExpansion.getChildren().get(0) instanceof DataList) {
                        DataList list = (DataList) rowExpansion.getChildren().get(0);
                        if (list.getHeader() != null) {
                            tableFacet(context, sheet, list, "header");
                        }
                        exportAll(context, list, sheet);
                    }
                    if (rowExpansion.getChildren().get(0) instanceof DataTable) {
                        DataTable childTable = (DataTable) rowExpansion.getChildren().get(0);
                        int columnsCount = Exporter.getColumnsCount(childTable);

                        if (childTable.getHeader() != null) {
                            tableFacet(context, sheet, childTable, columnsCount, "header");

                        }
                        tableColumnGroup(sheet, childTable, "header");

                        addColumnFacets(childTable, sheet, ColumnType.HEADER);

                        exportAll(context, childTable, sheet, false);

                        if (childTable.hasFooterColumn()) {
                            addColumnFacets(childTable, sheet, ColumnType.FOOTER);
                        }
                        tableColumnGroup(sheet, childTable, "footer");
                        childTable.setRowIndex(-1);
                    }

                }
            }
        }
    }

    protected void exportCells(SubTable table, Sheet sheet) {
        int sheetRowIndex = sheet.getLastRowNum() + 1;
        Row row = sheet.createRow(sheetRowIndex);

        for (UIColumn col : table.getColumns()) {
            if (!col.isRendered()) {
                continue;
            }

            if (col instanceof DynamicColumn) {
                ((DynamicColumn) col).applyModel();
            }

            if (col.isExportable()) {
                addColumnValue(row, col.getChildren(), "content");
            }
        }
    }

    protected void exportCells(DataList list, Sheet sheet) {
        int sheetRowIndex = sheet.getLastRowNum() + 1;
        Row row = sheet.createRow(sheetRowIndex);

        for (UIComponent component : list.getChildren()) {
            if (component instanceof Column) {
                UIColumn column = (UIColumn) component;
                for (UIComponent childComponent : column.getChildren()) {
                    int cellIndex = row.getLastCellNum() == -1 ? 0 : row.getLastCellNum();
                    Cell cell = row.createCell(cellIndex);
                    if (component.isRendered()) {
                        String value = component == null ? "" : exportValue(FacesContext.getCurrentInstance(), childComponent);
                        cell.setCellValue(new XSSFRichTextString(value));
                        cell.setCellStyle(cellStyle);
                    }
                }

            }
            else {
                int cellIndex = row.getLastCellNum() == -1 ? 0 : row.getLastCellNum();
                Cell cell = row.createCell(cellIndex);
                if (component.isRendered()) {
                    String value = component == null ? "" : exportValue(FacesContext.getCurrentInstance(), component);
                    cell.setCellValue(new XSSFRichTextString(value));
                    cell.setCellStyle(cellStyle);
                }
            }
        }

    }

    protected void addColumnFacets(DataTable table, Sheet sheet, ColumnType columnType) {

        int sheetRowIndex = sheet.getLastRowNum() + 1;
        Row rowHeader = sheet.createRow(sheetRowIndex);

        for (UIColumn col : table.getColumns()) {
            if (!col.isRendered()) {
                continue;
            }

            if (col instanceof DynamicColumn) {
                ((DynamicColumn) col).applyModel();
            }

            if (col.isExportable()) {
                // Adding RowIndex for custom Export
                UIComponent component = (UIComponent) col;
                if (component.getId().equalsIgnoreCase("subject")) {

                    Cell cell = rowHeader.createCell(0);
                    String value = "Index";
                    cell.setCellValue(new XSSFRichTextString(value));
                }
                // Adding RowIndex for custom Export
                addColumnValue(rowHeader, col.getFacet(columnType.facet()), "facet");
            }
        }

    }

    protected void addColumnFacets(SubTable table, Sheet sheet, ColumnType columnType) {

        int sheetRowIndex = sheet.getLastRowNum() + 1;
        Row rowHeader = sheet.createRow(sheetRowIndex);

        for (UIColumn col : table.getColumns()) {
            if (!col.isRendered()) {
                continue;
            }

            if (col instanceof DynamicColumn) {
                ((DynamicColumn) col).applyModel();
            }

            if (col.isExportable()) {
                addColumnValue(rowHeader, col.getFacet(columnType.facet()), "facet");
            }
        }
    }

    protected void addColumnValue(Row row, UIComponent component, String type) {
        int cellIndex = row.getLastCellNum() == -1 ? 0 : row.getLastCellNum();
        Cell cell = row.createCell(cellIndex);
        String value = component == null ? "" : exportValue(FacesContext.getCurrentInstance(), component);
        cell.setCellValue(new XSSFRichTextString(value));
        if (type.equalsIgnoreCase("facet")) {
            // addColumnAlignments(component,facetStyle);
            cell.setCellStyle(facetStyle);
        }
        else {
            CellStyle cellStyle = this.cellStyle;
            cellStyle = addColumnAlignments(component, cellStyle);
            cell.setCellStyle(cellStyle);
        }

    }

    protected void addColumnValue(Row row, List<UIComponent> components, String type) {
        int cellIndex = row.getLastCellNum() == -1 ? 0 : row.getLastCellNum();
        Cell cell = row.createCell(cellIndex);
        StringBuilder builder = new StringBuilder();
        FacesContext context = FacesContext.getCurrentInstance();

        for (UIComponent component : components) {
            if (component.isRendered()) {
                String value = exportValue(context, component);

                if (value != null) {
                    builder.append(value);
                }
            }
        }

        cell.setCellValue(new XSSFRichTextString(builder.toString()));

        if (type.equalsIgnoreCase("facet")) {
            // addColumnAlignments(components,facetStyle);
            cell.setCellStyle(facetStyle);
        }
        else {
            CellStyle cellStyle = this.cellStyle;
            for (UIComponent component : components) {
                cellStyle = addColumnAlignments(component, cellStyle);
                cell.setCellStyle(cellStyle);
            }
        }

    }

    protected static CellStyle addColumnAlignments(UIComponent component, CellStyle style) {
        if (component instanceof HtmlOutputText) {
            HtmlOutputText output = (HtmlOutputText) component;
            if (output.getStyle() != null && output.getStyle().contains("left")) {
                style.setAlignment(HorizontalAlignment.LEFT);
            }
            if (output.getStyle() != null && output.getStyle().contains("right")) {
                style.setAlignment(HorizontalAlignment.RIGHT);
            }
            if (output.getStyle() != null && output.getStyle().contains("center")) {
                style.setAlignment(HorizontalAlignment.CENTER);
            }
        }
        return style;
    }

    @Override
    public void customFormat(String facetBackground, String facetFontSize, String facetFontColor, String facetFontStyle, String fontName, String cellFontSize,
                String cellFontColor, String cellFontStyle, String datasetPadding, String orientation) {
        if (facetBackground != null) {
            this.facetBackground = Color.decode(facetBackground);
        }
        if (facetFontColor != null) {
            this.facetFontColor = Color.decode(facetFontColor);
        }
        if (fontName != null) {
            this.fontName = fontName;
        }
        if (cellFontColor != null) {
            this.cellFontColor = Color.decode(cellFontColor);
        }

        this.facetFontSize = new Short(facetFontSize);
        this.facetFontStyle = facetFontStyle;
        this.cellFontSize = new Short(cellFontSize);
        this.cellFontStyle = cellFontStyle;
        this.datasetPadding = datasetPadding;

    }

    protected void createCustomFonts() {

        Font facetFont = wb.createFont();
        Font cellFont = wb.createFont();

        if (cellFontColor != null) {
            XSSFColor cellColor = new XSSFColor(cellFontColor);
            ((XSSFFont) cellFont).setColor(cellColor);
        }
        if (cellFontSize != null) {
            cellFont.setFontHeightInPoints(cellFontSize);
        }

        if (cellFontStyle.equalsIgnoreCase("BOLD")) {
            cellFont.setBold(true);
        }
        if (cellFontStyle.equalsIgnoreCase("ITALIC")) {
            cellFont.setItalic(true);
        }

        if (facetFontStyle.equalsIgnoreCase("BOLD")) {
            facetFont.setBold(true);
        }
        if (facetFontStyle.equalsIgnoreCase("ITALIC")) {
            facetFont.setItalic(true);
        }

        if (fontName != null) {
            cellFont.setFontName(fontName);
            facetFont.setFontName(fontName);
        }

        cellStyle.setFont(cellFont);

        if (facetBackground != null) {
            XSSFColor backgroundColor = new XSSFColor(facetBackground);
            ((XSSFCellStyle) facetStyle).setFillForegroundColor(backgroundColor);
            facetStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        }

        if (facetFontColor != null) {
            XSSFColor facetColor = new XSSFColor(facetFontColor);
            ((XSSFFont) facetFont).setColor(facetColor);

        }
        if (facetFontSize != null) {
            facetFont.setFontHeightInPoints(facetFontSize);
        }

        facetStyle.setFont(facetFont);
        facetStyle.setAlignment(HorizontalAlignment.CENTER);

    }

    protected static void writeExcelToResponse(ExternalContext externalContext, org.apache.poi.ss.usermodel.Workbook generatedExcel, String filename)
                throws IOException {

        externalContext.setResponseContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        externalContext.setResponseHeader("Expires", "0");
        externalContext.setResponseHeader("Cache-Control", "must-revalidate, post-check=0, pre-check=0");
        externalContext.setResponseHeader("Pragma", "public");
        externalContext.setResponseHeader("Content-disposition", "attachment;filename=" + filename + ".xlsx");
        externalContext.addResponseCookie(Constants.DOWNLOAD_COOKIE, "true", Collections.<String, Object> emptyMap());

        OutputStream out = externalContext.getResponseOutputStream();
        generatedExcel.write(out);
        externalContext.responseFlushBuffer();
    }
}