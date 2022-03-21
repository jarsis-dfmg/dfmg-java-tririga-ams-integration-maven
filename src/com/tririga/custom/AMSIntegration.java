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
import java.util.Map;
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

        initialiseTWS(ws, profileId);

        try { // try anything and catch TRIRIGA exception
            String antennaManufacturer = getFieldValue(calcHelperId, "triInput1TX");
            String antennaModel = getFieldValue(calcHelperId, "triInput2TX");
            if (!antennaManufacturer.isEmpty() && !antennaModel.isEmpty()) {
                try {
                    String urlAms = getFieldValue(ioId, "triURLTX");
                    String userAms = getFieldValue(ioId, "triPostUserNameParameterTX");
                    String passwordAms = getFieldValue(ioId, "triPostPasswordParameterTX");

                    if (!urlAms.isEmpty() && !userAms.isEmpty() && !passwordAms.isEmpty()) {
                        String calcUrl = String.valueOf(urlAms) + "name=" + antennaModel + "&manufacturer=" + antennaManufacturer;
                        int code = -1;
                        HttpsURLConnection con = null;

                        String basicAuthString = userAms + ":" + passwordAms;
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
                        String responseMessage = con.getResponseMessage();

                        con.getInputStream();
                        StringWriter writer = new StringWriter();

                        IOUtils.copy(con.getInputStream(), writer, StandardCharsets.UTF_8.toString());
                        String theString = writer.toString();

                        con.disconnect();

                        // TODO need to add schema validation or at least JSON validation
                        JsonParser parser = new JsonParser();
                        JsonElement je = parser.parse(theString);
                        // TODO added here
                        IntegrationField[] fieldArr = integrationFieldsFromJson(je);

                        JsonObject jsonObject = jo.getAsJsonObject();

                        JsonElement manufacturerPH = jsonObject.get("manufacturer");
                        JsonObject jsonManPH = manufacturerPH.getAsJsonObject();
                        JsonElement antennaPH = jsonObject.get("antenna");
                        JsonObject jsonAntPH = antennaPH.getAsJsonObject();


                        String athStatus = jsonString(jsonObject, "athStatus");
                        String antennaType = jsonString(jsonObject, "antennaType");
                        String name = jsonString(jsonManPH, "name");
                        String manId = jsonString(jsonManPH, "manId");
                        String antennaName = jsonString(jsonAntPH, "antennaName");
                        String atyId = jsonString(jsonAntPH, "atyId");
                        String athId = jsonString(jsonObject, "athId");
                        String ca = jsonString(jsonObject, "ca");
                        String centerHeight = jsonString(jsonObject, "centerHeight");
                        String centerWidth = jsonString(jsonObject, "centerWidth");
                        String commentText = jsonString(jsonObject, "commentText");
                        String connection = jsonString(jsonObject, "connection");
                        String diameterDishAntenna = jsonString(jsonObject, "diameterDishAntenna");
                        String frontSurface = jsonString(jsonObject, "frontSurface");
                        String frontSurfaceDishAntenna = jsonString(jsonObject, "frontSurfaceDishAntenna");
                        String frontSurfaceSalesEq = jsonString(jsonObject, "frontSurfaceSalesEq");
                        String frontSurfaceSales = jsonString(jsonObject, "frontSurfaceSales");
                        String frontWindLoad = jsonString(jsonObject, "frontWindLoad");
                        String frontWindSpeed = jsonString(jsonObject, "frontWindSpeed");
                        String height = jsonString(jsonObject, "height");
                        String validFrom = jsonString(jsonObject, "validFrom");
                        String validTill = jsonString(jsonObject, "validTill");
                        String length = jsonString(jsonObject, "length");
                        String maxWindLoad = jsonString(jsonObject, "maxWindLoad");
                        String maxWindSpeed = jsonString(jsonObject, "maxWindSpeed");
                        String modDate = jsonString(jsonObject, "modDate");
                        String mounting = jsonString(jsonObject, "mounting");
                        String portsPos = jsonString(jsonObject, "portsPos");
                        String salesRelatedName = jsonString(jsonObject, "salesRelatedName");
                        String version = jsonString(jsonObject, "version");
                        String weightInclMounting = jsonString(jsonObject, "weightInclMounting");
                        String weight = jsonString(jsonObject, "weight");
                        String width = jsonString(jsonObject, "width");
                        String windLoad = jsonString(jsonObject, "windLoad");
                        String windSpeed = jsonString(jsonObject, "windSpeed");

                        IntegrationField[] fieldArr = new IntegrationField[36];

                        fieldArr[0] = new IntegrationField("cstHerstellerTX", name);
                        fieldArr[1] = new IntegrationField("cstHerstellerIdNU", manId);
                        fieldArr[2] = new IntegrationField("cstAntennaStatusTX", athStatus);
                        fieldArr[3] = new IntegrationField("cstAntennaTypTX", antennaType);
                        fieldArr[4] = new IntegrationField("cstAntennaTypIdNU", atyId);
                        fieldArr[5] = new IntegrationField("cstAntennaTypVersionIdNU", athId);
                        fieldArr[6] = new IntegrationField("cstCaNU", ca);
                        fieldArr[7] = new IntegrationField("cstCenterHeightNU", centerHeight);
                        fieldArr[8] = new IntegrationField("cstCenterWidthNU", centerWidth);
                        fieldArr[9] = new IntegrationField("cstCommentTX", commentText);
                        fieldArr[10] = new IntegrationField("cstConnectionTX", connection);
                        fieldArr[11] = new IntegrationField("cstDiameterDishAntennaNU", diameterDishAntenna);
                        fieldArr[12] = new IntegrationField("cstFrontSalesNU", frontSurface);
                        fieldArr[13] = new IntegrationField("cstFrontSurfaceDishAntennaNU", frontSurfaceDishAntenna);
                        fieldArr[14] = new IntegrationField("cstFrontSurfaceSalesEqBL", frontSurfaceSalesEq);
                        fieldArr[15] = new IntegrationField("cstFrontSurfaceSalesNU", frontSurfaceSales);
                        fieldArr[16] = new IntegrationField("cstFrontWindLoadNU", frontWindLoad);
                        fieldArr[17] = new IntegrationField("cstFrontWindSpeedNU", frontWindSpeed);
                        fieldArr[18] = new IntegrationField("cstHeightNU", height);
                        fieldArr[19] = new IntegrationField("cstHistFromTX", validFrom);
                        fieldArr[20] = new IntegrationField("cstHistTillTX", validTill);
                        fieldArr[21] = new IntegrationField("cstlengthNU", length);
                        fieldArr[22] = new IntegrationField("cstMaxWindLoadNU", maxWindLoad);
                        fieldArr[23] = new IntegrationField("cstMaxWindSpeedNU", maxWindSpeed);
                        fieldArr[24] = new IntegrationField("cstModDateTX", modDate);
                        fieldArr[25] = new IntegrationField("cstMountingTX", mounting);
                        fieldArr[26] = new IntegrationField("cstPortsPosTX", portsPos);
                        fieldArr[27] = new IntegrationField("cstSalesRelatedNameTX", salesRelatedName);
                        fieldArr[28] = new IntegrationField("cstVersionNU", version);
                        fieldArr[29] = new IntegrationField("cstWeightInclMountingNU", weightInclMounting);
                        fieldArr[30] = new IntegrationField("cstWeightNU", weight);
                        fieldArr[31] = new IntegrationField("cstWidthNU", width);
                        fieldArr[32] = new IntegrationField("cstWindloadNU", windLoad);
                        fieldArr[33] = new IntegrationField("cstWindSpeedNU", windSpeed);
                        fieldArr[34] = new IntegrationField("jrsAntennaSpecIdTX", Long.toString(antennaId));
                        fieldArr[35] = new IntegrationField("cstAntennaNameTX", antennaName);

                        long reph = createDTO(fieldArr);

                    }
                } catch (Exception e) {
                    logserver.error(e.getMessage());
                    logserver.error(e);
                    task.setExecutionWasSuccessful(fail);
                    return (CustomParamTaskResult) task;
                }
                ;

            } else {
                throw new Exception("No values to send");
            }


            task.setExecutionWasSuccessful(success);
            return (CustomParamTaskResult) task;


        } catch (Exception e) {
            logserver.error(e.getMessage());
            logserver.error(e);
            task.setExecutionWasSuccessful(fail);
            return (CustomParamTaskResult) task;

        }

    }

    public static IntegrationField[] integrationFieldsFromJson(JsonElement je) {
        IntegrationField[] fieldArr = new IntegrationField[36];
        int counter = 0;

        if (je.isJsonPrimitive()) {
            if (je.getAsJsonPrimitive().isBoolean())
                return "Boolean";
            if (je.getAsJsonPrimitive().isString())
                return "String";
            if (je.getAsJsonPrimitive().isNumber()){
                return "Number";
            }
        }

        if (je.isJsonArray()) {
            sb = new StringBuilder("array<");
            for (JsonElement e : je.getAsJsonArray()) {
                sb.append(printClass(e, ident+ "    "));
            }
            sb.append(">");
            return sb.toString();
        }

        if (je.isJsonObject()) {
            sb = new StringBuilder("map<\n");
            for (Map.Entry<String, JsonElement> e : je.getAsJsonObject().entrySet()) {
                if (e.getValue().)

                sb.append(ident);
                sb.append(e.getKey()).append(":");
                sb.append(printClass(e.getValue(), ident+"   ")); //ITERATION on itself
                sb.append("\n");
            }
            sb.append(ident);
            sb.append(">");
            return sb.toString();
        }
        return "";

        return fieldArr;
    }

    public static String getFieldValue(long specId, String fieldName) {
        SmartObject sObj = null;
        sObj = SmartObjectUtils.getSmartObject(specId);
        String fieldVal = "";

        try {
            TextField field = (TextField) sObj.getField(fieldName);
            if ((field != null) && field.isValueExists()) {
                fieldVal = field.getValueAsString();
            } else {
                ;
            }
        } catch (InvalidFieldRequestException e) {
            ;
        }
        return fieldVal;
    }

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

    public static String jsonString(JsonObject jsonPL, String keyword) {
        String fieldVal = null;
        try {
            JsonElement jsonEl = jsonPL.get(keyword);
            fieldVal = jsonEl.getAsString();
        } catch (Exception e) {
            logserver.error(e);
        }
        return fieldVal;
    }


}
