package com.digitalhie.QRDAReportGenerator.util;

import com.digitalhie.QRDAReportGenerator.model.DiagnosticStudy;
import com.digitalhie.QRDAReportGenerator.model.ObservationDetail;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import org.openhealthtools.mdht.uml.cda.*;
import org.openhealthtools.mdht.uml.cda.consol.ConsolPackage;
import org.openhealthtools.mdht.uml.cda.util.CDAUtil;
import org.openhealthtools.mdht.uml.hl7.datatypes.*;
import org.openhealthtools.mdht.uml.hl7.vocab.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.*;

@Service
public class CCDGenerator {

    Logger logger = LoggerFactory.getLogger(CCDGenerator.class);
    public String createCCD(String templateFilePath, JsonNode input) throws Exception {

        ConsolPackage.eINSTANCE.eClass();
        InputStream cpResource = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(templateFilePath);
        ClinicalDocument oClinicalDocument = CDAUtil.load(cpResource); //Loads CDADocument.

        // First tries to find patient data section else will look for Measure Section else throws error
        Section patientSection = oClinicalDocument.getSections().stream()
                .filter(section -> section.getTitle()!=null
                        && section.getTitle().getText()!=null
                        && section.getTitle().getText().equalsIgnoreCase("Patient Data")
                ).findFirst().orElse(
                        oClinicalDocument.getSections().stream()
                                .filter(section -> section.getTitle()!=null
                                        && section.getTitle().getText()!=null
                                        && section.getTitle().getText().equalsIgnoreCase("Measure Section")
                                ).findFirst()
                                .orElseThrow(() -> new Exception("Patient Data or Measure Section not found at the template"))
                );

        // This map is used to find the codeSystemID for their names
        Map<String, String> codeSystemNames = getCodeSystemNames(input);

        List<ObservationDetail> observations = getAllObservations(input);

        observations.forEach(observation -> {
            if(observation.getName().startsWith("Encounter")) {
                patientSection.getEntries().add(
                        createEntryWithEncounter(observation, codeSystemNames));
            } else {
                patientSection.getEntries().add(createEntryWithObservation(observation, codeSystemNames));
            }
        });

        getDiagnosticStudyPerformedFields(input).forEach(diagnosticStudy -> {
            patientSection.getEntries().add(createEntryWithDiagnosticStudyObservation(diagnosticStudy, codeSystemNames));
        });

        // write to file
        String fileName = UUID.randomUUID()+"_ccd_file.xml";
        FileOutputStream fos = new FileOutputStream(fileName);
        CDAUtil.save(oClinicalDocument, fos);
        fos.close();
        if(cpResource!=null)
            cpResource.close();

        return fileName;
    }

    private List<ObservationDetail> getAllObservations(JsonNode input) {

        List<ObservationDetail> observationDetails = new ArrayList<>();
        ArrayNode arrayNode = (ArrayNode) input.get("library").get("valueSets").get("def");
        arrayNode.forEach(e -> {
            String value = e.get("id").textValue();
            if(value.contains(":")) {
                String[] subSet = value.split(":");
                value = subSet[subSet.length-1];
            }
            ObservationDetail observationDetail = new ObservationDetail();
            observationDetail.setId(value);
            observationDetail.setName(e.get("name").asText());
            observationDetails.add(observationDetail);
        });
        return observationDetails;
    }

    private List<DiagnosticStudy> getDiagnosticStudyPerformedFields(JsonNode input) {
        List<DiagnosticStudy> diagnosticStudyRecords = new ArrayList<>();
        ArrayNode arrayNode = (ArrayNode) input.get("library").get("codes").get("def");
        arrayNode.forEach(e -> {
            DiagnosticStudy diagnosticStudy = new DiagnosticStudy();
            diagnosticStudy.setId(e.get("id").asText());
            diagnosticStudy.setName(e.get("name").textValue());
            diagnosticStudy.setCodeSystemName(e.get("codeSystem").get("name").asText());
            diagnosticStudyRecords.add(diagnosticStudy);
        });
        return diagnosticStudyRecords;
    }

    private Map<String, String> getCodeSystemNames(JsonNode input) {
        Map<String, String> fields = new HashMap<>();
        ArrayNode arrayNode = (ArrayNode) input.get("library").get("codeSystems").get("def");
        arrayNode.forEach(e -> {
            String value = e.get("id").textValue();
            if(value.contains(":")) {
                String[] subSet = value.split(":");
                value = subSet[subSet.length-1];
            }
            fields.put(e.get("name").asText(),value);
        });

        return fields;
    }

