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

import com.relevantcodes.extentreports.*;
public class GetRankPredictorAPI extends Common {
	static String apiPath;
	String api;
	static String[] params;
	Response apiResponse = null;
	String apiResponseMsgExpected;
	static String testDataFilePath;
	String stream;
	ExtentReports report;
	ExtentTest parent,child1;
	static ArrayList<Integer> responseTimes = new ArrayList<Integer>();
	String path;
	@BeforeClass
	public void doItBeforeTest() throws IOException {
		// apiResponseExpectedTime = 200; // in MS
		loadPropertiesFromConfig("predictor");
		// api 
		apiPath = "predictor/api/v1/info/getRankPredictor";
		api = serverPath + apiPath;
		// API params in sequence followed by apiResponseMsgExpected
		params = new String[] { "rank", "apiResponseMsgExpected" };
		// test data file
		testDataFilePath = "//src//test//resources//predictor//info//GetRankPredictor.xlsx";
		PrintWriter pw = new PrintWriter("Logs//RankPredictorMapLogs.txt");
		pw.close();
		path = new java.io.File(".").getCanonicalPath();
		path = path+"\\src\\test\\resources\\predictor\\info\\"+"rankPredictorConfig.properties";
		 report =  createExtinctReport("GetRankPredictorAPI");
		 parent = createParent(report, "GetRankPredictorAPI","");	
	}
	
