package com.digitalhie.QRDAReportGenerator;

import com.digitalhie.QRDAReportGenerator.model.DiagnosticStudy;
import com.digitalhie.QRDAReportGenerator.model.ObservationDetail;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.openhealthtools.mdht.uml.cda.*;
import org.openhealthtools.mdht.uml.cda.consol.ConsolPackage;
import org.openhealthtools.mdht.uml.cda.util.CDAUtil;
import org.openhealthtools.mdht.uml.hl7.datatypes.*;
import org.openhealthtools.mdht.uml.hl7.vocab.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/*
    This file used for local testing. Do not delete this file till we finish the project
 */
public class UnitTest {

    public static void main(String[] args) throws Exception {

        String templateFilePath =  "templates/2023MIPSGroupSampleQRDA-III-v1.1.xml"; // "templates/2023-CMS-QRDA-I-v1.2-Sample-File.xml";
        ObjectMapper mapper = new ObjectMapper();
        File from = new File("src/test/resources/templates/sample-input.json");
        JsonNode input = mapper.readTree(from);

        ConsolPackage.eINSTANCE.eClass();
        InputStream cpResource = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(templateFilePath);
        ClinicalDocument oClinicalDocument = CDAUtil.load(cpResource); //Loads CDADocument.

        Section measureSection = oClinicalDocument.getSections().stream()
                .filter(section -> {
                    for(II templateId: section.getTemplateIds()) {
                        if(templateId.getRoot() !=null && templateId.getRoot().equals("2.16.840.1.113883.10.20.24.2.2")) {
                            return true;
                        }
                    }
                    return false;
                }).findFirst().orElseThrow(() -> new Exception("Measure section not found at the template"));

        Map<String, String> codeSystemNames = new HashMap<>();
        codeSystemNames.put("LOINC","2.16.840.1.113883.6.1");

        ObservationDetail o = new ObservationDetail();
        o.setId("dummyId");
        o.setName("dummyName");
        measureSection.getEntries().add(createEntryWithObservation(o, codeSystemNames));

        // write to file
        String fileName = UUID.randomUUID()+"_ccd_file.xml";
        FileOutputStream fos = new FileOutputStream(fileName);
        CDAUtil.save(oClinicalDocument, fos);
        fos.close();
        if(cpResource!=null)
            cpResource.close();
    }

    private static Entry createEntryWithEncounter(ObservationDetail observationDetail, Map<String, String> codeSystemNames) {
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

    private static Entry createEntryWithObservation(ObservationDetail observationDetail, Map<String, String> codeSystemNames) {
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
        II templateId = DatatypesFactory.eINSTANCE.createII("rootId","extensionId");
        II id = DatatypesFactory.eINSTANCE.createII("sampleId","sampleExtensionId");
        o.getTemplateIds().add(templateId);
        o.getIds().add(id);
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
