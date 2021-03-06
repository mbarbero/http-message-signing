/*******************************************************************************
 * Copyright (c) 2018 Eclipse Foundation and others
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tech.barbero.http.message.signing;

import java.net.URI;

/**
 * A request message from a client to a server.
 */
public interface HttpRequest extends HttpMessage {

	/**
	 * Returns the method of this request.
	 *
	 * @return the method of this request.
	 */
	String method();

	/**
	 * Returns the target URI of this request.
	 *
	 * @return the target URI of this request.
	 */
	URI uri();
}
