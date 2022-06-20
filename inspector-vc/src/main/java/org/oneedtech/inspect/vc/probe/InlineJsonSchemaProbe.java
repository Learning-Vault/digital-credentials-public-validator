package org.oneedtech.inspect.vc.probe;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.oneedtech.inspect.core.probe.Probe;
import org.oneedtech.inspect.core.probe.RunContext;
import org.oneedtech.inspect.core.probe.json.JsonSchemaProbe;
import org.oneedtech.inspect.core.report.ReportItems;
import org.oneedtech.inspect.schema.SchemaKey;
import org.oneedtech.inspect.vc.Credential;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

/**
 * Detect inline schemas in a credential and run them. 
 * @author mgylling
 */
public class InlineJsonSchemaProbe extends Probe<Credential> {
	private static final Set<String> types = Set.of("1EdTechJsonSchemaValidator2019");
	private final boolean skipCanonical = true;
		
	public InlineJsonSchemaProbe() {
		super(ID);
	}
	
	@Override
	public ReportItems run(Credential crd, RunContext ctx) throws Exception {
		List<ReportItems> accumulator = new ArrayList<>();
		Set<String> ioErrors = new HashSet<>();		

//		JsonPathEvaluator jsonPath = ctx.get(RunContext.Key.JSONPATH_EVALUATOR);		
//		ArrayNode nodes = jsonPath.eval("$..*[?(@.credentialSchema)]", crd.getJson());
// 		note - we dont get deep nested ones in e.g. EndorsementCredential 
		
		JsonNode credentialSchemaNode = crd.asJson().get("credentialSchema");
		if(credentialSchemaNode == null) return success(ctx);
		
		ArrayNode schemas = (ArrayNode)	credentialSchemaNode; //TODO guard this cast
		
		for(JsonNode schemaNode : schemas) {
			JsonNode typeNode = schemaNode.get("type");
			if(typeNode == null || !types.contains(typeNode.asText())) continue;			
			JsonNode idNode = schemaNode.get("id");								
			if(idNode == null) continue;
			String id = idNode.asText().strip();				
			if(ioErrors.contains(id)) continue;				
			if(skipCanonical && equals(crd.getSchemaKey(), id)) continue;				
			try {								
				accumulator.add(new JsonSchemaProbe(id).run(crd.asJson(), ctx));
			} catch (Exception e) {	
				if(!ioErrors.contains(id)) {
					ioErrors.add(id);
					accumulator.add(error("Could not read schema resource " + id, ctx));
				}						
			}						
		}
				
		return new ReportItems(accumulator);
	}
	
	private boolean equals(Optional<SchemaKey> key, String id) {
		return key.isPresent() && key.get().getCanonicalURI().equals(id);					
	}
	
	public static final String ID = InlineJsonSchemaProbe.class.getSimpleName();
}
