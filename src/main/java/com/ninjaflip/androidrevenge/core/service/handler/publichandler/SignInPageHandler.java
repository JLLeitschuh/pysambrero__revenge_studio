package com.ninjaflip.androidrevenge.core.service.handler.publichandler;

import com.ninjaflip.androidrevenge.core.security.LicenseManager;
import com.ninjaflip.androidrevenge.utils.Utils;
import com.x5.template.Chunk;
import spark.Request;
import spark.Response;
import spark.Route;

import java.io.InputStream;
import java.net.URL;

/**
 * Created by Solitario on 17/07/2017.
 * <p>
 * Load sign in page handler
 */
public class SignInPageHandler implements Route {
    @Override
    public Object handle(Request request, Response response) throws Exception {
        response.type("text/html; charset=utf-8");
        response.status(200);
        return getSignInPage();
    }

    public static String getSignInPage() {
        try {
            URL url = SignInPageHandler.class.getResource("/www/static/public/html/signin.html");

            InputStream stream = url.openStream();
            // Return a String which has all
            // the contents of the file.
            String htmlContent = new String(Utils.readBytesFromStream(stream), "UTF-8");
            stream.close();

            Chunk html = new Chunk();
            html.append(htmlContent);

            html.setErrorHandling(true, System.err);
            String lsc ="";
            try{
                //lsc = LicenseManager.getInstance().getSerialAsHexString();
            }catch (Exception e){
                // do nothing
            }
            html.set("srl", lsc);

            return html.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
