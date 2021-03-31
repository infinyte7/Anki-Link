package io.github.infinyte7.ankilink;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;
import com.ichi2.anki.api.AddContentApi;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fi.iki.elonen.NanoHTTPD;

public class AnkiNanoHTTPD extends NanoHTTPD {
    private Context context;
    private AnkiDroidHelper mAnkiDroid;
    private boolean isAllowCors;
    SharedPreferences sharedPreferences;

    public AnkiNanoHTTPD(int port, Context context) {
        super(port);
        this.context = context;
    }

    public AnkiNanoHTTPD(String hostname, int port, Context context) {
        super(hostname, port);
        this.context = context;
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        Method method = session.getMethod();

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        isAllowCors = sharedPreferences.getBoolean(context.getString(R.string.allow_cors), false);

        if (Method.GET.equals(method)) {
            if (uri.equals("/")) {
                String responseMsg = "Anki Link Server is running...\n";
                Response response = newFixedLengthResponse(responseMsg);

                if (isAllowCors) {
                    response.addHeader("Access-Control-Allow-Origin", "*");

                    responseMsg = "Anki Link Server is running with cors enabled...\n";
                    response = newFixedLengthResponse(responseMsg);
                }

                return response;
            }
        }

        if (Method.POST.equals(method)) {
            String exceptionMsg = "";
            try {
                session.parseBody(new HashMap<String, String>());
                String body = session.getQueryParameterString();

                JSONObject json = new JSONObject(body);

                String action = json.getString("action");
                String version = json.getString("version");
                String params = json.getString("params");

                Log.i("Response::", json.toString());

                String responseMsg = performAction(action, version, params);

                Response response = newFixedLengthResponse(responseMsg);

                if (isAllowCors) {
                    response.addHeader("Access-Control-Allow-Origin", "*");
                }

                return response;

            } catch (IOException e) {
                e.printStackTrace();
                exceptionMsg += e.getLocalizedMessage();
            } catch (ResponseException e) {
                e.printStackTrace();
                exceptionMsg += e.getLocalizedMessage();
            } catch (JSONException e) {
                e.printStackTrace();
                exceptionMsg += e.getLocalizedMessage();
            }
            return newFixedLengthResponse(exceptionMsg);
        }

        return newFixedLengthResponse("Bad Request");
    }

