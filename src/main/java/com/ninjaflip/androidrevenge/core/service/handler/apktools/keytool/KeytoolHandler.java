package com.ninjaflip.androidrevenge.core.service.handler.apktools.keytool;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ninjaflip.androidrevenge.beans.KeystoreBean;
import com.ninjaflip.androidrevenge.core.db.dao.KeystoreDao;
import com.ninjaflip.androidrevenge.core.db.dao.UserDao;
import com.ninjaflip.androidrevenge.core.keytool.KeytoolManager;
import com.ninjaflip.androidrevenge.core.security.ServerSecurityManager;
import com.ninjaflip.androidrevenge.utils.Utils;
import net.minidev.json.JSONObject;
import org.apache.log4j.Logger;
import spark.Request;
import spark.Response;
import spark.Route;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Solitario on 20/09/2017.
 * <br>
 * Handler that takes care of all actions related to keystores
 */
public class KeytoolHandler implements Route {

    private final static Logger LOGGER = Logger.getLogger(KeytoolHandler.class);

    @Override
    public Object handle(Request request, Response response) {
        String userUuid = ServerSecurityManager.getInstance()
                .getUserUuidFromToken(request.cookie("token"));

        String action = request.queryParams("action");

        try {
            // check if action is valid
            if (action == null || action.equals("")) {
                response.type("text/plain; charset=utf-8");
                response.status(400);
                String reason = "You must provide a keytool action parameter!";
                response.body(reason);
                return reason;
            }

            switch (action) {
                case "GET_ALL": {
                    List<KeystoreBean> listKeystores = KeystoreDao.getInstance().getAll(userUuid);
                    for(KeystoreBean ks : listKeystores){
                        ks.setOwner(null); // hide owner data
                        ks.setBlob(null);// hide blob
                    }
                    Gson gson = new GsonBuilder().disableHtmlEscaping().create();
                    // send response
                    response.type("application/json; charset=UTF-8");
                    response.status(200);
                    return gson.toJson(listKeystores);
                }
                case "VALIDATE_FORM": {
                    String alias = request.queryParams("alias");
                    if (alias == null || alias.equals("")) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "You must provide a keystore alias parameter!";
                        response.body(reason);
                        return reason;
                    }

                    String kspwd = request.queryParams("kspwd");
                    if (kspwd == null || kspwd.equals("")) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "You must provide a keystore password parameter!";
                        response.body(reason);
                        return reason;
                    }

                    String keypwd = request.queryParams("keypwd");
                    if (keypwd == null || keypwd.equals("")) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "You must provide a private key password parameter!";
                        response.body(reason);
                        return reason;
                    }

                    // validity duration parameter
                    String validity = request.queryParams("validity");
                    if (validity == null || validity.equals("")) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "You must provide a validity parameter (number of years)!";
                        response.body(reason);
                        return reason;
                    } else {
                            /* check if certificate validity duration is a valid integer between 1 and 99 */
                        try {
                            int validityNumber = Integer.parseInt(validity);
                            if (validityNumber > 99 || validityNumber < 1) {
                                response.type("text/plain; charset=utf-8");
                                response.status(400);
                                String reason = "validity must be a number between 1 and 99!";
                                response.body(reason);
                                return reason;
                            }
                        } catch (NumberFormatException e) {
                            response.type("text/plain; charset=utf-8");
                            response.status(400);
                            String reason = "validity must be a number between 1 and 99!";
                            response.body(reason);
                            return reason;
                        }
                    }

