package courseData.info;



import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.testng.Assert;
import org.testng.Reporter;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import common.Common;
import common.DatabaseCommon;

public class getCourseDateSeatsStateWise extends Common {
	static String apiPath;
	String api;
	static String[] params;
	Response apiResponse = null;
	String apiResponseMsgExpected;
	static String testDataFilePath;
	static ArrayList<Integer> responseTimes = new ArrayList<Integer>();
	boolean result;
	String file = "Logs//getCourseDateSeatsStateWise";
	HashMap<String, HashMap<String, String>> DBList = new HashMap<String, HashMap<String, String>>();
	HashMap<String, String> innerMap;
	String sqlQuery = null;
	String ExpectedActualListingId = null;
	String path = "data.eligibility.";
	@BeforeClass
	public void doItBeforeTest() throws FileNotFoundException {
		// apiResponseExpectedTime = 200; // in MS
		loadPropertiesFromConfig("apifacade");
		// api path
		apiPath = "apigateway/courseapi/v1/info/getCourseData";
		api = serverPath + apiPath;
		// API params in sequence followed by apiResponseMsgExpected
		params = new String[] { "courseId", "cityId", "localityId",
				"apiResponseMsgExpected"};
		// test data file
		testDataFilePath = "//src//test//resources//courseData//info//GetCourseData.xlsx";
		PrintWriter pw = new PrintWriter(file + ".txt");
		pw.close();

	}

	@DataProvider
	public static String[][] testData() throws Exception {
		// The number of times data is repeated, test will be executed the same
		// no. of
		// times
		String[][] testData = prepareTestData(params, testDataFilePath);
		return testData;
	}

	@Test(dataProvider = "testData", priority = 1)
	public void verifyGetCoursedataSeatsStateWiseApi(String courseId, String cityId,
			String localityId, String apiResponseMsgExpected) {
		this.apiResponseMsgExpected = apiResponseMsgExpected;
//		courseId = "294724";
		// pass api params and hit apicompareCourseId
		if (courseId.equals("ignoreHeader")) {
			return;
			// apiResponse = RestAssured.given().param("userId",
			// userId).param("compareCourseIds[]",compareCourseId
			// ).when().post(api).then().extract().response();
		} else if (courseId.equals("ignoreInput")) {
			apiResponse = RestAssured.given()
					.header("AUTHREQUEST", "INFOEDGE_SHIKSHA").when().get(api)
					.then().extract().response();
		} else if (!courseId.equals("ignoreInput")) {
			if (!cityId.equals("ignoreInput")) {
				if (localityId.equals("ignoreInput")) {
					apiResponse = RestAssured.given()
							.header("AUTHREQUEST", "INFOEDGE_SHIKSHA")
							.when().get(api+"?courseId="+courseId+"&cityId="+cityId).then()
							.extract().response();
				} else {
					apiResponse = RestAssured.given()
							.header("AUTHREQUEST", "INFOEDGE_SHIKSHA")
							.when().get(api+"?courseId="+courseId+"&cityId="+cityId+"&localityId="+localityId)
							.then().extract().response();
				}
			} else {
				if (localityId.equals("ignoreInput")) {
					apiResponse = RestAssured.given()
							.header("AUTHREQUEST", "INFOEDGE_SHIKSHA")
							.when().get(api+"?courseId="+courseId)
							.then().extract().response();
				} else {
					apiResponse = RestAssured.given()
							.header("AUTHREQUEST", "INFOEDGE_SHIKSHA")
							.when().get(api+"?courseId="+courseId+"&localityId="+localityId)
							.then().extract().response();
				}
			}
		}

		// fetch status code from response
		int statusCode = apiResponse.getStatusCode();
		JsonPath jsonPath = apiResponse.jsonPath();
		if (statusCode == 200) { // Positive case
			Reporter.log("Positive case", true);
			verifyPostiveCases(courseId, cityId, localityId, jsonPath);
		} else if (statusCode == 400) { // Negative case
			Reporter.log("Negative case", true);
			verifyNegativeCases(params, jsonPath, null, apiResponseMsgExpected);
		} else if (statusCode == 403) { // unauthorized request
			Reporter.log("Unauthorized request case", true);
			verifyUnauthorizedRequestCase(jsonPath);
		} else {
			Reporter.log(", InCorrect Response Code : " + statusCode, true);
			Assert.fail("InCorrect Response Code : " + statusCode);
		}

		long apiResponseActualTime = apiResponse
				.getTimeIn(TimeUnit.MILLISECONDS);
		responseTimes.add((int) apiResponseActualTime);
		if (apiResponseActualTime > apiResponseExpectedTime) {
			Reporter.log("<br> API Response time : " + apiResponseActualTime
					+ " MS is greater than expected time ("
					+ apiResponseExpectedTime + " MS)", true);
			Assert.fail("API Response time : " + apiResponseActualTime
					+ " MS is greater than expected time ("
					+ apiResponseExpectedTime + " MS)");
		} else {
			Reporter.log("<br> API Response time : " + apiResponseActualTime
					+ " MS", true);
		}
	}

