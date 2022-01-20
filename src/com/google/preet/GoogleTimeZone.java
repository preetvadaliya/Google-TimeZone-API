package com.google.preet;

import android.app.Activity;
import com.google.appinventor.components.annotations.*;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.PropertyTypeConstants;
import com.google.appinventor.components.runtime.AndroidNonvisibleComponent;
import com.google.appinventor.components.runtime.ComponentContainer;
import com.google.appinventor.components.runtime.EventDispatcher;
import com.google.appinventor.components.runtime.util.AsynchUtil;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

@DesignerComponent(
  category = ComponentCategory.EXTENSION,
  description = "The Time Zone API provides time offset data for locations on the surface of the earth. Request the time zone information for a specific latitude/longitude pair and date. The API returns the name of that time zone, the time offset from UTC, and the daylight savings offset.",
  nonVisible = true,
  iconName = "https://img.icons8.com/color/16/000000/google-logo.png",
  version = 1,
  helpUrl = ""
)
@SimpleObject(external = true)
public class GoogleTimeZone extends AndroidNonvisibleComponent {

  private String apiKey;
  private final Activity activity;

  public GoogleTimeZone(ComponentContainer componentContainer) {
    super(componentContainer.$form());
    this.activity = componentContainer.$context();
  }

  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING, defaultValue = "Your API Key")
  @SimpleProperty(description = "Your Google TimeZone API key.")
  public void APIKey(final String apiKey) {
    this.apiKey = apiKey;
  }

  @SimpleProperty
  public String APIKey() {
    return this.apiKey;
  }

  @SimpleEvent(description = "Event raised when error occurred during execution.")
  public void ErrorOccurred(final String errorMessage) {
    EventDispatcher.dispatchEvent(this, "ErrorOccurred", errorMessage);
  }

  @SimpleEvent(description = "Event raised when time-zone data got using API.")
  public void GotTimeZone(final int dstOffset, final int rawOffset, final String status, final String timeZoneId, final String timeZoneName) {
    EventDispatcher.dispatchEvent(this, "GotTimeZone", dstOffset, rawOffset, status, timeZoneId, timeZoneName);
  }

  @SimpleFunction(description = "Send new request for given latitude, longitude and timestamp.")
  public void GetTimeZone(final String latitude, final String longitude, final String timestamp) {
    String url = "https://maps.googleapis.com/maps/api/timezone/json?";
    url += "location=" + latitude.trim() + "%2C" + longitude.trim();
    url += "&timestamp=" + timestamp.trim();
    url += "&key=" + this.apiKey.trim();
    final String finalUrl = url;
    AsynchUtil.runAsynchronously(new Runnable() {
      @Override
      public void run() {
        try {
          final URL requestURL = new URL(finalUrl);
          HttpURLConnection httpURLConnection = (HttpURLConnection) requestURL.openConnection();
          httpURLConnection.setDoOutput(true);
          httpURLConnection.setRequestMethod("GET");
          httpURLConnection.setConnectTimeout(5000);
          BufferedReader bufferedReader = null;
          if (httpURLConnection.getResponseCode() == 200) {
            bufferedReader = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()));
          } else {
            bufferedReader = new BufferedReader(new InputStreamReader(httpURLConnection.getErrorStream()));
          }
          StringBuilder stringBuilder = new StringBuilder();
          String line;
          while ((line = bufferedReader.readLine()) != null) {
            stringBuilder.append(line).append("\n");
          }
          bufferedReader.close();
          final String responseContent = stringBuilder.toString();
          activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
              try {
                JSONObject jsonObject = new JSONObject(responseContent);
                GotTimeZone(jsonObject.getInt("dstOffset"), jsonObject.getInt("rawOffset"), jsonObject.getString("status"), jsonObject.getString("timeZoneId"), jsonObject.getString("timeZoneName"));
              } catch (JSONException e) {
                e.printStackTrace();
                ErrorOccurred(responseContent);
              }
            }
          });
        } catch (MalformedURLException e) {
          e.printStackTrace();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    });
  }

}
