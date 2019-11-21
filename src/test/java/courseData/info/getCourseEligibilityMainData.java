package courseData.info;

import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import common.*;
import org.apache.commons.lang3.StringUtils;
import org.testng.*;
import org.testng.annotations.*;
import common.Common;

public class getCourseEligibilityMainData extends Common {
	static String apiPath;
	String api;
	static String[] params;
	Response apiResponse = null;
	String apiResponseMsgExpected;
	static String testDataFilePath;
	static ArrayList<Integer> responseTimes = new ArrayList<Integer>();
	boolean result;
	String file = "Logs//getCourseEligibilityMain";
	ArrayList<HashMap<String, String>> DBList = new ArrayList<HashMap<String, String>>();
	ArrayList<HashMap<String, String>> DBListforRankingLocation = new ArrayList<HashMap<String, String>>();
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
	public void verifyGetCourseEligibilityMaindataApi(String courseId, String cityId,
			String localityId, String apiResponseMsgExpected) {
		this.apiResponseMsgExpected = apiResponseMsgExpected;
		
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
			ResultSet rs = DatabaseCommon.getEligibilitymain(courseId);
			if(getCountOfResultSet(rs)>0){
				while(rs.next()){
					String DBYear = String.valueOf(rs.getInt("batch_year"));
					String DBworkMax = String.valueOf(rs.getInt("work-ex_max"));
					String DBworkMin = String.valueOf(rs.getInt("work-ex_min"));
					String DBageMin = String.valueOf(rs.getInt("age_min"));
					String DBageMax = String.valueOf(rs.getInt("age_max"));
					String DBinterStDesc = rs.getString("international_students_desc");
					String DBdiscription = rs.getString("description");
					String DBSubjects = rs.getString("subjects");
					List<String> dbSubject = new ArrayList<String>();
					if(DBSubjects!=null){
						DBSubjects = DBSubjects.replace("[", "");
						DBSubjects = DBSubjects.replace("]", "");
						DBSubjects = DBSubjects.replace("\"", "");
						String [] dBsubjects = DBSubjects.split(",");
						for(int i=0;i<dBsubjects.length;i++){
							dbSubject.add(dBsubjects[i]);
						}
						List<String> jsonSubjectList =new ArrayList<String>(jsonPath.getList(path+"twelthDetails.subjects"));
						if(dbSubject.equals(jsonSubjectList)){
							Reporter.log("Mandatory subject is appear correct for courseId: "+courseId, true);
						}
						else{
							Reporter.log("Mandatory subject is not appear correct for courseId: "+courseId, true);
							filewriter("Mandatory subject is not appear correct for courseId: "+courseId, file);
							filewriter("Actual result: "+jsonSubjectList, file);
							filewriter("Expected Result: "+dbSubject, file);
							Assert.assertTrue(false, "Mandatory Subject is not appear correct for courseId: "+courseId);
						}
					}
					else{
						if(jsonPath.getString(path+"twelthDetails.subjects")!=null){
							Reporter.log("Mandatory subject is not appear correct as appear null in db for courseId: "+courseId, true);
							filewriter("Mandatory subject is not appear correct as appear null in db for courseId: "+courseId, file);
							filewriter("Actual result: "+jsonPath.getList(path+"twelthDetails.subjects"), file);
							Assert.assertTrue(false,"Mandatory subject is not appear correct as appear null in db for courseId: "+courseId);
						}
						else{
							Reporter.log("Mandatory subject is appear correct for courseId: "+courseId, true);
						}
					}
					String jsonYear = jsonPath.getString(path+"year");
					if(DBYear.equals(jsonYear)){
						Reporter.log("Eligibility Year Appear correct for CourseId:"+courseId, true);
					}					
					else{
						Reporter.log("Eligibility Year not Appear correct for CourseId:"+courseId, true);
						filewriter("Eligibility Year not Appear correct for CourseId:"+courseId, file);
						filewriter("Actual Result: "+jsonYear, file);
						filewriter("Expected Result: "+DBYear, file);
						Assert.assertTrue(false,"Eligibility Year not Appear correct for CourseId:"+courseId);
					}
					
					String jsonMaxWork = jsonPath.getString(path+"maxWorkEx");
					if(jsonMaxWork ==null){
						jsonMaxWork="0";
					}
					if(StringUtils.equals(DBworkMax, jsonMaxWork)){
						Reporter.log("Eligibility work-max Appear correct for CourseId:"+courseId, true);
					}
					else{
						Reporter.log("Eligibility work-max not Appear correct for CourseId:"+courseId, true);
						filewriter("Eligibility work-max not Appear correct for CourseId:"+courseId, file);
						filewriter("Actual Result: "+jsonMaxWork, file);
						filewriter("Expected Result: "+DBworkMax, file);
						Assert.assertTrue(false,"Eligibility work-max not Appear correct for CourseId:"+courseId);
					}
					String jsonMinWork = jsonPath.getString(path+"minWorkEx");
					if(jsonMinWork ==null){
						jsonMinWork="0";
					}
					if(StringUtils.equals(DBworkMin, jsonMinWork)){
						Reporter.log("Eligibility work-min Appear correct for CourseId:"+courseId, true);
					}
					else{
						Reporter.log("Eligibility work-min not Appear correct for CourseId:"+courseId, true);
						filewriter("Eligibility work-min not Appear correct for CourseId:"+courseId, file);
						filewriter("Actual Result: "+jsonMinWork, file);
						filewriter("Expected Result: "+DBworkMin, file);
						Assert.assertTrue(false,"Eligibility work-min not Appear correct for CourseId:"+courseId);
					}
					
					String jsonMaxage = jsonPath.getString(path+"maxAge");
					if(jsonMaxage ==null){
						jsonMaxage="0";
					}
					if(StringUtils.equals(DBageMax,jsonMaxage)){
						Reporter.log("Eligibility maxAge Appear correct for CourseId:"+courseId, true);
					}
					else{
						Reporter.log("Eligibility maxAge not Appear correct for CourseId:"+courseId, true);
						filewriter("Eligibility maxAge not Appear correct for CourseId:"+courseId, file);
						filewriter("Actual Result: "+jsonMaxage, file);
						filewriter("Expected Result: "+DBageMax, file);
						Assert.assertTrue(false,"Eligibility maxAge not Appear correct for CourseId:"+courseId);
					}
					String jsonMinage = jsonPath.getString(path+"minAge");
					if(jsonMinage ==null){
						jsonMinage="0";
					}
					if(StringUtils.equals(DBageMin,jsonMinage)){
						Reporter.log("Eligibility minAge Appear correct for CourseId:"+courseId, true);
					}
					else{
						Reporter.log("Eligibility minAge not Appear correct for CourseId:"+courseId, true);
						filewriter("Eligibility minAge not Appear correct for CourseId:"+courseId, file);
						filewriter("Actual Result: "+jsonMinage, file);
						filewriter("Expected Result: "+DBageMin, file);
						Assert.assertTrue(false,"Eligibility minAge not Appear correct for CourseId:"+courseId);
					}
					
					if(StringUtils.equals(DBinterStDesc, jsonPath.getString(path+"internationalDescription"))){
						Reporter.log("Eligibility international Description Appear correct for CourseId:"+courseId, true);
					}
					else{
						Reporter.log("Eligibility international Description not Appear correct for CourseId:"+courseId, true);
						filewriter("Eligibility international Description not Appear correct for CourseId:"+courseId, file);
						filewriter("Actual Result: "+jsonPath.getString(path+"internationalDescription"), file);
						filewriter("Expected Result: "+DBinterStDesc, file);
						Assert.assertTrue(false,"Eligibility international Description not Appear correct for CourseId:"+courseId);
					}
					
					if(StringUtils.equals(DBdiscription, jsonPath.getString(path+"description"))){
						Reporter.log("Eligibility description Appear correct for CourseId:"+courseId, true);
					}
					else{
						Reporter.log("Eligibility description not Appear correct for CourseId:"+courseId, true);
						filewriter("Eligibility description not Appear correct for CourseId:"+courseId, file);
						filewriter("Actual Result: "+jsonPath.getString(path+"description"), file);
						filewriter("Expected Result: "+DBdiscription, file);
						Assert.assertTrue(false,"Eligibility description not Appear correct for CourseId:"+courseId);
					}
				}
			}
			else{
				if(jsonPath.getString("data.eligibility")!=null){
					Reporter.log("Eligibilty Main Details appears, Failed for CourseId:"+courseId, true);
					filewriter("Eligibilty Main Details appears, Failed for CourseId:"+courseId, file);
					filewriter("Eligibility details are:"+jsonPath.getList("data.eligibility"), file);
					Assert.assertTrue(false,"Eligibilty Main Details appears, Failed for CourseId:"+courseId);
				}
				else{
					Reporter.log("Eligibilty Main Details is appear null, passed successfully for courseId:"+courseId, true);
				}
			}
		}
		catch(SQLException e){
			e.printStackTrace();
		}
	}

	@Test(priority = 2)
	public static void respnseTimeStats_getCourseEligibilityMainData() {
		respnseTimeStatsCommon(responseTimes,
				apiPath.substring(apiPath.lastIndexOf("/") + 1));
	}
}
