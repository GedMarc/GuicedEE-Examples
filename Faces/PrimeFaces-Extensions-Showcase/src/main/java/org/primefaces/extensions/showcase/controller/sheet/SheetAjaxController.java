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
package org.primefaces.extensions.showcase.controller.sheet;

import java.util.List;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.validator.ValidatorException;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

import org.apache.commons.collections4.IterableUtils;
import org.primefaces.extensions.component.sheet.Sheet;
import org.primefaces.extensions.event.SheetEvent;
import org.primefaces.extensions.model.sheet.SheetUpdate;

/**
 * {@link Sheet} Ajax Controller.
 *
 * @author Melloware mellowaredev@gmail.com
 */
@Named
@ViewScoped
public class SheetAjaxController extends SheetController {

    private static final long serialVersionUID = 20120224L;

    /**
     * Ajax callback from the Sheet component when a cell value is changed.
     */
    @Override
    public void cellChangeEvent(final SheetEvent event) {
        final Sheet sheet = event.getSheet();
        final List<SheetUpdate> updates = sheet.getUpdates();

        // only show 1 update
        SheetUpdate sheetUpdate = IterableUtils.first(updates);
        final Long id = (Long) sheetUpdate.getRowKey();
        final Object oldValue = sheetUpdate.getOldValue();
        final Object newValue = sheetUpdate.getNewValue();

        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage("Update Success",
                    String.format("Asset %s updated. Old Value = %s, New Value = %s", id, oldValue, newValue)));

        sheet.commitUpdates();
    }

    /**
     * Ajax callback from the Sheet component when a column is selected.
     */
    public static void columnSelectEvent(final SheetEvent event) {
        final Sheet sheet = event.getSheet();
        final int column = sheet.getSelectedColumn() + 1;
        FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage("Column Selected", String.format("Column %d selected.", column)));
    }

    /**
     * Ajax callback from the Sheet component when a row is selected.
     */
    public static void rowSelectEvent(final SheetEvent event) {
        final Sheet sheet = event.getSheet();
        final int row = sheet.getSelectedRow() + 1;
        FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage("Row Selected", String.format("Row %d selected.", row)));
    }

    public static void validateExactly5(final FacesContext context, final UIComponent comp, final Object value) {
        if (context == null || comp == null) {
            return;
        }
        final Integer integer = (Integer) value;
        if (integer.intValue() != 5) {
            final FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error",
                        "Value must only be 5 exactly!");
            throw new ValidatorException(message);
        }
    }

}