	public void verifyPostiveCases(String courseId, String cityId,
			String localityId, JsonPath jsonPath) {
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
		try{
			ResultSet rs = DatabaseCommon.getSeatsDateStatewise(courseId);
			if(getCountOfResultSet(rs)>0){
				while(rs.next()){
					innerMap = new HashMap<String, String>();
					String cat = rs.getString("category");
					innerMap.put("category", cat);
					innerMap.put("seats", rs.getString("seats"));
					DBList.put(cat, innerMap);
				}
				List<HashMap<String, String>> jsonList = new ArrayList<HashMap<String,String>>(jsonPath.getList("data.seatsData.domicileWiseSeats"));
				for(int i=0; i<jsonList.size();i++){
					HashMap<String, String> jsontempMap = jsonList.get(i);
					for(Map.Entry<String, String> entrySet: jsontempMap.entrySet()){
						String catName = jsontempMap.get("category");
						HashMap<String, String> dbTempMap = DBList.get(catName.toLowerCase().replace(" ", "_"));
						
						String jsonkey = entrySet.getKey();
						String jsonValue = String.valueOf(entrySet.getValue());
						String dbValue = dbTempMap.get(jsonkey);
						if(dbValue.contains("_")){
							dbValue=dbValue.replace("_"," ");
						}
						if(jsonValue.equalsIgnoreCase(dbValue)){
							Reporter.log("Seat data statewise is appear correct for courseId: "+courseId+ " for Category: "+catName, true);
						}
						else{
							Reporter.log("Seat data statewise is not appear correct for courseId: "+courseId+" keyName: "+jsonkey, true);
							filewriter("Seat data statewise is not appear correct for courseId: "+courseId+" keyName: "+jsonkey, file);
							filewriter("jsonValue: "+jsonValue+" dbValue: "+dbValue, file);
							Assert.assertTrue(false, "for CourseId: "+courseId +" key: "+jsonkey+" jsonValue: "+jsonValue+" dbValue: "+dbValue);
						}
					} 
				}
				ResultSet rsRelatestate = DatabaseCommon.getRelatestatesInSeats(courseId);
				if(getCountOfResultSet(rsRelatestate)>0){
					while(rsRelatestate.next()){
					String relatedStateId = rsRelatestate.getString("related_state_list");
					if(relatedStateId!=null){
					String [] arrRelatedStateId = relatedStateId.split(",");
					List<String> relatedStateList = new ArrayList<>();
					for(int i=0;i<arrRelatedStateId.length;i++){
						relatedStateList.add(DatabaseCommon.getStateName(arrRelatedStateId[i]));
					}
					String jsonRelateState = jsonPath.getString("data.seatsData.relatedStates");
					if(jsonRelateState.contains("States under this quota: ")){
						jsonRelateState = jsonRelateState.replace("States under this quota: ", "");
					}
					List<String> jsonRelateStateList = new ArrayList<>();
					String [] arrjsonRelateState = jsonRelateState.split(", ");
					for(int i=0;i<arrjsonRelateState.length;i++){
						jsonRelateStateList.add(arrjsonRelateState[i]);
					}
					if(jsonRelateStateList.equals(relatedStateList)){
						Reporter.log("Related State in Seats is appear correct for courseId: "+courseId, true);
					}
					else{
						Reporter.log("Related State in Seats is not appear correct for courseId: "+courseId, true);
						filewriter("Related State in Seats is not appear correct for courseId: "+courseId, file);
						filewriter("Actual Value: "+jsonRelateStateList, file);
						filewriter("Expected: "+relatedStateList, file);
						Assert.assertTrue(false, "Related State in Seats is not appear correct for courseId: "+courseId);
					}
					}
					else{
						if(jsonPath.getString("data.seatsData.relatedStates")==null){
							Reporter.log("Seat data statewise appear to be null when result count is 0, so Passed for courseId: "+courseId, true);
						}
						else{
								Reporter.log("Seat data statewise is not appear null when result count is 0, so failed for courseId: "+courseId, true);
								filewriter("Seat data statewise is not appear null when result count is 0, so failed for courseId: "+courseId, file);
								filewriter("Seat Data statewise wise: "+jsonPath.getList("data.seatsData.relatedStates"), file);
								Assert.assertTrue(false, "Seat data statewise is not appear null when result count is 0, so failed for courseId: "+courseId);
							
					}
					}
					}
				}
				else{
					if(jsonPath.getString("data.seatsData.relatedStates")==null){
						Reporter.log("Related State in Seats is appear correct for courseId: "+courseId, true);
					}
					else{
						Reporter.log("Related State in Seats is not appear correct, DB return count is 0 for courseId: "+courseId, true);
						filewriter("Related State in Seats is not appear correct, DB return count is 0 for courseId: "+courseId, file);
						filewriter("Actual Value: "+jsonPath.getString("data.seatsData.relatedStates"), file);
						Assert.assertTrue(false, "Related State in Seats is not appear correct, DB return count is 0 for courseId: "+courseId);
					}
				}
			}
			else{
				if(jsonPath.getString("data.seatsData.domicileWiseSeats")==null){
					Reporter.log("Seat data statewise appear to be null when result count is 0, so Passed for courseId: "+courseId, true);
				}
				else{
					if(jsonPath.getList("data.seatsData.domicileWiseSeats").size()==0){
						Reporter.log("Seat data statewise appear to be null when result count is 0, so Passed for courseId: "+courseId, true);
					}
					else{
						Reporter.log("Seat data statewise is not appear null when result count is 0, so failed for courseId: "+courseId, true);
						filewriter("Seat data statewise is not appear null when result count is 0, so failed for courseId: "+courseId, file);
						filewriter("Seat Data statewise wise: "+jsonPath.getList("data.seatsData.domicileWiseSeats"), file);
						Assert.assertTrue(false, "Seat data statewise is not appear null when result count is 0, so failed for courseId: "+courseId);
					}
				}
			}
		}
		catch(SQLException e){
			e.printStackTrace();
		}
	}

	@Test(priority = 2)
	public static void respnseTimeStats_getCourseDataSeatsStateWise() {
		respnseTimeStatsCommon(responseTimes,
				apiPath.substring(apiPath.lastIndexOf("/") + 1));
	}
}
