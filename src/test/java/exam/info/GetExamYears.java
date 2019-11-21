package exam.info;

import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.testng.*;
import org.testng.annotations.*;

import com.relevantcodes.extentreports.*;

import common.Common;

public class GetExamYears extends Common {

	static String apiPath;
	String api;
	static String[] params;
	Response apiResponse = null;
	String apiResponseMsgExpected;
	static String testDataFilePath;
	int exam;
	Map<String, Integer> dbMap = new HashMap<String, Integer>();
	ExtentReports report;
	ExtentTest parent,child1;
	static ArrayList<Integer> responseTimes = new ArrayList<Integer>();

	@BeforeClass
	public void doItBeforeTest() throws FileNotFoundException {
		// apiResponseExpectedTime = 200; // in MS
		loadPropertiesFromConfig("exam");
		// api path
		apiPath = "exam/api/v1/info/getExamYears";
		api = serverPath + apiPath;
		// API params in sequence followed by apiResponseMsgExpected
		params = new String[] { "exam", "apiResponseMsgExpected" };
		// test data file
		testDataFilePath = "//src//test//resources//exam//info//getExamyear.xlsx";
		PrintWriter pw = new PrintWriter("Logs//GetExamyear.txt");
		pw.close();
		report =  createExtinctReport("GetExamYears");
		 parent = createParent(report, "GetExamYears","");	
	}

	@DataProvider
	public static String[][] testData() throws Exception {
		// The number of times data is repeated, test will be executed the same no. of
		// times
		String[][] testData = prepareTestData(params, testDataFilePath);
		return testData;
	}

	@Test(dataProvider = "testData", priority = 1)
	public void verifyGetExamYearApi(String streamIds, String apiResponseMsgExpected) {
		this.apiResponseMsgExpected = apiResponseMsgExpected;
		child1=createChild(report, streamIds, "");
		// pass api params and hit api
		if (streamIds.equals("ignoreHeader"))
			apiResponse = RestAssured.given().when().get(api).then().extract().response();
		else if (streamIds.equals("ignoreInput"))
			apiResponse = RestAssured.given().header("AUTHREQUEST", "INFOEDGE_SHIKSHA").when().get(api).then()
					.extract().response();

		// fetch status code from response
		int statusCode = apiResponse.getStatusCode();
		JsonPath jsonPath = apiResponse.jsonPath();

		if (statusCode == 200) { // Positive case
			Reporter.log("Positive case", true);
			verifyPostiveCases(jsonPath, report, parent, child1);
		}  else if (statusCode == 400) { // unauthorized request
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

	public void verifyPostiveCases(JsonPath jsonPath,ExtentReports report, ExtentTest parent, ExtentTest child) {
		try {
			// verify status is success
			String statusResponse = jsonPath.getString("status");
			if (statusResponse.contentEquals("success")) { pass(child, "Correct status [success]");
				Reporter.log(", Correct status [success]", true);
			} else { fail(child,"InCorrect status [" + statusResponse + "]");
				Reporter.log(", InCorrect status [" + statusResponse + "]", true);
				Assert.fail("InCorrect status [" + statusResponse + "]");
			}

			// verify message is null
			String messageResponse = jsonPath.getString("message");
			if (messageResponse == null) {pass(child,"Correct message [null]");
				Reporter.log(", Correct message [null]", true);
			} else { fail(child,"InCorrect message [" + messageResponse + "]");
				Reporter.log(", InCorrect message [" + messageResponse + "]", true);
				Assert.fail("InCorrect message [" + messageResponse + "]");
			}

			// fetch all stream details from db
			ResultSet rs = exceuteDbQuery(
					"select epm.name, eam.entityId from exampage_groups as epg join examAttributeMapping as eam on eam.groupId = epg.groupId join exampage_main as epm on epm.id = epg.examId where epg.isPrimary = 1 and epg.status = 'live' and eam.status = 'live' and eam.entityType = 'year' and epm.status = 'live';",
					"shiksha");
			Map<String, Integer> jsonMap1 = new HashMap<String, Integer>(jsonPath.getMap("data"));
			Map<String, Integer> jsonMap = new HashMap<String, Integer>(jsonMap1.size());
			for (Map.Entry<String, Integer> entry : jsonMap1.entrySet()) {
				jsonMap.put(entry.getKey().toLowerCase(), entry.getValue());
				}
			// get row count of resultSet of sql query
			int rowcount = getCountOfResultSet(rs);

			if (rowcount > 0) {
				while (rs.next()) {
					dbMap.put(rs.getString("name").toLowerCase(), rs.getInt("entityId"));
				}
				if(jsonMap.equals(dbMap)){
					pass(child,"Api Tested Sucessfully and Result is passed");
					Reporter.log("Api Tested Sucessfully and Result is passed");
					Assert.assertTrue(true);
				}
				else{
					fail(child, "Both Map are not matched and similar so failed");
					Reporter.log("Both Map are not matched and similar so failed");
					filewriter("Failed:"+"JsonMap: "+jsonMap, "Logs//GetExamyear");
					filewriter("Failed:"+"DatabaseMap: "+dbMap, "Logs//GetExamyear");
					Assert.assertTrue(false,"Both map values are different");
				}
			} else {
				// if no details fetched from db
				fail(child, "");
				Assert.fail("Unable to fetch data from db");
			}
		} catch (Exception e) {
			fail(child, "");
			e.printStackTrace();
			Assert.fail("SQLException occured :" + e.getMessage());
		}
		finally{
			closeChild(parent, child, report);
		}
	}

	@Test(priority = 2)
	public static void respnseTimeStats_GetExamyear() {
		respnseTimeStatsCommon(responseTimes, apiPath.substring(apiPath.lastIndexOf("/") + 1));
	}
}
