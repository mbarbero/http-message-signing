/*******************************************************************************
 * Copyright (c) 2017 Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Mikael Barbero - initial implementation
 *******************************************************************************/
package tech.barbero.httpsignatures.ahc4;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;

import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.BasicHttpProcessor;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.RequestContent;
import org.junit.jupiter.api.Test;

import tech.barbero.httpsignatures.HashKeyMap;
import tech.barbero.httpsignatures.HttpMessageSignatureVerificationException;
import tech.barbero.httpsignatures.HttpMessageSignatureVerifier;
import tech.barbero.httpsignatures.HttpMessageSigner;
import tech.barbero.httpsignatures.HttpMessageSigner.Algorithm;

public class TestHttpRequestSignerInterceptor {

	static class RequestFixedDate implements HttpRequestInterceptor {
		@Override
		public void process(HttpRequest request, HttpContext context) throws HttpException, IOException {
			request.setHeader(HTTP.DATE_HEADER, LocalDateTime.of(2016, 3, 20, 13, 20, 0).toInstant(ZoneOffset.ofHours(1)).toString());
		}
	}

	static class RequestDigest implements HttpRequestInterceptor {
		private final MessageDigest md;

		RequestDigest(MessageDigest md) {
			this.md = md;
		}

		@Override
		public void process(HttpRequest request, HttpContext context) throws IOException {
			if (request instanceof HttpEntityEnclosingRequest) {
				try(InputStream is = ((HttpEntityEnclosingRequest) request).getEntity().getContent()) {
					byte[] buffer = new byte[1024];
					int length;
					while ((length = is.read(buffer)) != -1) {
						md.update(buffer, 0, length);
					}
					byte[] digest = md.digest();
					request.addHeader("Digest", md.getAlgorithm() + "=" + Base64.getEncoder().encodeToString(digest));
				}
			}
		}
	}
	
	static BasicHttpProcessor createHttpProcessor(HttpMessageSigner httpSignature) throws NoSuchAlgorithmException {
		BasicHttpProcessor httpProcessor = new BasicHttpProcessor();
		httpProcessor.addInterceptor(new RequestContent());
		httpProcessor.addInterceptor(new RequestFixedDate());
		httpProcessor.addInterceptor(new RequestDigest(MessageDigest.getInstance("SHA-256")));
		httpProcessor.addInterceptor(new HttpRequestSignerInterceptor(httpSignature));
		return httpProcessor;
	}

	@Test
	public void testInterceptor() throws GeneralSecurityException, HttpException, IOException, HttpMessageSignatureVerificationException {
		HttpMessageSigner httpSignature = HttpMessageSigner.builder()
				.algorithm(Algorithm.RSA_SHA256)
				.keyMap(HashKeyMap.INSTANCE)
				.addHeaderToSign(HttpMessageSigner.REQUEST_TARGET)
				.addHeaderToSign("Date")
				.addHeaderToSign("Content-Length")
				.addHeaderToSign("Digest")
				.keyId("myKeyId").build();
		HttpEntityEnclosingRequest request = new BasicHttpEntityEnclosingRequest("GET", "http://www.example.com/web/service?foo=bar");
		request.setEntity(new StringEntity("Hello World!"));
		createHttpProcessor(httpSignature).process(request, new BasicHttpContext());
		
		HttpMessageSignatureVerifier signatureVerifier = HttpMessageSignatureVerifier.builder().keyMap(HashKeyMap.INSTANCE).build();
		assertTrue(signatureVerifier.verify(AHCRequest.from(request)));
		
	}
}
