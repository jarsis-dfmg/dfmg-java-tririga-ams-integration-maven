package com.tririga.custom;

import com.tririga.platform.smartobject.InvalidFieldRequestException;
import com.tririga.platform.smartobject.domain.SmartObject;
import com.tririga.platform.smartobject.domain.field.TextField;
import com.tririga.platform.smartobject.service.SmartObjectUtils;
import com.tririga.pub.workflow.CustomParamBusinessConnectTask;
import com.tririga.pub.workflow.CustomParamTaskResult;
import com.tririga.pub.workflow.CustomParamTaskResultImpl;
import com.tririga.pub.workflow.Record;
import com.tririga.pub.workflow.WFStepInfo;
import com.tririga.pub.workflow.WFVariable;

import com.tririga.ws.TririgaWS;
import com.tririga.ws.dto.IntegrationField;
import com.tririga.ws.dto.IntegrationRecord;
import com.tririga.ws.dto.IntegrationSection;
import com.tririga.ws.dto.ResponseHelper;
import com.tririga.ws.dto.ResponseHelperHeader;

import java.io.StringWriter;
import javax.net.ssl.HttpsURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jdk.nashorn.internal.parser.JSONParser;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import com.google.gson.*;

public class AMSIntegration
        implements CustomParamBusinessConnectTask {
    private static TririgaWS tririgaWs;

    private void initialiseTWS(TririgaWS ws, long userId) {
        this.tririgaWs = ws;
        try {
            this.tririgaWs.register(userId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static final Logger logserver = Logger.getLogger(AMSIntegration.class);


    public CustomParamTaskResult execute(TririgaWS ws, Map par, long profileId, Record[] records) {
        long antennaId = records[0].getRecordId();
        long calcHelperId = ((WFStepInfo) ((WFVariable) par.get("calcHelperId")).getValue()).getResultRecordId().longValue();
        long ioId = ((WFStepInfo) ((WFVariable) par.get("ioId")).getValue()).getResultRecordId().longValue();
        boolean success = true;
        boolean fail = false;
        CustomParamTaskResultImpl task = new CustomParamTaskResultImpl();

        Map<String, String> dtoMappingTable = Stream.of(new String[][]{
                {"name", "cstHerstellerTX"},
                {"manId", "cstHerstellerIdNU"},
                {"athStatus", "cstAntennaStatusTX"},
                {"antennaType", "cstAntennaTypTX"},
                {"atyId", "cstAntennaTypIdNU"},
                {"athId", "cstAntennaTypVersionIdNU"},
                {"ca", "cstCaNU"},
                {"centerHeight", "cstCenterHeightNU"},
                {"centerWidth", "cstCenterWidthNU"},
                {"commentText", "cstCommentTX"},
                {"connection", "cstConnectionTX"},
                {"diameterDishAntenna", "cstDiameterDishAntennaNU"},
                {"frontSurface", "cstFrontSalesNU"},
                {"frontSurfaceDishAntenna", "cstFrontSurfaceDishAntennaNU"},
                {"frontSurfaceSalesEq", "cstFrontSurfaceSalesEqBL"},
                {"frontSurfaceSales", "cstFrontSurfaceSalesNU"},
                {"frontWindLoad", "cstFrontWindLoadNU"},
                {"frontWindSpeed", "cstFrontWindSpeedNU"},
                {"height", "cstHeightNU"},
                {"validFrom", "cstHistFromTX"},
                {"validTill", "cstHistTillTX"},
                {"length", "cstlengthNU"},
                {"maxWindLoad", "cstMaxWindLoadNU"},
                {"maxWindSpeed", "cstMaxWindSpeedNU"},
                {"modDate", "cstModDateTX"},
                {"mounting", "cstMountingTX"},
                {"portsPos", "cstPortsPosTX"},
                {"salesRelatedName", "cstSalesRelatedNameTX"},
                {"version", "cstVersionNU"},
                {"weightInclMounting", "cstWeightInclMountingNU"},
                {"weight", "cstWeightNU"},
                {"width", "cstWidthNU"},
                {"windLoad", "cstWindloadNU"},
                {"windSpeed", "cstWindSpeedNU"},
                {"antennaName", "cstAntennaNameTX"}
        }).collect(Collectors.toMap(p -> p[0], p -> p[1]));

        initialiseTWS(ws, profileId);

        try { // try anything and catch TRIRIGA exception
            String antennaManufacturer = getFieldValue(calcHelperId, "triInput1TX");
            String antennaModel = getFieldValue(calcHelperId, "triInput2TX");
            String urlAms = getFieldValue(ioId, "triURLTX");
            String userAms = getFieldValue(ioId, "triPostUserNameParameterTX");
            String passwordAms = getFieldValue(ioId, "triPostPasswordParameterTX");

            if (!antennaManufacturer.isEmpty() && !antennaModel.isEmpty() && !urlAms.isEmpty() && !userAms.isEmpty() && !passwordAms.isEmpty()) {
                try {
                    // TODO analyze JSON in here
                    JSONParser parser = new JSONParser();
                    JsonElement je = parser.parse(getAMSData(urlAms, userAms, passwordAms, antennaModel, antennaManufacturer));

                    HashMap<String, String> amsHashMap = jsonToHashMap("",je);

                    IntegrationField[] fieldArr = new IntegrationField[36];

                    for (Map.Entry<String, String> entry : amsHashMap.entrySet()) {
                        fieldArr.add(new IntegrationField(dtoMappingTable.get(entry.getKey()), entry.getValue())); //TODO check if add
                    }

                    // TODO add manually
                    fieldArr.add(new IntegrationField("jrsAntennaSpecIdTX", Long.toString(antennaId)));

                    long reph = createDTO(fieldArr);
                }
            } catch(Exception e){
                logserver.error(e.getMessage());
                logserver.error(e);
                task.setExecutionWasSuccessful(fail);
                return (CustomParamTaskResult) task;
            }
            ;

        } else{
            throw new Exception("No values to send");
        }


        task.setExecutionWasSuccessful(success);
        return (CustomParamTaskResult) task;


    } catch(
    Exception e)

    {
        logserver.error(e.getMessage());
        logserver.error(e);
        task.setExecutionWasSuccessful(fail);
        return (CustomParamTaskResult) task;

    }

}

    /**
     * Posts against AMS System to get detailed antenna data back.
     *
     * @param URL
     * @param username
     * @param password
     * @param antennaModel
     * @param antennaManufacturer
     * @return
     */
    public static String getAMSData(String URL, String username, String password, String antennaModel, String antennaManufacturer) {
        try {
            String calcUrl = String.valueOf(URL) + "name=" + antennaModel + "&manufacturer=" + antennaManufacturer;
            int code = -1;
            HttpsURLConnection con = null;

            String basicAuthString = username + ":" + password;
            String authStringEnc = Base64.getEncoder().encodeToString(basicAuthString.getBytes());
            URL url = new URL(calcUrl);
            con = (HttpsURLConnection) url.openConnection();
            byte[] payload = "Test".toString().getBytes(StandardCharsets.UTF_8);
            int postDataLength = payload.length;

            con.setDoOutput(true);
            con.setInstanceFollowRedirects(false);
            con.setRequestMethod("POST");
            con.setRequestProperty("Authorization", "Basic " + authStringEnc);
            con.setRequestProperty("Host", "value");
            con.setRequestProperty("Content-Type", "application/json");
            con.setRequestProperty("charset", "utf-8");
            con.setRequestProperty("Content-Length", Integer.toString(postDataLength));
            con.setUseCaches(false);

            code = con.getResponseCode();

            if (code != 200) {
                // TODO throw Exception
            }

            String responseMessage = con.getResponseMessage();

            con.getInputStream();
            StringWriter writer = new StringWriter();

            IOUtils.copy(con.getInputStream(), writer, StandardCharsets.UTF_8.toString());
        } catch (Exception e) {
            // TODO connection not working
        } finally {
            con.disconnect(); // TODO check if that is a valid method
        }

        return writer.toString();
    }

    /**
     * Parses a json string into a hashmap
     *
     * @param key
     * @param je
     * @return
     */
    private static HashMap<String, String> jsonToHashMap(String key, JsonElement je) {
       HashMap<String, String> integrationMap = new HashMap<String, String>();
        if (je.isJsonNull()) {
            integrationMap.put(key, "null");
        }

        if (je.isJsonPrimitive()) {
            integrationMap.put(key, je.toString());
        }

        if (je.isJsonObject()) {
            for (Map.Entry<String, JsonElement> e : je.getAsJsonObject().entrySet()) {
                integrationMap.putAll(jsonToHashMap(e.getKey(), e.getValue()));
            }
        }

        return integrationMap;
    }

    /**
     * Creates a DTO Record through TRIRIGA Business Connect
     *
     * @param fields
     * @return
     * @throws Exception
     */
    private static long createDTO(IntegrationField[] fields) throws Exception {


        String module = "cstIntegationDTOs";
        String bo = "cstAMSAntennaDTO";

        int moduleId = tririgaWs.getModuleId("cstIntegationDTOs");
        long objectTypeId = tririgaWs.getObjectTypeId(module, bo);
        long guiId = tririgaWs.getDefaultGuiId(objectTypeId);

        IntegrationSection insect = new IntegrationSection();
        insect.setType("DEFAULT");
        insect.setName("General");

        insect.setFields(fields);

        IntegrationRecord amsAntennaDTO = new IntegrationRecord(moduleId, objectTypeId, bo, guiId, -1L, 1, "triCreateDraft");
        amsAntennaDTO.setSections(new IntegrationSection[]{
                insect
        });


        ResponseHelperHeader response = null;
        ResponseHelper h = null;
        try {
            response = tririgaWs.saveRecord(new IntegrationRecord[]{
                    amsAntennaDTO
            });

            ResponseHelper[] rh = response.getResponseHelpers();
            h = rh[0];

        } catch (Exception e1) {
            logserver.error(e1);
            e1.printStackTrace();
        }

        return h.getRecordId();
    }



}
