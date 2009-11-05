package shasta.rating;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

public class WRSClient {
	/**
	 * Sends an HTTP GET request to a url
	 * 
	 * @param endpoint
	 *            - The URL of the server. (Example:
	 *            " http://www.yahoo.com/search")
	 * @param requestParameters
	 *            - all the request parameters (Example:
	 *            "param1=val1&param2=val2"). Note: This method will add the
	 *            question mark (?) to the request - DO NOT add it yourself
	 * @return - The response from the end point
	 */
	public static String sendGetRequest(String endpoint,
			String requestParameters) {
		String result = null;
		if (endpoint.startsWith("http://")) {
			// Send a GET request to the servlet
			try {
				// Construct data
				StringBuffer data = new StringBuffer();

				// Send data
				String urlStr = endpoint;
				if (requestParameters != null && requestParameters.length() > 0) {
					urlStr += "?" + requestParameters;
				}
				URL url = new URL(urlStr);
				URLConnection conn = url.openConnection();

				// Get the response
				BufferedReader rd = new BufferedReader(new InputStreamReader(
						conn.getInputStream()));
				StringBuffer sb = new StringBuffer();
				String line;
				while ((line = rd.readLine()) != null) {
					sb.append(line);
				}
				rd.close();
				result = sb.toString();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return result;
	}
	/**
	 * 
	 * @param site
	 * @return site rating: g - for good, w - for warning b - for bad (red) and u - for unknown
	 */
	public static char getRatingForSite(String site) {
		String result = sendGetRequest("http://ratings-wrs.symantec.com/rating", "url="+site);
		//System.out.print(result);
		int resIdx = result.indexOf(" r=\"");
		if(resIdx < 0){
			return('u');	
		}
		else {						
			return result.charAt(resIdx+4);			
		}	
	}
	/**
	 * http://ratings-wrs.symantec.com/rating?url=cnn.com
	 * result is
	 * <symantec v="2.3">
	 *	<site id="cnn.com" r="g" sr="g" br="g" cache="600"/>
	 *	</symantec>
	 */
	public static void main(String [] args)
	{
		if(args.length != 1){
			System.out.print("Usage: java shasta.rating.WRSClient <site>");			
		}
		else {
			char res = getRatingForSite(args[0]);
			System.out.print(res);
		}
	}


}
