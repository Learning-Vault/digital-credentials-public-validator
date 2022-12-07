package org.oneedtech.inspect.vc.probe;

import static org.oneedtech.inspect.util.code.Defensives.checkNotNull;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

import org.oneedtech.inspect.core.probe.Probe;
import org.oneedtech.inspect.core.probe.RunContext;
import org.oneedtech.inspect.core.probe.RunContext.Key;
import org.oneedtech.inspect.core.report.ReportItems;
import org.oneedtech.inspect.util.resource.UriResource;
import org.oneedtech.inspect.vc.jsonld.JsonLdGeneratedObject;
import org.oneedtech.inspect.vc.jsonld.probe.JsonLDCompactionProve;
import org.oneedtech.inspect.vc.util.CachingDocumentLoader;
import org.oneedtech.inspect.vc.util.JsonNodeUtil;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import foundation.identity.jsonld.ConfigurableDocumentLoader;

/**
 * Verification probe for Open Badges 2.0
 * Maps to "ASSERTION_VERIFICATION_DEPENDENCIES" task in python implementation
 * @author xaracil
 */
public class VerificationDependenciesProbe extends Probe<JsonLdGeneratedObject> {
    private final String assertionId;

    public VerificationDependenciesProbe(String assertionId) {
        super(ID);
        this.assertionId = assertionId;
    }

    @Override
    public ReportItems run(JsonLdGeneratedObject jsonLdGeneratedObject, RunContext ctx) throws Exception {
        ObjectMapper mapper = (ObjectMapper) ctx.get(Key.JACKSON_OBJECTMAPPER);
        JsonNode jsonNode = (mapper).readTree(jsonLdGeneratedObject.getJson());

        // TODO: get verification object from graph
        String type = jsonNode.get("verification").get("type").asText().strip();
        if ("HostedBadge".equals(type)) {
            // get badge
            UriResource badgeUriResource = resolveUriResource(ctx, jsonNode.get("badge").asText().strip());
            JsonLdGeneratedObject badgeObject = (JsonLdGeneratedObject) ctx.getGeneratedObject(
                JsonLDCompactionProve.getId(badgeUriResource));

            // get issuer from badge
            JsonNode badgeNode = ((ObjectMapper) ctx.get(Key.JACKSON_OBJECTMAPPER))
                .readTree(badgeObject.getJson());

            UriResource issuerUriResource = resolveUriResource(ctx, badgeNode.get("issuer").asText().strip());

            JsonLdGeneratedObject issuerObject = (JsonLdGeneratedObject) ctx.getGeneratedObject(
                JsonLDCompactionProve.getId(issuerUriResource));
            JsonNode issuerNode = ((ObjectMapper) ctx.get(Key.JACKSON_OBJECTMAPPER))
                .readTree(issuerObject.getJson());

            // verify issuer
            JsonNode verificationPolicy = issuerNode.get("verification");
            try {
                checkNotNull(verificationPolicy);
                if (verificationPolicy.isTextual()) {
                    // get verification node
                    JsonLdGeneratedObject verificationPolicyObject = (JsonLdGeneratedObject) ctx.getGeneratedObject(
                        JsonLDCompactionProve.getId(verificationPolicy.asText().strip()));
                        verificationPolicy = ((ObjectMapper) ctx.get(Key.JACKSON_OBJECTMAPPER))
                            .readTree(verificationPolicyObject.getJson());
                }
            } catch (Throwable t) {
                verificationPolicy = getDefaultVerificationPolicy(issuerNode, mapper);
            }

            // starts with check
            if (verificationPolicy.has("startsWith")) {
                List<String> startsWith = JsonNodeUtil.asStringList(verificationPolicy.get("startsWith"));
                if (!startsWith.stream().anyMatch(assertionId::startsWith)) {
                    return error("Assertion id " + assertionId
                        + "does not start with any permitted values in its issuer's verification policy.", ctx);
                }
            }

            // allowed origins
            JsonNode allowedOriginsNode = verificationPolicy.get("allowedOrigins");
            List<String> allowedOrigins = null;
            String issuerId = issuerNode.get("id").asText().strip();
            if (allowedOriginsNode == null || allowedOriginsNode.isNull()) {
                String defaultAllowedOrigins = getDefaultAllowedOrigins(issuerId);
                if (defaultAllowedOrigins != null) {
                    allowedOrigins = List.of(defaultAllowedOrigins);
                }
            } else {
                JsonNodeUtil.asStringList(allowedOriginsNode);
            }

            if (allowedOrigins == null || allowedOrigins.isEmpty() || !issuerId.startsWith("http")) {
                return warning("Issuer " + issuerId + " has no HTTP domain to enforce hosted verification policy against.", ctx);
            }

            if (!allowedOrigins.contains(new URI(assertionId).getAuthority())) {
                return error("Assertion " + assertionId + " not hosted in allowed origins " + allowedOrigins, ctx);
            }
        }
        return success(ctx);
    }

    private JsonNode getDefaultVerificationPolicy(JsonNode issuerNode, ObjectMapper mapper) throws URISyntaxException {
        String issuerId =issuerNode.get("id").asText().strip();

        return mapper.createObjectNode()
            .put("type", "VerificationObject")
            .put("allowedOrigins", getDefaultAllowedOrigins(issuerId))
            .put("verificationProperty", "id");
    }

    private String getDefaultAllowedOrigins(String issuerId) throws URISyntaxException {
        URI issuerUri = new URI(issuerId);
        return issuerUri.getAuthority();
    }

    protected UriResource resolveUriResource(RunContext ctx, String url) throws URISyntaxException {
        URI uri = new URI(url);
        UriResource initialUriResource = new UriResource(uri);
        UriResource uriResource = initialUriResource;

        // check if uri points to a local resource
        if (ctx.get(Key.JSON_DOCUMENT_LOADER) instanceof ConfigurableDocumentLoader) {
            if (ConfigurableDocumentLoader.getDefaultHttpLoader() instanceof CachingDocumentLoader.HttpLoader) {
                URI resolvedUri = ((CachingDocumentLoader.HttpLoader) ConfigurableDocumentLoader.getDefaultHttpLoader()).resolve(uri);
                uriResource = new UriResource(resolvedUri);
            }
        }
        return uriResource;
    }

    public static final String ID = VerificationDependenciesProbe.class.getSimpleName();

}
