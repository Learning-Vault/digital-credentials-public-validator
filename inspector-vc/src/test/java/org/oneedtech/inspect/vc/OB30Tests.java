package org.oneedtech.inspect.vc;

import static org.junit.jupiter.api.Assertions.*;
import static org.oneedtech.inspect.test.Assertions.*;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.oneedtech.inspect.core.Inspector.Behavior;
import org.oneedtech.inspect.core.report.Report;
import org.oneedtech.inspect.test.PrintHelper;
import org.oneedtech.inspect.vc.probe.ExpirationVerifierProbe;
import org.oneedtech.inspect.vc.probe.InlineJsonSchemaProbe;
import org.oneedtech.inspect.vc.probe.IssuanceVerifierProbe;
import org.oneedtech.inspect.vc.probe.ProofVerifierProbe;
import org.oneedtech.inspect.vc.probe.TypePropertyProbe;

import com.google.common.collect.Iterables;

public class OB30Tests {
	private static OB30Inspector validator; 
	private static boolean verbose = false;
	
	@BeforeAll 
	static void setup() {		
		validator = new OB30Inspector.Builder()				
				.set(Behavior.TEST_INCLUDE_SUCCESS, true)				
				.build();		
	}
	
	@Test
	void testSimpleJsonValid() {
		assertDoesNotThrow(()->{
			Report report = validator.run(Samples.OB30.JSON.SIMPLE_JSON.asFileResource());
			if(verbose) PrintHelper.print(report, true);
			assertValid(report);			
		});	
	}
	
	@Test
	void testSimplePNGPlainValid() {
		assertDoesNotThrow(()->{
			Report report = validator.run(Samples.OB30.PNG.SIMPLE_JSON_PNG.asFileResource());
			if(verbose) PrintHelper.print(report, true);
			assertValid(report);			
		});	
	}
	
	@Test
	void testSimplePNGJWTValid() {
		assertDoesNotThrow(()->{
			Report report = validator.run(Samples.OB30.PNG.SIMPLE_JWT_PNG.asFileResource());
			if(verbose) PrintHelper.print(report, true);
			assertValid(report);			
		});	
	}

	@Test
	void testSimpleJsonSVGPlainValid() {
		assertDoesNotThrow(()->{
			Report report = validator.run(Samples.OB30.SVG.SIMPLE_JSON_SVG.asFileResource());
			if(verbose) PrintHelper.print(report, true);
			assertValid(report);			
		});	
	}
		
	@Test
	void testSimpleJsonSVGJWTValid() {
		assertDoesNotThrow(()->{
			Report report = validator.run(Samples.OB30.SVG.SIMPLE_JWT_SVG.asFileResource());
			if(verbose) PrintHelper.print(report, true);
			assertValid(report);			
		});	
	}

	@Test
	void testSimpleJsonInvalidUnknownType() {
		//add a dumb value to .type and remove the ob type
		assertDoesNotThrow(()->{
			Report report = validator.run(Samples.OB30.JSON.SIMPLE_JSON_UNKNOWN_TYPE.asFileResource());
			if(verbose) PrintHelper.print(report, true);
			assertInvalid(report);
			assertFatalCount(report, 1);
			assertHasProbeID(report, TypePropertyProbe.ID, true);
		});	
	}
	
	@Test
	void testSimpleJsonInvalidProof() {
		//add some garbage chars to proofValue
		assertDoesNotThrow(()->{
			Report report = validator.run(Samples.OB30.JSON.SIMPLE_JSON_PROOF_ERROR.asFileResource());
			if(verbose) PrintHelper.print(report, true);
			assertInvalid(report);
			assertErrorCount(report, 1);
			assertHasProbeID(report, ProofVerifierProbe.ID, true);
		});	
	}
	
	@Test
	void testSimpleJsonExpired() {
		//"expirationDate": "2020-01-20T00:00:00Z",
		assertDoesNotThrow(()->{
			Report report = validator.run(Samples.OB30.JSON.SIMPLE_JSON_EXPIRED.asFileResource());
			if(verbose) PrintHelper.print(report, true);
			assertInvalid(report);
			assertHasProbeID(report, ExpirationVerifierProbe.ID, true);
		});	
	}
	
	@Test
	void testSimpleJsonNotIssued() {
		//"issuanceDate": "2040-01-01T00:00:00Z",
		//this breaks the proof too
		assertDoesNotThrow(()->{
			Report report = validator.run(Samples.OB30.JSON.SIMPLE_JSON_ISSUED.asFileResource());
			if(verbose) PrintHelper.print(report, true);
			assertInvalid(report);
			assertHasProbeID(report, IssuanceVerifierProbe.ID, true);
		});	
	}
	
	@Test
	void testCompleteJsonInvalidInlineSchemaRef() throws Exception {
		//404 inline schema ref, and 404 refresh uri
		assertDoesNotThrow(()->{
			Report report = validator.run(Samples.OB30.JSON.COMPLETE_JSON.asFileResource());
			if(verbose) PrintHelper.print(report, true);
			assertFalse(report.asBoolean());
			assertTrue(Iterables.size(report.getErrors()) > 0);
			assertTrue(Iterables.size(report.getExceptions()) > 0);			
			assertHasProbeID(report, InlineJsonSchemaProbe.ID, true);									
		});	
	}

}
