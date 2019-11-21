package homepage.info;

import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

import org.testng.*;
import org.testng.annotations.*;

import common.Common;

public class getRHLData extends Common {
	static String apiPath;
	String api;
	static String[] params;
	Response apiResponse = null;
	String apiResponseMsgExpected;
	static String testDataFilePath;
	static ArrayList<Integer> responseTimes = new ArrayList<Integer>();
	int rowcountShortlist,rowcountInstituteDetails,locationid,cityid ,compareCourseId;
	String locationname,Cityname ;
	
	@BeforeClass
	public void doItBeforeTest() throws FileNotFoundException {
		// apiResponseExpectedTime = 200; // in MS
		loadPropertiesFromConfig("apifacade");
		// api path
		apiPath = "apigateway/commonapi/v1/info/getRHLData";
		api = serverPath + apiPath;
		// API params in sequence followed by apiResponseMsgExpected
		params = new String[] { "userId", "compareCourseIds", "apiResponseMsgExpected" };
		// test data file
		testDataFilePath = "//src//test//resources//homepage//info//GetRHLData.xlsx";

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
	public void verifyGetRHLdataApi(String userId, String compareCourseId,
			String apiResponseMsgExpected) {
		this.apiResponseMsgExpected = apiResponseMsgExpected;

		// pass api params and hit apicompareCourseId
		if (userId.equals("ignoreHeader")){
			return;
//			apiResponse = RestAssured.given().param("userId", userId).param("compareCourseIds[]",compareCourseId ).when().post(api).then().extract().response();
		}
		else if (userId.equals("ignoreInput")) {
			apiResponse = RestAssured.given()
					.header("AUTHREQUEST", "INFOEDGE_SHIKSHA").when().post(api).then().extract().response();
		} 
		else if(!userId.equals("ignoreInput")) 
		 {
			if(compareCourseId.equals("ignoreInput")){
				apiResponse = RestAssured.given()
						.header("AUTHREQUEST", "INFOEDGE_SHIKSHA")
						.param("userId", userId).when().post(api).then().extract()
						.response();
			}
			else{
			apiResponse = RestAssured.given()
					.header("AUTHREQUEST", "INFOEDGE_SHIKSHA")
					.param("userId", userId).and().param("compareCourseIds[]",compareCourseId ).when().post(api).then().extract()
					.response();
			}
		}

		// fetch status code from response
		int statusCode = apiResponse.getStatusCode();
		JsonPath jsonPath = apiResponse.jsonPath();
		if (statusCode == 200) { // Positive case
			Reporter.log("Positive case", true);
			verifyPostiveCases(userId, compareCourseId, jsonPath);
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

	public void verifyPostiveCases(String userId, String compareCourseIds, JsonPath jsonPath) {
		try {
			// verify status is success
			String statusResponse = jsonPath.getString("status");
			if (statusResponse.contentEquals("success")) {
				Reporter.log(", Correct status [success]", true);
			} else {
				Reporter.log(", InCorrect status [" + statusResponse + "]",
						true);
				Assert.fail("InCorrect status [" + statusResponse + "]");
			}

			// verify message is null
			String messageResponse = jsonPath.getString("message");
			if (messageResponse == null) {
				Reporter.log(", Correct message [null]", true);
			} else {
				Reporter.log(", InCorrect message [" + messageResponse + "]",
						true);
				Assert.fail("InCorrect message [" + messageResponse + "]");
			}
			int userid = Integer.parseInt(userId);
			ResultSet rsShortlistCount, rsInstituteDetails, rslocationname, rsCityname = null;
			if(userid!=0){
			 rsShortlistCount = exceuteDbQuery("select id from userShortlistedCourses where userId ="+userId+" and status = 'live';"
					, "shiksha");
			rowcountShortlist = getCountOfResultSet(rsShortlistCount);
			}
			else {
				rowcountShortlist =0;
			}
			// get row count of resultSet of sql query		
			if(!compareCourseIds.equals("ignoreInput")){
			String[] compareCourseArr = compareCourseIds.split(",");
			for (int i = 0; i < compareCourseArr.length; i++) {
			this.compareCourseId = Integer.parseInt(compareCourseArr[i]);
			try{
				if(jsonPath.getBoolean("data.compareCourseMap."+compareCourseId)){
			rsInstituteDetails = exceuteDbQuery("select sc.course_id, sc.name, si.listing_id, si.name as InstituteName, sil.city_id, sil.locality_id from  shiksha_institutes_locations as sil join shiksha_institutes as si on sil.listing_id = si.listing_id join shiksha_courses as sc on si.listing_id = sc.primary_id where sc.course_id = "+compareCourseId+" and sc.status = 'live' and si.status = 'live'and sil.status = 'live' and sil.is_main = 1;", 
					"shiksha");
			rowcountInstituteDetails = getCountOfResultSet(rsInstituteDetails);
			if(rowcountInstituteDetails>0){
				rsInstituteDetails.next();
			locationid = rsInstituteDetails.getInt("locality_id");
			cityid = rsInstituteDetails.getInt("city_id");
			}
		
			if(rowcountInstituteDetails != 0){
			if(locationid != 0){
				rslocationname = exceuteDbQuery("select localityName from localityCityMapping where localityId  ="+locationid+";", "shiksha");			
				rslocationname.next();
				locationname = rslocationname.getString("localityName");
			}
			else{
				locationname = "null";
			}
			
			rsCityname = exceuteDbQuery("select city_name from countryCityTable where city_id  ="+cityid+";", "shiksha");			
			rsCityname.next();
			Cityname = rsCityname.getString("city_name");
			}			
			int jsonShortlistcount = jsonPath.getInt("data.shortlistCoursesCount");
			if(rowcountShortlist ==jsonShortlistcount){
				Reporter.log("Shortlist Count is appear correct for user: "+userid, true);
			}
			else{
				Reporter.log("Shortlist Count is not appear correct for user: "+userid+" Expectec Count: "+rowcountShortlist +" Actual Count: "+ jsonShortlistcount, false);
				Assert.assertTrue(false);
			}
			
			int jsonCompareCollegeCourseId = Integer.parseInt(jsonPath.getString("data.compareCourseMap."+compareCourseId+".courseId"));
			int dbCompareCollegeCourseId = rsInstituteDetails.getInt("course_id");
			if(dbCompareCollegeCourseId==jsonCompareCollegeCourseId){
				Reporter.log("Course ID for Compare College is appear correct for CourseId: "+compareCourseId, true);
			}
			else{
				Reporter.log("Course ID for Compare College is not appear correct for CourseId"+compareCourseId+" Expected: "+dbCompareCollegeCourseId+" Actual: "+jsonCompareCollegeCourseId, false);
				Assert.assertTrue(false);
			}
			
			String jsonCompareCourseName = jsonPath.getString("data.compareCourseMap."+compareCourseId+".courseName");
			String dbCompareCourseName = rsInstituteDetails.getString("name");
			if(jsonCompareCourseName.equalsIgnoreCase(dbCompareCourseName)){
				Reporter.log("Course Name for Compare College is appear correct for CourseId: "+compareCourseId, true);
			}
			else{
				Reporter.log("Course Name for Compare College is not appear correct for CourseId"+compareCourseId+" Expected: "+dbCompareCourseName+" Actual: "+jsonCompareCourseName, false);
				Assert.assertTrue(false);
			}
			int jsonCourseInstituteId = Integer.parseInt(jsonPath.getString("data.compareCourseMap."+compareCourseId+".instituteId"));
			int dbCourseInstituteId = rsInstituteDetails.getInt("listing_id");
			if(jsonCourseInstituteId==dbCourseInstituteId){
				Reporter.log("Institute ID for Compare College is appear correct for CourseId: "+compareCourseId, true);
			}
			else{
				Reporter.log("Institute ID for Compare College is not appear correct for CourseId"+compareCourseId+" Expected: "+dbCourseInstituteId+" Actual: "+jsonCourseInstituteId, false);
				Assert.assertTrue(false);			
			}
			String jsonCourseInstituteName = jsonPath.getString("data.compareCourseMap."+compareCourseId+".instituteName");
			String dbCourseInstituteName = rsInstituteDetails.getString("InstituteName");
			if(jsonCourseInstituteName.equalsIgnoreCase(dbCourseInstituteName)){
				Reporter.log("Institute Name for Compare College is appear correct for CourseId: "+compareCourseId, true);
			}
			else{
				Reporter.log("Institute Name for Compare College is not appear correct for CourseId"+compareCourseId+" Expected: "+dbCourseInstituteName+" Actual: "+jsonCourseInstituteName, false);
				Assert.assertTrue(false);	
			}
			int jsonCourseCityId = jsonPath.getInt("data.compareCourseMap."+compareCourseId+".cityId");
			int dbCourseCityId = rsInstituteDetails.getInt("city_id");
			if(jsonCourseCityId == dbCourseCityId){
				Reporter.log("Institute City id is appear correct for Course: "+compareCourseId, true);
			}
			else{
				Reporter.log("Institute City id is appear correct for Course: "+compareCourseId +" Expected Result: "+dbCourseCityId +" Actual Result: "+jsonCourseCityId, false);
				Assert.assertTrue(false);
			}
			int jsonCourselocalityId = jsonPath.getInt("data.compareCourseMap."+compareCourseId+".localityId");
			int dbCourselocalityId = rsInstituteDetails.getInt("locality_id");
			if(jsonCourselocalityId == dbCourselocalityId){
				Reporter.log("Institute Locality id is appear correct for Course: "+compareCourseId, true);
			}
			else{
				Reporter.log("Institute Locality id is appear correct for Course: "+compareCourseId +" Expected Result: "+dbCourselocalityId +" Actual Result: "+jsonCourselocalityId, false);
				Assert.assertTrue(false);
			}
			
			String jsonCourseCityName = jsonPath.getString("data.compareCourseMap."+compareCourseId+".cityName");
			String dbCourseCityName = rsCityname.getString("city_name");
			if(jsonCourseCityName.equalsIgnoreCase(dbCourseCityName)){
				Reporter.log("City Name for Compare College is appear correct for CourseId: "+compareCourseId, true);
			}
			else{
				Reporter.log("City Name for Compare College is not appear correct for CourseId"+compareCourseId+" Expected: "+dbCourseCityName+" Actual: "+jsonCourseCityName, false);
				Assert.assertTrue(false);	
			}
			
			String jsonCourseLocalityName = jsonPath.getString("data.compareCourseMap."+compareCourseId+".localityName");
			String dbCourseLocalityName = rsCityname.getString("localityName");
			if(jsonCourseLocalityName.equalsIgnoreCase(dbCourseLocalityName)){
				Reporter.log("City Name for Compare College is appear correct for CourseId: "+compareCourseId, true);
			}
			else{
				Reporter.log("City Name for Compare College is not appear correct for CourseId"+compareCourseId+" Expected: "+dbCourseLocalityName+" Actual: "+jsonCourseLocalityName, false);
				Assert.assertTrue(false);	
			}
				}
			}
			catch(NullPointerException e){
				rsInstituteDetails = exceuteDbQuery("select sc.course_id, sc.name, si.listing_id, si.name as InstituteName, sil.city_id, sil.locality_id from  shiksha_institutes_locations as sil join shiksha_institutes as si on sil.listing_id = si.listing_id join shiksha_courses as sc on si.listing_id = sc.primary_id where sc.course_id = "+compareCourseId+" and sc.status = 'live' and si.status = 'live'and sil.status = 'live' and sil.is_main = 1;", 
						"shiksha");
				rowcountInstituteDetails = getCountOfResultSet(rsInstituteDetails);
				if(rowcountInstituteDetails ==0){
					Assert.assertTrue(true);
				}
				else{
					Reporter.log("Data appear for DB row count is: "+rowcountInstituteDetails+" for CourseId: "+compareCourseId);
					Assert.assertTrue(false);
				}
			}
			}
		}
			else{
				if(jsonPath.getMap("data.compareCourseMap").isEmpty()){
					Assert.assertTrue(true);
				}
				else{
					Assert.assertTrue(false,"Map Found Under CompareCOurseMap in json File"+jsonPath.getMap("data.compareCourseMap"));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail("SQLException occured :" + e.getMessage());
		}
	}

	 @Test(priority = 2)
	 public static void respnseTimeStats_GetRHLdata() {
	 respnseTimeStatsCommon(responseTimes,
	 apiPath.substring(apiPath.lastIndexOf("/") + 1));
	 }
}
