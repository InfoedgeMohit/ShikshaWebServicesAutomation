package baseentities.info;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import org.testng.Assert;
import org.testng.Reporter;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import common.Common;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;

public class GetAllSubStreams extends Common {

	static String apiPath;
	String api;
	static String[] params;
	Response apiResponse = null;
	String apiResponseMsgExpected;
	static String testDataFilePath;
	int subStreamId;

	static ArrayList<Integer> responseTimes = new ArrayList<Integer>();

	@BeforeClass
	public void doItBeforeTest() {
		// apiResponseExpectedTime = 200; // in MS
		loadPropertiesFromConfig("baseentities");
		// api path
		apiPath = "baseentities/api/v1/info/getAllSubstreams";
		api = serverPath + apiPath;
		// API params in sequence followed by apiResponseMsgExpected
		params = new String[] { "substreamIds", "apiResponseMsgExpected" };
		// test data file
		testDataFilePath = "//src//test//resources//baseentities//info//GetAllSubstreams.xlsx";

	}

	@DataProvider
	public static String[][] testData() throws Exception {
		// The number of times data is repeated, test will be executed the same no. of
		// times
		String[][] testData = prepareTestData(params, testDataFilePath);
		return testData;
	}

	@Test(dataProvider = "testData", priority = 1)
	public void verifyGetAllSubStreamsApi(String substreamIds, String apiResponseMsgExpected) {
		this.apiResponseMsgExpected = apiResponseMsgExpected;

		// pass api params and hit api
		if (substreamIds.equals("ignoreHeader"))
			return;
//			apiResponse = RestAssured.given().when().post(api).then().extract().response();
		else if (substreamIds.equals("ignoreInput"))
			apiResponse = RestAssured.given().header("AUTHREQUEST", "INFOEDGE_SHIKSHA").when().post(api).then()
					.extract().response();

		// fetch status code from response
		int statusCode = apiResponse.getStatusCode();
		JsonPath jsonPath = apiResponse.jsonPath();

		if (statusCode == 200) { // Positive case
			Reporter.log("Positive case", true);
			verifyPostiveCases(jsonPath);
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

	public void verifyPostiveCases(JsonPath jsonPath) {
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

			// fetch all substream details from db
			ResultSet rs = exceuteDbQuery(
					"SELECT substream_id, name, alias, synonym, primary_stream_id, display_order FROM substreams where status = 'live';",
					"shiksha");

			// get row count of resultSet of sql query
			int rowcount = getCountOfResultSet(rs);

			if (rowcount > 0) {
				while (rs.next()) {
					// verify substream_id in data[response] as same in db
					this.subStreamId = rs.getInt("substream_id");
					int subStreamIdOfSingleSubStreamInDataResponse = jsonPath
							.getInt("data." + subStreamId + ".substreamId");
					if (subStreamIdOfSingleSubStreamInDataResponse == subStreamId) {
						Reporter.log(", Correct value of subStreamId [" + subStreamIdOfSingleSubStreamInDataResponse
								+ "] for subStreamId : " + subStreamId + "", true);
					} else {
						Reporter.log(", InCorrect value of subStreamId for subStreamId: " + subStreamId + " ["
								+ subStreamIdOfSingleSubStreamInDataResponse + "]", true);
						Assert.fail("InCorrect value of subStreamId for subStreamId : " + subStreamId + " ["
								+ subStreamIdOfSingleSubStreamInDataResponse + "]");
					}

					// verify name in data[response] as same in db
					String subStreamNameDb = rs.getString("name");
					String nameOfSingleSubStreamInDataResponse = jsonPath.getString("data." + subStreamId + ".name");
					if (nameOfSingleSubStreamInDataResponse.equalsIgnoreCase(subStreamNameDb)) {
						Reporter.log(", Correct name [" + nameOfSingleSubStreamInDataResponse + "] of subStreamId : "
								+ subStreamId + "", true);
					} else {
						Reporter.log(", InCorrect name of subStreamId: " + subStreamId + " ["
								+ nameOfSingleSubStreamInDataResponse + "]", true);
						Assert.fail("InCorrect name of subStreamId : " + subStreamId + " ["
								+ nameOfSingleSubStreamInDataResponse + "]");
					}

					// verify alias in data[response] as same in db
					String aliasDb = rs.getString("alias");
					if (aliasDb.equals(""))
						aliasDb = null;
					String aliasOfSingleSubStreamInDataResponse = jsonPath.getString("data." + subStreamId + ".alias");
					if ((aliasDb == null && aliasOfSingleSubStreamInDataResponse == aliasDb) || (aliasOfSingleSubStreamInDataResponse.equalsIgnoreCase(aliasDb))) {
						Reporter.log(", Correct alias [" + aliasOfSingleSubStreamInDataResponse + "] of subStreamId : "
								+ subStreamId + "", true);
					} else {
						Reporter.log(", InCorrect alias of subStreamId: " + subStreamId + " ["
								+ aliasOfSingleSubStreamInDataResponse + "]", true);
						Assert.fail("InCorrect alias of subStreamId : " + subStreamId + " ["
								+ aliasOfSingleSubStreamInDataResponse + "]");
					}

					// verify synonyms in data[response] as same in db
					String synonymsDb = "[" + rs.getString("synonym") + "]";
					String synonymsOfSingleSubStreamInDataResponse = jsonPath
							.getString("data." + subStreamId + ".synonyms");
					if (synonymsOfSingleSubStreamInDataResponse.equalsIgnoreCase(synonymsDb)) {
						Reporter.log(", Correct synonyms [" + synonymsOfSingleSubStreamInDataResponse
								+ "] of subStreamId : " + subStreamId + "", true);
					} else {
						Reporter.log(", InCorrect synonyms of subStreamId: " + subStreamId + " ["
								+ synonymsOfSingleSubStreamInDataResponse + "]", true);
						Assert.fail("InCorrect synonyms of subStreamId : " + subStreamId + " ["
								+ synonymsOfSingleSubStreamInDataResponse + "]");
					}

					// verify primaryStreamId in data[response] as same in db
					String primaryStreamIdDb = rs.getString("primary_stream_id");
					String primaryStreamIdOfSingleSubStreamInDataResponse = jsonPath
							.getString("data." + subStreamId + ".primaryStreamId");
					if (primaryStreamIdOfSingleSubStreamInDataResponse.equalsIgnoreCase(primaryStreamIdDb)) {
						Reporter.log(", Correct primaryStreamId [" + primaryStreamIdOfSingleSubStreamInDataResponse
								+ "] of subStreamId : " + subStreamId + "", true);
					} else {
						Reporter.log(", InCorrect primaryStreamId of subStreamId: " + subStreamId + " ["
								+ primaryStreamIdOfSingleSubStreamInDataResponse + "]", true);
						Assert.fail("InCorrect primaryStreamId of subStreamId : " + subStreamId + " ["
								+ primaryStreamIdOfSingleSubStreamInDataResponse + "]");
					}

					// verify display_order in data[response] as same in db
					String displayOrderDb = rs.getString("display_order");
					String displayOrderOfSinglestreamInDataResponse = jsonPath
							.getString("data." + subStreamId + ".displayOrder");
					if (displayOrderOfSinglestreamInDataResponse.equalsIgnoreCase(displayOrderDb)) {
						Reporter.log(", Correct displayOrder [" + displayOrderOfSinglestreamInDataResponse
								+ "] of subStreamId : " + subStreamId + "", true);
					} else {
						Reporter.log(", InCorrect displayOrder of subStreamId: " + subStreamId + " ["
								+ displayOrderOfSinglestreamInDataResponse + "]", true);
						Assert.fail("InCorrect displayOrder of subStreamId : " + subStreamId + " ["
								+ displayOrderOfSinglestreamInDataResponse + "]");
					}
				}
			} else {
				// if no details fetched from db
				Assert.fail("Unable to fetch data from db");
			}
		} catch (SQLException e) {
			e.printStackTrace();
			Assert.fail("SQLException occured :" + e.getMessage());
		}
	}

	@Test(priority = 2)
	public static void respnseTimeStats_GetAllSubStreams() {
		respnseTimeStatsCommon(responseTimes, apiPath.substring(apiPath.lastIndexOf("/") + 1));
	}
}