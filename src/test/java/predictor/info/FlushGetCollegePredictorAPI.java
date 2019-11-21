package predictor.info;

import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.testng.*;
import org.testng.annotations.*;
import common.Common;


public class FlushGetCollegePredictorAPI extends Common{
	static String apiPath;
	String api;
	static String[] params;
	Response apiResponse = null;
	String apiResponseMsgExpected;
	static String testDataFilePath;
	String stream;;
	static ArrayList<Integer> responseTimes = new ArrayList<Integer>();

	@BeforeClass
	public void doItBeforeTest() throws FileNotFoundException {
		// apiResponseExpectedTime = 200; // in MS
		loadPropertiesFromConfig("predictor");
		// api path
		apiPath = "predictor/api/v1/info/getCollegePredictor";
		api = serverPath + apiPath;
		// API params in sequence followed by apiResponseMsgExpected
		params = new String[] { "stream", "apiResponseMsgExpected" };
		// test data file
		testDataFilePath = "//src//test//resources//predictor//info//GetCollegePredictor.xlsx";
		PrintWriter pw = new PrintWriter("Logs//CollegePredictorMapLogs.txt");
		pw.close();
	}
	
	@DataProvider
	public static String[][] testData() throws Exception {
		// The number of times data is repeated, test will be executed the same no. of
		// times
		String[][] testData = prepareTestData(params, testDataFilePath);
		return testData;
	}
	@Test(dataProvider = "testData", priority = 1)
	public void verifyGetCollegePredictor(String stream, String apiResponseMsgExpected) {
		this.apiResponseMsgExpected = apiResponseMsgExpected;
		// pass api params and hit api
		if (stream.equals("ignoreHeader"))
			apiResponse = RestAssured.given().when().post(api).then().extract().response();
		else if (stream.equals("ignoreInput"))
			apiResponse = RestAssured.given().header("AUTHREQUEST", "INFOEDGE_SHIKSHA").when().post(api).then()
					.extract().response();
		else
			apiResponse = RestAssured.given().header("AUTHREQUEST", "INFOEDGE_SHIKSHA").param("stream", stream).when()
			.post(api).then().extract().response();
		// fetch status code from response
		int statusCode = apiResponse.getStatusCode();
		JsonPath jsonPath = apiResponse.jsonPath();

		if (statusCode == 200) { // Positive case
			Reporter.log("Positive case", true);
			verifyPostiveCases(stream, jsonPath);			
		} else if (statusCode == 403) { // unauthorized request
			Reporter.log("Unauthorized request case", true);
			verifyUnauthorizedRequestCase(jsonPath);
		} else {
			Reporter.log(", InCorrect Response Code : " + statusCode, true);
			Assert.fail("InCorrect Response Code : " + statusCode);
		}

		long apiResponseActualTime = apiResponse.getTimeIn(TimeUnit.MILLISECONDS);
		responseTimes.add((int) apiResponseActualTime);

		if (apiResponseActualTime > apiResponseExpectedTime) {
			Reporter.log("<br> API Response time : " + apiResponseActualTime + " MS is greater than expected time ("
					+ apiResponseExpectedTime + " MS)", true);
			Assert.fail("API Response time : " + apiResponseActualTime + " MS is greater than expected time ("
					+ apiResponseExpectedTime + " MS)");
		} else {
			Reporter.log("<br> API Response time : " + apiResponseActualTime + " MS", true);
		}
	}
	public void verifyPostiveCases(String stream, JsonPath jsonPath) {
		try {
			
			// verify status is success
			String statusResponse = jsonPath.getString("status");
			if (statusResponse.contentEquals("success")) {
				Reporter.log(", Correct status [success]", true);
			} else {
				Reporter.log(", InCorrect status [" + statusResponse + "]", true);
				Assert.fail("InCorrect status [" + statusResponse + "]");
			}

			// verify message is null
			String messageResponse = jsonPath.getString("message");
			if (messageResponse == null) {
				Reporter.log(", Correct message [null]", true);
			} else {
				Reporter.log(", InCorrect message [" + messageResponse + "]", true);
				Assert.fail("InCorrect message [" + messageResponse + "]");
			}
			System.out.println(stream);
			if(!jsonPath.getList("data.popularPredictor").isEmpty()){
			List<String> name = jsonPath.getList("data.popularPredictor.name");
			List<String> url = jsonPath.getList("data.popularPredictor.url");
			Map<String, String> jsonpopularMap= new HashMap<String,String>();
			Map<String, String> propMap = new HashMap<String, String>();
			for(int i=0;i<name.size();i++)
			{
				String predictorname = name.get(i);
				String predictorfullurl = url.get(i);
				String predictorUrl=predictorfullurl.substring(predictorfullurl.indexOf(".com")+4);
				if(jsonpopularMap.containsValue(predictorUrl)){
					
				}else{
					jsonpopularMap.put(predictorname, predictorUrl);
				}
			}	
			for ( String key : jsonpopularMap.keySet() ) {
				if(key.contains(" ")){
					key = key.replace(" ", "-");
					key = key.toUpperCase();
				}
		
		Properties props = new Properties();
		props.load(new FileInputStream("collegePredictorConfig.properties"));
		for (Enumeration<?> e = props.propertyNames(); e.hasMoreElements(); ) {
		    String propname = (String)e.nextElement();
		    // now you have name and value
		    if(key.equalsIgnoreCase("JEE-MAIN")){
		    	key = "JEE-MAINS";
		    }
		    
		    String propshortname = null;
    		String prepurl = null;
		    String propkey = null;
		    propkey = "collegeConfig.cpExams."+key;
		    if (propname.startsWith(propkey)) {
		    	 propshortname = props.getProperty(propkey+".shortName");
		    	String propCollegeUrl = props.getProperty(propkey+".collegeUrl");
		    	String propdirectory = props.getProperty(propkey+".directoryName");
		    	prepurl = propdirectory+"/"+propCollegeUrl;
		    }
		    if(propshortname != null || prepurl != null){
		    propMap.put(propshortname, prepurl);
		    }
		    
		}
			}
			if(jsonpopularMap.equals(propMap)){
				System.out.println("Json Popular MAP: " +jsonpopularMap);
				System.out.println("Properties MAP: " +propMap);
				Assert.assertTrue(true, "Popular List Verified Sucessfully");
			}
			else{
				for ( Map.Entry<String, String> entry : jsonpopularMap.entrySet() ) {
				    filewriter("JsonpopularMap: "+entry, "Logs//CollegePredictorMapLogs");
				}
				for ( Map.Entry<String, String> entry : propMap.entrySet() ) {
				    filewriter("propMap: "+entry, "Logs//CollegePredictorMapLogs");
				}
				Assert.assertTrue(false,"Popular List Verification Failed");
				
			}
			}
			else{
				filewriter("Popular List is empty", "Logs//CollegePredictorMapLogs");
			}
			
			if(!jsonPath.getList("data.otherPredictor").isEmpty()){
				List<String> name = jsonPath.getList("data.otherPredictor.name");
				List<String> url = jsonPath.getList("data.otherPredictor.url");
				Map<String, String> jsonotherMap= new HashMap<String,String>();
				Map<String, String> propMap = new HashMap<String, String>();
				for(int i=0;i<name.size();i++)
				{
					String predictorname = name.get(i);
					if(predictorname.contains(" ")){
						predictorname = predictorname.replace(" ", "-");
					}
					String predictorfullurl = url.get(i);
					String predictorUrl=predictorfullurl.substring(predictorfullurl.indexOf(".com")+4);
					if(jsonotherMap.containsValue(predictorUrl)){
						
					}else{
						jsonotherMap.put(predictorname.toUpperCase(), predictorUrl);
					}
				}	
				for ( String key : jsonotherMap.keySet() ) {
			
			
			Properties props = new Properties();
			props.load(new FileInputStream("collegePredictorConfig.properties"));
			for (Enumeration<?> e = props.propertyNames(); e.hasMoreElements(); ) {
			    String propname = (String)e.nextElement();
			    // now you have name and value
			    if(key.equalsIgnoreCase("JEE-MAIN")){
			    	key = "JEE-MAINS";
			    }
			    
			    String propshortname = null;
	    		String prepurl = null;
			    String propkey = null;
			    propkey = "collegeConfig.cpExams."+key;
			    if (propname.startsWith(propkey)) {
			    	 propshortname = props.getProperty(propkey+".shortName");
			    	String propCollegeUrl = props.getProperty(propkey+".collegeUrl");
			    	String propdirectory = props.getProperty(propkey+".directoryName");
			    	prepurl = propdirectory+"/"+propCollegeUrl;
			    }
			    if(propshortname != null || prepurl != null){
			    propMap.put(propshortname, prepurl);
			    }
			    
			}
				}
				if(jsonotherMap.equals(propMap)){
					System.out.println("Json Other MAP: " +jsonotherMap);
					System.out.println("Properties MAP: " +propMap);
					Assert.assertTrue(true, "Other List Verified Sucessfully");
				}
				else{
					for ( Map.Entry<String, String> entry : jsonotherMap.entrySet() ) {
					    filewriter("JsonotherMap: "+entry, "Logs//CollegePredictorMapLogs");
					}
					for ( Map.Entry<String, String> entry : propMap.entrySet() ) {
					    filewriter("propMap: "+entry, "Logs//CollegePredictorMapLogs");
					}
					Assert.assertTrue(false,"other List Verification Failed");
					
				}
				
			}
			else
				filewriter("Other List is Empty"+ jsonPath.getList("data.otherList"), "Logs//CollegePredictorMapLogs");
					}
			catch (Exception e) {
			e.printStackTrace();
			Assert.fail("Exception occured :" + e.getMessage());
		}
		}

	@Test(priority = 2)
	public static void respnseTimeStats_GetCollegePredictor() {
		respnseTimeStatsCommon(responseTimes, apiPath.substring(apiPath.lastIndexOf("/") + 1));
	}

}
