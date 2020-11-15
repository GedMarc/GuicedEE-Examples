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
package org.primefaces.extensions.showcase.controller;

import java.io.Serializable;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

import org.primefaces.extensions.event.ClipboardErrorEvent;
import org.primefaces.extensions.event.ClipboardSuccessEvent;

/**
 * Clipboard Controller.
 *
 * @author Melloware mellowaredev@gmail.com
 */
@Named
@ViewScoped
public class ClipboardController implements Serializable {

    private static final long serialVersionUID = 20120224L;

    private String copyInput = "Test Copy!";
    private String cutInput = "Cut Me!";
    private String lineBreaks = "PrimeFaces Clipboard\nRocks Ajax!";

    public void successListener(final ClipboardSuccessEvent successEvent) {
        final FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_INFO, "Success",
                    "Component id: " + successEvent.getComponent().getId() + " Action: " + successEvent.getAction()
                                + " Text: " + successEvent.getText());
        FacesContext.getCurrentInstance().addMessage(null, msg);
    }

    public void errorListener(final ClipboardErrorEvent errorEvent) {
        final FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error",
                    "Component id: " + errorEvent.getComponent().getId() + " Action: " + errorEvent.getAction());
        FacesContext.getCurrentInstance().addMessage(null, msg);
    }

    public String getCopyInput() {
        return copyInput;
    }

    public void setCopyInput(final String copyInput) {
        this.copyInput = copyInput;
    }

    public String getCutInput() {
        return cutInput;
    }

    public void setCutInput(final String cutInput) {
        this.cutInput = cutInput;
    }

    public String getLineBreaks() {
        return lineBreaks;
    }

    public void setLineBreaks(final String lineBreaks) {
        this.lineBreaks = lineBreaks;
    }
}
