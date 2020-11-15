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

import org.primefaces.event.SelectEvent;

/**
 * Counter Controller.
 *
 * @author Melloware mellowaredev@gmail.com
 */
@Named
@ViewScoped
public class CounterController implements Serializable {

    private static final long serialVersionUID = 20120224L;

    public void startListener(final SelectEvent<Double> event) {
        final FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_INFO, "Start fired",
                    "Value: " + event.getObject());
        FacesContext.getCurrentInstance().addMessage(null, msg);
    }

    public void endListener(final SelectEvent<Double> event) {
        final FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_WARN, "End fired",
                    "Value: " + event.getObject());
        FacesContext.getCurrentInstance().addMessage(null, msg);
    }

}