                    String description = request.queryParams("description");
                    if (description == null) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "You must provide a description parameter!";
                        response.body(reason);
                        return reason;
                    }

                    if (description.length() > 200) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "Description must not exceed 200 characters!";
                        response.body(reason);
                        return reason;
                    }
                    response.status(204);
                    return "";
                }
                case "SUBMIT_CREATE_KEYSTORE": {
                    String alias = request.queryParams("alias");
                    if (alias == null || alias.equals("")) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "You must provide a keystore alias parameter!";
                        response.body(reason);
                        return reason;
                    }

                    String kspwd = request.queryParams("kspwd");
                    if (kspwd == null || kspwd.equals("")) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "You must provide a keystore password parameter!";
                        response.body(reason);
                        return reason;
                    }

                    String keypwd = request.queryParams("keypwd");
                    if (keypwd == null || keypwd.equals("")) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "You must provide a private key password parameter!";
                        response.body(reason);
                        return reason;
                    }

                    // validity duration parameter
                    String validity = request.queryParams("validity");
                    if (validity == null || validity.equals("")) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "You must provide a validity parameter (number of years)!";
                        response.body(reason);
                        return reason;
                    } else {
                            /* check if certificate validity duration is a valid integer between 1 and 99 */
                        try {
                            int validityNumber = Integer.parseInt(validity);
                            if (validityNumber > 99 || validityNumber < 1) {
                                response.type("text/plain; charset=utf-8");
                                response.status(400);
                                String reason = "validity must be a number between 1 and 99!";
                                response.body(reason);
                                return reason;
                            }
                        } catch (NumberFormatException e) {
                            response.type("text/plain; charset=utf-8");
                            response.status(400);
                            String reason = "validity must be a number between 1 and 99!";
                            response.body(reason);
                            return reason;
                        }
                    }

                    String description = request.queryParams("description");
                    if (description == null) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "Description parameter not found!";
                        response.body(reason);
                        return reason;
                    }

                    if (description.length() > 200) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "Description must not exceed 200 characters!";
                        response.body(reason);
                        return reason;
                    }

                    byte[] blob = KeytoolManager.getInstance().createNewKeystore(alias, kspwd, keypwd, request.queryParams("CN"), request.queryParams("OU"), request.queryParams("O"),
                            request.queryParams("L"), request.queryParams("ST"), request.queryParams("C"), request.queryParams("E"), Integer.parseInt(validity));
                    KeystoreBean ksBean = new KeystoreBean(kspwd, alias, keypwd, description, UserDao.getInstance().getByUuid(userUuid), blob);
                    KeystoreDao.getInstance().insert(ksBean);

                    response.status(204);
                    return "";
                }
                case "REMOVE_KEYSTORE":{
                    String keystoreUuid = request.queryParams("keystoreUuid");
                    if (keystoreUuid == null || keystoreUuid.equals("")) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "You must provide a keystore uuid parameter!";
                        response.body(reason);
                        return reason;
                    }

                    // check ks record exists in the database
                    KeystoreBean ksBean = KeystoreDao.getInstance().getByUuid(keystoreUuid);
                    if(ksBean == null){
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "This keystore does not exists in the database!";
                        response.body(reason);
                        return reason;
                    }

                    // // check ks belongs to user uuid
                    if(!KeystoreDao.getInstance().keystoreExistsAndBelongsToUser(keystoreUuid, userUuid)){
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "This keystore does not belong to you!!";
                        response.body(reason);
                        return reason;
                    }
                    // remove it from db
                    KeystoreDao.getInstance().delete(ksBean);

                    // send response
                    response.status(204);
                    return "";
                }
                case "DETAILS_KEYSTORE":{
                    String keystoreUuid = request.queryParams("keystoreUuid");
                    if (keystoreUuid == null || keystoreUuid.equals("")) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "You must provide a keystore uuid parameter!";
                        response.body(reason);
                        return reason;
                    }

                    // check ks record exists in the database
                    KeystoreBean ksBean = KeystoreDao.getInstance().getByUuid(keystoreUuid);
                    if(ksBean == null){
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "This keystore does not exists in the database!";
                        response.body(reason);
                        return reason;
                    }

                    // // check ks belongs to user uuid
                    if(!KeystoreDao.getInstance().keystoreExistsAndBelongsToUser(keystoreUuid, userUuid)){
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "This keystore does not belong to you!!";
                        response.body(reason);
                        return reason;
                    }

                    List<JSONObject> details = KeytoolManager.getInstance().getKeystoreDetails(ksBean);

                    JSONObject properties = new JSONObject();
                    properties.put("alias", ksBean.getAlias());
                    properties.put("ksPwd", ksBean.getKsPass());
                    properties.put("keyPwd", ksBean.getKeyPass());
                    properties.put("description", ksBean.getDescription());
                    properties.put("dateCreated", ksBean.getDateCreated());
                    properties.put("dateCreatedAgo", ksBean.getDateCreated());

                    JSONObject result = new JSONObject();
                    result.put("details", details);
                    result.put("properties", properties);

                    response.type("application/json; charset=UTF-8");
                    response.status(200);
                    return result;
                }
                case "VALIDATE_KEYSTORE":{
                    String tmpFilePath = request.queryParams("tmpFilePath");
                    if (tmpFilePath == null || tmpFilePath.equals("")) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "You must provide a keystore tmp file path parameter!";
                        response.body(reason);
                        return reason;
                    }

                    String ksPwd = request.queryParams("ksPwd");
                    if (ksPwd == null || ksPwd.equals("")) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "You must provide keystore password parameter!";
                        response.body(reason);
                        return reason;
                    }

                    File ks = new File(tmpFilePath);
                    if(!ks.exists()){
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "The keystore file you uploaded does not exist!";
                        response.body(reason);
                        return reason;
                    }

                    List<JSONObject> info;
                    try{
                        info = KeytoolManager.getInstance().getKeystoreCertificatesInfo(tmpFilePath, ksPwd);
                    }catch (Exception e){
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "Couldn't open keystore using password '"+ksPwd+"', please check your keystore!";
                        response.body(reason);
                        return reason;
                    }

                    JSONObject result = new JSONObject();
                    result.put("info", info);
                    response.type("application/json; charset=UTF-8");
                    response.status(200);
                    return result;

                }
                case "SUBMIT_IMPORT_KEYSTORE":{
                    String tmpFilePath = request.queryParams("tmpFilePath");
                    if (tmpFilePath == null || tmpFilePath.equals("")) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "You must provide a keystore tmp file path parameter!";
                        response.body(reason);
                        return reason;
                    }

                    String ksPwd = request.queryParams("ksPwd");
                    if (ksPwd == null || ksPwd.equals("")) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "You must provide keystore password parameter!";
                        response.body(reason);
                        return reason;
                    }

                    String alias = request.queryParams("alias");
                    if (alias == null || alias.equals("")) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "You must provide alias parameter!";
                        response.body(reason);
                        return reason;
                    }

                    String keyPwd = request.queryParams("keyPwd");
                    if (keyPwd == null || keyPwd.equals("")) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "You must provide a private key password parameter!";
                        response.body(reason);
                        return reason;
                    }

                    // validate keystore/ksPwd
                    KeyStore ks;
                    try {
                        ks = KeytoolManager.getInstance().validateKeystorePassword(tmpFilePath, ksPwd);
                    }catch (Exception ex){
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "Couldn't open keystore using password '"+ksPwd+"', please check your keystore!";
                        response.body(reason);
                        return reason;
                    }

                    // validate alias/keyPassword
                    try{
                        KeytoolManager.getInstance().validateAliasPassword(ks, alias, keyPwd);
                    }catch (Exception ex){
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "Wrong password '"+keyPwd+"' for alias '"+alias+"', please check your PrivateKey password!";
                        response.body(reason);
                        return reason;
                    }

                    String description = request.queryParams("description");
                    if (description == null) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "Description parameter not found!";
                        response.body(reason);
                        return reason;
                    }

                    if (description.length() > 200) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "Description must not exceed 200 characters!";
                        response.body(reason);
                        return reason;
                    }

                    File ksTmpFile = new File(tmpFilePath);
                    if(!ksTmpFile.exists()){
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "The keystore file you uploaded does not exist!";
                        response.body(reason);
                        return reason;
                    }

                    // add its record to database
                    KeystoreBean ksBean = new KeystoreBean(ksPwd, alias, keyPwd, description,
                            UserDao.getInstance().getByUuid(userUuid), Files.readAllBytes(Paths.get(ksTmpFile.getPath())));
                    KeystoreDao.getInstance().insert(ksBean);

                    // send empty response
                    response.status(204);
                    return "";
                }
                case "GET_MODAL_HTML_UPDATE_KEYSTORE":{
                    String keystoreUuid = request.queryParams("keystoreUuid");
                    if (keystoreUuid == null || keystoreUuid.equals("")) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "Keystore uuid parameter not found!";
                        response.body(reason);
                        return reason;
                    }

                    if(!KeystoreDao.getInstance().keystoreExistsAndBelongsToUser(keystoreUuid, userUuid)){
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "This keystore does not exists or does not belong to you!";
                        response.body(reason);
                        return reason;
                    }

                    KeystoreBean ksBean = KeystoreDao.getInstance().getByUuid(keystoreUuid);

                    Map<String, String> templateVarValues = new HashMap<>();
                    templateVarValues.put("ks_alias", ksBean.getAlias());
                    templateVarValues.put("ks_uuid", ksBean.getUuid());
                    templateVarValues.put("ks_description", ksBean.getDescription());
                    String stringResponse = Utils.loadTemplateAsString("apk_reverse/keytool",
                            "mst_tmpl_modal_content_update_ks_description", templateVarValues);

                    response.type("text/html; charset=utf-8");
                    response.status(200);
                    return stringResponse;
                }
                case "SUBMIT_UPDATE_KEYSTORE":{
                    String keystoreUuid = request.queryParams("keystoreUuid");
                    if (keystoreUuid == null || keystoreUuid.equals("")) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "Keystore uuid parameter not found!";
                        response.body(reason);
                        return reason;
                    }

                    String ksDescription = request.queryParams("ksDescription");
                    if (ksDescription == null || ksDescription.equals("")) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "Keystore new description parameter not found!";
                        response.body(reason);
                        return reason;
                    }

                    if(!KeystoreDao.getInstance().keystoreExistsAndBelongsToUser(keystoreUuid, userUuid)){
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "This keystore does not exists or does not belong to you!";
                        response.body(reason);
                        return reason;
                    }

                    KeystoreBean ksBean = KeystoreDao.getInstance().getByUuid(keystoreUuid);
                    ksBean.setDescription(ksDescription);
                    KeystoreDao.getInstance().update(ksBean);

                    response.status(204);
                    return "";
                }
                default: {
                    response.type("text/plain; charset=utf-8");
                    response.status(400);
                    String reason = "Unknown keytool action parameter!";
                    response.body(reason);
                    return reason;
                }
            }
        } catch (Exception e) {
            //e.printStackTrace();
            LOGGER.error("error: " + e.getMessage());
            response.type("text/plain; charset=UTF-8");
            String reason = e.getMessage();
            response.status(500);
            response.body(reason);
            return reason;
        }
    }
}
