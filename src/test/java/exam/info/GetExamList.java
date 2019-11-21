package exam.info;

import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.sql.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

import org.testng.*;
import org.testng.annotations.*;

import com.relevantcodes.extentreports.*;

import common.Common;

public class GetExamList extends Common {
	static String apiPath;
	String api;
	static String[] params;
	Response apiResponse = null;
	String apiResponseMsgExpected;
	static String testDataFilePath;
	String Exam;
	String query;
	ArrayList<String> dbList ;
	static ArrayList<Integer> responseTimes = new ArrayList<Integer>();
	List<String> jsonnamelist ;
	List<String> jsonurllist ;
	ExtentReports report;
	ExtentTest parent,child1;
	
	@BeforeClass
	public void doItBeforeTest() throws FileNotFoundException {
		// apiResponseExpectedTime = 200; // in MS
		
		loadPropertiesFromConfig("exam");
		// api path
		apiPath = "exam/api/v1/info/getExamList";
		api = serverPath + apiPath;
		// API params in sequence followed by apiResponseMsgExpected
		params = new String[] { "exam", "apiResponseMsgExpected" };
		// test data file
		testDataFilePath = "//src//test//resources//exam//info//GetExamList.xlsx";
		PrintWriter pw = new PrintWriter("Logs//GetExamList.txt");
		pw.close();
		report =  createExtinctReport("GetExamList");
		 parent = createParent(report, "GetExamList","");	
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
	public void verifyGetExamListApi(String Exam,
			String apiResponseMsgExpected) {
		this.apiResponseMsgExpected = apiResponseMsgExpected;
		child1 = createChild(report, Exam, "");
		// pass api params and hit api
		if (Exam.equals("ignoreInput")) {
			apiResponse = RestAssured.given()
					.header("AUTHREQUEST", "INFOEDGE_SHIKSHA").when().post(api)
					.then().extract().response();
			query = "select distinct(epm.id), epm.name,epm.url from exampage_main as epm"
					+ " join exampage_master as epmas on epm.id = epmas.exam_id "
					+ "join examAttributeMapping as eam on epmas.exam_id = eam.examId"
					+ " join base_courses as bc on eam.entityId = bc.base_course_id "
					+ "where bc.alias = 'MBA' and eam.entityType ='course' and bc.status = 'live' and"
					+ " eam.status = 'live' and epm.status = 'live' and epmas.status = 'live';";
		} else if (Exam.equals("mba")) {
			apiResponse = RestAssured.given()
					.header("AUTHREQUEST", "INFOEDGE_SHIKSHA")
					.param("category", Exam).when().post(api).then().extract()
					.response();
			query = "select distinct(epm.id), epm.name,epm.url from exampage_main as epm"
					+ " join exampage_master as epmas on epm.id = epmas.exam_id "
					+ "join examAttributeMapping as eam on epmas.exam_id = eam.examId"
					+ " join base_courses as bc on eam.entityId = bc.base_course_id "
					+ "where bc.alias = 'MBA' and eam.entityType ='course' and bc.status = 'live' and"
					+ " eam.status = 'live' and epm.status = 'live' and epmas.status = 'live';";
		} else if (Exam.equalsIgnoreCase("engineering")) {
			apiResponse = RestAssured.given()
					.header("AUTHREQUEST", "INFOEDGE_SHIKSHA")
					.param("category", Exam).when().post(api).then().extract()
					.response();
			query = "select distinct(epm.id), epm.name,epm.url from exampage_main as epm"
					+ " join exampage_master as epmas on epm.id = epmas.exam_id"
					+ " join examAttributeMapping as eam on epmas.groupId = eam.groupId"
					+ " join base_courses as bc on eam.entityId = bc.base_course_id"
					+ " where bc.alias = 'B.Tech' and eam.entityType ='course' and bc.status = 'live' and"
					+ " eam.status = 'live' and epm.status = 'live' and epmas.status = 'live';";
		} 
		else 
		{
			if (Exam.equals("ignoreHeader")){
				return;
//				apiResponse = RestAssured.given().when().post(api).then().extract().response();
			}
			else {
			apiResponse = RestAssured.given()
					.header("AUTHREQUEST", "INFOEDGE_SHIKSHA")
					.param("category", Exam).when().post(api).then().extract()
					.response();
			query = "select distinct(epm.id), epm.name,epm.url from exampage_main as epm "
					+ "join exampage_master as epmas on epm.id = epmas.exam_id "
					+ "join examAttributeMapping as eam on epmas.groupId = eam.groupId join base_hierarchies as bh on eam.entityId = bh.hierarchy_id "
					+ "join streams as st on bh.stream_id = st.stream_id "
					+ "where st.name = '"
					+ Exam
					+ "' and eam.entityType ='primaryHierarchy' and "
					+ "st.status = 'live' and eam.status = 'live' and epm.status = 'live' and epmas.status = 'live' and eam.groupId!=0;";
			}
		}

		// fetch status code from response
		int statusCode = apiResponse.getStatusCode();
		JsonPath jsonPath = apiResponse.jsonPath();

		if (statusCode == 200) { // Positive case
			Reporter.log("Positive case", true);
			verifyPostiveCases(query, Exam, jsonPath, report, parent, child1);
		} else if (statusCode == 400) { // Negative case
			Reporter.log("Negative case", true);
			verifyNegativeCases(params, jsonPath, null, apiResponseMsgExpected, report, parent, child1);
		} else if (statusCode == 403) { // unauthorized request
			Reporter.log("Unauthorized request case", true);
			 verifyUnauthorizedRequestCase(jsonPath, report, parent, child1);
		} else {
			fail(child1,"InCorrect Response Code : " + statusCode);
			Reporter.log(", InCorrect Response Code : " + statusCode, true);
			Assert.fail("InCorrect Response Code : " + statusCode);
		}

		long apiResponseActualTime = apiResponse
				.getTimeIn(TimeUnit.MILLISECONDS);
		responseTimes.add((int) apiResponseActualTime);

		if (apiResponseActualTime > apiResponseExpectedTime) {
			 fail(child1,"API Response time : " + apiResponseActualTime + " MS is greater than expected time ("
						+ apiResponseExpectedTime + " MS)");
			Reporter.log("<br> API Response time : " + apiResponseActualTime
					+ " MS is greater than expected time ("
					+ apiResponseExpectedTime + " MS)", true);
			Assert.fail("API Response time : " + apiResponseActualTime
					+ " MS is greater than expected time ("
					+ apiResponseExpectedTime + " MS)");
		} else {
			pass(child1," API Response time : " + apiResponseActualTime + " MS");
			Reporter.log("<br> API Response time : " + apiResponseActualTime
					+ " MS", true);
		}
	}

	public void verifyPostiveCases(String query, String Exam, JsonPath jsonPath, ExtentReports report, ExtentTest parent, ExtentTest child) {
		try {
			// verify status is success
			String statusResponse = jsonPath.getString("status");
			if (statusResponse.contentEquals("success")) {
				pass(child, "Correct status [success]");
				Reporter.log(", Correct status [success]", true);
			} else {
				 fail(child,"InCorrect status [" + statusResponse + "]");
				Reporter.log(", InCorrect status [" + statusResponse + "]",
						true);
				Assert.fail("InCorrect status [" + statusResponse + "]");
			}

			// verify message is null
			String messageResponse = jsonPath.getString("message");
			if (messageResponse == null) {pass(child,"Correct message [null]");
				Reporter.log(", Correct message [null]", true);
			} else {fail(child,"InCorrect message [" + messageResponse + "]");
				Reporter.log(", InCorrect message [" + messageResponse + "]",
						true);
				Assert.fail("InCorrect message [" + messageResponse + "]");
			}

			
			
			ResultSet rs = exceuteDbQuery(query, "shiksha");
			// get row count of resultSet of sql query
			int rowcount = getCountOfResultSet(rs);
			dbList = new ArrayList<String>();
			if (rowcount > 0) {
				ArrayList<HashMap<String, String>> popularjsonArray = new ArrayList<HashMap<String, String>>(
						jsonPath.getList("data.popularExams"));
				ArrayList<HashMap<String, String>> otherjsonArray = new ArrayList<HashMap<String, String>>(
						jsonPath.getList("data.otherExams"));
				ArrayList<HashMap<String, String>> jsonArray = new ArrayList<HashMap<String, String>>();
				jsonArray.addAll(popularjsonArray);
				jsonArray.addAll(otherjsonArray);
				dbList.clear();
				while (rs.next()) {
	

					String name = null;


					jsonnamelist = new ArrayList<String>();
					jsonurllist = new ArrayList<String>();

					name = rs.getString("name");

					dbList.add(name);
				}
				for(int i=0;i<jsonArray.size();i++){
					String s = jsonArray.get(i).get("url");
					s= s.substring(s.indexOf(".com/")+5);
					if(s==""){
						fail(child,"String Appear to be null as no url passed from DB for Exam");
						Assert.assertTrue(false,"String Appear to be null as no url passed from DB for Exam"+ jsonArray.get(i).get("name"));
					filewriter("String Appear to be null as no url passed from DB for Exam :"+ jsonArray.get(i).get("name"), "GetExamList");
					}
				}
				
				
				for(int i=0;i<jsonArray.size();i++){
					String s = jsonArray.get(i).get("name");
					jsonnamelist.add(s);
				}
				
				Collections.sort(jsonnamelist);
				Collections.sort(dbList);
				if (jsonnamelist.equals(dbList)) {
					pass(child,"Get Exam List Pass Sucessfully");
					Reporter.log("Get Exam List Passed Sucessfully", true);
				} else {
					fail(child,"Test Execution Failed for getExamList");
					filewriter("for Exam: "+Exam+" Json Array: " + jsonnamelist, "GetExamList");
					filewriter("for Exam: "+Exam+" DB Value Array: " + dbList , "GetExamList");
					Reporter.log("Test Case Execute Failed for Exam: " + Exam, true);
					Assert.assertTrue(false, "Test Execution Failed for exam: "
							+ Exam);
				}

			
			}else if (jsonPath.getString("data.popularExams")==null
					&& jsonPath.getString("data.otherExams")==null) {
				pass(child,"No Result Available for the same in GetExamList");
				Reporter.log("No Result Available for the same: " + Exam, true);
				Assert.assertTrue(true, "No Result Available for the same: "
						+ Exam);
			} else {
				fail(child,"Test Case Execute Failed for GetExamList" );
				filewriter("for Exam: "+Exam+" Json Array: " + jsonnamelist, "GetExamList");
				filewriter("for Exam: "+Exam+" DB Value Array: " + dbList, "GetExamList");
				Reporter.log("Test Case Execute Failed for Exam: " + Exam, true);
				Assert.assertTrue(false,
						"Test Failed due to Sql return 0 Result: " + Exam);
			}
			
		} catch (Exception e) {
			fail(child, "");
//			e.printStackTrace();
			Assert.fail("SQLException occured :" + e.getMessage());
		}
		finally{
		closeChild(parent, child, report);}
	}

	 @Test(priority = 2)
	 public static void respnseTimeStats_GetExamList() {
	 respnseTimeStatsCommon(responseTimes,
	 apiPath.substring(apiPath.lastIndexOf("/") + 1));
	 }
}