    private String performAction(String action, String version, String params) {
        mAnkiDroid = new AnkiDroidHelper(context);
        AddContentApi api = mAnkiDroid.getApi();

        String modelName = "";
        long mid = 1l;
        long did = 1l;

        JSONObject resultJSONObject = new JSONObject();
        String error = "null";

        switch (action) {
            // deck actions
            case "getDeckName":
                Long deckId = Long.parseLong(params);
                String deckName = api.getDeckName(deckId);
                try {
                    resultJSONObject.put("result", deckName);
                    resultJSONObject.put("error", error);
                    return resultJSONObject.toString();
                } catch (JSONException e) {
                    e.printStackTrace();
                    return e.getLocalizedMessage();
                }

            case "getSelectedDeckName":
                try {
                    resultJSONObject.put("result", api.getSelectedDeckName());
                    resultJSONObject.put("error", error);
                    return resultJSONObject.toString();
                } catch (JSONException e) {
                    e.printStackTrace();
                    return e.getLocalizedMessage();
                }

            case "getDeckList":
                Map<Long, String> deckList = api.getDeckList();

                try {
                    Gson gson = new Gson();
                    String jsonStr = gson.toJson(deckList);
                    resultJSONObject.put("result", jsonStr);
                    resultJSONObject.put("error", error);
                    return resultJSONObject.toString();
                } catch (JSONException e) {
                    e.printStackTrace();
                    return e.getLocalizedMessage();
                }

            case "addNewDeck":
                Long newDeckId = api.addNewDeck(params);
                try {
                    resultJSONObject.put("result", newDeckId);
                    resultJSONObject.put("error", error);
                    return resultJSONObject.toString();
                } catch (JSONException e) {
                    e.printStackTrace();
                    Toast.makeText(context, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                }
                break;

            // model actions
            case "getModelName":
                Long modelId = Long.parseLong(params);
                String modelName1 = api.getModelName(modelId);
                try {
                    resultJSONObject.put("result", modelName1);
                    resultJSONObject.put("error", error);
                    return resultJSONObject.toString();
                } catch (JSONException e) {
                    e.printStackTrace();
                    return e.getLocalizedMessage();
                }

            case "getCurrentModelId":
                try {
                    resultJSONObject.put("result", api.getCurrentModelId());
                    resultJSONObject.put("error", error);
                    return resultJSONObject.toString();
                } catch (JSONException e) {
                    e.printStackTrace();
                    return e.getLocalizedMessage();
                }

            case "getModelList":
                Map<Long, String> modelList = api.getModelList();
                try {
                    Gson gson = new Gson();
                    String jsonStr = gson.toJson(modelList);
                    resultJSONObject.put("result", jsonStr);
                    resultJSONObject.put("error", error);
                    return resultJSONObject.toString();
                } catch (JSONException e) {
                    e.printStackTrace();
                    return e.getLocalizedMessage();
                }

            case "getModelListWithNumFields":
                int minNumFields = Integer.parseInt(params);
                Map<Long, String> modelListWithNumFields = api.getModelList(minNumFields);
                try {
                    Gson gson = new Gson();
                    String jsonStr = gson.toJson(modelListWithNumFields);
                    resultJSONObject.put("result", jsonStr);
                    resultJSONObject.put("error", error);
                    return resultJSONObject.toString();
                } catch (JSONException e) {
                    e.printStackTrace();
                    return e.getLocalizedMessage();
                }

            case "addNewBasicModel":
                modelName = params;
                Long newBasicModelId = api.addNewBasicModel(modelName);
                try {
                    resultJSONObject.put("result", newBasicModelId);
                    resultJSONObject.put("error", error);
                    return resultJSONObject.toString();
                } catch (JSONException e) {
                    e.printStackTrace();
                    return e.getLocalizedMessage();
                }

            case "addNewBasic2Model":
                modelName = params;
                Long newBasic2ModelId = api.addNewBasic2Model(modelName);
                try {
                    resultJSONObject.put("result", newBasic2ModelId);
                    resultJSONObject.put("error", error);
                    return resultJSONObject.toString();
                } catch (JSONException e) {
                    e.printStackTrace();
                    return e.getLocalizedMessage();
                }

            case "addNewCustomModel":
                try {
                    JSONObject json = new JSONObject(params);
                    modelName = json.optString("modelName");
                    String[] fields = jsonArrayToStringArray(json, "fields");
                    String[] cards = jsonArrayToStringArray(json, "cards");
                    String[] qfmt = jsonArrayToStringArray(json, "qfmt");
                    String[] afmt = jsonArrayToStringArray(json, "afmt");
                    String css = json.getString("css");
                    did = json.getLong("did");
                    //int sortf = json.optInt("sortf", 0);

                    Long newCustomModelId = api.addNewCustomModel(modelName, fields, cards, qfmt, afmt, css, did, null);

                    resultJSONObject.put("result", newCustomModelId);
                    resultJSONObject.put("error", error);
                    return resultJSONObject.toString();

                } catch (JSONException e) {
                    e.printStackTrace();
                    return e.getLocalizedMessage();
                }

            // note actions
            case "getNote":
                long nid = Long.parseLong(params);
                try {
                    resultJSONObject.put("result", api.getNote(nid));
                    resultJSONObject.put("error", error);
                    return resultJSONObject.toString();
                } catch (JSONException e) {
                    e.printStackTrace();
                    return e.getLocalizedMessage();
                }

            case "getNoteCount":
                mid = Long.parseLong(params);
                try {
                    resultJSONObject.put("result", api.getNoteCount(mid));
                    resultJSONObject.put("error", error);
                    return resultJSONObject.toString();
                } catch (JSONException e) {
                    e.printStackTrace();
                    return e.getLocalizedMessage();
                }

            case "addNote":
                try {
                    JSONObject json = new JSONObject(params);
                    mid = json.getLong("mid");
                    did = json.getLong("did");
                    String[] fields = jsonArrayToStringArray(json, "fields");

                    String[] tagsArray = jsonArrayToStringArray(json, "tags");
                    Set<String> tags = new HashSet<String>(Arrays.asList(tagsArray));

                    Long newNoteId = api.addNote(mid, did, fields, tags);

                    resultJSONObject.put("result", newNoteId);
                    resultJSONObject.put("error", error);
                    return resultJSONObject.toString();

                } catch (JSONException e) {
                    e.printStackTrace();
                    return e.getLocalizedMessage();
                }

//            case "addNotes":
//                return null;

        }
        return "";
    }

    public String[] jsonArrayToStringArray(JSONObject json, String key) throws JSONException {
        JSONArray jsonArray = json.optJSONArray(key);
        List<String> list = new ArrayList<String>();
        for (int i = 0; i < jsonArray.length(); i++) {
            list.add(jsonArray.getString(i));
        }
        String[] stringArray = list.toArray(new String[list.size()]);
        return stringArray;
    }

}