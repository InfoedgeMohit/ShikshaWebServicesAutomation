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

public class GetMultipleSubStreams extends Common {

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
		apiPath = "baseentities/api/v1/info/getMultipleSubstreams";
		api = serverPath + apiPath;
		// API params in sequence followed by apiResponseMsgExpected
		params = new String[] { "substreamIds", "apiResponseMsgExpected" };
		// test data file
		testDataFilePath = "//src//test//resources//baseentities//info//GetMultipleSubStreams.xlsx";

	}

	@DataProvider
	public static String[][] testData() throws Exception {
		// The number of times data is repeated, test will be executed the same no. of
		// times
		String[][] testData = prepareTestData(params, testDataFilePath);
		return testData;
	}

	@Test(dataProvider = "testData", priority = 1)
	public void verifyGetMultipleSubStreamsApi(String substreamIds, String apiResponseMsgExpected) {
		this.apiResponseMsgExpected = apiResponseMsgExpected;

		// pass api params and hit api
		if (substreamIds.equals("ignoreHeader"))
			return;
//			apiResponse = RestAssured.given().when().post(api).then().extract().response();
		else if (substreamIds.equals("ignoreInput"))
			apiResponse = RestAssured.given().header("AUTHREQUEST", "INFOEDGE_SHIKSHA").when().post(api).then()
					.extract().response();
		else if (!substreamIds.equals("ignoreInput"))
			apiResponse = RestAssured.given().header("AUTHREQUEST", "INFOEDGE_SHIKSHA")
					.param("substreamIds[]", substreamIds).when().post(api).then().extract().response();

		// fetch status code from response
		int statusCode = apiResponse.getStatusCode();
		JsonPath jsonPath = apiResponse.jsonPath();

		if (statusCode == 200) { // Positive case
			Reporter.log("Positive case", true);
			verifyPostiveCases(substreamIds, jsonPath);
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

	public void verifyPostiveCases(String subStreamIds, JsonPath jsonPath) {
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

			// loop for multiple subStreamIds in array
			String[] subStreamIdsArr = subStreamIds.split(",");
			for (int i = 0; i < subStreamIdsArr.length; i++) {
				this.subStreamId = Integer.parseInt(subStreamIdsArr[i]);

				// fetch data of subStreamId from api response [jsonPath]
				String dataOfSingleSubStreamInResponse = jsonPath.getString("data." + subStreamId);

				// fetch details from substreams
				ResultSet rs = exceuteDbQuery(
						"SELECT name, alias, synonym, primary_stream_id, display_order FROM substreams where status = 'live' and substream_id = "
								+ subStreamId + ";",
						"shiksha");

				// get row count of resultSet of sql query
				int rowcount = getCountOfResultSet(rs);

				if (rowcount > 0) {

					rs.next();

					// verify subStreamId in data[response] as same in db
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
					String streamNameDb = rs.getString("name");
					String nameOfSingleSubStreamInDataResponse = jsonPath.getString("data." + subStreamId + ".name");
					if (nameOfSingleSubStreamInDataResponse.equalsIgnoreCase(streamNameDb)) {
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
					String primaryStreamIdOfSinglesubStreamInDataResponse = jsonPath
							.getString("data." + subStreamId + ".primaryStreamId");
					if (primaryStreamIdOfSinglesubStreamInDataResponse.equalsIgnoreCase(primaryStreamIdDb)) {
						Reporter.log(", Correct primaryStreamId [" + primaryStreamIdOfSinglesubStreamInDataResponse
								+ "] of subStreamId : " + subStreamId + "", true);
					} else {
						Reporter.log(", InCorrect primaryStreamId of subStreamId: " + subStreamId + " ["
								+ primaryStreamIdOfSinglesubStreamInDataResponse + "]", true);
						Assert.fail("InCorrect primaryStreamId of subStreamId : " + subStreamId + " ["
								+ primaryStreamIdOfSinglesubStreamInDataResponse + "]");
					}

					// verify display_order in data[response] as same in db
					String displayOrderDb = rs.getString("display_order");
					String displayOrderOfSingleSubStreamInDataResponse = jsonPath
							.getString("data." + subStreamId + ".displayOrder");
					if (displayOrderOfSingleSubStreamInDataResponse.equalsIgnoreCase(displayOrderDb)) {
						Reporter.log(", Correct displayOrder [" + displayOrderOfSingleSubStreamInDataResponse
								+ "] of subStreamId : " + subStreamId + "", true);
					} else {
						Reporter.log(", InCorrect displayOrder of subStreamId: " + subStreamId + " ["
								+ displayOrderOfSingleSubStreamInDataResponse + "]", true);
						Assert.fail("InCorrect displayOrder of subStreamId : " + subStreamId + " ["
								+ displayOrderOfSingleSubStreamInDataResponse + "]");
					}

				} else {
					// if subStreamId doesn't exist in db, then no data would be returned for that
					// particular subStreamId
					if (dataOfSingleSubStreamInResponse == null) {
						Reporter.log(", No data returned for subStreamId : " + subStreamId + " [Correct]", true);
					} else {
						Reporter.log(", InCorrect data of subStreamId : " + subStreamId + " ["
								+ dataOfSingleSubStreamInResponse + "]", true);
						Assert.fail("InCorrect data of subStreamId : " + subStreamId + " ["
								+ dataOfSingleSubStreamInResponse + "]");
					}
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
			Assert.fail("SQLException occured :" + e.getMessage());
		}
	}

	@Test(priority = 2)
	public static void respnseTimeStats_GetMultipleSubStreams() {
		respnseTimeStatsCommon(responseTimes, apiPath.substring(apiPath.lastIndexOf("/") + 1));
	}
}