    private Entry createEntryWithEncounter(ObservationDetail observationDetail, Map<String, String> codeSystemNames) {
        Entry entry = CDAFactory.eINSTANCE.createEntry();
        entry.setTypeCode(x_ActRelationshipEntry.DRIV);
        CD code = DatatypesFactory.eINSTANCE.createCD();
        code.setCodeSystemName("LOINC");
        code.setCodeSystem(codeSystemNames.get("LOINC"));
        code.setDisplayName(observationDetail.getName());
        Encounter e = CDAFactory.eINSTANCE.createEncounter();
        e.setCode(code);
        e.setClassCode(ActClass.ENC);
        e.setMoodCode(x_DocumentEncounterMood.EVN);
        ED ed = DatatypesFactory.eINSTANCE.createED("Encounter Performed: "+observationDetail.getName());
        e.setStatusCode(DatatypesFactory.eINSTANCE.createCS("completed"));
        e.setText(ed);
        EntryRelationship er = CDAFactory.eINSTANCE.createEntryRelationship();
        Observation o = CDAFactory.eINSTANCE.createObservation();
        CD value =  DatatypesFactory.eINSTANCE.createCD();
        value.setCodeSystem(observationDetail.getId());
        o.getValues().add(value);
        er.setObservation(o);
        e.getEntryRelationships().add(er);
        entry.setEncounter(e);

        return entry;
    }

    private Entry createEntryWithObservation(ObservationDetail observationDetail, Map<String, String> codeSystemNames) {
        Entry entry = CDAFactory.eINSTANCE.createEntry();
        entry.setTypeCode(x_ActRelationshipEntry.DRIV);
        CD code = DatatypesFactory.eINSTANCE.createCD();
        code.setCodeSystemName("LOINC");
        code.setCodeSystem(codeSystemNames.get("LOINC"));
        code.setDisplayName(observationDetail.getName());
        Observation o = CDAFactory.eINSTANCE.createObservation();
        o.setCode(code);
        o.setClassCode(ActClassObservation.OBS);
        o.setMoodCode(x_ActMoodDocumentObservation.EVN);
        ED ed = DatatypesFactory.eINSTANCE.createED("Patient Characteristic "+observationDetail.getName());
        o.setStatusCode(DatatypesFactory.eINSTANCE.createCS("completed"));
        o.setText(ed);
        ST derivationExpr = DatatypesFactory.eINSTANCE.createST(
                observationDetail.getName()+"_PatientCharacteristic"+observationDetail.getName());
        o.setDerivationExpr(derivationExpr);

        CD value =  DatatypesFactory.eINSTANCE.createCD();
        value.setCodeSystem(observationDetail.getId());
        o.getValues().add(value);

        entry.setObservation(o);

        return entry;
    }

    private static Entry createEntryWithDiagnosticStudyObservation(
            DiagnosticStudy diagnosticStudy,
            Map<String, String> codeSystemNames) {
        Entry entry = CDAFactory.eINSTANCE.createEntry();
        entry.setTypeCode(x_ActRelationshipEntry.DRIV);
        CD code = DatatypesFactory.eINSTANCE.createCD();
        code.setCode(diagnosticStudy.getId());
        code.setCodeSystemName(diagnosticStudy.getCodeSystemName());
        code.setCodeSystem(codeSystemNames.get(diagnosticStudy.getCodeSystemName()));
        code.setDisplayName(diagnosticStudy.getName());
        Observation o = CDAFactory.eINSTANCE.createObservation();
        o.setCode(code);
        o.setClassCode(ActClassObservation.OBS);
        o.setMoodCode(x_ActMoodDocumentObservation.EVN);
        ED ed = DatatypesFactory.eINSTANCE.createED("Diagnostic Study, Performed: "+diagnosticStudy.getName());
        o.setStatusCode(DatatypesFactory.eINSTANCE.createCS("completed"));
        o.setText(ed);
        ST derivationExpr = DatatypesFactory.eINSTANCE.createST("DiagnosticStudyPerformed: "+diagnosticStudy.getName());
        o.setDerivationExpr(derivationExpr);
        entry.setObservation(o);

        return entry;
    }
}