	@DataProvider
	public static String[][] testData() throws Exception {
		// The number of times data is repeated, test will be executed the same no. of
		// times
		String[][] testData = prepareTestData(params, testDataFilePath);
		return testData;
	}
	@Test(dataProvider = "testData", priority = 1)
	public void verifyGetRankPredictor(String rank, String apiResponseMsgExpected) {
		this.apiResponseMsgExpected = apiResponseMsgExpected;
		child1= createChild(report, rank, "");
		// pass api params and hit api
		if (rank.equals("ignoreHeader")){
			apiResponse = RestAssured.given().when().post(api).then().extract().response();	
		}
		else if (rank.equals("ignoreInput")){
			apiResponse = RestAssured.given().header("AUTHREQUEST", "INFOEDGE_SHIKSHA").when().post(api).then()
					.extract().response();
			}
		else {
			apiResponse = RestAssured.given().header("AUTHREQUEST", "INFOEDGE_SHIKSHA").when().post(api).then()
					.extract().response();
		}
		// fetch status code from response
		int statusCode = apiResponse.getStatusCode();
		JsonPath jsonPath = apiResponse.jsonPath();

		if (statusCode == 200) { // Positive case
			Reporter.log("Positive case", true);
			verifyPostiveCases(jsonPath, report, parent, child1);
		}
		else if (statusCode == 400) { // Negative case
			Reporter.log("Negative case", true);
			verifyNegativeCases(params, jsonPath, null, apiResponseMsgExpected, report, parent, child1);
		}else if (statusCode == 403) { // unauthorized request
			Reporter.log("Unauthorized request case", true);
			 verifyUnauthorizedRequestCase(jsonPath, report, parent, child1);
		} else {
			fail(child1,"InCorrect Response Code : " + statusCode);
			Reporter.log(", InCorrect Response Code : " + statusCode, true);
			Assert.fail("InCorrect Response Code : " + statusCode);
		}

		long apiResponseActualTime = apiResponse.getTimeIn(TimeUnit.MILLISECONDS);
		responseTimes.add((int) apiResponseActualTime);

		if (apiResponseActualTime > apiResponseExpectedTime) {
			 fail(child1,"API Response time : " + apiResponseActualTime + " MS is greater than expected time ("
						+ apiResponseExpectedTime + " MS)");
			Reporter.log("<br> API Response time : " + apiResponseActualTime + " MS is greater than expected time ("
					+ apiResponseExpectedTime + " MS)", true);
			Assert.fail("API Response time : " + apiResponseActualTime + " MS is greater than expected time ("
					+ apiResponseExpectedTime + " MS)");
		} else {
			pass(child1," API Response time : " + apiResponseActualTime + " MS");
			Reporter.log("<br> API Response time : " + apiResponseActualTime + " MS", true);
		}
	}
	public void verifyPostiveCases(JsonPath jsonPath, ExtentReports report, ExtentTest parent, ExtentTest child) {
		try {
			
			// verify status is success
			String statusResponse = jsonPath.getString("status");
			if (statusResponse.contentEquals("success")) {
				pass(child, "Correct status [success]");
				Reporter.log(", Correct status [success]", true);
			} else {
				fail(child,"InCorrect status [" + statusResponse + "]");
				Reporter.log(", InCorrect status [" + statusResponse + "]", true);
				Assert.fail("InCorrect status [" + statusResponse + "]");
			}

			// verify message is null
			String messageResponse = jsonPath.getString("message");
			if (messageResponse == null) {
				 pass(child, "Correct message [success]");
				Reporter.log(", Correct message [null]", true);
			} else {
				 fail(child,"InCorrect message [" + messageResponse + "]");
				Reporter.log(", InCorrect message [" + messageResponse + "]", true);
				Assert.fail("InCorrect message [" + messageResponse + "]");
			}
			//Fetch Data From Json
			if(!jsonPath.getList("data.popularPredictor").isEmpty()){
			List<String> name = jsonPath.getList("data.popularPredictor.name");
			List<String> url = jsonPath.getList("data.popularPredictor.url");
			Map<String, String> jsonpopularMap= new HashMap<String,String>();
			Map<String, String> propMap = new HashMap<String, String>();
			for(int i=0;i<name.size();i++)
			{
				String predictorname = name.get(i);
				String predictorfullurl = url.get(i);
				String predictorUrl=predictorfullurl.substring(predictorfullurl.indexOf(".com/")+5);
				if(jsonpopularMap.containsValue(predictorUrl)){
					
				}else{
					jsonpopularMap.put(predictorname, predictorUrl);
				}
			}	
			for ( String key : jsonpopularMap.keySet() ) {
				if(key.contains(" ")){
					key = key.replace(" ", "-");
				}
	
				//Fetch Data from properties File
		Properties props = new Properties();
		props.load(new FileInputStream(path));
		for (Enumeration<?> e = props.propertyNames(); e.hasMoreElements(); ) {
		    String propname = (String)e.nextElement();
		    key = key.toLowerCase();
		    // now you have name and value		    
		    String propshortname = null;
    		String prepurl = null;
		    String propkey = null;
		    propkey = "rankPredictorConfig.rpExams."+key;
		    if (propname.startsWith(propkey)) {
		    	 propshortname = props.getProperty(propkey+".name");
		    	String propCollegeUrl = props.getProperty(propkey+".url");
		    	prepurl = "b-tech/resources/"+propCollegeUrl;
		    }
		    if(propshortname != null || prepurl != null){
		    propMap.put(propshortname, prepurl);
		    }
		    
		}
			}
			//Compare 2 Map of Json Data Map and Properties file Map
			if(jsonpopularMap.equals(propMap)){
				pass(child, "Popular List Verified Sucessfully");
				Reporter.log("Json Popular MAP: " +jsonpopularMap,true);
				Reporter.log("Properties MAP: " +propMap,true);
				Assert.assertTrue(true, "Popular List Verified Sucessfully");
			}
			else{
				for ( Map.Entry<String, String> entry : jsonpopularMap.entrySet() ) {
					Reporter.log("JsonpopularMap: "+entry,true);
				    filewriter("JsonpopularMap: "+entry, "Logs//RankPredictorMapLogs");
				}
				for ( Map.Entry<String, String> entry : propMap.entrySet() ) {
				    filewriter("propMap: "+entry, "Logs//RankPredictorMapLogs");
				    Reporter.log("propMap: "+entry,true);
				}
				fail(child, "Popular List Verification Failed");
				Assert.assertTrue(false,"Popular List Verification Failed");
				
			}
			}
			else{
				pass(child,"Popular List is empty");
				Reporter.log("Popular List is empty",true);
				filewriter("Popular List is empty", "Logs//RankPredictorMapLogs");
			}
			//Other Json List
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
			props.load(new FileInputStream(path));
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
					Reporter.log("Json Other MAP: " +jsonotherMap,true);
					Reporter.log("Properties MAP: " +propMap,true);
					pass(child,"Other List Verified Sucessfully");
					Assert.assertTrue(true, "Other List Verified Sucessfully");
				}
				else{
					for ( Map.Entry<String, String> entry : jsonotherMap.entrySet() ) {
						Reporter.log("JsonotherMap: "+entry,true);
					    filewriter("JsonotherMap: "+entry, "Logs//RankPredictorMapLogs");
					}
					for ( Map.Entry<String, String> entry : propMap.entrySet() ) {
						Reporter.log("propMap: "+entry,true);
					    filewriter("propMap: "+entry, "Logs//RankPredictorMapLogs");
					}
					fail(child, "other List Verification Failed");
					Assert.assertTrue(false,"other List Verification Failed");
					
				}
				
			}
			else{
				pass(child, "Other List is empty");
				Reporter.log("Other List is Empty"+ jsonPath.getList("data.otherList"));
				filewriter("Other List is Empty", "Logs//RankPredictorMapLogs");
			}
			
					}
			catch (Exception e) {
				fail(child, "");
			e.printStackTrace();
			Assert.fail("Exception occured :" + e.getMessage());
		}
		finally{
			closeChild(parent, child, report);
		}
		}

	@Test(priority = 2)
	public static void respnseTimeStats_GetRankPredictor() {
		respnseTimeStatsCommon(responseTimes, apiPath.substring(apiPath.lastIndexOf("/") + 1));
	}
}
