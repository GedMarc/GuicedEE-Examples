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
package org.primefaces.extensions.showcase.controller.speedtest;

import java.io.Serializable;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.RandomStringUtils;
import org.primefaces.extensions.component.speedtest.Speedtest;
import org.primefaces.extensions.event.SpeedTestEvent;

/**
 * {@link Speedtest} Controller.
 *
 * @author Stefan Sibitz ssibitz@me.com
 */
@Named
@ViewScoped
public class SpeedTestController implements Serializable {

    private static final long serialVersionUID = -5297177555931103300L;
    private static final Integer ONEMB = 1024 * 1024;
    // Size of Download-File (Default=5)
    private Integer sizeInMB = 5;

    public static void saveResults(final SpeedTestEvent speedTestEvent) {
        final String msg = "PingTimeMS       ='" + String.valueOf(speedTestEvent.getPingTimeMS()) + "', \n"
                    + "Jitter           ='" + String.valueOf(speedTestEvent.getJitterTimeMS()) + "', \n"
                    + "SpeedMbpsDownload='" + String.valueOf(speedTestEvent.getSpeedMbpsDownload()) + "', \n"
                    + "SpeedMbpsUpload='" + String.valueOf(speedTestEvent.getSpeedMbpsUpload()) + "'";
        final FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, "Saved your speed results:", msg);
        FacesContext.getCurrentInstance().addMessage(null, message);
    }

    private String generatePayloadFile() {
        // Generate random test string
        final Integer size2Generate = ONEMB * sizeInMB;
        return RandomStringUtils.randomAlphanumeric(size2Generate);
    }

    public void downloadPayloadFile() {
        final String payloadString = generatePayloadFile();
        try {
            final FacesContext facesContext = FacesContext.getCurrentInstance();
            final HttpServletResponse response = (HttpServletResponse) facesContext.getExternalContext().getResponse();
            response.reset();
            response.setHeader("Content-Type", "application/octet-stream");
            response.setHeader("Content-Disposition", "attachment;filename=dummy.payload");
            response.getWriter().write(payloadString);
            response.getWriter().flush();
            facesContext.responseComplete();
        }
        catch (final Exception e) {
            // ignore
        }
    }

    public void updateSizeInMB() {
        // Dummy for updating the size on the server...
    }

    public Integer getSizeInMB() {
        return sizeInMB;
    }

    public void setSizeInMB(final Integer sizeInMB) {
        this.sizeInMB = sizeInMB;
    }
}
