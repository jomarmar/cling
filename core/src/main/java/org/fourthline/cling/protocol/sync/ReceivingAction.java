/*
 * Copyright (C) 2011 4th Line GmbH, Switzerland
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.fourthline.cling.protocol.sync;

import org.fourthline.cling.UpnpService;
import org.fourthline.cling.model.action.ActionException;
import org.fourthline.cling.model.action.ActionInvocation;
import org.fourthline.cling.model.message.StreamRequestMessage;
import org.fourthline.cling.model.message.StreamResponseMessage;
import org.fourthline.cling.model.message.UpnpResponse;
import org.fourthline.cling.model.message.control.IncomingActionRequestMessage;
import org.fourthline.cling.model.message.control.OutgoingActionResponseMessage;
import org.fourthline.cling.model.message.header.ContentTypeHeader;
import org.fourthline.cling.model.message.header.UpnpHeader;
import org.fourthline.cling.model.resource.ServiceControlResource;
import org.fourthline.cling.model.types.ErrorCode;
import org.fourthline.cling.protocol.ReceivingSync;
import org.fourthline.cling.model.UnsupportedDataException;
import org.seamless.util.Exceptions;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles reception of control messages, invoking actions on local services.
 * <p>
 * Actions are invoked through the {@link org.fourthline.cling.model.action.ActionExecutor} returned
 * by the registered {@link org.fourthline.cling.model.meta.LocalService#getExecutor(org.fourthline.cling.model.meta.Action)}
 * method.
 * </p>
 *
 * @author Christian Bauer
 */
public class ReceivingAction extends ReceivingSync<StreamRequestMessage, StreamResponseMessage> {

    final private static Logger log = Logger.getLogger(ReceivingAction.class.getName());

    public ReceivingAction(UpnpService upnpService, StreamRequestMessage inputMessage) {
        super(upnpService, inputMessage);
    }

    protected StreamResponseMessage executeSync() {

        ContentTypeHeader contentTypeHeader =
                getInputMessage().getHeaders().getFirstHeader(UpnpHeader.Type.CONTENT_TYPE, ContentTypeHeader.class);

        // Special rules for action messages! UDA 1.0 says:
        // 'If the CONTENT-TYPE header specifies an unsupported value (other then "text/xml") the
        // device must return an HTTP status code "415 Unsupported Media Type".'
        if (contentTypeHeader != null && !contentTypeHeader.isUDACompliantXML()) {
            log.warning("Received invalid Content-Type '" + contentTypeHeader + "': " + getInputMessage());
            return new StreamResponseMessage(new UpnpResponse(UpnpResponse.Status.UNSUPPORTED_MEDIA_TYPE));
        }

        if (contentTypeHeader == null) {
            log.warning("Received without Content-Type: " + getInputMessage());
        }

        ServiceControlResource resource =
                getUpnpService().getRegistry().getResource(
                        ServiceControlResource.class,
                        getInputMessage().getUri()
                );

        if (resource == null) {
            log.fine("No local resource found: " + getInputMessage());
            return null;
        }

        log.fine("Found local action resource matching relative request URI: " + getInputMessage().getUri());

        ActionInvocation invocation;
        OutgoingActionResponseMessage responseMessage = null;

        try {

            // Throws ActionException if the action can't be found
            IncomingActionRequestMessage requestMessage =
                    new IncomingActionRequestMessage(getInputMessage(), resource.getModel());

            log.finer("Created incoming action request message: " + requestMessage);
            invocation = new ActionInvocation(requestMessage.getAction(), getClientInfo());

            // Throws UnsupportedDataException if the body can't be read
            log.fine("Reading body of request message");
            getUpnpService().getConfiguration().getSoapActionProcessor().readBody(requestMessage, invocation);

            log.fine("Executing on local service: " + invocation);
            resource.getModel().getExecutor(invocation.getAction()).execute(invocation);

            if (invocation.getFailure() == null) {
                responseMessage =
                        new OutgoingActionResponseMessage(invocation.getAction());
            } else {
                responseMessage =
                        new OutgoingActionResponseMessage(UpnpResponse.Status.INTERNAL_SERVER_ERROR, invocation.getAction());

            }

        } catch (ActionException ex) {
            log.finer("Error executing local action: " + ex);

            invocation = new ActionInvocation(ex);
            responseMessage = new OutgoingActionResponseMessage(UpnpResponse.Status.INTERNAL_SERVER_ERROR);

        } catch (UnsupportedDataException ex) {
        	log.log(Level.WARNING, "Error reading action request XML body: " + ex.toString(), Exceptions.unwrap(ex));

            invocation =
                    new ActionInvocation(
                            Exceptions.unwrap(ex) instanceof ActionException
                                    ? (ActionException)Exceptions.unwrap(ex)
                                    : new ActionException(ErrorCode.ACTION_FAILED, ex.getMessage())
                    );
            responseMessage = new OutgoingActionResponseMessage(UpnpResponse.Status.INTERNAL_SERVER_ERROR);

        }

        try {

            log.fine("Writing body of response message");
            getUpnpService().getConfiguration().getSoapActionProcessor().writeBody(responseMessage, invocation);

            log.fine("Returning finished response message: " + responseMessage);
            return responseMessage;

        } catch (UnsupportedDataException ex) {
            log.warning("Failure writing body of response message, sending '500 Internal Server Error' without body");
            log.log(Level.WARNING, "Exception root cause: ", Exceptions.unwrap(ex));
            return new StreamResponseMessage(UpnpResponse.Status.INTERNAL_SERVER_ERROR);
        }
    }

}
