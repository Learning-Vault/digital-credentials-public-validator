package org.oneedtech.inspect.vc;

import java.net.URI;
import java.util.Date;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;

import foundation.identity.jsonld.JsonLDUtils;

public class W3CVerifiableCredentialDM2 extends com.danubetech.verifiablecredentials.VerifiableCredential {
	public static final URI[] DEFAULT_JSONLD_CONTEXTS = { URI.create(VerifiableCredential.JSONLD_CONTEXT_W3C_CREDENTIALS_V2) };

	@JsonCreator
	public W3CVerifiableCredentialDM2() {
		super();
	}

	protected W3CVerifiableCredentialDM2(Map<String, Object> jsonObject) {
		super(jsonObject);
	}

	public static W3CVerifiableCredentialDM2 fromJson(String json) {
		return new W3CVerifiableCredentialDM2(readJson(json));
	}

	public Date getValidFrom() {
		return JsonLDUtils.stringToDate(JsonLDUtils.jsonLdGetString(this.getJsonObject(), JSONLD_TERM_VALIDFROM));
	}

	public Date getValidUntil() {
		return JsonLDUtils.stringToDate(JsonLDUtils.jsonLdGetString(this.getJsonObject(), JSONLD_TERM_VALIDUNTIL));
	}
    private static final String JSONLD_TERM_VALIDFROM = "validFrom";
    private static final String JSONLD_TERM_VALIDUNTIL = "validUntil";

}
